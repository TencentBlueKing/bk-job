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

import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.engine.TaskExecuteControlMsgSender;
import com.tencent.bk.job.execute.engine.consts.IpStatus;
import com.tencent.bk.job.execute.engine.model.*;
import com.tencent.bk.job.execute.engine.result.ha.ResultHandleTaskKeepaliveManager;
import com.tencent.bk.job.execute.engine.util.IpHelper;
import com.tencent.bk.job.execute.model.*;
import com.tencent.bk.job.execute.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.StopWatch;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.tencent.bk.job.common.util.function.LambdasUtil.not;

/**
 * GSE任务执行结果公共处理类
 *
 * @param <T>
 */
@Slf4j
public abstract class AbstractResultHandleTask<T> implements ContinuousScheduledTask {
    /**
     * GSE任务执行结果为空,Job最大容忍时间,5min.用于异常情况下的任务自动终止，防止长时间占用系统资源
     */
    private static final int GSE_TASK_EMPTY_RESULT_MAX_TOLERATION_MILLS = 300_000;
    /**
     * GSE任务超时未结束,Job最大容忍时间。5min.用于异常情况下的任务自动终止，防止长时间占用系统资源
     */
    private static final int GSE_TASK_TIMEOUT_MAX_TOLERATION_MILLS = 300_000;

    /*
     * 同步锁
     */
    private final Object stopMonitor = new Object();
    // ---------------- dependent service --------------------
    protected LogService logService;
    protected TaskInstanceService taskInstanceService;
    protected GseTaskLogService gseTaskLogService;
    protected TaskInstanceVariableService taskInstanceVariableService;
    protected StepInstanceVariableValueService stepInstanceVariableValueService;
    // ---------------- dependent service --------------------
    protected TaskExecuteControlMsgSender taskManager;
    protected ResultHandleTaskKeepaliveManager resultHandleTaskKeepaliveManager;
    /**
     * 任务请求的requestId，用于防止重复下发任务
     */
    protected String requestId;
    /**
     * 任务实例
     */
    protected TaskInstanceDTO taskInstance;
    /**
     * 作业步骤实例
     */
    protected StepInstanceDTO stepInstance;
    /**
     * 任务实例ID
     */
    protected long taskInstanceId;
    /**
     * 步骤实例ID
     */
    protected long stepInstanceId;
    /**
     * GSE 任务执行结果
     */
    protected GseTaskLogDTO gseTaskLog;
    /**
     * GSE 主机任务执行结果
     */
    protected Map<String, GseTaskIpLogDTO> ipLogMap;
    /**
     * 全局参数分析结果
     */
    protected TaskVariablesAnalyzeResult taskVariablesAnalyzeResult;
    /**
     * 全局变量初始值
     */
    protected Map<String, TaskVariableDTO> initialVariables = new HashMap<>();
    /**
     * 任务包含的所有目标服务器
     */
    protected Set<String> targetIpSet = new HashSet<>();
    /**
     * 未开始任务的服务器
     */
    protected Set<String> notStartedIpSet = new HashSet<>();
    /**
     * 正在执行任务的服务器
     */
    protected Set<String> runningIpSet = new HashSet<>();

    // ---------------- analysed task execution result for server --------------------
    /**
     * 已经分析结果完成的服务器
     */
    protected Set<String> analyseFinishedIpSet = new HashSet<>();
    /**
     * 执行成功的服务器
     */
    protected Set<String> successIpSet = new HashSet<>();
    /**
     * 不合法的服务器
     */
    protected Set<String> invalidIpSet = new HashSet<>();
    /**
     * 任务成功被终止
     */
    protected boolean isTerminatedSuccess = false;
    /**
     * 任务是否在运行中
     */
    protected volatile boolean isRunning = false;
    /**
     * 任务是否已停止
     */
    protected volatile boolean isStopped = false;
    // ---------------- analysed task execution result for server --------------------
    /**
     * 任务是否启用
     */
    protected volatile boolean isActive = true;
    /**
     * 拉取执行结果次数
     */
    private AtomicInteger pullLogTimes = new AtomicInteger(0);

