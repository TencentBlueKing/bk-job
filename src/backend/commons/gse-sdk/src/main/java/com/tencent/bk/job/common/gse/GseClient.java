package com.tencent.bk.job.common.gse;

import com.tencent.bk.job.common.gse.util.AgentUtils;
import com.tencent.bk.job.common.gse.v1.GseV1ApiClient;
import com.tencent.bk.job.common.gse.v2.GseV2ApiClient;
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
        return chooseGseApiClientByAgentId(request.getAgents().get(0).getAgentId()).asyncExecuteScript(request);
    }

    private IGseClient chooseGseApiClientByAgentId(String agentId) {
        if (AgentUtils.isGseV1AgentId(agentId)) {
            log.debug("Choose GseV1ApiClient, agentId: {}", agentId);
            return gseV1ApiClient;
        } else {
            log.debug("Choose GseV2ApiClient, agent: {}", agentId);
            return gseV2APIClient;
        }
    }

    private IGseClient chooseGseApiClientByGseTaskId(String gseTaskId) {
        if (gseTaskId.startsWith("GSE:V2")) {
            return gseV2APIClient;
        } else {
            return gseV1ApiClient;
        }
    }

    @Override
    public ScriptTaskResult getExecuteScriptResult(GetExecuteScriptResultRequest request) {
        return chooseGseApiClientByGseTaskId(request.getTaskId()).getExecuteScriptResult(request);
    }

    @Override
    public List<AgentState> listAgentState(ListAgentStateReq req) {
        return chooseGseApiClientByAgentId(req.getAgentIdList().get(0)).listAgentState(req);
    }

    @Override
    public GseTaskResponse asyncTransferFile(TransferFileRequest request) {
        return chooseGseApiClientByAgentId(request.getTasks().get(0).getTarget().getAgents().get(0).getAgentId())
            .asyncTransferFile(request);
    }

    @Override
    public FileTaskResult getTransferFileResult(GetTransferFileResultRequest request) {
        return chooseGseApiClientByGseTaskId(request.getTaskId()).getTransferFileResult(request);
    }

    @Override
    public GseTaskResponse terminateGseFileTask(TerminateGseTaskRequest request) {
        return chooseGseApiClientByGseTaskId(request.getTaskId()).terminateGseFileTask(request);
    }

    @Override
    public GseTaskResponse terminateGseScriptTask(TerminateGseTaskRequest request) {
        return chooseGseApiClientByGseTaskId(request.getTaskId()).terminateGseScriptTask(request);
    }
}
