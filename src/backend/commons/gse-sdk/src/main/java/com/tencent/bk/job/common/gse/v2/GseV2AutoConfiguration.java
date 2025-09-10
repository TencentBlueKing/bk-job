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

import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.BkApiGatewayProperties;
import com.tencent.bk.job.common.gse.GseClient;
import com.tencent.bk.job.common.gse.IGseClient;
import com.tencent.bk.job.common.gse.config.ConditionalOnMockGseV2ApiDisabled;
import com.tencent.bk.job.common.gse.config.ConditionalOnMockGseV2ApiEnabled;
import com.tencent.bk.job.common.gse.config.GseV2Properties;
import com.tencent.bk.job.common.gse.mock.MockGseV2Client;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({GseV2Properties.class})
@ConditionalOnProperty(name = "gseV2.enabled", havingValue = "true", matchIfMissing = true)
public class GseV2AutoConfiguration {

    @Primary
    @Bean("gseV2ApiClient")
    @ConditionalOnMockGseV2ApiDisabled
    @ConditionalOnProperty(name = "gseV2.retry.enabled", havingValue = "false", matchIfMissing = true)
    public IGseClient gseV2ApiClient(MeterRegistry meterRegistry,
                                     AppProperties appProperties,
                                     BkApiGatewayProperties bkApiGatewayProperties,
                                     TenantEnvService tenantEnvService) {
        log.info("Init gseV2ApiClient");
        return new GseClient(
            new GseV2ApiClient(
                meterRegistry,
                appProperties,
                bkApiGatewayProperties,
                tenantEnvService
            )
        );
    }

    @Bean
    @ConditionalOnMockGseV2ApiEnabled
    public IGseClient mockedGseV2ApiClient() {
        return new MockGseV2Client();
    }

    @Bean("retryableGseV2ApiClient")
    @ConditionalOnMockGseV2ApiDisabled
    @ConditionalOnProperty(name = "gseV2.retry.enabled", havingValue = "true")
    public IGseClient retryableGseV2ApiClient(MeterRegistry meterRegistry,
                                              AppProperties appProperties,
                                              BkApiGatewayProperties bkApiGatewayProperties,
                                              GseV2Properties gseV2Properties,
                                              TenantEnvService tenantEnvService) {
        log.info("Init retryableGseV2ApiClient");
        return new RetryableGseV2ApiClient(
            meterRegistry,
            appProperties,
            bkApiGatewayProperties,
            gseV2Properties,
            tenantEnvService
        );
    }
}
