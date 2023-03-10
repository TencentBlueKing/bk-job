package com.tencent.bk.job.common.gse.v2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.esb.config.BkApiConfig;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.sdk.AbstractBkApiClient;
import com.tencent.bk.job.common.esb.sdk.BkApiContext;
import com.tencent.bk.job.common.esb.sdk.BkApiLogStrategy;
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
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("GseV2ApiClient")
@Slf4j
public class GseV2ApiClient extends AbstractBkApiClient implements IGseClient {

    private static final String URI_ASYNC_EXECUTE_SCRIPT = "/api/v2/task/async_execute_script";
    private static final String URI_GET_EXECUTE_SCRIPT_RESULT = "/api/v2/task/get_execute_script_result";
    private static final String URI_LIST_AGENT_STATE = "/api/v2/cluster/list_agent_state";
    private static final String URI_ASYNC_TRANSFER_FILE = "/api/v2/task/async_transfer_file";
    private static final String URI_GET_TRANSFER_FILE_RESULT = "/api/v2/task/async/get_transfer_file_result";

    @Autowired
    public GseV2ApiClient(BkApiConfig bkApiConfig) {
        super(bkApiConfig.getBkGseApiGatewayUrl(), bkApiConfig.getAppCode(), bkApiConfig.getAppSecret());
        log.info("Init GseV2ApiClient, bkGseApiGatewayUrl: {}, appCode: {}",
            bkApiConfig.getBkGseApiGatewayUrl(), bkApiConfig.getAppCode());
    }

    @Override
    public GseTaskResponse asyncExecuteScript(ExecuteScriptRequest request) {
        EsbResp<AsyncGseTaskResult> resp =
            doHttpPost(URI_ASYNC_EXECUTE_SCRIPT,
                request, new TypeReference<EsbResp<AsyncGseTaskResult>>() {
                });

        return buildGseTaskResponse(resp);
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
        EsbResp<ScriptTaskResult> resp =
            doHttpPost(URI_GET_EXECUTE_SCRIPT_RESULT,
                request,
                new TypeReference<EsbResp<ScriptTaskResult>>() {
                },
                null,
                new BkApiLogStrategy() {
                    @Override
                    public <T, R> void logResp(Logger log, BkApiContext<T, R> context) {
                        if (log.isInfoEnabled()) {
                            // 自定义输出，防止脚本任务结果中的执行日志字段过大导致内存溢出
                            log.info("[AbstractBkApiClient] Response|method={}|uri={}|success={}|costTime={}|resp={}|",
                                context.getMethod(),
                                context.getUri(),
                                context.isSuccess(),
                                context.getCostTime(),
                                context.getResp() != null ? JsonUtils.toJsonWithoutSkippedFields(context.getResp()) :
                                    StringUtil.substring(context.getOriginResp(), 10000));
                        }
                    }
                });
        return resp.getData();
    }


    @Override
    public List<AgentState> listAgentState(ListAgentStateReq req) {
        EsbResp<List<AgentState>> resp = doHttpPost(URI_LIST_AGENT_STATE,
            req, new TypeReference<EsbResp<List<AgentState>>>() {
            });
        return resp.getData();
    }

    @Override
    public GseTaskResponse asyncTransferFile(TransferFileRequest request) {
        EsbResp<AsyncGseTaskResult> resp =
            doHttpPost(URI_ASYNC_TRANSFER_FILE,
                request, new TypeReference<EsbResp<AsyncGseTaskResult>>() {
                }, null);

        return buildGseTaskResponse(resp);
    }

    @Override
    public FileTaskResult getTransferFileResult(GetTransferFileResultRequest request) {
        EsbResp<FileTaskResult> resp =
            doHttpPost(URI_GET_TRANSFER_FILE_RESULT,
                request, new TypeReference<EsbResp<FileTaskResult>>() {
                }, null);
        return resp.getData();
    }

    @Override
    public GseTaskResponse terminateGseFileTask(TerminateGseTaskRequest request) {
        EsbResp<AsyncGseTaskResult> resp =
            doHttpPost("/api/v2/task/async_terminate_transfer_file",
                request, new TypeReference<EsbResp<AsyncGseTaskResult>>() {
                }, null);
        return buildGseTaskResponse(resp);
    }

    @Override
    public GseTaskResponse terminateGseScriptTask(TerminateGseTaskRequest request) {
        EsbResp<AsyncGseTaskResult> resp =
            doHttpPost("/api/v2/task/async_terminate_execute_script",
                request, new TypeReference<EsbResp<AsyncGseTaskResult>>() {
                }, null);
        return buildGseTaskResponse(resp);
    }
}
