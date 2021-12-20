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

import brave.Tracing;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.engine.TaskExecuteControlMsgSender;
import com.tencent.bk.job.execute.engine.consts.IpStatus;
import com.tencent.bk.job.execute.engine.exception.ExceptionStatusManager;
import com.tencent.bk.job.execute.engine.model.GseTaskExecuteResult;
import com.tencent.bk.job.execute.engine.model.GseTaskResponse;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.engine.model.TaskVariablesAnalyzeResult;
import com.tencent.bk.job.execute.engine.result.ResultHandleManager;
import com.tencent.bk.job.execute.engine.result.ha.ResultHandleTaskKeepaliveManager;
import com.tencent.bk.job.execute.model.AccountDTO;
import com.tencent.bk.job.execute.model.GseTaskIpLogDTO;
import com.tencent.bk.job.execute.model.GseTaskLogDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepInstanceVariableValuesDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.monitor.metrics.ExecuteMonitor;
import com.tencent.bk.job.execute.monitor.metrics.GseTasksExceptionCounter;
import com.tencent.bk.job.execute.service.AccountService;
import com.tencent.bk.job.execute.service.AgentService;
import com.tencent.bk.job.execute.service.GseTaskLogService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.StepInstanceVariableValueService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceVariableService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * GSE任务执行基础类
 */
@Slf4j
public abstract class AbstractGseTaskExecutor implements ResumableTask {
    /**
     * GSE任务异常Counter
     */
    private final GseTasksExceptionCounter gseTasksExceptionCounter;
    protected ResultHandleManager resultHandleManager;
    protected TaskInstanceService taskInstanceService;
    protected GseTaskLogService gseTaskLogService;
    protected AccountService accountService;
    protected TaskInstanceVariableService taskInstanceVariableService;
    protected StepInstanceVariableValueService stepInstanceVariableValueService;
    protected AgentService agentService;
    protected LogService logService;
    protected TaskExecuteControlMsgSender taskManager;
    protected ResultHandleTaskKeepaliveManager resultHandleTaskKeepaliveManager;
    protected ExecuteMonitor executeMonitor;
    protected ExceptionStatusManager exceptionStatusManager;
    protected JobExecuteConfig jobExecuteConfig;
    protected String requestId;
    protected Tracing tracing;
    /**
     * 步骤实例
     */
    protected StepInstanceDTO stepInstance;
    /**
     * 作业实例
     */
    protected TaskInstanceDTO taskInstance;
    /**
     * GSE任务信息
     */
    protected GseTaskLogDTO gseTaskLog;
    /**
     * 步骤实例ID
     */
    protected long stepInstanceId;
    /**
     * 执行次数
     */
    protected int executeCount;
    /**
     * 目标主机
     */
    protected Set<String> jobIpSet = new HashSet<>();
    /**
     * 不合法的主机
     */
    protected Set<String> invalidIpSet;
    /**
     * 文件源主机
     */
    protected Set<String> fileSourceIPSet = new HashSet<>();
    /**
     * gse 原子任务信息
     */
    protected Map<String, GseTaskIpLogDTO> ipLogMap = new HashMap<>();
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
     * 执行任务的所有主机
     */
    private Set<String> allJobIpSet = new HashSet<>();
    /**
     * 未开始执行任务的主机
     */
    private Set<String> notStartedJobIPSet = new HashSet<>();

    /**
     * GSETaskExecutor Constructor
     *
     * @param requestId                请求ID
     * @param gseTasksExceptionCounter GSE任务异常Counter
     * @param taskInstance             作业实例
     * @param stepInstance             步骤实例
     * @param executeIps               目标IP
     */
    AbstractGseTaskExecutor(String requestId, GseTasksExceptionCounter gseTasksExceptionCounter,
                            TaskInstanceDTO taskInstance, StepInstanceDTO stepInstance, Set<String> executeIps) {
        this.requestId = requestId;
        this.gseTasksExceptionCounter = gseTasksExceptionCounter;
        this.taskInstance = taskInstance;
        this.stepInstance = stepInstance;
        this.stepInstanceId = stepInstance.getId();
        this.executeCount = stepInstance.getExecuteCount();
        this.invalidIpSet = stepInstance.getInvalidIps();
        if (!this.invalidIpSet.isEmpty()) {
            stepInstance.setInvalidIps(this.invalidIpSet);
            log.warn("Init gse task info, task contains invalid hosts: {}", this.invalidIpSet);
        }

        this.allJobIpSet.addAll(executeIps);
        executeIps.removeAll(this.invalidIpSet);
        this.jobIpSet.addAll(executeIps);
        this.notStartedJobIPSet.addAll(executeIps);
    }

