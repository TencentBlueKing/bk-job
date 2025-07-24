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

package com.tencent.bk.job.execute.config;

import com.tencent.bk.job.common.gse.config.AgentStateQueryConfig;
import com.tencent.bk.job.common.gse.constants.DefaultBeanNames;
import com.tencent.bk.job.common.gse.service.AgentStateClient;
import com.tencent.bk.job.common.gse.service.AutoChoosingAgentStateClientImpl;
import com.tencent.bk.job.common.gse.service.BizHostInfoQueryService;
import com.tencent.bk.job.common.gse.service.GseV1AgentStateClientImpl;
import com.tencent.bk.job.common.gse.service.GseV2AgentStateClientImpl;
import com.tencent.bk.job.common.gse.service.UseV2ByFeatureAgentStateClientImpl;
import com.tencent.bk.job.common.gse.v1.GseV1ApiClient;
import com.tencent.bk.job.common.gse.v2.GseV2ApiClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration(value = "jobExecuteGseConfig")
public class GseConfig {

    public static final String EXECUTE_BEAN_PREFIX = "jobExecute";
    public static final String EXECUTE_BEAN_GSE_V1_AGENT_STATE_CLIENT = EXECUTE_BEAN_PREFIX + "GseV1AgentStateClient";
    public static final String EXECUTE_BEAN_GSE_V2_AGENT_STATE_CLIENT = EXECUTE_BEAN_PREFIX + "GseV2AgentStateClient";
    public static final String EXECUTE_BEAN_AGENT_STATE_CLIENT = EXECUTE_BEAN_PREFIX + "AgentStateClient";
    public static final String EXECUTE_BEAN_USE_V2_BY_FEATURE_AGENT_STATE_CLIENT =
        EXECUTE_BEAN_PREFIX + DefaultBeanNames.USE_V2_BY_FEATURE_AGENT_STATE_CLIENT;

    @Bean(EXECUTE_BEAN_GSE_V1_AGENT_STATE_CLIENT)
    public GseV1AgentStateClientImpl gseV1AgentStateClient(AgentStateQueryConfig agentStateQueryConfig,
                                                           ObjectProvider<GseV1ApiClient> gseV1ApiClient,
                                                           @Qualifier(DefaultBeanNames.AGENT_STATUS_QUERY_THREAD_POOL_EXECUTOR)
                                                               ThreadPoolExecutor threadPoolExecutor) {
        return new GseV1AgentStateClientImpl(
            agentStateQueryConfig,
            gseV1ApiClient.getIfAvailable(),
            threadPoolExecutor
        );
    }

    @Bean(EXECUTE_BEAN_GSE_V2_AGENT_STATE_CLIENT)
    public GseV2AgentStateClientImpl gseV2AgentStateClient(AgentStateQueryConfig agentStateQueryConfig,
                                                           ObjectProvider<GseV2ApiClient> gseV2ApiClient,
                                                           @Qualifier(DefaultBeanNames.AGENT_STATUS_QUERY_THREAD_POOL_EXECUTOR)
                                                               ThreadPoolExecutor threadPoolExecutor) {
        return new GseV2AgentStateClientImpl(
            agentStateQueryConfig,
            gseV2ApiClient.getIfAvailable(),
            threadPoolExecutor
        );
    }

    @Bean(EXECUTE_BEAN_USE_V2_BY_FEATURE_AGENT_STATE_CLIENT)
    public AgentStateClient useV2ByFeatureAgentStateClient(@Qualifier(EXECUTE_BEAN_GSE_V1_AGENT_STATE_CLIENT)
                                                               GseV1AgentStateClientImpl gseV1AgentStateClient,
                                                           @Qualifier(EXECUTE_BEAN_GSE_V2_AGENT_STATE_CLIENT)
                                                               GseV2AgentStateClientImpl gseV2AgentStateClient,
                                                           @Qualifier("jobExecuteBizHostInfoQueryService")
                                                               BizHostInfoQueryService bizHostInfoQueryService) {
        return new UseV2ByFeatureAgentStateClientImpl(
            gseV1AgentStateClient,
            gseV2AgentStateClient,
            bizHostInfoQueryService
        );
    }

    @Primary
    @Bean(EXECUTE_BEAN_AGENT_STATE_CLIENT)
    public AgentStateClient AutoChoosingAgentStateClientImpl(
        @Qualifier(DefaultBeanNames.PREFER_V2_AGENT_STATE_CLIENT) AgentStateClient preferV2AgentStateClient,
        @Qualifier(EXECUTE_BEAN_USE_V2_BY_FEATURE_AGENT_STATE_CLIENT)
            AgentStateClient useV2ByFeatureAgentStateClient
    ) {
        return new AutoChoosingAgentStateClientImpl(preferV2AgentStateClient, useV2ByFeatureAgentStateClient);
    }
}
