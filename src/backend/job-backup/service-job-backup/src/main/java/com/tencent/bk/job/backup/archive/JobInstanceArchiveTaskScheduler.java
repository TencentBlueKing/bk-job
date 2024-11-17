/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.backup.archive;

import com.tencent.bk.job.backup.archive.dao.JobInstanceColdDAO;
import com.tencent.bk.job.backup.archive.dao.impl.TaskInstanceRecordDAO;
import com.tencent.bk.job.backup.archive.model.JobInstanceArchiveTaskInfo;
import com.tencent.bk.job.backup.config.ArchiveProperties;
import com.tencent.bk.job.backup.constant.ArchiveTaskTypeEnum;
import com.tencent.bk.job.backup.metrics.ArchiveErrorTaskCounter;
import com.tencent.bk.job.common.sharding.mysql.config.ShardingProperties;
import com.tencent.bk.job.common.util.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.SmartLifecycle;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 作业执行历史归档任务调度
 */
@Slf4j
public class JobInstanceArchiveTaskScheduler implements SmartLifecycle {

    private final ArchiveTaskService archiveTaskService;

    private final TaskInstanceRecordDAO taskInstanceRecordDAO;

    private final ArchiveProperties archiveProperties;

    private final ShardingProperties shardingProperties;

    private final JobInstanceArchiveTaskScheduleLock jobInstanceArchiveTaskScheduleLock;

    private final JobInstanceSubTableArchivers jobInstanceSubTableArchivers;

    private final JobInstanceColdDAO jobInstanceColdDAO;
    private final ArchiveTaskLock archiveTaskLock;
    private final ArchiveErrorTaskCounter archiveErrorTaskCounter;
    private final ArchiveTablePropsStorage archiveTablePropsStorage;

    private final AtomicInteger runningTasksCount = new AtomicInteger(0);

    private final Object lifecycleMonitor = new Object();

    /**
     * 作业执行历史归档任务调度组件是否处于活动状态
     */
    private volatile boolean active;
    /**
     * 组件是否正在运行(用于 Spring Lifecycle isRunning 判断)
     */
    private volatile boolean running = false;
    /**
     * 是否正在进行任务调度中
     */
    private volatile boolean scheduling = false;

