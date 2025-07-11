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

package com.tencent.bk.job.execute.engine.listener;

import com.tencent.bk.job.common.gse.v2.model.ExecuteObjectGseKey;
import com.tencent.bk.job.common.util.FilePathUtils;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.config.FileDistributeConfig;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.engine.EngineDependentServiceHolder;
import com.tencent.bk.job.execute.engine.listener.event.ResultHandleTaskResumeEvent;
import com.tencent.bk.job.execute.engine.model.FileDest;
import com.tencent.bk.job.execute.engine.model.JobFile;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.engine.model.TaskVariablesAnalyzeResult;
import com.tencent.bk.job.execute.engine.result.FileResultHandleTask;
import com.tencent.bk.job.execute.engine.result.ResultHandleManager;
import com.tencent.bk.job.execute.engine.result.ScriptResultHandleTask;
import com.tencent.bk.job.execute.engine.util.JobSrcFileUtils;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.FileExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.GseTaskService;
import com.tencent.bk.job.execute.service.ScriptExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
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
    private final EngineDependentServiceHolder engineDependentServiceHolder;
    private final TaskInstanceService taskInstanceService;

    private final ResultHandleManager resultHandleManager;

    private final TaskInstanceVariableService taskInstanceVariableService;

    private final GseTaskService gseTaskService;

    private final FileDistributeConfig fileDistributeConfig;

    private final ScriptExecuteObjectTaskService scriptExecuteObjectTaskService;

    private final FileExecuteObjectTaskService fileExecuteObjectTaskService;

    private final StepInstanceService stepInstanceService;

    private final JobExecuteConfig jobExecuteConfig;

    @Autowired
    public ResultHandleResumeListener(EngineDependentServiceHolder engineDependentServiceHolder,
                                      TaskInstanceService taskInstanceService,
                                      ResultHandleManager resultHandleManager,
                                      TaskInstanceVariableService taskInstanceVariableService,
                                      GseTaskService gseTaskService,
                                      FileDistributeConfig fileDistributeConfig,
                                      ScriptExecuteObjectTaskService scriptExecuteObjectTaskService,
                                      FileExecuteObjectTaskService fileExecuteObjectTaskService,
                                      StepInstanceService stepInstanceService,
                                      JobExecuteConfig jobExecuteConfig) {
        this.engineDependentServiceHolder = engineDependentServiceHolder;
        this.taskInstanceService = taskInstanceService;
        this.resultHandleManager = resultHandleManager;
        this.taskInstanceVariableService = taskInstanceVariableService;
        this.gseTaskService = gseTaskService;
        this.fileDistributeConfig = fileDistributeConfig;
        this.scriptExecuteObjectTaskService = scriptExecuteObjectTaskService;
        this.fileExecuteObjectTaskService = fileExecuteObjectTaskService;
        this.stepInstanceService = stepInstanceService;
        this.jobExecuteConfig = jobExecuteConfig;
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
            StepInstanceDTO stepInstance = stepInstanceService.getStepInstanceDetail(stepInstanceId);
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
        Map<ExecuteObjectGseKey, ExecuteObjectTask> executeObjectTaskMap = new HashMap<>();
        List<ExecuteObjectTask> executeObjectTasks
            = scriptExecuteObjectTaskService.listTasksByGseTaskId(stepInstance, gseTask.getId());
        executeObjectTasks.stream()
            .filter(executeObjectTask -> !executeObjectTask.getExecuteObject().isAgentIdEmpty())
            .forEach(executeObjectTask -> executeObjectTaskMap.put(
                executeObjectTask.getExecuteObject().toExecuteObjectGseKey(), executeObjectTask));

        ScriptResultHandleTask scriptResultHandleTask = new ScriptResultHandleTask(
            engineDependentServiceHolder,
            scriptExecuteObjectTaskService,
            jobExecuteConfig,
            taskInstance,
            stepInstance,
            taskVariablesAnalyzeResult,
            executeObjectTaskMap,
            gseTask,
            requestId,
            executeObjectTasks);
        resultHandleManager.handleDeliveredTask(scriptResultHandleTask);
    }

    private void resumeFileTask(TaskInstanceDTO taskInstance,
                                StepInstanceDTO stepInstance,
                                TaskVariablesAnalyzeResult taskVariablesAnalyzeResult,
                                GseTaskDTO gseTask,
                                String requestId) {
        Set<JobFile> sendFiles = JobSrcFileUtils.parseSrcFiles(stepInstance,
            fileDistributeConfig.getJobDistributeRootPath());
        String targetDir = FilePathUtils.standardizedDirPath(stepInstance.getResolvedFileTargetPath());
        Map<JobFile, FileDest> srcAndDestMap = JobSrcFileUtils.buildSourceDestPathMapping(
            sendFiles, targetDir, stepInstance.getFileTargetName());

        Map<ExecuteObjectGseKey, ExecuteObjectTask> sourceAgentTaskMap = new HashMap<>();
        Map<ExecuteObjectGseKey, ExecuteObjectTask> targetAgentTaskMap = new HashMap<>();
        List<ExecuteObjectTask> executeObjectTasks
            = fileExecuteObjectTaskService.listTasksByGseTaskId(stepInstance, gseTask.getId());
        executeObjectTasks.stream()
            .filter(executeObjectTask -> !executeObjectTask.getExecuteObject().isAgentIdEmpty())
            .forEach(executeObjectTask -> {
                if (executeObjectTask.isTarget()) {
                    targetAgentTaskMap.put(
                        executeObjectTask.getExecuteObject().toExecuteObjectGseKey(), executeObjectTask);
                } else {
                    sourceAgentTaskMap.put(
                        executeObjectTask.getExecuteObject().toExecuteObjectGseKey(), executeObjectTask);
                }
            });

        FileResultHandleTask fileResultHandleTask = new FileResultHandleTask(
            engineDependentServiceHolder,
            fileExecuteObjectTaskService,
            jobExecuteConfig,
            taskInstance,
            stepInstance,
            taskVariablesAnalyzeResult,
            targetAgentTaskMap,
            sourceAgentTaskMap,
            gseTask,
            srcAndDestMap,
            requestId,
            executeObjectTasks);
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
