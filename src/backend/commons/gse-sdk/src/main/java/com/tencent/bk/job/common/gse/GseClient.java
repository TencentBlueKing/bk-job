package com.tencent.bk.job.common.gse;

import com.tencent.bk.job.common.gse.v1.GseV1ApiClient;
import com.tencent.bk.job.common.gse.v2.GseV2ApiClient;
import com.tencent.bk.job.common.gse.v2.model.ExecuteScriptRequest;
import com.tencent.bk.job.common.gse.v2.model.FileTaskResult;
import com.tencent.bk.job.common.gse.v2.model.GetExecuteScriptResultRequest;
import com.tencent.bk.job.common.gse.v2.model.GetTransferFileResultRequest;
import com.tencent.bk.job.common.gse.v2.model.GseExecutionContext;
import com.tencent.bk.job.common.gse.v2.model.GseTaskResponse;
import com.tencent.bk.job.common.gse.v2.model.ScriptTaskResult;
import com.tencent.bk.job.common.gse.v2.model.TerminateGseTaskRequest;
import com.tencent.bk.job.common.gse.v2.model.TransferFileRequest;
import com.tencent.bk.job.common.gse.v2.model.req.ListAgentStateReq;
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;
import com.tencent.bk.job.common.util.feature.FeatureExecutionContext;
import com.tencent.bk.job.common.util.feature.FeatureToggle;
import com.tencent.bk.job.common.util.feature.ResourceScopeToggleStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        return getGseApiClient(request.getContext()).asyncExecuteScript(request);
    }

    private IGseClient getGseApiClient(GseExecutionContext gseExecutionContext) {
        FeatureExecutionContext featureExecutionContext = null;
        if (gseExecutionContext != null) {
            Map<String, Object> params = new HashMap<>();
            if (gseExecutionContext.getResourceScope() != null) {
                params.put(ResourceScopeToggleStrategy.CTX_PARAM_RESOURCE_SCOPE,
                    gseExecutionContext.getResourceScope());
            }
            featureExecutionContext = new FeatureExecutionContext(params);
        }
        if (FeatureToggle.getInstance().checkFeature(FeatureToggle.FEATURE_GSE_V2, featureExecutionContext)) {
            log.debug("Choose GseV2ApiClient, gseExecutionContext: {}", gseExecutionContext);
            return gseV2APIClient;
        } else {
            log.debug("Choose GseV1ApiClient, gseExecutionContext: {}", gseExecutionContext);
            return gseV1ApiClient;
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
        return getGseApiClient(req.getContext()).listAgentState(req);
    }

    @Override
    public GseTaskResponse asyncTransferFile(TransferFileRequest request) {
        return getGseApiClient(request.getContext()).asyncTransferFile(request);
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
