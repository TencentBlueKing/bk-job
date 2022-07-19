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
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.gse.GseClient;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.exception.MessageHandlerUnavailableException;
import com.tencent.bk.job.execute.common.ha.DestroyOrder;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.config.StorageSystemConfig;
import com.tencent.bk.job.execute.engine.evict.TaskEvictPolicyExecutor;
import com.tencent.bk.job.execute.engine.exception.ExceptionStatusManager;
import com.tencent.bk.job.execute.engine.listener.event.EventSource;
import com.tencent.bk.job.execute.engine.listener.event.StepEvent;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.result.ResultHandleManager;
import com.tencent.bk.job.execute.engine.result.ha.ResultHandleTaskKeepaliveManager;
import com.tencent.bk.job.execute.engine.util.RunningTaskCounter;
import com.tencent.bk.job.execute.engine.variable.JobBuildInVariableResolver;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.monitor.ExecuteMetricNames;
import com.tencent.bk.job.execute.monitor.metrics.ExecuteMonitor;
import com.tencent.bk.job.execute.monitor.metrics.GseTasksExceptionCounter;
import com.tencent.bk.job.execute.service.AccountService;
import com.tencent.bk.job.execute.service.AgentService;
import com.tencent.bk.job.execute.service.FileAgentTaskService;
import com.tencent.bk.job.execute.service.GseTaskService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.ScriptAgentTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GSE任务执行管理
 */
@Component
@Slf4j
public class GseTaskManager implements SmartLifecycle {
    private final ResultHandleManager resultHandleManager;
    private final TaskInstanceService taskInstanceService;
    private final GseTaskService gseTaskService;
    private final ScriptAgentTaskService scriptAgentTaskService;
    private final FileAgentTaskService fileAgentTaskService;
    private final TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;
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
    private final TaskEvictPolicyExecutor taskEvictPolicyExecutor;
    private final GseTasksExceptionCounter gseTasksExceptionCounter;
    private final StepInstanceService stepInstanceServcice;
    private final GseClient gseClient;

    private final Object lifecycleMonitor = new Object();
    private final RunningTaskCounter<String> counter = new RunningTaskCounter<>("GseTask-Counter");
    /**
     * 正在执行中的任务
     */
    private final Map<String, AbstractGseTaskStartCommand> startingGseTasks = new ConcurrentHashMap<>();
    private volatile boolean running = false;
    private volatile boolean active = false;

    /**
     * 正在处理的gse任务数
     */
    private final AtomicInteger runningTasks = new AtomicInteger(0);
    /**
     * 正在处理的gse文件任务数
     */
    private final AtomicInteger runningFileTasks = new AtomicInteger(0);
    /**
     * 正在处理的gse脚本任务数
     */
    private final AtomicInteger runningScriptTasks = new AtomicInteger(0);
    /**
     * 正在处理的gse文件任务数
     */
    private final AtomicInteger fileTaskCounter = new AtomicInteger(0);
    /**
     * 正在处理的gse脚本任务数
     */
    private final AtomicInteger scriptTaskCounter = new AtomicInteger(0);

    /**
     * GseTaskManager Constructor
     */
    @Autowired
    public GseTaskManager(ResultHandleManager resultHandleManager,
                          TaskInstanceService taskInstanceService,
                          GseTaskService gseTaskService,
                          TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
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
                          JobExecuteConfig jobExecuteConfig,
                          TaskEvictPolicyExecutor taskEvictPolicyExecutor,
                          ScriptAgentTaskService scriptAgentTaskService,
                          FileAgentTaskService fileAgentTaskService,
                          StepInstanceService stepInstanceServcice,
                          GseClient gseClient) {
        this.resultHandleManager = resultHandleManager;
        this.taskInstanceService = taskInstanceService;
        this.gseTaskService = gseTaskService;
        this.scriptAgentTaskService = scriptAgentTaskService;
        this.fileAgentTaskService = fileAgentTaskService;
        this.taskExecuteMQEventDispatcher = taskExecuteMQEventDispatcher;
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
        this.taskEvictPolicyExecutor = taskEvictPolicyExecutor;
        this.stepInstanceServcice = stepInstanceServcice;
        this.gseClient = gseClient;
    }

