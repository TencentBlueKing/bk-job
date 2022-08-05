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
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.Utils;
import com.tencent.bk.job.execute.api.esb.v2.EsbGetJobInstanceLogResource;
import com.tencent.bk.job.execute.common.constants.FileDistModeEnum;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.model.AgentTaskDetailDTO;
import com.tencent.bk.job.execute.model.AgentTaskResultGroupDTO;
import com.tencent.bk.job.execute.model.FileIpLogContent;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.ScriptHostLogContent;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v2.EsbStepInstanceResultAndLog;
import com.tencent.bk.job.execute.model.esb.v2.request.EsbGetJobInstanceLogRequest;
import com.tencent.bk.job.execute.service.FileAgentTaskService;
import com.tencent.bk.job.execute.service.GseTaskService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.ScriptAgentTaskService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@Slf4j
public class EsbGetJobInstanceLogResourceImpl extends JobQueryCommonProcessor implements EsbGetJobInstanceLogResource {

    private final TaskInstanceService taskInstanceService;
    private final AppScopeMappingService appScopeMappingService;
    private final ScriptAgentTaskService scriptAgentTaskService;
    private final FileAgentTaskService fileAgentTaskService;
    private final GseTaskService gseTaskService;
    private final LogService logService;

    public EsbGetJobInstanceLogResourceImpl(TaskInstanceService taskInstanceService,
                                            AppScopeMappingService appScopeMappingService,
                                            ScriptAgentTaskService scriptAgentTaskService,
                                            FileAgentTaskService fileAgentTaskService,
                                            GseTaskService gseTaskService,
                                            LogService logService) {
        this.taskInstanceService = taskInstanceService;
        this.appScopeMappingService = appScopeMappingService;
        this.scriptAgentTaskService = scriptAgentTaskService;
        this.fileAgentTaskService = fileAgentTaskService;
        this.gseTaskService = gseTaskService;
        this.logService = logService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v2_get_job_instance_log"})
    public EsbResp<List<EsbStepInstanceResultAndLog>> getJobInstanceLogUsingPost(EsbGetJobInstanceLogRequest request) {
        request.fillAppResourceScope(appScopeMappingService);

        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Get job instance log request is illegal!");
            throw new InvalidParamException(checkResult);
        }

        long taskInstanceId = request.getTaskInstanceId();
        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(taskInstanceId);
        authViewTaskInstance(request.getUserName(), request.getAppResourceScope(), taskInstance);

        List<StepInstanceBaseDTO> stepInstanceList =
            taskInstanceService.listStepInstanceByTaskInstanceId(taskInstanceId);
        List<EsbStepInstanceResultAndLog> stepInstResultAndLogList = Lists.newArrayList();
        for (StepInstanceBaseDTO stepInstance : stepInstanceList) {
            GseTaskDTO gseTask = gseTaskService.getGseTask(stepInstance.getId(),
                stepInstance.getExecuteCount(), 0);
            if (null == gseTask) {
                EsbStepInstanceResultAndLog stepInstResultAndLog = new EsbStepInstanceResultAndLog();
                stepInstResultAndLog.setFinished(false);
                stepInstResultAndLog.setStepInstanceId(stepInstance.getId());
                stepInstResultAndLog.setName(stepInstance.getName());
                stepInstResultAndLog.setStatus(stepInstance.getStatus().getValue());
                stepInstResultAndLog.setStepResults(Lists.newArrayList());
                stepInstResultAndLogList.add(stepInstResultAndLog);
                continue;
            }

            EsbStepInstanceResultAndLog stepInstResultAndLog = new EsbStepInstanceResultAndLog();
            stepInstResultAndLog.setFinished(!gseTask.getStatus().equals(RunStatusEnum.BLANK.getValue())
                && !gseTask.getStatus().equals(RunStatusEnum.RUNNING.getValue()));
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

            stepInstResultAndLog.setStepInstanceId(stepInstance.getId());
            stepInstResultAndLog.setName(stepInstance.getName());
            stepInstResultAndLog.setStatus(stepInstance.getStatus().getValue());
            stepInstResultAndLog.setStepResults(stepInstResultList);
            stepInstResultAndLogList.add(stepInstResultAndLog);
        }
        return EsbResp.buildSuccessResp(stepInstResultAndLogList);
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
                ScriptHostLogContent scriptHostLogContent = logService.getScriptHostLogContent(stepInstanceId, executeCount,
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
    public EsbResp<List<EsbStepInstanceResultAndLog>> getJobInstanceLog(String appCode,
                                                                        String username,
                                                                        Long appId,
                                                                        String scopeType,
                                                                        String scopeId,
                                                                        Long taskInstanceId) {
        EsbGetJobInstanceLogRequest req = new EsbGetJobInstanceLogRequest();
        req.setAppCode(appCode);
        req.setUserName(username);
        req.setBizId(appId);
        req.setScopeType(scopeType);
        req.setScopeId(scopeId);
        req.setTaskInstanceId(taskInstanceId);
        return getJobInstanceLogUsingPost(req);
    }
}
