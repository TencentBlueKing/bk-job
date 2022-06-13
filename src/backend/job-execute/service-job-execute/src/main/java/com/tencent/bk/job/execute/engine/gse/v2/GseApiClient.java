package com.tencent.bk.job.execute.engine.gse.v2;

import com.tencent.bk.job.execute.engine.gse.v2.model.Agent;
import com.tencent.bk.job.execute.engine.gse.v2.model.ExecuteScriptRequest;
import com.tencent.bk.job.execute.engine.gse.v2.model.ExecuteScriptResult;
import com.tencent.bk.job.execute.engine.gse.v2.model.GetExecuteScriptResultRequest;
import com.tencent.bk.job.execute.engine.model.GseTaskResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GseApiClient implements IGseClient {
    @Override
    public GseTaskResponse asyncExecuteScript(ExecuteScriptRequest request) {
        log.info("AsyncExecuteScript, request: {}", request);
        return null;

    }

    @Override
    public ExecuteScriptResult getExecuteScriptResult(GetExecuteScriptResultRequest request) {
        return null;
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
