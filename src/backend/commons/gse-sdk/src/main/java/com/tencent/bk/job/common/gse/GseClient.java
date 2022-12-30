package com.tencent.bk.job.common.gse;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.gse.util.AgentUtils;
import com.tencent.bk.job.common.gse.v1.GseV1ApiClient;
import com.tencent.bk.job.common.gse.v2.GseV2ApiClient;
import com.tencent.bk.job.common.gse.v2.model.ExecuteScriptRequest;
import com.tencent.bk.job.common.gse.v2.model.FileTaskResult;
import com.tencent.bk.job.common.gse.v2.model.GetExecuteScriptResultRequest;
import com.tencent.bk.job.common.gse.v2.model.GetTransferFileResultRequest;
import com.tencent.bk.job.common.gse.v2.model.GseReq;
import com.tencent.bk.job.common.gse.v2.model.GseTaskResponse;
import com.tencent.bk.job.common.gse.v2.model.ScriptTaskResult;
import com.tencent.bk.job.common.gse.v2.model.TerminateGseTaskRequest;
import com.tencent.bk.job.common.gse.v2.model.TransferFileRequest;
import com.tencent.bk.job.common.gse.v2.model.req.ListAgentStateReq;
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("GseApiClient")
@Slf4j
public class GseClient implements IGseClient {

    private final GseV1ApiClient gseV1ApiClient;
    private final GseV2ApiClient gseV2ApiClient;


    public GseClient(@Autowired(required = false) GseV1ApiClient gseV1ApiClient,
                     @Autowired(required = false) GseV2ApiClient gseV2ApiClient) {
        this.gseV1ApiClient = gseV1ApiClient;
        this.gseV2ApiClient = gseV2ApiClient;
        log.info("Init gseClient, gseV1ApiClient: {}, gseV2ApiClient: {}", gseV1ApiClient, gseV2ApiClient);
    }


    @Override
    public GseTaskResponse asyncExecuteScript(ExecuteScriptRequest request) {
        return chooseGseApiClientByGseTaskVersion(request).asyncExecuteScript(request);
    }

    private IGseClient chooseGseApiClientByGseTaskVersion(GseReq gseReq) {
        if (gseReq.isGseV2Task()) {
            return gseV2ApiClient;
        } else {
            return gseV1ApiClient;
        }
    }

    @Override
    public ScriptTaskResult getExecuteScriptResult(GetExecuteScriptResultRequest request) {
        return chooseGseApiClientByGseTaskVersion(request).getExecuteScriptResult(request);
    }

    @Override
    public List<AgentState> listAgentState(ListAgentStateReq req) {
        return chooseGseApiClientByAgentId(req.getAgentIdList().get(0)).listAgentState(req);
    }

    private IGseClient chooseGseApiClientByAgentId(String agentId) {
        if (StringUtils.isEmpty(agentId)) {
            log.error("Empty agentId!");
            throw new InternalException("AgentId is empty", ErrorCode.INTERNAL_ERROR);
        }
        if (AgentUtils.isGseV1AgentId(agentId)) {
            log.debug("Choose GseV1ApiClient, agentId: {}", agentId);
            return gseV1ApiClient;
        } else {
            log.debug("Choose GseV2ApiClient, agentId: {}", agentId);
            return gseV2ApiClient;
        }
    }

    @Override
    public GseTaskResponse asyncTransferFile(TransferFileRequest request) {
        return chooseGseApiClientByGseTaskVersion(request).asyncTransferFile(request);
    }

    @Override
    public FileTaskResult getTransferFileResult(GetTransferFileResultRequest request) {
        return chooseGseApiClientByGseTaskVersion(request).getTransferFileResult(request);
    }

    @Override
    public GseTaskResponse terminateGseFileTask(TerminateGseTaskRequest request) {
        return chooseGseApiClientByGseTaskVersion(request).terminateGseFileTask(request);
    }

    @Override
    public GseTaskResponse terminateGseScriptTask(TerminateGseTaskRequest request) {
        return chooseGseApiClientByGseTaskVersion(request).terminateGseScriptTask(request);
    }
}
