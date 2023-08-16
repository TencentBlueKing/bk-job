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

package com.tencent.bk.job.execute.engine.schedule;

import com.tencent.bk.job.execute.common.exception.MessageHandlerUnavailableException;
import com.tencent.bk.job.execute.common.ha.DestroyOrder;
import com.tencent.bk.job.execute.engine.schedule.ha.ScheduleTaskKeepaliveManager;
import com.tencent.bk.job.execute.engine.schedule.ha.ScheduleTaskLimiter;
import com.tencent.bk.job.execute.engine.schedule.metrics.DelayedScheduleTaskCounter;
import com.tencent.bk.job.execute.engine.schedule.metrics.ExceptionScheduleTasksCounter;
import com.tencent.bk.job.execute.engine.schedule.metrics.ScheduleTaskGauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 任务调度管理。
 * <p>
 * 方案：java DelayQueue 延迟队列 + 消费者模式实现任务的延迟调度
 */
@Slf4j
public class ScheduleTaskManager implements SmartLifecycle {
    /**
     * 任务调度管理器名称
     */
    private final String schedulerName;
    /**
     * 日志调用链Tracer
     */
    private final Tracer tracer;
    private final MeterRegistry meterRegistry;

    /**
     * 任务存活管理
     */
    private final ScheduleTaskKeepaliveManager scheduleTaskKeepaliveManager;
    private final Object workersMonitor = new Object();
    private final Object lifecycleMonitor = new Object();
    /**
     * 任务调度引擎限流
     */
    private final ScheduleTaskLimiter scheduleTaskLimiter;
    /**
     * 任务结果处理的任务队列
     */
    private final DelayQueue<ScheduledDelayedTask> tasksQueue = new DelayQueue<>();
    /**
     * 调度的所有的任务
     */
    private final Map<String, ScheduledDelayedTask> scheduledTasks = new ConcurrentHashMap<>();
    /**
     * 任务消费者
     */
    private final Set<TaskWorker> workers = new HashSet<>();
    /**
     * 异步任务执行器，用于启动消费者线程
     */
    private final Executor taskExecutor;
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
     * 最近一次worker停止时间
     */
    private volatile long lastWorkerStoppedAt;
    /**
     * 任务结果处理引擎是否活动状态
     */
    private volatile boolean active = false;
    /**
     * whether this component is currently running(Spring Lifecycle isRunning method)
     */
    private volatile boolean running = false;
    private final ExecutorService shutdownExecutor;

    private DelayedScheduleTaskCounter delayedScheduleTaskCounter;
    private ExceptionScheduleTasksCounter exceptionScheduleTasksCounter;

    public ScheduleTaskManager(String schedulerName,
                               Tracer tracer,
                               MeterRegistry meterRegistry,
                               ScheduleTaskKeepaliveManager scheduleTaskKeepaliveManager,
                               ExecutorService shutdownExecutor,
                               int maxTaskSize) {
        this.schedulerName = schedulerName;
        this.tracer = tracer;
        this.meterRegistry = meterRegistry;
        this.scheduleTaskKeepaliveManager = scheduleTaskKeepaliveManager;
        this.scheduleTaskLimiter = new ScheduleTaskLimiter(maxTaskSize);
        this.shutdownExecutor = shutdownExecutor;
        this.taskExecutor = new SimpleAsyncTaskExecutor(schedulerName);
        initMetric();
    }

    private void initMetric() {
        new ScheduleTaskGauge(meterRegistry, this);
        this.delayedScheduleTaskCounter = new DelayedScheduleTaskCounter(meterRegistry, schedulerName);
        this.exceptionScheduleTasksCounter = new ExceptionScheduleTasksCounter(meterRegistry, schedulerName);
    }

    /**
     * 处理任务
     *
     * @param task 任务
     */
    public void handleDeliveredTask(ContinuousScheduleTask task) {
        scheduleTaskLimiter.acquire();
        log.info("Handle delivered task: {}", task);
        ScheduledDelayedTask scheduleTask =
            new ScheduledDelayedTask(tracer, task, this,
                scheduleTaskKeepaliveManager, scheduleTaskLimiter, meterRegistry);
        synchronized (lifecycleMonitor) {
            if (!isActive()) {
                log.warn("ScheduleTaskManager is not active, reject! task: {}", task);
                throw new MessageHandlerUnavailableException();
            }
            this.scheduledTasks.put(scheduleTask.getTaskId(), scheduleTask);
        }

        scheduleTaskKeepaliveManager.addRunningTaskKeepaliveInfo(task.getTaskId());
        this.tasksQueue.add(scheduleTask);
        // 触发任务被调度引擎接受回调
        task.onAccept();
    }

