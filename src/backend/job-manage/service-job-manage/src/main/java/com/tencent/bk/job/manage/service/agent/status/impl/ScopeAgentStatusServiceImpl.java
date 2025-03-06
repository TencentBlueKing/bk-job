package com.tencent.bk.job.manage.service.agent.status.impl;

import com.tencent.bk.job.common.gse.IGseClient;
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

    private final IGseClient gseClient;

    public ScopeAgentStatusServiceImpl(@Autowired(required = false) IGseClient gseClient) {
        this.gseClient = gseClient;
    }

    @Override
    public List<AgentState> listAgentState(ResourceScope resourceScope, ListAgentStateReq req) {
        return gseClient.listAgentState(req);
    }

}
