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

package com.tencent.bk.job.execute.engine;

import brave.Tracing;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.exception.MessageHandlerUnavailableException;
import com.tencent.bk.job.execute.common.ha.DestroyOrder;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.config.StorageSystemConfig;
import com.tencent.bk.job.execute.engine.consts.IpStatus;
import com.tencent.bk.job.execute.engine.exception.ExceptionStatusManager;
import com.tencent.bk.job.execute.engine.executor.AbstractGseTaskExecutor;
import com.tencent.bk.job.execute.engine.executor.FileTaskExecutor;
import com.tencent.bk.job.execute.engine.executor.SQLScriptTaskExecutor;
import com.tencent.bk.job.execute.engine.executor.ScriptTaskExecutor;
import com.tencent.bk.job.execute.engine.model.GseTaskExecuteResult;
import com.tencent.bk.job.execute.engine.result.ResultHandleManager;
import com.tencent.bk.job.execute.engine.result.ha.ResultHandleTaskKeepaliveManager;
import com.tencent.bk.job.execute.engine.util.RunningTaskCounter;
import com.tencent.bk.job.execute.engine.variable.JobBuildInVariableResolver;
import com.tencent.bk.job.execute.model.GseTaskIpLogDTO;
import com.tencent.bk.job.execute.model.GseTaskLogDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.monitor.ExecuteMetricNames;
import com.tencent.bk.job.execute.monitor.metrics.ExecuteMonitor;
import com.tencent.bk.job.execute.monitor.metrics.GseTasksExceptionCounter;
import com.tencent.bk.job.execute.service.AccountService;
import com.tencent.bk.job.execute.service.AgentService;
import com.tencent.bk.job.execute.service.GseTaskLogService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.StepInstanceVariableValueService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceVariableService;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * GSE任务执行管理
 */
@Component
@Slf4j
public class GseTaskManager implements SmartLifecycle {
    private final ResultHandleManager resultHandleManager;
    private final TaskInstanceService taskInstanceService;
    private final GseTaskLogService gseTaskLogService;
    private final TaskExecuteControlMsgSender taskManager;
    private final AccountService accountService;
    private final LogService logService;
    private final TaskInstanceVariableService taskInstanceVariableService;
    private final StepInstanceVariableValueService stepInstanceVariableValueService;
    private final AgentService agentService;
    private final ResultHandleTaskKeepaliveManager resultHandleTaskKeepaliveManager;
    private final JobBuildInVariableResolver jobBuildInVariableResolver;
    private final ExceptionStatusManager exceptionStatusManager;
    private final Tracing tracing;
    private final ExecuteMonitor executeMonitor;
    private final StorageSystemConfig storageSystemConfig;
    private final JobExecuteConfig jobExecuteConfig;
    private final Object lifecycleMonitor = new Object();
    private final RunningTaskCounter<String> counter = new RunningTaskCounter<>("GseTask-Counter");
    /**
     * 正在执行中的任务
     */
    private Map<String, AbstractGseTaskExecutor> executorMap = new ConcurrentHashMap<>();
    /**
     * 本地服务器IP
     */
    private String serverIp = null;
    private volatile boolean running = false;
    private volatile boolean active = false;

    private GseTasksExceptionCounter gseTasksExceptionCounter;
    /**
     * 正在处理的gse任务数
     */
    private AtomicInteger runningTasks = new AtomicInteger(0);
    /**
     * 正在处理的gse文件任务数
     */
    private AtomicInteger runningFileTasks = new AtomicInteger(0);
    /**
     * 正在处理的gse脚本任务数
     */
    private AtomicInteger runningScriptTasks = new AtomicInteger(0);
    /**
     * 正在处理的gse文件任务数
     */
    private AtomicInteger fileTaskCounter = new AtomicInteger(0);
    /**
     * 正在处理的gse脚本任务数
     */
    private AtomicInteger scriptTaskCounter = new AtomicInteger(0);