    /**
     * 初始化依赖的服务
     */
    public void initDependentService(ResultHandleManager resultHandleManager,
                                     TaskInstanceService taskInstanceService,
                                     GseTaskLogService gseTaskLogService,
                                     AccountService accountService,
                                     TaskInstanceVariableService taskInstanceVariableService,
                                     StepInstanceVariableValueService stepInstanceVariableValueService,
                                     AgentService agentService,
                                     LogService logService,
                                     TaskExecuteControlMsgSender taskManager,
                                     ResultHandleTaskKeepaliveManager resultHandleTaskKeepaliveManager,
                                     ExecuteMonitor executeMonitor,
                                     JobExecuteConfig jobExecuteConfig) {
        this.resultHandleManager = resultHandleManager;
        this.taskInstanceService = taskInstanceService;
        this.gseTaskLogService = gseTaskLogService;
        this.accountService = accountService;
        this.taskInstanceVariableService = taskInstanceVariableService;
        this.stepInstanceVariableValueService = stepInstanceVariableValueService;
        this.agentService = agentService;
        this.logService = logService;
        this.taskManager = taskManager;
        this.resultHandleTaskKeepaliveManager = resultHandleTaskKeepaliveManager;
        this.executeMonitor = executeMonitor;
        this.jobExecuteConfig = jobExecuteConfig;
    }

    public void setExceptionStatusManager(ExceptionStatusManager exceptionStatusManager) {
        this.exceptionStatusManager = exceptionStatusManager;
    }

    private void analyseAndSetTaskVariables() {
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
    }

    /**
     * 保存将要执行的gse原子任务初始状态
     */
    protected void initAndSaveGseIpLogsToBeStarted() {
        for (String cloudAreaIdAndIp : notStartedJobIPSet) {
            GseTaskIpLogDTO ipLog = buildGseTaskIpLog(cloudAreaIdAndIp, IpStatus.WAITING, true, false);
            ipLogMap.put(cloudAreaIdAndIp, ipLog);
        }
        gseTaskLogService.batchSaveIpLog(new ArrayList<>(ipLogMap.values()));
    }

    /**
     * 保存不合法主机的错误信息
     */
    private void saveInvalidGseIpLogs() {
        if (!invalidIpSet.isEmpty()) {
            List<GseTaskIpLogDTO> ipLogList = new ArrayList<>();
            invalidIpSet.forEach(cloudAreaIdAndIp -> {
                boolean isTargetServer = this.allJobIpSet.contains(cloudAreaIdAndIp);
                GseTaskIpLogDTO ipLog = buildGseTaskIpLog(cloudAreaIdAndIp, IpStatus.HOST_NOT_EXIST, isTargetServer,
                    !isTargetServer);
                ipLogList.add(ipLog);
            });
            gseTaskLogService.batchSaveIpLog(ipLogList);
            logService.batchWriteJobSystemScriptLog(taskInstance.getCreateTime(), stepInstanceId,
                stepInstance.getExecuteCount(),
                buildIpAndLogOffsetMap(invalidIpSet), "The host(s) is not belongs to the Business, or doesn't exists" +
                    ".", System.currentTimeMillis());
        }
    }

    private Map<String, Integer> buildIpAndLogOffsetMap(Collection<String> ips) {
        Map<String, Integer> ipAndLogOffsetMap = new HashMap<>();
        ips.forEach(ip -> {
            GseTaskIpLogDTO ipLog = ipLogMap.get(ip);
            if (ipLog != null) {
                ipAndLogOffsetMap.put(ip, ipLog.getOffset());
            } else {
                ipAndLogOffsetMap.put(ip, 0);
            }
        });
        return ipAndLogOffsetMap;
    }

    protected GseTaskIpLogDTO buildGseTaskIpLog(String cloudAreaIdAndIp, IpStatus status, boolean isTargetServer,
                                                boolean isSourceServer) {
        String[] cloudAreaIdAndIpArray = cloudAreaIdAndIp.split(":");
        GseTaskIpLogDTO ipLog = new GseTaskIpLogDTO();
        ipLog.setStepInstanceId(stepInstanceId);
        ipLog.setExecuteCount(executeCount);
        ipLog.setStatus(status.getValue());
        ipLog.setTargetServer(isTargetServer);
        ipLog.setIp(cloudAreaIdAndIpArray[1]);
        ipLog.setCloudAreaAndIp(cloudAreaIdAndIp);
        ipLog.setCloudAreaId(Long.valueOf(cloudAreaIdAndIpArray[0]));
        ipLog.setDisplayIp(cloudAreaIdAndIpArray[1]);
        ipLog.setSourceServer(isSourceServer);
        return ipLog;
    }

