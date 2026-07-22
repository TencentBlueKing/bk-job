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

package com.tencent.bk.job.execute.engine.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TaskContext} 单元测试。
 * <p>
 * 覆盖 Issue #4368：结果处理任务对作业存活心跳的释放需严格幂等——同一任务的正常完成路径与优雅停机转移路径
 * 可能并发触发 stop，借助 markKeepaliveStopped 保证仅首次生效，避免引用计数被过度递减。
 */
class TaskContextTest {

    @Test
    @DisplayName("markKeepaliveStopped 首次返回 true，重复调用返回 false")
    void markKeepaliveStopped_shouldBeIdempotent() {
        TaskContext taskContext = new TaskContext(1L);

        assertThat(taskContext.markKeepaliveStopped()).isTrue();
        assertThat(taskContext.markKeepaliveStopped()).isFalse();
        assertThat(taskContext.markKeepaliveStopped()).isFalse();
    }

    @Test
    @DisplayName("并发调用 markKeepaliveStopped 仅有一个线程胜出")
    void markKeepaliveStopped_shouldAllowOnlyOneWinnerUnderConcurrency() throws InterruptedException {
        TaskContext taskContext = new TaskContext(1L);
        int threadCount = 64;
        AtomicInteger winnerCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        try {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        if (taskContext.markKeepaliveStopped()) {
                            winnerCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
            startLatch.countDown();
            assertThat(doneLatch.await(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        assertThat(winnerCount.get()).isEqualTo(1);
    }
}
