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

package com.tencent.bk.job.execute.engine.quota.limit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link RunningJobKeepaliveManager} 单元测试。
 * <p>
 * 覆盖 Issue #4368 并行错峰模式下作业存活心跳按 jobInstanceId 引用计数的语义：
 * <ul>
 *     <li>add 两次 + stop 一次仍保留（计数 1，末批未结束不停刷新）；</li>
 *     <li>再 stop 一次归 0 才真正移除；</li>
 *     <li>对不存在 key 的 stop 不产生负计数、不创建 entry；</li>
 *     <li>大量并发 add/stop 收敛到 0（引用计数全程不为负）。</li>
 * </ul>
 */
class RunningJobKeepaliveManagerTest {

    private RunningJobKeepaliveManager manager;

    @BeforeEach
    void setUp() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        // 提供字符串序列化器，保证 updateJobLatestAliveTimestamp 不抛异常
        when(redisTemplate.getStringSerializer()).thenReturn(RedisSerializer.string());
        // execute(RedisCallback) 默认返回 null 且不真正连接 redis，规避外部依赖
        when(redisTemplate.execute(any(org.springframework.data.redis.core.RedisCallback.class))).thenReturn(null);
        manager = new RunningJobKeepaliveManager(redisTemplate);
    }

    @Test
    @DisplayName("并行错峰：add 两次 + stop 一次，心跳仍保留（计数 1）")
    void addTwiceStopOnce_shouldKeepKeepaliveWithRefCountOne() {
        long jobInstanceId = 100L;

        manager.addKeepaliveTask(jobInstanceId);
        manager.addKeepaliveTask(jobInstanceId);
        assertThat(manager.getRunningJobKeepaliveTaskCount()).isEqualTo(1);
        assertThat(manager.getRefCount(jobInstanceId)).isEqualTo(2);

        manager.stopKeepaliveTask(jobInstanceId);
        // 中途批次完成：计数递减但仍 > 0，心跳保留、继续刷新
        assertThat(manager.getRunningJobKeepaliveTaskCount()).isEqualTo(1);
        assertThat(manager.getRefCount(jobInstanceId)).isEqualTo(1);
    }

    @Test
    @DisplayName("并行错峰：计数归 0 才真正移除心跳")
    void stopUntilZero_shouldRemoveKeepalive() {
        long jobInstanceId = 100L;

        manager.addKeepaliveTask(jobInstanceId);
        manager.addKeepaliveTask(jobInstanceId);
        manager.stopKeepaliveTask(jobInstanceId);
        assertThat(manager.getRefCount(jobInstanceId)).isEqualTo(1);

        // 末批结束：计数归 0，真正移除并停止刷新
        manager.stopKeepaliveTask(jobInstanceId);
        assertThat(manager.getRunningJobKeepaliveTaskCount()).isEqualTo(0);
        assertThat(manager.getRefCount(jobInstanceId)).isEqualTo(0);
    }

    @Test
    @DisplayName("串行/非滚动向后兼容：add 一次 + stop 一次严格 1:1，归 0 移除")
    void addOnceStopOnce_shouldBehaveAsBefore() {
        long jobInstanceId = 200L;

        manager.addKeepaliveTask(jobInstanceId);
        assertThat(manager.getRefCount(jobInstanceId)).isEqualTo(1);
        assertThat(manager.getRunningJobKeepaliveTaskCount()).isEqualTo(1);

        manager.stopKeepaliveTask(jobInstanceId);
        assertThat(manager.getRefCount(jobInstanceId)).isEqualTo(0);
        assertThat(manager.getRunningJobKeepaliveTaskCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("对不存在 key 的 stop 不产生负计数、不创建 entry")
    void stopNonExistent_shouldNotCreateNegativeCount() {
        long jobInstanceId = 999L;

        manager.stopKeepaliveTask(jobInstanceId);
        assertThat(manager.getRunningJobKeepaliveTaskCount()).isEqualTo(0);
        assertThat(manager.getRefCount(jobInstanceId)).isEqualTo(0);

        // 归 0 移除后再次 stop（真正的重复 stop）同样不产生负计数
        manager.addKeepaliveTask(jobInstanceId);
        manager.stopKeepaliveTask(jobInstanceId);
        manager.stopKeepaliveTask(jobInstanceId);
        assertThat(manager.getRunningJobKeepaliveTaskCount()).isEqualTo(0);
        assertThat(manager.getRefCount(jobInstanceId)).isEqualTo(0);
    }

    @Test
    @DisplayName("并发 add/stop 收敛到 0，引用计数全程不为负")
    void concurrentAddStop_shouldConvergeToZero() throws InterruptedException {
        long jobInstanceId = 300L;
        int threadCount = 64;
        int loopPerThread = 200;

        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        try {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < loopPerThread; j++) {
                            // 每个线程严格先 add 后 stop，净配对 1:1
                            manager.addKeepaliveTask(jobInstanceId);
                            manager.stopKeepaliveTask(jobInstanceId);
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

        // add 与 stop 总数相等且每个 stop 前必有对应 add，最终收敛到 0 且被移除
        assertThat(manager.getRefCount(jobInstanceId)).isEqualTo(0);
        assertThat(manager.getRunningJobKeepaliveTaskCount()).isEqualTo(0);
    }
}
