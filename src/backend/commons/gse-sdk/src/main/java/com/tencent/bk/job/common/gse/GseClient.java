package com.tencent.bk.job.common.gse;

import com.tencent.bk.job.common.gse.config.GseProperties;
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
    private final GseProperties gseProperties;


    public GseClient(@Autowired(required = false) GseV1ApiClient gseV1ApiClient,
                     @Autowired(required = false) GseV2ApiClient gseV2APIClient,
                     @Autowired GseProperties gseProperties) {
        this.gseV1ApiClient = gseV1ApiClient;
        this.gseV2APIClient = gseV2APIClient;
        this.gseProperties = gseProperties;
    }


    @Override
    public GseTaskResponse asyncExecuteScript(ExecuteScriptRequest request) {
        return getGseApiClient().asyncExecuteScript(request);
    }

    private IGseClient getGseApiClient() {
        if (gseProperties.getVersion().equals("v2")) {
            return gseV2APIClient;
        } else {
            return gseV1ApiClient;
        }
    }

    @Override
    public ScriptTaskResult getExecuteScriptResult(GetExecuteScriptResultRequest request) {
        return getGseApiClient().getExecuteScriptResult(request);
    }

    @Override
    public List<AgentState> listAgentState(ListAgentStateReq req) {
        return getGseApiClient().listAgentState(req);
    }

    @Override
    public GseTaskResponse asyncTransferFile(TransferFileRequest request) {
        return getGseApiClient().asyncTransferFile(request);
    }

    @Override
    public FileTaskResult getTransferFileResult(GetTransferFileResultRequest request) {
        return getGseApiClient().getTransferFileResult(request);
    }

    @Override
    public GseTaskResponse terminateGseFileTask(TerminateGseTaskRequest request) {
        return getGseApiClient().terminateGseFileTask(request);
    }

    @Override
    public GseTaskResponse terminateGseScriptTask(TerminateGseTaskRequest request) {
        return getGseApiClient().terminateGseScriptTask(request);
    }
}
