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

import brave.Tracing;
import com.tencent.bk.gse.taskapi.api_agent;
import com.tencent.bk.gse.taskapi.api_stop_task_request;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.engine.gse.GseRequestUtils;
import com.tencent.bk.job.execute.engine.model.GseTaskResponse;
import com.tencent.bk.job.execute.model.AccountDTO;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.AccountService;
import com.tencent.bk.job.execute.service.AgentService;
import com.tencent.bk.job.execute.service.AgentTaskService;
import com.tencent.bk.job.execute.service.GseTaskService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class FileGseTaskStopCommand extends AbstractGseTaskCommand {

    public FileGseTaskStopCommand(AgentService agentService,
                                  AccountService accountService,
                                  GseTaskService gseTaskService,
                                  AgentTaskService agentTaskService,
                                  Tracing tracing,
                                  TaskInstanceDTO taskInstance,
                                  StepInstanceDTO stepInstance,
                                  GseTaskDTO gseTask) {
        super(agentService,
            accountService,
            gseTaskService,
            agentTaskService,
            tracing,
            taskInstance,
            stepInstance,
            gseTask);
    }

    @Override
    public void execute() {
        log.info("Stop gse task, gseTask:" + gseTaskUniqueName);
        List<AgentTaskDTO> agentTasks;
        if (gseTask.getId() != null) {
            agentTasks = agentTaskService.listAgentTasksByGseTaskId(gseTask.getId());
        } else {
            // tmp: 兼容旧的调度任务，发布完成后删除
            agentTasks = agentTaskService.listAgentTasks(gseTask.getStepInstanceId(),
                gseTask.getExecuteCount(), gseTask.getBatch());
        }
        AccountDTO targetAccount = getAccountBean(stepInstance.getAccountId(), stepInstance.getAccount(),
            stepInstance.getAppId());
        Set<String> agentIds = agentTasks.stream()
            .map(AgentTaskDTO::getAgentId)
            .collect(Collectors.toSet());
        //目标机器的agent
        List<api_agent> agentList = GseRequestUtils.buildAgentList(agentIds, targetAccount.getAccount(),
            targetAccount.getPassword());

        api_stop_task_request stopTaskRequest = new api_stop_task_request();
        stopTaskRequest.setStop_task_id(gseTask.getGseTaskId());
        stopTaskRequest.setAgents(agentList);
        stopTaskRequest.setType(StepExecuteTypeEnum.SEND_FILE.getValue());
        stopTaskRequest.setM_caller(buildGSETraceInfo());

        GseTaskResponse gseTaskResponse = GseRequestUtils.sendForceStopTaskRequest(stepInstance.getId(),
            stopTaskRequest);
        if (GseTaskResponse.ERROR_CODE_SUCCESS != gseTaskResponse.getErrorCode()) {
            log.error("Terminate gse task failed! gseTask: {}", gseTaskUniqueName);
        } else {
            log.info("Terminate gse task response success! gseTask: {}", gseTaskUniqueName);
            gseTask.setStatus(RunStatusEnum.STOPPING.getValue());
            gseTaskService.updateGseTask(gseTask);
        }
    }
}