    // ---------------- task lifecycle properties --------------------
    /**
     * 拉取执行结果失败次数
     */
    private AtomicInteger pullLogFailCount = new AtomicInteger(0);
    /**
     * 最近一次成功拉取GSE执行结果的时间
     */
    private long latestPullGseLogSuccessTimeMillis;
    /**
     * 任务执行结果
     */
    private GseTaskExecuteResult executeResult = GseTaskExecuteResult.RUNNING;
    /**
     * GSE任务是否处于终止状态
     */
    private volatile boolean isGseTaskTerminating = false;
    // ---------------- task lifecycle properties --------------------


    /**
     * Constructor
     *
     * @param taskInstance               作业实例
     * @param stepInstance               步骤实例
     * @param taskVariablesAnalyzeResult 变量信息
     * @param ipLogMap                   GSE任务-主机-信息
     * @param gseTaskLog                 GSE任务整体信息
     * @param targetIps                  目标服务器IP
     * @param requestId                  请求ID,防止重复执行
     */
    protected AbstractResultHandleTask(TaskInstanceDTO taskInstance,
                                       StepInstanceDTO stepInstance,
                                       TaskVariablesAnalyzeResult taskVariablesAnalyzeResult,
                                       Map<String, GseTaskIpLogDTO> ipLogMap,
                                       GseTaskLogDTO gseTaskLog,
                                       Set<String> targetIps,
                                       String requestId) {
        this.requestId = requestId;
        this.taskInstance = taskInstance;
        this.taskInstanceId = taskInstance.getId();
        this.stepInstance = stepInstance;
        this.stepInstanceId = stepInstance.getId();
        this.taskVariablesAnalyzeResult = taskVariablesAnalyzeResult;
        this.ipLogMap = ipLogMap;
        this.gseTaskLog = gseTaskLog;
        this.targetIpSet.addAll(targetIps);
        this.notStartedIpSet.addAll(targetIps);
        if (CollectionUtils.isNotEmpty(stepInstance.getInvalidIps())) {
            this.invalidIpSet.addAll(stepInstance.getInvalidIps());
        }

        List<TaskVariableDTO> taskVariables = taskVariablesAnalyzeResult.getTaskVars();
        if (taskVariables != null && !taskVariables.isEmpty()) {
            taskVariables.forEach(var -> initialVariables.put(var.getName(), var));
        }
    }

    /**
     * 初始化依赖的服务
     */
    public void initDependentService(TaskInstanceService taskInstanceService,
                                     GseTaskLogService gseTaskLogService,
                                     LogService logService,
                                     TaskInstanceVariableService taskInstanceVariableService,
                                     StepInstanceVariableValueService stepInstanceVariableValueService,
                                     TaskExecuteControlMsgSender taskManager,
                                     ResultHandleTaskKeepaliveManager resultHandleTaskKeepaliveManager) {
        this.taskInstanceService = taskInstanceService;
        this.gseTaskLogService = gseTaskLogService;
        this.logService = logService;
        this.taskInstanceVariableService = taskInstanceVariableService;
        this.stepInstanceVariableValueService = stepInstanceVariableValueService;
        this.taskManager = taskManager;
        this.resultHandleTaskKeepaliveManager = resultHandleTaskKeepaliveManager;
    }

