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

import com.tencent.bk.job.common.WatchableThreadPoolExecutor;
import com.tencent.bk.job.common.cc.mock.MockBizCmdbClient;
import com.tencent.bk.job.common.cc.mock.MockTenantSetCmdbClient;
import com.tencent.bk.job.common.cc.sdk.BizCmdbClient;
import com.tencent.bk.job.common.cc.sdk.BizSetCmdbClient;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.cc.sdk.IBizSetCmdbClient;
import com.tencent.bk.job.common.cc.sdk.ITenantSetCmdbClient;
import com.tencent.bk.job.common.cc.sdk.TenantSetCmdbClient;
import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.BkApiAutoConfiguration;
import com.tencent.bk.job.common.esb.config.BkApiGatewayProperties;
import com.tencent.bk.job.common.esb.constants.EsbLang;
import com.tencent.bk.job.common.paas.user.IVirtualAdminAccountProvider;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.FlowController;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration(proxyBeanMethods = false)
@Import({CmdbConfig.class})
@AutoConfigureAfter(BkApiAutoConfiguration.class)
@Slf4j
public class CmdbAutoConfiguration {
    @Bean("cmdbThreadPoolExecutor")
    public ThreadPoolExecutor cmdbThreadPoolExecutor(MeterRegistry meterRegistry, CmdbConfig cmdbConfig) {
        int cmdbQueryThreadsNum = cmdbConfig.getCmdbQueryThreadsNum();
        return new WatchableThreadPoolExecutor(
            meterRegistry,
            "cmdbThreadPoolExecutor",
            cmdbQueryThreadsNum,
            cmdbQueryThreadsNum,
            180L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(cmdbQueryThreadsNum * 4), (r, executor) -> {
            //使用请求的线程直接拉取数据
            log.error("cmdb request runnable rejected, use current thread({}), plz add more threads",
                Thread.currentThread().getName());
            r.run();
        });
    }

    @Bean("cmdbLongTermThreadPoolExecutor")
    public ThreadPoolExecutor cmdbLongTermThreadPoolExecutor(CmdbConfig cmdbConfig, MeterRegistry meterRegistry) {
        int longTermCmdbQueryThreadsNum = cmdbConfig.getFindHostRelationLongTermConcurrency();
        return new WatchableThreadPoolExecutor(
            meterRegistry,
            "cmdbLongTermThreadPoolExecutor",
            longTermCmdbQueryThreadsNum,
            longTermCmdbQueryThreadsNum,
            180L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(longTermCmdbQueryThreadsNum * 4), (r, executor) -> {
            //使用请求的线程直接拉取数据
            log.warn("cmdb long term request runnable rejected, use current thread({}), plz add more threads",
                Thread.currentThread().getName());
            r.run();
        });
    }

    @Bean
    @Primary
    @ConditionalOnMockCmdbApiDisabled
    public IBizCmdbClient bizCmdbClient(AppProperties appProperties,
                                        BkApiGatewayProperties bkApiGatewayProperties,
                                        CmdbConfig cmdbConfig,
                                        ThreadPoolExecutor cmdbThreadPoolExecutor,
                                        ThreadPoolExecutor cmdbLongTermThreadPoolExecutor,
                                        MeterRegistry meterRegistry,
                                        ObjectProvider<FlowController> flowControllerProvider,
                                        TenantEnvService tenantEnvService,
                                        IVirtualAdminAccountProvider virtualAdminAccountProvider) {
        return new BizCmdbClient(
            appProperties,
            bkApiGatewayProperties,
            cmdbConfig,
            EsbLang.EN,
            cmdbThreadPoolExecutor,
            cmdbLongTermThreadPoolExecutor,
            flowControllerProvider.getIfAvailable(),
            meterRegistry,
            tenantEnvService,
            virtualAdminAccountProvider
        );
    }

    @Bean("cnBizCmdbClient")
    @ConditionalOnMockCmdbApiDisabled
    public IBizCmdbClient cnBizCmdbClient(AppProperties appProperties,
                                          BkApiGatewayProperties bkApiGatewayProperties,
                                          CmdbConfig cmdbConfig,
                                          ThreadPoolExecutor cmdbThreadPoolExecutor,
                                          ThreadPoolExecutor cmdbLongTermThreadPoolExecutor,
                                          MeterRegistry meterRegistry,
                                          ObjectProvider<FlowController> flowControllerProvider,
                                          TenantEnvService tenantEnvService,
                                          IVirtualAdminAccountProvider virtualAdminAccountProvider) {
        return new BizCmdbClient(
            appProperties,
            bkApiGatewayProperties,
            cmdbConfig,
            EsbLang.CN,
            cmdbThreadPoolExecutor,
            cmdbLongTermThreadPoolExecutor,
            flowControllerProvider.getIfAvailable(),
            meterRegistry,
            tenantEnvService,
            virtualAdminAccountProvider
        );
    }

