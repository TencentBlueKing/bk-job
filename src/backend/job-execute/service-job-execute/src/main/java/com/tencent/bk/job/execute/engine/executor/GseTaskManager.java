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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.exception.MessageHandlerUnavailableException;
import com.tencent.bk.job.execute.common.ha.DestroyOrder;
import com.tencent.bk.job.execute.config.FileDistributeConfig;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.engine.EngineDependentServiceHolder;
import com.tencent.bk.job.execute.engine.evict.TaskEvictPolicyExecutor;
import com.tencent.bk.job.execute.engine.listener.event.EventSource;
import com.tencent.bk.job.execute.engine.listener.event.StepEvent;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.util.RunningTaskCounter;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.monitor.ExecuteMetricNames;
import com.tencent.bk.job.execute.monitor.metrics.ExecuteMonitor;
import com.tencent.bk.job.execute.service.FileExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.GseTaskService;
import com.tencent.bk.job.execute.service.ScriptExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GSE任务执行管理
 */
@Component
@Slf4j
public class GseTaskManager implements SmartLifecycle {
    private final EngineDependentServiceHolder engineDependentServiceHolder;
    private final TaskInstanceService taskInstanceService;
    private final GseTaskService gseTaskService;
    private final ScriptExecuteObjectTaskService scriptExecuteObjectTaskService;
    private final FileExecuteObjectTaskService fileExecuteObjectTaskService;
    private final TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;
    private final ExecuteMonitor executeMonitor;
    private final FileDistributeConfig fileDistributeConfig;
    private final JobExecuteConfig jobExecuteConfig;
    private final TaskEvictPolicyExecutor taskEvictPolicyExecutor;
    private final StepInstanceService stepInstanceService;

