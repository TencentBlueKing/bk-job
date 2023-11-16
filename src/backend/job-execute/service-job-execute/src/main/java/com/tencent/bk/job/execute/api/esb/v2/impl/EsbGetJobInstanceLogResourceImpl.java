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
import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.audit.annotations.AuditRequestBody;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.gse.constants.FileDistModeEnum;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.Utils;
import com.tencent.bk.job.execute.api.esb.v2.EsbGetJobInstanceLogResource;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.model.AgentTaskDetailDTO;
import com.tencent.bk.job.execute.model.AgentTaskResultGroupDTO;
import com.tencent.bk.job.execute.model.FileIpLogContent;
import com.tencent.bk.job.execute.model.ScriptHostLogContent;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.esb.v2.EsbStepInstanceResultAndLog;
import com.tencent.bk.job.execute.model.esb.v2.request.EsbGetJobInstanceLogRequest;
import com.tencent.bk.job.execute.service.FileAgentTaskService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.ScriptAgentTaskService;
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
    private final ScriptAgentTaskService scriptAgentTaskService;
    private final FileAgentTaskService fileAgentTaskService;
    private final LogService logService;
    private final TaskInstanceAccessProcessor taskInstanceAccessProcessor;
    private final AppScopeMappingService appScopeMappingService;

    public EsbGetJobInstanceLogResourceImpl(TaskInstanceService taskInstanceService,
                                            ScriptAgentTaskService scriptAgentTaskService,
                                            FileAgentTaskService fileAgentTaskService,
                                            LogService logService,
                                            TaskInstanceAccessProcessor taskInstanceAccessProcessor,
                                            AppScopeMappingService appScopeMappingService) {
        this.taskInstanceService = taskInstanceService;
        this.scriptAgentTaskService = scriptAgentTaskService;
        this.fileAgentTaskService = fileAgentTaskService;
        this.logService = logService;
        this.taskInstanceAccessProcessor = taskInstanceAccessProcessor;
        this.appScopeMappingService = appScopeMappingService;
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
            taskInstanceService.listStepInstanceByTaskInstanceId(request.getTaskInstanceId());
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
        List<AgentTaskResultGroupDTO> resultGroups = Collections.emptyList();
        if (stepInstance.isScriptStep()) {
            resultGroups = scriptAgentTaskService.listAndGroupAgentTasks(stepInstance,
                stepInstance.getExecuteCount(), null);
        } else if (stepInstance.isFileStep()) {
            resultGroups = fileAgentTaskService.listAndGroupAgentTasks(stepInstance,
                stepInstance.getExecuteCount(), null);
        }

        List<EsbStepInstanceResultAndLog.StepInstResultDTO> stepInstResultList =
            Lists.newArrayListWithCapacity(resultGroups.size());

        for (AgentTaskResultGroupDTO resultGroup : resultGroups) {
            EsbStepInstanceResultAndLog.StepInstResultDTO stepInstResult =
                new EsbStepInstanceResultAndLog.StepInstResultDTO();
            stepInstResult.setIpStatus(resultGroup.getStatus());
            stepInstResult.setTag(resultGroup.getTag());
            List<AgentTaskDetailDTO> agentTasks = resultGroup.getAgentTasks();
            addLogContent(stepInstance, agentTasks);
            List<EsbStepInstanceResultAndLog.EsbGseAgentTaskDTO> esbGseAgentTaskList =
                Lists.newArrayListWithCapacity(agentTasks.size());
            for (AgentTaskDetailDTO agentTask : agentTasks) {
                EsbStepInstanceResultAndLog.EsbGseAgentTaskDTO esbGseAgentTaskDTO =
                    new EsbStepInstanceResultAndLog.EsbGseAgentTaskDTO();
                esbGseAgentTaskDTO.setLogContent(Utils.htmlEncode(agentTask.getScriptLogContent()));
                esbGseAgentTaskDTO.setExecuteCount(agentTask.getExecuteCount());
                esbGseAgentTaskDTO.setEndTime(agentTask.getEndTime());
                esbGseAgentTaskDTO.setStartTime(agentTask.getStartTime());
                esbGseAgentTaskDTO.setErrCode(agentTask.getErrorCode());
                esbGseAgentTaskDTO.setExitCode(agentTask.getExitCode());
                esbGseAgentTaskDTO.setTotalTime(agentTask.getTotalTime());
                esbGseAgentTaskDTO.setCloudAreaId(agentTask.getBkCloudId());
                esbGseAgentTaskDTO.setIp(agentTask.getIp());
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

    private void addLogContent(StepInstanceBaseDTO stepInstance, List<AgentTaskDetailDTO> agentTasks) {
        long stepInstanceId = stepInstance.getId();
        int executeCount = stepInstance.getExecuteCount();

        for (AgentTaskDetailDTO agentTask : agentTasks) {
            if (stepInstance.isScriptStep()) {
                ScriptHostLogContent scriptHostLogContent = logService.getScriptHostLogContent(stepInstanceId,
                    executeCount,
                    null, agentTask.getHost());
                agentTask.setScriptLogContent(scriptHostLogContent == null ? "" : scriptHostLogContent.getContent());
            } else if (stepInstance.isFileStep()) {
                FileIpLogContent fileIpLogContent = logService.getFileIpLogContent(stepInstanceId, executeCount,
                    null, agentTask.getHost(), FileDistModeEnum.DOWNLOAD.getValue());
                agentTask.setScriptLogContent(fileIpLogContent == null ? "" : fileIpLogContent.getContent());
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
