/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bk.job.execute.engine.rolling.scatter;

import com.tencent.bk.job.execute.common.context.JobExecuteContextThreadLocalRepo;
import com.tencent.bk.job.execute.common.ha.DestroyOrder;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.engine.listener.event.RollingBatchDispatchResumeEvent;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.monitor.metrics.ScatterDispatchMonitor;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 并行错峰模式批次下发调度器。
 * <p>
 * 仿 {@code ResultHandleManager}：SmartLifecycle + Java {@link DelayQueue} + 消费者线程池，
 * <b>不依赖 RabbitMQ 延迟队列</b>。到点后交由 {@link ScatterBatchDispatcher} 仅发送 MQ 事件。
 * 停机时把队列中未下发批次通过 {@link RollingBatchDispatchResumeEvent} 转移出去（其它实例按剩余延时重新入队）。
 */
@Component
@Slf4j
public class ScatterDispatchManager implements SmartLifecycle {

    /**
     * 批次下发延迟告警阈值(ms)，超过该阈值说明到点触发不准时，日志升级为 WARN
     */
    private static final long DISPATCH_DELAY_WARN_THRESHOLD_MS = 1000L;

    private final ScatterBatchDispatcher scatterBatchDispatcher;
    private final TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;
    /**
     * 日志调用链Tracer，用于在到点触发线程重建 trace 上下文，使错峰批次日志带上可追踪的 traceId
     */
    private final Tracer tracer;
    private final ScatterDispatchMonitor scatterDispatchMonitor;
    private final int workerNum;

    private final DelayQueue<ScatterDispatchTask> tasksQueue = new DelayQueue<>();
    private final Set<Worker> workers = new HashSet<>();
    private final Executor taskExecutor = new SimpleAsyncTaskExecutor("scatter-dispatch-");
    private final Object lifecycleMonitor = new Object();

    private volatile boolean active = false;
    private volatile boolean running = false;

    @Autowired
    public ScatterDispatchManager(ScatterBatchDispatcher scatterBatchDispatcher,
                                  TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
                                  Tracer tracer,
                                  ScatterDispatchMonitor scatterDispatchMonitor,
                                  JobExecuteConfig jobExecuteConfig) {
        this.scatterBatchDispatcher = scatterBatchDispatcher;
        this.taskExecuteMQEventDispatcher = taskExecuteMQEventDispatcher;
        this.tracer = tracer;
        this.scatterDispatchMonitor = scatterDispatchMonitor;
        int configuredWorkerNum = jobExecuteConfig.getRollingScatterWorkerNum();
        this.workerNum = configuredWorkerNum > 0 ? configuredWorkerNum : 3;
        // 绑定延迟队列积压数量 Gauge，反映待下发批次堆积情况
        this.scatterDispatchMonitor.registerQueueSizeGauge(tasksQueue::size);
    }

    /**
     * 提交一个批次延迟下发任务
     *
     * @param task 批次延迟下发任务
     */
    public void addTask(ScatterDispatchTask task) {
        if (!isActive()) {
            log.warn("ScatterDispatchManager is not active, reject task: {}", task);
            return;
        }
        // 在登记线程（原始请求/MQ 消费线程，携带 trace 上下文）捕获 Span 与作业执行上下文，
        // 供到点触发线程重建 trace，使错峰批次日志带上可追踪的 traceId。
        task.bindTraceContext(tracer.currentSpan(), JobExecuteContextThreadLocalRepo.get());
        log.info("Add scatter dispatch task: {}", task);
        tasksQueue.add(task);
    }

    /**
     * 取消某步骤在队列中尚未下发的批次（用于并行模式整步终止）。
     *
     * @param taskInstanceId 作业实例ID
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @return 被取消的批次列表
     */
    public List<ScatterDispatchTask> cancelStepTasks(Long taskInstanceId, long stepInstanceId, int executeCount) {
        List<ScatterDispatchTask> removed = new ArrayList<>();
        for (ScatterDispatchTask task : new ArrayList<>(tasksQueue)) {
            if (task.getStepInstanceId() == stepInstanceId
                && task.getExecuteCount() == executeCount
                && Objects.equals(task.getTaskInstanceId(), taskInstanceId)) {
                if (tasksQueue.remove(task)) {
                    removed.add(task);
                }
            }
        }
        if (!removed.isEmpty()) {
            log.info("Cancel scatter dispatch tasks, stepInstanceId={}, executeCount={}, canceled={}",
                stepInstanceId, executeCount, removed.size());
        }
        return removed;
    }

    boolean isActive() {
        synchronized (lifecycleMonitor) {
            return active;
        }
    }

    private boolean isWorkerActive(Worker worker) {
        synchronized (lifecycleMonitor) {
            return active && workers.contains(worker);
        }
    }

    @Override
    public void start() {
        synchronized (lifecycleMonitor) {
            if (running) {
                return;
            }
            this.active = true;
            this.running = true;
            log.info("ScatterDispatchManager starting, workerNum: {}", workerNum);
            for (int i = 0; i < workerNum; i++) {
                Worker worker = new Worker();
                workers.add(worker);
                taskExecutor.execute(worker);
            }
        }
    }

    @Override
    public void stop() {
        log.info("ScatterDispatchManager stopping.");
        synchronized (lifecycleMonitor) {
            if (!active) {
                return;
            }
            this.active = false;
            this.running = false;
        }
        handoffPendingTasks();
        log.info("ScatterDispatchManager stopped.");
    }

