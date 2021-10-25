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

package com.tencent.bk.job.execute.engine.result;

import brave.ScopedSpan;
import brave.Tracing;
import com.tencent.bk.job.execute.common.exception.MessageHandlerUnavailableException;
import com.tencent.bk.job.execute.common.ha.DestroyOrder;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.engine.result.ha.ResultHandleLimiter;
import com.tencent.bk.job.execute.engine.result.ha.ResultHandleTaskKeepaliveManager;
import com.tencent.bk.job.execute.monitor.metrics.ExecuteMonitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 任务执行结果处理。
 * 背景: 作业平台任务下发到管控平台之后，由于任务执行时间较长，且任务并发很高，所以在任务下发之后，采用异步轮询的方式查询任务执行结果。
 * 方案：通过java DelayQueue 延迟队列 + 消费者模式实现了任务结果定时轮询逻辑
 */
@Component
@Slf4j
public class ResultHandleManager implements SmartLifecycle {
    /**
     * 日志调用链Tracing
     */
    private final Tracing tracing;
    /**
     * 结果处理任务存活管理
     */
    private final ResultHandleTaskKeepaliveManager resultHandleTaskKeepaliveManager;
    private final Object workersMonitor = new Object();
    private final Object lifecycleMonitor = new Object();
    /**
     * 任务执行计数器
     */
    private final ExecuteMonitor counters;
    /**
     * 任务处理数据采样器
     */
    private final ResultHandleTaskSampler resultHandleTaskSampler;
    private final ResultHandleLimiter resultHandleLimiter;
    /**
     * 任务结果处理的任务队列
     */
    private final DelayQueue<ScheduledContinuousResultHandleTask> tasksQueue = new DelayQueue<>();
    /**
     * 调度的所有的任务
     */
    private final Map<String, ScheduledContinuousResultHandleTask> scheduledTasks = new ConcurrentHashMap<>();
    /**
     * 任务消费者
     */
    private final Set<TaskWorker> workers = new HashSet<>();
    /**
     * 异步任务执行器，用于启动消费者线程
     */
    private final Executor taskExecutor = new SimpleAsyncTaskExecutor("task-result-handle-");
    /**
     * 最小任务处理线程
     */
    private final int CORE_WORKERS = 50;
    /**
     * 最大任务处理线程
     */
    private final int MAX_WORKERS = 100;
    /**
     * 触发新增worker阈值：worker连续处理的任务数
     */
    private final int consecutiveActiveTrigger = 10;
    /**
     * 触发回收worker阈值：worker连续空闲的周期数
     */
    private final int consecutiveIdleTrigger = 10;
    /**
     * 最近一次worker启动时间
     */
    private volatile long lastWorkerStartedAt;
    /**
     * 新增消费者线程最小间隔时间
     */
    private volatile long startConsumerMinInterval = 10000;
    /**
     * 最近一次worker停止时间
     */
    private volatile long lastWorkerStoppedAt;
    /**
     * 停止消费者线程最小间隔时间
     */
    private volatile long stopConsumerMinInterval = 60000;
    /**
     * 任务结果处理引擎是否活动状态
     */
    private volatile boolean active = false;
    /**
     * whether this component is currently running(Spring Lifecycle isRunning method)
     */
    private volatile boolean running = false;
    private final ExecutorService shutdownExecutorService = new ThreadPoolExecutor(
        10, 20, 120, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>());

    @Autowired
    public ResultHandleManager(Tracing tracing, ExecuteMonitor counters,
                               ResultHandleTaskKeepaliveManager resultHandleTaskKeepaliveManager,
                               ResultHandleTaskSampler resultHandleTaskSampler, JobExecuteConfig jobExecuteConfig) {
        this.tracing = tracing;
        this.counters = counters;
        this.resultHandleTaskKeepaliveManager = resultHandleTaskKeepaliveManager;
        this.resultHandleTaskSampler = resultHandleTaskSampler;
        this.resultHandleLimiter = new ResultHandleLimiter(jobExecuteConfig.getResultHandleTasksLimit());
    }

