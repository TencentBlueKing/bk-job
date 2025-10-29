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

package com.tencent.bk.job.execute.api.esb.v4;

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.esb.util.EsbDTOAppScopeMappingHelper;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v4.resp.V4JobInstanceStatusResp;
import com.tencent.bk.job.execute.service.FileExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.ScriptExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class OpenApiGetJobInstanceStatusV4ResourceImpl implements OpenApiGetJobInstanceStatusV4Resource {

    private final TaskInstanceService taskInstanceService;
    private final ScriptExecuteObjectTaskService scriptExecuteObjectTaskService;
    private final FileExecuteObjectTaskService fileExecuteObjectTaskService;
    private final StepInstanceService stepInstanceService;
    private final AppScopeMappingService appScopeMappingService;

    public OpenApiGetJobInstanceStatusV4ResourceImpl(TaskInstanceService taskInstanceService,
                                                     ScriptExecuteObjectTaskService scriptExecuteObjectTaskService,
                                                     FileExecuteObjectTaskService fileExecuteObjectTaskService,
                                                     StepInstanceService stepInstanceService,
                                                     AppScopeMappingService appScopeMappingService) {
        this.taskInstanceService = taskInstanceService;
        this.scriptExecuteObjectTaskService = scriptExecuteObjectTaskService;
        this.fileExecuteObjectTaskService = fileExecuteObjectTaskService;
        this.stepInstanceService = stepInstanceService;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v4_get_job_instance_status"})
    public EsbV4Response<V4JobInstanceStatusResp> getJobInstanceStatus(String username,
                                                                       String appCode,
                                                                       String scopeType,
                                                                       String scopeId,
                                                                       Long taskInstanceId,
                                                                       boolean returnIpResult) {
        AppResourceScope appResourceScope = appScopeMappingService.getAppResourceScope(scopeType, scopeId);

        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(
            username,
            appResourceScope.getAppId(),
            taskInstanceId
        );
        List<StepInstanceBaseDTO> stepInstances =
            stepInstanceService.listBaseStepInstanceByTaskInstanceId(taskInstanceId);

        if (stepInstances == null || stepInstances.isEmpty()) {
            log.warn("Get job instance status by taskInstanceId:{}, stepInstanceList is empty!", taskInstanceId);
            throw new NotFoundException(ErrorCode.TASK_INSTANCE_NOT_EXIST);
        }

        V4JobInstanceStatusResp jobInstanceStatus = buildV4JobInstanceStatusResp(
            taskInstance,
            stepInstances,
            returnIpResult
        );
        return EsbV4Response.success(jobInstanceStatus);
    }

    private V4JobInstanceStatusResp buildV4JobInstanceStatusResp(TaskInstanceDTO taskInstance,
                                                                 List<StepInstanceBaseDTO> stepInstances,
                                                                 boolean returnIpResult) {
        V4JobInstanceStatusResp jobInstanceStatus = new V4JobInstanceStatusResp();
        jobInstanceStatus.setFinished(RunStatusEnum.isFinishedStatus(taskInstance.getStatus()));

        jobInstanceStatus.setJobInstance(buildJobInstance(taskInstance));
        jobInstanceStatus.setStepInstances(
            stepInstances.stream().map(stepInstance ->
                buildStepInstance(stepInstance, returnIpResult))
                .collect(Collectors.toList())
        );
        return jobInstanceStatus;
    }

    private V4JobInstanceStatusResp.JobInstance buildJobInstance(TaskInstanceDTO taskInstance) {
        V4JobInstanceStatusResp.JobInstance jobInstance = new V4JobInstanceStatusResp.JobInstance();
        EsbDTOAppScopeMappingHelper.fillEsbAppScopeDTOByAppId(taskInstance.getAppId(), jobInstance);
        jobInstance.setId(taskInstance.getId());
        jobInstance.setStatus(taskInstance.getStatus().getValue());
        jobInstance.setName(taskInstance.getName());
        jobInstance.setCreateTime(taskInstance.getCreateTime());
        jobInstance.setStartTime(taskInstance.getStartTime());
        jobInstance.setEndTime(taskInstance.getEndTime());
        jobInstance.setTotalTime(taskInstance.getTotalTime());
        return jobInstance;
    }

    private V4JobInstanceStatusResp.StepInstance buildStepInstance(StepInstanceBaseDTO stepInstance,
                                                                   boolean returnIpResult) {
        V4JobInstanceStatusResp.StepInstance stepInstanceResp = new V4JobInstanceStatusResp.StepInstance();
        stepInstanceResp.setId(stepInstance.getId());
        stepInstanceResp.setName(stepInstance.getName());
        stepInstanceResp.setStatus(stepInstance.getStatus().getValue());
        stepInstanceResp.setType(stepInstance.getExecuteType().getValue());
        stepInstanceResp.setExecuteCount(stepInstance.getExecuteCount());
        stepInstanceResp.setStartTime(stepInstance.getStartTime());
        stepInstanceResp.setEndTime(stepInstance.getEndTime());
        stepInstanceResp.setTotalTime(stepInstance.getTotalTime());
        stepInstanceResp.setCreateTime(stepInstance.getCreateTime());

        if (returnIpResult) {
            List<ExecuteObjectTask> executeObjectTaskList = null;
            if (stepInstance.isScriptStep()) {
                executeObjectTaskList = scriptExecuteObjectTaskService.listTasks(
                    stepInstance,
                    stepInstance.getExecuteCount(),
                    null // 获取所有批次
                );
            } else if (stepInstance.isFileStep()) {
                executeObjectTaskList = fileExecuteObjectTaskService.listTasks(
                    stepInstance,
                    stepInstance.getExecuteCount(),
                    null // 获取所有批次
                );
                if (CollectionUtils.isNotEmpty(executeObjectTaskList)) {
                    // 文件分发任务只返回目标结果
                    executeObjectTaskList = executeObjectTaskList.stream()
                        .filter(executeObjectTask ->
                            executeObjectTask.getFileTaskMode() == FileTaskModeEnum.DOWNLOAD)
                        .collect(Collectors.toList());
                }
            }
            if (CollectionUtils.isNotEmpty(executeObjectTaskList)) {
                List<V4JobInstanceStatusResp.IpResult> ipResults = executeObjectTaskList.stream()
                    .map(this::convertToIpResult)
                    .collect(Collectors.toList());
                stepInstanceResp.setStepIpResult(ipResults);
            }
        }
        return stepInstanceResp;
    }

    private V4JobInstanceStatusResp.IpResult convertToIpResult(ExecuteObjectTask executeObjectTask) {
        V4JobInstanceStatusResp.IpResult ipResult = new V4JobInstanceStatusResp.IpResult();
        ipResult.setHostId(executeObjectTask.getExecuteObject().getHost().getHostId());
        ipResult.setCloudAreaId(executeObjectTask.getExecuteObject().getHost().getBkCloudId());
        ipResult.setIp(executeObjectTask.getExecuteObject().getHost().getIp());
        ipResult.setStatus(executeObjectTask.getStatus().getValue());
        ipResult.setTag(executeObjectTask.getTag());
        ipResult.setExitCode(executeObjectTask.getExitCode());
        ipResult.setStartTime(executeObjectTask.getStartTime());
        ipResult.setEndTime(executeObjectTask.getEndTime());
        ipResult.setTotalTime(executeObjectTask.getTotalTime());
        return ipResult;
    }
}
