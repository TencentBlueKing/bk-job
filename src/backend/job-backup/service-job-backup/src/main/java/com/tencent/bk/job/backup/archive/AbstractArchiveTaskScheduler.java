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

import com.tencent.bk.job.backup.archive.model.ArchiveTaskInfo;
import com.tencent.bk.job.backup.archive.service.ArchiveTaskService;
import com.tencent.bk.job.backup.config.ArchiveProperties;
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
 * 通用归档任务调度抽象基类
 */
@Slf4j
public abstract class AbstractArchiveTaskScheduler<T extends JobHistoricalDataArchiveTask> implements SmartLifecycle {

    protected final ArchiveTaskService archiveTaskService;
    protected final ArchiveProperties archiveProperties;
    protected final ExecutorService archiveTaskStopExecutor;
    protected final ArchiveErrorTaskCounter archiveErrorTaskCounter;
    protected final Tracer tracer;

    private final Map<String, T> scheduledTasks = new ConcurrentHashMap<>();
    private final Object lifecycleMonitor = new Object();
    /**
     * 调度器线程挂起 object monitor
     */
    private final Object schedulerHangMonitor = new Object();

    /**
     * 作业执行日志归档任务调度组件是否处于活动状态
     */
    private volatile boolean active;
    /**
     * 是否正在进行任务调度中
     */
    private volatile boolean scheduling = false;

