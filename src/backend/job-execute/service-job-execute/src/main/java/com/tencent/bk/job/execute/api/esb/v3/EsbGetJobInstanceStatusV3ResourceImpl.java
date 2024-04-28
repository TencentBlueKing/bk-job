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

package com.tencent.bk.job.execute.api.esb.v3;

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
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbJobInstanceStatusV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.request.EsbGetJobInstanceStatusV3Request;
import com.tencent.bk.job.execute.service.FileExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.ScriptExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class EsbGetJobInstanceStatusV3ResourceImpl implements EsbGetJobInstanceStatusV3Resource {

    private final TaskInstanceService taskInstanceService;
    private final ScriptExecuteObjectTaskService scriptExecuteObjectTaskService;
    private final FileExecuteObjectTaskService fileExecuteObjectTaskService;
    private final AppScopeMappingService appScopeMappingService;
    private final StepInstanceService stepInstanceService;

    public EsbGetJobInstanceStatusV3ResourceImpl(TaskInstanceService taskInstanceService,
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
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_job_instance_status"})
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    public EsbResp<EsbJobInstanceStatusV3DTO> getJobInstanceStatusUsingPost(
        String username,
        String appCode,
        @AuditRequestBody EsbGetJobInstanceStatusV3Request request) {
        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Get job instance status request is illegal!");
            throw new InvalidParamException(checkResult);
        }

        long taskInstanceId = request.getTaskInstanceId();

        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(
            username, request.getAppResourceScope().getAppId(), request.getTaskInstanceId());

        List<StepInstanceBaseDTO> stepInstances =
            stepInstanceService.listBaseStepInstanceByTaskInstanceId(taskInstanceId);
        if (stepInstances == null || stepInstances.isEmpty()) {
            log.warn("Get job instance status by taskInstanceId:{}, stepInstanceList is empty!", taskInstanceId);
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }

        boolean isReturnIpResult = request.getReturnIpResult() != null && request.getReturnIpResult();
        EsbJobInstanceStatusV3DTO jobInstanceStatus = buildEsbJobInstanceStatusDTO(taskInstance, stepInstances,
            isReturnIpResult);

        return EsbResp.buildSuccessResp(jobInstanceStatus);
    }

    private ValidateResult checkRequest(EsbGetJobInstanceStatusV3Request request) {
        if (request.getTaskInstanceId() == null || request.getTaskInstanceId() < 1) {
            log.warn("TaskInstanceId is empty or illegal, taskInstanceId={}", request.getTaskInstanceId());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "job_instance_id");
        }
        return ValidateResult.pass();
    }

    private EsbJobInstanceStatusV3DTO buildEsbJobInstanceStatusDTO(TaskInstanceDTO taskInstance,
                                                                   List<StepInstanceBaseDTO> stepInstances,
                                                                   boolean isReturnIpResult) {
        EsbJobInstanceStatusV3DTO jobInstanceStatus = new EsbJobInstanceStatusV3DTO();
        jobInstanceStatus.setFinished(RunStatusEnum.isFinishedStatus(taskInstance.getStatus()));

        EsbJobInstanceStatusV3DTO.JobInstance jobInstance = new EsbJobInstanceStatusV3DTO.JobInstance();
        EsbDTOAppScopeMappingHelper.fillEsbAppScopeDTOByAppId(taskInstance.getAppId(), jobInstance);
        jobInstance.setId(taskInstance.getId());
        jobInstance.setName(taskInstance.getName());
        jobInstance.setCreateTime(taskInstance.getCreateTime());
        jobInstance.setStartTime(taskInstance.getStartTime());
        jobInstance.setEndTime(taskInstance.getEndTime());
        jobInstance.setStatus(taskInstance.getStatus().getValue());
        jobInstance.setTotalTime(taskInstance.getTotalTime());
        jobInstanceStatus.setJobInstance(jobInstance);

        List<EsbJobInstanceStatusV3DTO.StepInst> stepInsts = new ArrayList<>(stepInstances.size());
        for (StepInstanceBaseDTO stepInstance : stepInstances) {
            EsbJobInstanceStatusV3DTO.StepInst stepInst = new EsbJobInstanceStatusV3DTO.StepInst();
            stepInst.setId(stepInstance.getId());
            stepInst.setName(stepInstance.getName());
            stepInst.setCreateTime(stepInstance.getCreateTime());
            stepInst.setEndTime(stepInstance.getEndTime());
            stepInst.setStartTime(stepInstance.getStartTime());
            stepInst.setType(stepInstance.getExecuteType().getValue());
            stepInst.setExecuteCount(stepInstance.getExecuteCount());
            stepInst.setStatus(stepInstance.getStatus().getValue());
            stepInst.setTotalTime(stepInstance.getTotalTime());

            if (isReturnIpResult) {
                List<EsbJobInstanceStatusV3DTO.IpResult> stepIpResults = new ArrayList<>();
                List<ExecuteObjectTask> executeObjectTaskList = null;
                if (stepInstance.isScriptStep()) {
                    executeObjectTaskList = scriptExecuteObjectTaskService.listTasks(stepInstance,
                        stepInstance.getExecuteCount(), null);
                } else if (stepInstance.isFileStep()) {
                    executeObjectTaskList = fileExecuteObjectTaskService.listTasks(stepInstance,
                        stepInstance.getExecuteCount(), null);
                    if (CollectionUtils.isNotEmpty(executeObjectTaskList)) {
                        // 如果是文件分发任务，只返回目标Agent结果
                        executeObjectTaskList = executeObjectTaskList.stream()
                            .filter(executeObjectTask ->
                                executeObjectTask.getFileTaskMode() == FileTaskModeEnum.DOWNLOAD)
                            .collect(Collectors.toList());
                    }
                }
                if (CollectionUtils.isNotEmpty(executeObjectTaskList)) {
                    for (ExecuteObjectTask executeObjectTask : executeObjectTaskList) {
                        ExecuteObject executeObject = executeObjectTask.getExecuteObject();
                        EsbJobInstanceStatusV3DTO.IpResult stepIpResult = new EsbJobInstanceStatusV3DTO.IpResult();
                        stepIpResult.setHostId(executeObject.getHost().getHostId());
                        stepIpResult.setCloudAreaId(executeObject.getHost().getBkCloudId());
                        stepIpResult.setIp(executeObject.getHost().getIp());
                        stepIpResult.setExitCode(executeObjectTask.getExitCode());
                        stepIpResult.setErrorCode(executeObjectTask.getErrorCode());
                        stepIpResult.setStartTime(executeObjectTask.getStartTime());
                        stepIpResult.setEndTime(executeObjectTask.getEndTime());
                        stepIpResult.setTotalTime(executeObjectTask.getTotalTime());
                        stepIpResult.setTag(executeObjectTask.getTag());
                        stepIpResult.setStatus(executeObjectTask.getStatus().getValue());
                        stepIpResults.add(stepIpResult);
                    }
                }
                stepInst.setStepIpResult(stepIpResults);
            }
            stepInsts.add(stepInst);
        }
        jobInstanceStatus.setStepInstances(stepInsts);

        return jobInstanceStatus;
    }

    @Override
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    public EsbResp<EsbJobInstanceStatusV3DTO> getJobInstanceStatus(String username,
                                                                   String appCode,
                                                                   Long bizId,
                                                                   String scopeType,
                                                                   String scopeId,
                                                                   Long taskInstanceId,
                                                                   boolean returnIpResult) {
        EsbGetJobInstanceStatusV3Request request = new EsbGetJobInstanceStatusV3Request();
        request.setBizId(bizId);
        request.setScopeType(scopeType);
        request.setScopeId(scopeId);
        request.setTaskInstanceId(taskInstanceId);
        request.setReturnIpResult(returnIpResult);
        request.fillAppResourceScope(appScopeMappingService);
        return getJobInstanceStatusUsingPost(username, appCode, request);
    }
}
