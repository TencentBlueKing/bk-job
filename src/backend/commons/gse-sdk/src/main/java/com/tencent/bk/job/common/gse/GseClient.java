package com.tencent.bk.job.common.gse;

import com.tencent.bk.job.common.gse.v1.GseV1ApiClient;
import com.tencent.bk.job.common.gse.v2.GseV2ApiClient;
import com.tencent.bk.job.common.gse.v2.model.Agent;
import com.tencent.bk.job.common.gse.v2.model.ExecuteScriptRequest;
import com.tencent.bk.job.common.gse.v2.model.FileTaskResult;
import com.tencent.bk.job.common.gse.v2.model.GetExecuteScriptResultRequest;
import com.tencent.bk.job.common.gse.v2.model.GetTransferFileResultRequest;
import com.tencent.bk.job.common.gse.v2.model.GseTaskResponse;
import com.tencent.bk.job.common.gse.v2.model.ScriptTaskResult;
import com.tencent.bk.job.common.gse.v2.model.TerminateGseTaskRequest;
import com.tencent.bk.job.common.gse.v2.model.TransferFileRequest;
import com.tencent.bk.job.common.gse.v2.model.req.ListAgentStateReq;
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component("GseApiClient")
@Slf4j
public class GseClient implements IGseClient {

    private final GseV1ApiClient gseV1ApiClient;
    private final GseV2ApiClient gseV2APIClient;


    public GseClient(@Autowired(required = false) GseV1ApiClient gseV1ApiClient,
                     @Autowired(required = false) GseV2ApiClient gseV2APIClient) {
        this.gseV1ApiClient = gseV1ApiClient;
        this.gseV2APIClient = gseV2APIClient;
    }


    @Override
    public GseTaskResponse asyncExecuteScript(ExecuteScriptRequest request) {
        return null;
    }

    @Override
    public ScriptTaskResult getExecuteScriptResult(GetExecuteScriptResultRequest request) {
        return null;
    }

    @Override
    public List<AgentState> listAgentState(ListAgentStateReq req) {
        return null;
    }

    @Override
    public List<Agent> buildAgents(Collection<String> agentIds, String user, String password) {
        return null;
    }

    @Override
    public Agent buildAgent(String agentId, String user, String password) {
        return null;
    }

    @Override
    public GseTaskResponse asyncTransferFile(TransferFileRequest request) {
        return null;
    }

    @Override
    public FileTaskResult getTransferFileResult(GetTransferFileResultRequest request) {
        return null;
    }

    @Override
    public GseTaskResponse terminateGseFileTask(TerminateGseTaskRequest request) {
        return null;
    }

    @Override
    public GseTaskResponse terminateGseScriptTask(TerminateGseTaskRequest request) {
        return null;
    }
}