    private final ExecutorService shutdownExecutor = new ThreadPoolExecutor(1, 20, 120L,
        TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    /**
     * 调度的所有的任务
     */
    private final Map<String, JobInstanceMainDataArchiveTask> scheduledTasks = new ConcurrentHashMap<>();


    public JobInstanceArchiveTaskScheduler(ArchiveTaskService archiveTaskService,
                                           TaskInstanceRecordDAO taskInstanceRecordDAO,
                                           ArchiveProperties archiveProperties,
                                           ShardingProperties shardingProperties,
                                           JobInstanceArchiveTaskScheduleLock jobInstanceArchiveTaskScheduleLock,
                                           JobInstanceSubTableArchivers jobInstanceSubTableArchivers,
                                           JobInstanceColdDAO jobInstanceColdDAO,
                                           ArchiveTaskLock archiveTaskLock,
                                           ArchiveErrorTaskCounter archiveErrorTaskCounter,
                                           ArchiveTablePropsStorage archiveTablePropsStorage) {
        this.archiveTaskService = archiveTaskService;
        this.taskInstanceRecordDAO = taskInstanceRecordDAO;
        this.archiveProperties = archiveProperties;
        this.shardingProperties = shardingProperties;
        this.jobInstanceArchiveTaskScheduleLock = jobInstanceArchiveTaskScheduleLock;
        this.jobInstanceSubTableArchivers = jobInstanceSubTableArchivers;
        this.jobInstanceColdDAO = jobInstanceColdDAO;
        this.archiveTaskLock = archiveTaskLock;
        this.archiveErrorTaskCounter = archiveErrorTaskCounter;
        this.archiveTablePropsStorage = archiveTablePropsStorage;
    }

    public void schedule() {
        try {
            if (isScheduling()) {
                log.info("Scheduler is already working");
                return;
            }
            if (!isActive()) {
                log.info("JobInstanceArchiveTaskScheduler is not active, skip");
                return;
            }

            while (true) {
                // 获取归档任务调度锁
                boolean locked = jobInstanceArchiveTaskScheduleLock.lock();
                if (!locked) {
                    log.info("Get lock fail, wait 1s");
                    ThreadUtils.sleep(1000L);
                    continue;
                }
                if (runningTasksCount.get() >= archiveProperties.getConcurrent()) {
                    // 休眠一分钟，等待并行任务减少
                    log.info("Running archive task count exceed concurrent limit : {}, wait 60s",
                        archiveProperties.getConcurrent());
                    ThreadUtils.sleep(1000 * 60L);
                    continue;
                }
                // 获取待调度的任务列表
                List<JobInstanceArchiveTaskInfo> needScheduleTasks =
                    archiveTaskService.listScheduleTasks(ArchiveTaskTypeEnum.JOB_INSTANCE, 100);
                if (CollectionUtils.isEmpty(needScheduleTasks)) {
                    // 所有任务都已经被调度完成，退出本次任务调度
                    return;
                }

                // 获取正在执行中的任务列表
                List<JobInstanceArchiveTaskInfo> runningTasks =
                    archiveTaskService.listRunningTasks(ArchiveTaskTypeEnum.JOB_INSTANCE);
                // 对待调度的任务进行优先级排序，保证同一个 db 上的归档任务数量尽可能均衡，避免出现db 热点
                ArchiveTaskPrioritySorter.sort(runningTasks, shardingProperties.getDbNodeCount(), needScheduleTasks);

                startArchiveTask(needScheduleTasks.get(0));
            }
        } finally {
            this.scheduling = false;
            jobInstanceArchiveTaskScheduleLock.unlock();
        }

    }

    private void startArchiveTask(JobInstanceArchiveTaskInfo archiveTaskInfo) {
        log.info("Start JobInstanceArchiveTask : {}", archiveTaskInfo);
        JobInstanceMainDataArchiveTask archiveTask = new JobInstanceMainDataArchiveTask(
            taskInstanceRecordDAO,
            jobInstanceSubTableArchivers,
            jobInstanceColdDAO,
            archiveProperties,
            archiveTaskLock,
            archiveErrorTaskCounter,
            archiveTaskInfo,
            archiveTaskService,
            archiveTablePropsStorage
        );
        synchronized (lifecycleMonitor) {
            if (!isActive()) {
                log.info("JobInstanceArchiveTaskScheduler is not active, skip");
                return;
            }
            scheduledTasks.put(archiveTask.getTaskId(), archiveTask);
        }
        runningTasksCount.incrementAndGet();
        ArchiveTaskWorker worker = new ArchiveTaskWorker(archiveTask);
        worker.start();
    }

    /**
     * Spring Bean 生命周期管理-启动
     */
    @Override
    public void start() {
        if (isRunning()) {
            return;
        }
        synchronized (lifecycleMonitor) {
            this.active = true;
            this.running = true;
        }
    }

    /**
     * Spring Bean 生命周期管理-停止
     */
    @Override
    public void stop() {
        log.info("JobInstanceArchiveTaskScheduler stopping.");
        synchronized (this.lifecycleMonitor) {
            if (!isActive()) {
                log.info("Shutdown ignored - JobInstanceArchiveTaskScheduler is not active already");
                return;
            }
            this.active = false;
            this.running = false;
        }
        stopTasksGraceful();
        log.info("JobInstanceArchiveTaskScheduler stop successfully!");
    }

    private void stopTasksGraceful() {
        log.info("Stop archive tasks graceful - start");
        long start = System.currentTimeMillis();
        StopTaskCounter stopTaskCounter = null;
        synchronized (lifecycleMonitor) {
            if (!this.scheduledTasks.isEmpty()) {
                log.info("Stop archive tasks, size: {}, tasks: {}", scheduledTasks.size(), scheduledTasks);
                stopTaskCounter = StopTaskCounter.getInstance();
                stopTaskCounter.initCounter(scheduledTasks.keySet());
            }
            for (JobInstanceArchiveTask task : scheduledTasks.values()) {
                shutdownExecutor.execute(new StopTask(task));
            }
        }
        try {
            if (stopTaskCounter != null) {
                stopTaskCounter.waitingForAllTasksDone();
            }
        } catch (Throwable e) {
            log.error("Stop tasks caught exception", e);
        }
        long end = System.currentTimeMillis();
        log.info("Stop tasks graceful - end, cost: {}", end - start);
    }

    private static final class StopTask implements Runnable {
        private final JobInstanceArchiveTask task;

        StopTask(JobInstanceArchiveTask task) {
            this.task = task;
        }

        @Override
        public void run() {
            try {
                task.stop();
            } catch (Throwable e) {
                String errorMsg = "Stop archive task caught exception, task: " + task;
                log.warn(errorMsg, e);
            }
        }
    }

    @Override
    public boolean isRunning() {
        synchronized (this.lifecycleMonitor) {
            return (this.running);
        }
    }

    private boolean isActive() {
        synchronized (this.lifecycleMonitor) {
            return this.active;
        }
    }

    private boolean isScheduling() {
        synchronized (this.lifecycleMonitor) {
            return this.scheduling;
        }
    }

}
