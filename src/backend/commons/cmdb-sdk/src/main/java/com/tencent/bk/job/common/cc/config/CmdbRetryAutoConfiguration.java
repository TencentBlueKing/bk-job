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

package com.tencent.bk.job.common.cc.config;

import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.cc.sdk.IBizSetCmdbClient;
import com.tencent.bk.job.common.cc.sdk.ITenantSetCmdbClient;
import com.tencent.bk.job.common.cc.sdk.RetryableBizCmdbClient;
import com.tencent.bk.job.common.cc.sdk.RetryableBizSetCmdbClient;
import com.tencent.bk.job.common.cc.sdk.RetryableTenantSetCmdbClient;
import com.tencent.bk.job.common.config.CircuitBreakerProperties;
import com.tencent.bk.job.common.config.ExternalSystemRetryProperties;
import com.tencent.bk.job.common.config.RetryProperties;
import com.tencent.bk.job.common.constant.BKConstants;
import com.tencent.bk.job.common.retry.ExponentialBackoffRetryPolicy;
import com.tencent.bk.job.common.retry.circuitbreaker.SystemCircuitBreakerManager;
import com.tencent.bk.job.common.retry.metrics.RetryMetricsRecorder;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * CMDB 客户端重试自动配置
 * <p>
 * 当启用外部系统重试时，自动为 CMDB 客户端添加重试能力
 * </p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ExternalSystemRetryProperties.class)
@AutoConfigureAfter(CmdbAutoConfiguration.class)
@Conditional(CmdbRetryCondition.class)
public class CmdbRetryAutoConfiguration {

    @Bean
    @Primary
    @ConditionalOnBean(IBizCmdbClient.class)
    @ConditionalOnMockCmdbApiDisabled
    public IBizCmdbClient retryableBizCmdbClient(IBizCmdbClient bizCmdbClient,
                                                 ExternalSystemRetryProperties retryProperties,
                                                 ObjectProvider<MeterRegistry> meterRegistryProvider) {
        // 检查 CMDB 系统级别是否启用重试
        if (!retryProperties.isSystemRetryEnabled(retryProperties.getCmdb())) {
            log.info("CMDB retry is disabled by system-level config");
            return bizCmdbClient;
        }

        ExponentialBackoffRetryPolicy retryPolicy = buildRetryPolicy(retryProperties);
        RetryMetricsRecorder metricsRecorder = buildMetricsRecorder(retryProperties, meterRegistryProvider);
        SystemCircuitBreakerManager circuitBreakerManager = buildCircuitBreakerManager(retryProperties);

        log.info("Init RetryableBizCmdbClient");
        return new RetryableBizCmdbClient(bizCmdbClient, retryPolicy, metricsRecorder, circuitBreakerManager);
    }

    @Bean
    @Primary
    @ConditionalOnBean(IBizSetCmdbClient.class)
    public IBizSetCmdbClient retryableBizSetCmdbClient(IBizSetCmdbClient bizSetCmdbClient,
                                                       ExternalSystemRetryProperties retryProperties,
                                                       ObjectProvider<MeterRegistry> meterRegistryProvider) {
        // 检查 CMDB 系统级别是否启用重试
        if (!retryProperties.isSystemRetryEnabled(retryProperties.getCmdb())) {
            log.info("CMDB BizSet retry is disabled by system-level config");
            return bizSetCmdbClient;
        }

        ExponentialBackoffRetryPolicy retryPolicy = buildRetryPolicy(retryProperties);
        RetryMetricsRecorder metricsRecorder = buildMetricsRecorder(retryProperties, meterRegistryProvider);
        SystemCircuitBreakerManager circuitBreakerManager = buildCircuitBreakerManager(retryProperties);

        log.info("Init RetryableBizSetCmdbClient");
        return new RetryableBizSetCmdbClient(bizSetCmdbClient, retryPolicy, metricsRecorder, circuitBreakerManager);
    }

    @Bean
    @Primary
    @ConditionalOnBean(ITenantSetCmdbClient.class)
    @ConditionalOnMockCmdbApiDisabled
    public ITenantSetCmdbClient retryableTenantSetCmdbClient(ITenantSetCmdbClient tenantSetCmdbClient,
                                                             ExternalSystemRetryProperties retryProperties,
                                                             ObjectProvider<MeterRegistry> meterRegistryProvider) {
        // 检查 CMDB 系统级别是否启用重试
        if (!retryProperties.isSystemRetryEnabled(retryProperties.getCmdb())) {
            log.info("CMDB TenantSet retry is disabled by system-level config");
            return tenantSetCmdbClient;
        }

        ExponentialBackoffRetryPolicy retryPolicy = buildRetryPolicy(retryProperties);
        RetryMetricsRecorder metricsRecorder = buildMetricsRecorder(retryProperties, meterRegistryProvider);
        SystemCircuitBreakerManager circuitBreakerManager = buildCircuitBreakerManager(retryProperties);

        log.info("Init RetryableTenantSetCmdbClient");
        return new RetryableTenantSetCmdbClient(
            tenantSetCmdbClient,
            retryPolicy,
            metricsRecorder,
            circuitBreakerManager
        );
    }

    private ExponentialBackoffRetryPolicy buildRetryPolicy(ExternalSystemRetryProperties retryProperties) {
        return ExponentialBackoffRetryPolicy.builder()
            .initialIntervalMs(retryProperties.getSystemInitialIntervalMs(retryProperties.getCmdb()))
            .maxIntervalMs(retryProperties.getSystemMaxIntervalMs(retryProperties.getCmdb()))
            .maxAttempts(retryProperties.getSystemMaxAttempts(retryProperties.getCmdb()))
            .multiplier(retryProperties.getSystemMultiplier(retryProperties.getCmdb()))
            .build();
    }

    private RetryMetricsRecorder buildMetricsRecorder(ExternalSystemRetryProperties retryProperties,
                                                      ObjectProvider<MeterRegistry> meterRegistryProvider) {
        return new RetryMetricsRecorder(
            meterRegistryProvider.getIfAvailable(),
            retryProperties.isMetricsEnabled(retryProperties.getCmdb())
        );
    }

    private SystemCircuitBreakerManager buildCircuitBreakerManager(ExternalSystemRetryProperties retryProperties) {
        CircuitBreakerProperties globalCircuitBreakerProperties = retryProperties.getGlobal().getCircuitBreaker();
        CircuitBreakerProperties finalCircuitBreakerProperties = globalCircuitBreakerProperties;
        RetryProperties cmdbRetryProperties = retryProperties.getCmdb();
        if (cmdbRetryProperties != null && cmdbRetryProperties.getCircuitBreaker() != null) {
            // 优先使用为指定系统配置的值，未配置的字段使用全局配置填充
            finalCircuitBreakerProperties = cmdbRetryProperties.getCircuitBreaker();
            finalCircuitBreakerProperties.fillDefault(globalCircuitBreakerProperties);
        }
        return new SystemCircuitBreakerManager(BKConstants.SYSTEM_NAME_CMDB, finalCircuitBreakerProperties);
    }
}
