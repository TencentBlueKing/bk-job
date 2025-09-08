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

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 重试工具类测试用例
 */
public class RetryUtilsTest {

    @Test
    void testSuccessWithoutRetry() {
        Callable<String> task = () -> "Success";
        String result = RetryUtils.executeWithRetry(task, 3, Duration.ofSeconds(1));
        assertEquals("Success", result);
    }

    @Test
    void testSuccessAfterRetry() {
        AtomicInteger attempts = new AtomicInteger(0);
        Callable<String> task = () -> {
            if (attempts.incrementAndGet() < 2) {
                throw new RuntimeException("Temporary failure");
            }
            return "Success";
        };

        String result = RetryUtils.executeWithRetry(task, 3, Duration.ofSeconds(1));
        assertEquals("Success", result);
        assertEquals(2, attempts.get());
    }

    @Test
    void testAbortOnNonRetryableException() {
        Callable<String> task = () -> {
            throw new RuntimeException("Permanent failure");
        };

        Predicate<Exception> retryCondition = e -> !e.getMessage().contains("Permanent");
        assertThrows(RuntimeException.class, () ->
            RetryUtils.executeWithRetry(task, 3, Duration.ofSeconds(1), retryCondition));
    }

    @Test
    void testExceedMaxAttempts() {
        Callable<String> task = () -> {
            throw new RuntimeException("Temporary failure");
        };

        assertThrows(RuntimeException.class, () ->
            RetryUtils.executeWithRetry(task, 3, Duration.ofSeconds(1)));
    }

    @Test
    void testInterrupted() {
        Callable<String> task = () -> {
            Thread.currentThread().interrupt();
            throw new InterruptedException("Interrupted");
        };

        assertThrows(RetryAbortedException.class, () ->
            RetryUtils.executeWithRetry(task, 3, Duration.ofSeconds(1)));
    }
}