    /**
     * 启动任务(首次执行/继续执行异常中断的任务)
     *
     * @param gseTask   GSE任务
     * @param requestId 请求ID
     */
    public void startTask(GseTaskDTO gseTask, String requestId) {
        long stepInstanceId = gseTask.getStepInstanceId();
        checkActiveStatus(stepInstanceId);

        boolean success = false;
        String taskName = gseTask.getTaskUniqueName();

        StopWatch watch = new StopWatch("startGseTask");
        String startTaskRequestId = requestId;
        if (StringUtils.isEmpty(startTaskRequestId)) {
            startTaskRequestId = UUID.randomUUID().toString();
        }

        AbstractGseTaskStartCommand startCommand = null;
        try {
            watch.start("getRunningLock");
            // 可重入锁，如果任务正在执行，则放弃
            if (!LockUtils.tryGetReentrantLock(
                "job:running:gse:task:" + gseTask.getId(), startTaskRequestId, 30000L)) {
                log.info("Fail to get running lock, gseTaskId: {}", gseTask.getId());
                return;
            }
            watch.stop();

            watch.start("loadTaskAndCheck");
            StepInstanceDTO stepInstance = taskInstanceService.getStepInstanceDetail(stepInstanceId);
            TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(stepInstance.getTaskInstanceId());

            // 如果任务应当被驱逐，直接置为被丢弃状态
            if (taskEvictPolicyExecutor.shouldEvictTask(taskInstance)) {
                log.warn("Evict job, taskInstanceId: {}, gseTask: {}", taskInstance.getId(), taskName);
                taskEvictPolicyExecutor.updateEvictedTaskStatus(taskInstance, stepInstance);
                watch.stop();
                return;
            }

            // 如果任务处于“终止中”状态，直接终止,不需要下发任务给GSE
            if (taskInstance.getStatus().equals(RunStatusEnum.STOPPING.getValue())) {
                log.info("Task instance status is stopping, stop executing the step! taskInstanceId:{}, "
                    + "stepInstanceId:{}", taskInstance.getId(), stepInstance.getId());
                gseTask.setStatus(RunStatusEnum.STOP_SUCCESS.getValue());
                gseTaskService.saveGseTask(gseTask);
                taskExecuteMQEventDispatcher.dispatchStepEvent(StepEvent.refreshStep(stepInstanceId,
                    EventSource.buildGseTaskEventSource(stepInstanceId, stepInstance.getExecuteCount(),
                        stepInstance.getBatch(), gseTask.getId())));
                watch.stop();
                return;
            }
            watch.stop();

            watch.start("initStarCommand");
            startCommand = initGseTaskStartCommand(startTaskRequestId, stepInstance, taskInstance, gseTask);
            watch.stop();

            watch.start("executeTask");
            counter.add(taskName);
            executeTask(startCommand, gseTask);
            watch.stop();
            success = true;
        } finally {
            if (!watch.isRunning()) {
                watch.start("release-running-lock");
            }
            LockUtils.releaseDistributedLock("job:running:gse:task:", String.valueOf(gseTask.getId()),
                startTaskRequestId);
            counter.release(taskName);
            watch.stop();
            if (watch.getTotalTimeMillis() > 2000L) {
                log.warn("GseTaskManager-> start gse task is slow, statistics:{}", watch.prettyPrint());
            }
            executeMonitor.getMeterRegistry().timer(ExecuteMetricNames.EXECUTE_TASK_PREFIX,
                "task_type", getTaskTypeDesc(startCommand), "status", success ? "ok" : "error")
                .record(watch.getTotalTimeNanos(), TimeUnit.NANOSECONDS);
        }
    }

