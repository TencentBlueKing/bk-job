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

package com.tencent.bk.job.execute.engine.executor;

import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.gse.GseClient;
import com.tencent.bk.job.common.gse.v2.model.GseTaskResponse;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.engine.consts.AgentTaskStatusEnum;
import com.tencent.bk.job.execute.engine.evict.TaskEvictPolicyExecutor;
import com.tencent.bk.job.execute.engine.listener.event.EventSource;
import com.tencent.bk.job.execute.engine.listener.event.StepEvent;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.engine.model.TaskVariablesAnalyzeResult;
import com.tencent.bk.job.execute.engine.result.ResultHandleManager;
import com.tencent.bk.job.execute.engine.result.ha.ResultHandleTaskKeepaliveManager;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepInstanceVariableValuesDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.monitor.metrics.ExecuteMonitor;
import com.tencent.bk.job.execute.monitor.metrics.GseTasksExceptionCounter;
import com.tencent.bk.job.execute.service.AccountService;
import com.tencent.bk.job.execute.service.AgentService;
import com.tencent.bk.job.execute.service.AgentTaskService;
import com.tencent.bk.job.execute.service.GseTaskService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.StepInstanceVariableValueService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceVariableService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.util.StopWatch;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractGseTaskStartCommand extends AbstractGseTaskCommand {

    protected final GseTasksExceptionCounter gseTasksExceptionCounter;
    protected final ResultHandleManager resultHandleManager;
    protected final TaskInstanceService taskInstanceService;
    protected final TaskInstanceVariableService taskInstanceVariableService;
    protected final StepInstanceVariableValueService stepInstanceVariableValueService;
    protected final LogService logService;
    protected final TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;
    protected final ResultHandleTaskKeepaliveManager resultHandleTaskKeepaliveManager;
    protected final ExecuteMonitor executeMonitor;
    protected final TaskEvictPolicyExecutor taskEvictPolicyExecutor;
    protected final JobExecuteConfig jobExecuteConfig;
    protected final StepInstanceService stepInstanceService;

    /**
     * 任务下发请求ID,防止重复下发任务
     */
    protected String requestId;
    /**
     * GSE 目标 Agent 任务, Map<AgentId,AgentTask>
     */
    protected Map<String, AgentTaskDTO> targetAgentTaskMap = new HashMap<>();
    /**
     * 全局参数分析结果
     */
    protected TaskVariablesAnalyzeResult taskVariablesAnalyzeResult;
    /**
     * 步骤输入参数
     */
    protected StepInstanceVariableValuesDTO stepInputVariables;
    /**
     * 全局变量参数定义与初始值
     */
    protected Map<String, TaskVariableDTO> globalVariables = new HashMap<>();
    /**
     * Agent ID 与 host 映射关系
     */
    protected Map<String, HostDTO> agentIdHostMap;
    /**
     * hostId 与 host 映射关系
     */
    protected Map<Long, HostDTO> hostIdHostMap;
    /**
     * Agent tasks
     */
    protected List<AgentTaskDTO> agentTasks;


    AbstractGseTaskStartCommand(ResultHandleManager resultHandleManager,
                                TaskInstanceService taskInstanceService,
                                GseTaskService gseTaskService,
                                AgentTaskService agentTaskService,
                                AccountService accountService,
                                TaskInstanceVariableService taskInstanceVariableService,
                                StepInstanceVariableValueService stepInstanceVariableValueService,
                                AgentService agentService,
                                LogService logService,
                                TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
                                ResultHandleTaskKeepaliveManager resultHandleTaskKeepaliveManager,
                                ExecuteMonitor executeMonitor,
                                JobExecuteConfig jobExecuteConfig,
                                TaskEvictPolicyExecutor taskEvictPolicyExecutor,
                                GseTasksExceptionCounter gseTasksExceptionCounter,
                                Tracer tracer,
                                GseClient gseClient,
                                String requestId,
                                TaskInstanceDTO taskInstance,
                                StepInstanceDTO stepInstance,
                                GseTaskDTO gseTask,
                                StepInstanceService stepInstanceService) {
        super(agentService,
            accountService,
            gseTaskService,
            agentTaskService,
            tracer,
            gseClient,
            taskInstance,
            stepInstance,
            gseTask);
        this.resultHandleManager = resultHandleManager;
        this.taskInstanceService = taskInstanceService;
        this.taskInstanceVariableService = taskInstanceVariableService;
        this.stepInstanceVariableValueService = stepInstanceVariableValueService;
        this.logService = logService;
        this.taskExecuteMQEventDispatcher = taskExecuteMQEventDispatcher;
        this.resultHandleTaskKeepaliveManager = resultHandleTaskKeepaliveManager;
        this.executeMonitor = executeMonitor;
        this.jobExecuteConfig = jobExecuteConfig;
        this.taskEvictPolicyExecutor = taskEvictPolicyExecutor;
        this.gseTasksExceptionCounter = gseTasksExceptionCounter;
        this.requestId = requestId;
        this.stepInstanceService = stepInstanceService;
        this.agentIdHostMap = stepInstanceService.computeStepHosts(stepInstance, HostDTO::getAgentId);
        this.hostIdHostMap = stepInstanceService.computeStepHosts(stepInstance, HostDTO::getHostId);
    }


    /**
     * 执行GSE任务
     */
    @Override
    public void execute() {
        StopWatch watch = new StopWatch("startGseTask-" + this.gseTaskInfo);

        // 初始化任务执行上下文
        watch.start("initExecutionContext");
        initExecutionContext();
        watch.stop();

        // 检查任务
        watch.start("checkGseTaskExecutable");
        if (!checkGseTaskExecutable()) {
            finishGseTask(RunStatusEnum.FAIL);
            return;
        }
        watch.stop();

        // 下发任务给GSE
        boolean startSuccess = startGseTaskIfNotAvailable(watch);
        if (!startSuccess) {
            return;
        }

        // 添加执行结果处理后台任务
        watch.start("addResultHandleTask");
        if (stepInstance.getStatus() == RunStatusEnum.RUNNING) {
            addResultHandleTask();
        }
        watch.stop();

        if (watch.getTotalTimeMillis() > 1000L) {
            log.warn("Start gse task slow , statistics:{}", watch.prettyPrint());
        }
    }

    /**
     * 下发 GSE 任务。
     *
     * @param watch watch
     * @return 如果任务下发成功/任务已下发，返回true;下发失败返回false
     */
    private boolean startGseTaskIfNotAvailable(StopWatch watch) {
        boolean isGseTaskStarted = StringUtils.isNotEmpty(gseTask.getGseTaskId());
        if (!isGseTaskStarted) {
            watch.start("sendGseTask");
            gseTask.setStartTime(System.currentTimeMillis());
            log.info("[{}] Sending task to gse server", this.gseTaskInfo);
            GseTaskResponse gseTaskResponse = startGseTask();
            watch.stop();

            watch.start("handleGseResponse");
            if (GseTaskResponse.ERROR_CODE_SUCCESS != gseTaskResponse.getErrorCode()) {
                log.error("[{}] Start gse task fail, response: {}", this.gseTaskInfo, gseTaskResponse);
                handleStartGseTaskError(gseTaskResponse);
                gseTasksExceptionCounter.increment();
                taskExecuteMQEventDispatcher.dispatchStepEvent(StepEvent.refreshStep(stepInstanceId,
                    EventSource.buildGseTaskEventSource(stepInstanceId, executeCount, batch, gseTask.getId())));
                watch.stop();
                return false;
            } else {
                log.info("[{}] Start gse task successfully, gseTaskId: {}", this.gseTaskInfo,
                    gseTaskResponse.getGseTaskId());
                updateGseTaskExecutionInfo(gseTaskResponse.getGseTaskId(), RunStatusEnum.RUNNING, null);
            }
            watch.stop();
            return true;
        } else {
            // GSE 任务已经下发过，不做处理
            log.info("[{}] Gse Task had already started!", this.gseTaskInfo);
            return true;
        }
    }

    private void initAgentTasks() {
        this.agentTasks = agentTaskService.listAgentTasksByGseTaskId(gseTask.getId());
        updateUninstalledAgentTasks(this.agentTasks);

        agentTasks.stream()
            .filter(AgentTaskDTO::isTarget)
            .filter(agentTask -> !agentTask.isAgentIdEmpty())
            .forEach(agentTask -> this.targetAgentTaskMap.put(agentTask.getAgentId(), agentTask));
    }

    private void updateUninstalledAgentTasks(Collection<AgentTaskDTO> agentTasks) {
        List<AgentTaskDTO> invalidAgentTasks = agentTasks.stream()
            .filter(AgentTaskDTO::isAgentIdEmpty)
            .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(invalidAgentTasks)) {
            log.warn("{} contains invalid agent tasks: {}", gseTaskInfo, invalidAgentTasks);
            invalidAgentTasks.forEach(agentTask -> {
                agentTask.setStatus(AgentTaskStatusEnum.AGENT_NOT_INSTALLED);
                agentTask.setStartTime(System.currentTimeMillis());
                agentTask.setEndTime(System.currentTimeMillis());
                agentTask.calculateTotalTime();
            });
            agentTaskService.batchUpdateAgentTasks(agentTasks);
        }
    }

    private void initVariables() {
        if (taskInstance.isPlanInstance()) {
            List<TaskVariableDTO> taskVariables =
                taskInstanceVariableService.getByTaskInstanceId(stepInstance.getTaskInstanceId());
            if (taskVariables != null && !taskVariables.isEmpty()) {
                taskVariables.forEach(var -> globalVariables.put(var.getName(), var));
            }
            taskVariablesAnalyzeResult = new TaskVariablesAnalyzeResult(taskVariables);
            if (!taskVariablesAnalyzeResult.isExistAnyVar()) {
                return;
            }
            stepInputVariables = stepInstanceVariableValueService.computeInputStepInstanceVariableValues(
                taskInstance.getId(), stepInstance.getId(), taskVariables);
            log.info("Compute step input variable, stepInputVariables:{}", stepInputVariables);
        } else {
            taskVariablesAnalyzeResult = new TaskVariablesAnalyzeResult(null);
        }
    }


    /**
     * 更新GSE任务执行情况
     *
     * @param gseTaskId GSE返回的任务ID;如果不需要更新，传入null
     * @param status    任务状态;如果不需要更新，传入null
     * @param endTime   任务结束时间;如果不需要更新，传入null
     */
    protected void updateGseTaskExecutionInfo(String gseTaskId,
                                              RunStatusEnum status,
                                              Long endTime) {
        if (StringUtils.isNotEmpty(gseTaskId)) {
            gseTask.setGseTaskId(gseTaskId);
        }
        if (status != null) {
            gseTask.setStatus(status.getValue());
        }
        if (gseTask.getStartTime() == null) {
            gseTask.setStartTime(System.currentTimeMillis());
        }
        if (endTime != null) {
            gseTask.setEndTime(endTime);
            gseTask.setTotalTime(endTime - gseTask.getStartTime());
        }
        gseTaskService.updateGseTask(gseTask);
    }

    /**
     * 获取字符类型（TaskVariableTypeEnum.STRING|TaskVariableTypeEnum.CIPHER)全局变量的变量名和值
     *
     * @param stepInputVariables 步骤入参
     * @return 字符类型全局变量的变量名和值
     */
    protected Map<String, String> buildStringGlobalVarKV(StepInstanceVariableValuesDTO stepInputVariables) {
        Map<String, String> globalVarValueMap = new HashMap<>();
        if (stepInputVariables == null || CollectionUtils.isEmpty(stepInputVariables.getGlobalParams())) {
            return globalVarValueMap;
        }
        stepInputVariables.getGlobalParams().forEach(globalParam -> {
            TaskVariableTypeEnum variableType = TaskVariableTypeEnum.valOf(globalParam.getType());
            if (variableType == TaskVariableTypeEnum.STRING || variableType == TaskVariableTypeEnum.CIPHER) {
                globalVarValueMap.put(globalParam.getName(), globalParam.getValue());
            }
        });
        return globalVarValueMap;
    }

    private void finishGseTask(RunStatusEnum gseTaskStatus) {
        updateGseTaskExecutionInfo(null, gseTaskStatus, DateUtils.currentTimeMillis());
        taskExecuteMQEventDispatcher.dispatchStepEvent(StepEvent.refreshStep(stepInstanceId,
            EventSource.buildGseTaskEventSource(stepInstanceId, executeCount, batch, gseTask.getId())));
    }

    /**
     * 初始化执行上下文，在GSE任务下发前调用
     */
    protected void initExecutionContext() {
        initAgentTasks();
        initVariables();
    }

    /**
     * 检查任务
     *
     * @return 是否可以执行
     */
    protected abstract boolean checkGseTaskExecutable();

    /**
     * 下发GSE任务
     *
     * @return GSE任务下发请求结果
     */
    protected abstract GseTaskResponse startGseTask();

    /**
     * 处理GSE任务下发失败
     */
    protected abstract void handleStartGseTaskError(GseTaskResponse gseTaskResponse);

    /**
     * 添加执行结果处理任务
     */
    protected abstract void addResultHandleTask();

}
