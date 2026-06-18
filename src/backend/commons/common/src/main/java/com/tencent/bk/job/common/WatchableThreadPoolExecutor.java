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

package com.tencent.bk.job.common;

import com.tencent.bk.job.common.util.JobContextUtil;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 带监控指标的线程池。
 *
 * <p>提交到该线程池的任务会自动捕获提交线程的 {@link io.micrometer.context.ThreadLocalAccessor}
 * 注册的 ThreadLocal（包括 trace 上下文与业务上下文），并在工作线程执行时恢复，
 * 实现日志 traceId 跨线程传播。同时为每个任务在工作线程上新开一个 child span，
 * 让异步任务在 trace 树中拥有独立分支（类比 MQ producer/consumer 跨边界开新 span），
 * 便于独立观测耗时与异常。</p>
 *
 * <p>关闭策略：</p>
 * <ul>
 *   <li>使用带 {@code propagateContext=false} 参数的构造函数：完全跳过上下文传播与 span 创建</li>
 *   <li>容器中没有 {@link Tracer} bean：上下文照常传，但不创建 child span</li>
 * </ul>
 */
@Slf4j
public class WatchableThreadPoolExecutor extends ThreadPoolExecutor {

    private static final ContextSnapshotFactory CONTEXT_SNAPSHOT_FACTORY =
        ContextSnapshotFactory.builder().build();

    /**
     * 由 Spring 容器在启动阶段通过 {@link #setTracer(Tracer)} 注入，
     * 用于在工作线程上为每个任务创建 child span。
     *
     * <p>使用静态字段是因为 {@code WatchableThreadPoolExecutor} 通常作为工具类被
     * {@code @Bean} 方法直接 {@code new}，不便通过依赖注入获得 {@code Tracer}。
     * 静态注入只在容器启动一次，运行期可见。若 Tracer 尚未注入（如启动早期或单元测试），
     * 任务仍能正常执行，仅跳过 child span 创建。</p>
     */
    private static volatile Tracer tracer;

    /**
     * 任务开始时间
     */
    private final ThreadLocal<Long> startTimeThreadLocal = new ThreadLocal<>();

    private MeterRegistry meterRegistry;

    private List<Tag> tags;

    /**
     * 线程池名称，用作 child span 名前缀，便于 trace 树识别来源。
     */
    private final String poolName;

    /**
     * 是否在提交任务时自动捕获并向工作线程传播上下文（trace + 业务），
     * 同时控制是否在工作线程上创建 child span。
     */
    private final boolean propagateContext;

    /**
     * 注入全局 {@link Tracer}，由 {@code JobCommonAutoConfiguration} 在容器启动时调用一次。
     */
    public static void setTracer(Tracer tracer) {
        WatchableThreadPoolExecutor.tracer = tracer;
    }

    public WatchableThreadPoolExecutor(MeterRegistry meterRegistry,
                                       String poolName,
                                       int corePoolSize,
                                       int maximumPoolSize,
                                       long keepAliveTime,
                                       TimeUnit unit,
                                       BlockingQueue<Runnable> workQueue) {
        this(meterRegistry, poolName, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, true);
    }

