package com.tencent.bk.job.common.gse.v2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.esb.config.BkApiConfig;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.sdk.AbstractBkApiClient;
import com.tencent.bk.job.common.gse.IGseClient;
import com.tencent.bk.job.common.gse.v2.model.AsyncGseTaskResult;
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

@Component("GseV2ApiClient")
@Slf4j
public class GseV2ApiClient extends AbstractBkApiClient implements IGseClient {

    private static final String URI_ASYNC_EXECUTE_SCRIPT = "/api/v2/task/async_execute_script";
    private static final String URI_GET_EXECUTE_SCRIPT_RESULT = "/api/v2/task/get_execute_script_result";
    private static final String URI_LIST_AGENT_STATE = "/api/v2/cluster/list_agent_state";

    @Autowired
    public GseV2ApiClient(BkApiConfig bkApiConfig) {
        super(bkApiConfig.getBkGseApiGatewayUrl(), bkApiConfig.getAppCode(), bkApiConfig.getAppSecret());
    }

    @Override
    public GseTaskResponse asyncExecuteScript(ExecuteScriptRequest request) {
        log.info("AsyncExecuteScript, request: {}", request);
        EsbResp<AsyncGseTaskResult> resp =
            doHttpPost(URI_ASYNC_EXECUTE_SCRIPT,
                request, new TypeReference<EsbResp<AsyncGseTaskResult>>() {
                });
        GseTaskResponse gseTaskResponse = buildGseTaskResponse(resp);
        log.info("AsyncExecuteScript, resp: {}", gseTaskResponse);

        return gseTaskResponse;
    }

    private GseTaskResponse buildGseTaskResponse(EsbResp<AsyncGseTaskResult> resp) {
        AsyncGseTaskResult asyncGseTaskResult = resp.getData();
        GseTaskResponse gseTaskResponse = new GseTaskResponse();
        gseTaskResponse.setErrorCode(resp.getCode());
        gseTaskResponse.setErrorMessage(resp.getMessage());
        gseTaskResponse.setGseTaskId(asyncGseTaskResult.getResult().getTaskId());
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
    public GseTaskResponse asyncTransferFile(TransferFileRequest request) {
        log.info("AsyncTransferFile, request: {}", request);
        EsbResp<AsyncGseTaskResult> resp =
            doHttpPost("/api/v2/task/async_transfer_file",
                request, new TypeReference<EsbResp<AsyncGseTaskResult>>() {
                }, null);
        GseTaskResponse gseTaskResponse = buildGseTaskResponse(resp);
        log.info("AsyncTransferFile, resp: {}", gseTaskResponse);

        return gseTaskResponse;
    }

    @Override
    public FileTaskResult getTransferFileResult(GetTransferFileResultRequest request) {
        log.info("GetTransferFileResult, request: {}", request);
        EsbResp<FileTaskResult> resp =
            doHttpPost("/api/v2/task/async/get_transfer_file_result",
                request, new TypeReference<EsbResp<FileTaskResult>>() {
                }, null);
        log.info("GetTransferFileResult, resp: {}", resp);
        return resp.getData();
    }

    @Override
    public GseTaskResponse terminateGseFileTask(TerminateGseTaskRequest request) {
        log.info("TerminateGseFileTask, request: {}", request);
        EsbResp<AsyncGseTaskResult> resp =
            doHttpPost("/api/v2/task/async/async_terminate_transfer_file",
                request, new TypeReference<EsbResp<AsyncGseTaskResult>>() {
                }, null);
        GseTaskResponse gseTaskResponse = buildGseTaskResponse(resp);
        log.info("TerminateGseFileTask, resp: {}", gseTaskResponse);
        return gseTaskResponse;
    }

    @Override
    public GseTaskResponse terminateGseScriptTask(TerminateGseTaskRequest request) {
        log.info("TerminateGseScriptTask, request: {}", request);
        EsbResp<AsyncGseTaskResult> resp =
            doHttpPost("/api/v2/task/async/async_terminate_execute_script",
                request, new TypeReference<EsbResp<AsyncGseTaskResult>>() {
                }, null);
        GseTaskResponse gseTaskResponse = buildGseTaskResponse(resp);
        log.info("TerminateGseScriptTask, resp: {}", gseTaskResponse);
        return gseTaskResponse;
    }
}
