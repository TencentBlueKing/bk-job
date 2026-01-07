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

package com.tencent.bk.job.common.paas.config;

import com.tencent.bk.job.common.config.ExternalSystemRetryProperties;
import com.tencent.bk.job.common.paas.login.ILoginClient;
import com.tencent.bk.job.common.paas.login.RetryableLoginClient;
import com.tencent.bk.job.common.paas.user.IUserApiClient;
import com.tencent.bk.job.common.paas.user.RetryableUserApiClient;
import com.tencent.bk.job.common.retry.ExponentialBackoffRetryPolicy;
import com.tencent.bk.job.common.retry.metrics.RetryMetricsRecorder;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * PaaS 客户端（BK-Login、BK-User）重试自动配置
 * <p>
 * 当启用外部系统重试时，自动为 Login 和 UserMgr 客户端添加重试能力
 * </p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ExternalSystemRetryProperties.class)
@AutoConfigureAfter({LoginAutoConfiguration.class, UserMgrAutoConfiguration.class})
@ConditionalOnProperty(name = "external-system.retry.enabled", havingValue = "true")
public class PaasRetryAutoConfiguration {

    @Bean
    @Primary
    @ConditionalOnBean(ILoginClient.class)
    public ILoginClient retryableLoginClient(ILoginClient loginClient,
                                             ObjectProvider<ExternalSystemRetryProperties> retryPropertiesProvider,
                                             ObjectProvider<MeterRegistry> meterRegistryProvider) {
        ExternalSystemRetryProperties retryProperties = retryPropertiesProvider.getIfAvailable(
            ExternalSystemRetryProperties::new);
        
        // 检查 BK-Login 系统级别是否启用重试
        if (!retryProperties.isSystemRetryEnabled(retryProperties.getBkLogin())) {
            log.info("BK-Login retry is disabled by system-level config");
            return loginClient;
        }
        
        ExternalSystemRetryProperties.SystemRetryProperties loginRetryProps = retryProperties.getBkLogin();
        ExponentialBackoffRetryPolicy retryPolicy = ExponentialBackoffRetryPolicy.builder()
            .initialIntervalMs(retryProperties.getSystemInitialIntervalMs(loginRetryProps))
            .maxIntervalMs(retryProperties.getSystemMaxIntervalMs(loginRetryProps))
            .maxAttempts(retryProperties.getSystemMaxAttempts(loginRetryProps))
            .multiplier(retryProperties.getSystemMultiplier(loginRetryProps))
            .build();
        
        RetryMetricsRecorder metricsRecorder = new RetryMetricsRecorder(
            meterRegistryProvider.getIfAvailable(),
            retryProperties.isMetricsEnabled()
        );
        
        log.info("Init RetryableLoginClient with exponential backoff: maxAttempts={}, initialIntervalMs={}",
            retryPolicy.getMaxAttempts(), retryPolicy.getInitialIntervalMs());
        return new RetryableLoginClient(loginClient, retryPolicy, metricsRecorder);
    }

    @Bean
    @Primary
    @ConditionalOnBean(IUserApiClient.class)
    public IUserApiClient retryableUserApiClient(IUserApiClient userApiClient,
                                                 ObjectProvider<ExternalSystemRetryProperties> retryPropertiesProvider,
                                                 ObjectProvider<MeterRegistry> meterRegistryProvider) {
        ExternalSystemRetryProperties retryProperties = retryPropertiesProvider.getIfAvailable(
            ExternalSystemRetryProperties::new);
        
        // 检查 BK-User 系统级别是否启用重试
        if (!retryProperties.isSystemRetryEnabled(retryProperties.getBkUser())) {
            log.info("BK-User retry is disabled by system-level config");
            return userApiClient;
        }
        
        ExternalSystemRetryProperties.SystemRetryProperties userRetryProps = retryProperties.getBkUser();
        ExponentialBackoffRetryPolicy retryPolicy = ExponentialBackoffRetryPolicy.builder()
            .initialIntervalMs(retryProperties.getSystemInitialIntervalMs(userRetryProps))
            .maxIntervalMs(retryProperties.getSystemMaxIntervalMs(userRetryProps))
            .maxAttempts(retryProperties.getSystemMaxAttempts(userRetryProps))
            .multiplier(retryProperties.getSystemMultiplier(userRetryProps))
            .build();
        
        RetryMetricsRecorder metricsRecorder = new RetryMetricsRecorder(
            meterRegistryProvider.getIfAvailable(),
            retryProperties.isMetricsEnabled()
        );
        
        log.info("Init RetryableUserApiClient with exponential backoff: maxAttempts={}, initialIntervalMs={}",
            retryPolicy.getMaxAttempts(), retryPolicy.getInitialIntervalMs());
        return new RetryableUserApiClient(userApiClient, retryPolicy, metricsRecorder);
    }
}