    public WatchableThreadPoolExecutor(MeterRegistry meterRegistry,
                                       String poolName,
                                       int corePoolSize,
                                       int maximumPoolSize,
                                       long keepAliveTime,
                                       TimeUnit unit,
                                       BlockingQueue<Runnable> workQueue,
                                       boolean propagateContext) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        this.poolName = poolName;
        this.propagateContext = propagateContext;
        init(poolName, meterRegistry);
    }

    public WatchableThreadPoolExecutor(MeterRegistry meterRegistry,
                                       String poolName,
                                       int corePoolSize,
                                       int maximumPoolSize,
                                       long keepAliveTime,
                                       TimeUnit unit,
                                       BlockingQueue<Runnable> workQueue,
                                       ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        this.poolName = poolName;
        this.propagateContext = true;
        init(poolName, meterRegistry);
    }

    public WatchableThreadPoolExecutor(MeterRegistry meterRegistry,
                                       String poolName,
                                       int corePoolSize,
                                       int maximumPoolSize,
                                       long keepAliveTime,
                                       TimeUnit unit,
                                       BlockingQueue<Runnable> workQueue,
                                       RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
        this.poolName = poolName;
        this.propagateContext = true;
        init(poolName, meterRegistry);
    }

    public WatchableThreadPoolExecutor(MeterRegistry meterRegistry,
                                       String poolName,
                                       int corePoolSize,
                                       int maximumPoolSize,
                                       long keepAliveTime,
                                       TimeUnit unit,
                                       BlockingQueue<Runnable> workQueue,
                                       ThreadFactory threadFactory,
                                       RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        this.poolName = poolName;
        this.propagateContext = true;
        init(poolName, meterRegistry);
    }

    public WatchableThreadPoolExecutor(MeterRegistry meterRegistry,
                                       String poolName,
                                       int corePoolSize,
                                       int maximumPoolSize,
                                       boolean allowCoreThreadTimeOut,
                                       long keepAliveTime,
                                       TimeUnit unit,
                                       BlockingQueue<Runnable> workQueue,
                                       ThreadFactory threadFactory,
                                       RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        this.poolName = poolName;
        this.propagateContext = true;
        allowCoreThreadTimeOut(allowCoreThreadTimeOut);
        init(poolName, meterRegistry);
    }

    private void init(String poolName, MeterRegistry meterRegistry) {
        this.tags = Collections.singletonList(Tag.of("pool_name", poolName));
        this.meterRegistry = meterRegistry;
        startWatch();
    }

    private void startWatch() {
        meterRegistry.gauge("job_thread_pool_active_thread_size",
            getTags(),
            this,
            threadPoolExecutor -> (double) threadPoolExecutor.getActiveCount());
        meterRegistry.gauge("job_thread_pool_pool_size",
            getTags(),
            this,
            threadPoolExecutor -> (double) threadPoolExecutor.getPoolSize());
        meterRegistry.gauge("job_thread_pool_core_pool_size",
            getTags(),
            this,
            threadPoolExecutor -> (double) threadPoolExecutor.getCorePoolSize());
        meterRegistry.gauge("job_thread_pool_max_pool_size",
            getTags(),
            this,
            threadPoolExecutor -> (double) threadPoolExecutor.getMaximumPoolSize());
        meterRegistry.gauge("job_thread_pool_task_count",
            getTags(),
            this,
            threadPoolExecutor -> (double) threadPoolExecutor.getTaskCount());
        meterRegistry.gauge("job_thread_pool_completed_task_count",
            getTags(),
            this,
            threadPoolExecutor -> (double) threadPoolExecutor.getCompletedTaskCount());
        meterRegistry.gauge("job_thread_pool_largest_pool_size",
            getTags(),
            this,
            threadPoolExecutor -> (double) threadPoolExecutor.getLargestPoolSize());
        meterRegistry.gauge("job_thread_pool_queue_size",
            getTags(),
            this,
            threadPoolExecutor -> (double) (threadPoolExecutor.getQueue() == null ? 0 :
                threadPoolExecutor.getQueue().size()));
    }

    private List<Tag> getTags() {
        return tags;
    }

    /**
     * 提交任务时捕获提交线程的上下文（trace + 业务），并在工作线程执行任务时恢复，
     * 让 {@code submit} / {@code invokeAll} / {@code invokeAny} 等所有入口统一受益。
     *
     * <p>同时为每个任务在工作线程上新开一个 child span（跨边界开 span），
     * 让异步任务在 trace 树中拥有独立的 spanId 与耗时统计。</p>
     */
    @Override
    public void execute(Runnable command) {
        if (!propagateContext) {
            super.execute(command);
            return;
        }
        ContextSnapshot snapshot = CONTEXT_SNAPSHOT_FACTORY.captureAll();
        super.execute(() -> runWithContext(command, snapshot));
    }

    /**
     * 工作线程上执行 task 的实际逻辑：先恢复上下文，再开 child span 包裹 task。
     */
    private void runWithContext(Runnable command, ContextSnapshot snapshot) {
        try (ContextSnapshot.Scope ignored = snapshot.setThreadLocals()) {
            // 工作线程在恢复父线程上下文后，立即把 JobContext 替换为隔离副本，
            // 避免多个工作线程并发读写父线程同一份 JobContext 内的可变集合
            // (如 metricTagsMap 中的 ArrayList) 而抛出 ArrayIndexOutOfBoundsException。
            // Scope.close() 时会自动还原父线程原 JobContext，无需手动清理。
            JobContextUtil.isolateContextForChildThread();
            Tracer t = tracer;
            // tracer 未注入，或当前线程没有父 span（例如启动初始化任务），不创建 child span
            if (t == null || t.currentSpan() == null) {
                command.run();
                return;
            }
            Span childSpan = t.nextSpan().name(spanName()).start();
            try (Tracer.SpanInScope ignoredScope = t.withSpan(childSpan)) {
                command.run();
            } catch (Throwable e) {
                childSpan.error(e);
                throw e;
            } finally {
                childSpan.end();
            }
        }
    }

    /**
     * 异步任务的 span 名，便于 trace 系统按线程池区分异步分支。
     */
    private String spanName() {
        return "async-" + poolName;
    }

    /**
     * 任务执行之前，记录任务开始时间
     */
    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        startTimeThreadLocal.set(System.currentTimeMillis());
    }

    /**
     * 任务执行之后，计算任务结束时间
     */
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        try {
            long costTime = System.currentTimeMillis() - startTimeThreadLocal.get();
            recordTaskCostTime(costTime);
        } finally {
            startTimeThreadLocal.remove();
        }
    }


    private void recordTaskCostTime(long costInMills) {
        try {
            Timer.builder("job_thread_pool_tasks")
                .tags(getTags())
                .publishPercentileHistogram(true)
                .register(meterRegistry)
                .record(costInMills, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("Fail to record thread pool task timer metrics", e);
        }
    }

}