    /**
     * GseTaskManager Constructor
     */
    @Autowired
    public GseTaskManager(ResultHandleManager resultHandleManager,
                          TaskInstanceService taskInstanceService,
                          GseTaskLogService gseTaskLogService,
                          TaskExecuteControlMsgSender taskManager,
                          AccountService accountService,
                          LogService logService,
                          TaskInstanceVariableService taskInstanceVariableService,
                          StepInstanceVariableValueService stepInstanceVariableValueService,
                          JobBuildInVariableResolver jobBuildInVariableResolver,
                          StorageSystemConfig storageSystemConfig,
                          AgentService agentService,
                          ResultHandleTaskKeepaliveManager resultHandleTaskKeepaliveManager,
                          ExceptionStatusManager exceptionStatusManager,
                          GseTasksExceptionCounter gseTasksExceptionCounter,
                          Tracing tracing,
                          ExecuteMonitor executeMonitor,
                          JobExecuteConfig jobExecuteConfig) {
        this.resultHandleManager = resultHandleManager;
        this.taskInstanceService = taskInstanceService;
        this.gseTaskLogService = gseTaskLogService;
        this.taskManager = taskManager;
        this.accountService = accountService;
        this.logService = logService;
        this.taskInstanceVariableService = taskInstanceVariableService;
        this.stepInstanceVariableValueService = stepInstanceVariableValueService;
        this.jobBuildInVariableResolver = jobBuildInVariableResolver;
        this.storageSystemConfig = storageSystemConfig;
        this.agentService = agentService;
        this.resultHandleTaskKeepaliveManager = resultHandleTaskKeepaliveManager;
        this.exceptionStatusManager = exceptionStatusManager;
        this.gseTasksExceptionCounter = gseTasksExceptionCounter;
        this.tracing = tracing;
        this.executeMonitor = executeMonitor;
        this.jobExecuteConfig = jobExecuteConfig;
    }

    /**
     * 启动任务(首次执行/继续执行异常中断的任务)
     *
     * @param stepInstanceId 步骤实例ID
     */
    public void startStep(long stepInstanceId, String requestId) {
        if (!isActive()) {
            log.warn("GseTaskManager is not active, reject! stepInstanceId: {}", stepInstanceId);
            throw new MessageHandlerUnavailableException();
        }

        boolean success = false;
        String taskName = "start-gse-task:" + stepInstanceId;
        StopWatch watch = new StopWatch("GseTaskManager-start-step-" + stepInstanceId);
        String startTaskRequestId = requestId;
        if (StringUtils.isEmpty(startTaskRequestId)) {
            startTaskRequestId = UUID.randomUUID().toString();
        }

        AbstractGseTaskExecutor gseTaskExecutor = null;
        try {
            watch.start("get-running-lock");
            // 可重入锁，如果任务正在执行，则放弃
            if (!LockUtils.tryGetReentrantLock(
                "job:running:gse:task:" + stepInstanceId,
                startTaskRequestId,
                30000L
            )) {
                log.info("Fail to get running lock, stepInstanceId: {}", stepInstanceId);
                return;
            }
            watch.stop();

            watch.start("get-task-and-step-from-db");
            StepInstanceDTO stepInstance = taskInstanceService.getStepInstanceDetail(stepInstanceId);
            TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(stepInstance.getTaskInstanceId());

            // 如果任务处于“终止中”状态，直接终止
            if (taskInstance.getStatus().equals(RunStatusEnum.STOPPING.getValue())) {
                log.info("Task instance status is stopping, stop executing the step! taskInstanceId:{}, " +
                        "stepInstanceId:{}",
                    taskInstance.getId(), stepInstance.getId());
                taskManager.refreshTask(stepInstance.getTaskInstanceId());
                return;
            }
            watch.stop();

            watch.start("init-task-executor");
            int executeCount = stepInstance.getExecuteCount();
            Set<String> executeIps = new HashSet<>();
            stepInstance.getTargetServers().getIpList().forEach(ipDTO -> {
                executeIps.add(ipDTO.getCloudAreaId() + ":" + ipDTO.getIp());
            });
            watch.stop();

            watch.start("init-gse-task-executor");
            gseTaskExecutor = initGseTaskExecutor(startTaskRequestId, stepInstance, taskInstance, executeIps);
            if (gseTaskExecutor == null) {
                return;
            }
            watch.stop();

            counter.add(taskName);
            watch.start("execute-task");
            executeTask(gseTaskExecutor, stepInstanceId, executeCount);
            watch.stop();
            success = true;
        } finally {
            if (!watch.isRunning()) {
                watch.start("release-running-lock");
            }
            LockUtils.releaseDistributedLock("job:running:gse:task:", String.valueOf(stepInstanceId),
                startTaskRequestId);
            counter.release(taskName);
            watch.stop();
            if (watch.getTotalTimeMillis() > 2000L) {
                log.warn("GseTaskManager-> start gse step is slow, run statistics:{}", watch.prettyPrint());
            }
            executeMonitor.getMeterRegistry().timer(ExecuteMetricNames.EXECUTE_TASK_PREFIX,
                "task_type", getTaskTypeDesc(gseTaskExecutor), "status", success ? "ok" : "error")
                .record(watch.getTotalTimeNanos(), TimeUnit.NANOSECONDS);
        }

    }

