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

package com.tencent.bk.job.execute.api.esb.v2.impl;

import com.google.common.collect.Lists;
import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.audit.annotations.AuditRequestBody;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.util.EsbDTOAppScopeMappingHelper;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.execute.api.esb.v2.EsbGetJobInstanceStatusResource;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v2.EsbIpStatusDTO;
import com.tencent.bk.job.execute.model.esb.v2.EsbJobInstanceStatusDTO;
import com.tencent.bk.job.execute.model.esb.v2.request.EsbGetJobInstanceStatusRequest;
import com.tencent.bk.job.execute.service.FileExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.ScriptExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class EsbGetJobInstanceStatusResourceImpl implements EsbGetJobInstanceStatusResource {

    private final TaskInstanceService taskInstanceService;
    private final ScriptExecuteObjectTaskService scriptExecuteObjectTaskService;
    private final FileExecuteObjectTaskService fileExecuteObjectTaskService;
    private final AppScopeMappingService appScopeMappingService;
    private final StepInstanceService stepInstanceService;

    public EsbGetJobInstanceStatusResourceImpl(TaskInstanceService taskInstanceService,
                                               ScriptExecuteObjectTaskService scriptExecuteObjectTaskService,
                                               FileExecuteObjectTaskService fileExecuteObjectTaskService,
                                               AppScopeMappingService appScopeMappingService,
                                               StepInstanceService stepInstanceService) {
        this.taskInstanceService = taskInstanceService;
        this.scriptExecuteObjectTaskService = scriptExecuteObjectTaskService;
        this.fileExecuteObjectTaskService = fileExecuteObjectTaskService;
        this.appScopeMappingService = appScopeMappingService;
        this.stepInstanceService = stepInstanceService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v2_get_job_instance_status"})
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    public EsbResp<EsbJobInstanceStatusDTO> getJobInstanceStatusUsingPost(
        String username,
        String appCode,
        @AuditRequestBody EsbGetJobInstanceStatusRequest request) {
        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Get job instance status request is illegal!");
            throw new InvalidParamException(checkResult);
        }

        long taskInstanceId = request.getTaskInstanceId();
        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(username,
            request.getAppResourceScope().getAppId(), request.getTaskInstanceId());

        List<StepInstanceBaseDTO> stepInstances =
            stepInstanceService.listBaseStepInstanceByTaskInstanceId(taskInstanceId);
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
            List<ExecuteObjectTask> executeObjectTasks = null;
            if (stepInstance.isScriptStep()) {
                executeObjectTasks = scriptExecuteObjectTaskService.listTasks(stepInstance,
                    stepInstance.getExecuteCount(), null);
            } else if (stepInstance.isFileStep()) {
                executeObjectTasks = fileExecuteObjectTaskService.listTasks(stepInstance,
                    stepInstance.getExecuteCount(), null);
                if (CollectionUtils.isNotEmpty(executeObjectTasks)) {
                    // 如果是文件分发任务，只返回目标Agent结果
                    executeObjectTasks = executeObjectTasks.stream()
                        .filter(executeObjectTask -> executeObjectTask.getFileTaskMode() == FileTaskModeEnum.DOWNLOAD)
                        .collect(Collectors.toList());
                }
            }
            List<EsbIpStatusDTO> ipResultList = Lists.newArrayList();
            if (CollectionUtils.isEmpty(executeObjectTasks)) {
                stepIpResult.put(stepInstance.getId(), ipResultList);
                continue;
            }
            for (ExecuteObjectTask executeObjectTask : executeObjectTasks) {
                EsbIpStatusDTO ipStatus = new EsbIpStatusDTO();
                ipStatus.setIp(executeObjectTask.getExecuteObject().getHost().getIp());
                ipStatus.setCloudAreaId(executeObjectTask.getExecuteObject().getHost().getBkCloudId());

                ipStatus.setStatus(executeObjectTask.getStatus().getValue());
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
            RunStatusEnum.isFinishedStatus(taskInstance.getStatus()));

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
        jobInstance.setStatus(taskInstance.getStatus().getValue());
        jobInstance.setTaskId(taskInstance.getPlanId());
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
            stepInst.setType(stepInstance.getExecuteType().getValue());
            stepInst.setOperator(stepInstance.getOperator());
            stepInst.setExecuteCount(stepInstance.getExecuteCount());
            stepInst.setStatus(stepInstance.getStatus().getValue());
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
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v2_get_job_instance_status"})
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    public EsbResp<EsbJobInstanceStatusDTO> getJobInstanceStatus(String username,
                                                                 String appCode,
                                                                 Long bizId,
                                                                 String scopeType,
                                                                 String scopeId,
                                                                 Long taskInstanceId) {
        EsbGetJobInstanceStatusRequest req = new EsbGetJobInstanceStatusRequest();
        req.setBizId(bizId);
        req.setScopeType(scopeType);
        req.setScopeId(scopeId);
        req.setTaskInstanceId(taskInstanceId);
        req.fillAppResourceScope(appScopeMappingService);
        return getJobInstanceStatusUsingPost(username, appCode, req);
    }
}
