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

package com.tencent.bk.job.common.retry.circuitbreaker;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CountBasedSlidingWindow 性能测试
 * 用于测试在高并发场景下的性能表现
 */
public class CountBasedSlidingWindowPerformanceTest {

    /**
     * 测试不同并发级别下的性能
     */
    @Test
    public void testConcurrentPerformance() throws InterruptedException {
        // 测试完成后仅保留关键测试值，加快构建时单元测试速度
        int[] windowSizeArr = {5000};
        int[] threadCounts = {1000};
        int callsPerThread = 15000;
        for (int windowSize : windowSizeArr) {
            for (int threadCount : threadCounts) {
                testMixedReadWrite(windowSize, threadCount, threadCount, callsPerThread);
            }
        }
    }

    /**
     * 压力测试：混合读写场景
     */
    public void testMixedReadWrite(int windowSize,
                                   int writerThreads,
                                   int readerThreads,
                                   int callsPerThread) throws InterruptedException {
        CountBasedSlidingWindow window = new CountBasedSlidingWindow(windowSize);
        ExecutorService executor = Executors.newFixedThreadPool(writerThreads + readerThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(writerThreads + readerThreads);

        // 写入线程
        for (int i = 0; i < writerThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < callsPerThread; j++) {
                        if (j % 3 == 0) {
                            window.recordSuccess(100, false);
                        } else if (j % 3 == 1) {
                            window.recordSuccess(1000, true);
                        } else {
                            window.recordFailure(200);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 读取线程
        for (int i = 0; i < readerThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 10000; j++) {
                        window.getMetrics();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        long startTime = System.nanoTime();
        startLatch.countDown();
        endLatch.await();
        long endTime = System.nanoTime();

        long totalTimeMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        SlidingWindowMetrics finalMetrics = window.getMetrics();
        double qps = callsPerThread / (totalTimeMs / 1000.0);

        System.out.printf(
            "混合读写压力测试 - " +
                "写入线程: %d, " +
                "读取线程: %d, " +
                "总耗时: %dms, " +
                "成功调用: %d, " +
                "失败调用: %d, " +
                "总调用数: %d, " +
                "QPS: %f/s%n",
            writerThreads,
            readerThreads,
            totalTimeMs,
            finalMetrics.getSuccessCalls(),
            finalMetrics.getFailureCalls(),
            finalMetrics.getTotalCalls(),
            qps
        );

        // 数据正确
        assertThat(finalMetrics.getTotalCalls()).isEqualTo(windowSize);
        // QPS>2K
        assertThat(qps).isGreaterThan(2000);
        executor.shutdown();
    }
}
