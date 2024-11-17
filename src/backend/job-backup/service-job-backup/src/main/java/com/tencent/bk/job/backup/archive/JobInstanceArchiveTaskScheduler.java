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

import com.tencent.bk.job.backup.archive.dao.impl.TaskInstanceRecordDAO;
import com.tencent.bk.job.backup.archive.model.JobInstanceArchiveTaskInfo;
import com.tencent.bk.job.backup.config.ArchiveProperties;
import com.tencent.bk.job.backup.constant.ArchiveTaskTypeEnum;
import com.tencent.bk.job.common.sharding.mysql.config.ShardingProperties;
import com.tencent.bk.job.common.util.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 作业执行历史归档任务调度
 */
@Slf4j
public class JobInstanceArchiveTaskScheduler extends SmartLifecycle {

    private final ArchiveTaskTypeEnum archiveTaskType = ArchiveTaskTypeEnum.JOB_INSTANCE;

    private final ArchiveTaskService archiveTaskService;

    private final TaskInstanceRecordDAO taskInstanceRecordDAO;

    private final ArchiveProperties archiveProperties;

    private final ShardingProperties shardingProperties;

    private final JobInstanceArchiveTaskScheduleLock jobInstanceArchiveTaskScheduleLock;

    private final Set<ArchiveTaskWorker> workers = new HashSet<>();

    /**
     * 异步归档任务执行器
     */
    private final Executor taskExecutor = new SimpleAsyncTaskExecutor("JobInstanceArchiveTaskExecutor");

    private final AtomicInteger runningTasksCount = new AtomicInteger(0);

    private final Object lifecycleMonitor = new Object();

    /**
     * 作业执行历史归档任务调度组件是否处于活动状态
     */
    private volatile boolean active;


    public JobInstanceArchiveTaskScheduler(ArchiveTaskService archiveTaskService,
                                           TaskInstanceRecordDAO taskInstanceRecordDAO,
                                           ArchiveProperties archiveProperties,
                                           ShardingProperties shardingProperties,
                                           JobInstanceArchiveTaskScheduleLock jobInstanceArchiveTaskScheduleLock) {
        this.archiveTaskService = archiveTaskService;
        this.taskInstanceRecordDAO = taskInstanceRecordDAO;
        this.archiveProperties = archiveProperties;
        this.shardingProperties = shardingProperties;
        this.jobInstanceArchiveTaskScheduleLock = jobInstanceArchiveTaskScheduleLock;
    }

    public void schedule() {
        try {
            if (isRunning) {
                log.info("JobInstanceArchiveTaskScheduler is running, skip");
                return;
            } else {
                this.isRunning = true;
            }

            while (true) {
                // 获取归档任务调度锁
                boolean locked = jobInstanceArchiveTaskScheduleLock.lock();
                if (!locked) {
                    return;
                }
                if (runningTasksCount.get() >= archiveProperties.getConcurrent()) {
                    // 休眠一分钟，等待并行任务减少
                    ThreadUtils.sleep(1000 * 60L);
                    return;
                }

                List<JobInstanceArchiveTaskInfo> runningTasks =
                    archiveTaskService.listRunningTasks(ArchiveTaskTypeEnum.JOB_INSTANCE);
                List<JobInstanceArchiveTaskInfo> needScheduleTasks =
                    archiveTaskService.listScheduleTasks(ArchiveTaskTypeEnum.JOB_INSTANCE, 100);
                if (CollectionUtils.isEmpty(needScheduleTasks)) {
                    // 所有任务都已经被调度完成，退出本次任务调度
                    return;
                }
                ArchiveTaskPrioritySorter.sort(runningTasks, shardingProperties.getDbNodeCount(), needScheduleTasks);
                startArchiveTask(needScheduleTasks.get(0));
            }
        } finally {
            this.isRunning = false;
            jobInstanceArchiveTaskScheduleLock.unlock();
        }

    }

    private void startArchiveTask(JobInstanceArchiveTaskInfo archiveTaskInfo) {
        log.info("Start JobInstanceArchiveTask : {}", archiveTaskInfo);
        runningTasksCount.incrementAndGet();
        JobInstanceMainDataArchiveTask archiveTask = new JobInstanceMainDataArchiveTask();
        ArchiveTaskWorker worker = new ArchiveTaskWorker(archiveTask);
        workers.add(worker);
        worker.start();
    }

    /**
     * Spring Bean 生命周期管理-启动
     */
    @Override
    public void start() {
        synchronized (lifecycleMonitor) {
            this.active = true;
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
            for (ScheduledContinuousResultHandleTask task : scheduledTasks.values()) {
                shutdownExecutor.execute(new StopTask(task, tracer));
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

    @Override
    public boolean isRunning() {
        synchronized (this.lifecycleMonitor) {
            return (this.active);
        }
    }

    boolean isActive() {
        synchronized (this.lifecycleMonitor) {
            return this.active;
        }
    }

    /**
     * 消费者是否处于运行状态
     *
     * @param worker 消费者
     * @return 是否运行
     */
    private boolean isWorkerActive(TaskWorker worker) {
        boolean workerActive;
        synchronized (this.workersMonitor) {
            workerActive = this.workers.contains(worker);
        }
        return workerActive && this.isActive();
    }


}