    /**
     * 处理任务
     *
     * @param task 任务
     */
    public void handleDeliveredTask(ContinuousScheduledTask task) {
        resultHandleLimiter.acquire();
        log.info("Handle delivered task: {}", task);
        ScheduledContinuousResultHandleTask scheduleTask =
            new ScheduledContinuousResultHandleTask(resultHandleTaskSampler, tracing, task, this,
                resultHandleTaskKeepaliveManager, resultHandleLimiter);
        synchronized (lifecycleMonitor) {
            if (!isActive()) {
                log.warn("ResultHandleManager is not active, reject! task: {}", task);
                throw new MessageHandlerUnavailableException();
            }
            this.scheduledTasks.put(scheduleTask.getTaskId(), scheduleTask);
        }

        if (task instanceof AbstractResultHandleTask) {
            resultHandleTaskKeepaliveManager.addRunningTaskKeepaliveInfo(task.getTaskId());
        }
        this.tasksQueue.add(scheduleTask);
        if (task instanceof ScriptResultHandleTask) {
            ScriptResultHandleTask scriptResultHandleTask = (ScriptResultHandleTask) task;
            resultHandleTaskSampler.incrementScriptTask(scriptResultHandleTask.getAppId());
        } else if (task instanceof FileResultHandleTask) {
            FileResultHandleTask fileResultHandleTask = (FileResultHandleTask) task;
            resultHandleTaskSampler.incrementFileTask(fileResultHandleTask.getAppId());
        }
    }

    DelayQueue<ScheduledContinuousResultHandleTask> getTasksQueue() {
        return this.tasksQueue;
    }