    public void execute() {
        StopWatch watch = new StopWatch("Result-Handle-Task-" + stepInstanceId);
        try {
            if (!checkTaskActiveAndSetRunningStatus()) {
                return;
            }
            watch.start("get-lock");
            if (!LockUtils.tryGetReentrantLock("job:result:handle:" + stepInstanceId, requestId, 30000L)) {
                log.info("Fail to get result handle lock, stepInstanceId: {}", stepInstanceId);
                this.executeResult = GseTaskExecuteResult.DISCARDED;
                return;
            }
            watch.stop();

            watch.start("check-skip-or-stop");
            if (shouldSkipStep()) {
                this.executeResult = GseTaskExecuteResult.SKIPPED;
                log.info("[{}]: Skip task, set unfinished ip task status to unknown!", stepInstanceId);
                saveStatusWhenSkip();
                return;
            }
            terminateGseTaskIfDetectTaskStatusIsStopping();
            watch.stop();

            // 拉取执行结果日志
            log.info("[{}]: Start pull log, times: {}", stepInstanceId, pullLogTimes.addAndGet(1));
            GseLogBatchPullResult<T> gseLogBatchPullResult;
            int batch = 0;
            do {
                batch++;
                watch.start("pull-task-result-batch-" + batch);
                // 分批拉取GSE任务执行结果
                gseLogBatchPullResult = pullGseTaskLogInBatches();

                // 拉取结果校验
                if (!checkPullResult(gseLogBatchPullResult)) {
                    return;
                }

                // 检查任务异常并处理
                GseLog<T> gseLog = gseLogBatchPullResult.getGseLog();
                if (determineTaskAbnormal(gseLog)) {
                    return;
                }
                watch.stop();

                try {
                    watch.start("analyse-task-result-batch-" + batch);
                    this.executeResult = analyseGseTaskLog(gseLog);
                    watch.stop();
                } catch (Exception e) {
                    log.error("[" + stepInstanceId + "]: analyse gse task log result error.", e);
                    this.executeResult = GseTaskExecuteResult.FAILED;
                    handleExecuteResult(this.executeResult);
                    return;
                }
            } while (!gseLogBatchPullResult.isLastBatch());

            watch.start("handle-execute-result");
            handleExecuteResult(this.executeResult);
            watch.stop();
        } finally {
            this.isRunning = false;
            LockUtils.releaseDistributedLock("job:result:handle:", String.valueOf(stepInstanceId), requestId);
            if (watch.isRunning()) {
                watch.stop();
            }
            if (watch.getTotalTimeMillis() > 1000L) {
                log.warn("AbstractResultHandleTask-> handle task result is slow, run statistics:{}",
                    watch.prettyPrint());
            }
        }
    }

    private boolean checkTaskActiveAndSetRunningStatus() {
        if (!isActive) {
            log.info("Task is inactive, stepInstanceId: {}", stepInstanceId);
            return false;
        }
        this.isRunning = true;
        // 二次确认，防止isActive在设置this.isRunning=true期间发生变化
        if (!isActive) {
            log.info("Task is inactive, stepInstanceId: {}", stepInstanceId);
            return false;
        }
        return true;
    }

    private void terminateGseTaskIfDetectTaskStatusIsStopping() {
        if (!isGseTaskTerminating) {
            this.taskInstance = taskInstanceService.getTaskInstance(taskInstanceId);
            // 如果任务处于“终止中”状态，触发任务终止
            if (this.taskInstance.getStatus().equals(RunStatusEnum.STOPPING.getValue())) {
                log.info("Task instance status is stopping, stop executing the step! taskInstanceId:{}, " +
                        "stepInstanceId:{}",
                    taskInstance.getId(), stepInstance.getId());
                taskManager.stopGseStep(stepInstanceId);
                this.isGseTaskTerminating = true;
                log.info("Send stop gse step control action successfully!");
            }
        }
    }

    private boolean shouldSkipStep() {
        StepInstanceBaseDTO stepInstance = taskInstanceService.getBaseStepInstance(stepInstanceId);
        return stepInstance == null || RunStatusEnum.SKIPPED.getValue().equals(stepInstance.getStatus());
    }