    protected Map<String, String> buildReferenceGlobalVarValueMap(StepInstanceVariableValuesDTO stepInputVariables) {
        Map<String, String> globalVarValueMap = new HashMap<>();
        if (stepInputVariables == null || CollectionUtils.isEmpty(stepInputVariables.getGlobalParams())) {
            return globalVarValueMap;
        }
        stepInputVariables.getGlobalParams().forEach(globalParam -> {
            if (TaskVariableTypeEnum.valOf(globalParam.getType()) == TaskVariableTypeEnum.STRING) {
                globalVarValueMap.put(globalParam.getName(), globalParam.getValue());
            }
        });
        return globalVarValueMap;
    }


    /**
     * 执行GSE任务
     */
    public void execute() {
        StopWatch watch = new StopWatch("GseTaskExecutor-execute-" + stepInstanceId);

        watch.start("init-execution-context");
        initExecutionContext();
        watch.stop();

        watch.start("check-executable");
        if (!checkHostExecutable()) {
            log.warn("Task is not executable, stepInstanceId:{}", stepInstanceId);
            handleStartGseTaskError(System.currentTimeMillis(), IpStatus.HOST_NOT_EXIST, "The host(s) is not belongs " +
                "to the Business, or doesn't exists.");
            return;
        }
        watch.stop();

        watch.start("get-gse-task-log-from-db");
        gseTaskLog = gseTaskLogService.getGseTaskLog(stepInstanceId, executeCount);
        watch.stop();

        boolean shouldSendTaskToGseServer = (gseTaskLog == null || StringUtils.isEmpty(gseTaskLog.getGseTaskId()));
        /**
         * gse任务ID
         */
        String gseTaskId;
        if (shouldSendTaskToGseServer) {
            watch.start("send-task-to-gse-server");
            log.info("Sending task to gse server, stepInstanceId:{}", stepInstanceId);
            long startTime = DateUtils.currentTimeMillis();
            GseTaskResponse gseTaskResponse = startGseTask(stepInstance);
            watch.stop();
            watch.start("analyse-gse-response");
            if (GseTaskResponse.ERROR_CODE_SUCCESS != gseTaskResponse.getErrorCode()) {
                handleStartGseTaskError(startTime, IpStatus.SUBMIT_FAILED,
                    "GSE Job failed:" + gseTaskResponse.getErrorMessage());
                gseTasksExceptionCounter.increment();
                return;
            } else {
                // 如果gseTaskLog不存在，需要填充一些基本信息；如果gseTaskLog存在，则只需要更新本次执行的一些状态信息
                if (gseTaskLog == null) {
                    gseTaskLog = new GseTaskLogDTO();
                    gseTaskLog.setStepInstanceId(stepInstanceId);
                    gseTaskLog.setExecuteCount(executeCount);
                }
                gseTaskLog.setStartTime(startTime);
                gseTaskLog.setGseTaskId(gseTaskResponse.getGseTaskId());
                gseTaskLog.setStatus(RunStatusEnum.RUNNING.getValue());
                gseTaskLogService.saveGseTaskLog(gseTaskLog);
            }
            watch.stop();
        } else {
            gseTaskId = gseTaskLog.getGseTaskId();
            log.debug("Init FileTaskExecutor,get from db,gseTaskId={},executeCount={}", gseTaskId, executeCount);
        }

        // 添加执行结果处理后台任务
        watch.start("add-result-handle-task");
        if (stepInstance.getStatus().equals(RunStatusEnum.RUNNING.getValue())) {
            addExecutionResultHandleTask();
        }
        watch.stop();
        if (watch.getTotalTimeMillis() > 1000L) {
            log.warn("GseTaskExecutor-> execute task is slow, run statistics:{}", watch.prettyPrint());
        }
    }

    /**
     * 添加执行结果处理任务
     */
    abstract void addExecutionResultHandleTask();

    /**
     * 处理gse任务下发失败
     *
     * @param startTime 启动时间
     * @param status    失败状态
     * @param msg       错误信息
     */
    private void handleStartGseTaskError(long startTime, IpStatus status, String msg) {
        gseTaskLog = new GseTaskLogDTO();
        gseTaskLog.setStepInstanceId(stepInstanceId);
        gseTaskLog.setExecuteCount(executeCount);
        long endTime = DateUtils.currentTimeMillis();

        gseTaskLog.setStatus(RunStatusEnum.FAIL.getValue());
        gseTaskLog.setEndTime(endTime);
        gseTaskLog.setTotalTime(endTime - startTime);
        gseTaskLogService.saveGseTaskLog(gseTaskLog);

        // 处理未完成的任务
        handleNotStartedIPResult(startTime, endTime, status, msg);

        taskInstanceService.updateStepEndTime(stepInstanceId, endTime);
        int invalidIpNum = this.invalidIpSet == null ? 0 : this.invalidIpSet.size();
        taskInstanceService.updateStepStatInfo(stepInstanceId, invalidIpNum + jobIpSet.size(), 0,
            invalidIpNum + jobIpSet.size());
        taskInstanceService.updateStepTotalTime(stepInstanceId, endTime - stepInstance.getStartTime());
        exceptionStatusManager.setAbnormalStatusForStep(stepInstanceId);
    }