    /**
     * 停机时把未下发批次转移出去，交由其它实例竞争消费并重新入队。
     */
    private void handoffPendingTasks() {
        List<ScatterDispatchTask> pending = new ArrayList<>(tasksQueue);
        tasksQueue.clear();
        if (pending.isEmpty()) {
            return;
        }
        log.info("Handoff pending scatter dispatch tasks, size: {}", pending.size());
        for (ScatterDispatchTask task : pending) {
            handoffTaskWithTrace(task);
        }
    }

    /**
     * 转移单个未下发批次，并在原作业 trace 作用域内发出恢复事件。
     * <p>
     * {@link #stop()} 生命周期线程本身无 trace 作用域，若直接发事件则 MQ messaging 埋点无 trace 可透传，
     * 接收方将在全新的消费 trace 下 {@code addTask}，与原作业 traceId 断链、跨副本日志无法串联。
     * 因此以登记时捕获的 Span（{@link ScatterDispatchTask#getTraceContext()}）为父重建 trace 作用域后再发事件
     * （无父上下文时新建可关联的根 Span），使 {@code streamBridge.send} 经 micrometer messaging 埋点携带原 traceId；
     * 接收端 listener 便在延续原 traceId 的消费 trace 下重新入队，整条转移链路日志不断链。
     * 同时恢复 {@link JobExecuteContextThreadLocalRepo}，与 {@link #fireTaskWithTrace} 对齐；
     * start/withSpan/finally end 保证不泄漏 Span。
     *
     * @param task 待转移的未下发批次
     */
    private void handoffTaskWithTrace(ScatterDispatchTask task) {
        Span parent = task.getTraceContext();
        Span span = (parent != null ? tracer.nextSpan(parent) : tracer.nextSpan()).name("scatter-handoff");
        try (Tracer.SpanInScope ignored = tracer.withSpan(span.start())) {
            JobExecuteContextThreadLocalRepo.set(task.getJobExecuteContext());
            taskExecuteMQEventDispatcher.dispatchRollingBatchDispatchResumeEvent(
                RollingBatchDispatchResumeEvent.resume(
                    task.getTaskInstanceId(),
                    task.getStepInstanceId(),
                    task.getExecuteCount(),
                    task.getBatch(),
                    task.getDispatchTime(),
                    task.getGseTaskId()
                ));
        } catch (Throwable e) {
            span.error(e);
            log.error("Handoff scatter dispatch task failed, task: " + task, e);
        } finally {
            JobExecuteContextThreadLocalRepo.unset();
            span.end();
        }
    }

    @Override
    public boolean isRunning() {
        synchronized (lifecycleMonitor) {
            return running;
        }
    }

    @Override
    public int getPhase() {
        return DestroyOrder.ROLLING_SCATTER_DISPATCHER;
    }

    /**
     * 到点触发批次下发，并重建 trace 与作业执行上下文。
     * <p>
     * 以登记时捕获的 Span 为父新建子 Span（转移重建的任务无父上下文时新建可关联的根 Span），
     * 使本批次「fired / 下发 GseTaskEvent」等日志带上 traceId；同时恢复 {@link JobExecuteContextThreadLocalRepo}，
     * 保证随后 dispatch 的 GseTaskEvent 经 MQ 透传 trace 与作业执行上下文，下游 GseTaskListener 消费得以延续 trace。
     *
     * @param task 到点的批次下发任务
     */
    private void fireTaskWithTrace(ScatterDispatchTask task) {
        Span parent = task.getTraceContext();
        Span span = (parent != null ? tracer.nextSpan(parent) : tracer.nextSpan()).name("scatter-dispatch");
        try (Tracer.SpanInScope ignored = tracer.withSpan(span.start())) {
            JobExecuteContextThreadLocalRepo.set(task.getJobExecuteContext());
            // 记录批次下发延迟：实际下发时刻 - 计划下发时刻，反映到点触发准时性与队列调度压力
            long dispatchDelayMs = System.currentTimeMillis() - task.getDispatchTime();
            scatterDispatchMonitor.recordDispatchDelay(dispatchDelayMs);
            // 延迟超过阈值说明到点触发不准时（队列积压/worker 繁忙），升级为 WARN 以便告警观测
            if (dispatchDelayMs > DISPATCH_DELAY_WARN_THRESHOLD_MS) {
                log.warn("Scatter dispatch task fired with high delay: {}ms, task: {}", dispatchDelayMs, task);
            } else {
                log.info("Scatter dispatch task fired, delay: {}ms, task: {}", dispatchDelayMs, task);
            }
            scatterBatchDispatcher.dispatchBatch(
                task.getTaskInstanceId(),
                task.getStepInstanceId(),
                task.getExecuteCount(),
                task.getBatch(),
                task.getGseTaskId());
        } catch (Throwable e) {
            span.error(e);
            log.error("Scatter dispatch batch error, task: " + task, e);
        } finally {
            JobExecuteContextThreadLocalRepo.unset();
            span.end();
        }
    }

    private final class Worker implements Runnable {
        @Override
        public void run() {
            while (isWorkerActive(this)) {
                try {
                    ScatterDispatchTask task = tasksQueue.poll(1000, TimeUnit.MILLISECONDS);
                    if (task == null) {
                        continue;
                    }
                    fireTaskWithTrace(task);
                } catch (InterruptedException e) {
                    log.warn("Scatter dispatch worker interrupted", e);
                    Thread.currentThread().interrupt();
                    break;
                } catch (Throwable e) {
                    log.warn("Scatter dispatch worker caught exception", e);
                }
            }
        }
    }
}
