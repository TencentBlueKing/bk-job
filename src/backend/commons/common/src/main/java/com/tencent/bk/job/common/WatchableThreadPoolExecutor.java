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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WatchableThreadPoolExecutor extends ThreadPoolExecutor {
    /**
     * 任务开始时间
     */
    private final ThreadLocal<Long> startTimeThreadLocal = new ThreadLocal<>();

    private MeterRegistry meterRegistry;

    private List<Tag> tags;

    public WatchableThreadPoolExecutor(MeterRegistry meterRegistry,
                                       String poolName,
                                       int corePoolSize,
                                       int maximumPoolSize,
                                       long keepAliveTime,
                                       TimeUnit unit,
                                       BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
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
        meterRegistry.gauge("job_thread_pool_task_total",
            getTags(),
            this,
            threadPoolExecutor -> (double) threadPoolExecutor.getTaskCount());
        meterRegistry.gauge("job_thread_pool_completed_task_total",
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
