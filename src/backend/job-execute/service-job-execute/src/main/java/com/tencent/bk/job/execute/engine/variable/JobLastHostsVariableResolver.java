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

package com.tencent.bk.job.execute.engine.variable;

import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.execute.engine.consts.IpStatus;
import com.tencent.bk.job.execute.engine.consts.JobBuildInVariables;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.service.AgentTaskService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 任务前置步骤目标主机-变量解析器
 */
@Service
@Slf4j
public class JobLastHostsVariableResolver implements VariableResolver {
    private final TaskInstanceService taskInstanceService;
    private final AgentTaskService agentTaskService;
    private final Set<String> BUILD_IN_VARIABLES = new HashSet<>();

    @Autowired
    public JobLastHostsVariableResolver(TaskInstanceService taskInstanceService,
                                        AgentTaskService agentTaskService) {
        this.taskInstanceService = taskInstanceService;
        this.agentTaskService = agentTaskService;
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
        StepInstanceDTO preStepInstance = taskInstanceService.getPreExecutableStepInstance(taskInstanceId,
            stepInstanceId);
        if (preStepInstance == null) {
            log.info("Resolve value from latest executable step instance, but no pre step exist! taskInstanceId: {}, " +
                    "stepInstanceId:{}",
                taskInstanceId, stepInstanceId);
            return null;
        }
        Set<IpDTO> hosts = null;
        if (JobBuildInVariables.JOB_LAST_ALL.equals(variableName)) {
            hosts = extractAllHosts(preStepInstance);
        } else if (JobBuildInVariables.JOB_LAST_SUCCESS.equals(variableName)) {
            List<AgentTaskDTO> agentTasks = agentTaskService.getAgentTask(preStepInstance.getId(),
                preStepInstance.getExecuteCount(), true);
            if (CollectionUtils.isNotEmpty(agentTasks)) {
                hosts =
                    agentTasks.stream().filter(agentTask -> (agentTask.getStatus() == IpStatus.SUCCESS.getValue()
                        || agentTask.getStatus() == IpStatus.LAST_SUCCESS.getValue()))
                        .map(agentTask -> new IpDTO(agentTask.getCloudAreaId(), agentTask.getIp())).collect(Collectors.toSet());
            }
        } else if (JobBuildInVariables.JOB_LAST_FAIL.equals(variableName)) {
            List<AgentTaskDTO> agentTasks = agentTaskService.getAgentTask(preStepInstance.getId(),
                preStepInstance.getExecuteCount(), true);
            if (CollectionUtils.isNotEmpty(agentTasks)) {
                hosts =
                    agentTasks.stream().filter(agentTask -> (agentTask.getStatus() != IpStatus.SUCCESS.getValue()
                        && agentTask.getStatus() != IpStatus.LAST_SUCCESS.getValue()))
                        .map(agentTask -> new IpDTO(agentTask.getCloudAreaId(), agentTask.getIp())).collect(Collectors.toSet());
            }
        }
        String value = VariableResolveUtils.formatHosts(hosts);
        log.info("Resolve value from latest executable step instance, variableName: {}, value: {}", variableName,
            value);
        return value;
    }


    private Set<IpDTO> extractAllHosts(StepInstanceDTO stepInstance) {
        Set<IpDTO> hosts = new HashSet<>();
        if (CollectionUtils.isNotEmpty(stepInstance.getTargetServers().getIpList())) {
            hosts.addAll(stepInstance.getTargetServers().getIpList());
        }
        if (CollectionUtils.isNotEmpty(stepInstance.getTargetServers().getInvalidIpList())) {
            hosts.addAll(stepInstance.getTargetServers().getInvalidIpList());
        }
        if (CollectionUtils.isNotEmpty(stepInstance.getFileSourceList())) {
            stepInstance.getFileSourceList().forEach(fileSource -> {
                if (fileSource.getServers() != null
                    && CollectionUtils.isNotEmpty(fileSource.getServers().getIpList())) {
                    hosts.addAll(fileSource.getServers().getIpList());
                }
            });
        }
        return hosts;
    }
}