    private void handleNotStartedIPResult(Long startTime, Long endTime, IpStatus status, String errorMsg) {
        log.info("[{}]: handleNotStartedIPResult| noStartJobIPSet={}", stepInstanceId, this.notStartedJobIPSet);

        Set<String> unfinishedIPSet = new HashSet<>();
        unfinishedIPSet.addAll(notStartedJobIPSet);
        unfinishedIPSet.addAll(this.fileSourceIPSet);
        if (unfinishedIPSet.isEmpty()) {
            log.debug("unfinishedIPSet is empty");
            return;
        }
        if (StringUtils.isNotEmpty(errorMsg)) {
            logService.batchWriteJobSystemScriptLog(taskInstance.getCreateTime(), stepInstanceId,
                stepInstance.getExecuteCount(),
                buildIpAndLogOffsetMap(unfinishedIPSet), errorMsg, endTime);
        }

        gseTaskLogService.batchUpdateIpLog(stepInstanceId, executeCount, unfinishedIPSet, startTime, endTime, status);
    }

    /**
     * 获取账号信息
     *
     * @param accountId    账号ID
     * @param accountAlias 账号别名
     * @param appId        业务ID
     * @return 账号
     */
    protected AccountDTO getAccountBean(Long accountId, String accountAlias, Long appId) {
        AccountDTO accountInfo = null;
        if (accountId != null && accountId > 0) {
            accountInfo = accountService.getAccountById(accountId);
        } else if (StringUtils.isNotBlank(accountAlias)) { //原account传的是account,改为支持alias，减少用户API调用增加参数的成本
            accountInfo = accountService.getSystemAccountByAlias(accountAlias, appId);
        }
        // 可能帐号已经被删除了的情况：如从执行历史中点重做/克隆的方式。
        if (accountInfo == null && StringUtils.isNotBlank(accountAlias)) {//兼容老的传参，直接传递没有密码的只有帐号名称的认证
            accountInfo = new AccountDTO();
            accountInfo.setAccount(accountAlias);
            accountInfo.setAlias(accountAlias);
        }
        return accountInfo;
    }

    /**
     * 初始化执行上下文，在GSE任务下发前调用
     */
    protected void initExecutionContext() {
        this.initAndSaveGseIpLogsToBeStarted();
        this.saveInvalidGseIpLogs();
        this.analyseAndSetTaskVariables();
    }

    /**
     * 生成GSE trace 信息
     */
    protected Map<String, String> buildTraceInfoMap() {
        // 捕获所有异常，避免影响任务下发主流程
        Map<String, String> traceInfoMap = new HashMap<>();
        try {
            traceInfoMap.put("CALLER_NAME", "JOB");
            traceInfoMap.put("JOB_ID", stepInstance.getTaskInstanceId().toString());
            traceInfoMap.put("STEP_ID", stepInstance.getId().toString());
            traceInfoMap.put("EXECUTE_COUNT", String.valueOf(stepInstance.getExecuteCount()));
            traceInfoMap.put("JOB_BIZ_ID", taskInstance.getAppId().toString());
            if (tracing != null) {
                traceInfoMap.put("REQUEST_ID", tracing.currentTraceContext().get().traceIdString());
            }
            if (org.apache.commons.lang3.StringUtils.isNotEmpty(taskInstance.getAppCode())) {
                traceInfoMap.put("APP_CODE", taskInstance.getAppCode());
            }
            traceInfoMap.put("CALLER_IP", agentService.getLocalAgentBindIp());
            traceInfoMap.put("TASK_ACCOUNT", stepInstance.getOperator());
        } catch (Throwable e) {
            log.warn("Build trace info map for gse failed");
        }
        return traceInfoMap;
    }

    /**
     * 检查任务是否可被执行
     *
     * @return 是否可执行
     */
    protected abstract boolean checkHostExecutable();

    /**
     * 下发GSE任务
     *
     * @param stepInstance 步骤实例
     * @return GSE任务下发请求结果
     */
    protected abstract GseTaskResponse startGseTask(StepInstanceDTO stepInstance);

    /**
     * 终止GSE任务
     *
     * @return GSE任务终止请求结果
     */
    public abstract GseTaskExecuteResult stopGseTask();

    @Override
    public void interrupt() {
        // 利用mq的ack机制即可，此处仅打印日志
        log.info("Interrupt running, stepInstanceId: {}", stepInstanceId);
    }

    public void setTracing(Tracing tracing) {
        this.tracing = tracing;
    }
}
