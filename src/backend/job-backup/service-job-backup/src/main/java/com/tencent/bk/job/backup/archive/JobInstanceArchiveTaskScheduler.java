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
import com.tencent.bk.job.backup.archive.service.ArchiveTaskService;
import com.tencent.bk.job.backup.config.ArchiveProperties;
import com.tencent.bk.job.backup.constant.ArchiveTaskTypeEnum;
import com.tencent.bk.job.backup.metrics.ArchiveErrorTaskCounter;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.SmartLifecycle;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 作业执行历史归档任务调度
 */
@Slf4j
public class JobInstanceArchiveTaskScheduler implements SmartLifecycle {

    private final ArchiveTaskService archiveTaskService;

    private final TaskInstanceRecordDAO taskInstanceRecordDAO;

    private final ArchiveProperties archiveProperties;

    private final JobInstanceArchiveTaskScheduleLock jobInstanceArchiveTaskScheduleLock;

    private final JobInstanceSubTableArchivers jobInstanceSubTableArchivers;

    private final JobInstanceColdDAO jobInstanceColdDAO;
    private final ArchiveTaskLock archiveTaskLock;
    private final ArchiveErrorTaskCounter archiveErrorTaskCounter;
    private final ArchiveTablePropsStorage archiveTablePropsStorage;

    private final Tracer tracer;

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
                                           JobInstanceArchiveTaskScheduleLock jobInstanceArchiveTaskScheduleLock,
                                           JobInstanceSubTableArchivers jobInstanceSubTableArchivers,
                                           JobInstanceColdDAO jobInstanceColdDAO,
                                           ArchiveTaskLock archiveTaskLock,
                                           ArchiveErrorTaskCounter archiveErrorTaskCounter,
                                           ArchiveTablePropsStorage archiveTablePropsStorage,
                                           Tracer tracer) {
        this.archiveTaskService = archiveTaskService;
        this.taskInstanceRecordDAO = taskInstanceRecordDAO;
        this.archiveProperties = archiveProperties;
        this.jobInstanceArchiveTaskScheduleLock = jobInstanceArchiveTaskScheduleLock;
        this.jobInstanceSubTableArchivers = jobInstanceSubTableArchivers;
        this.jobInstanceColdDAO = jobInstanceColdDAO;
        this.archiveTaskLock = archiveTaskLock;
        this.archiveErrorTaskCounter = archiveErrorTaskCounter;
        this.archiveTablePropsStorage = archiveTablePropsStorage;
        this.tracer = tracer;
    }

    public void schedule() {
        try {
            if (isScheduling()) {
                log.info("Scheduler is working");
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

                // 获取待调度的任务信息(按照 DB 节点计数)
                Map<String, Integer> scheduleTasksGroupByDb =
                    archiveTaskService.countScheduleTasksGroupByDb(ArchiveTaskTypeEnum.JOB_INSTANCE);
                if (scheduleTasksGroupByDb.isEmpty()) {
                    // 所有任务都已经被调度完成，退出本次任务调度
                    log.info("No archive task need scheduling! Exit");
                    return;
                }
                log.info("Count archive task group by db, result: {}", scheduleTasksGroupByDb);

                // 获取正在执行中的任务列表
                List<JobInstanceArchiveTaskInfo> runningTasks =
                    archiveTaskService.listRunningTasks(ArchiveTaskTypeEnum.JOB_INSTANCE);

                // 对待调度的任务进行优先级排序，保证同一个 db 上的归档任务数量尽可能均衡，避免出现db 热点
                ArchiveDbNodePriorityEvaluator.DbNodeTasksInfo highestPriorityDbNodeTasksInfo =
                    ArchiveDbNodePriorityEvaluator.evaluateHighestPriorityDbNode(runningTasks, scheduleTasksGroupByDb);
                int taskConcurrent = archiveProperties.getTasks().getJobInstance().getConcurrent();
                if (highestPriorityDbNodeTasksInfo.getRunningTaskCount() >= taskConcurrent) {
                    // 休眠5分钟，等待并行任务减少
                    log.info("Running archive task count exceed concurrent limit : {}, wait 300s", taskConcurrent);
                    ThreadUtils.sleep(1000 * 60L);
                    continue;
                }

                // 获取优先级最高的归档任务
                String dbNodeId = highestPriorityDbNodeTasksInfo.getDbNodeId();
                JobInstanceArchiveTaskInfo archiveTaskInfo =
                    archiveTaskService.getFirstScheduleArchiveTaskByDb(ArchiveTaskTypeEnum.JOB_INSTANCE, dbNodeId);

                // 启动任务
                startArchiveTask(archiveTaskInfo);
            }
        } finally {
            this.scheduling = false;
            jobInstanceArchiveTaskScheduleLock.unlock();
        }

    }

    private void startArchiveTask(JobInstanceArchiveTaskInfo archiveTaskInfo) {
        log.info("Start JobInstanceArchiveTask, taskId: {}, taskInfo: {}",
            archiveTaskInfo.buildTaskUniqueId(), JsonUtils.toJson(archiveTaskInfo));
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
        // 注册任务完成回调函数
        archiveTask.registerDoneCallback(() -> {
            synchronized (lifecycleMonitor) {
                scheduledTasks.remove(archiveTask.getTaskId());
            }
        });
        synchronized (lifecycleMonitor) {
            if (!isActive()) {
                log.info("JobInstanceArchiveTaskScheduler is not active, skip");
                return;
            }
            scheduledTasks.put(archiveTask.getTaskId(), archiveTask);
        }
        ArchiveTaskWorker worker = new ArchiveTaskWorker(archiveTask, tracer);
        worker.start();
        log.info("Start JobInstanceArchiveTask success, taskId: {}", archiveTaskInfo.buildTaskUniqueId());
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
        TaskCountDownLatch taskCountDownLatch = null;
        synchronized (lifecycleMonitor) {
            if (!this.scheduledTasks.isEmpty()) {
                log.info("Stop archive tasks, size: {}, tasks: {}", scheduledTasks.size(), scheduledTasks);
                taskCountDownLatch = new TaskCountDownLatch(scheduledTasks.keySet());
            }
            for (JobInstanceArchiveTask task : scheduledTasks.values()) {
                shutdownExecutor.execute(new StopTask(task, taskCountDownLatch));
            }
        }
        try {
            if (taskCountDownLatch != null) {
                taskCountDownLatch.waitingForAllTasksDone();
            }
        } catch (Throwable e) {
            log.error("Stop archive tasks caught exception", e);
        }
        long end = System.currentTimeMillis();
        log.info("Stop archive tasks graceful - end, cost: {}", end - start);
    }

    private static final class StopTask implements Runnable {
        private final JobInstanceArchiveTask task;
        private final TaskCountDownLatch taskCountDownLatch;

        StopTask(JobInstanceArchiveTask task, TaskCountDownLatch taskCountDownLatch) {
            this.task = task;
            this.taskCountDownLatch = taskCountDownLatch;
        }

        @Override
        public void run() {
            try {
                task.stop(() -> taskCountDownLatch.decrement(task.getTaskId()));
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

    /**
     * 判断是否处于激活状态
     */
    private boolean isActive() {
        synchronized (this.lifecycleMonitor) {
            return this.active;
        }
    }

    /**
     * 判断是否正在进行任务调度中
     */
    private boolean isScheduling() {
        synchronized (this.lifecycleMonitor) {
            return this.scheduling;
        }
    }

}
