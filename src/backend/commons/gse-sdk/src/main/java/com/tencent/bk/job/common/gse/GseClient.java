/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;

import java.util.Collections;
import java.util.List;


@Slf4j
public class GseClient implements IGseClient {

    private final GseV1ApiClient gseV1ApiClient;
    private final GseV2ApiClient gseV2ApiClient;


    public GseClient(GseV1ApiClient gseV1ApiClient,
                     GseV2ApiClient gseV2ApiClient) {
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
        StopWatch watch = new StopWatch("listAgentState");
        List<String> agentIdList = req.getAgentIdList();
        if (CollectionUtils.isEmpty(agentIdList)) {
            log.info("agentIdList is empty");
            return Collections.emptyList();
        }
        String firstAgentId = agentIdList.get(0);

        watch.start("chooseGseApiClientByAgentId");
        IGseClient gseClient = chooseGseApiClientByAgentId(firstAgentId);
        watch.stop();

        watch.start("gseClient.listAgentState");
        List<AgentState> resultList = gseClient.listAgentState(req);
        watch.stop();

        if (watch.getTotalTimeMillis() > 3000) {
            log.warn("listAgentState slow, statistics: " + watch.prettyPrint());
        }

        return resultList;
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
