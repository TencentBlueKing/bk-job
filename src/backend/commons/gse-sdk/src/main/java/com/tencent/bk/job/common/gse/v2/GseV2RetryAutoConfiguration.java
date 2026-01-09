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

package com.tencent.bk.job.common.gse.v2;

import com.tencent.bk.job.common.config.CircuitBreakerProperties;
import com.tencent.bk.job.common.config.ExternalSystemRetryProperties;
import com.tencent.bk.job.common.config.RetryProperties;
import com.tencent.bk.job.common.constant.BKConstants;
import com.tencent.bk.job.common.gse.IGseClient;
import com.tencent.bk.job.common.gse.config.ConditionalOnMockGseV2ApiDisabled;
import com.tencent.bk.job.common.gse.config.GseRetryCondition;
import com.tencent.bk.job.common.retry.ExponentialBackoffRetryPolicy;
import com.tencent.bk.job.common.retry.circuitbreaker.SystemCircuitBreakerManager;
import com.tencent.bk.job.common.retry.metrics.RetryMetricsRecorder;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Slf4j
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter({GseV2AutoConfiguration.class})
@EnableConfigurationProperties({ExternalSystemRetryProperties.class})
@ConditionalOnProperty(name = "gseV2.enabled", havingValue = "true", matchIfMissing = true)
@Conditional(GseRetryCondition.class)
public class GseV2RetryAutoConfiguration {

    @Primary
    @Bean("retryableGseV2ApiClient")
    @ConditionalOnMockGseV2ApiDisabled
    public IGseClient retryableGseV2ApiClient(IGseClient gseClient,
                                              MeterRegistry meterRegistry,
                                              ExternalSystemRetryProperties retryProperties) {
        // 检查 GSE 系统级别是否启用重试
        if (!retryProperties.isSystemRetryEnabled(retryProperties.getGse())) {
            log.info("GSE retry is disabled by system-level config, using non-retryable client");
            return gseClient;
        }

        // 使用 GSE 系统级配置，如果没有则使用全局配置
        RetryProperties gseRetryProps = retryProperties.getGse();
        ExponentialBackoffRetryPolicy retryPolicy = ExponentialBackoffRetryPolicy.builder()
            .initialIntervalMs(retryProperties.getSystemInitialIntervalMs(gseRetryProps))
            .maxIntervalMs(retryProperties.getSystemMaxIntervalMs(gseRetryProps))
            .maxAttempts(retryProperties.getSystemMaxAttempts(gseRetryProps))
            .multiplier(retryProperties.getSystemMultiplier(gseRetryProps))
            .build();

        RetryMetricsRecorder metricsRecorder = new RetryMetricsRecorder(
            meterRegistry,
            retryProperties.isMetricsEnabled(gseRetryProps)
        );

        log.info("Init retryableGseV2ApiClient");
        SystemCircuitBreakerManager circuitBreakerManager = buildCircuitBreakerManager(retryProperties);
        return new RetryableGseV2ApiClient(gseClient, retryPolicy, metricsRecorder, circuitBreakerManager);
    }

    private SystemCircuitBreakerManager buildCircuitBreakerManager(ExternalSystemRetryProperties retryProperties) {
        CircuitBreakerProperties globalCircuitBreakerProperties = retryProperties.getGlobal().getCircuitBreaker();
        CircuitBreakerProperties finalCircuitBreakerProperties = globalCircuitBreakerProperties;
        RetryProperties gseRetryProperties = retryProperties.getGse();
        if (gseRetryProperties != null && gseRetryProperties.getCircuitBreaker() != null) {
            // 优先使用为指定系统配置的值，未配置的字段使用全局配置填充
            finalCircuitBreakerProperties = gseRetryProperties.getCircuitBreaker();
            finalCircuitBreakerProperties.fillDefault(globalCircuitBreakerProperties);
        }
        return new SystemCircuitBreakerManager(BKConstants.SYSTEM_NAME_GSE, finalCircuitBreakerProperties);
    }
}
