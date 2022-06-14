package com.tencent.bk.job.common.gse.v2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.esb.config.BkApiConfig;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.sdk.AbstractBkApiClient;
import com.tencent.bk.job.common.gse.model.GseTaskResponse;
import com.tencent.bk.job.common.gse.v2.model.Agent;
import com.tencent.bk.job.common.gse.v2.model.AsyncExecuteScriptResult;
import com.tencent.bk.job.common.gse.v2.model.ExecuteScriptRequest;
import com.tencent.bk.job.common.gse.v2.model.GetExecuteScriptResultRequest;
import com.tencent.bk.job.common.gse.v2.model.ScriptTaskResult;
import com.tencent.bk.job.common.gse.v2.model.req.ListAgentStateReq;
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GseApiClient extends AbstractBkApiClient implements IGseClient {

    private static final String URI_ASYNC_EXECUTE_SCRIPT = "/api/v2/task/async_execute_script";
    private static final String URI_GET_EXECUTE_SCRIPT_RESULT = "/api/v2/task/get_execute_script_result";
    private static final String URI_LIST_AGENT_STATE = "/api/v2/cluster/list_agent_state";

    @Autowired
    public GseApiClient(BkApiConfig bkApiConfig) {
        super(bkApiConfig.getBkGseApiGatewayUrl(), bkApiConfig.getAppCode(), bkApiConfig.getAppSecret());
    }

    @Override
    public GseTaskResponse asyncExecuteScript(ExecuteScriptRequest request) {
        log.info("AsyncExecuteScript, request: {}", request);
        EsbResp<AsyncExecuteScriptResult> resp =
            doHttpPost(URI_ASYNC_EXECUTE_SCRIPT,
                request, new TypeReference<EsbResp<AsyncExecuteScriptResult>>() {
                });
        AsyncExecuteScriptResult asyncExecuteScriptResult = resp.getData();
        GseTaskResponse gseTaskResponse = new GseTaskResponse();
        gseTaskResponse.setErrorCode(resp.getCode());
        gseTaskResponse.setErrorMessage(resp.getMessage());
        gseTaskResponse.setGseTaskId(asyncExecuteScriptResult.getResult().getTaskId());

        log.info("AsyncExecuteScript, resp: {}", gseTaskResponse);

        return gseTaskResponse;

    }

    @Override
    public ScriptTaskResult getExecuteScriptResult(GetExecuteScriptResultRequest request) {
        log.info("GetExecuteScriptResult, request: {}", request);
        EsbResp<ScriptTaskResult> resp =
            doHttpPost(URI_GET_EXECUTE_SCRIPT_RESULT,
                request, new TypeReference<EsbResp<ScriptTaskResult>>() {
                });
        log.info("GetExecuteScriptResult, resp: {}", resp);
        return resp.getData();
    }

    @Override
    public List<AgentState> listAgentState(ListAgentStateReq req) {
        log.info("listAgentState, req: {}", req);
        EsbResp<List<AgentState>> resp = doHttpPost(URI_LIST_AGENT_STATE,
            req, new TypeReference<EsbResp<List<AgentState>>>() {
            });
        log.info("listAgentState, resp: {}", resp);
        return resp.getData();
    }

    @Override
    public List<Agent> buildAgents(Collection<String> agentIds, String user, String password) {
        return agentIds.stream()
            .map(agentId -> {
                Agent agent = new Agent();
                agent.setAgentId(agentId);
                agent.setUser(user);
                agent.setPwd(password);
                return agent;
            }).collect(Collectors.toList());
    }


}
