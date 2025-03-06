package com.tencent.bk.job.manage.service.agent.status.impl;

import com.tencent.bk.job.common.gse.v2.GseV2ApiClient;
import com.tencent.bk.job.common.gse.v2.model.req.ListAgentStateReq;
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.manage.service.agent.status.ScopeAgentStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ScopeAgentStatusServiceImpl implements ScopeAgentStatusService {

    private final GseV2ApiClient gseV2ApiClient;

    public ScopeAgentStatusServiceImpl(@Autowired(required = false) GseV2ApiClient gseV2ApiClient) {
        this.gseV2ApiClient = gseV2ApiClient;
    }

    @Override
    public List<AgentState> listAgentState(ResourceScope resourceScope, ListAgentStateReq req) {
        return gseV2ApiClient.listAgentState(req);
    }

}
