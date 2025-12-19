/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.common.gse.v2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.constant.HttpMethodEnum;
import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.BkApiGatewayProperties;
import com.tencent.bk.job.common.esb.model.BkApiAuthorization;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.OpenApiRequestInfo;
import com.tencent.bk.job.common.esb.sdk.BkApiContext;
import com.tencent.bk.job.common.esb.sdk.BkApiLogStrategy;
import com.tencent.bk.job.common.esb.sdk.BkApiV1Client;
import com.tencent.bk.job.common.gse.IGseClient;
import com.tencent.bk.job.common.gse.constants.GseConstants;
import com.tencent.bk.job.common.gse.constants.GseMetricNames;
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
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import com.tencent.bk.job.common.util.http.JobHttpRequestRetryHandler;
import com.tencent.bk.job.common.util.json.JsonUtils;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;

@Slf4j
public class GseV2ApiClient extends BkApiV1Client implements IGseClient {

    private static final String URI_LIST_AGENT_STATE = "/api/v2/cluster/list_agent_state";
    private static final String URI_ASYNC_EXECUTE_SCRIPT = "/api/v2/task/extensions/async_execute_script";
    private static final String URI_GET_EXECUTE_SCRIPT_RESULT = "/api/v2/task/extensions/get_execute_script_result";
    private static final String URI_ASYNC_TRANSFER_FILE = "/api/v2/task/extensions/async_transfer_file";
    private static final String URI_GET_TRANSFER_FILE_RESULT = "/api/v2/task/extensions/get_transfer_file_result";
    private static final String URI_ASYNC_TERMINATE_TRANSFER_FILE =
        "/api/v2/task/extensions/async_terminate_transfer_file";
    private static final String URI_ASYNC_TERMINATE_EXECUTE_SCRIPT =
        "/api/v2/task/extensions/async_terminate_execute_script";
    private final BkApiAuthorization gseBkApiAuthorization;

    public GseV2ApiClient(MeterRegistry meterRegistry,
                          AppProperties appProperties,
                          BkApiGatewayProperties bkApiGatewayProperties,
                          TenantEnvService tenantEnvService) {

        super(meterRegistry,
            GseMetricNames.GSE_V2_API_METRICS_NAME_PREFIX,
            bkApiGatewayProperties.getGse().getUrl(),
            HttpHelperFactory.createHttpHelper(
                15000,
                15000,
                60000,
                1000,
                2000,
                60,
                true,
                new JobHttpRequestRetryHandler(),
                httpClientBuilder -> httpClientBuilder.addInterceptorLast(getLogBkApiRequestIdInterceptor())
            ),
            tenantEnvService
        );
        gseBkApiAuthorization = BkApiAuthorization.appAuthorization(appProperties.getCode(), appProperties.getSecret());
        log.info("Init GseV2ApiClient, bkGseApiGatewayUrl: {}, appCode: {}",
            bkApiGatewayProperties.getGse().getUrl(), appProperties.getCode());
    }

    @Override
    public GseTaskResponse asyncExecuteScript(ExecuteScriptRequest request) {
        EsbResp<AsyncGseTaskResult> resp =
            requestGseApi(
                URI_ASYNC_EXECUTE_SCRIPT,
                request,
                new TypeReference<EsbResp<AsyncGseTaskResult>>() {
                },
                null,
                false);

        return buildGseTaskResponse(resp);
    }

