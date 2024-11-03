package com.tencent.bk.job.manage.service.agent.status.impl;

import com.tencent.bk.job.common.gse.IGseClient;
import com.tencent.bk.job.common.gse.v1.GseV1ApiClient;
import com.tencent.bk.job.common.gse.v2.GseV2ApiClient;
import com.tencent.bk.job.common.gse.v2.model.req.ListAgentStateReq;
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.util.toggle.ToggleEvaluateContext;
import com.tencent.bk.job.common.util.toggle.ToggleStrategyContextParams;
import com.tencent.bk.job.common.util.toggle.feature.FeatureIdConstants;
import com.tencent.bk.job.common.util.toggle.feature.FeatureToggle;
import com.tencent.bk.job.manage.service.agent.status.ScopeAgentStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ScopeAgentStatusServiceImpl implements ScopeAgentStatusService {

    private final GseV1ApiClient gseV1ApiClient;
    private final GseV2ApiClient gseV2ApiClient;

    public ScopeAgentStatusServiceImpl(@Autowired(required = false) GseV1ApiClient gseV1ApiClient,
                                       @Autowired(required = false) GseV2ApiClient gseV2ApiClient) {
        this.gseV1ApiClient = gseV1ApiClient;
        this.gseV2ApiClient = gseV2ApiClient;
    }

    @Override
    public boolean needToUseGseV2(ResourceScope resourceScope) {
        ToggleEvaluateContext featureExecutionContext =
            ToggleEvaluateContext.builder()
                .addContextParam(ToggleStrategyContextParams.CTX_PARAM_RESOURCE_SCOPE, resourceScope);
        return FeatureToggle.checkFeature(
            FeatureIdConstants.FEATURE_AGENT_STATUS_GSE_V2,
            featureExecutionContext
        );
    }

    @Override
    public List<AgentState> listAgentState(ResourceScope resourceScope, ListAgentStateReq req) {
        IGseClient gseClient = chooseGseApiClientByResourceScope(resourceScope);
        return gseClient.listAgentState(req);
    }

    private IGseClient chooseGseApiClientByResourceScope(ResourceScope resourceScope) {
        return needToUseGseV2(resourceScope) ? gseV2ApiClient : gseV1ApiClient;
    }

}