    /**
     * 判定GSE任务管理器是否活跃
     *
     * @param stepInstanceId 当前请求调度的步骤Id
     */
    private void checkActiveStatus(long stepInstanceId) {
        if (!isActive()) {
            log.warn("GseTaskManager is not active, reject! stepInstanceId: {}", stepInstanceId);
            throw new MessageHandlerUnavailableException();
        }
    }


    private String getTaskTypeDesc(AbstractGseTaskStartCommand command) {
        if (command == null) {
            return "none";
        }
        if (command instanceof ScriptGseTaskStartCommand) {
            return "script";
        } else if (command instanceof FileGseTaskStartCommand) {
            return "file";
        } else {
            return "none";
        }
    }

    private void incrementRunningTasksCount(AbstractGseTaskStartCommand gseTaskStartCommand) {
        this.runningTasks.incrementAndGet();
        if (gseTaskStartCommand instanceof ScriptGseTaskStartCommand) {
            this.runningScriptTasks.incrementAndGet();
        } else if (gseTaskStartCommand instanceof FileGseTaskStartCommand) {
            this.runningFileTasks.incrementAndGet();
        }
    }

    private void decrementRunningTasksCount(AbstractGseTaskStartCommand gseTaskStartCommand) {
        this.runningTasks.decrementAndGet();
        if (gseTaskStartCommand instanceof ScriptGseTaskStartCommand) {
            this.runningScriptTasks.decrementAndGet();
        } else if (gseTaskStartCommand instanceof FileGseTaskStartCommand) {
            this.runningFileTasks.decrementAndGet();
        }
    }

    private AbstractGseTaskStartCommand initGseTaskStartCommand(String requestId,
                                                                StepInstanceDTO stepInstance,
                                                                TaskInstanceDTO taskInstance,
                                                                GseTaskDTO gseTask) {
        AbstractGseTaskStartCommand gseTaskStartCommand = null;
        int executeType = stepInstance.getExecuteType();
        if (executeType == StepExecuteTypeEnum.EXECUTE_SCRIPT.getValue()) {
            gseTaskStartCommand = new ScriptGseTaskStartCommand(
                resultHandleManager,
                taskInstanceService,
                stepInstanceServcice,
                gseTaskService,
                scriptAgentTaskService,
                accountService,
                taskInstanceVariableService,
                stepInstanceVariableValueService,
                agentService,
                logService,
                taskExecuteMQEventDispatcher,
                resultHandleTaskKeepaliveManager,
                executeMonitor,
                jobExecuteConfig,
                taskEvictPolicyExecutor,
                exceptionStatusManager,
                gseTasksExceptionCounter,
                jobBuildInVariableResolver,
                tracing,
                gseClient,
                requestId,
                taskInstance,
                stepInstance,
                gseTask
            );
            scriptTaskCounter.incrementAndGet();
        } else if (executeType == StepExecuteTypeEnum.EXECUTE_SQL.getValue()) {
            gseTaskStartCommand = new SQLScriptGseTaskStartCommand(
                resultHandleManager,
                taskInstanceService,
                stepInstanceServcice,
                gseTaskService,
                scriptAgentTaskService,
                accountService,
                taskInstanceVariableService,
                stepInstanceVariableValueService,
                agentService,
                logService,
                taskExecuteMQEventDispatcher,
                resultHandleTaskKeepaliveManager,
                executeMonitor,
                jobExecuteConfig,
                taskEvictPolicyExecutor,
                exceptionStatusManager,
                gseTasksExceptionCounter,
                jobBuildInVariableResolver,
                tracing,
                gseClient,
                requestId,
                taskInstance,
                stepInstance,
                gseTask
            );
            scriptTaskCounter.incrementAndGet();
        } else if (executeType == TaskStepTypeEnum.FILE.getValue()) {
            gseTaskStartCommand = new FileGseTaskStartCommand(
                resultHandleManager,
                taskInstanceService,
                gseTaskService,
                fileAgentTaskService,
                accountService,
                taskInstanceVariableService,
                stepInstanceVariableValueService,
                agentService,
                logService,
                taskExecuteMQEventDispatcher,
                resultHandleTaskKeepaliveManager,
                executeMonitor,
                jobExecuteConfig,
                taskEvictPolicyExecutor,
                exceptionStatusManager,
                gseTasksExceptionCounter,
                stepInstanceServcice,
                tracing,
                gseClient,
                requestId,
                taskInstance,
                stepInstance,
                gseTask,
                storageSystemConfig.getJobStorageRootPath()
            );
            fileTaskCounter.incrementAndGet();
        }

        if (gseTaskStartCommand == null) {
            log.error("No match GseTaskStartCommand, gseTask: {}", gseTask.getTaskUniqueName());
            throw new InternalException("No match GseTaskStartCommand", ErrorCode.INTERNAL_ERROR);
        }

        return gseTaskStartCommand;
    }

