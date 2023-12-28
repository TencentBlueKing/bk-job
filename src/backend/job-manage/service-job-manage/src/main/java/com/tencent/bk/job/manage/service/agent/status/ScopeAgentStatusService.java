package com.tencent.bk.job.manage.service.agent.status;

import com.tencent.bk.job.common.gse.v2.model.req.ListAgentStateReq;
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;
import com.tencent.bk.job.common.model.dto.ResourceScope;

import java.util.List;

public interface ScopeAgentStatusService {

    boolean needToUseGseV2(ResourceScope resourceScope);

    List<AgentState> listAgentState(ResourceScope resourceScope, ListAgentStateReq req);

}
