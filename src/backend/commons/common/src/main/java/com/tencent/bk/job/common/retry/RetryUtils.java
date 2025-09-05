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

import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

/**
 * 重试工具类
 */
@Slf4j
public class RetryUtils {
    /**
     * 对抛出异常的任务进行重试，任何异常都重试
     *
     * @param task        待执行任务
     * @param maxAttempts 最大执行次数
     * @param interval    重试间隔
     * @param <T>         返回值类型
     * @return 执行结果
     * @throws RetryAbortedException 不满足重试条件时（重试条件判定失败、超出最大重试次数）抛出
     */
    public static <T> T executeWithRetry(Callable<T> task,
                                         int maxAttempts,
                                         Duration interval) {
        return executeWithRetry(task, maxAttempts, interval, e -> true);
    }

    /**
     * 对抛出异常的任务进行重试，根据重试条件决定是否重试
     *
     * @param task           待执行任务
     * @param maxAttempts    最大执行次数
     * @param interval       重试间隔
     * @param retryCondition 重试条件
     * @param <T>            返回值类型
     * @return 执行结果
     * @throws RetryAbortedException 不满足重试条件时（重试条件判定失败、超出最大重试次数）抛出
     */
    public static <T> T executeWithRetry(Callable<T> task,
                                         int maxAttempts,
                                         Duration interval,
                                         Predicate<Exception> retryCondition) {
        int attempts = 0;
        Exception lastException = null;
        while (attempts < maxAttempts) {
            try {
                return task.call();
            } catch (Exception e) {
                lastException = e;
                attempts++;
                if (attempts >= maxAttempts) {
                    break;
                }

                if (!retryCondition.test(e)) {
                    throw new RetryAbortedException("Abort retry due to non-retryable exception", e);
                }
                String message = MessageFormatter.format(
                    "Retry {} after {}s, cause:",
                    attempts,
                    interval.getSeconds()
                ).getMessage();
                log.warn(message, e);
                sleepSafely(interval);
            }
        }
        throw new RetryAbortedException("Exceeded max attempts: " + maxAttempts, lastException);
    }

    /**
     * 安全地进行线程休眠
     *
     * @param duration 休眠时间
     */
    private static void sleepSafely(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RetryAbortedException("Retry interrupted", e);
        }
    }
}