    private void executeTask(AbstractGseTaskStartCommand startCommand,
                             GseTaskDTO gseTask) {
        try {
            startingGseTasks.put(gseTask.getTaskUniqueName(), startCommand);
            incrementRunningTasksCount(startCommand);
            startCommand.execute();
        } finally {
            startingGseTasks.remove(gseTask.getTaskUniqueName());
            decrementRunningTasksCount(startCommand);
        }
    }

    /**
     * 停止任务
     *
     * @param gseTask   GSE任务
     */
    public void stopTask(GseTaskDTO gseTask) {
        long stepInstanceId = gseTask.getStepInstanceId();
        checkActiveStatus(stepInstanceId);

        String taskName = gseTask.getTaskUniqueName();

        StopWatch watch = new StopWatch("stopGseTask");
        GseTaskCommand stopCommand;
        try {
            watch.start("loadTaskAndCheck");
            StepInstanceDTO stepInstance = taskInstanceService.getStepInstanceDetail(stepInstanceId);
            TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(stepInstance.getTaskInstanceId());
            watch.stop();

            watch.start("initStopCommand");
            stopCommand = initGseTaskStopCommand(stepInstance, taskInstance, gseTask);
            watch.stop();

            watch.start("stopTask");
            counter.add(taskName);
            stopCommand.execute();
            watch.stop();
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            counter.release(taskName);
            if (watch.getTotalTimeMillis() > 2000L) {
                log.warn("GseTaskManager-> stop gse task is slow, statistics: {}", watch.prettyPrint());
            }
        }
    }

    private GseTaskCommand initGseTaskStopCommand(StepInstanceDTO stepInstance,
                                                  TaskInstanceDTO taskInstance,
                                                  GseTaskDTO gseTask) {
        GseTaskCommand gseTaskStopCommand = null;
        if (stepInstance.isScriptStep()) {
            gseTaskStopCommand = new ScriptGseTaskStopCommand(
                agentService,
                accountService,
                gseTaskService,
                scriptAgentTaskService,
                tracing,
                gseClient,
                taskInstance,
                stepInstance,
                gseTask
            );
            scriptTaskCounter.incrementAndGet();
        } else if (stepInstance.isFileStep()) {
            gseTaskStopCommand = new FileGseTaskStopCommand(
                agentService,
                accountService,
                gseTaskService,
                fileAgentTaskService,
                tracing,
                gseClient,
                taskInstance,
                stepInstance,
                gseTask
            );
            scriptTaskCounter.incrementAndGet();
        }

        if (gseTaskStopCommand == null) {
            log.error("No match GseTaskStopCommand, gseTask: {}", gseTask.getTaskUniqueName());
            throw new InternalException("No match GseTaskStopCommand", ErrorCode.INTERNAL_ERROR);
        }

        return gseTaskStopCommand;
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
        log.info("GseTaskManager stopping");
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
        log.info("GseTaskManager is stopped");
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
