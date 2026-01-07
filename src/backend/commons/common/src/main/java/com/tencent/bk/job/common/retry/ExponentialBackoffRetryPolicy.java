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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 指数退避重试策略
 * <p>
 * 重试间隔按指数增长，例如：500ms → 1000ms → 2000ms → 4000ms → 8000ms
 * </p>
 */
@Slf4j
@Getter
public class ExponentialBackoffRetryPolicy implements RetryPolicy {

    /**
     * 默认初始间隔（毫秒）
     */
    public static final long DEFAULT_INITIAL_INTERVAL_MS = 500L;

    /**
     * 默认最大间隔（毫秒）
     */
    public static final long DEFAULT_MAX_INTERVAL_MS = 30000L;

    /**
     * 默认最大重试次数
     */
    public static final int DEFAULT_MAX_ATTEMPTS = 5;

    /**
     * 默认间隔增长倍数
     */
    public static final double DEFAULT_MULTIPLIER = 2.0;

    /**
     * 初始间隔（毫秒）
     */
    private final long initialIntervalMs;

    /**
     * 最大间隔（毫秒）
     */
    private final long maxIntervalMs;

    /**
     * 最大重试次数
     */
    private final int maxAttempts;

    /**
     * 间隔增长倍数
     */
    private final double multiplier;

    /**
     * 使用默认参数创建指数退避重试策略
     */
    public ExponentialBackoffRetryPolicy() {
        this(DEFAULT_INITIAL_INTERVAL_MS, DEFAULT_MAX_INTERVAL_MS, DEFAULT_MAX_ATTEMPTS, DEFAULT_MULTIPLIER);
    }

    /**
     * 创建指数退避重试策略
     *
     * @param initialIntervalMs 初始间隔（毫秒）
     * @param maxIntervalMs     最大间隔（毫秒）
     * @param maxAttempts       最大重试次数
     * @param multiplier        间隔增长倍数
     */
    public ExponentialBackoffRetryPolicy(long initialIntervalMs, long maxIntervalMs, int maxAttempts, double multiplier) {
        if (initialIntervalMs <= 0) {
            throw new IllegalArgumentException("initialIntervalMs must be positive");
        }
        if (maxIntervalMs <= 0) {
            throw new IllegalArgumentException("maxIntervalMs must be positive");
        }
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        if (multiplier <= 0) {
            throw new IllegalArgumentException("multiplier must be positive");
        }
        this.initialIntervalMs = initialIntervalMs;
        this.maxIntervalMs = maxIntervalMs;
        this.maxAttempts = maxAttempts;
        this.multiplier = multiplier;
    }

    @Override
    public long getWaitTimeMs(int attemptNumber) {
        if (attemptNumber <= 0) {
            return initialIntervalMs;
        }
        // 计算指数退避：initialInterval * multiplier^(attemptNumber-1)
        double waitTime = initialIntervalMs * Math.pow(multiplier, attemptNumber - 1);
        // 确保不超过最大间隔
        return Math.min((long) waitTime, maxIntervalMs);
    }

    @Override
    public boolean shouldRetry(int attemptNumber, Exception exception) {
        // 默认情况下，只要未达到最大重试次数就继续重试
        return attemptNumber < maxAttempts;
    }

    /**
     * 创建 Builder
     *
     * @return Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder 模式
     */
    public static class Builder {
        private long initialIntervalMs = DEFAULT_INITIAL_INTERVAL_MS;
        private long maxIntervalMs = DEFAULT_MAX_INTERVAL_MS;
        private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
        private double multiplier = DEFAULT_MULTIPLIER;

        public Builder initialIntervalMs(long initialIntervalMs) {
            this.initialIntervalMs = initialIntervalMs;
            return this;
        }

        public Builder maxIntervalMs(long maxIntervalMs) {
            this.maxIntervalMs = maxIntervalMs;
            return this;
        }

        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder multiplier(double multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        public ExponentialBackoffRetryPolicy build() {
            return new ExponentialBackoffRetryPolicy(initialIntervalMs, maxIntervalMs, maxAttempts, multiplier);
        }
    }
}
