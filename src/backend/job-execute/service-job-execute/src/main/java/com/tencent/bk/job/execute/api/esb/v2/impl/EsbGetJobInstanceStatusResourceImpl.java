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

package com.tencent.bk.job.execute.api.esb.v2.impl;

import com.google.common.collect.Lists;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.util.EsbDTOAppScopeMappingHelper;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.execute.api.esb.v2.EsbGetJobInstanceStatusResource;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.model.AgentTaskDetailDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v2.EsbIpStatusDTO;
import com.tencent.bk.job.execute.model.esb.v2.EsbJobInstanceStatusDTO;
import com.tencent.bk.job.execute.model.esb.v2.request.EsbGetJobInstanceStatusRequest;
import com.tencent.bk.job.execute.service.FileAgentTaskService;
import com.tencent.bk.job.execute.service.ScriptAgentTaskService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
public class EsbGetJobInstanceStatusResourceImpl
    extends JobQueryCommonProcessor
    implements EsbGetJobInstanceStatusResource {

    private final TaskInstanceService taskInstanceService;
    private final AppScopeMappingService appScopeMappingService;
    private final ScriptAgentTaskService scriptAgentTaskService;
    private final FileAgentTaskService fileAgentTaskService;

    public EsbGetJobInstanceStatusResourceImpl(TaskInstanceService taskInstanceService,
                                               AppScopeMappingService appScopeMappingService,
                                               ScriptAgentTaskService scriptAgentTaskService,
                                               FileAgentTaskService fileAgentTaskService) {
        this.taskInstanceService = taskInstanceService;
        this.appScopeMappingService = appScopeMappingService;
        this.scriptAgentTaskService = scriptAgentTaskService;
        this.fileAgentTaskService = fileAgentTaskService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v2_get_job_instance_status"})
    public EsbResp<EsbJobInstanceStatusDTO> getJobInstanceStatusUsingPost(EsbGetJobInstanceStatusRequest request) {
        request.fillAppResourceScope(appScopeMappingService);

        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Get job instance status request is illegal!");
            throw new InvalidParamException(checkResult);
        }

        long taskInstanceId = request.getTaskInstanceId();

        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(request.getTaskInstanceId());
        authViewTaskInstance(request.getUserName(), request.getAppResourceScope(), taskInstance);


        List<StepInstanceBaseDTO> stepInstances = taskInstanceService.listStepInstanceByTaskInstanceId(taskInstanceId);
        if (stepInstances == null || stepInstances.isEmpty()) {
            log.warn("Get job instance status by taskInstanceId:{}, stepInstanceList is empty!", taskInstanceId);
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }

        Map<Long, List<EsbIpStatusDTO>> stepIpResultMap = getStepIpResult(stepInstances);

        return EsbResp.buildSuccessResp(buildEsbJobInstanceStatusDTO(taskInstance, stepInstances, stepIpResultMap));
    }

    private ValidateResult checkRequest(EsbGetJobInstanceStatusRequest request) {
        if (request.getTaskInstanceId() == null || request.getTaskInstanceId() < 1) {
            log.warn("TaskInstanceId is empty or illegal, taskInstanceId={}", request.getTaskInstanceId());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "job_instance_id");
        }
        return ValidateResult.pass();
    }

    private Map<Long, List<EsbIpStatusDTO>> getStepIpResult(List<StepInstanceBaseDTO> stepInstanceList) {
        Map<Long, List<EsbIpStatusDTO>> stepIpResult = new HashMap<>();
        for (StepInstanceBaseDTO stepInstance : stepInstanceList) {
            List<AgentTaskDetailDTO> agentTasks = null;
            if (stepInstance.isScriptStep()) {
                agentTasks = scriptAgentTaskService.listAgentTaskDetail(stepInstance,
                    stepInstance.getExecuteCount(), null);
            } else if (stepInstance.isFileStep()) {
                agentTasks = fileAgentTaskService.listAgentTaskDetail(stepInstance,
                    stepInstance.getExecuteCount(), null);
            }
            List<EsbIpStatusDTO> ipResultList = Lists.newArrayList();
            if (CollectionUtils.isEmpty(agentTasks)) {
                stepIpResult.put(stepInstance.getId(), ipResultList);
                continue;
            }
            for (AgentTaskDetailDTO agentTask : agentTasks) {
                EsbIpStatusDTO ipStatus = new EsbIpStatusDTO();
                ipStatus.setIp(agentTask.getIp());
                ipStatus.setCloudAreaId(agentTask.getBkCloudId());

                ipStatus.setStatus(agentTask.getStatus());
                ipResultList.add(ipStatus);
            }
            stepIpResult.put(stepInstance.getId(), ipResultList);
        }
        return stepIpResult;
    }

    private EsbJobInstanceStatusDTO buildEsbJobInstanceStatusDTO(TaskInstanceDTO taskInstance,
                                                                 List<StepInstanceBaseDTO> stepInstances,
                                                                 Map<Long, List<EsbIpStatusDTO>> stepIpResultMap) {
        EsbJobInstanceStatusDTO jobInstanceStatus = new EsbJobInstanceStatusDTO();
        jobInstanceStatus.setIsFinished(
            RunStatusEnum.isFinishedStatus(RunStatusEnum.valueOf(taskInstance.getStatus())));

        EsbJobInstanceStatusDTO.JobInstance jobInstance = new EsbJobInstanceStatusDTO.JobInstance();
        EsbDTOAppScopeMappingHelper.fillEsbAppScopeDTOByAppId(taskInstance.getAppId(), jobInstance);
        jobInstance.setCurrentStepId(taskInstance.getCurrentStepInstanceId());
        jobInstance.setId(taskInstance.getId());
        jobInstance.setName(taskInstance.getName());
        jobInstance.setOperator(taskInstance.getOperator());
        jobInstance.setCreateTime(taskInstance.getCreateTime());
        jobInstance.setStartTime(taskInstance.getStartTime());
        jobInstance.setEndTime(taskInstance.getEndTime());
        jobInstance.setStartWay(taskInstance.getStartupMode());
        jobInstance.setStatus(taskInstance.getStatus());
        jobInstance.setTaskId(taskInstance.getTaskId());
        jobInstance.setTotalTime(taskInstance.getTotalTime());
        jobInstanceStatus.setJobInstance(jobInstance);

        List<EsbJobInstanceStatusDTO.Block> blocks = new ArrayList<>();
        for (StepInstanceBaseDTO stepInstance : stepInstances) {
            EsbJobInstanceStatusDTO.Block block = new EsbJobInstanceStatusDTO.Block();

            List<EsbJobInstanceStatusDTO.StepInst> stepInsts = new ArrayList<>(1);
            EsbJobInstanceStatusDTO.StepInst stepInst = new EsbJobInstanceStatusDTO.StepInst();
            stepInst.setId(stepInstance.getId());
            stepInst.setName(stepInstance.getName());
            stepInst.setCreateTime(stepInstance.getCreateTime());
            stepInst.setEndTime(stepInstance.getEndTime());
            stepInst.setStartTime(stepInstance.getStartTime());
            stepInst.setType(stepInstance.getExecuteType());
            stepInst.setOperator(stepInstance.getOperator());
            stepInst.setExecuteCount(stepInstance.getExecuteCount());
            stepInst.setStatus(stepInstance.getStatus());
            stepInst.setStepId(stepInstance.getStepId());
            stepInst.setTotalTime(stepInstance.getTotalTime());
            List<EsbIpStatusDTO> stepIpResult = stepIpResultMap.get(stepInstance.getId());
            stepInst.setStepIpResult(stepIpResult);
            stepInsts.add(stepInst);

            block.setStepInstances(stepInsts);
            blocks.add(block);
        }
        jobInstanceStatus.setBlocks(blocks);

        return jobInstanceStatus;
    }

    @Override
    public EsbResp<EsbJobInstanceStatusDTO> getJobInstanceStatus(String appCode,
                                                                 String username,
                                                                 Long bizId,
                                                                 String scopeType,
                                                                 String scopeId,
                                                                 Long taskInstanceId) {
        EsbGetJobInstanceStatusRequest req = new EsbGetJobInstanceStatusRequest();
        req.setAppCode(appCode);
        req.setUserName(username);
        req.setBizId(bizId);
        req.setScopeType(scopeType);
        req.setScopeId(scopeId);
        req.setTaskInstanceId(taskInstanceId);
        return getJobInstanceStatusUsingPost(req);
    }
}