    private String getTaskTypeDesc(AbstractGseTaskExecutor executor) {
        if (executor == null) {
            return "none";
        }
        if (executor instanceof ScriptTaskExecutor) {
            return "script";
        } else if (executor instanceof FileTaskExecutor) {
            return "file";
        } else {
            return "none";
        }
    }

    private void incrementRunningTasksCount(AbstractGseTaskExecutor executor) {
        this.runningTasks.incrementAndGet();
        if (executor instanceof ScriptTaskExecutor) {
            this.runningScriptTasks.incrementAndGet();
        } else {
            this.runningFileTasks.incrementAndGet();
        }
    }

    private void decrementRunningTasksCount(AbstractGseTaskExecutor executor) {
        this.runningTasks.decrementAndGet();
        if (executor instanceof ScriptTaskExecutor) {
            this.runningScriptTasks.decrementAndGet();
        } else {
            this.runningFileTasks.decrementAndGet();
        }
    }

    /**
     * 初始化步骤对应的executor
     *
     * @param stepInstance 步骤
     * @param taskInstance 作业
     * @param executeIps   目标ip
     * @return executor
     */
    private AbstractGseTaskExecutor initGseTaskExecutor(String requestId, StepInstanceDTO stepInstance,
                                                        TaskInstanceDTO taskInstance,
                                                        Set<String> executeIps) {
        AbstractGseTaskExecutor gseTaskExecutor = null;
        int executeType = stepInstance.getExecuteType();
        if (executeType == StepExecuteTypeEnum.EXECUTE_SCRIPT.getValue()) {
            scriptTaskCounter.incrementAndGet();
            gseTaskExecutor = new ScriptTaskExecutor(requestId, gseTasksExceptionCounter, taskInstance, stepInstance,
                executeIps, jobBuildInVariableResolver);
        } else if (executeType == StepExecuteTypeEnum.EXECUTE_SQL.getValue()) {
            gseTaskExecutor = new SQLScriptTaskExecutor(requestId, gseTasksExceptionCounter, taskInstance,
                stepInstance, executeIps);
            scriptTaskCounter.incrementAndGet();
        } else if (executeType == TaskStepTypeEnum.FILE.getValue()) {
            gseTaskExecutor = new FileTaskExecutor(requestId, gseTasksExceptionCounter,
                storageSystemConfig.getJobStorageRootPath(),
                agentService.getLocalAgentBindIp(), taskInstance, stepInstance, executeIps);
            fileTaskCounter.incrementAndGet();
        }

        if (gseTaskExecutor == null) {
            log.warn("No match GseTaskExecutor, stepInstanceId:{}", stepInstance.getId());
            return null;
        }

        gseTaskExecutor.initDependentService(resultHandleManager, taskInstanceService, gseTaskLogService,
            accountService, taskInstanceVariableService, stepInstanceVariableValueService, agentService, logService,
            taskManager, resultHandleTaskKeepaliveManager, executeMonitor, jobExecuteConfig);
        gseTaskExecutor.setExceptionStatusManager(exceptionStatusManager);
        gseTaskExecutor.setTracing(tracing);
        return gseTaskExecutor;
    }