    private final Object lifecycleMonitor = new Object();
    private final RunningTaskCounter<String> counter = new RunningTaskCounter<>("GseTask-Counter");
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
    public GseTaskManager(EngineDependentServiceHolder engineDependentServiceHolder,
                          TaskInstanceService taskInstanceService,
                          GseTaskService gseTaskService,
                          TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
                          FileDistributeConfig fileDistributeConfig,
                          ExecuteMonitor executeMonitor,
                          JobExecuteConfig jobExecuteConfig,
                          TaskEvictPolicyExecutor taskEvictPolicyExecutor,
                          ScriptExecuteObjectTaskService scriptExecuteObjectTaskService,
                          FileExecuteObjectTaskService fileExecuteObjectTaskService,
                          StepInstanceService stepInstanceService) {
        this.engineDependentServiceHolder = engineDependentServiceHolder;
        this.taskInstanceService = taskInstanceService;
        this.gseTaskService = gseTaskService;
        this.scriptExecuteObjectTaskService = scriptExecuteObjectTaskService;
        this.fileExecuteObjectTaskService = fileExecuteObjectTaskService;
        this.taskExecuteMQEventDispatcher = taskExecuteMQEventDispatcher;
        this.fileDistributeConfig = fileDistributeConfig;
        this.executeMonitor = executeMonitor;
        this.jobExecuteConfig = jobExecuteConfig;
        this.taskEvictPolicyExecutor = taskEvictPolicyExecutor;
        this.stepInstanceService = stepInstanceService;
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
        String lockKey = buildGseTaskLockKey(gseTask);
        try {
            watch.start("getRunningLock");
            // 可重入锁，如果任务正在执行，则放弃
            if (!LockUtils.tryGetReentrantLock(lockKey, startTaskRequestId, 30000L)) {
                log.error("Fail to get running lock, lockKey: {}", lockKey);
                return;
            }
            watch.stop();

            watch.start("loadTaskAndCheck");
            StepInstanceDTO stepInstance = stepInstanceService.getStepInstanceDetail(
                gseTask.getTaskInstanceId(), stepInstanceId);
            TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(stepInstance.getTaskInstanceId());

            // 如果任务应当被驱逐，直接置为被丢弃状态
            if (taskEvictPolicyExecutor.shouldEvictTask(taskInstance)) {
                log.warn("Evict job, taskInstanceId: {}, gseTask: {}", taskInstance.getId(), taskName);
                finishGseTask(gseTask, RunStatusEnum.ABANDONED);
                watch.stop();
                return;
            }

            // 如果任务处于“终止中”状态，直接终止,不需要下发任务给GSE
            if (taskInstance.getStatus() == RunStatusEnum.STOPPING) {
                log.info("Task instance status is stopping, stop executing the step! taskInstanceId:{}, "
                    + "stepInstanceId:{}", taskInstance.getId(), stepInstance.getId());
                finishGseTask(gseTask, RunStatusEnum.STOP_SUCCESS);
                watch.stop();
                return;
            }
            watch.stop();

            watch.start("initStarCommand");
            startCommand = initGseTaskStartCommand(startTaskRequestId, stepInstance, taskInstance, gseTask);
            watch.stop();

            watch.start("executeTask");
            counter.add(taskName);
            executeTask(startCommand);
            watch.stop();
            success = true;
        } finally {
            if (!watch.isRunning()) {
                watch.start("release-running-lock");
            }
            LockUtils.releaseDistributedLock(lockKey, startTaskRequestId);
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

    private void finishGseTask(GseTaskDTO gseTask, RunStatusEnum status) {
        gseTask.setStatus(status.getValue());
        long endTime = System.currentTimeMillis();
        gseTask.setEndTime(endTime);
        if (gseTask.getStartTime() == null) {
            gseTask.setStartTime(endTime);
        }
        gseTask.setTotalTime(endTime - gseTask.getStartTime());
        gseTaskService.updateGseTask(gseTask);
        taskExecuteMQEventDispatcher.dispatchStepEvent(
            StepEvent.refreshStep(
                gseTask.getTaskInstanceId(),
                gseTask.getStepInstanceId(),
                EventSource.buildGseTaskEventSource(
                    gseTask.getTaskInstanceId(),
                    gseTask.getStepInstanceId(),
                    gseTask.getExecuteCount(),
                    gseTask.getBatch(),
                    gseTask.getId()
                )
            )
        );
    }

    private String buildGseTaskLockKey(GseTaskDTO gseTask) {
        return "job:running:gse:task:" + gseTask.getId();
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
        StepExecuteTypeEnum executeType = stepInstance.getExecuteType();
        if (executeType == StepExecuteTypeEnum.EXECUTE_SCRIPT) {
            gseTaskStartCommand = new ScriptGseTaskStartCommand(
                engineDependentServiceHolder,
                scriptExecuteObjectTaskService,
                jobExecuteConfig,
                requestId,
                taskInstance,
                stepInstance,
                gseTask
            );
            scriptTaskCounter.incrementAndGet();
        } else if (executeType == StepExecuteTypeEnum.EXECUTE_SQL) {
            gseTaskStartCommand = new SQLScriptGseTaskStartCommand(
                engineDependentServiceHolder,
                scriptExecuteObjectTaskService,
                jobExecuteConfig,
                requestId,
                taskInstance,
                stepInstance,
                gseTask
            );
            scriptTaskCounter.incrementAndGet();
        } else if (executeType == StepExecuteTypeEnum.SEND_FILE) {
            gseTaskStartCommand = new FileGseTaskStartCommand(
                engineDependentServiceHolder,
                fileExecuteObjectTaskService,
                jobExecuteConfig,
                requestId,
                taskInstance,
                stepInstance,
                gseTask,
                fileDistributeConfig.getJobDistributeRootPath()
            );
            fileTaskCounter.incrementAndGet();
        }

        if (gseTaskStartCommand == null) {
            log.error("No match GseTaskStartCommand, gseTask: {}", gseTask.getTaskUniqueName());
            throw new InternalException("No match GseTaskStartCommand", ErrorCode.INTERNAL_ERROR);
        }

        return gseTaskStartCommand;
    }

    private void executeTask(AbstractGseTaskStartCommand startCommand) {
        try {
            incrementRunningTasksCount(startCommand);
            startCommand.execute();
        } finally {
            decrementRunningTasksCount(startCommand);
        }
    }

    /**
     * 停止任务
     *
     * @param gseTask GSE任务
     */
    public void stopTask(GseTaskDTO gseTask) {
        long stepInstanceId = gseTask.getStepInstanceId();
        checkActiveStatus(stepInstanceId);

        String taskName = gseTask.getTaskUniqueName();

        StopWatch watch = new StopWatch("stopGseTask");
        GseTaskCommand stopCommand;
        try {
            watch.start("loadTaskAndCheck");
            StepInstanceDTO stepInstance = stepInstanceService.getStepInstanceDetail(
                gseTask.getTaskInstanceId(), stepInstanceId);
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
                engineDependentServiceHolder,
                scriptExecuteObjectTaskService,
                taskInstance,
                stepInstance,
                gseTask
            );
            scriptTaskCounter.incrementAndGet();
        } else if (stepInstance.isFileStep()) {
            gseTaskStopCommand = new FileGseTaskStopCommand(
                engineDependentServiceHolder,
                fileExecuteObjectTaskService,
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