    private <R> EsbResp<R> requestGseApi(String uri,
                                         Object reqBody,
                                         TypeReference<EsbResp<R>> typeReference,
                                         BkApiLogStrategy logStrategy,
                                         boolean isRequestIdempotent) {
        OpenApiRequestInfo<Object> requestInfo = OpenApiRequestInfo
            .builder()
            .method(HttpMethodEnum.POST)
            .uri(uri)
            .addHeader(buildTenantHeader(tenantEnvService.getTenantIdForGSE()))
            .body(reqBody)
            .authorization(gseBkApiAuthorization)
            .setIdempotent(isRequestIdempotent)
            .build();
        return doRequest(requestInfo, typeReference, logStrategy, null);
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
            requestGseApi(
                URI_GET_EXECUTE_SCRIPT_RESULT,
                request,
                new TypeReference<EsbResp<ScriptTaskResult>>() {
                },
                new BkApiLogStrategy() {
                    @Override
                    public <T, R> void logResp(Logger log, BkApiContext<T, R> context) {
                        if (log.isInfoEnabled()) {
                            // 自定义输出，防止脚本任务结果中的执行日志字段过大导致内存溢出
                            log.info("[BaseBkApiClient] Response|bkApiRequestId={}|method={}|uri={}|success={}" +
                                    "|costTime={}|resp={}|",
                                context.getRequestId(),
                                context.getMethod(),
                                context.getUri(),
                                context.isSuccess(),
                                context.getCostTime(),
                                context.getResp() != null ? JsonUtils.toJson(context.getResp()) :
                                    StringUtil.substring(context.getOriginResp(), 10000));
                        }
                    }
                },
                true);
        return resp.getData();
    }


    @Override
    public List<AgentState> listAgentState(ListAgentStateReq req) {
        if (CollectionUtils.isEmpty(req.getAgentIdList())) {
            log.info("agentIdList is empty");
            return Collections.emptyList();
        }
        EsbResp<List<AgentState>> resp = requestGseApi(
            URI_LIST_AGENT_STATE,
            req,
            new TypeReference<EsbResp<List<AgentState>>>() {
            },
            null,
            true);
        return resp.getData();
    }

    @Override
    public GseTaskResponse asyncTransferFile(TransferFileRequest request) {
        EsbResp<AsyncGseTaskResult> resp =
            requestGseApi(
                URI_ASYNC_TRANSFER_FILE,
                request,
                new TypeReference<EsbResp<AsyncGseTaskResult>>() {
                },
                null,
                false);

        return buildGseTaskResponse(resp);
    }

    @Override
    public FileTaskResult getTransferFileResult(GetTransferFileResultRequest request) {
        EsbResp<FileTaskResult> resp =
            requestGseApi(
                URI_GET_TRANSFER_FILE_RESULT,
                request,
                new TypeReference<EsbResp<FileTaskResult>>() {
                },
                null,
                true);
        FileTaskResult fileTaskResult = resp.getData();
        if (fileTaskResult != null && CollectionUtils.isNotEmpty(fileTaskResult.getAtomicFileTaskResults())) {
            fileTaskResult.getAtomicFileTaskResults().forEach(atomicFileTaskResult -> {
                if (atomicFileTaskResult.getContent() != null) {
                    // 由于GSE2.0 删除了protocolVersion，会导致Job解析协议版本出问题；按照Job的设计，对接GSE2.0的才会走BK-GSE-API-GATEWAY, 协议版本必定是V2
                    atomicFileTaskResult.getContent().setProtocolVersion(GseConstants.GSE_FILE_PROTOCOL_VERSION_V2);
                }
            });
        }
        return fileTaskResult;
    }

    @Override
    public GseTaskResponse terminateGseFileTask(TerminateGseTaskRequest request) {
        EsbResp<AsyncGseTaskResult> resp =
            requestGseApi(URI_ASYNC_TERMINATE_TRANSFER_FILE,
                request,
                new TypeReference<EsbResp<AsyncGseTaskResult>>() {
                },
                null,
                true);
        return buildGseTaskResponse(resp);
    }

    @Override
    public GseTaskResponse terminateGseScriptTask(TerminateGseTaskRequest request) {
        EsbResp<AsyncGseTaskResult> resp =
            requestGseApi(URI_ASYNC_TERMINATE_EXECUTE_SCRIPT,
                request,
                new TypeReference<EsbResp<AsyncGseTaskResult>>() {
                },
                null,
                true);
        return buildGseTaskResponse(resp);
    }
}
