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

import com.tencent.bk.job.common.retry.metrics.RetryMetricsRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 重试执行器测试用例
 */
@DisplayName("RetryExecutor 测试")
public class RetryExecutorTest {

    private RetryMetricsRecorder mockMetricsRecorder;
    private ExponentialBackoffRetryPolicy retryPolicy;
    private RetryExecutor retryExecutor;

    @BeforeEach
    void setUp() {
        mockMetricsRecorder = mock(RetryMetricsRecorder.class);
        // 使用较短的重试间隔以加快测试速度
        retryPolicy = ExponentialBackoffRetryPolicy.builder()
            .initialIntervalMs(10L)
            .maxIntervalMs(100L)
            .maxAttempts(3)
            .multiplier(2.0)
            .build();
        retryExecutor = new RetryExecutor(retryPolicy, mockMetricsRecorder, "test-system");
    }

    @Nested
    @DisplayName("成功场景测试")
    class SuccessScenarioTest {

        @Test
        @DisplayName("首次执行成功，retry_count=0，final_result=success")
        void testFirstAttemptSuccess() {
            String result = retryExecutor.executeWithRetry(() -> "Success", "testApi");

            assertEquals("Success", result);

            // 验证指标记录：retry_count=0（首次成功），final_result=success
            verify(mockMetricsRecorder, times(1)).recordRetry(
                "test-system",
                "testApi",
                0,       // retry_count = 0 表示首次成功
                true     // success
            );
        }

        @Test
        @DisplayName("重试后成功，retry_count=N，final_result=success")
        void testSuccessAfterRetry() {
            AtomicInteger attempts = new AtomicInteger(0);

            String result = retryExecutor.executeWithRetry(() -> {
                int attempt = attempts.incrementAndGet();
                if (attempt < 2) {
                    throw new RuntimeException("Temporary failure");
                }
                return "Success after retry";
            }, "testApi");

            assertEquals("Success after retry", result);
            assertEquals(2, attempts.get());

            // 验证指标记录：retry_count=1（重试1次后成功），final_result=success
            verify(mockMetricsRecorder, times(1)).recordRetry(
                "test-system",
                "testApi",
                1,       // retry_count = 1 表示重试了1次
                true     // success
            );
        }

        @Test
        @DisplayName("重试2次后成功")
        void testSuccessAfterTwoRetries() {
            AtomicInteger attempts = new AtomicInteger(0);

            String result = retryExecutor.executeWithRetry(() -> {
                int attempt = attempts.incrementAndGet();
                if (attempt < 3) {
                    throw new RuntimeException("Temporary failure");
                }
                return "Success after 2 retries";
            }, "testApi");

            assertEquals("Success after 2 retries", result);
            assertEquals(3, attempts.get());

            // 验证指标记录：retry_count=2
            verify(mockMetricsRecorder, times(1)).recordRetry(
                "test-system",
                "testApi",
                2,       // retry_count = 2
                true     // success
            );
        }
    }

    @Nested
    @DisplayName("失败场景测试")
    class FailureScenarioTest {

        @Test
        @DisplayName("达到最大重试次数后失败，final_result=failure")
        void testFailureAfterMaxAttempts() {
            AtomicInteger attempts = new AtomicInteger(0);

            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                retryExecutor.executeWithRetry(() -> {
                    attempts.incrementAndGet();
                    throw new RuntimeException("Permanent failure");
                }, "testApi")
            );

            assertEquals("Permanent failure", exception.getMessage());
            assertEquals(3, attempts.get()); // maxAttempts = 3

            // 验证指标记录：retry_count=3（重试次数等于maxAttempts），final_result=failure
            verify(mockMetricsRecorder, times(1)).recordRetry(
                "test-system",
                "testApi",
                3,       // retry_count = maxAttempts
                false    // failure
            );
        }

        @Test
        @DisplayName("受检异常包装为 RetryAbortedException")
        void testCheckedExceptionWrapped() {
            assertThrows(RetryAbortedException.class, () ->
                retryExecutor.executeWithRetry(() -> {
                    throw new IOException("IO Error");
                }, "testApi")
            );

            // 验证指标记录
            verify(mockMetricsRecorder, times(1)).recordRetry(
                anyString(),
                anyString(),
                anyInt(),
                anyBoolean()
            );
        }

        @Test
        @DisplayName("Error 类型异常直接抛出")
        void testErrorRethrown() {
            assertThrows(OutOfMemoryError.class, () ->
                retryExecutor.executeWithRetry(() -> {
                    throw new OutOfMemoryError("Out of memory");
                }, "testApi")
            );
        }
    }

    @Nested
    @DisplayName("无指标记录器场景测试")
    class NoMetricsRecorderTest {

        @Test
        @DisplayName("不配置指标记录器时正常工作")
        void testWithoutMetricsRecorder() {
            RetryExecutor executorWithoutMetrics = new RetryExecutor(retryPolicy);

            String result = executorWithoutMetrics.executeWithRetry(() -> "Success", "testApi");

            assertEquals("Success", result);
            // 不会抛出 NullPointerException
        }

        @Test
        @DisplayName("指标记录器为 null 时不记录指标")
        void testNullMetricsRecorder() {
            RetryExecutor executorWithNullMetrics = new RetryExecutor(retryPolicy, null, "test-system");

            String result = executorWithNullMetrics.executeWithRetry(() -> "Success", "testApi");

            assertEquals("Success", result);
        }
    }

    @Nested
    @DisplayName("指标维度测试")
    class MetricsDimensionTest {

        @Test
        @DisplayName("验证指标维度正确：system、api、retry_count、final_result")
        void testMetricsDimensions() {
            // 使用 ArgumentCaptor 捕获参数
            ArgumentCaptor<String> systemCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> apiCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Integer> retryCountCaptor = ArgumentCaptor.forClass(Integer.class);
            ArgumentCaptor<Boolean> successCaptor = ArgumentCaptor.forClass(Boolean.class);

            // 执行首次成功
            retryExecutor.executeWithRetry(() -> "Success", "getUserById");

            verify(mockMetricsRecorder).recordRetry(
                systemCaptor.capture(),
                apiCaptor.capture(),
                retryCountCaptor.capture(),
                successCaptor.capture()
            );

            assertEquals("test-system", systemCaptor.getValue());
            assertEquals("getUserById", apiCaptor.getValue());
            assertEquals(0, retryCountCaptor.getValue());
            assertEquals(true, successCaptor.getValue());
        }
    }

    @Nested
    @DisplayName("重试逻辑测试")
    class RetryLogicTest {

        @Test
        @DisplayName("验证重试次数正确")
        void testRetryCount() {
            AtomicInteger attempts = new AtomicInteger(0);

            assertThrows(RuntimeException.class, () ->
                retryExecutor.executeWithRetry(() -> {
                    attempts.incrementAndGet();
                    throw new RuntimeException("Always fail");
                }, "testApi")
            );

            // maxAttempts = 3，所以执行3次
            assertEquals(3, attempts.get());
        }

        @Test
        @DisplayName("null 返回值正常处理")
        void testNullReturnValue() {
            String result = retryExecutor.executeWithRetry(() -> null, "testApi");

            assertEquals(null, result);

            // 验证指标记录
            verify(mockMetricsRecorder, times(1)).recordRetry(
                "test-system",
                "testApi",
                0,
                true
            );
        }
    }
}
