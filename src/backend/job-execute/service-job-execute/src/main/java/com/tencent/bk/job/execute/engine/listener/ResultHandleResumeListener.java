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

package com.tencent.bk.job.execute.engine.listener;

import com.tencent.bk.job.common.gse.GseClient;
import com.tencent.bk.job.common.gse.util.FilePathUtils;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.config.StorageSystemConfig;
import com.tencent.bk.job.execute.engine.evict.TaskEvictPolicyExecutor;
import com.tencent.bk.job.execute.engine.listener.event.ResultHandleTaskResumeEvent;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.model.FileDest;
import com.tencent.bk.job.execute.engine.model.JobFile;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.engine.model.TaskVariablesAnalyzeResult;
import com.tencent.bk.job.execute.engine.result.FileResultHandleTask;
import com.tencent.bk.job.execute.engine.result.ResultHandleManager;
import com.tencent.bk.job.execute.engine.result.ScriptResultHandleTask;
import com.tencent.bk.job.execute.engine.result.ha.ResultHandleTaskKeepaliveManager;
import com.tencent.bk.job.execute.engine.util.JobSrcFileUtils;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.FileAgentTaskService;
import com.tencent.bk.job.execute.service.GseTaskService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.ScriptAgentTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.StepInstanceVariableValueService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceVariableService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 执行引擎事件处理-任务恢复
 */
@Component
@Slf4j
public class ResultHandleResumeListener {
    private final TaskInstanceService taskInstanceService;

    private final ResultHandleManager resultHandleManager;

    private final TaskInstanceVariableService taskInstanceVariableService;

    private final GseTaskService gseTaskService;

    private final StorageSystemConfig storageSystemConfig;

    private final LogService logService;

    private final StepInstanceVariableValueService stepInstanceVariableValueService;

    private final TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;

    private final ResultHandleTaskKeepaliveManager resultHandleTaskKeepaliveManager;

    private final TaskEvictPolicyExecutor taskEvictPolicyExecutor;

    private final ScriptAgentTaskService scriptAgentTaskService;

    private final FileAgentTaskService fileAgentTaskService;

    private final StepInstanceService stepInstanceService;
    private final GseClient gseClient;

    @Autowired
    public ResultHandleResumeListener(TaskInstanceService taskInstanceService,
                                      ResultHandleManager resultHandleManager,
                                      TaskInstanceVariableService taskInstanceVariableService,
                                      GseTaskService gseTaskService,
                                      StorageSystemConfig storageSystemConfig,
                                      LogService logService,
                                      StepInstanceVariableValueService stepInstanceVariableValueService,
                                      TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
                                      ResultHandleTaskKeepaliveManager resultHandleTaskKeepaliveManager,
                                      TaskEvictPolicyExecutor taskEvictPolicyExecutor,
                                      ScriptAgentTaskService scriptAgentTaskService,
                                      FileAgentTaskService fileAgentTaskService,
                                      StepInstanceService stepInstanceService,
                                      GseClient gseClient) {
        this.taskInstanceService = taskInstanceService;
        this.resultHandleManager = resultHandleManager;
        this.taskInstanceVariableService = taskInstanceVariableService;
        this.gseTaskService = gseTaskService;
        this.storageSystemConfig = storageSystemConfig;
        this.logService = logService;
        this.stepInstanceVariableValueService = stepInstanceVariableValueService;
        this.taskExecuteMQEventDispatcher = taskExecuteMQEventDispatcher;
        this.resultHandleTaskKeepaliveManager = resultHandleTaskKeepaliveManager;
        this.taskEvictPolicyExecutor = taskEvictPolicyExecutor;
        this.scriptAgentTaskService = scriptAgentTaskService;
        this.fileAgentTaskService = fileAgentTaskService;
        this.stepInstanceService = stepInstanceService;
        this.gseClient = gseClient;
    }


    /**
     * 恢复被中断的作业结果处理任务
     */
    public void handleEvent(ResultHandleTaskResumeEvent event) {
        log.info("Receive gse task result handle task resume event: {}, duration: {}ms", event, event.duration());
        GseTaskDTO gseTask = gseTaskService.getGseTask(event.getGseTaskId());
        long stepInstanceId = gseTask.getStepInstanceId();
        String requestId = StringUtils.isNotEmpty(event.getRequestId()) ? event.getRequestId()
            : UUID.randomUUID().toString();

        try {
            StepInstanceDTO stepInstance = taskInstanceService.getStepInstanceDetail(stepInstanceId);
            TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(stepInstance.getTaskInstanceId());

            if (!checkIsTaskResumeable(stepInstance, gseTask)) {
                log.warn("Task can not resume, stepStatus: {}, gseTaskStatus: {}",
                    stepInstance.getStatus(), gseTask.getStatus());
                return;
            }

            List<TaskVariableDTO> taskVariables =
                taskInstanceVariableService.getByTaskInstanceId(stepInstance.getTaskInstanceId());
            TaskVariablesAnalyzeResult taskVariablesAnalyzeResult = new TaskVariablesAnalyzeResult(taskVariables);

            if (stepInstance.isScriptStep()) {
                resumeScriptTask(taskInstance, stepInstance, taskVariablesAnalyzeResult, gseTask, requestId);
            } else if (stepInstance.isFileStep()) {
                resumeFileTask(taskInstance, stepInstance, taskVariablesAnalyzeResult, gseTask, requestId);
            } else {
                log.error("Not support resume step type! stepType: {}", stepInstance.getExecuteType());
            }
        } catch (Exception e) {
            String errorMsg = "Handling task control message error,stepInstanceId=" + stepInstanceId;
            log.error(errorMsg, e);
        }
    }

