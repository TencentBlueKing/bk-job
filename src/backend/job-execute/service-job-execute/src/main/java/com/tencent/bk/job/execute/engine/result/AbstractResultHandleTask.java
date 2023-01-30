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
import com.tencent.bk.job.common.gse.GseClient;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.engine.consts.AgentTaskStatusEnum;
import com.tencent.bk.job.execute.engine.evict.TaskEvictPolicyExecutor;
import com.tencent.bk.job.execute.engine.listener.event.EventSource;
import com.tencent.bk.job.execute.engine.listener.event.GseTaskEvent;
import com.tencent.bk.job.execute.engine.listener.event.ResultHandleTaskResumeEvent;
import com.tencent.bk.job.execute.engine.listener.event.StepEvent;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.model.GseLogBatchPullResult;
import com.tencent.bk.job.execute.engine.model.GseTaskExecuteResult;
import com.tencent.bk.job.execute.engine.model.GseTaskResult;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.engine.model.TaskVariablesAnalyzeResult;
import com.tencent.bk.job.execute.engine.result.ha.ResultHandleTaskKeepaliveManager;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.AgentTaskService;
import com.tencent.bk.job.execute.service.GseTaskService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.StepInstanceVariableValueService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceVariableService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.StopWatch;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    protected GseTaskService gseTaskService;
    protected TaskInstanceVariableService taskInstanceVariableService;
    protected StepInstanceVariableValueService stepInstanceVariableValueService;
    protected TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;
    protected ResultHandleTaskKeepaliveManager resultHandleTaskKeepaliveManager;
    protected TaskEvictPolicyExecutor taskEvictPolicyExecutor;
    protected AgentTaskService agentTaskService;
    protected StepInstanceService stepInstanceService;
    protected GseClient gseClient;
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
     * 业务ID
     */
    protected long appId;
    /**
     * GSE 任务执行结果
     */
    protected GseTaskDTO gseTask;
    /**
     * GSE 主机任务执行结果，Map<AgentId, AgentTaskDTO>
     */
    protected Map<String, AgentTaskDTO> targetAgentTasks;
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
    protected Set<String> targetAgentIds = new HashSet<>();
    /**
     * 未开始任务的目标服务器
     */
    protected Set<String> notStartedTargetAgentIds = new HashSet<>();
    /**
     * 正在执行任务的目标服务器
     */
    protected Set<String> runningTargetAgentIds = new HashSet<>();

    // ---------------- analysed task execution result for server --------------------
    /**
     * 已经分析结果完成的目标服务器
     */
    protected Set<String> analyseFinishedTargetAgentIds = new HashSet<>();
    /**
     * 执行成功的目标服务器
     */
    protected Set<String> successTargetAgentIds = new HashSet<>();
    /**
     * Agent ID 与 host 映射关系
     */
    protected Map<String, HostDTO> agentIdHostMap;
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
    private final AtomicInteger pullLogTimes = new AtomicInteger(0);

    // ---------------- task lifecycle properties --------------------
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
    /**
     * 是否是GSE V2 TASK
     */
    protected boolean gseV2Task;
    // ---------------- task lifecycle properties --------------------


    protected AbstractResultHandleTask(TaskInstanceService taskInstanceService,
                                       GseTaskService gseTaskService,
                                       LogService logService,
                                       TaskInstanceVariableService taskInstanceVariableService,
                                       StepInstanceVariableValueService stepInstanceVariableValueService,
                                       TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
                                       ResultHandleTaskKeepaliveManager resultHandleTaskKeepaliveManager,
                                       TaskEvictPolicyExecutor taskEvictPolicyExecutor,
                                       AgentTaskService agentTaskService,
                                       StepInstanceService stepInstanceService,
                                       GseClient gseClient,
                                       TaskInstanceDTO taskInstance,
                                       StepInstanceDTO stepInstance,
                                       TaskVariablesAnalyzeResult taskVariablesAnalyzeResult,
                                       Map<String, AgentTaskDTO> targetAgentTasks,
                                       GseTaskDTO gseTask,
                                       String requestId) {
        this.taskInstanceService = taskInstanceService;
        this.gseTaskService = gseTaskService;
        this.logService = logService;
        this.taskInstanceVariableService = taskInstanceVariableService;
        this.stepInstanceVariableValueService = stepInstanceVariableValueService;
        this.taskExecuteMQEventDispatcher = taskExecuteMQEventDispatcher;
        this.resultHandleTaskKeepaliveManager = resultHandleTaskKeepaliveManager;
        this.taskEvictPolicyExecutor = taskEvictPolicyExecutor;
        this.agentTaskService = agentTaskService;
        this.stepInstanceService = stepInstanceService;
        this.gseClient = gseClient;
        this.requestId = requestId;
        this.taskInstance = taskInstance;
        this.taskInstanceId = taskInstance.getId();
        this.stepInstance = stepInstance;
        this.gseV2Task = stepInstance.isTargetGseV2Agent();
        this.appId = stepInstance.getAppId();
        this.stepInstanceId = stepInstance.getId();
        this.taskVariablesAnalyzeResult = taskVariablesAnalyzeResult;
        this.targetAgentTasks = targetAgentTasks;
        this.gseTask = gseTask;

        targetAgentTasks.values().forEach(agentTask -> this.targetAgentIds.add(agentTask.getAgentId()));
        this.notStartedTargetAgentIds.addAll(targetAgentIds);

        this.agentIdHostMap = stepInstanceService.computeStepHosts(stepInstance,
            host -> host.getAgentId() != null ? host.getAgentId() : host.toCloudIp());

        // 如果是执行方案，需要初始化全局变量
        if (taskInstance.isPlanInstance()) {
            List<TaskVariableDTO> taskVariables = taskVariablesAnalyzeResult.getTaskVars();
            if (taskVariables != null && !taskVariables.isEmpty()) {
                taskVariables.forEach(var -> initialVariables.put(var.getName(), var));
            }
        }
    }

    /**
     * 检查是否应当驱逐当前任务，若应当驱逐则驱逐并更新任务状态
     *
     * @param watch 外部传入的耗时统计watch对象
     * @return 是否应当驱逐当前任务
     */
    private boolean checkAndEvictTaskIfNeed(StopWatch watch) {
        watch.start("check-evict-task");
        if (taskEvictPolicyExecutor.shouldEvictTask(taskInstance)) {
            log.info("taskInstance {} evicted", taskInstance.getId());
            // 更新任务与步骤状态
            taskEvictPolicyExecutor.updateEvictedTaskStatus(taskInstance, stepInstance);
            // 停止日志拉取调度
            this.executeResult = GseTaskExecuteResult.DISCARDED;
            finishGseTask(this.executeResult, false);
            watch.stop();
            return true;
        }
        watch.stop();
        return false;
    }

    /**
     * 拉取GSE任务结果并分析
     *
     * @param watch 外部传入的耗时统计watch对象
     * @return 是否应当继续后续流程
     */
    private boolean pullGSEResultAndAnalyse(StopWatch watch) {
        log.info("[{}]: Start pull gse task result, times: {}", gseTask.getTaskUniqueName(),
            pullLogTimes.addAndGet(1));
        GseLogBatchPullResult<T> gseLogBatchPullResult;
        int batch = 0;
        do {
            batch++;
            if (checkAndEvictTaskIfNeed(watch)) {
                return false;
            }

            watch.start("pull-task-result-batch-" + batch);
            // 分批拉取GSE任务执行结果
            gseLogBatchPullResult = pullGseTaskResultInBatches();

            // 拉取结果校验
            if (!checkPullResult(gseLogBatchPullResult)) {
                return false;
            }

            // 检查任务异常并处理
            GseTaskResult<T> gseTaskResult = gseLogBatchPullResult.getGseTaskResult();
            if (determineTaskAbnormal(gseTaskResult)) {
                return false;
            }

            watch.stop();

            try {
                watch.start("analyse-task-result-batch-" + batch);
                this.executeResult = analyseGseTaskResult(gseTaskResult);
                watch.stop();
            } catch (Throwable e) {
                log.error("[" + gseTask.getTaskUniqueName() + "]: analyse gse task result error.", e);
                throw e;
            }
        } while (!gseLogBatchPullResult.isLastBatch());
        return true;
    }

    public void execute() {
        StopWatch watch = new StopWatch("Result-Handle-Task-" + stepInstanceId);
        String lockKey = buildGseTaskLockKey(gseTask);
        try {
            if (!checkTaskActiveAndSetRunningStatus()) {
                return;
            }
            if (checkAndEvictTaskIfNeed(watch)) {
                return;
            }

            watch.start("get-lock");
            if (!LockUtils.tryGetReentrantLock(lockKey, requestId, 30000L)) {
                log.error("Fail to get result handle lock, lockKey: {}", lockKey);
                this.executeResult = GseTaskExecuteResult.DISCARDED;
                return;
            }
            watch.stop();

            watch.start("check-skip-or-stop");
            if (shouldSkipStep()) {
                this.executeResult = GseTaskExecuteResult.SKIPPED;
                log.info("[{}]: Skip task, set unfinished ip task status to unknown!", gseTask.getTaskUniqueName());
                saveStatusWhenSkip();
                return;
            }
            terminateGseTaskIfDetectTaskStatusIsStopping();
            watch.stop();

            // 拉取执行结果日志
            if (!pullGSEResultAndAnalyse(watch)) {
                return;
            }

            // 如果任务已结束
            if (this.executeResult != GseTaskExecuteResult.RUNNING) {
                watch.start("finish-gse-task");
                finishGseTask(this.executeResult, true);
                watch.stop();
            }
        } catch (Throwable e) {
            log.error("[" + gseTask.getTaskUniqueName() + "]: result handle error.", e);
            this.executeResult = GseTaskExecuteResult.EXCEPTION;
            finishGseTask(this.executeResult, true);
        } finally {
            this.isRunning = false;
            LockUtils.releaseDistributedLock(lockKey, requestId);
            if (watch.isRunning()) {
                watch.stop();
            }
            if (watch.getTotalTimeMillis() > 1000L) {
                log.warn("AbstractResultHandleTask-> handle task result is slow, run statistics:{}",
                    watch.prettyPrint());
            }
        }
    }

    private String buildGseTaskLockKey(GseTaskDTO gseTask) {
        return "job:result:handle:" + gseTask.getId();
    }

    private boolean checkTaskActiveAndSetRunningStatus() {
        if (!isActive) {
            log.info("Task is inactive, task: {}", gseTask.getTaskUniqueName());
            return false;
        }
        this.isRunning = true;
        // 二次确认，防止isActive在设置this.isRunning=true期间发生变化
        if (!isActive) {
            log.info("Task is inactive, task: {}", gseTask.getTaskUniqueName());
            return false;
        }
        return true;
    }

    private void terminateGseTaskIfDetectTaskStatusIsStopping() {
        if (!isGseTaskTerminating) {
            this.taskInstance = taskInstanceService.getTaskInstance(taskInstanceId);
            // 如果任务处于“终止中”状态，触发任务终止
            if (this.taskInstance.getStatus() == RunStatusEnum.STOPPING) {
                log.info("Task instance status is stopping, stop executing the step! taskInstanceId:{}, " +
                        "task:{}",
                    taskInstance.getId(), gseTask.getTaskUniqueName());
                taskExecuteMQEventDispatcher.dispatchGseTaskEvent(GseTaskEvent.stopGseTask(
                    gseTask.getStepInstanceId(), gseTask.getExecuteCount(), gseTask.getBatch(), gseTask.getId()));
                this.isGseTaskTerminating = true;
                log.info("Send stop gse step control action successfully!");
            }
        }
    }

    private boolean shouldSkipStep() {
        StepInstanceBaseDTO stepInstance = taskInstanceService.getBaseStepInstance(stepInstanceId);
        return stepInstance == null || RunStatusEnum.SKIPPED == stepInstance.getStatus();
    }

    private void saveStatusWhenSkip() {
        List<AgentTaskDTO> notFinishedGseAgentTasks =
            targetAgentTasks.values().stream().filter(not(AgentTaskDTO::isFinished)).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(notFinishedGseAgentTasks)) {
            notFinishedGseAgentTasks.forEach(agentTask -> {
                agentTask.setStatus(AgentTaskStatusEnum.UNKNOWN);
                agentTask.setEndTime(System.currentTimeMillis());
            });
        }
        agentTaskService.batchUpdateAgentTasks(notFinishedGseAgentTasks);
    }

    /*
     * 检查任务是否异常，执行结果持续为空、超时未结束等
     */
    private boolean determineTaskAbnormal(GseTaskResult<?> gseTaskResult) {
        return checkTaskTimeout() || checkEmptyGseResult(gseTaskResult);
    }

    private boolean checkTaskTimeout() {
        long startTime = gseTask.getStartTime();
        long runDuration = System.currentTimeMillis() - startTime;
        boolean isTimeout = false;
        // 作业执行超时,但是GSE并没有按照预期结束作业;job最大容忍5min，然后判定任务异常
        long timeout = stepInstance.getTimeout() == null ?
            JobConstants.DEFAULT_JOB_TIMEOUT_SECONDS : stepInstance.getTimeout();
        if (runDuration - 1000 * timeout >= GSE_TASK_TIMEOUT_MAX_TOLERATION_MILLS) {
            log.warn("[{}]: Task execution timeout! runDuration: {}ms, timeout: {}s", gseTask.getTaskUniqueName(),
                runDuration, stepInstance.getTimeout());
            this.executeResult = GseTaskExecuteResult.FAILED;
            saveFailInfoForUnfinishedAgentTask(AgentTaskStatusEnum.LOG_ERROR,
                "Task execution may be abnormal or timeout.");
            finishGseTask(GseTaskExecuteResult.FAILED, true);
            isTimeout = true;
        }
        return isTimeout;
    }

    /*
     * 检查从GSE 拉取的任务执行结果是否持续为空
     */
    private boolean checkEmptyGseResult(GseTaskResult<?> gseTaskResult) {
        if (latestPullGseLogSuccessTimeMillis == 0) {
            latestPullGseLogSuccessTimeMillis = System.currentTimeMillis();
        }
        boolean isAbnormal = false;
        if (null == gseTaskResult || gseTaskResult.isNullResult()) {
            long currentTimeMillis = System.currentTimeMillis();
            // 执行结果持续为空
            if (currentTimeMillis - latestPullGseLogSuccessTimeMillis >= GSE_TASK_EMPTY_RESULT_MAX_TOLERATION_MILLS) {
                log.warn("[{}]: Execution result log always empty!", gseTask.getTaskUniqueName());
                this.executeResult = GseTaskExecuteResult.FAILED;
                saveFailInfoForUnfinishedAgentTask(AgentTaskStatusEnum.LOG_ERROR, "Execution result log always empty.");
                finishGseTask(GseTaskExecuteResult.FAILED, true);
                isAbnormal = true;
            }
        } else {
            latestPullGseLogSuccessTimeMillis = System.currentTimeMillis();
        }
        return isAbnormal;
    }

    private boolean checkPullResult(GseLogBatchPullResult<T> gseLogBatchPullResult) {
        if (!gseLogBatchPullResult.isSuccess()) {
            log.error("[{}] Pull gse task result error, errorMsg: {}", gseTask.getTaskUniqueName(),
                gseLogBatchPullResult.getErrorMsg());
            this.executeResult = GseTaskExecuteResult.FAILED;
            saveFailInfoForUnfinishedAgentTask(AgentTaskStatusEnum.LOG_ERROR, gseLogBatchPullResult.getErrorMsg());
            finishGseTask(GseTaskExecuteResult.FAILED, true);
            return false;
        }
        return true;
    }


    /**
     * 设置目标gent任务结束状态
     *
     * @param agentId   agentId
     * @param startTime 起始时间
     * @param endTime   终止时间
     * @param agentTask 日志
     */
    protected void dealTargetAgentFinish(String agentId, Long startTime, Long endTime, AgentTaskDTO agentTask) {
        log.info("[{}]: Deal target agent finished| agentId={}| startTime:{}, endTime:{}, agentTask:{}",
            gseTask.getTaskUniqueName(), agentId, startTime, endTime, JsonUtils.toJsonWithoutSkippedFields(agentTask));

        notStartedTargetAgentIds.remove(agentId);
        runningTargetAgentIds.remove(agentId);
        analyseFinishedTargetAgentIds.add(agentId);

        if (endTime - startTime <= 0) {
            agentTask.setTotalTime(100L);
        } else {
            agentTask.setTotalTime(endTime - startTime);
        }
        agentTask.setStartTime(startTime);
        agentTask.setEndTime(endTime);
    }

    /**
     * 设置GSE TASK 完成状态并分发事件
     *
     * @param result               任务执行结果
     * @param dispatchRefreshEvent 是否分发Refresh事件
     */
    private void finishGseTask(GseTaskExecuteResult result, boolean dispatchRefreshEvent) {
        int gseTaskExecuteResult = result.getResultCode();

        // 处理GSE任务执行结果
        log.info("Finish gse task, task:{}, executeResult:{}", gseTask.getTaskUniqueName(), gseTaskExecuteResult);

        long startTime = this.gseTask.getStartTime();
        long endTime = DateUtils.currentTimeMillis();
        long gseTotalTime = endTime - startTime;

        updateGseTaskExecutionInfo(result, endTime, gseTotalTime);

        if (dispatchRefreshEvent) {
            taskExecuteMQEventDispatcher.dispatchStepEvent(StepEvent.refreshStep(stepInstanceId,
                EventSource.buildGseTaskEventSource(stepInstanceId, stepInstance.getExecuteCount(),
                    stepInstance.getBatch(), gseTask.getId())));
        }
    }

    private void updateGseTaskExecutionInfo(GseTaskExecuteResult result, long endTime,
                                            long totalTime) {

        gseTask.setStatus(analyseGseTaskStatus(result).getValue());
        gseTask.setEndTime(endTime);
        gseTask.setTotalTime(totalTime);
        gseTaskService.updateGseTask(gseTask);
    }

    private RunStatusEnum analyseGseTaskStatus(GseTaskExecuteResult result) {
        if (result.equals(GseTaskExecuteResult.RUNNING)) {
            return RunStatusEnum.RUNNING;
        } else if (result.equals(GseTaskExecuteResult.SUCCESS)) {
            return RunStatusEnum.SUCCESS;
        } else if (result.equals(GseTaskExecuteResult.FAILED)) {
            return RunStatusEnum.FAIL;
        } else if (result.equals(GseTaskExecuteResult.STOP_SUCCESS)) {
            return RunStatusEnum.STOP_SUCCESS;
        } else if (result.equals(GseTaskExecuteResult.DISCARDED)) {
            return RunStatusEnum.ABANDONED;
        } else if (result.equals(GseTaskExecuteResult.EXCEPTION)) {
            return RunStatusEnum.ABNORMAL_STATE;
        } else if (result.equals(GseTaskExecuteResult.SKIPPED)) {
            return RunStatusEnum.SKIPPED;
        } else {
            return RunStatusEnum.FAIL;
        }
    }

    /**
     * 批量更新AgentTask并重置changed标志
     *
     * @param agentTasks agent任务列表
     */
    protected void batchSaveChangedGseAgentTasks(Collection<AgentTaskDTO> agentTasks) {
        if (CollectionUtils.isNotEmpty(agentTasks)) {
            List<AgentTaskDTO> changedGseAgentTasks =
                agentTasks.stream().filter(AgentTaskDTO::isChanged).collect(Collectors.toList());
            agentTaskService.batchUpdateAgentTasks(changedGseAgentTasks);
            changedGseAgentTasks.forEach(agentTask -> agentTask.setChanged(false));
        }
    }

    protected void saveFailInfoForUnfinishedAgentTask(AgentTaskStatusEnum status, String errorMsg) {
        log.info("[{}]: Deal unfinished agent result| noStartJobAgentIds : {}| runningJobAgentIds : {}",
            gseTask.getTaskUniqueName(), notStartedTargetAgentIds, runningTargetAgentIds);
        Set<String> unfinishedAgentIds = new HashSet<>();
        unfinishedAgentIds.addAll(notStartedTargetAgentIds);
        unfinishedAgentIds.addAll(runningTargetAgentIds);
        long startTime = (gseTask != null && gseTask.getStartTime() != null) ?
            gseTask.getStartTime() : System.currentTimeMillis();
        for (String agentId : unfinishedAgentIds) {
            AgentTaskDTO agentTask = targetAgentTasks.get(agentId);
            agentTask.setStartTime(startTime);
            agentTask.setEndTime(System.currentTimeMillis());
            agentTask.setStatus(status);
        }
        batchSaveChangedGseAgentTasks(targetAgentTasks.values());
    }

    @Override
    public void stop() {
        synchronized (stopMonitor) {
            if (!isStopped) {
                this.isActive = false;
                tryStopImmediately();
            } else {
                log.info("Task is stopped, task: {}", gseTask.getTaskUniqueName());
            }
        }
    }

    private void tryStopImmediately() {
        if (!this.isRunning) {
            log.info("ResultHandleTask-onStop start, task: {}", gseTask.getTaskUniqueName());
            resultHandleTaskKeepaliveManager.stopKeepaliveInfoTask(getTaskId());
            taskExecuteMQEventDispatcher.dispatchResultHandleTaskResumeEvent(
                ResultHandleTaskResumeEvent.resume(gseTask.getStepInstanceId(),
                    gseTask.getExecuteCount(), gseTask.getBatch(), gseTask.getId(), requestId));

            this.isStopped = true;
            StopTaskCounter.getInstance().decrement(getTaskId());
            log.info("ResultHandleTask-onStop end, task: {}", gseTask.getTaskUniqueName());
        } else {
            log.info("ResultHandleTask-onStop, task is running now, will stop when idle. stepInstanceId: {}",
                stepInstanceId);
        }
    }

    public long getAppId() {
        return appId;
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
     * 获取执行结果
     *
     * @return 执行结果
     */
    protected final GseTaskExecuteResult getExecuteResult() {
        return this.executeResult;
    }

    /**
     * 分批拉取GSE任务结果
     *
     * @return 日志
     */
    abstract GseLogBatchPullResult<T> pullGseTaskResultInBatches();

    /**
     * 解析GSE任务结果
     *
     * @param gseTaskResult GSE日志
     * @return 任务执行结果
     */
    abstract GseTaskExecuteResult analyseGseTaskResult(GseTaskResult<T> gseTaskResult);
}
