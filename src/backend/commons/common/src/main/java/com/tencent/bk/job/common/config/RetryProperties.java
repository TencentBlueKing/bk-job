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

package com.tencent.bk.job.common.config;


import lombok.Data;

/**
 * 调用外部系统时的重试配置
 */
@Data
public class RetryProperties {
    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 最大重试次数
     */
    private Integer maxAttempts;

    /**
     * 初始重试间隔
     */
    private Long initialIntervalMs;

    /**
     * 最大重试间隔
     */
    private Long maxIntervalMs;

    /**
     * 间隔增长倍数
     */
    private Double multiplier;

    /**
     * 是否启用重试指标采集
     */
    private Boolean metricsEnabled;

    /**
     * 熔断器配置
     */
    private CircuitBreakerProperties circuitBreaker;

    /**
     * 获取默认的全局重试配置参数
     *
     * @return 默认的全局重试配置参数
     */
    public static RetryProperties defaultGlobalConfig() {
        RetryProperties retryProperties = new RetryProperties();
        // 是否启用重试：默认关闭
        retryProperties.setEnabled(false);
        // 初始重试间隔（毫秒），默认 500ms
        retryProperties.setInitialIntervalMs(500L);
        // 最大重试次数，默认 5 次
        retryProperties.setMaxAttempts(5);
        // 最大重试间隔（毫秒），默认 30000ms (30秒)
        retryProperties.setMaxIntervalMs(30000L);
        // 间隔增长倍数，默认 2.0
        retryProperties.setMultiplier(2.0);
        // 是否启用重试指标采集，默认开启
        retryProperties.setMetricsEnabled(true);
        // 熔断器
        retryProperties.setCircuitBreaker(CircuitBreakerProperties.defaultConfig());
        return retryProperties;
    }
}
