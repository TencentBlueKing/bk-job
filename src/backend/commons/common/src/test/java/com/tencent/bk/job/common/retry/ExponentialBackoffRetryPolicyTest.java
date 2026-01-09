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

package com.tencent.bk.job.common.retry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 指数退避重试策略测试用例
 */
@DisplayName("ExponentialBackoffRetryPolicy 测试")
public class ExponentialBackoffRetryPolicyTest {

    @Nested
    @DisplayName("getWaitTimeMs 方法测试")
    class GetWaitTimeMsTest {

        @Test
        @DisplayName("指数退避计算正确：500 → 1000 → 2000 → 4000")
        void testExponentialBackoff() {
            ExponentialBackoffRetryPolicy policy = new ExponentialBackoffRetryPolicy(
                500L, 30000L, 5, 2.0
            );

            // 第1次重试等待 500ms
            assertEquals(500L, policy.getWaitTimeMs(1));
            // 第2次重试等待 1000ms (500 * 2)
            assertEquals(1000L, policy.getWaitTimeMs(2));
            // 第3次重试等待 2000ms (500 * 4)
            assertEquals(2000L, policy.getWaitTimeMs(3));
            // 第4次重试等待 4000ms (500 * 8)
            assertEquals(4000L, policy.getWaitTimeMs(4));
            // 第5次重试等待 8000ms (500 * 16)
            assertEquals(8000L, policy.getWaitTimeMs(5));
        }

        @Test
        @DisplayName("不超过最大间隔")
        void testMaxIntervalCap() {
            ExponentialBackoffRetryPolicy policy = new ExponentialBackoffRetryPolicy(
                1000L, 5000L, 10, 2.0
            );

            // 第1次重试等待 1000ms
            assertEquals(1000L, policy.getWaitTimeMs(1));
            // 第2次重试等待 2000ms
            assertEquals(2000L, policy.getWaitTimeMs(2));
            // 第3次重试等待 4000ms
            assertEquals(4000L, policy.getWaitTimeMs(3));
            // 第4次重试等待 5000ms（被限制，原本应是 8000ms）
            assertEquals(5000L, policy.getWaitTimeMs(4));
            // 第5次重试等待 5000ms（被限制）
            assertEquals(5000L, policy.getWaitTimeMs(5));
        }

        @Test
        @DisplayName("attemptNumber 小于等于0返回初始间隔")
        void testNonPositiveAttemptNumber() {
            ExponentialBackoffRetryPolicy policy = new ExponentialBackoffRetryPolicy(
                500L, 30000L, 5, 2.0
            );

            assertEquals(500L, policy.getWaitTimeMs(0));
            assertEquals(500L, policy.getWaitTimeMs(-1));
        }

        @Test
        @DisplayName("使用非整数倍数计算")
        void testNonIntegerMultiplier() {
            ExponentialBackoffRetryPolicy policy = new ExponentialBackoffRetryPolicy(
                100L, 10000L, 5, 1.5
            );

            // 第1次重试等待 100ms
            assertEquals(100L, policy.getWaitTimeMs(1));
            // 第2次重试等待 150ms (100 * 1.5)
            assertEquals(150L, policy.getWaitTimeMs(2));
            // 第3次重试等待 225ms (100 * 1.5^2)
            assertEquals(225L, policy.getWaitTimeMs(3));
        }
    }

    @Nested
    @DisplayName("shouldRetry 方法测试")
    class ShouldRetryTest {

        @Test
        @DisplayName("未达到最大重试次数时返回 true")
        void testShouldRetryWhenNotExhausted() {
            ExponentialBackoffRetryPolicy policy = new ExponentialBackoffRetryPolicy(
                500L, 30000L, 5, 2.0
            );
            RuntimeException exception = new RuntimeException("Test exception");

            assertTrue(policy.shouldRetry(1, exception));
            assertTrue(policy.shouldRetry(2, exception));
            assertTrue(policy.shouldRetry(3, exception));
            assertTrue(policy.shouldRetry(4, exception));
        }

        @Test
        @DisplayName("达到最大重试次数时返回 false")
        void testShouldNotRetryWhenExhausted() {
            ExponentialBackoffRetryPolicy policy = new ExponentialBackoffRetryPolicy(
                500L, 30000L, 5, 2.0
            );
            RuntimeException exception = new RuntimeException("Test exception");

            assertFalse(policy.shouldRetry(5, exception));
            assertFalse(policy.shouldRetry(6, exception));
        }

        @Test
        @DisplayName("maxAttempts=1 时第一次就不再重试")
        void testMaxAttemptsOne() {
            ExponentialBackoffRetryPolicy policy = ExponentialBackoffRetryPolicy.builder()
                .maxAttempts(1)
                .build();
            RuntimeException exception = new RuntimeException("Test exception");

            assertFalse(policy.shouldRetry(1, exception));
        }
    }
}
