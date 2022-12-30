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

package com.tencent.bk.job.execute.engine.executor;

import com.tencent.bk.job.common.gse.GseClient;
import com.tencent.bk.job.common.gse.v2.model.GseTaskResponse;
import com.tencent.bk.job.common.gse.v2.model.TerminateGseTaskRequest;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.AccountService;
import com.tencent.bk.job.execute.service.AgentService;
import com.tencent.bk.job.execute.service.AgentTaskService;
import com.tencent.bk.job.execute.service.GseTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Tracer;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ScriptGseTaskStopCommand extends AbstractGseTaskCommand {

    public ScriptGseTaskStopCommand(AgentService agentService,
                                    AccountService accountService,
                                    GseTaskService gseTaskService,
                                    AgentTaskService agentTaskService,
                                    Tracer tracer,
                                    GseClient gseClient,
                                    TaskInstanceDTO taskInstance,
                                    StepInstanceDTO stepInstance,
                                    GseTaskDTO gseTask) {
        super(agentService,
            accountService,
            gseTaskService,
            agentTaskService,
            tracer,
            gseClient,
            taskInstance,
            stepInstance,
            gseTask);
    }

    @Override
    public void execute() {
        log.info("Stop gse script task, gseTask:" + gseTaskUniqueName);
        List<AgentTaskDTO> agentTasks = agentTaskService.listAgentTasksByGseTaskId(gseTask.getId());
        List<String> terminateAgentIds = agentTasks.stream()
            .map(AgentTaskDTO::getAgentId)
            .distinct()
            .collect(Collectors.toList());


        TerminateGseTaskRequest request = new TerminateGseTaskRequest(gseTask.getGseTaskId(), terminateAgentIds);
        request.setGseV2Task(gseV2Task);
        GseTaskResponse gseTaskResponse = gseClient.terminateGseScriptTask(request);
        if (GseTaskResponse.ERROR_CODE_SUCCESS != gseTaskResponse.getErrorCode()) {
            log.error("Terminate gse task failed! gseTask: {}", gseTaskUniqueName);
        } else {
            log.info("Terminate gse task response success!");
            gseTask.setStatus(RunStatusEnum.STOPPING.getValue());
            gseTaskService.updateGseTask(gseTask);
        }
    }
}
