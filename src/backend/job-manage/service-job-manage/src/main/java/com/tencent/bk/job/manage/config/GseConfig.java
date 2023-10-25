package com.tencent.bk.job.manage.config;

import com.tencent.bk.job.common.gse.GseClient;
import com.tencent.bk.job.common.gse.config.AgentStateQueryConfig;
import com.tencent.bk.job.common.gse.constants.DefaultBeanNames;
import com.tencent.bk.job.common.gse.service.AgentStateClient;
import com.tencent.bk.job.common.gse.service.AutoChoosingAgentStateClientImpl;
import com.tencent.bk.job.common.gse.service.BizHostInfoQueryService;
import com.tencent.bk.job.common.gse.service.UseV2ByFeatureAgentStateClientImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration(value = "jobManageGseConfig")
public class GseConfig {

    public static final String MANAGE_BEAN_PREFIX = "jobManage";
    public static final String MANAGE_BEAN_AGENT_STATE_CLIENT = MANAGE_BEAN_PREFIX + "AgentStateClient";
    public static final String MANAGE_BEAN_USE_V2_BY_FEATURE_AGENT_STATE_CLIENT =
        MANAGE_BEAN_PREFIX + DefaultBeanNames.USE_V2_BY_FEATURE_AGENT_STATE_CLIENT;

    @Bean(MANAGE_BEAN_USE_V2_BY_FEATURE_AGENT_STATE_CLIENT)
    public AgentStateClient useV2ByFeatureAgentStateClient(AgentStateQueryConfig agentStateQueryConfig,
                                                           GseClient gseClient,
                                                           @Qualifier("jobManageBizHostInfoQueryService")
                                                               BizHostInfoQueryService bizHostInfoQueryService,
                                                           @Qualifier(DefaultBeanNames.AGENT_STATUS_QUERY_THREAD_POOL_EXECUTOR)
                                                               ThreadPoolExecutor threadPoolExecutor) {
        return new UseV2ByFeatureAgentStateClientImpl(
            agentStateQueryConfig,
            gseClient,
            bizHostInfoQueryService,
            threadPoolExecutor
        );
    }

    @Primary
    @Bean(MANAGE_BEAN_AGENT_STATE_CLIENT)
    public AgentStateClient AutoChoosingAgentStateClientImpl(
        @Qualifier(DefaultBeanNames.PREFER_V2_AGENT_STATE_CLIENT)
            AgentStateClient preferV2AgentStateClient,
        @Qualifier(MANAGE_BEAN_USE_V2_BY_FEATURE_AGENT_STATE_CLIENT)
            AgentStateClient useV2ByFeatureAgentStateClient
    ) {
        return new AutoChoosingAgentStateClientImpl(preferV2AgentStateClient, useV2ByFeatureAgentStateClient);
    }
}
