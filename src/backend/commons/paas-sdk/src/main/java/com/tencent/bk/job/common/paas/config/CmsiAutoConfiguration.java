/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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
import com.tencent.bk.job.common.paas.cmsi.CmsiApiClient;
import com.tencent.bk.job.common.paas.cmsi.ICmsiClient;
import com.tencent.bk.job.common.paas.cmsi.MockCmsiClient;
import com.tencent.bk.job.common.paas.config.condition.ConditionalOnMockCmsiApiDisable;
import com.tencent.bk.job.common.paas.config.condition.ConditionalOnMockCmsiApiEnable;
import com.tencent.bk.job.common.paas.user.IVirtualAdminAccountProvider;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@Slf4j
public class CmsiAutoConfiguration {

    @Bean
    @ConditionalOnMockCmsiApiDisable
    public ICmsiClient cmsiApiClient(AppProperties appProperties,
                                     BkApiGatewayProperties apiGatewayProperties,
                                     ObjectProvider<MeterRegistry> meterRegistryObjectProvider,
                                     TenantEnvService tenantEnvService,
                                     IVirtualAdminAccountProvider virtualAdminAccountProvider) {
        log.info("Init CmsiApiClient");
        return new CmsiApiClient(
            apiGatewayProperties,
            appProperties,
            meterRegistryObjectProvider.getIfAvailable(),
            tenantEnvService,
            virtualAdminAccountProvider
        );
    }

    @Bean
    @ConditionalOnMockCmsiApiEnable
    public ICmsiClient mockCmsiClient(AppProperties appProperties,
                                      BkApiGatewayProperties apiGatewayProperties,
                                      ObjectProvider<MeterRegistry> meterRegistryObjectProvider,
                                      TenantEnvService tenantEnvService) {
        log.info("Init mockCmsiClient");
        return new MockCmsiClient(
            apiGatewayProperties,
            appProperties,
            meterRegistryObjectProvider.getIfAvailable(),
            tenantEnvService
        );
    }

}