    DelayQueue<ScheduledDelayedTask> getTasksQueue() {
        return this.tasksQueue;
    }

    Map<String, ScheduledDelayedTask> getScheduledTasks() {
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
            log.info("Init schedule workers, initial worker num: {}", CORE_WORKERS);
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
        log.info("ScheduleTaskManager stopping.");
        synchronized (this.lifecycleMonitor) {
            if (!isActive()) {
                log.info("Shutdown ignored - manager is not active already");
                return;
            }
            this.active = false;
            this.running = false;
        }
        stopTasksGraceful();
        log.info("ScheduleTaskManager stop successfully!");
    }

    private void stopTasksGraceful() {
        log.info("Stop tasks graceful - start");
        long start = System.currentTimeMillis();
        StopTaskCounter stopTaskCounter = null;
        synchronized (lifecycleMonitor) {
            if (!this.scheduledTasks.isEmpty()) {
                log.info("Stop schedule tasks, size: {}, tasks: {}", scheduledTasks.size(), scheduledTasks);
                stopTaskCounter = StopTaskCounter.getInstance();
                stopTaskCounter.initCounter(scheduledTasks.keySet());
            }
            for (ScheduledDelayedTask task : scheduledTasks.values()) {
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
                // 新增消费者线程最小间隔时间
                long startConsumerMinInterval = 10000;
                if (this.lastWorkerStartedAt + startConsumerMinInterval < now) {
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
                // 停止消费者线程最小间隔时间
                long stopConsumerMinInterval = 60000;
                if (this.lastWorkerStoppedAt + stopConsumerMinInterval < now) {
                    workers.remove(worker);
                    this.lastWorkerStoppedAt = now;
                    log.debug("Remove idle worker, worker count : {}", workers.size());
                }
            }
        }
    }

    /**
     * 正在工作的Worker数
     */
    public int getBusyWorkers() {
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
     * 空闲的Worker数
     */
    public int getIdleWorkers() {
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

    public int getWaitingScheduleTasks() {
        return scheduleTaskLimiter.getWaitingThreads();
    }

    @Override
    public int getPhase() {
        return DestroyOrder.JOB_TASK_SCHEDULER;
    }

    private static final class StopTask implements Runnable {
        private final ScheduledDelayedTask task;
        private final Tracer tracer;

        StopTask(ScheduledDelayedTask task, Tracer tracer) {
            this.task = task;
            this.tracer = tracer;
        }

        @Override
        public void run() {
            Span span = null;
            try {
                span = tracer.nextSpan(task.getTraceContext()).name("stop-task");
                log.info("Begin to stop task, task: {}", task.getScheduleTask());
                task.getScheduleTask().stop();
                log.info("Stop task successfully, task: {}", task.getScheduleTask());
            } catch (Throwable e) {
                String errorMsg = "Stop task caught exception, task: {}" + task;
                log.warn(errorMsg, e);
            } finally {
                if (span != null) {
                    span.end();
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
                ScheduledDelayedTask task =
                    ScheduleTaskManager.this.tasksQueue.poll(this.waitingTaskTimeout, TimeUnit.MILLISECONDS);
                if (task != null) {
                    isBusy = true;
                    log.debug("Get task from queue, task: {}", task);
                    // 调度误差
                    long scheduleErrorInMills = System.currentTimeMillis() - task.getExpireTime();
                    if (scheduleErrorInMills > 1000) {
                        log.warn("Delay scheduling, task: {}, errorInMills:{}", task, scheduleErrorInMills);
                        delayedScheduleTaskCounter.increment();
                    }
                    try {
                        task.execute();
                    } catch (Throwable e) {
                        exceptionScheduleTasksCounter.increment();
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
                    if (this.consecutiveTasks++ > ScheduleTaskManager.this.consecutiveActiveTrigger) {
                        considerAddingAConsumer();
                        this.consecutiveTasks = 0;
                    }
                }
            } else {
                this.consecutiveTasks = 0;
                if (this.consecutiveIdles++ > ScheduleTaskManager.this.consecutiveIdleTrigger) {
                    considerStoppingAConsumer(this);
                    this.consecutiveIdles = 0;
                }
            }
        }

        boolean isBusy() {
            return this.isBusy;
        }
    }

    public String getSchedulerName() {
        return schedulerName;
    }
}
