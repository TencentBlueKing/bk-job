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

package com.tencent.bk.job.execute.api.esb.v3;

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.audit.annotations.AuditRequestBody;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.util.EsbDTOAppScopeMappingHelper;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v3.bkci.plugin.EsbBkCIPluginGetJobInstanceStatusRequest;
import com.tencent.bk.job.execute.model.esb.v3.bkci.plugin.EsbJobInstanceStatusDTO;
import com.tencent.bk.job.execute.service.FileExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.ScriptExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class EsbBkCIPluginGetJobInstanceStatusV3ResourceImpl implements EsbBkCIPluginGetJobInstanceStatusV3Resource {


    private final TaskInstanceService taskInstanceService;
    private final ScriptExecuteObjectTaskService scriptExecuteObjectTaskService;
    private final FileExecuteObjectTaskService fileExecuteObjectTaskService;
    private final StepInstanceService stepInstanceService;

    public EsbBkCIPluginGetJobInstanceStatusV3ResourceImpl(
        TaskInstanceService taskInstanceService,
        ScriptExecuteObjectTaskService scriptExecuteObjectTaskService,
        FileExecuteObjectTaskService fileExecuteObjectTaskService,
        StepInstanceService stepInstanceService) {

        this.taskInstanceService = taskInstanceService;
        this.scriptExecuteObjectTaskService = scriptExecuteObjectTaskService;
        this.fileExecuteObjectTaskService = fileExecuteObjectTaskService;
        this.stepInstanceService = stepInstanceService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_bkci_plugin_get_job_instance_status"})
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    public EsbResp<EsbJobInstanceStatusDTO> getJobInstanceStatus(
        String username,
        String appCode,
        @AuditRequestBody
        @Validated
            EsbBkCIPluginGetJobInstanceStatusRequest request) {

        long taskInstanceId = request.getTaskInstanceId();

        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(
            username, request.getAppResourceScope().getAppId(), request.getTaskInstanceId());

        List<StepInstanceBaseDTO> stepInstances =
            stepInstanceService.listBaseStepInstanceByTaskInstanceId(taskInstanceId);
        if (stepInstances == null || stepInstances.isEmpty()) {
            log.warn("Get job instance status by taskInstanceId:{}, stepInstanceList is empty!", taskInstanceId);
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }

        EsbJobInstanceStatusDTO jobInstanceStatus = buildEsbJobInstanceStatusDTO(taskInstance, stepInstances,
            request.isIncludeExecuteObjectTaskResult());

        return EsbResp.buildSuccessResp(jobInstanceStatus);
    }

    private EsbJobInstanceStatusDTO buildEsbJobInstanceStatusDTO(TaskInstanceDTO taskInstance,
                                                                 List<StepInstanceBaseDTO> stepInstances,
                                                                 boolean isIncludeExecuteObjectTaskResult) {
        EsbJobInstanceStatusDTO jobInstanceStatus = new EsbJobInstanceStatusDTO();
        jobInstanceStatus.setFinished(RunStatusEnum.isFinishedStatus(taskInstance.getStatus()));

        EsbJobInstanceStatusDTO.JobInstance jobInstance = new EsbJobInstanceStatusDTO.JobInstance();
        EsbDTOAppScopeMappingHelper.fillEsbAppScopeDTOByAppId(taskInstance.getAppId(), jobInstance);
        jobInstance.setId(taskInstance.getId());
        jobInstance.setName(taskInstance.getName());
        jobInstance.setCreateTime(taskInstance.getCreateTime());
        jobInstance.setStartTime(taskInstance.getStartTime());
        jobInstance.setEndTime(taskInstance.getEndTime());
        jobInstance.setStatus(taskInstance.getStatus().getValue());
        jobInstance.setTotalTime(taskInstance.getTotalTime());
        jobInstanceStatus.setJobInstance(jobInstance);

        List<EsbJobInstanceStatusDTO.StepResult> stepResults = new ArrayList<>(stepInstances.size());
        for (StepInstanceBaseDTO stepInstance : stepInstances) {
            EsbJobInstanceStatusDTO.StepResult stepResult = new EsbJobInstanceStatusDTO.StepResult();
            stepResult.setId(stepInstance.getId());
            stepResult.setName(stepInstance.getName());
            stepResult.setCreateTime(stepInstance.getCreateTime());
            stepResult.setEndTime(stepInstance.getEndTime());
            stepResult.setStartTime(stepInstance.getStartTime());
            stepResult.setType(stepInstance.getExecuteType().getValue());
            stepResult.setExecuteCount(stepInstance.getExecuteCount());
            stepResult.setStatus(stepInstance.getStatus().getValue());
            stepResult.setTotalTime(stepInstance.getTotalTime());

            if (isIncludeExecuteObjectTaskResult) {
                List<EsbJobInstanceStatusDTO.ExecuteObjectResult> executeObjectResults = new ArrayList<>();
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
                        EsbJobInstanceStatusDTO.ExecuteObjectResult executeObjectResult =
                            new EsbJobInstanceStatusDTO.ExecuteObjectResult();
                        executeObjectResult.setExecuteObject(
                            executeObjectTask.getExecuteObject().toOpenApiExecuteObjectDTO());
                        executeObjectResult.setExitCode(executeObjectTask.getExitCode());
                        executeObjectResult.setErrorCode(executeObjectTask.getErrorCode());
                        executeObjectResult.setStartTime(executeObjectTask.getStartTime());
                        executeObjectResult.setEndTime(executeObjectTask.getEndTime());
                        executeObjectResult.setTotalTime(executeObjectTask.getTotalTime());
                        executeObjectResult.setTag(executeObjectTask.getTag());
                        executeObjectResult.setStatus(executeObjectTask.getStatus().getValue());
                        executeObjectResults.add(executeObjectResult);
                    }
                }
                stepResult.setExecuteObjectResults(executeObjectResults);
            }
            stepResults.add(stepResult);
        }
        jobInstanceStatus.setStepResults(stepResults);

        return jobInstanceStatus;
    }
}