    Map<String, ScheduledContinuousResultHandleTask> getScheduledTasks() {
        synchronized (lifecycleMonitor) {
            return this.scheduledTasks;
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

    /**
     * Spring Bean 生命周期管理-启动
     */
    @Override
    public void start() {
        if (isRunning()) {
            return;
        }
        synchronized (lifecycleMonitor) {
            doStart();
        }
    }

    private void doStart() {
        this.active = true;
        this.running = true;
        initWorker();
    }

    private void initWorker() {
        synchronized (workersMonitor) {
            log.info("Init task result handle workers, initial worker num: {}", CORE_WORKERS);
            for (int workerCount = 0; workerCount < CORE_WORKERS; workerCount++) {
                TaskWorker worker = new TaskWorker();
                workers.add(worker);
                taskExecutor.execute(worker);
            }
        }
    }

    /**
     * Spring Bean 生命周期管理-停止
     */
    @Override
    public void stop() {
        log.info("ResultHandleManager stopping.");
        synchronized (this.lifecycleMonitor) {
            if (!isActive()) {
                log.info("Shutdown ignored - manager is not active already");
                return;
            }
            this.active = false;
            this.running = false;
        }
        stopTasksGraceful();
        log.info("ResultHandleManager stop successfully!");
    }

    private void stopTasksGraceful() {
        log.info("Stop tasks graceful - start");
        long start = System.currentTimeMillis();
        StopTaskCounter stopTaskCounter = StopTaskCounter.getInstance();
        synchronized (lifecycleMonitor) {
            if (!this.scheduledTasks.isEmpty()) {
                log.info("Stop result handle tasks, size: {}, tasks: {}", scheduledTasks.size(), scheduledTasks);
                stopTaskCounter.initCounter(scheduledTasks.keySet());
            }
            for (ScheduledContinuousResultHandleTask task : scheduledTasks.values()) {
                shutdownExecutorService.execute(new StopTask(task, tracing));
            }
        }
        try {
            stopTaskCounter.waitingForAllTasksDone();
        } catch (Throwable e) {
            log.error("Stop tasks caught exception", e);
        }
        long end = System.currentTimeMillis();
        log.info("Stop tasks graceful - end, cost: {}", end - start);
    }

    @Override
    public boolean isRunning() {
        synchronized (this.lifecycleMonitor) {
            return (this.running);
        }
    }

    /**
     * 消费者线程不足的情况下考虑新增一个消费者
     */
    private void considerAddingAConsumer() {
        synchronized (this.workersMonitor) {
            if (this.workers.size() < this.MAX_WORKERS) {
                long now = System.currentTimeMillis();
                if (this.lastWorkerStartedAt + this.startConsumerMinInterval < now) {
                    TaskWorker worker = new TaskWorker();
                    workers.add(worker);
                    taskExecutor.execute(worker);
                    this.lastWorkerStartedAt = now;
                    log.debug("Add new worker, worker count : {}", workers.size());
                }
            }
        }
    }

    /**
     * 消费者线程充足的情况下考虑删除一个消费者
     */
    private void considerStoppingAConsumer(TaskWorker worker) {
        synchronized (this.workersMonitor) {
            if (this.workers.size() > this.CORE_WORKERS) {
                long now = System.currentTimeMillis();
                if (this.lastWorkerStoppedAt + this.stopConsumerMinInterval < now) {
                    workers.remove(worker);
                    this.lastWorkerStoppedAt = now;
                    log.debug("Remove idle worker, worker count : {}", workers.size());
                }
            }
        }
    }

    /**
     * 任务结果处理线程正在工作的线程数
     *
     * @return 正在工作的线程数
     */
    public int getResultHandleBusyThreads() {
        int workingThreads = 0;
        synchronized (this.workersMonitor) {
            for (TaskWorker worker : workers) {
                if (worker.isBusy()) {
                    workingThreads++;
                }
            }
        }
        return workingThreads;
    }

    /**
     * 任务结果处理空闲的线程数
     *
     * @return 空闲的线程数
     */
    public int getResultHandleIdleThreads() {
        int idleThreads = 0;
        synchronized (this.workersMonitor) {
            for (TaskWorker worker : workers) {
                if (!worker.isBusy()) {
                    idleThreads++;
                }
            }
        }
        return idleThreads;
    }

    public int getResultHandleWaitingScheduleTasks() {
        return resultHandleLimiter.getWaitingThreads();
    }

    @Override
    public int getPhase() {
        return DestroyOrder.GSE_TASK_RESULT_HANDLER;
    }

    private static final class StopTask implements Runnable {
        private final ScheduledContinuousResultHandleTask task;
        private final Tracing tracing;

        StopTask(ScheduledContinuousResultHandleTask task, Tracing tracing) {
            this.task = task;
            this.tracing = tracing;
        }

        @Override
        public void run() {
            ScopedSpan span = null;
            try {
                span = tracing.tracer().startScopedSpanWithParent("stop-task", task.getTraceContext());
                log.info("Begin to stop task, task: {}", task.getResultHandleTask());
                task.getResultHandleTask().stop();
                log.info("Stop task successfully, task: {}", task.getResultHandleTask());
            } catch (Throwable e) {
                String errorMsg = "Stop task caught exception, task: {}" + task;
                log.warn(errorMsg, e);
            } finally {
                if (span != null) {
                    span.finish();
                }
            }
        }
    }

    /**
     * 任务处理worker
     */
    private final class TaskWorker implements Runnable {
        /**
         * Worker连续空闲的周期
         */
        private int consecutiveIdles;
        /**
         * Worker连续执行的任务
         */
        private int consecutiveTasks;
        /**
         * 等待从阻塞队列获取任务的超时时间，单位毫秒
         */
        private int waitingTaskTimeout = 1000;
        /**
         * Worker是否正在执行任务
         */
        private volatile boolean isBusy = false;

        @Override
        public void run() {
            if (!isActive()) {
                log.info("Manager is not active!");
                return;
            }
            while (isWorkerActive(this)) {
                loop();
            }

        }

        public void stop() {

        }

        private void loop() {
            try {
                ScheduledContinuousResultHandleTask task =
                    ResultHandleManager.this.tasksQueue.poll(this.waitingTaskTimeout, TimeUnit.MILLISECONDS);
                if (task != null) {
                    isBusy = true;
                    log.debug("Get task from queue, task: {}", task);
                    // 调度误差
                    long scheduleErrorInMills = System.currentTimeMillis() - task.getExpireTime();
                    if (scheduleErrorInMills > 1000) {
                        log.warn("Inaccurate scheduling, task: {}, errorInMills:{}", task, scheduleErrorInMills);
                        counters.getResultHandleDelayedScheduleCounter().increment();
                    }
                    try {
                        task.execute();
                    } catch (Throwable e) {
                        counters.getGseTasksExceptionCounter().increment();
                        log.warn("Task execution error", e);
                    }
                }
                checkAdjustWorker(task != null);
            } catch (InterruptedException e) {
                log.warn("Task worker is interrupted", e);
            } catch (Throwable e) {
                log.warn("Execute task caught exception", e);
            } finally {
                isBusy = false;
            }
        }

        /**
         * 调整worker线程数
         *
         * @param fetchTaskOK 是否获取到任务
         */
        private void checkAdjustWorker(boolean fetchTaskOK) {
            if (fetchTaskOK) {
                if (isWorkerActive(this)) {
                    this.consecutiveIdles = 0;
                    if (this.consecutiveTasks++ > ResultHandleManager.this.consecutiveActiveTrigger) {
                        considerAddingAConsumer();
                        this.consecutiveTasks = 0;
                    }
                }
            } else {
                this.consecutiveTasks = 0;
                if (this.consecutiveIdles++ > ResultHandleManager.this.consecutiveIdleTrigger) {
                    considerStoppingAConsumer(this);
                    this.consecutiveIdles = 0;
                }
            }
        }

        boolean isBusy() {
            return this.isBusy;
        }
    }
}