    /**
     * 终止步骤
     *
     * @param stepInstanceId 步骤实例ID
     */
    public void stopStep(long stepInstanceId, String requestId) {
        log.info("Stop gse task, stepInstanceId:" + stepInstanceId);

        StepInstanceDTO stepInstance = taskInstanceService.getStepInstanceDetail(stepInstanceId);
        if (stepInstance == null || !stepInstance.getStatus().equals(RunStatusEnum.RUNNING.getValue())) {
            log.info("StepInstance: {} is null or is not running, should not stop!", stepInstanceId);
            return;
        }

        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(stepInstance.getTaskInstanceId());
        int executeCount = stepInstance.getExecuteCount();
        GseTaskLogDTO gseTaskLog = gseTaskLogService.getGseTaskLog(stepInstanceId, executeCount);
        if (null == gseTaskLog) {
            log.info("Get gseTaskLog return null, stepInstanceId: {}, executeCount:{}", stepInstanceId, executeCount);
            return;
        }
        Set<String> stopIps = new HashSet<>();
        stepInstance.getTargetServers().getIpList().forEach(ipDTO -> {
            stopIps.add(ipDTO.getCloudAreaId() + ":" + ipDTO.getIp());
        });

        AbstractGseTaskExecutor gseTaskExecutor = initGseTaskExecutor(createRequestIdIfEmpty(requestId),
            stepInstance, taskInstance, stopIps);
        if (gseTaskExecutor == null) {
            log.warn("TaskExecutor is not found!");
            return;
        }

        GseTaskExecuteResult stopResult = gseTaskExecutor.stopGseTask();
        // 处理GSE任务执行结果
        if (stopResult.getResultCode().equals(GseTaskExecuteResult.RESULT_CODE_STOP_SUCCESS)) {
            taskInstanceService.updateStepStatus(stepInstanceId, RunStatusEnum.STOPPING.getValue());
        }
    }

    private String createRequestIdIfEmpty(String requestId) {
        String reqId = requestId;
        if (StringUtils.isEmpty(requestId)) {
            reqId = UUID.randomUUID().toString();
        }
        return reqId;
    }

    @PostConstruct
    public void init() {
        this.serverIp = agentService.getLocalAgentBindIp();
        log.info("Server ip: {}", serverIp);
    }

