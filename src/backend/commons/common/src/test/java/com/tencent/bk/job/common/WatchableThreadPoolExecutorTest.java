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

import com.tencent.bk.job.common.context.JobContext;
import com.tencent.bk.job.common.context.JobContextThreadLocal;
import com.tencent.bk.job.common.context.JobContextThreadLocalAccessor;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.http.HttpMetricUtil;
import io.micrometer.context.ContextRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.AbstractList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link WatchableThreadPoolExecutor} 上下文隔离回归测试。
 *
 * <p>验证子线程通过 {@code WatchableThreadPoolExecutor} 继承父线程 {@link JobContext} 时，
 * 不会与父线程及其它工作线程共享同一份 {@code metricTagsMap}，从而避免并发场景下
 * {@link HttpMetricUtil#addTagForCurrentMetric(Tag)} 抛出
 * {@link ArrayIndexOutOfBoundsException}（GitHub Issue #4333）。</p>
 */
class WatchableThreadPoolExecutorTest {

    @BeforeAll
    static void registerAccessor() {
        // 单元测试无 Spring 容器，需手动把 JobContextThreadLocalAccessor 注册到全局 ContextRegistry,
        // 否则 captureAll() 不会捕获 JobContext，无法复现并发场景。
        ContextRegistry.getInstance().registerThreadLocalAccessor(new JobContextThreadLocalAccessor());
    }

    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
    }

    @AfterEach
    void tearDown() {
        JobContextThreadLocal.unset();
    }

    /**
     * 模拟 Issue #4333 描述的场景：
     * 父线程持有 JobContext 与已初始化的 metricTagsMap，多个工作线程并发追加 metric tag。
     * 修复后期望：所有任务正常完成，父线程 metricTagsMap 不被子线程污染，且子线程内部不抛异常。
     */
    @Test
    void shouldIsolateMetricTagsMapAcrossWorkerThreads() throws Exception {
        // 父线程初始化 JobContext + 空 metricTagsMap, 模拟 web 请求处理早期已建立上下文的场景
        JobContext parent = new JobContext();
        parent.setMetricTagsMap(new HashMap<>());
        JobContextThreadLocal.set(parent);

        int workerCount = 16;
        int tagPerWorker = 500;
        WatchableThreadPoolExecutor executor = new WatchableThreadPoolExecutor(
            meterRegistry,
            "isolation-test-pool",
            workerCount,
            workerCount,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
        );

        try {
            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch doneGate = new CountDownLatch(workerCount);
            AtomicInteger successCount = new AtomicInteger();
            AtomicReference<Throwable> firstError = new AtomicReference<>();

            for (int i = 0; i < workerCount; i++) {
                final int workerIndex = i;
                executor.execute(() -> {
                    try {
                        // 所有 worker 同时起跑，最大化并发竞争压力
                        startGate.await();
                        HttpMetricUtil.setHttpMetricName("worker-metric-" + workerIndex);
                        for (int j = 0; j < tagPerWorker; j++) {
                            HttpMetricUtil.addTagForCurrentMetric(Tag.of("seq", String.valueOf(j)));
                        }
                        // 校验当前 worker 自己看到的 tag 数量是完整的, 不会因被其它线程并发修改而丢失
                        AbstractList<Tag> tags = HttpMetricUtil.getCurrentMetricTags();
                        if (tags.size() == tagPerWorker) {
                            successCount.incrementAndGet();
                        }
                    } catch (Throwable e) {
                        firstError.compareAndSet(null, e);
                    } finally {
                        HttpMetricUtil.clearHttpMetric();
                        doneGate.countDown();
                    }
                });
            }

            startGate.countDown();
            boolean finished = doneGate.await(30, TimeUnit.SECONDS);

            assertThat(finished).as("all workers finished in time").isTrue();
            assertThat(firstError.get()).as("no exception thrown in workers").isNull();
            assertThat(successCount.get())
                .as("each worker should see its own complete metric tag list")
                .isEqualTo(workerCount);

            // 关键断言：父线程的 metricTagsMap 不被任何子线程污染
            Map<String, Pair<String, AbstractList<Tag>>> parentMap = parent.getMetricTagsMap();
            assertThat(parentMap)
                .as("parent metricTagsMap must not be polluted by worker threads")
                .isEmpty();
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 验证：子线程通过 {@link JobContextUtil} 拿到的 JobContext 与父线程不是同一实例，
     * 但只读字段(如 requestId)能正确继承。
     */
    @Test
    void workerThreadShouldReceiveIsolatedCopyButInheritReadOnlyFields() throws Exception {
        JobContext parent = new JobContext();
        parent.setRequestId("req-from-parent");
        parent.setUserLang("zh-CN");
        JobContextThreadLocal.set(parent);

        WatchableThreadPoolExecutor executor = new WatchableThreadPoolExecutor(
            meterRegistry,
            "isolation-readonly-test-pool",
            1,
            1,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
        );

        try {
            CountDownLatch done = new CountDownLatch(1);
            AtomicReference<JobContext> childRef = new AtomicReference<>();
            executor.execute(() -> {
                try {
                    childRef.set(JobContextThreadLocal.get());
                } finally {
                    done.countDown();
                }
            });
            assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();

            JobContext child = childRef.get();
            assertThat(child).isNotNull();
            assertThat(child).as("worker thread should see an isolated copy").isNotSameAs(parent);
            assertThat(child.getRequestId()).isEqualTo("req-from-parent");
            assertThat(child.getUserLang()).isEqualTo("zh-CN");
            assertThat(child.getMetricTagsMap()).as("mutable collection must be reset").isNull();
        } finally {
            executor.shutdownNow();
        }
    }
}
