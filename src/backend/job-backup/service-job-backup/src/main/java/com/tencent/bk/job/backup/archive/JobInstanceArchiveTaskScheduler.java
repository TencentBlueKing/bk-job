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
import com.tencent.bk.job.backup.archive.dao.impl.JobInstanceHotRecordDAO;
import com.tencent.bk.job.backup.archive.model.JobInstanceArchiveTaskInfo;
import com.tencent.bk.job.backup.archive.service.ArchiveTaskService;
import com.tencent.bk.job.backup.archive.util.lock.ArchiveTaskExecuteLock;
import com.tencent.bk.job.backup.archive.util.lock.JobInstanceArchiveTaskScheduleLock;
import com.tencent.bk.job.backup.config.ArchiveProperties;
import com.tencent.bk.job.backup.constant.ArchiveTaskTypeEnum;
import com.tencent.bk.job.backup.metrics.ArchiveErrorTaskCounter;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.StopWatch;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * 作业执行历史归档任务调度
 */
@Slf4j
public class JobInstanceArchiveTaskScheduler implements SmartLifecycle {

    private final ArchiveTaskService archiveTaskService;

    private final JobInstanceHotRecordDAO taskInstanceRecordDAO;

    private final ArchiveProperties archiveProperties;

    private final JobInstanceArchiveTaskScheduleLock jobInstanceArchiveTaskScheduleLock;

    private final JobInstanceSubTableArchivers jobInstanceSubTableArchivers;

    private final JobInstanceColdDAO jobInstanceColdDAO;
    private final ArchiveTaskExecuteLock archiveTaskExecuteLock;
    private final ArchiveErrorTaskCounter archiveErrorTaskCounter;
    private final ArchiveTablePropsStorage archiveTablePropsStorage;

    private final Tracer tracer;

    private final Object lifecycleMonitor = new Object();

    /**
     * 作业执行历史归档任务调度组件是否处于活动状态
     */
    private volatile boolean active;
    /**
     * 是否正在进行任务调度中
     */
    private volatile boolean scheduling = false;

    private final ExecutorService archiveTaskStopExecutor;

    /**
     * 调度的所有的任务
     */
    private final Map<String, JobInstanceMainDataArchiveTask> scheduledTasks = new ConcurrentHashMap<>();

    /**
     * 调度器线程挂起 object monitor
     */
    private final Object schedulerHangMonitor = new Object();


    public JobInstanceArchiveTaskScheduler(ArchiveTaskService archiveTaskService,
                                           JobInstanceHotRecordDAO taskInstanceRecordDAO,
                                           ArchiveProperties archiveProperties,
                                           JobInstanceArchiveTaskScheduleLock jobInstanceArchiveTaskScheduleLock,
                                           JobInstanceSubTableArchivers jobInstanceSubTableArchivers,
                                           JobInstanceColdDAO jobInstanceColdDAO,
                                           ArchiveTaskExecuteLock archiveTaskExecuteLock,
                                           ArchiveErrorTaskCounter archiveErrorTaskCounter,
                                           ArchiveTablePropsStorage archiveTablePropsStorage,
                                           Tracer tracer,
                                           ExecutorService archiveTaskStopExecutor) {
        this.archiveTaskService = archiveTaskService;
        this.taskInstanceRecordDAO = taskInstanceRecordDAO;
        this.archiveProperties = archiveProperties;
        this.jobInstanceArchiveTaskScheduleLock = jobInstanceArchiveTaskScheduleLock;
        this.jobInstanceSubTableArchivers = jobInstanceSubTableArchivers;
        this.jobInstanceColdDAO = jobInstanceColdDAO;
        this.archiveTaskExecuteLock = archiveTaskExecuteLock;
        this.archiveErrorTaskCounter = archiveErrorTaskCounter;
        this.archiveTablePropsStorage = archiveTablePropsStorage;
        this.tracer = tracer;
        this.archiveTaskStopExecutor = archiveTaskStopExecutor;
    }

