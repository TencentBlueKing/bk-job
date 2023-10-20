package com.tencent.bk.job.manage.config;

import com.tencent.bk.job.common.gse.GseClient;
import com.tencent.bk.job.common.gse.config.AgentStateQueryConfig;
import com.tencent.bk.job.common.gse.constants.DefaultBeanNames;
import com.tencent.bk.job.common.gse.service.AgentStateClient;
import com.tencent.bk.job.common.gse.service.BizHostInfoQueryService;
import com.tencent.bk.job.common.gse.service.UseV2ByFeatureAgentStateClientImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration(value = "jobManageGseConfig")
public class GseConfig {

    @Primary
    @ConditionalOnProperty(name = "job.features.agentStatusGseV2.enabled", havingValue = "true")
    @Bean("UseV2ByFeatureAgentStateClient")
    public AgentStateClient useV2ByFeatureAgentStateClient(AgentStateQueryConfig agentStateQueryConfig,
                                                           GseClient gseClient,
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
}
