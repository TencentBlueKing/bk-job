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

package com.tencent.bk.job.common.gse.mock;

import com.tencent.bk.job.common.gse.IGseClient;
import com.tencent.bk.job.common.gse.v2.model.ExecuteScriptRequest;
import com.tencent.bk.job.common.gse.v2.model.FileTaskResult;
import com.tencent.bk.job.common.gse.v2.model.GetExecuteScriptResultRequest;
import com.tencent.bk.job.common.gse.v2.model.GetTransferFileResultRequest;
import com.tencent.bk.job.common.gse.v2.model.GseTaskResponse;
import com.tencent.bk.job.common.gse.v2.model.ScriptExecuteObjectTaskResult;
import com.tencent.bk.job.common.gse.v2.model.ScriptTaskResult;
import com.tencent.bk.job.common.gse.v2.model.TerminateGseTaskRequest;
import com.tencent.bk.job.common.gse.v2.model.TransferFileRequest;
import com.tencent.bk.job.common.gse.v2.model.req.ListAgentStateReq;
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;

import java.util.ArrayList;
import java.util.List;

public class MockGseV2Client implements IGseClient {
    @Override
    public GseTaskResponse asyncExecuteScript(ExecuteScriptRequest request) {
        GseTaskResponse response = new GseTaskResponse();
        response.setGseTaskId("MockTaskId1:" + request.getAgents().get(0).getAgentId());
        response.setErrorCode(0);
        response.setErrorMessage("OK");
        return response;
    }

    @Override
    public ScriptTaskResult getExecuteScriptResult(GetExecuteScriptResultRequest request) {
        ScriptTaskResult scriptTaskResult = new ScriptTaskResult();
        List<ScriptExecuteObjectTaskResult> resultList = new ArrayList<>();
        ScriptExecuteObjectTaskResult scriptExecuteObjectTaskResult = new ScriptExecuteObjectTaskResult();
        scriptExecuteObjectTaskResult.setAgentId(request.getAgentTasks().get(0).getAgentId());
        scriptExecuteObjectTaskResult.setStatus(2);
        scriptExecuteObjectTaskResult.setErrorCode(0);
        scriptExecuteObjectTaskResult.setErrorMsg("OK");
        scriptExecuteObjectTaskResult.setStartTime(0L);
        scriptExecuteObjectTaskResult.setEndTime(100L);
        scriptExecuteObjectTaskResult.setExitCode(0);
        scriptExecuteObjectTaskResult.setTag("MockTag1");
        scriptExecuteObjectTaskResult.setScreen("Hello World");
        scriptExecuteObjectTaskResult.setAtomicTaskId(0);
        scriptExecuteObjectTaskResult.setContentLength(11);
        resultList.add(scriptExecuteObjectTaskResult);
        scriptTaskResult.setResult(resultList);
        return scriptTaskResult;
    }

    @Override
    public List<AgentState> listAgentState(ListAgentStateReq req) {
        List<AgentState> agentStateList = new ArrayList<>();
        AgentState agentState = new AgentState();
        agentState.setAgentId("MockAgentId1");
        agentState.setCloudId(1);
        agentState.setVersion("MockVersion1");
        agentState.setRunMode(1);
        agentState.setStatusCode(2);
        agentStateList.add(agentState);
        return agentStateList;
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