    private void resumeScriptTask(TaskInstanceDTO taskInstance,
                                  StepInstanceDTO stepInstance,
                                  TaskVariablesAnalyzeResult taskVariablesAnalyzeResult,
                                  GseTaskDTO gseTask,
                                  String requestId) {
        Map<String, AgentTaskDTO> agentTaskMap = new HashMap<>();
        List<AgentTaskDTO> agentTasks = scriptAgentTaskService.listAgentTasksByGseTaskId(gseTask.getId());
        agentTasks.stream()
            .filter(agentTask -> StringUtils.isNotEmpty(agentTask.getAgentId()))
            .forEach(agentTask -> agentTaskMap.put(agentTask.getAgentId(), agentTask));

        ScriptResultHandleTask scriptResultHandleTask = new ScriptResultHandleTask(
            taskInstanceService,
            gseTaskService,
            logService,
            taskInstanceVariableService,
            stepInstanceVariableValueService,
            taskExecuteMQEventDispatcher,
            resultHandleTaskKeepaliveManager,
            taskEvictPolicyExecutor,
            scriptAgentTaskService,
            stepInstanceService,
            gseClient,
            taskInstance,
            stepInstance,
            taskVariablesAnalyzeResult,
            agentTaskMap,
            gseTask,
            requestId,
            agentTasks);
        resultHandleManager.handleDeliveredTask(scriptResultHandleTask);
    }

    private void resumeFileTask(TaskInstanceDTO taskInstance,
                                StepInstanceDTO stepInstance,
                                TaskVariablesAnalyzeResult taskVariablesAnalyzeResult,
                                GseTaskDTO gseTask,
                                String requestId) {
        Set<JobFile> sendFiles = JobSrcFileUtils.parseSrcFiles(stepInstance,
            storageSystemConfig.getJobStorageRootPath());
        String targetDir = FilePathUtils.standardizedDirPath(stepInstance.getResolvedFileTargetPath());
        Map<JobFile, FileDest> srcAndDestMap = JobSrcFileUtils.buildSourceDestPathMapping(
            sendFiles, targetDir, stepInstance.getFileTargetName());

        Map<String, AgentTaskDTO> sourceAgentTaskMap = new HashMap<>();
        Map<String, AgentTaskDTO> targetAgentTaskMap = new HashMap<>();
        List<AgentTaskDTO> agentTasks = fileAgentTaskService.listAgentTasksByGseTaskId(gseTask.getId());
        agentTasks.stream()
            .filter(agentTask -> StringUtils.isNotEmpty(agentTask.getAgentId()))
            .forEach(agentTask -> {
                if (agentTask.isTarget()) {
                    targetAgentTaskMap.put(agentTask.getAgentId(), agentTask);
                } else {
                    sourceAgentTaskMap.put(agentTask.getAgentId(), agentTask);
                }
            });

        FileResultHandleTask fileResultHandleTask = new FileResultHandleTask(
            taskInstanceService,
            gseTaskService,
            logService,
            taskInstanceVariableService,
            stepInstanceVariableValueService,
            taskExecuteMQEventDispatcher,
            resultHandleTaskKeepaliveManager,
            taskEvictPolicyExecutor,
            fileAgentTaskService,
            stepInstanceService,
            gseClient,
            taskInstance,
            stepInstance,
            taskVariablesAnalyzeResult,
            targetAgentTaskMap,
            sourceAgentTaskMap,
            gseTask,
            srcAndDestMap,
            requestId,
            agentTasks);
        resultHandleManager.handleDeliveredTask(fileResultHandleTask);
    }

    private boolean checkIsTaskResumeable(StepInstanceDTO stepInstance, GseTaskDTO gseTask) {
        RunStatusEnum stepStatus = stepInstance.getStatus();
        RunStatusEnum gseTaskStatus = RunStatusEnum.valueOf(gseTask.getStatus());
        return (stepStatus == RunStatusEnum.WAITING_USER || stepStatus == RunStatusEnum.RUNNING
            || stepStatus == RunStatusEnum.STOPPING) && (gseTaskStatus == RunStatusEnum.WAITING_USER
            || gseTaskStatus == RunStatusEnum.RUNNING || gseTaskStatus == RunStatusEnum.STOPPING);
    }
}
