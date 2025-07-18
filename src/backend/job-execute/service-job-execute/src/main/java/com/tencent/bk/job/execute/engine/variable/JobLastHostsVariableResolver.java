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

package com.tencent.bk.job.execute.engine.variable;

import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.engine.consts.JobBuildInVariables;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.service.FileExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.ScriptExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskStepTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.bk.job.common.util.function.LambdasUtil.not;

/**
 * 任务前置步骤目标主机-变量解析器
 */
@Service
@Slf4j
public class JobLastHostsVariableResolver implements VariableResolver {
    private final StepInstanceService stepInstanceService;
    private final ScriptExecuteObjectTaskService scriptExecuteObjectTaskService;
    private final FileExecuteObjectTaskService fileExecuteObjectTaskService;
    private final Set<String> BUILD_IN_VARIABLES = new HashSet<>();

    @Autowired
    public JobLastHostsVariableResolver(StepInstanceService stepInstanceService,
                                        ScriptExecuteObjectTaskService scriptExecuteObjectTaskService,
                                        FileExecuteObjectTaskService fileExecuteObjectTaskService) {
        this.stepInstanceService = stepInstanceService;
        this.scriptExecuteObjectTaskService = scriptExecuteObjectTaskService;
        this.fileExecuteObjectTaskService = fileExecuteObjectTaskService;
        init();
    }

    private void init() {
        BUILD_IN_VARIABLES.add(JobBuildInVariables.JOB_LAST_ALL);
        BUILD_IN_VARIABLES.add(JobBuildInVariables.JOB_LAST_SUCCESS);
        BUILD_IN_VARIABLES.add(JobBuildInVariables.JOB_LAST_FAIL);
    }

    @Override
    public boolean isMatch(String variable) {
        return BUILD_IN_VARIABLES.contains(variable);
    }

    public String resolve(VariableResolveContext context, String variableName) {
        long taskInstanceId = context.getTaskInstanceId();
        long stepInstanceId = context.getStepInstanceId();
        StepInstanceDTO preStepInstance = stepInstanceService.getPreExecutableStepInstance(taskInstanceId,
            stepInstanceId);
        if (preStepInstance == null) {
            log.info("Resolve value from latest executable step instance, but no pre step exist! taskInstanceId: {}, " +
                    "stepInstanceId:{}",
                taskInstanceId, stepInstanceId);
            return null;
        }
        Set<HostDTO> hosts = null;
        switch (variableName) {
            case JobBuildInVariables.JOB_LAST_ALL:
                hosts = preStepInstance.extractAllHosts();
                break;
            case JobBuildInVariables.JOB_LAST_SUCCESS: {
                List<ExecuteObjectTask> executeObjectTasks = listAgentTasks(preStepInstance);
                if (CollectionUtils.isNotEmpty(executeObjectTasks)) {
                    hosts = executeObjectTasks.stream()
                        .filter(ExecuteObjectTask::isSuccess)
                        .map(task -> task.getExecuteObject().getHost())
                        .collect(Collectors.toSet());
                }
                break;
            }
            case JobBuildInVariables.JOB_LAST_FAIL: {
                List<ExecuteObjectTask> executeObjectTasks = listAgentTasks(preStepInstance);
                if (CollectionUtils.isNotEmpty(executeObjectTasks)) {
                    hosts = executeObjectTasks.stream()
                        .filter(not(ExecuteObjectTask::isSuccess))
                        .map(task -> task.getExecuteObject().getHost())
                        .collect(Collectors.toSet());
                }
                break;
            }
        }

        String value = VariableResolveUtils.formatHosts(hosts);
        log.info("Resolve value from latest executable step instance, variableName: {}, value: {}", variableName,
            value);
        return value;
    }

    private List<ExecuteObjectTask> listAgentTasks(StepInstanceDTO stepInstance) {
        TaskStepTypeEnum stepType = stepInstance.getStepType();
        List<ExecuteObjectTask> agentTasks = null;
        if (stepType == TaskStepTypeEnum.SCRIPT) {
            agentTasks = scriptExecuteObjectTaskService.listTasks(stepInstance, stepInstance.getExecuteCount(), null);
        } else if (stepType == TaskStepTypeEnum.FILE) {
            agentTasks = fileExecuteObjectTaskService.listTasks(stepInstance, stepInstance.getExecuteCount(), null);
            if (CollectionUtils.isNotEmpty(agentTasks)) {
                agentTasks = agentTasks.stream()
                    .filter(agentTask -> agentTask.getFileTaskMode() == FileTaskModeEnum.DOWNLOAD)
                    .collect(Collectors.toList());
            }
        }
        return agentTasks;
    }
}
