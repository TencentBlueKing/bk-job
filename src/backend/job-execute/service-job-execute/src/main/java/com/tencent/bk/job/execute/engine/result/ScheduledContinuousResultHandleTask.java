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

import com.tencent.bk.job.execute.engine.result.ha.ResultHandleLimiter;
import com.tencent.bk.job.execute.engine.result.ha.ResultHandleTaskKeepaliveManager;
import com.tencent.bk.job.execute.monitor.ExecuteMetricNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;

import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 通过延时队列调度的持续性任务结果处理任务
 */
@Slf4j
public class ScheduledContinuousResultHandleTask extends DelayedTask {
    /**
     * 日志调用链tracer
     */
    private final Tracer tracer;
    /**
     * 任务采样器
     */
    private final ResultHandleTaskSampler sampler;
    private final ResultHandleManager resultHandleManager;
    private final ResultHandleTaskKeepaliveManager resultHandleTaskKeepaliveManager;
    private final ResultHandleLimiter resultHandleLimiter;
    /**
     * 任务队列
     */
    private final DelayQueue<ScheduledContinuousResultHandleTask> tasksQueue;
    /**
     * 任务
     */
    private final ContinuousScheduledTask task;
    /**
     * 延时任务
     */
    private DelayedTask delayedTask;
    /**
     * 调用链父上下文
     */
    private final Span parent;

    /**
     * ScheduledContinuousQueuedTask Constructor
     *
     * @param sampler             采样器
     * @param tracer              日志调用链
     * @param task                任务
     * @param resultHandleManager resultHandleManager
     * @param resultHandleLimiter 限流
     */
    public ScheduledContinuousResultHandleTask(ResultHandleTaskSampler sampler,
                                               Tracer tracer, ContinuousScheduledTask task,
                                               ResultHandleManager resultHandleManager,
                                               ResultHandleTaskKeepaliveManager resultHandleTaskKeepaliveManager,
                                               ResultHandleLimiter resultHandleLimiter) {
        this.sampler = sampler;
        this.tracer = tracer;
        this.parent = tracer.currentSpan();
        this.task = task;
        this.delayedTask = new DelayedTask(this.task, this.task.getScheduleStrategy().getDelay());
        this.resultHandleManager = resultHandleManager;
        this.tasksQueue = resultHandleManager.getTasksQueue();
        this.resultHandleTaskKeepaliveManager = resultHandleTaskKeepaliveManager;
        this.resultHandleLimiter = resultHandleLimiter;
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
            if (!resultHandleManager.isActive()) {
                task.stop();
                return;
            }

            isExecutable = true;
            executeTask();

            if (!resultHandleManager.isActive()) {
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
                if (task instanceof ScriptResultHandleTask) {
                    ScriptResultHandleTask scriptTask = (ScriptResultHandleTask) task;
                    resultHandleTaskKeepaliveManager.stopKeepaliveInfoTask(scriptTask.getTaskId());
                    sampler.decrementScriptTask(scriptTask.getAppId());
                } else if (task instanceof FileResultHandleTask) {
                    FileResultHandleTask fileTask = (FileResultHandleTask) task;
                    resultHandleTaskKeepaliveManager.stopKeepaliveInfoTask(fileTask.getTaskId());
                    sampler.decrementFileTask(fileTask.getAppId());
                }
                resultHandleManager.getScheduledTasks().remove(task.getTaskId());
            }
            if (isExecutable) {
                long end = System.nanoTime();
                sampler.getMeterRegistry().timer(ExecuteMetricNames.RESULT_HANDLE_TASK_SCHEDULE_PREFIX,
                    "task_type", task.getTaskType(), "status", status)
                    .record(end - start, TimeUnit.NANOSECONDS);
            }
            if (!isReScheduled) {
                resultHandleLimiter.release();
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
        this.delayedTask = this.delayedTask.reScheduled(task.getScheduleStrategy().getDelay());
        log.debug("Add undone task to queue, task: {}", task);
        long start = System.currentTimeMillis();
        tasksQueue.offer(this);
        long end = System.currentTimeMillis();
        if (end - start > 10) {
            log.warn("Adding undone task to queue is slow, task: {}, cost:{}", task, end - start);
        }
    }

    public ContinuousScheduledTask getResultHandleTask() {
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
        return new StringJoiner(", ", ScheduledContinuousResultHandleTask.class.getSimpleName() + "[", "]")
            .add("delayedTask=" + delayedTask)
            .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduledContinuousResultHandleTask that = (ScheduledContinuousResultHandleTask) o;
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