    private void saveStatusWhenSkip() {
        List<GseTaskIpLogDTO> notFinishedIpLogs =
            ipLogMap.values().stream().filter(not(GseTaskIpLogDTO::isFinished)).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(notFinishedIpLogs)) {
            notFinishedIpLogs.forEach(ipLog -> {
                ipLog.setStatus(IpStatus.UNKNOWN.getValue());
                ipLog.setEndTime(System.currentTimeMillis());
            });
        }
        gseTaskLogService.batchSaveIpLog(notFinishedIpLogs);
    }

    /*
     * 检查任务是否异常，执行结果持续为空、超时未结束等
     */
    private boolean determineTaskAbnormal(GseLog<?> gseLog) {
        return checkTaskTimeout() || checkEmptyGseResult(gseLog);
    }

    private boolean checkTaskTimeout() {
        long startTime = gseTaskLog.getStartTime();
        long runDuration = System.currentTimeMillis() - startTime;
        boolean isTimeout = false;
        // 作业执行超时,但是GSE并没有按照预期结束作业;job最大容忍5min，然后判定任务异常
        long timeout = stepInstance.getTimeout() == null ?
                JobConstants.DEFAULT_JOB_TIMEOUT_SECONDS : stepInstance.getTimeout();
        if (runDuration - 1000 * timeout >= GSE_TASK_TIMEOUT_MAX_TOLERATION_MILLS) {
            log.warn("[{}]: Task execution timeout! runDuration: {}ms, timeout: {}s", stepInstanceId, runDuration,
                    stepInstance.getTimeout());
            this.executeResult = GseTaskExecuteResult.FAILED;
            saveFailInfoForUnfinishedIpTask(IpStatus.LOG_ERROR.getValue(),
                    "Task execution may be abnormal or timeout.");
            handleExecuteResult(GseTaskExecuteResult.FAILED);
            isTimeout = true;
        }
        return isTimeout;
    }

    /*
     * 检查从GSE 拉取的任务执行结果是否持续为空
     */
    private boolean checkEmptyGseResult(GseLog<?> gseLog) {
        if (latestPullGseLogSuccessTimeMillis == 0) {
            latestPullGseLogSuccessTimeMillis = System.currentTimeMillis();
        }
        boolean isAbnormal = false;
        if (null == gseLog || gseLog.isNullResp()) {
            long currentTimeMillis = System.currentTimeMillis();
            // 执行结果持续为空
            if (currentTimeMillis - latestPullGseLogSuccessTimeMillis >= GSE_TASK_EMPTY_RESULT_MAX_TOLERATION_MILLS) {
                log.warn("[{}]: Execution result log always empty!", stepInstanceId);
                this.executeResult = GseTaskExecuteResult.FAILED;
                saveFailInfoForUnfinishedIpTask(IpStatus.LOG_ERROR.getValue(), "Execution result log always empty.");
                handleExecuteResult(GseTaskExecuteResult.FAILED);
                isAbnormal = true;
            }
        } else {
            latestPullGseLogSuccessTimeMillis = System.currentTimeMillis();
        }
        return isAbnormal;
    }

    private boolean checkPullResult(GseLogBatchPullResult<T> gseLogBatchPullResult) {
        if (!gseLogBatchPullResult.isSuccess()) {
            log.error("[{}] Pull gse log error, errorMsg: {}", stepInstanceId, gseLogBatchPullResult.getErrorMsg());
            this.executeResult = GseTaskExecuteResult.FAILED;
            saveFailInfoForUnfinishedIpTask(IpStatus.LOG_ERROR.getValue(), gseLogBatchPullResult.getErrorMsg());
            handleExecuteResult(GseTaskExecuteResult.FAILED);
            return false;
        }

        GseLog<T> gseLog = gseLogBatchPullResult.getGseLog();
        if (null != gseLog && !gseLog.isNullResp() && gseLog.isError()) {
            log.error("[{}]: Pull gse log error", stepInstanceId);
            // 如果日志返回错误，默认重试3次
            int failPullLogTimes = this.pullLogFailCount.incrementAndGet();
            if (failPullLogTimes <= 3) {
                return true;
            }
            this.executeResult = GseTaskExecuteResult.FAILED;
            saveFailInfoForUnfinishedIpTask(IpStatus.LOG_ERROR.getValue(), gseLog.getErrorMsg());
            handleExecuteResult(this.executeResult);
            return false;
        }
        this.pullLogFailCount.set(0);
        return true;
    }


    protected void dealIPFinish(String cloudIp, Long startTime, Long endTime, GseTaskIpLogDTO ipLog) {
        log.info("[{}]: Deal ip finished| ip={}| startTime:{}, endTime:{}, ipLog:{}",
            stepInstanceId, cloudIp, startTime, endTime, JsonUtils.toJsonWithoutSkippedFields(ipLog));

        notStartedIpSet.remove(cloudIp);
        runningIpSet.remove(cloudIp);
        analyseFinishedIpSet.add(cloudIp);

        if (endTime - startTime <= 0) {
            ipLog.setTotalTime(100L);
        } else {
            ipLog.setTotalTime(endTime - startTime);
        }
        ipLog.setStartTime(startTime);
        ipLog.setEndTime(endTime);
    }

    /**
     * 处理任务结果
     *
     * @param result 任务执行结果
     */
    private void handleExecuteResult(GseTaskExecuteResult result) {
        int gseTaskExecuteResult = result.getResultCode();
        // 如果任务正在执行中
        if (gseTaskExecuteResult == GseTaskExecuteResult.RESULT_CODE_RUNNING) {
            return;
        }

        // 处理GSE任务执行结果,并更新任务步骤状态
        log.info("Handle execute result, stepInstanceId:{}, executeResult:{}", stepInstanceId, gseTaskExecuteResult);

        long startTime = this.gseTaskLog.getStartTime();
        long endTime = DateUtils.currentTimeMillis();
        long gseTotalTime = endTime - startTime;
        long stepTotalTime = endTime - stepInstance.getStartTime();

        int targetIpNum = this.targetIpSet.size();
        int allSuccessIPNum = this.successIpSet.size();
        int invalidIpNum = this.invalidIpSet == null ? 0 : this.invalidIpSet.size();
        int successTargetIpNum = successIpSet.size();
        int failTargetIpNum = targetIpNum - successTargetIpNum;

        boolean isSuccess = CollectionUtils.isEmpty(this.invalidIpSet) && allSuccessIPNum == targetIpNum;

        saveGseTaskLogExecutionInfo(result, isSuccess, endTime, gseTotalTime);

        if (gseTaskExecuteResult == GseTaskExecuteResult.RESULT_CODE_STOP_SUCCESS) {
            int stepStatus = taskInstanceService.getBaseStepInstance(stepInstanceId).getStatus();
            if (stepStatus == RunStatusEnum.STOPPING.getValue() || stepStatus == RunStatusEnum.RUNNING.getValue()) {
                taskInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.STOP_SUCCESS,
                    startTime, endTime, stepTotalTime, targetIpNum + invalidIpNum,
                    successTargetIpNum, failTargetIpNum + invalidIpNum);
                taskManager.refreshTask(stepInstance.getTaskInstanceId());
            }
        } else {
            int stepStatus = taskInstanceService.getBaseStepInstance(stepInstanceId).getStatus();
            if (gseTaskExecuteResult == GseTaskExecuteResult.RESULT_CODE_FAILED) {
                taskInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.FAIL,
                    startTime, endTime, stepTotalTime, targetIpNum + invalidIpNum,
                    successTargetIpNum, failTargetIpNum + invalidIpNum);
            } else if (stepStatus == RunStatusEnum.STOPPING.getValue()) {
                taskInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.STOP_SUCCESS,
                    startTime, endTime, stepTotalTime, targetIpNum + invalidIpNum,
                    successTargetIpNum, failTargetIpNum + invalidIpNum);
            } else if (gseTaskExecuteResult == GseTaskExecuteResult.RESULT_CODE_SUCCESS) {
                taskInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.SUCCESS,
                    startTime, endTime, stepTotalTime, targetIpNum + invalidIpNum,
                    successTargetIpNum, failTargetIpNum + invalidIpNum);
            }
            taskManager.refreshTask(stepInstance.getTaskInstanceId());
        }
    }

    private void saveGseTaskLogExecutionInfo(GseTaskExecuteResult result, boolean isSuccess, long endTime,
                                             long totalTime) {
        if (GseTaskExecuteResult.RESULT_CODE_STOP_SUCCESS == result.getResultCode()) {
            gseTaskLog.setStatus(RunStatusEnum.STOP_SUCCESS.getValue());
        } else {
            gseTaskLog.setStatus(isSuccess ? RunStatusEnum.SUCCESS.getValue() : RunStatusEnum.FAIL.getValue());
        }

        gseTaskLog.setEndTime(endTime);
        gseTaskLog.setTotalTime(totalTime);
        gseTaskLogService.saveGseTaskLog(gseTaskLog);
    }

    protected void saveFailInfoForUnfinishedIpTask(int errorType, String errorMsg) {
        log.info("[{}]: Deal unfinished ip result| noStartJobIPSet={}| runningJobIPSet={}",
            stepInstanceId, this.notStartedIpSet, this.runningIpSet);
        Set<String> unfinishedIPSet = new HashSet<>();
        unfinishedIPSet.addAll(notStartedIpSet);
        unfinishedIPSet.addAll(this.runningIpSet);
        long startTime = (this.gseTaskLog != null && this.gseTaskLog.getStartTime() != null) ?
            this.gseTaskLog.getStartTime() : System.currentTimeMillis();
        batchSaveFailIpLog(unfinishedIPSet, startTime, System.currentTimeMillis(), errorType);
    }

    private void batchSaveFailIpLog(Collection<String> ipSet, long startTime, long endTime, int status) {
        List<GseTaskIpLogDTO> ipLogList = new ArrayList<>();
        for (String ip : ipSet) {
            GseTaskIpLogDTO ipLog = new GseTaskIpLogDTO();
            ipLog.setStepInstanceId(stepInstanceId);
            ipLog.setExecuteCount(stepInstance.getExecuteCount());
            IpDTO ipDto = IpHelper.transform(ip);
            ipLog.setCloudAreaAndIp(IpHelper.compose(ipDto));
            ipLog.setDisplayIp(ipDto.getIp());
            ipLog.setStartTime(startTime);
            ipLog.setEndTime(endTime);
            ipLog.setStatus(status);
            ipLogList.add(ipLog);
        }
        gseTaskLogService.batchSaveIpLog(ipLogList);
    }

    @Override
    public void stop() {
        synchronized (stopMonitor) {
            if (!isStopped) {
                this.isActive = false;
                tryStopImmediately();
            } else {
                log.info("Task is stopped, stepInstanceId: {}", stepInstanceId);
            }
        }
    }

    private void tryStopImmediately() {
        if (!this.isRunning) {
            log.info("ResultHandleTask-onStop start, stepInstanceId: {}", stepInstanceId);
            resultHandleTaskKeepaliveManager.stopKeepaliveInfoTask(getTaskId());
            taskManager.resumeGseStep(stepInstanceId, stepInstance.getExecuteCount(), requestId);
            this.isStopped = true;
            StopTaskCounter.getInstance().decrement(getTaskId());
            log.info("ResultHandleTask-onStop end, stepInstanceId: {}", stepInstanceId);
        } else {
            log.info("ResultHandleTask-onStop, task is running now, will stop when idle. stepInstanceId: {}",
                stepInstanceId);
        }
    }

    @Override
    public String getTaskId() {
        return "gse_task:" + this.stepInstance.getId() + ":" + this.stepInstance.getExecuteCount();
    }

    @Override
    public String getTaskType() {
        if (stepInstance.isScriptStep()) {
            return "script";
        } else if (stepInstance.isFileStep()) {
            return "file";
        } else {
            return "default";
        }
    }

    /**
     * 获取G步骤执行结果
     *
     * @return 执行结果
     */
    protected final GseTaskExecuteResult getExecuteResult() {
        return this.executeResult;
    }

    /**
     * 分批拉取GSE日志
     *
     * @return 日志
     */
    abstract GseLogBatchPullResult<T> pullGseTaskLogInBatches();

    /**
     * 解析GSE日志并获取结果
     *
     * @param gseLog GSE日志
     * @return 任务执行结果
     */
    abstract GseTaskExecuteResult analyseGseTaskLog(GseLog<T> gseLog);
}
