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

package com.tencent.bk.job.common.gse.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.tencent.bk.job.common.WatchableThreadPoolExecutor;
import com.tencent.bk.job.common.crypto.Encryptor;
import com.tencent.bk.job.common.crypto.RSAEncryptor;
import com.tencent.bk.job.common.gse.GseClient;
import com.tencent.bk.job.common.gse.constants.DefaultBeanNames;
import com.tencent.bk.job.common.gse.constants.GseConstants;
import com.tencent.bk.job.common.gse.service.AgentStateClient;
import com.tencent.bk.job.common.gse.service.PreferV2AgentStateClientImpl;
import com.tencent.bk.job.common.gse.v1.GseV1ApiClient;
import com.tencent.bk.job.common.gse.v1.config.GseV1AutoConfiguration;
import com.tencent.bk.job.common.gse.v2.GseV2ApiClient;
import com.tencent.bk.job.common.gse.v2.GseV2AutoConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration(proxyBeanMethods = false)
@Import(
    {
        AgentStateQueryConfig.class,
        GseV1AutoConfiguration.class,
        GseV2AutoConfiguration.class
    }
)
public class GseAutoConfiguration {

    @Bean("GseApiClient")
    public GseClient gseClient(ObjectProvider<GseV1ApiClient> gseV1ApiClient,
                               ObjectProvider<GseV2ApiClient> gseV2ApiClient) {
        return new GseClient(gseV1ApiClient.getIfAvailable(),
            gseV2ApiClient.getIfAvailable());
    }

    @ConditionalOnMissingBean(name = DefaultBeanNames.AGENT_STATUS_QUERY_THREAD_POOL_EXECUTOR)
    @Bean(DefaultBeanNames.AGENT_STATUS_QUERY_THREAD_POOL_EXECUTOR)
    public ThreadPoolExecutor agentStatusQueryExecutor(MeterRegistry meterRegistry,
                                                       AgentStateQueryConfig agentStateQueryConfig) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("agentStatusQuery-thread-%d").build();
        return new WatchableThreadPoolExecutor(
            meterRegistry,
            "agentStatusQueryExecutor",
            agentStateQueryConfig.getGseQueryThreadsNum(),
            agentStateQueryConfig.getGseQueryThreadsMaxNum(),
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(agentStateQueryConfig.getGseQueryThreadsNum()),
            threadFactory,
            (r, executor) -> {
                log.warn("agentStatusQueryExecutor busy, use current thread to query");
                r.run();
            }
        );
    }

    @Bean(DefaultBeanNames.PREFER_V2_AGENT_STATE_CLIENT)
    public AgentStateClient preferV2AgentStateClientImpl(AgentStateQueryConfig agentStateQueryConfig,
                                                         GseClient gseClient,
                                                         @Qualifier("agentStatusQueryExecutor")
                                                             ThreadPoolExecutor threadPoolExecutor) {
        return new PreferV2AgentStateClientImpl(agentStateQueryConfig, gseClient, threadPoolExecutor);
    }

    @Bean("gseRsaEncryptor")
    public Encryptor rsaEncryptor() throws IOException, GeneralSecurityException {
        return new RSAEncryptor(GseConstants.publicKeyPermBase64);
    }
}
