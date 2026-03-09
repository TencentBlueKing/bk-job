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

package com.tencent.bk.job.common.retry.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * 重试指标记录器
 * <p>
 * 用于记录外部系统调用的重试指标
 * </p>
 */
@Slf4j
public class RetryMetricsRecorder {

    private final MeterRegistry meterRegistry;
    private final boolean enabled;

    /**
     * 创建重试指标记录器
     *
     * @param meterRegistry 指标注册中心
     * @param enabled       是否启用指标记录
     */
    public RetryMetricsRecorder(MeterRegistry meterRegistry, boolean enabled) {
        this.meterRegistry = meterRegistry;
        this.enabled = enabled;
    }

    /**
     * 记录重试指标
     *
     * @param system     外部系统名称
     * @param api        API 名称/方法名
     * @param retryCount 重试次数（0 表示首次成功，1-N 表示实际重试次数）
     * @param success    是否最终成功
     */
    public void recordRetry(String system, String api, int retryCount, boolean success) {
        if (!enabled || meterRegistry == null) {
            return;
        }

        try {
            String finalResult = success
                ? RetryMetricsConstants.TAG_VALUE_RESULT_SUCCESS
                : RetryMetricsConstants.TAG_VALUE_RESULT_FAILURE;

            Counter.builder(RetryMetricsConstants.NAME_EXTERNAL_SYSTEM_RETRY)
                .description("External system retry metrics")
                .tag(RetryMetricsConstants.TAG_KEY_SYSTEM, system)
                .tag(RetryMetricsConstants.TAG_KEY_API, api)
                .tag(RetryMetricsConstants.TAG_KEY_RETRY_COUNT, String.valueOf(retryCount))
                .tag(RetryMetricsConstants.TAG_KEY_FINAL_RESULT, finalResult)
                .register(meterRegistry)
                .increment();

            if (log.isDebugEnabled()) {
                log.debug("Recorded retry metric: system={}, api={}, retryCount={}, success={}",
                    system, api, retryCount, success);
            }
        } catch (Exception e) {
            log.warn("Failed to record retry metric: system={}, api={}, retryCount={}, success={}",
                system, api, retryCount, success, e);
        }
    }

    /**
     * 是否启用指标记录
     *
     * @return 是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }
}
