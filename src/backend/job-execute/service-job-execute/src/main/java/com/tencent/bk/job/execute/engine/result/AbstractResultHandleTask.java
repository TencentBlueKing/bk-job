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

package com.tencent.bk.job.execute.engine.result;

import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.gse.IGseClient;
import com.tencent.bk.job.common.gse.v2.model.ExecuteObjectGseKey;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.config.PollingStrategyProperties;
import com.tencent.bk.job.execute.engine.EngineDependentServiceHolder;
import com.tencent.bk.job.execute.engine.consts.ExecuteObjectTaskStatusEnum;
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
import com.tencent.bk.job.execute.engine.quota.limit.RunningJobKeepaliveManager;
import com.tencent.bk.job.execute.engine.result.ha.ResultHandleTaskKeepaliveManager;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.ExecuteObjectTaskService;
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
     * GSE任务执行结果为空,Job最大容忍时间1min.用于异常情况下的任务自动终止，防止长时间占用系统资源
     */
    private static final int GSE_TASK_EMPTY_RESULT_MAX_TOLERATION_MILLS = 60_000;
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
    protected ExecuteObjectTaskService executeObjectTaskService;
    protected StepInstanceService stepInstanceService;
    protected IGseClient gseClient;
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
     * GSE 任务执行
     */
    protected GseTaskDTO gseTask;
    /**
     * 执行对象任务列表(全量)
     */
    protected List<ExecuteObjectTask> executeObjectTasks;
    /**
     * GSE任务与JOB执行对象任务的映射关系
     */
    protected Map<ExecuteObjectGseKey, ExecuteObjectTask> targetExecuteObjectTasks;
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
    protected Set<ExecuteObjectGseKey> targetExecuteObjectGseKeys = new HashSet<>();
    /**
     * 未结束的目标服务器
     */
    protected Set<ExecuteObjectGseKey> notFinishedTargetExecuteObjectGseKeys = new HashSet<>();
    /**
     * 已经分析结果完成的目标服务器
     */
    protected Set<ExecuteObjectGseKey> analyseFinishedTargetExecuteObjectGseKeys = new HashSet<>();
    /**
     * 执行成功的目标服务器
     */
    protected Set<ExecuteObjectGseKey> successTargetExecuteObjectGseKeys = new HashSet<>();
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

    /**
     * 任务是否启用
     */
    protected volatile boolean isActive = true;
    /**
     * 拉取执行结果次数
     */
    private final AtomicInteger pullLogTimes = new AtomicInteger(0);


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
     * 是否存在不可执行的目标执行对象
     */
    protected boolean existNoExecutableTargetExecuteObject;
    /**
     * GSE 任务信息，用于日志输出
     */
    protected String gseTaskInfo;

    protected final RunningJobKeepaliveManager runningJobKeepaliveManager;

    private final TaskContext taskContext;

    /**
     * 轮询策略配置
     */
    protected final PollingStrategyProperties pollingStrategyProperties;

    protected AbstractResultHandleTask(EngineDependentServiceHolder engineDependentServiceHolder,
                                       ExecuteObjectTaskService executeObjectTaskService,
                                       TaskInstanceDTO taskInstance,
                                       StepInstanceDTO stepInstance,
                                       TaskVariablesAnalyzeResult taskVariablesAnalyzeResult,
                                       Map<ExecuteObjectGseKey, ExecuteObjectTask> targetExecuteObjectTasks,
                                       GseTaskDTO gseTask,
                                       String requestId,
                                       List<ExecuteObjectTask> executeObjectTasks) {
        this.taskInstanceService = engineDependentServiceHolder.getTaskInstanceService();
        this.gseTaskService = engineDependentServiceHolder.getGseTaskService();
        this.logService = engineDependentServiceHolder.getLogService();
        this.taskInstanceVariableService = engineDependentServiceHolder.getTaskInstanceVariableService();
        this.stepInstanceVariableValueService = engineDependentServiceHolder.getStepInstanceVariableValueService();
        this.taskExecuteMQEventDispatcher = engineDependentServiceHolder.getTaskExecuteMQEventDispatcher();
        this.resultHandleTaskKeepaliveManager = engineDependentServiceHolder.getResultHandleTaskKeepaliveManager();
        this.taskEvictPolicyExecutor = engineDependentServiceHolder.getTaskEvictPolicyExecutor();
        this.stepInstanceService = engineDependentServiceHolder.getStepInstanceService();
        this.gseClient = engineDependentServiceHolder.getGseClient();
        this.runningJobKeepaliveManager = engineDependentServiceHolder.getRunningJobKeepaliveManager();
        this.pollingStrategyProperties = engineDependentServiceHolder.getPollingStrategyProperties();

        this.executeObjectTaskService = executeObjectTaskService;

        this.requestId = requestId;
        this.taskInstance = taskInstance;
        this.taskInstanceId = taskInstance.getId();
        this.stepInstance = stepInstance;
        this.appId = stepInstance.getAppId();
        this.stepInstanceId = stepInstance.getId();
        this.taskVariablesAnalyzeResult = taskVariablesAnalyzeResult;
        this.targetExecuteObjectTasks = targetExecuteObjectTasks;
        this.gseTask = gseTask;
        this.executeObjectTasks = executeObjectTasks;
        this.gseTaskInfo = buildGseTaskInfo(stepInstance.getTaskInstanceId(), gseTask);
        this.taskContext = new TaskContext(taskInstanceId);

        targetExecuteObjectTasks.values().forEach(executeObjectTask ->
            this.targetExecuteObjectGseKeys.add(executeObjectTask.getExecuteObject().toExecuteObjectGseKey()));
        this.notFinishedTargetExecuteObjectGseKeys.addAll(targetExecuteObjectGseKeys);

        // 如果是执行方案，需要初始化全局变量
        if (taskInstance.isPlanInstance()) {
            List<TaskVariableDTO> taskVariables = taskVariablesAnalyzeResult.getTaskVars();
            if (taskVariables != null && !taskVariables.isEmpty()) {
                taskVariables.forEach(var -> initialVariables.put(var.getName(), var));
            }
        }

        this.existNoExecutableTargetExecuteObject =
            executeObjectTasks.stream().
                filter(ExecuteObjectTask::isTarget)
                .anyMatch(executeObjectTask -> !executeObjectTask.getExecuteObject().isExecutable());
    }

    private String buildGseTaskInfo(Long jobInstanceId, GseTaskDTO gseTask) {
        return "Job:" + jobInstanceId + "-" + gseTask.getTaskUniqueName();
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
            // 停止日志拉取调度
            this.executeResult = GseTaskExecuteResult.DISCARDED;
            finishGseTask(this.executeResult);

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
        log.info("[{}]: Start pull gse task result, times: {}", gseTaskInfo,
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
                log.error("[" + gseTaskInfo + "]: analyse gse task result error.", e);
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
                log.info("[{}]: Skip task, set unfinished ip task status to unknown!", gseTaskInfo);
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
                finishGseTask(this.executeResult);
                watch.stop();
            }
        } catch (Throwable e) {
            log.error("[" + gseTaskInfo + "]: result handle error.", e);
            this.executeResult = GseTaskExecuteResult.EXCEPTION;
            finishGseTask(this.executeResult);
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
            log.info("Task is inactive, task: {}", gseTaskInfo);
            return false;
        }
        this.isRunning = true;
        // 二次确认，防止isActive在设置this.isRunning=true期间发生变化
        if (!isActive) {
            log.info("Task is inactive, task: {}", gseTaskInfo);
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
                    "task:{}", taskInstance.getId(), gseTaskInfo);
                taskExecuteMQEventDispatcher.dispatchGseTaskEvent(GseTaskEvent.stopGseTask(
                    taskInstanceId, gseTask.getStepInstanceId(), gseTask.getExecuteCount(),
                    gseTask.getBatch(), gseTask.getId()));
                this.isGseTaskTerminating = true;
                log.info("Send stop gse step control action successfully!");
            }
        }
    }

    private boolean shouldSkipStep() {
        StepInstanceBaseDTO stepInstance = stepInstanceService.getBaseStepInstance(taskInstanceId, stepInstanceId);
        return stepInstance == null || RunStatusEnum.SKIPPED == stepInstance.getStatus();
    }

    private void saveStatusWhenSkip() {
        List<ExecuteObjectTask> notFinishedExecuteObjectTasks =
            targetExecuteObjectTasks.values().stream()
                .filter(not(ExecuteObjectTask::isFinished))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(notFinishedExecuteObjectTasks)) {
            notFinishedExecuteObjectTasks.forEach(executeObjectTask -> {
                executeObjectTask.setStatus(ExecuteObjectTaskStatusEnum.UNKNOWN);
                executeObjectTask.setEndTime(System.currentTimeMillis());
            });
        }
        executeObjectTaskService.batchUpdateTasks(notFinishedExecuteObjectTasks);
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
            log.warn("[{}]: Task execution timeout! runDuration: {}ms, timeout: {}s", gseTaskInfo,
                runDuration, stepInstance.getTimeout());
            this.executeResult = GseTaskExecuteResult.FAILED;
            saveFailInfoForUnfinishedExecuteObjectTask(ExecuteObjectTaskStatusEnum.LOG_ERROR,
                "Task execution may be abnormal or timeout.");
            finishGseTask(GseTaskExecuteResult.FAILED);
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
        if (null == gseTaskResult || gseTaskResult.isEmptyResult()) {
            long currentTimeMillis = System.currentTimeMillis();
            // 执行结果持续为空,并且超出 Job 最大容忍时间，需要终止调度任务
            if (currentTimeMillis - latestPullGseLogSuccessTimeMillis >= GSE_TASK_EMPTY_RESULT_MAX_TOLERATION_MILLS) {
                log.warn("[{}]: Execution result log always empty!", gseTaskInfo);
                this.executeResult = GseTaskExecuteResult.FAILED;
                saveFailInfoForUnfinishedExecuteObjectTask(ExecuteObjectTaskStatusEnum.LOG_ERROR, "Execution result " +
                    "log " +
                    "always empty.");
                finishGseTask(GseTaskExecuteResult.FAILED);
                isAbnormal = true;
            }
        } else {
            latestPullGseLogSuccessTimeMillis = System.currentTimeMillis();
        }
        return isAbnormal;
    }

    /**
     * 设置目标gent任务结束状态
     *
     * @param executeObjectGseKey executeObjectGseKey
     * @param startTime           起始时间
     * @param endTime             终止时间
     * @param executeObjectTask   执行对象任务
     */
    protected void dealTargetExecuteObjectFinish(ExecuteObjectGseKey executeObjectGseKey,
                                                 Long startTime,
                                                 Long endTime,
                                                 ExecuteObjectTask executeObjectTask) {
        log.info("[{}]: Deal target agent finished| executeObjectGseKey={}| startTime:{}, endTime:{}, " +
                "executeObjectTask:{}",
            gseTaskInfo, executeObjectGseKey, startTime, endTime,
            JsonUtils.toJsonWithoutSkippedFields(executeObjectTask));

        notFinishedTargetExecuteObjectGseKeys.remove(executeObjectGseKey);
        analyseFinishedTargetExecuteObjectGseKeys.add(executeObjectGseKey);

        if (endTime - startTime <= 0) {
            executeObjectTask.setTotalTime(100L);
        } else {
            executeObjectTask.setTotalTime(endTime - startTime);
        }
        executeObjectTask.setStartTime(startTime);
        executeObjectTask.setEndTime(endTime);
    }

    /**
     * 设置GSE TASK 完成状态并分发事件
     *
     * @param result 任务执行结果
     */
    private void finishGseTask(GseTaskExecuteResult result) {
        int gseTaskExecuteResult = result.getResultCode();

        // 处理GSE任务执行结果
        log.info("Finish gse task, task:{}, executeResult:{}", gseTaskInfo, gseTaskExecuteResult);

        long startTime = this.gseTask.getStartTime();
        long endTime = DateUtils.currentTimeMillis();
        long gseTotalTime = endTime - startTime;

        updateGseTaskExecutionInfo(result, endTime, gseTotalTime);

        taskExecuteMQEventDispatcher.dispatchStepEvent(
            StepEvent.refreshStep(
                taskInstanceId,
                stepInstanceId,
                EventSource.buildGseTaskEventSource(
                    taskInstanceId,
                    stepInstanceId,
                    stepInstance.getExecuteCount(),
                    stepInstance.getBatch(),
                    gseTask.getId())
            )
        );
    }

    private void updateGseTaskExecutionInfo(GseTaskExecuteResult result, long endTime, long totalTime) {

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
     * 批量更新执行对象任务并重置changed标志
     *
     * @param executeObjectTasks 执行对象任务列表
     */
    protected void batchSaveChangedExecuteObjectTasks(Collection<ExecuteObjectTask> executeObjectTasks) {
        if (CollectionUtils.isNotEmpty(executeObjectTasks)) {
            List<ExecuteObjectTask> changedTasks =
                executeObjectTasks.stream().filter(ExecuteObjectTask::isChanged).collect(Collectors.toList());
            executeObjectTaskService.batchUpdateTasks(changedTasks);
            changedTasks.forEach(executeObjectTask -> executeObjectTask.setChanged(false));
        }
    }

    protected void saveFailInfoForUnfinishedExecuteObjectTask(ExecuteObjectTaskStatusEnum status, String errorMsg) {
        log.info("[{}]: Deal unfinished agent result| notFinishedTargetAgentIds : {}",
            gseTaskInfo, notFinishedTargetExecuteObjectGseKeys);
        long startTime = (gseTask != null && gseTask.getStartTime() != null) ?
            gseTask.getStartTime() : System.currentTimeMillis();
        for (ExecuteObjectGseKey executeObjectGseKey : notFinishedTargetExecuteObjectGseKeys) {
            ExecuteObjectTask executeObjectTask = targetExecuteObjectTasks.get(executeObjectGseKey);
            executeObjectTask.setStartTime(startTime);
            executeObjectTask.setEndTime(System.currentTimeMillis());
            executeObjectTask.setStatus(status);
        }
        batchSaveChangedExecuteObjectTasks(targetExecuteObjectTasks.values());
    }

    @Override
    public void stop() {
        synchronized (stopMonitor) {
            if (!isStopped) {
                this.isActive = false;
                tryStopImmediately();
            } else {
                log.info("Task is stopped, task: {}", gseTaskInfo);
            }
        }
    }

    private void tryStopImmediately() {
        if (!this.isRunning) {
            log.info("ResultHandleTask-onStop start, task: {}", gseTaskInfo);
            resultHandleTaskKeepaliveManager.stopKeepaliveInfoTask(getTaskId());
            runningJobKeepaliveManager.stopKeepaliveTask(taskInstanceId);

            taskExecuteMQEventDispatcher.dispatchResultHandleTaskResumeEvent(
                ResultHandleTaskResumeEvent.resume(stepInstance.getTaskInstanceId(), gseTask.getStepInstanceId(),
                    gseTask.getExecuteCount(), gseTask.getBatch(), gseTask.getId(), requestId));

            this.isStopped = true;
            StopTaskCounter.getInstance().decrement(getTaskId());
            log.info("ResultHandleTask-onStop end, task: {}", gseTaskInfo);
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
     * 是否所有目标上的执行对象任务都完成
     */
    protected boolean isAllTargetExecuteObjectTasksDone() {
        return this.analyseFinishedTargetExecuteObjectGseKeys.size() == this.targetExecuteObjectGseKeys.size();
    }

    /**
     * 是否所有目标上的执行对象任务都执行成功
     */
    protected boolean isAllTargetExecuteObjectTasksSuccess() {
        return this.targetExecuteObjectGseKeys.size() == this.successTargetExecuteObjectGseKeys.size();
    }

    /**
     * 所有执行对象任务结束的时候，分析整体GSE任务状态
     */
    protected GseTaskExecuteResult analyseFinishedExecuteResult() {
        GseTaskExecuteResult rst;
        if (isAllTargetExecuteObjectTasksSuccess()) {
            // 如果源/目标包含非法主机，设置任务状态为失败
            if (existNoExecutableExecuteObject()) {
                log.info("Gse task contains invalid execute object, set execute result fail");
                rst = GseTaskExecuteResult.FAILED;
            } else {
                rst = GseTaskExecuteResult.SUCCESS;
            }
        } else {
            if (this.isTerminatedSuccess) {
                rst = GseTaskExecuteResult.STOP_SUCCESS;
            } else {
                rst = GseTaskExecuteResult.FAILED;
            }
        }
        return rst;
    }

    /**
     * 任务是否包含不可执行的执行对象
     */
    protected boolean existNoExecutableExecuteObject() {
        return this.existNoExecutableTargetExecuteObject;
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

    @Override
    public TaskContext getTaskContext() {
        return this.taskContext;
    }
}