    /**
     * 重试执行失败的IP
     *
     * @param stepInstanceId 步骤实例ID
     */
    public void retryFail(long stepInstanceId, String requestId) {
        if (!isActive()) {
            log.warn("GseTaskManager is not active, reject!");
            throw new MessageHandlerUnavailableException();
        }

        String taskName = "retry-fail:" + stepInstanceId;
        try {
            StepInstanceDTO stepInstance = taskInstanceService.getStepInstanceDetail(stepInstanceId);
            if (stepInstance == null) {
                log.warn("StepInstance is not exist, stop retry! stepInstanceId:{}", stepInstanceId);
                return;
            }
            TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(stepInstance.getTaskInstanceId());
            if (taskInstance == null) {
                log.warn("TaskInstance is not exist, stop retry! taskInstanceId:{}", stepInstance.getTaskInstanceId());
                return;
            }

            int executeCount = stepInstance.getExecuteCount();
            List<GseTaskIpLogDTO> successGseTaskIpLogs = gseTaskLogService.getSuccessGseTaskIp(stepInstanceId,
                executeCount - 1);
            Set<String> lastSuccessIpSet = new HashSet<>();
            if (successGseTaskIpLogs != null) {
                lastSuccessIpSet.addAll(successGseTaskIpLogs.parallelStream().map(GseTaskIpLogDTO::getCloudAreaAndIp)
                    .collect(Collectors.toSet()));
            }
            Set<String> executeIps = new HashSet<>();
            stepInstance.getTargetServers().getIpList().forEach(ipDTO -> {
                String fullIp = ipDTO.getCloudAreaId() + ":" + ipDTO.getIp();
                if (!lastSuccessIpSet.contains(fullIp)) {
                    executeIps.add(fullIp);
                }
            });
            log.info("Get execute ips, stepInstanceId:{}, executeIps:{}", stepInstanceId, executeIps);
            AbstractGseTaskExecutor gseTaskExecutor = initGseTaskExecutor(createRequestIdIfEmpty(requestId),
                stepInstance, taskInstance, executeIps);
            if (gseTaskExecutor == null) {
                log.warn("No match GseTaskExecutor for task, stepInstanceId:{}", stepInstanceId);
                return;
            }

            // GseTaskLog初始状态
            dealGseTaskLog(stepInstance);
            // 已成功执行过的IP无需执行，仅保存记录
            if (successGseTaskIpLogs != null && !successGseTaskIpLogs.isEmpty()) {
                dealLastSuccessIp(executeCount, successGseTaskIpLogs);
            }

            counter.add(taskName);
            executeTask(gseTaskExecutor, stepInstanceId, executeCount);
        } finally {
            counter.release(taskName);
        }

    }

    private void dealGseTaskLog(StepInstanceDTO stepInstance) {
        // 初始化GseTaskLog
        GseTaskLogDTO gseTaskLog = new GseTaskLogDTO();
        gseTaskLog.setStepInstanceId(stepInstance.getId());
        gseTaskLog.setExecuteCount(stepInstance.getExecuteCount());
        gseTaskLog.setStatus(RunStatusEnum.RUNNING.getValue());
        gseTaskLog.setStartTime(DateUtils.currentTimeMillis());
        gseTaskLogService.saveGseTaskLog(gseTaskLog);
    }

    /**
     * 处理已执行成功的IP
     *
     * @param executeCount             执行次数
     * @param lastSuccessGseTaskIpLogs 已执行成功IP
     */
    private void dealLastSuccessIp(int executeCount, List<GseTaskIpLogDTO> lastSuccessGseTaskIpLogs) {
        List<GseTaskIpLogDTO> ipLogList = new ArrayList<>();
        for (GseTaskIpLogDTO gseTaskIpLog : lastSuccessGseTaskIpLogs) {
            GseTaskIpLogDTO ipLog = setGseTaskIpLogForRetry(gseTaskIpLog, executeCount, IpStatus.LAST_SUCCESS);
            ipLogList.add(ipLog);
        }
        if (ipLogList.size() > 0) {
            gseTaskLogService.batchSaveIpLog(ipLogList);
        }
    }

    private GseTaskIpLogDTO setGseTaskIpLogForRetry(GseTaskIpLogDTO gseTaskIpLog, int executeCount, IpStatus ipStatus) {
        gseTaskIpLog.setExecuteCount(executeCount);
        gseTaskIpLog.setStatus(ipStatus.getValue());
        return gseTaskIpLog;
    }

