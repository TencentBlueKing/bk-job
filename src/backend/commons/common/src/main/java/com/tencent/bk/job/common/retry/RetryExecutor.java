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

import com.tencent.bk.job.common.retry.circuitbreaker.CircuitBreaker;
import com.tencent.bk.job.common.retry.circuitbreaker.CircuitBreakerException;
import com.tencent.bk.job.common.retry.circuitbreaker.CircuitBreakerOpenException;
import com.tencent.bk.job.common.retry.circuitbreaker.SystemCircuitBreakerManager;
import com.tencent.bk.job.common.retry.metrics.RetryMetricsRecorder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;

import java.util.concurrent.Callable;

/**
 * 重试执行器，使用指定的重试策略执行任务，并自动记录重试指标
 */
@Slf4j
public class RetryExecutor {

    /**
     * 调用的目标系统名称
     */
    private final String systemName;
    /**
     * 重试策略
     */
    private final RetryPolicy retryPolicy;
    /**
     * 熔断管理器
     */
    private final SystemCircuitBreakerManager circuitBreakerManager;
    /**
     * 指标记录器
     */
    private final RetryMetricsRecorder metricsRecorder;

    /**
     * 创建重试执行器
     *
     * @param retryPolicy           重试策略
     * @param metricsRecorder       指标记录器（可为 null）
     * @param systemName            外部系统名称（用于指标记录）
     * @param circuitBreakerManager 熔断管理器（可为 null）
     */
    public RetryExecutor(RetryPolicy retryPolicy,
                         RetryMetricsRecorder metricsRecorder,
                         String systemName,
                         SystemCircuitBreakerManager circuitBreakerManager) {
        this.systemName = systemName;
        this.retryPolicy = retryPolicy;
        this.circuitBreakerManager = circuitBreakerManager;
        this.metricsRecorder = metricsRecorder;
    }

    /**
     * 执行带重试的任务
     *
     * @param task    待执行任务
     * @param apiName API 名称（用于指标记录和日志）
     * @param <T>     返回值类型
     * @return 执行结果
     * @throws Error                 待执行任务抛出 Error 时原样抛出
     * @throws RuntimeException      待执行任务抛出 RuntimeException 时原样抛出
     * @throws RetryAbortedException 重试被中断或待执行任务抛出受检异常时抛出
     */
    public <T> T executeWithRetry(Callable<T> task, String apiName) {
        int attemptNumber = 0;
        Exception lastException = null;
        long startTime = System.currentTimeMillis();
        CircuitBreaker circuitBreaker = circuitBreakerManager.getCircuitBreaker(apiName);
        while (attemptNumber < retryPolicy.getMaxAttempts()) {
            try {
                // 首先判断是否需要被熔断器处理执行
                T circuitBreakerResult = executeWithCircuitBreaker(circuitBreaker, apiName, task);
                if (circuitBreakerResult != null) {
                    return circuitBreakerResult;
                }

                startTime = System.currentTimeMillis();
                T result = task.call();
                long duration = System.currentTimeMillis() - startTime;

                // 记录熔断器成功
                if (circuitBreaker != null) {
                    circuitBreaker.onSuccess(duration);
                }

                // 成功，记录指标
                recordMetrics(apiName, attemptNumber, true);
                return result;
            } catch (Exception e) {
                lastException = e;
                attemptNumber++;
                // 检查是否应该重试
                if (attemptNumber >= retryPolicy.getMaxAttempts() || !retryPolicy.shouldRetry(attemptNumber, e)) {
                    break;
                }

                // 记录熔断器失败
                if (circuitBreaker != null) {
                    long duration = System.currentTimeMillis() - startTime;
                    circuitBreaker.onError(duration, lastException);
                }
                // 等待重试
                waitForRetry(apiName, attemptNumber, e);
            }
        }

        // 如果达到最大重试次数，记录失败指标并抛出异常
        if (lastException != null) {
            long duration = System.currentTimeMillis() - startTime;
            // 记录熔断器失败
            if (circuitBreaker != null) {
                circuitBreaker.onError(duration, lastException);
            }
            recordMetrics(apiName, attemptNumber, false);
            wrapAndThrowIfNecessary(lastException);
        }
        throw new RetryAbortedException("maxAttempts is invalid: " + retryPolicy.getMaxAttempts());
    }

    /**
     * 通过熔断器执行
     *
     * @param circuitBreaker 熔断器
     * @param apiName        API名称
     * @param task           调用任务
     * @param <T>            执行结果
     * @return null：熔断器未熔断，交给后续逻辑继续处理
     * T：熔断器已熔断，在特定配置下继续执行原始调用获得的结果
     */
    private <T> T executeWithCircuitBreaker(CircuitBreaker circuitBreaker, String apiName, Callable<T> task) {
        if (circuitBreaker == null) {
            return null;
        }
        if (circuitBreaker.canExecute()) {
            return null;
        }
        if (circuitBreaker.shouldFastFail()) {
            // 策略 1：快速失败
            throw new CircuitBreakerOpenException("CircuitBreakerFastFail: " + circuitBreaker.getFullName());
        } else {
            // 策略 2：继续调用但不重试
            log.warn("CircuitBreakerIsOpen: system={}, api={}, continue call without retry", systemName, apiName);
            long startTime = System.currentTimeMillis();
            try {
                T result = task.call();
                long duration = System.currentTimeMillis() - startTime;
                circuitBreaker.onSuccess(duration);
                return result;
            } catch (Throwable t) {
                long duration = System.currentTimeMillis() - startTime;
                circuitBreaker.onError(duration, t);
                String message = MessageFormatter.format(
                    "CircuitBreakerIsOpen, rawCallWithoutRetryFailed, system={}, api={}",
                    systemName,
                    apiName
                ).getMessage();
                throw new CircuitBreakerException(message, t);
            }
        }
    }

    /**
     * 等待重试
     *
     * @param apiName       API名称
     * @param attemptNumber 当前重试次数
     * @param e             导致重试的异常
     */
    private void waitForRetry(String apiName, int attemptNumber, Exception e) {
        // 计算等待时间
        long waitTimeMs = retryPolicy.getWaitTimeMs(attemptNumber);
        log.warn(
            "Retry attempt {} for api={}, system={}, waitTime={}ms, cause: {}",
            attemptNumber,
            apiName,
            systemName,
            waitTimeMs,
            e.getMessage()
        );
        // 等待后重试
        sleepSafely(waitTimeMs);
    }

    /**
     * 记录重试指标
     *
     * @param apiName       API 名称
     * @param attemptNumber 尝试次数
     * @param success       是否成功
     */
    private void recordMetrics(String apiName, int attemptNumber, boolean success) {
        if (metricsRecorder != null && systemName != null && apiName != null) {
            // retryCount = attemptNumber, 因为 attemptNumber 从 0 开始计数
            // attemptNumber=0 表示首次成功，attemptNumber=N 表示重试了 N 次
            metricsRecorder.recordRetry(systemName, apiName, attemptNumber, success);
        }
    }

    /**
     * 安全地进行线程休眠
     *
     * @param millis 休眠时间（毫秒）
     */
    private static void sleepSafely(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RetryAbortedException("Retry interrupted", e);
        }
    }

    /**
     * 对异常进行包装并抛出
     *
     * @param t 原始异常
     */
    private static void wrapAndThrowIfNecessary(Throwable t) {
        if (t instanceof Error) {
            throw (Error) t;
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RetryAbortedException("Retry failed", t);
    }
}
