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

package com.tencent.bk.job.execute.engine.result;

import com.tencent.bk.job.common.gse.GseClient;
import com.tencent.bk.job.execute.engine.evict.TaskEvictPolicyExecutor;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.model.FileDest;
import com.tencent.bk.job.execute.engine.model.JobFile;
import com.tencent.bk.job.execute.engine.model.TaskVariablesAnalyzeResult;
import com.tencent.bk.job.execute.engine.schedule.ScheduleTaskManager;
import com.tencent.bk.job.execute.engine.schedule.ha.ScheduleTaskKeepaliveManager;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ResultHandleTaskManager {
    private final ScheduleTaskManager scheduleTaskManager;
    private final TaskInstanceService taskInstanceService;
    private final TaskInstanceVariableService taskInstanceVariableService;
    private final StepInstanceVariableValueService stepInstanceVariableValueService;
    private final LogService logService;
    private final TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;
    private final TaskEvictPolicyExecutor taskEvictPolicyExecutor;
    private final StepInstanceService stepInstanceService;
    private final ResultHandleTaskSampler resultHandleTaskSampler;
    private final GseTaskService gseTaskService;
    private final GseClient gseClient;
    private final FileAgentTaskService fileAgentTaskService;
    private final ScriptAgentTaskService scriptAgentTaskService;
    private final ScheduleTaskKeepaliveManager scheduleTaskKeepaliveManager;

    @Autowired
    public ResultHandleTaskManager(
        @Qualifier("resultHandleScheduleTaskManager") ScheduleTaskManager scheduleTaskManager,
        TaskInstanceService taskInstanceService,
        TaskInstanceVariableService taskInstanceVariableService,
        StepInstanceVariableValueService stepInstanceVariableValueService,
        LogService logService,
        TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
        TaskEvictPolicyExecutor taskEvictPolicyExecutor,
        StepInstanceService stepInstanceService,
        ResultHandleTaskSampler resultHandleTaskSampler,
        GseTaskService gseTaskService,
        GseClient gseClient,
        FileAgentTaskService fileAgentTaskService,
        ScriptAgentTaskService scriptAgentTaskService,
        ScheduleTaskKeepaliveManager scheduleTaskKeepaliveManager) {
        this.scheduleTaskManager = scheduleTaskManager;
        this.taskInstanceService = taskInstanceService;
        this.taskInstanceVariableService = taskInstanceVariableService;
        this.stepInstanceVariableValueService = stepInstanceVariableValueService;
        this.logService = logService;
        this.taskExecuteMQEventDispatcher = taskExecuteMQEventDispatcher;
        this.taskEvictPolicyExecutor = taskEvictPolicyExecutor;
        this.stepInstanceService = stepInstanceService;
        this.resultHandleTaskSampler = resultHandleTaskSampler;
        this.gseTaskService = gseTaskService;
        this.gseClient = gseClient;
        this.fileAgentTaskService = fileAgentTaskService;
        this.scriptAgentTaskService = scriptAgentTaskService;
        this.scheduleTaskKeepaliveManager = scheduleTaskKeepaliveManager;
    }

    public void addFileResultHandleTask(TaskInstanceDTO taskInstance,
                                        StepInstanceDTO stepInstance,
                                        TaskVariablesAnalyzeResult taskVariablesAnalyzeResult,
                                        Map<String, AgentTaskDTO> targetAgentTasks,
                                        Map<String, AgentTaskDTO> sourceAgentTasks,
                                        GseTaskDTO gseTask,
                                        Map<JobFile, FileDest> srcDestFileMap,
                                        String requestId,
                                        List<AgentTaskDTO> agentTasks) {
        FileResultHandleTask fileResultHandleTask =
            new FileResultHandleTask(
                taskInstanceService,
                gseTaskService,
                logService,
                taskInstanceVariableService,
                stepInstanceVariableValueService,
                taskExecuteMQEventDispatcher,
                scheduleTaskKeepaliveManager,
                taskEvictPolicyExecutor,
                fileAgentTaskService,
                stepInstanceService,
                gseClient,
                resultHandleTaskSampler,
                taskInstance,
                stepInstance,
                taskVariablesAnalyzeResult,
                targetAgentTasks,
                sourceAgentTasks,
                gseTask,
                srcDestFileMap,
                requestId,
                agentTasks);
        scheduleTaskManager.handleDeliveredTask(fileResultHandleTask);
    }

    public void addScriptResultHandleTask(TaskInstanceDTO taskInstance,
                                          StepInstanceDTO stepInstance,
                                          TaskVariablesAnalyzeResult taskVariablesAnalyzeResult,
                                          Map<String, AgentTaskDTO> agentTaskMap,
                                          GseTaskDTO gseTask,
                                          String requestId,
                                          List<AgentTaskDTO> agentTasks) {
        ScriptResultHandleTask scriptResultHandleTask =
            new ScriptResultHandleTask(
                taskInstanceService,
                gseTaskService,
                logService,
                taskInstanceVariableService,
                stepInstanceVariableValueService,
                taskExecuteMQEventDispatcher,
                scheduleTaskKeepaliveManager,
                taskEvictPolicyExecutor,
                scriptAgentTaskService,
                stepInstanceService,
                gseClient,
                resultHandleTaskSampler,
                taskInstance,
                stepInstance,
                taskVariablesAnalyzeResult,
                agentTaskMap,
                gseTask,
                requestId,
                agentTasks);
        scheduleTaskManager.handleDeliveredTask(scriptResultHandleTask);
    }
}