    /**
     * 重试执行所有的IP,包括已执行成功的
     *
     * @param stepInstanceId 步骤实例ID
     */
    public void retryAll(long stepInstanceId, String requestId) {
        if (!isActive()) {
            log.warn("GseTaskManager is not active, reject!");
            throw new MessageHandlerUnavailableException();
        }
        String taskName = "retry-all:" + stepInstanceId;
        try {
            StepInstanceDTO stepInstance = taskInstanceService.getStepInstanceDetail(stepInstanceId);
            if (stepInstance == null) {
                log.warn("StepInstance is not exist, stop retry! stepInstanceId:{}", stepInstanceId);
                return;
            }
            TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(stepInstance.getTaskInstanceId());
            if (taskInstance == null) {
                log.warn("TaskInstance is not exist, stop retry! taskInstanceId:{}", stepInstance.getTaskInstanceId());
                return;
            }

            int executeCount = stepInstance.getExecuteCount();
            Set<String> executeIps = new HashSet<>();
            stepInstance.getTargetServers().getIpList().forEach(ipDTO -> {
                String fullIp = ipDTO.getCloudAreaId() + ":" + ipDTO.getIp();
                executeIps.add(fullIp);
            });
            log.info("Get execute ips, stepInstanceId:{}, executeIps:{}", stepInstanceId, executeIps);
            AbstractGseTaskExecutor gseTaskExecutor = initGseTaskExecutor(createRequestIdIfEmpty(requestId),
                stepInstance, taskInstance, executeIps);
            if (gseTaskExecutor == null) {
                log.warn("No match GseTaskExecutor for task, stepInstanceId:{}", stepInstanceId);
                return;
            }

            counter.add(taskName);
            executeTask(gseTaskExecutor, stepInstanceId, executeCount);
        } finally {
            counter.release(taskName);
        }

    }

    private void executeTask(AbstractGseTaskExecutor gseTaskExecutor, long stepInstanceId, int executeCount) {
        String taskKey = stepInstanceId + "_" + executeCount;
        try {
            executorMap.put(taskKey, gseTaskExecutor);
            incrementRunningTasksCount(gseTaskExecutor);
            gseTaskExecutor.execute();
        } finally {
            executorMap.remove(taskKey);
            decrementRunningTasksCount(gseTaskExecutor);
        }
    }


    private boolean isActive() {
        synchronized (this.lifecycleMonitor) {
            return this.active;
        }
    }

    @Override
    public void start() {
        if (isRunning()) {
            return;
        }
        log.info("GseTaskManager starting.");
        counter.start();
        synchronized (lifecycleMonitor) {
            this.running = true;
            this.active = true;
        }
    }

    @Override
    public void stop() {
        log.info("GseTaskManager stopping.");
        synchronized (this.lifecycleMonitor) {
            this.active = false;
        }
        try {
            counter.stop();
            counter.waitUntilTaskDone(5, TimeUnit.SECONDS);
        } finally {
            synchronized (this.lifecycleMonitor) {
                this.running = false;
            }
        }
        log.info("Save unfinished task snapshot, tasks: {}", executorMap.keySet());
        executorMap.values().parallelStream().forEach(AbstractGseTaskExecutor::interrupt);
        log.info("Save unfinished task snapshot successfully.");
    }

    @Override
    public boolean isRunning() {
        synchronized (this.lifecycleMonitor) {
            return (this.running);
        }
    }

    @Override
    public int getPhase() {
        return DestroyOrder.GSE_TASK_HANDLER;
    }

    /**
     * 返回正在执行的任务数量
     *
     * @return 任务数量
     */
    public int getRunningTaskCount() {
        return this.runningTasks.get();
    }

    /**
     * 返回正在执行的文件任务数量
     *
     * @return 任务数量
     */
    public int getRunningFileTaskCount() {
        return this.runningFileTasks.get();
    }

    /**
     * 返回正在执行的脚本任务数量
     *
     * @return 任务数量
     */
    public int getRunningScriptTaskCount() {
        return this.runningScriptTasks.get();
    }

    /**
     * 返回累计处理的文件任务数量
     *
     * @return 任务数量
     */
    public int getFileTaskCount() {
        return this.fileTaskCounter.get();
    }

    /**
     * 返回累计处理的脚本任务数量
     *
     * @return 任务数量
     */
    public int getScriptTaskCount() {
        return this.scriptTaskCounter.get();
    }
}
