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

import com.tencent.bk.job.common.config.ExternalSystemRetryProperties;
import com.tencent.bk.job.common.gse.IGseClient;
import com.tencent.bk.job.common.gse.config.ConditionalOnMockGseV2ApiDisabled;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Slf4j
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter({GseV2AutoConfiguration.class})
@EnableConfigurationProperties({ExternalSystemRetryProperties.class})
@ConditionalOnProperty(name = "gseV2.enabled", havingValue = "true", matchIfMissing = true)
public class GseV2RetryAutoConfiguration {

    @Primary
    @Bean("retryableGseV2ApiClient")
    @ConditionalOnMockGseV2ApiDisabled
    @ConditionalOnProperty(name = "external-system.retry.enabled", havingValue = "true")
    public IGseClient retryableGseV2ApiClient(IGseClient gseClient,
                                              MeterRegistry meterRegistry,
                                              ObjectProvider<ExternalSystemRetryProperties> retryPropertiesProvider) {
        ExternalSystemRetryProperties retryProperties = retryPropertiesProvider.getIfAvailable(
            ExternalSystemRetryProperties::new
        );

        // 检查 GSE 系统级别是否启用重试
        if (!retryProperties.isSystemRetryEnabled(retryProperties.getGse())) {
            log.info("GSE retry is disabled by system-level config, using non-retryable client");
            return gseClient;
        }

        log.info("Init retryableGseV2ApiClient with exponential backoff");
        return new RetryableGseV2ApiClient(gseClient, meterRegistry, retryProperties);
    }
}
