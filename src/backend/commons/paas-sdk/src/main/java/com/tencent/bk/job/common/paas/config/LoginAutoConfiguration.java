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

import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.BkApiGatewayProperties;
import com.tencent.bk.job.common.esb.config.EsbProperties;
import com.tencent.bk.job.common.paas.config.condition.ConditionalOnCustomLoginDisable;
import com.tencent.bk.job.common.paas.config.condition.ConditionalOnCustomLoginEnable;
import com.tencent.bk.job.common.paas.config.condition.ConditionalOnLoginUseApiGw;
import com.tencent.bk.job.common.paas.config.condition.ConditionalOnLoginUseEsb;
import com.tencent.bk.job.common.paas.login.CustomLoginClient;
import com.tencent.bk.job.common.paas.login.ILoginClient;
import com.tencent.bk.job.common.paas.login.StandardLoginApiGwClient;
import com.tencent.bk.job.common.paas.login.StandardLoginEsbClient;
import com.tencent.bk.job.common.paas.login.v3.BkLoginApiGwClient;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration(proxyBeanMethods = false)
@Slf4j
@EnableConfigurationProperties(LoginProperties.class)
public class LoginAutoConfiguration {

    @Bean
    @ConditionalOnCustomLoginEnable
    public ILoginClient customLoginClient(@Autowired LoginProperties loginProperties) {
        log.info("Init customLoginClient");
        return new CustomLoginClient(loginProperties.getCustom().getLoginUrl());
    }

    @Bean
    @Primary
    @ConditionalOnLoginUseApiGw
    @ConditionalOnCustomLoginDisable
    public ILoginClient standardLoginApiGwClient(BkApiGatewayProperties bkApiGatewayProperties,
                                                 AppProperties appProperties,
                                                 ObjectProvider<MeterRegistry> meterRegistryObjectProvider,
                                                 TenantEnvService tenantEnvService) {
        log.info("Init standardLoginApiGwClient");
        return new StandardLoginApiGwClient(
            new BkLoginApiGwClient(
                bkApiGatewayProperties,
                appProperties,
                meterRegistryObjectProvider.getIfAvailable(),
                tenantEnvService
            ),
            tenantEnvService
        );
    }

    @Bean
    @ConditionalOnLoginUseEsb
    @ConditionalOnCustomLoginDisable
    public ILoginClient standardLoginEsbClient(EsbProperties esbProperties,
                                               AppProperties appProperties,
                                               MeterRegistry meterRegistry,
                                               TenantEnvService tenantEnvService) {
        log.info("Init standardLoginEsbClient");
        return new StandardLoginEsbClient(
            esbProperties,
            appProperties,
            meterRegistry,
            tenantEnvService
        );
    }
}