    public AbstractArchiveTaskScheduler(ArchiveTaskService archiveTaskService,
                                        ArchiveProperties archiveProperties,
                                        ExecutorService archiveTaskStopExecutor,
                                        ArchiveErrorTaskCounter archiveErrorTaskCounter,
                                        Tracer tracer) {
        this.archiveTaskService = archiveTaskService;
        this.archiveProperties = archiveProperties;
        this.archiveTaskStopExecutor = archiveTaskStopExecutor;
        this.archiveErrorTaskCounter = archiveErrorTaskCounter;
        this.tracer = tracer;
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

    public void schedule() {
        try {
            if (isScheduling()) {
                log.info("{} is working", getClass().getSimpleName());
                return;
            }
            this.scheduling = true;

            while (true) {
                if (!isActive()) {
                    log.info("{} is not active, skip", getClass().getSimpleName());
                    return;
                }
                StopWatch watch = new StopWatch("archive-task-schedule-" + getClass().getSimpleName());
                boolean locked = false;
                try {
                    // 1.获取归档任务调度锁
                    locked = acquireScheduleLock();
                    if (!locked) {
                        log.info("{} get lock fail, wait 1s", getClass().getSimpleName());
                        ThreadUtils.sleep(1000L);
                        continue;
                    }

                    // 2.获取待调度的任务信息(按照DB节点计数)，如果所有任务都已经被调度完成，退出本次任务调度
                    watch.start("countScheduleTasks");
                    Map<String, Integer> scheduleTasksGroupByDb = countScheduleTasksGroupByDb();
                    if (scheduleTasksGroupByDb.isEmpty()) {
                        log.info("{} no archive task need scheduling! Exit",
                            getClass().getSimpleName());
                        return;
                    }
                    watch.stop();
                    log.info("{} count archive task group by db, result: {}",
                        getClass().getSimpleName(), scheduleTasksGroupByDb);

                    // 3.获取正在执行中的任务列表
                    watch.start("queryRunningTasks");
                    List<ArchiveTaskInfo> runningTasks = listRunningTasks();
                    watch.stop();

                    // 4. 是否需要等待
                    int taskConcurrent = archiveProperties.getTasks().getArchiveTaskConfig().getConcurrent();
                    if (isExecuteLogArchiveTask()) {
                        taskConcurrent = archiveProperties.getExecuteLog().getConcurrent();
                    }
                    if (shouldWait(watch, runningTasks, scheduleTasksGroupByDb)) {
                        // 休眠1分钟，等待并行任务减少
                        log.info("{} running archive task count exceed concurrent limit : {}, " +
                            "wait 60s", taskConcurrent, getClass().getSimpleName());
                        // 释放锁
                        releaseScheduleLock();
                        locked = false;
                        synchronized (schedulerHangMonitor) {
                            schedulerHangMonitor.wait(1000 * 60L);
                        }
                        continue;
                    }

                    // 5.获取优先级最高的归档任务
                    watch.start("getNextTask");
                    ArchiveTaskInfo next = getNextTask(scheduleTasksGroupByDb, runningTasks);
                    watch.stop();

                    // 6. 启动归档任务
                    watch.start("startTask");
                    startArchiveTask(next);
                    watch.stop();

                    if (watch.getTotalTimeMillis() > 1000) {
                        log.info("{} scheduling slow for {}, times: {}",
                            getClass().getSimpleName(), watch.prettyPrint(), getClass().getSimpleName());
                    }
                } finally {
                    if (locked) {
                        releaseScheduleLock();
                    }
                    if (watch.isRunning()) {
                        watch.stop();
                    }
                    if (watch.getTotalTimeMillis() > 1000) {
                        log.info("{} schedule archive task slow, cost statistics: {}",
                            watch.prettyPrint(), getClass().getSimpleName());
                    }
                }
            }
        } catch (Throwable t) {
            log.error("{} schedule caught exception", getClass().getSimpleName(), t);
        } finally {
            this.scheduling = false;
        }
    }

    private void startArchiveTask(ArchiveTaskInfo archiveTaskInfo) {
        log.info("Start {}, taskId: {}, taskInfo: {}", getClass().getSimpleName(),
            archiveTaskInfo.buildTaskUniqueId(), JsonUtils.toJson(archiveTaskInfo));
        T archiveTask = createArchiveTask(archiveTaskInfo);

        // 注册任务完成回调函数
        archiveTask.registerDoneCallback(() -> {
            synchronized (lifecycleMonitor) {
                scheduledTasks.remove(archiveTask.getTaskId());
            }
        });
        synchronized (lifecycleMonitor) {
            if (!isActive()) {
                log.info("{} is not active, skip", getClass().getSimpleName());
                return;
            }
            scheduledTasks.put(archiveTask.getTaskId(), archiveTask);
        }
        ArchiveTaskWorker worker = new ArchiveTaskWorker(archiveTask, tracer);
        worker.start();
        log.info("Started data archive task {} for {}",
            archiveTaskInfo.buildTaskUniqueId(), getClass().getSimpleName());
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
            for (JobHistoricalDataArchiveTask task : scheduledTasks.values()) {
                log.info("Submit stop archive task to executor, taskId: {}", task.getTaskId());
                archiveTaskStopExecutor.execute(new StopTask(task, taskCountDownLatch));
            }
        }
        try {
            if (taskCountDownLatch != null) {
                // 等待任务结束，最多等待 30s(等待时间太长进程会被k8s kill掉)
                boolean isAllTaskStopped = taskCountDownLatch.waitingForAllTasksDone(30);
                if (!isAllTaskStopped) {
                    for (JobHistoricalDataArchiveTask task : scheduledTasks.values()) {
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
        private final JobHistoricalDataArchiveTask task;
        private final TaskCountDownLatch taskCountDownLatch;

        StopTask(JobHistoricalDataArchiveTask task, TaskCountDownLatch taskCountDownLatch) {
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

    /**
     * 是否执行日志归档
     */
    protected boolean isExecuteLogArchiveTask() {
        return false;
    }

    /**
     * 获取调度锁
     */
    protected abstract boolean acquireScheduleLock();

    /**
     * 释放调度锁
     */
    protected abstract void releaseScheduleLock();

    /**
     * 获取待调度的任务信息(按照DB节点计数)
     */
    protected abstract Map<String, Integer> countScheduleTasksGroupByDb();

    /**
     * 获取正在运行的归档任务列表
     */
    protected abstract List<ArchiveTaskInfo> listRunningTasks();

    /**
     * 是否需要等待，比如达到了最大归档任务数
     */
    protected abstract boolean shouldWait(StopWatch watch,
                               List<ArchiveTaskInfo> runningTasks,
                               Map<String, Integer> scheduleTasksGroupByDb);

    /**
     * 获取优先级最高的归档任务
     */
    protected abstract ArchiveTaskInfo getNextTask(Map<String, Integer> scheduleTasksGroupByDb,
                                                     List<ArchiveTaskInfo> running);

    /**
     * 创建归档任务对象
     */
    protected abstract T createArchiveTask(ArchiveTaskInfo archiveTaskInfo);
}
