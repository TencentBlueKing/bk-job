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

import com.tencent.bk.job.execute.engine.schedule.ha.ScheduleTaskKeepaliveManager;
import com.tencent.bk.job.execute.engine.schedule.ha.ScheduleTaskLimiter;
import com.tencent.bk.job.execute.engine.schedule.metrics.ScheduleMetricNames;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;

import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 通过延时队列调度的任务
 */
@Slf4j
public class ScheduledDelayedTask extends DelayedTask {
    /**
     * 日志调用链tracer
     */
    private final Tracer tracer;
    private final ScheduleTaskManager scheduleTaskManager;
    private final ScheduleTaskKeepaliveManager scheduleTaskKeepaliveManager;
    private final ScheduleTaskLimiter scheduleTaskLimiter;
    /**
     * 任务队列
     */
    private final DelayQueue<ScheduledDelayedTask> tasksQueue;
    /**
     * 任务
     */
    private final ContinuousScheduleTask task;
    /**
     * 延时任务
     */
    private DelayedTask delayedTask;
    /**
     * 调用链父上下文
     */
    private final Span parent;

    private final MeterRegistry meterRegistry;

    /**
     * Constructor
     *
     * @param tracer                       日志调用链
     * @param task                         任务
     * @param scheduleTaskManager          任务调度管理
     * @param scheduleTaskKeepaliveManager scheduleTaskKeepaliveManager
     * @param scheduleTaskLimiter          限流
     * @param meterRegistry                meterRegistry
     */
    public ScheduledDelayedTask(Tracer tracer,
                                ContinuousScheduleTask task,
                                ScheduleTaskManager scheduleTaskManager,
                                ScheduleTaskKeepaliveManager scheduleTaskKeepaliveManager,
                                ScheduleTaskLimiter scheduleTaskLimiter,
                                MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.tracer = tracer;
        this.parent = tracer.currentSpan();
        this.task = task;
        this.delayedTask = new DelayedTask(this.task, this.task.getScheduleDelayStrategy().getNextDelay());
        this.scheduleTaskManager = scheduleTaskManager;
        this.tasksQueue = scheduleTaskManager.getTasksQueue();
        this.scheduleTaskKeepaliveManager = scheduleTaskKeepaliveManager;
        this.scheduleTaskLimiter = scheduleTaskLimiter;
    }

    private Span getChildSpan() {
        return this.tracer.nextSpan(parent).name("execute-task");
    }

    @Override
    public void execute() {
        Span span = getChildSpan();
        try (Tracer.SpanInScope ignored = this.tracer.withSpan(span.start())) {
            doExecute();
        } catch (Exception e) {
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }

    public void doExecute() {
        // 任务是否执行完成
        boolean isDone = false;
        // 任务是否是可执行的
        boolean isExecutable = false;
        // 任务是否需要重新被引擎调度
        boolean isReScheduled = false;
        long start = System.nanoTime();
        String status = "ok";
        try {
            if (!scheduleTaskManager.isActive()) {
                task.stop();
                return;
            }

            isExecutable = true;
            executeTask();

            if (!scheduleTaskManager.isActive()) {
                task.stop();
                return;
            }

            if (!task.isFinished()) {
                // 如果任务未完成，重新放入延时队列，等待重新调度
                reScheduleTask();
                isReScheduled = true;
            } else {
                isDone = true;
            }
        } catch (Throwable e) {
            status = "error";
            isDone = true;
            throw e;
        } finally {
            if (isDone) {
                scheduleTaskKeepaliveManager.stopKeepaliveInfoTask(task.getTaskId());
                scheduleTaskManager.getScheduledTasks().remove(task.getTaskId());
                // 触发任务结束回调
                task.onFinish();
            }
            if (isExecutable) {
                long end = System.nanoTime();
                meterRegistry.timer(ScheduleMetricNames.JOB_SCHEDULE_TASKS_HISTOGRAM,
                    "task_type", task.getTaskType(), "status", status)
                    .record(end - start, TimeUnit.NANOSECONDS);
            }
            if (!isReScheduled) {
                scheduleTaskLimiter.release();
            }
        }
    }

    private void executeTask() {
        long executeStart = System.currentTimeMillis();
        task.execute();
        long executeCost = System.currentTimeMillis() - executeStart;
        if (executeCost > 2000) {
            log.warn("Result handle task execution is slow, task: {}, cost: {}", task, executeCost);
        }
    }

    private void reScheduleTask() {
        // 如果任务未完成，重新放入延时队列，等待重新调度
        this.delayedTask = this.delayedTask.reScheduled(task.getScheduleDelayStrategy().getNextDelay());
        log.debug("Add undone task to queue, task: {}", task);
        long start = System.currentTimeMillis();
        tasksQueue.offer(this);
        long end = System.currentTimeMillis();
        if (end - start > 10) {
            log.warn("Adding undone task to queue is slow, task: {}, cost:{}", task, end - start);
        }
    }

    public ContinuousScheduleTask getScheduleTask() {
        return this.task;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return this.delayedTask.getDelay(unit);
    }

    @Override
    public int compareTo(Delayed o) {
        return this.delayedTask.compareTo(o);
    }

    @Override
    public long getExpireTime() {
        return this.delayedTask.getExpireTime();
    }

    public Span getTraceContext() {
        return this.parent;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ScheduledDelayedTask.class.getSimpleName() + "[", "]")
            .add("delayedTask=" + delayedTask)
            .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduledDelayedTask that = (ScheduledDelayedTask) o;
        return task.getTaskId().equals(that.task.getTaskId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(task.getTaskId());
    }

    @Override
    public String getTaskId() {
        return this.task.getTaskId();
    }
}