    public void schedule() {
        try {
            if (isScheduling()) {
                log.info("Scheduler is working");
                return;
            }
            this.scheduling = true;

            while (true) {
                if (!isActive()) {
                    log.info("JobInstanceArchiveTaskScheduler is not active, skip");
                    return;
                }
                StopWatch watch = new StopWatch("archive-task-schedule");
                boolean locked = false;
                try {
                    // 获取归档任务调度锁
                    locked = jobInstanceArchiveTaskScheduleLock.lock();
                    if (!locked) {
                        log.info("Get lock fail, wait 1s");
                        ThreadUtils.sleep(1000L);
                        continue;
                    }

                    // 获取待调度的任务信息(按照 DB 节点计数)
                    watch.start("countScheduleTasks");
                    Map<String, Integer> scheduleTasksGroupByDb =
                        archiveTaskService.countScheduleTasksGroupByDb(ArchiveTaskTypeEnum.JOB_INSTANCE);
                    if (scheduleTasksGroupByDb.isEmpty()) {
                        // 所有任务都已经被调度完成，退出本次任务调度
                        log.info("No archive task need scheduling! Exit");
                        return;
                    }
                    watch.stop();
                    log.info("Count archive task group by db, result: {}", scheduleTasksGroupByDb);

                    // 获取正在执行中的任务列表
                    watch.start("queryRunningTasks");
                    List<JobInstanceArchiveTaskInfo> runningTasks =
                        archiveTaskService.listRunningTasks(ArchiveTaskTypeEnum.JOB_INSTANCE);
                    watch.stop();

                    // 对待调度的任务进行优先级排序，保证同一个 db 上的归档任务数量尽可能均衡，避免出现db 热点
                    watch.start("evaluateTaskPriority");
                    ArchiveDbNodePriorityEvaluator.DbNodeTasksInfo highestPriorityDbNodeTasksInfo =
                        ArchiveDbNodePriorityEvaluator.evaluateHighestPriorityDbNode(runningTasks,
                            scheduleTasksGroupByDb);
                    watch.stop();
                    int taskConcurrent = archiveProperties.getTasks().getJobInstance().getConcurrent();
                    if (highestPriorityDbNodeTasksInfo.getRunningTaskCount() >= taskConcurrent) {
                        // 休眠1分钟，等待并行任务减少
                        log.info("Running archive task count exceed concurrent limit : {}, wait 60s", taskConcurrent);
                        // 释放锁
                        jobInstanceArchiveTaskScheduleLock.unlock();
                        locked = false;
                        synchronized (schedulerHangMonitor) {
                            schedulerHangMonitor.wait(1000 * 60L);
                        }
                        continue;
                    }

                    // 获取优先级最高的归档任务
                    watch.start("getHighestPriorityTaskFromDb");
                    String dbNodeId = highestPriorityDbNodeTasksInfo.getDbNodeId();
                    JobInstanceArchiveTaskInfo archiveTaskInfo =
                        archiveTaskService.getFirstScheduleArchiveTaskByDb(ArchiveTaskTypeEnum.JOB_INSTANCE, dbNodeId);
                    watch.stop();

                    // 启动任务
                    watch.start("startArchiveTask");
                    startArchiveTask(archiveTaskInfo);
                    watch.stop();
                } finally {
                    if (locked) {
                        jobInstanceArchiveTaskScheduleLock.unlock();
                    }
                    if (watch.isRunning()) {
                        watch.stop();
                    }
                    if (watch.getTotalTimeMillis() > 1000) {
                        log.info("Schedule archive task slow, cost statistics: {}", watch.prettyPrint());
                    }
                }
            }
        } catch (Throwable e) {
            log.error("Schedule archive task caught exception", e);
        } finally {
            this.scheduling = false;
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
            archiveTaskExecuteLock,
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
        synchronized (schedulerHangMonitor) {
            schedulerHangMonitor.notify();
            log.info("Try notify scheduler when stopping");
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
                log.info("Submit stop archive task to executor, taskId: {}", task.getTaskId());
                archiveTaskStopExecutor.execute(new StopTask(task, taskCountDownLatch));
            }
        }
        try {
            if (taskCountDownLatch != null) {
                // 等待任务结束，最多等待 30s(等待时间太长进程会被k8s kill掉)
                boolean isAllTaskStopped = taskCountDownLatch.waitingForAllTasksDone(30);
                if (!isAllTaskStopped) {
                    for (JobInstanceArchiveTask task : scheduledTasks.values()) {
                        task.forceStopAtOnce();
                    }
                }
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
                log.info("[{}] Run stop task begin", task.getTaskId());
                task.stop(() -> taskCountDownLatch.decrement(task.getTaskId()));
            } catch (Throwable e) {
                String errorMsg = "Stop archive task caught exception, task: " + task.getTaskId();
                log.warn(errorMsg, e);
            } finally {
                log.info("[{}] Run stop task end", task.getTaskId());
            }
        }
    }

    @Override
    public boolean isRunning() {
        synchronized (this.lifecycleMonitor) {
            return (this.active);
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
