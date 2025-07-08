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
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.Utils;
import com.tencent.bk.job.execute.api.esb.v2.EsbGetJobInstanceLogResource;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.FileExecuteObjectLogContent;
import com.tencent.bk.job.execute.model.ResultGroupDTO;
import com.tencent.bk.job.execute.model.ScriptExecuteObjectLogContent;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.esb.v2.EsbStepInstanceResultAndLog;
import com.tencent.bk.job.execute.model.esb.v2.request.EsbGetJobInstanceLogRequest;
import com.tencent.bk.job.execute.service.FileExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.ScriptExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceAccessProcessor;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@Slf4j
public class EsbGetJobInstanceLogResourceImpl implements EsbGetJobInstanceLogResource {

    private final TaskInstanceService taskInstanceService;
    private final ScriptExecuteObjectTaskService scriptExecuteObjectTaskService;
    private final FileExecuteObjectTaskService fileExecuteObjectTaskService;
    private final LogService logService;
    private final TaskInstanceAccessProcessor taskInstanceAccessProcessor;
    private final AppScopeMappingService appScopeMappingService;
    private final StepInstanceService stepInstanceService;

    public EsbGetJobInstanceLogResourceImpl(TaskInstanceService taskInstanceService,
                                            ScriptExecuteObjectTaskService scriptExecuteObjectTaskService,
                                            FileExecuteObjectTaskService fileExecuteObjectTaskService,
                                            LogService logService,
                                            TaskInstanceAccessProcessor taskInstanceAccessProcessor,
                                            AppScopeMappingService appScopeMappingService,
                                            StepInstanceService stepInstanceService) {
        this.taskInstanceService = taskInstanceService;
        this.scriptExecuteObjectTaskService = scriptExecuteObjectTaskService;
        this.fileExecuteObjectTaskService = fileExecuteObjectTaskService;
        this.logService = logService;
        this.taskInstanceAccessProcessor = taskInstanceAccessProcessor;
        this.appScopeMappingService = appScopeMappingService;
        this.stepInstanceService = stepInstanceService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v2_get_job_instance_log"})
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    public EsbResp<List<EsbStepInstanceResultAndLog>> getJobInstanceLogUsingPost(
        String username,
        String appCode,
        @AuditRequestBody EsbGetJobInstanceLogRequest request) {

        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Get job instance log request is illegal!");
            throw new InvalidParamException(checkResult);
        }

        taskInstanceAccessProcessor.processBeforeAccess(username,
            request.getAppResourceScope().getAppId(), request.getTaskInstanceId());

        List<StepInstanceBaseDTO> stepInstanceList =
            stepInstanceService.listBaseStepInstanceByTaskInstanceId(request.getTaskInstanceId());
        List<EsbStepInstanceResultAndLog> stepInstResultAndLogList = Lists.newArrayList();
        for (StepInstanceBaseDTO stepInstance : stepInstanceList) {
            EsbStepInstanceResultAndLog stepInstResultAndLog = new EsbStepInstanceResultAndLog();
            stepInstResultAndLog.setFinished(RunStatusEnum.isFinishedStatus(stepInstance.getStatus()));
            stepInstResultAndLog.setStepInstanceId(stepInstance.getId());
            stepInstResultAndLog.setName(stepInstance.getName());
            stepInstResultAndLog.setStatus(stepInstance.getStatus().getValue());
            stepInstResultAndLog.setStepResults(buildStepInstResult(stepInstance));
            stepInstResultAndLogList.add(stepInstResultAndLog);
        }
        return EsbResp.buildSuccessResp(stepInstResultAndLogList);
    }

