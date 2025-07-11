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

package com.tencent.bk.job.execute.engine.executor;

import com.tencent.bk.job.common.gse.v2.model.Agent;
import com.tencent.bk.job.common.gse.v2.model.GseTaskResponse;
import com.tencent.bk.job.common.gse.v2.model.TerminateGseTaskRequest;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.engine.EngineDependentServiceHolder;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.ExecuteObjectTaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ScriptGseTaskStopCommand extends AbstractGseTaskCommand {

    public ScriptGseTaskStopCommand(EngineDependentServiceHolder engineDependentServiceHolder,
                                    ExecuteObjectTaskService executeObjectTaskService,
                                    TaskInstanceDTO taskInstance,
                                    StepInstanceDTO stepInstance,
                                    GseTaskDTO gseTask) {
        super(engineDependentServiceHolder,
            executeObjectTaskService,
            taskInstance,
            stepInstance,
            gseTask);
    }

    @Override
    public void execute() {
        log.info("Stop gse script task, gseTask:" + gseTaskInfo);
        List<ExecuteObjectTask> executeObjectTasks =
            executeObjectTaskService.listTasksByGseTaskId(stepInstance, gseTask.getId());
        List<Agent> terminateAgents = executeObjectTasks.stream()
            .map(executeObjectTask -> executeObjectTask.getExecuteObject().toGseAgent())
            .filter(agent -> agent != null && StringUtils.isNotEmpty(agent.getAgentId()))
            .distinct()
            .collect(Collectors.toList());


        TerminateGseTaskRequest request = new TerminateGseTaskRequest(gseTask.getGseTaskId(),
            terminateAgents, gseV2Task);
        GseTaskResponse gseTaskResponse = gseClient.terminateGseScriptTask(request);
        if (GseTaskResponse.ERROR_CODE_SUCCESS != gseTaskResponse.getErrorCode()) {
            log.error("Terminate gse task failed! gseTask: {}", gseTaskInfo);
        } else {
            log.info("Terminate gse task response success!");
            gseTask.setStatus(RunStatusEnum.STOPPING.getValue());
            gseTaskService.updateGseTask(gseTask);
        }
    }
}
