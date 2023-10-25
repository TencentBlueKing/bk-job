package com.tencent.bk.job.execute.config;

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

@Configuration(value = "jobExecuteGseConfig")
public class GseConfig {

    public static final String EXECUTE_BEAN_PREFIX = "jobExecute";
    public static final String EXECUTE_BEAN_AGENT_STATE_CLIENT = EXECUTE_BEAN_PREFIX + "AgentStateClient";
    public static final String EXECUTE_BEAN_USE_V2_BY_FEATURE_AGENT_STATE_CLIENT =
        EXECUTE_BEAN_PREFIX + DefaultBeanNames.USE_V2_BY_FEATURE_AGENT_STATE_CLIENT;

    @Bean(EXECUTE_BEAN_USE_V2_BY_FEATURE_AGENT_STATE_CLIENT)
    public AgentStateClient useV2ByFeatureAgentStateClient(AgentStateQueryConfig agentStateQueryConfig,
                                                           GseClient gseClient,
                                                           @Qualifier("jobExecuteBizHostInfoQueryService")
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
    @Bean(EXECUTE_BEAN_AGENT_STATE_CLIENT)
    public AgentStateClient AutoChoosingAgentStateClientImpl(
        @Qualifier(DefaultBeanNames.PREFER_V2_AGENT_STATE_CLIENT) AgentStateClient preferV2AgentStateClient,
        @Qualifier(EXECUTE_BEAN_USE_V2_BY_FEATURE_AGENT_STATE_CLIENT)
            AgentStateClient useV2ByFeatureAgentStateClient
    ) {
        return new AutoChoosingAgentStateClientImpl(preferV2AgentStateClient, useV2ByFeatureAgentStateClient);
    }
}