    private List<EsbStepInstanceResultAndLog.StepInstResultDTO> buildStepInstResult(StepInstanceBaseDTO stepInstance) {
        List<ResultGroupDTO> resultGroups = Collections.emptyList();
        if (stepInstance.isScriptStep()) {
            resultGroups = scriptExecuteObjectTaskService.listAndGroupTasks(stepInstance,
                stepInstance.getExecuteCount(), null);
        } else if (stepInstance.isFileStep()) {
            resultGroups = fileExecuteObjectTaskService.listAndGroupTasks(stepInstance,
                stepInstance.getExecuteCount(), null);
        }

        List<EsbStepInstanceResultAndLog.StepInstResultDTO> stepInstResultList =
            Lists.newArrayListWithCapacity(resultGroups.size());

        for (ResultGroupDTO resultGroup : resultGroups) {
            EsbStepInstanceResultAndLog.StepInstResultDTO stepInstResult =
                new EsbStepInstanceResultAndLog.StepInstResultDTO();
            stepInstResult.setIpStatus(resultGroup.getStatus());
            stepInstResult.setTag(resultGroup.getTag());
            List<ExecuteObjectTask> executeObjectTasks = resultGroup.getExecuteObjectTasks();
            addLogContent(stepInstance, executeObjectTasks);
            List<EsbStepInstanceResultAndLog.EsbGseAgentTaskDTO> esbGseAgentTaskList =
                Lists.newArrayListWithCapacity(executeObjectTasks.size());
            for (ExecuteObjectTask executeObjectTask : executeObjectTasks) {
                EsbStepInstanceResultAndLog.EsbGseAgentTaskDTO esbGseAgentTaskDTO =
                    new EsbStepInstanceResultAndLog.EsbGseAgentTaskDTO();
                esbGseAgentTaskDTO.setLogContent(Utils.htmlEncode(executeObjectTask.getScriptLogContent()));
                esbGseAgentTaskDTO.setExecuteCount(executeObjectTask.getExecuteCount());
                esbGseAgentTaskDTO.setEndTime(executeObjectTask.getEndTime());
                esbGseAgentTaskDTO.setStartTime(executeObjectTask.getStartTime());
                esbGseAgentTaskDTO.setErrCode(executeObjectTask.getErrorCode());
                esbGseAgentTaskDTO.setExitCode(executeObjectTask.getExitCode());
                esbGseAgentTaskDTO.setTotalTime(executeObjectTask.getTotalTime());
                esbGseAgentTaskDTO.setCloudAreaId(executeObjectTask.getExecuteObject().getHost().getBkCloudId());
                esbGseAgentTaskDTO.setIp(executeObjectTask.getExecuteObject().getHost().getIp());
                esbGseAgentTaskList.add(esbGseAgentTaskDTO);
            }
            stepInstResult.setIpLogs(esbGseAgentTaskList);
            stepInstResultList.add(stepInstResult);
        }

        return stepInstResultList;
    }

    private ValidateResult checkRequest(EsbGetJobInstanceLogRequest request) {
        if (request.getTaskInstanceId() == null || request.getTaskInstanceId() < 1) {
            log.warn("TaskInstanceId is empty or illegal, taskInstanceId={}", request.getTaskInstanceId());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "job_instance_id");
        }
        return ValidateResult.pass();
    }

    private void addLogContent(StepInstanceBaseDTO stepInstance, List<ExecuteObjectTask> executeObjectTasks) {
        int executeCount = stepInstance.getExecuteCount();

        for (ExecuteObjectTask executeObjectTask : executeObjectTasks) {
            if (stepInstance.isScriptStep()) {
                ScriptExecuteObjectLogContent scriptExecuteObjectLogContent =
                    logService.getScriptExecuteObjectLogContent(stepInstance, executeCount,
                        null, executeObjectTask);
                executeObjectTask.setScriptLogContent(
                    scriptExecuteObjectLogContent == null ? "" : scriptExecuteObjectLogContent.getContent());
            } else if (stepInstance.isFileStep()) {
                FileExecuteObjectLogContent fileExecuteObjectLogContent = logService.getFileExecuteObjectLogContent(
                    stepInstance, executeCount, null, executeObjectTask);
                executeObjectTask.setScriptLogContent(
                    fileExecuteObjectLogContent == null ? "" : fileExecuteObjectLogContent.getContent());
            }
        }
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v2_get_job_instance_log"})
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    public EsbResp<List<EsbStepInstanceResultAndLog>> getJobInstanceLog(String username,
                                                                        String appCode,
                                                                        Long appId,
                                                                        String scopeType,
                                                                        String scopeId,
                                                                        Long taskInstanceId) {
        EsbGetJobInstanceLogRequest req = new EsbGetJobInstanceLogRequest();
        req.setBizId(appId);
        req.setScopeType(scopeType);
        req.setScopeId(scopeId);
        req.setTaskInstanceId(taskInstanceId);
        req.fillAppResourceScope(appScopeMappingService);
        return getJobInstanceLogUsingPost(username, appCode, req);
    }
}