    @Bean
    public IBizSetCmdbClient bizSetCmdbClient(AppProperties appProperties,
                                              BkApiGatewayProperties bkApiGatewayProperties,
                                              CmdbConfig cmdbConfig,
                                              MeterRegistry meterRegistry,
                                              ObjectProvider<FlowController> flowControllerProvider,
                                              TenantEnvService tenantEnvService,
                                              IVirtualAdminAccountProvider virtualAdminAccountProvider) {
        return new BizSetCmdbClient(
            appProperties,
            bkApiGatewayProperties,
            cmdbConfig,
            flowControllerProvider.getIfAvailable(),
            meterRegistry,
            tenantEnvService,
            virtualAdminAccountProvider
        );
    }

    @Bean
    @ConditionalOnMockCmdbApiDisabled
    public ITenantSetCmdbClient tenantSetCmdbClient(AppProperties appProperties,
                                                    BkApiGatewayProperties bkApiGatewayProperties,
                                                    CmdbConfig cmdbConfig,
                                                    MeterRegistry meterRegistry,
                                                    ObjectProvider<FlowController> flowControllerProvider,
                                                    TenantEnvService tenantEnvService,
                                                    IVirtualAdminAccountProvider virtualAdminAccountProvider) {
        return new TenantSetCmdbClient(
            appProperties,
            bkApiGatewayProperties,
            cmdbConfig,
            flowControllerProvider.getIfAvailable(),
            meterRegistry,
            tenantEnvService,
            virtualAdminAccountProvider
        );
    }

    @Bean
    @Primary
    @ConditionalOnMockCmdbApiEnabled
    public IBizCmdbClient mockedBizCmdbClient(AppProperties appProperties,
                                              BkApiGatewayProperties bkApiGatewayProperties,
                                              CmdbConfig cmdbConfig,
                                              ThreadPoolExecutor cmdbThreadPoolExecutor,
                                              ThreadPoolExecutor cmdbLongTermThreadPoolExecutor,
                                              MeterRegistry meterRegistry,
                                              ObjectProvider<FlowController> flowControllerProvider,
                                              TenantEnvService tenantEnvService,
                                              IVirtualAdminAccountProvider virtualAdminAccountProvider) {
        return new MockBizCmdbClient(
            appProperties,
            bkApiGatewayProperties,
            cmdbConfig,
            EsbLang.EN,
            cmdbThreadPoolExecutor,
            cmdbLongTermThreadPoolExecutor,
            meterRegistry,
            flowControllerProvider,
            tenantEnvService,
            virtualAdminAccountProvider
        );
    }

    @Bean("cnBizCmdbClient")
    @ConditionalOnMockCmdbApiEnabled
    public IBizCmdbClient mockedCnBizCmdbClient(AppProperties appProperties,
                                                BkApiGatewayProperties bkApiGatewayProperties,
                                                CmdbConfig cmdbConfig,
                                                ThreadPoolExecutor cmdbThreadPoolExecutor,
                                                ThreadPoolExecutor cmdbLongTermThreadPoolExecutor,
                                                MeterRegistry meterRegistry,
                                                ObjectProvider<FlowController> flowControllerProvider,
                                                TenantEnvService tenantEnvService,
                                                IVirtualAdminAccountProvider virtualAdminAccountProvider) {
        return new MockBizCmdbClient(
            appProperties,
            bkApiGatewayProperties,
            cmdbConfig,
            EsbLang.CN,
            cmdbThreadPoolExecutor,
            cmdbLongTermThreadPoolExecutor,
            meterRegistry,
            flowControllerProvider,
            tenantEnvService,
            virtualAdminAccountProvider
        );
    }

    @Bean
    @ConditionalOnMockCmdbApiEnabled
    public ITenantSetCmdbClient mockedTenantSetCmdbClient() {
        return new MockTenantSetCmdbClient();
    }
}
