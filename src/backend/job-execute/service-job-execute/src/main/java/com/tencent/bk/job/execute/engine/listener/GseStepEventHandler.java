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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.RollingModeEnum;
import com.tencent.bk.job.common.exception.NotImplementedException;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.util.TaskCostCalculator;
import com.tencent.bk.job.execute.engine.consts.ExecuteObjectTaskStatusEnum;
import com.tencent.bk.job.execute.engine.consts.StepActionEnum;
import com.tencent.bk.job.execute.engine.listener.event.EventSource;
import com.tencent.bk.job.execute.engine.listener.event.GseTaskEvent;
import com.tencent.bk.job.execute.engine.listener.event.JobEvent;
import com.tencent.bk.job.execute.engine.listener.event.ScatterBatchCancelEvent;
import com.tencent.bk.job.execute.engine.listener.event.StepEvent;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.engine.prepare.FilePrepareService;
import com.tencent.bk.job.execute.engine.rolling.scatter.ScatterBatchTimeCalculator;
import com.tencent.bk.job.execute.engine.rolling.scatter.ScatterDispatchManager;
import com.tencent.bk.job.execute.engine.rolling.scatter.ScatterDispatchTask;
import com.tencent.bk.job.execute.engine.rolling.scatter.ScatterStepConverger;
import com.tencent.bk.job.execute.model.db.ExecuteObjectRollingConfigDetailDO;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.RollingConfigDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepInstanceFileBatchDTO;
import com.tencent.bk.job.execute.model.StepInstanceRollingTaskDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.db.RollingExecuteObjectsBatchDO;
import com.tencent.bk.job.execute.service.FileExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.GseTaskService;
import com.tencent.bk.job.execute.service.ScriptExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.StepInstanceRollingTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.rolling.RollingConfigService;
import com.tencent.bk.job.execute.service.rolling.StepInstanceFileBatchService;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GSE 任务(脚本执行/文件分发)步骤事件处理
 */
@Component
@Slf4j
public class GseStepEventHandler extends AbstractStepEventHandler {

    private final FilePrepareService filePrepareService;
    private final GseTaskService gseTaskService;
    private final RollingConfigService rollingConfigService;
    private final StepInstanceRollingTaskService stepInstanceRollingTaskService;
    private final ScriptExecuteObjectTaskService scriptExecuteObjectTaskService;
    private final FileExecuteObjectTaskService fileExecuteObjectTaskService;
    private final StepInstanceFileBatchService stepInstanceFileBatchService;
    private final ScatterDispatchManager scatterDispatchManager;
    private final ScatterStepConverger scatterStepConverger;

    @Autowired
    public GseStepEventHandler(TaskInstanceService taskInstanceService,
                               StepInstanceService stepInstanceService,
                               TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
                               FilePrepareService filePrepareService,
                               GseTaskService gseTaskService,
                               RollingConfigService rollingConfigService,
                               StepInstanceRollingTaskService stepInstanceRollingTaskService,
                               ScriptExecuteObjectTaskService scriptExecuteObjectTaskService,
                               FileExecuteObjectTaskService fileExecuteObjectTaskService,
                               StepInstanceFileBatchService stepInstanceFileBatchService,
                               ScatterDispatchManager scatterDispatchManager,
                               ScatterStepConverger scatterStepConverger) {
        super(taskInstanceService, stepInstanceService, taskExecuteMQEventDispatcher);
        this.filePrepareService = filePrepareService;
        this.gseTaskService = gseTaskService;
        this.rollingConfigService = rollingConfigService;
        this.stepInstanceRollingTaskService = stepInstanceRollingTaskService;
        this.scriptExecuteObjectTaskService = scriptExecuteObjectTaskService;
        this.fileExecuteObjectTaskService = fileExecuteObjectTaskService;
        this.stepInstanceFileBatchService = stepInstanceFileBatchService;
        this.scatterDispatchManager = scatterDispatchManager;
        this.scatterStepConverger = scatterStepConverger;
    }

    @Override
    public void handleEvent(StepEvent stepEvent,
                            StepInstanceDTO stepInstance) {
        long stepInstanceId = stepEvent.getStepInstanceId();
        try {
            StepActionEnum action = StepActionEnum.valueOf(stepEvent.getAction());

            switch (action) {
                case START:
                    startStep(stepEvent, stepInstance);
                    break;
                case STOP:
                    stopStep(stepInstance);
                    break;
                case SKIP:
                    skipStep(stepInstance);
                    break;
                case NEXT_STEP:
                    nextStep(stepInstance);
                    break;
                case RETRY_ALL:
                    retryStepAll(stepInstance);
                    break;
                case RETRY_FAIL:
                    retryStepFail(stepInstance);
                    break;
                case IGNORE_ERROR:
                    ignoreError(stepInstance);
                    break;
                case REFRESH:
                    refreshStep(stepEvent, stepInstance);
                    break;
                case CONTINUE_FILE_PUSH:
                    continueGseFileStep(stepInstance);
                    break;
                case PREPARE_FILE:
                    prepareFile(stepInstance);
                    break;
                case CLEAR:
                    clearStep(stepInstance);
                    break;
                default:
                    log.error("Unhandled step event: {}", stepEvent);
            }
        } catch (Throwable e) {
            String errorMsg = "Handling step event error,stepInstanceId:" + stepInstanceId;
            log.error(errorMsg, e);
            safelyFinishStepWhenCaughtException(stepInstance);
        }
    }

    private void startStep(StepEvent stepEvent, StepInstanceDTO stepInstance) {
        long taskInstanceId = stepInstance.getTaskInstanceId();
        long stepInstanceId = stepInstance.getId();
        boolean isRollingStep = stepInstance.isRollingStep();
        if (isRollingStep) {
            Integer batch = stepEvent.getBatch();
            if (batch == null) {
                log.error("Empty batch for rolling step. Start step fail");
                return;
            }
            stepInstance.setBatch(batch);
            log.info("Start rolling step, stepInstanceId={}, batch: {}", stepInstanceId, batch);
        } else {
            log.info("Start step, stepInstanceId={}", stepInstanceId);
        }

        RunStatusEnum stepStatus = stepInstance.getStatus();
        // 只有当步骤状态为“未执行”、“滚动等待”, "等待用户"时可以启动步骤
        if (RunStatusEnum.BLANK == stepStatus
            || RunStatusEnum.ROLLING_WAITING == stepStatus
            || RunStatusEnum.WAITING_USER == stepStatus) {

            RollingConfigDTO rollingConfig = null;
            if (isRollingStep) {
                rollingConfig = rollingConfigService.getRollingConfig(stepInstance.getTaskInstanceId(),
                    stepInstance.getRollingConfigId());
                log.info("Rolling config: {}", rollingConfig);
                // 更新滚动进度
                stepInstanceService.updateStepCurrentBatch(taskInstanceId, stepInstanceId, stepInstance.getBatch());
                // 初始化步骤滚动任务
                saveInitialStepInstanceRollingTask(stepInstance);
            }

            Long gseTaskId = saveInitialGseTask(stepInstance);
            saveExecuteObjectTasksForStartStep(gseTaskId, stepInstance, rollingConfig);

            // 设置步骤开始时间
            if (stepInstance.getStartTime() == null) {
                stepInstance.setStartTime(DateUtils.currentTimeMillis());
            }
            stepInstanceService.updateStepExecutionInfo(taskInstanceId, stepInstanceId, RunStatusEnum.RUNNING,
                stepInstance.getStartTime(), null, null);
            if (isRollingStep) {
                stepInstanceRollingTaskService.updateRollingTask(
                    taskInstanceId,
                    stepInstanceId,
                    stepInstance.getExecuteCount(),
                    stepInstance.getBatch(),
                    RunStatusEnum.RUNNING,
                    System.currentTimeMillis(),
                    null,
                    null
                );
            }

            startGseTask(stepInstance, gseTaskId);

            // 并行错峰模式：第一批次启动后，预登记并调度后续批次错峰下发
            if (isRollingStep && rollingConfig != null
                && rollingConfig.isParallelExecution() && stepInstance.isFirstRollingBatch()) {
                scheduleParallelScatterBatches(stepInstance, rollingConfig);
            }
        } else {
            log.warn("Unsupported step instance run status for starting step, stepInstanceId={}, status={}",
                stepInstanceId, stepStatus);
        }
    }

    /**
     * 并行错峰模式：登记并调度第一批次之后的所有批次错峰下发。
     * 仅创建/更新滚动任务的下发登记信息与延迟入队，不在此直接执行 GSE 下发。
     *
     * @param stepInstance  步骤实例（batch 为第一批次）
     * @param rollingConfig 滚动配置
     */
    private void scheduleParallelScatterBatches(StepInstanceDTO stepInstance, RollingConfigDTO rollingConfig) {
        long taskInstanceId = stepInstance.getTaskInstanceId();
        long stepInstanceId = stepInstance.getId();
        int executeCount = stepInstance.getExecuteCount();
        ExecuteObjectRollingConfigDetailDO detail = rollingConfig.getExecuteObjectRollingConfig();
        int totalBatch = detail.getTotalBatch();
        long fixedMs = detail.getBatchStartWaitFixedMs() == null ? 0L : detail.getBatchStartWaitFixedMs();
        long randomMinMs = detail.getBatchStartWaitRandomMinMs() == null ? 0L : detail.getBatchStartWaitRandomMinMs();
        long randomMaxMs = detail.getBatchStartWaitRandomMaxMs() == null ? 0L : detail.getBatchStartWaitRandomMaxMs();
        long baseTime = stepInstance.getStartTime() != null ? stepInstance.getStartTime() : System.currentTimeMillis();

        // 累积式错峰：批1下发时刻=baseTime；批k=批(k-1)下发时刻+fixed+random(min,max)，随机每批独立采样
        long[] dispatchTimes = ScatterBatchTimeCalculator.computeDispatchTimes(
            baseTime, totalBatch, fixedMs, randomMinMs, randomMaxMs);

        // 第一批次已即时下发，补登记下发信息（批1下发时刻=baseTime，偏移0）
        stepInstanceRollingTaskService.updateDispatchInfo(taskInstanceId, stepInstanceId, executeCount, 1,
            dispatchTimes[0], Boolean.TRUE);

        for (int batch = 2; batch <= totalBatch; batch++) {
            long dispatchTime = dispatchTimes[batch - 1];
            // 预登记批次滚动任务（未下发）
            StepInstanceRollingTaskDTO rollingTask = new StepInstanceRollingTaskDTO();
            rollingTask.setTaskInstanceId(taskInstanceId);
            rollingTask.setStepInstanceId(stepInstanceId);
            rollingTask.setExecuteCount(executeCount);
            rollingTask.setBatch(batch);
            rollingTask.setStatus(RunStatusEnum.BLANK);
            rollingTask.setDispatchTime(dispatchTime);
            rollingTask.setDispatched(Boolean.FALSE);
            stepInstanceRollingTaskService.saveRollingTask(rollingTask);
            // 入延迟队列
            scatterDispatchManager.addTask(new ScatterDispatchTask(
                taskInstanceId, stepInstanceId, executeCount, batch, dispatchTime));
        }
        log.info("Scheduled parallel scatter batches, stepInstanceId={}, executeCount={}, totalBatch={}",
            stepInstanceId, executeCount, totalBatch);
    }

    /**
     * 启动GSE任务
     *
     * @param stepInstance 步骤实例
     * @param gseTaskId    Gse任务ID
     */
    private void startGseTask(StepInstanceDTO stepInstance, Long gseTaskId) {
        if (stepInstance.isScriptStep()) {
            taskExecuteMQEventDispatcher.dispatchGseTaskEvent(
                GseTaskEvent.startGseTask(
                    stepInstance.getTaskInstanceId(),
                    stepInstance.getId(),
                    stepInstance.getExecuteCount(),
                    stepInstance.getBatch(),
                    gseTaskId,
                    null));
        } else if (stepInstance.isFileStep()) {
            if (filePrepareService.needToPrepareSourceFilesForGseTask(stepInstance)) {
                filePrepareService.prepareFileForGseTask(stepInstance);
            } else {
                taskExecuteMQEventDispatcher.dispatchGseTaskEvent(
                    GseTaskEvent.startGseTask(
                        stepInstance.getTaskInstanceId(),
                        stepInstance.getId(),
                        stepInstance.getExecuteCount(),
                        stepInstance.getBatch(),
                        gseTaskId,
                        null));
            }
        }
    }

    /**
     * 再次准备要分发的文件
     *
     * @param stepInstance 步骤实例
     */
    private void prepareFile(StepInstanceDTO stepInstance) {
        filePrepareService.prepareFileForGseTask(stepInstance);
    }

    /**
     * 初始化的GSE任务
     *
     * @param stepInstance 步骤实例
     * @return GSE 任务ID
     */
    private Long saveInitialGseTask(StepInstanceDTO stepInstance) {
        GseTaskDTO gseTask = new GseTaskDTO(stepInstance.getTaskInstanceId(), stepInstance.getId(),
            stepInstance.getExecuteCount(), stepInstance.getBatch());
        gseTask.setStatus(RunStatusEnum.BLANK.getValue());

        return gseTaskService.saveGseTask(gseTask);
    }

    /**
     * 启动步骤的时候保存执行对象任务
     *
     * @param gseTaskId     GSE任务ID
     * @param stepInstance  步骤实例
     * @param rollingConfig 滚动配置
     */
    private void saveExecuteObjectTasksForStartStep(Long gseTaskId,
                                                    StepInstanceDTO stepInstance,
                                                    RollingConfigDTO rollingConfig) {
        long stepInstanceId = stepInstance.getId();
        int executeCount = stepInstance.getExecuteCount();
        int batch = stepInstance.getBatch();

        if (stepInstance.isRollingStep()) {
            // 滚动步骤
            saveGseExecuteObjectTasksForStartRollingStep(gseTaskId, stepInstance, rollingConfig);
        } else {
            // 普通步骤，启动的时候需要初始化所有ExecuteObjectTask
            List<ExecuteObjectTask> executeObjectTasks = new ArrayList<>(
                buildInitialExecuteObjectTasks(
                    stepInstance.getTaskInstanceId(),
                    stepInstanceId,
                    executeCount,
                    executeCount,
                    batch,
                    gseTaskId,
                    stepInstance.getTargetExecuteObjects().getExecuteObjectsCompatibly())
            );
            saveExecuteObjectTasks(stepInstance, executeObjectTasks);
        }
    }

    /**
     * 启动滚动执行步骤的时候保存执行对象任务
     *
     * @param gseTaskId     GSE任务ID
     * @param stepInstance  步骤实例
     * @param rollingConfig 滚动配置
     */
    private void saveGseExecuteObjectTasksForStartRollingStep(Long gseTaskId,
                                                              StepInstanceDTO stepInstance,
                                                              RollingConfigDTO rollingConfig) {
        long stepInstanceId = stepInstance.getId();
        int executeCount = stepInstance.getExecuteCount();
        int batch = stepInstance.getBatch();
        if (stepInstance.isFirstRollingBatch()) {
            // 如果是第一批次的执行，需要提前初始化所有批次的执行对象任务（作业详情查询主机任务列表需要)
            List<ExecuteObjectTask> executeObjectTasks = new ArrayList<>();
            if (rollingConfig.isExecuteObjectBatchRollingStep(stepInstanceId)) {
                // 按目标执行对象分批
                List<RollingExecuteObjectsBatchDO> executeObjectsBatchList =
                    rollingConfig.getExecuteObjectRollingConfig().getExecuteObjectsBatchListCompatibly();
                // 为每一批执行对象生成单个执行对象任务数据
                executeObjectsBatchList.forEach(executeObjectsBatch -> {
                    executeObjectTasks.addAll(
                        buildInitialExecuteObjectTasks(
                            stepInstance.getTaskInstanceId(),
                            stepInstanceId,
                            executeCount,
                            executeObjectsBatch.getBatch() == 1 ? executeCount : null,
                            executeObjectsBatch.getBatch(),
                            executeObjectsBatch.getBatch() == 1 ? gseTaskId : 0,
                            executeObjectsBatch.getExecuteObjectsCompatibly()
                        )
                    );
                });
                saveExecuteObjectTasks(stepInstance, executeObjectTasks);
            } else if (rollingConfig.isFileSourceBatchRollingStep(stepInstanceId)) {
                // 按源文件分批
                List<StepInstanceFileBatchDTO> stepInstanceFileBatchList = stepInstanceFileBatchService.list(
                    stepInstance.getTaskInstanceId(),
                    stepInstanceId
                );
                stepInstanceFileBatchList.forEach(stepInstanceFileBatch -> {
                    executeObjectTasks.addAll(
                        buildInitialExecuteObjectTasks(
                            stepInstance.getTaskInstanceId(),
                            stepInstanceId,
                            executeCount,
                            stepInstanceFileBatch.getBatch() == 1 ? executeCount : null,
                            stepInstanceFileBatch.getBatch(),
                            stepInstanceFileBatch.getBatch() == 1 ? gseTaskId : 0,
                            stepInstance.getTargetExecuteObjects().getExecuteObjectsCompatibly()
                        )
                    );
                });
                saveExecuteObjectTasks(stepInstance, executeObjectTasks);
            } else {
                // 暂时不支持，滚动执行二期需求
                log.warn("All rolling step is not supported!");
                throw new NotImplementedException("All rolling step is not supported",
                    ErrorCode.NOT_SUPPORT_FEATURE);
            }
        } else {
            // 滚动执行步骤除了第一批次，后续的批次仅更新 ExecuteObjectTask 的 actualExecuteCount、gseTaskId
            if (stepInstance.isScriptStep()) {
                scriptExecuteObjectTaskService.updateTaskFields(stepInstance, executeCount, batch, executeCount,
                    gseTaskId);
            } else if (stepInstance.isFileStep()) {
                fileExecuteObjectTaskService.updateTaskFields(stepInstance, executeCount, batch, executeCount,
                    gseTaskId);
            }
        }
    }

    private List<ExecuteObjectTask> buildInitialExecuteObjectTasks(long taskInstanceId,
                                                                   long stepInstanceId,
                                                                   int executeCount,
                                                                   Integer actualExecuteCount,
                                                                   int batch,
                                                                   Long gseTaskId,
                                                                   List<ExecuteObject> executeObjects) {
        return executeObjects.stream()
            .map(executeObject -> {
                ExecuteObjectTask executeObjectTask = new ExecuteObjectTask(
                    taskInstanceId,
                    stepInstanceId,
                    executeCount,
                    batch,
                    executeObject
                );
                executeObjectTask.setStepInstanceId(stepInstanceId);
                executeObjectTask.setExecuteCount(executeCount);
                executeObjectTask.setActualExecuteCount(actualExecuteCount);
                executeObjectTask.setBatch(batch);
                executeObjectTask.setGseTaskId(gseTaskId);
                executeObjectTask.setStatus(executeObject.isExecutable() ?
                    ExecuteObjectTaskStatusEnum.WAITING :
                    executeObject.isAgentIdEmpty() ?
                        ExecuteObjectTaskStatusEnum.AGENT_NOT_INSTALLED :
                        ExecuteObjectTaskStatusEnum.INVALID_EXECUTE_OBJECT);
                if (!executeObject.isExecutable()) {
                    executeObjectTask.setStartTime(System.currentTimeMillis());
                    executeObjectTask.setEndTime(System.currentTimeMillis());
                    executeObjectTask.setTotalTime(0L);
                }
                executeObjectTask.setFileTaskMode(FileTaskModeEnum.DOWNLOAD);
                executeObjectTask.setExecuteObject(executeObject);
                return executeObjectTask;
            })
            .collect(Collectors.toList());
    }

    /**
     * 保存初始化的步骤实例上的滚动任务
     *
     * @param stepInstance 步骤实例
     */
    private void saveInitialStepInstanceRollingTask(StepInstanceDTO stepInstance) {
        StepInstanceRollingTaskDTO stepInstanceRollingTask = new StepInstanceRollingTaskDTO();
        stepInstanceRollingTask.setTaskInstanceId(stepInstance.getTaskInstanceId());
        stepInstanceRollingTask.setStepInstanceId(stepInstance.getId());
        stepInstanceRollingTask.setBatch(stepInstance.getBatch());
        stepInstanceRollingTask.setExecuteCount(stepInstance.getExecuteCount());
        stepInstanceRollingTask.setStatus(RunStatusEnum.RUNNING);
        stepInstanceRollingTask.setStartTime(System.currentTimeMillis());
        stepInstanceRollingTaskService.saveRollingTask(stepInstanceRollingTask);
    }

    private void nextStep(StepInstanceDTO stepInstance) {
        log.info("Next step, stepInstanceId={}", stepInstance.getId());

        long taskInstanceId = stepInstance.getTaskInstanceId();
        long stepInstanceId = stepInstance.getId();
        RunStatusEnum stepStatus = stepInstance.getStatus();

        if (RunStatusEnum.STOP_SUCCESS == stepStatus) {
            taskInstanceService.updateTaskStatus(taskInstanceId, RunStatusEnum.RUNNING.getValue());
            long endTime = DateUtils.currentTimeMillis();
            long totalTime = TaskCostCalculator.calculate(stepInstance.getStartTime(), endTime,
                stepInstance.getTotalTime());
            // 终止成功，进入下一步，该步骤设置为“跳过”
            stepInstanceService.updateStepExecutionInfo(
                taskInstanceId,
                stepInstanceId,
                RunStatusEnum.SKIPPED,
                null,
                endTime,
                totalTime);
            taskExecuteMQEventDispatcher.dispatchJobEvent(
                JobEvent.refreshJob(taskInstanceId, EventSource.buildStepEventSource(taskInstanceId, stepInstanceId)));
        } else {
            log.warn("Unsupported step instance status for next step action, stepInstanceId:{}, status:{}",
                stepInstanceId, stepInstance.getStatus());
        }
    }

    private void ignoreError(StepInstanceDTO stepInstance) {
        log.info("Ignore step error, stepInstanceId={}", stepInstance.getId());

        if (stepInstance.getStatus() != RunStatusEnum.FAIL) {
            log.warn("Current step status does not support ignore error operation! stepInstanceId:{}, status:{}",
                stepInstance.getId(), stepInstance.getStatus());
            return;
        }

        long taskInstanceId = stepInstance.getTaskInstanceId();
        long stepInstanceId = stepInstance.getId();
        boolean isRollingStep = stepInstance.isRollingStep();
        if (isRollingStep) {
            log.info("Retry-fail for rolling step, stepInstanceId={}, batch: {}", stepInstanceId,
                stepInstance.getBatch());
        } else {
            log.info("Retry-fail for step, stepInstanceId={}", stepInstanceId);
        }

        stepInstanceService.updateStepStatus(taskInstanceId, stepInstance.getId(),
            RunStatusEnum.IGNORE_ERROR.getValue());
        taskInstanceService.resetTaskExecuteInfoForRetry(stepInstance.getTaskInstanceId());
        if (isRollingStep) {
            StepInstanceRollingTaskDTO stepInstanceRollingTask =
                stepInstanceRollingTaskService.queryRollingTask(taskInstanceId, stepInstanceId,
                    stepInstance.getExecuteCount(), stepInstance.getBatch());
            if (stepInstanceRollingTask != null) {
                finishRollingTask(taskInstanceId, stepInstanceId, stepInstance.getExecuteCount(),
                    stepInstance.getBatch(), RunStatusEnum.IGNORE_ERROR);
            }
        }

        taskExecuteMQEventDispatcher.dispatchJobEvent(
            JobEvent.refreshJob(stepInstance.getTaskInstanceId(),
                EventSource.buildStepEventSource(taskInstanceId, stepInstance.getId())));
    }


    private void skipStep(StepInstanceDTO stepInstance) {
        RunStatusEnum stepStatus = stepInstance.getStatus();
        long stepInstanceId = stepInstance.getId();
        long taskInstanceId = stepInstance.getTaskInstanceId();

        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(stepInstance.getTaskInstanceId());
        if (!(taskInstance.getCurrentStepInstanceId() == stepInstanceId)) {
            log.warn("Only current running step is support for skipping, stepInstanceId={}", stepInstanceId);
            return;
        }

        log.info("Skip step, stepInstanceId={}", stepInstanceId);

        // 只有当步骤状态为'终止中'时可以跳过步骤
        if (RunStatusEnum.STOPPING == stepStatus) {
            long now = DateUtils.currentTimeMillis();
            stepInstanceService.updateStepStartTimeIfNull(taskInstanceId, stepInstanceId, now);
            stepInstanceService.updateStepStatus(taskInstanceId, stepInstanceId, RunStatusEnum.SKIPPED.getValue());
            stepInstanceService.updateStepEndTime(taskInstanceId, stepInstanceId, now);

            taskInstanceService.updateTaskStatus(taskInstanceId, RunStatusEnum.RUNNING.getValue());
            taskExecuteMQEventDispatcher.dispatchJobEvent(
                JobEvent.refreshJob(taskInstanceId, EventSource.buildStepEventSource(taskInstanceId, stepInstanceId)));
        } else {
            log.warn("Unsupported step instance run status for skipping step, stepInstanceId={}, status={}",
                stepInstanceId, stepStatus);
        }
    }

    private void stopStep(StepInstanceDTO stepInstance) {
        long taskInstanceId = stepInstance.getTaskInstanceId();
        long stepInstanceId = stepInstance.getId();
        log.info("Force stop step, stepInstanceId={}", stepInstanceId);

        RunStatusEnum stepStatus = stepInstance.getStatus();
        if (stepStatus == RunStatusEnum.WAITING_USER) {
            log.info("Step status is WAITING_USER, set step status stop_success directly!");
            // 等待用户的步骤可以直接结束
            long endTime = DateUtils.currentTimeMillis();
            stepInstanceService.updateStepExecutionInfo(
                taskInstanceId,
                stepInstanceId,
                RunStatusEnum.STOP_SUCCESS,
                null,
                endTime,
                TaskCostCalculator.calculate(stepInstance.getStartTime(), endTime, stepInstance.getTotalTime()));

            taskExecuteMQEventDispatcher.dispatchJobEvent(JobEvent.refreshJob(stepInstance.getTaskInstanceId(),
                EventSource.buildStepEventSource(taskInstanceId, stepInstanceId)));
        } else if (stepStatus == RunStatusEnum.RUNNING) {
            // 正在运行中的任务无法立即结束，需要等待任务调度引擎检测到停止状态;这里只需要处理设置步骤状态即可
            stepInstanceService.updateStepStatus(taskInstanceId, stepInstanceId, RunStatusEnum.STOPPING.getValue());
            // 并行错峰模式：取消队列中尚未下发的批次，并把它们标记为终止成功，便于完成判定收敛
            if (stepInstance.isRollingStep()) {
                RollingConfigDTO rollingConfig = rollingConfigService.getRollingConfig(
                    taskInstanceId, stepInstance.getRollingConfigId());
                if (rollingConfig != null && rollingConfig.isParallelExecution()) {
                    // 本副本直接取消并收敛（即时性）
                    cancelUnDispatchedBatchesForStop(stepInstance, rollingConfig);
                    // #3A 广播到所有副本（fanout，含自身），让其它副本即时取消并收敛其本地队列中的未下发批次。
                    // 未下发批次仅存在于唯一持有副本队列：若终止落到该持有副本，上面已就地取消，自身广播到达时
                    // 队列已空（cancelStepTasks 返回空）天然幂等；若落到非持有副本，则由广播抵达持有副本完成取消。
                    // 广播仅作即时性优化，即使丢失仍由 ScatterBatchDispatcher 到点兜底收敛保证最终正确性。
                    taskExecuteMQEventDispatcher.broadcastScatterBatchCancelEvent(
                        ScatterBatchCancelEvent.cancel(taskInstanceId, stepInstanceId,
                            stepInstance.getExecuteCount()));
                }
            }
        }
    }

    /**
     * 并行错峰模式整步终止：取消本副本延迟队列中未下发的批次，并将其置为终止成功以参与完成判定。
     * <p>
     * 注意：未下发批次仅存在于「触发调度副本」的本地内存队列，而终止事件经 MQ 被任一副本竞争消费，
     * 故本副本可能取消到 0 个批次（终止落到非调度副本）。此时依靠两道保障使步骤仍能正常收敛，不会永久卡在 STOPPING：
     * <ul>
     *     <li>#3A 广播即时取消：终止路径另发 {@link ScatterBatchCancelEvent} 广播到所有副本，持有队列副本收到后
     *     就地取消并收敛（见 {@link ScatterBatchCancelListener}）；</li>
     *     <li>B 兜底：即使广播丢失，未取消批次到点触发下发时由
     *     {@link com.tencent.bk.job.execute.engine.rolling.scatter.ScatterBatchDispatcher} 感知步骤处于终止/终态
     *     语义而自行收敛。</li>
     * </ul>
     */
    private void cancelUnDispatchedBatchesForStop(StepInstanceDTO stepInstance, RollingConfigDTO rollingConfig) {
        long taskInstanceId = stepInstance.getTaskInstanceId();
        long stepInstanceId = stepInstance.getId();
        int executeCount = stepInstance.getExecuteCount();
        int totalBatch = rollingConfig.getExecuteObjectRollingConfig().getTotalBatch();

        List<ScatterDispatchTask> canceled = scatterDispatchManager.cancelStepTasks(
            taskInstanceId, stepInstanceId, executeCount);
        if (CollectionUtils.isEmpty(canceled)) {
            log.info("No un-dispatched scatter batch canceled on this replica, stepInstanceId={}, executeCount={}. "
                    + "Remaining un-dispatched batches (if any) will self-converge at dispatch time on their "
                    + "holding replica.", stepInstanceId, executeCount);
            return;
        }
        long now = System.currentTimeMillis();
        for (ScatterDispatchTask task : canceled) {
            scatterStepConverger.finishBatchAndConverge(stepInstance, executeCount, task.getBatch(),
                RunStatusEnum.STOP_SUCCESS, now, totalBatch, true);
        }
    }

    /**
     * 第三方文件源文件拉取完成后继续GSE文件分发
     *
     * @param stepInstance 步骤实例
     */
    private void continueGseFileStep(StepInstanceDTO stepInstance) {
        log.info("Continue file push step, stepInstanceId={}", stepInstance.getId());

        GseTaskDTO gseTask =
            gseTaskService.getGseTask(stepInstance.getTaskInstanceId(), stepInstance.getId(),
                stepInstance.getExecuteCount(), stepInstance.getBatch());
        taskExecuteMQEventDispatcher.dispatchGseTaskEvent(GseTaskEvent.startGseTask(
            stepInstance.getTaskInstanceId(), gseTask.getStepInstanceId(), gseTask.getExecuteCount(),
            gseTask.getBatch(), gseTask.getId(), null));
    }

    /**
     * 重新执行步骤失败的任务
     */
    private void retryStepFail(StepInstanceDTO stepInstance) {
        long stepInstanceId = stepInstance.getId();
        boolean isRollingStep = stepInstance.isRollingStep();
        if (isRollingStep) {
            log.info("Retry-fail for rolling step, stepInstanceId={}, batch: {}", stepInstanceId,
                stepInstance.getBatch());
        } else {
            log.info("Retry-fail for step, stepInstanceId={}", stepInstanceId);
        }

        RunStatusEnum stepStatus = stepInstance.getStatus();
        if (isStepSupportRetry(stepStatus)) {

            // 并行错峰模式：重试作用域为整步所有批次，重算各批错峰下发
            if (isRollingStep) {
                RollingConfigDTO rollingConfig = rollingConfigService.getRollingConfig(
                    stepInstance.getTaskInstanceId(), stepInstance.getRollingConfigId());
                if (rollingConfig != null && rollingConfig.isParallelExecution()) {
                    retryParallelStep(stepInstance, rollingConfig, false);
                    return;
                }
            }

            resetExecutionInfoForRetry(stepInstance);

            if (isRollingStep) {
                // 初始化步骤滚动任务
                saveInitialStepInstanceRollingTask(stepInstance);
            }

            Long gseTaskId = saveInitialGseTask(stepInstance);
            saveExecuteObjectTasksForRetryFail(stepInstance, stepInstance.getExecuteCount(), stepInstance.getBatch(),
                gseTaskId);

            startGseTask(stepInstance, gseTaskId);
        } else {
            log.warn("Unsupported step instance run status for retry step, stepInstanceId={}, status={}",
                stepInstanceId, stepStatus);
        }
    }

    /**
     * 并行错峰模式整步重试：为所有批次重新准备执行对象任务与 GSE 任务，
     * 第一批次即时下发，其余批次重算 dispatch_time 后错峰下发。
     *
     * @param stepInstance  步骤实例
     * @param rollingConfig 滚动配置
     * @param retryAll      true-从头重试全部；false-仅重试失败
     */
    private void retryParallelStep(StepInstanceDTO stepInstance, RollingConfigDTO rollingConfig, boolean retryAll) {
        long taskInstanceId = stepInstance.getTaskInstanceId();
        long stepInstanceId = stepInstance.getId();

        resetExecutionInfoForRetry(stepInstance);

        int executeCount = stepInstance.getExecuteCount();
        ExecuteObjectRollingConfigDetailDO detail = rollingConfig.getExecuteObjectRollingConfig();
        int totalBatch = detail.getTotalBatch();
        long fixedMs = detail.getBatchStartWaitFixedMs() == null ? 0L : detail.getBatchStartWaitFixedMs();
        long randomMinMs = detail.getBatchStartWaitRandomMinMs() == null ? 0L : detail.getBatchStartWaitRandomMinMs();
        long randomMaxMs = detail.getBatchStartWaitRandomMaxMs() == null ? 0L : detail.getBatchStartWaitRandomMaxMs();
        long baseTime = System.currentTimeMillis();

        if (stepInstance.getStartTime() == null) {
            stepInstance.setStartTime(baseTime);
        }
        stepInstanceService.updateStepExecutionInfo(taskInstanceId, stepInstanceId, RunStatusEnum.RUNNING,
            stepInstance.getStartTime(), null, null);

        // 累积式错峰：批1下发时刻=baseTime，批k = 批(k-1)下发时刻 + fixed + random(min,max)，随机每批独立采样
        long[] dispatchTimes = ScatterBatchTimeCalculator.computeDispatchTimes(
            baseTime, totalBatch, fixedMs, randomMinMs, randomMaxMs);
        for (int batch = 1; batch <= totalBatch; batch++) {
            stepInstance.setBatch(batch);
            // 登记批次滚动任务
            StepInstanceRollingTaskDTO rollingTask = new StepInstanceRollingTaskDTO();
            rollingTask.setTaskInstanceId(taskInstanceId);
            rollingTask.setStepInstanceId(stepInstanceId);
            rollingTask.setExecuteCount(executeCount);
            rollingTask.setBatch(batch);
            long dispatchTime = dispatchTimes[batch - 1];
            rollingTask.setDispatchTime(dispatchTime);
            if (batch == 1) {
                rollingTask.setStatus(RunStatusEnum.RUNNING);
                rollingTask.setStartTime(baseTime);
                rollingTask.setDispatched(Boolean.TRUE);
            } else {
                rollingTask.setStatus(RunStatusEnum.BLANK);
                rollingTask.setDispatched(Boolean.FALSE);
            }
            stepInstanceRollingTaskService.saveRollingTask(rollingTask);

            // 为该批次准备 GSE 任务与执行对象任务（尊重失败重试/全部重试语义）
            // 注意：并行重试按批次逐次调用，必须仅保存「本批次」的执行对象任务；
            // 若沿用串行的整表保存逻辑，会在后续批次迭代时重复插入前序批次的行，
            // 触发 gse_file_execute_obj_task 唯一键 uk_step_id_execute_count_batch_mode_execute_obj_id
            // 冲突导致循环中断，出现「仅批1重试、批2..N不下发、整步卡 RUNNING」的缺陷。
            Long gseTaskId = saveInitialGseTask(stepInstance);
            saveExecuteObjectTasksForParallelRetryBatch(stepInstance, executeCount, batch, gseTaskId, retryAll);

            if (batch == 1) {
                startGseTask(stepInstance, gseTaskId);
            } else {
                scatterDispatchManager.addTask(new ScatterDispatchTask(
                    taskInstanceId, stepInstanceId, executeCount, batch, dispatchTime, gseTaskId));
            }
        }
        log.info("Retry parallel step scheduled, stepInstanceId={}, executeCount={}, totalBatch={}, retryAll={}",
            stepInstanceId, executeCount, totalBatch, retryAll);
    }

    private boolean isStepSupportRetry(RunStatusEnum stepStatus) {
        return RunStatusEnum.FAIL == stepStatus || RunStatusEnum.ABNORMAL_STATE == stepStatus
            || RunStatusEnum.STOP_SUCCESS == stepStatus;
    }

    private void saveExecuteObjectTasksForRetryFail(StepInstanceBaseDTO stepInstance,
                                                    int executeCount,
                                                    Integer batch,
                                                    Long gseTaskId) {
        List<ExecuteObjectTask> retryExecuteObjectTasks = listTargetExecuteObjectTasks(stepInstance, executeCount - 1);

        for (ExecuteObjectTask retryExecuteObjectTask : retryExecuteObjectTasks) {
            retryExecuteObjectTask.setExecuteCount(executeCount);
            if (batch != null && retryExecuteObjectTask.getBatch() != batch) {
                continue;
            }
            // 只有失败的目标主机才需要参与重试
            if (!ExecuteObjectTaskStatusEnum.isSuccess(retryExecuteObjectTask.getStatus())) {
                if (retryExecuteObjectTask.getExecuteObject().isExecutable()) {
                    retryExecuteObjectTask.setActualExecuteCount(executeCount);
                    retryExecuteObjectTask.resetTaskInitialStatus();
                }
                retryExecuteObjectTask.setGseTaskId(gseTaskId);
            }
        }

        saveExecuteObjectTasks(stepInstance, retryExecuteObjectTasks);
    }


    private void saveExecuteObjectTasksForRetryAll(StepInstanceBaseDTO stepInstance,
                                                   int executeCount,
                                                   Integer batch,
                                                   Long gseTaskId) {
        List<ExecuteObjectTask> retryExecuteObjectTasks = listTargetExecuteObjectTasks(stepInstance, executeCount - 1);

        for (ExecuteObjectTask retryExecuteObjectTask : retryExecuteObjectTasks) {
            retryExecuteObjectTask.setExecuteCount(executeCount);
            if (batch != null && retryExecuteObjectTask.getBatch() != batch) {
                continue;
            }
            if (retryExecuteObjectTask.getExecuteObject().isExecutable()) {
                // 重置运行数据
                retryExecuteObjectTask.setActualExecuteCount(executeCount);
                retryExecuteObjectTask.resetTaskInitialStatus();
            }
            retryExecuteObjectTask.setGseTaskId(gseTaskId);
        }

        saveExecuteObjectTasks(stepInstance, retryExecuteObjectTasks);
    }

    /**
     * 并行错峰模式整步重试：仅为「单个批次」准备并保存执行对象任务。
     * <p>
     * 与串行重试的 {@link #saveExecuteObjectTasksForRetryAll}/{@link #saveExecuteObjectTasksForRetryFail}
     * 不同，本方法在保存前按 batch 过滤，保证每个批次的执行对象任务只在其对应迭代中插入一次，
     * 避免并行重试逐批循环时对同一 (step, executeCount, batch, mode, executeObject) 重复插入而违反唯一键。
     *
     * @param stepInstance 步骤实例
     * @param executeCount 本次重试的执行次数
     * @param batch        目标批次
     * @param gseTaskId    本批次的 GSE 任务ID
     * @param retryAll     true-全部重试（所有可执行对象重跑）；false-失败重试（仅失败对象重跑，成功对象结转）
     */
    private void saveExecuteObjectTasksForParallelRetryBatch(StepInstanceBaseDTO stepInstance,
                                                             int executeCount,
                                                             int batch,
                                                             Long gseTaskId,
                                                             boolean retryAll) {
        // 仅查询「当前批次」的前序执行对象任务，避免逐批循环时重复加载全量数据造成的查询与内存浪费
        List<ExecuteObjectTask> batchExecuteObjectTasks =
            listTargetExecuteObjectTasks(stepInstance, executeCount - 1, batch);
        for (ExecuteObjectTask executeObjectTask : batchExecuteObjectTasks) {
            executeObjectTask.setExecuteCount(executeCount);
            boolean needRetry = retryAll
                || !ExecuteObjectTaskStatusEnum.isSuccess(executeObjectTask.getStatus());
            if (needRetry) {
                if (executeObjectTask.getExecuteObject().isExecutable()) {
                    executeObjectTask.setActualExecuteCount(executeCount);
                    executeObjectTask.resetTaskInitialStatus();
                }
                executeObjectTask.setGseTaskId(gseTaskId);
            }
        }
        saveExecuteObjectTasks(stepInstance, batchExecuteObjectTasks);
    }

    private List<ExecuteObjectTask> listTargetExecuteObjectTasks(StepInstanceBaseDTO stepInstance, int executeCount) {
        return listTargetExecuteObjectTasks(stepInstance, executeCount, null);
    }

    /**
     * 查询步骤的目标执行对象任务，可按批次过滤。
     *
     * @param stepInstance 步骤实例
     * @param executeCount 执行次数
     * @param batch        目标批次；为 {@code null} 时查询全部批次
     */
    private List<ExecuteObjectTask> listTargetExecuteObjectTasks(StepInstanceBaseDTO stepInstance,
                                                                 int executeCount,
                                                                 Integer batch) {
        List<ExecuteObjectTask> executeObjectTasks = Collections.emptyList();
        if (stepInstance.isScriptStep()) {
            executeObjectTasks = scriptExecuteObjectTaskService.listTasks(stepInstance, executeCount, batch);
        } else if (stepInstance.isFileStep()) {
            executeObjectTasks = fileExecuteObjectTaskService.listTasks(stepInstance, executeCount, batch,
                FileTaskModeEnum.DOWNLOAD);
        }
        return executeObjectTasks;
    }

    private void saveExecuteObjectTasks(StepInstanceBaseDTO stepInstance, List<ExecuteObjectTask> executeObjectTasks) {
        if (CollectionUtils.isNotEmpty(executeObjectTasks)) {
            if (stepInstance.isScriptStep()) {
                scriptExecuteObjectTaskService.batchSaveTasks(executeObjectTasks);
            } else if (stepInstance.isFileStep()) {
                fileExecuteObjectTaskService.batchSaveTasks(executeObjectTasks);
            }
        }
    }

    /**
     * 从头执行步骤
     */
    private void retryStepAll(StepInstanceDTO stepInstance) {

        long stepInstanceId = stepInstance.getId();
        boolean isRollingStep = stepInstance.isRollingStep();
        if (isRollingStep) {
            log.info("Retry-all for rolling step, stepInstanceId={}, batch: {}", stepInstanceId,
                stepInstance.getBatch());
        } else {
            log.info("Retry-all for step, stepInstanceId={}", stepInstanceId);
        }

        RunStatusEnum stepStatus = stepInstance.getStatus();
        if (isStepSupportRetry(stepStatus)) {

            // 并行错峰模式：重试作用域为整步所有批次
            if (isRollingStep) {
                RollingConfigDTO rollingConfig = rollingConfigService.getRollingConfig(
                    stepInstance.getTaskInstanceId(), stepInstance.getRollingConfigId());
                if (rollingConfig != null && rollingConfig.isParallelExecution()) {
                    retryParallelStep(stepInstance, rollingConfig, true);
                    return;
                }
            }

            resetExecutionInfoForRetry(stepInstance);

            if (isRollingStep) {
                // 初始化步骤滚动任务
                saveInitialStepInstanceRollingTask(stepInstance);
            }

            Long gseTaskId = saveInitialGseTask(stepInstance);
            saveExecuteObjectTasksForRetryAll(stepInstance, stepInstance.getExecuteCount(), stepInstance.getBatch(),
                gseTaskId);

            startGseTask(stepInstance, gseTaskId);
        } else {
            log.warn("Unsupported step instance run status for retry step, stepInstanceId={}, status={}",
                stepInstanceId, stepStatus);
        }
    }

    /**
     * 清理执行完的步骤
     */
    private void clearStep(StepInstanceDTO stepInstance) {
        // 当前仅有文件分发类步骤需要清理中间文件
        if (stepInstance.isFileStep()) {
            log.info("Clear file step, stepInstanceId={}", stepInstance.getId());
            filePrepareService.clearPreparedTmpFile(stepInstance.getTaskInstanceId(), stepInstance.getId());
        }
    }

    /**
     * 重置作业、步骤执行状态，包括结束时间、任务状态、任务耗时
     *
     * @param stepInstance 步骤实例
     */
    private void resetExecutionInfoForRetry(StepInstanceDTO stepInstance) {
        long stepInstanceId = stepInstance.getId();
        long taskInstanceId = stepInstance.getTaskInstanceId();

        stepInstanceService.resetStepExecuteInfoForRetry(taskInstanceId, stepInstanceId);
        taskInstanceService.resetTaskExecuteInfoForRetry(taskInstanceId);
    }

    private void refreshStep(StepEvent stepEvent, StepInstanceDTO stepInstance) {

        long stepInstanceId = stepInstance.getId();
        EventSource eventSource = stepEvent.getSource();

        GseTaskDTO gseTask = gseTaskService.getGseTask(eventSource.getJobInstanceId(), eventSource.getGseTaskId());

        // 并行错峰模式：各批次并发执行，GSE 任务终态回调携带真正结束的批次；
        // 而并行模式下 JobListener 不逐批 nextStep/不 updateStepCurrentBatch，stepInstance.getBatch() 恒为 1，
        // 故此处按 gseTask 回填结束批次，保证 finishParallelBatch 按真正结束的批次做终态跃迁与完成判定。
        // 串行模式 stepInstance.getBatch() 由 JobListener 逐批维护且与 gseTask 一致，此处不做回填以免影响既有逻辑。
        if (stepInstance.isRollingStep()) {
            RollingConfigDTO rollingConfig = rollingConfigService.getRollingConfig(
                stepInstance.getTaskInstanceId(), stepInstance.getRollingConfigId());
            if (rollingConfig != null && rollingConfig.isParallelExecution()) {
                stepInstance.setBatch(gseTask.getBatch());
            }
        }

        RunStatusEnum gseTaskStatus = RunStatusEnum.valueOf(gseTask.getStatus());
        log.info("Refresh step according to gse task status, stepInstanceId: {}, gseTaskStatus: {}, batch: {}",
            stepInstance.getId(), gseTaskStatus.name(), stepInstance.getBatch());

        switch (gseTaskStatus) {
            case SUCCESS:
                onSuccess(stepInstance);
                break;
            case FAIL:
                onFail(stepInstance);
                break;
            case STOP_SUCCESS:
                onStopSuccess(stepInstance);
                break;
            case ABNORMAL_STATE:
                finishStepWithAbnormalState(stepInstance);
                break;
            case ABANDONED:
                onAbandonState(stepInstance);
                break;
            default:
                log.error("Refresh step fail because of unexpected gse task status. stepInstanceId: {}, " +
                        "gseTaskStatus: {}",
                    stepInstanceId, gseTaskStatus.getValue());
                finishStep(stepInstance, RunStatusEnum.ABNORMAL_STATE);
                break;
        }

        // 更新作业状态
        taskExecuteMQEventDispatcher.dispatchJobEvent(
            JobEvent.refreshJob(stepInstance.getTaskInstanceId(),
                EventSource.buildStepEventSource(stepInstance.getTaskInstanceId(), stepInstanceId)));
    }

    private void onSuccess(StepInstanceDTO stepInstance) {
        long taskInstanceId = stepInstance.getTaskInstanceId();
        long stepInstanceId = stepInstance.getId();
        long endTime = System.currentTimeMillis();
        long startTime = stepInstance.getStartTime();
        long totalTime = endTime - startTime;

        // 并行错峰模式：单批次成功，走并发安全的完成判定
        if (stepInstance.isRollingStep()) {
            RollingConfigDTO rollingConfig = rollingConfigService.getRollingConfig(
                taskInstanceId, stepInstance.getRollingConfigId());
            if (rollingConfig != null && rollingConfig.isParallelExecution()) {
                finishParallelBatch(stepInstance, rollingConfig, RunStatusEnum.SUCCESS);
                return;
            }
        }

        if (stepInstance.isRollingStep()) {
            finishRollingTask(
                taskInstanceId,
                stepInstanceId,
                stepInstance.getExecuteCount(),
                stepInstance.getBatch(),
                RunStatusEnum.SUCCESS
            );
            int totalBatch = rollingConfigService.getTotalBatch(
                taskInstanceId,
                stepInstanceId,
                stepInstance.getRollingConfigId()
            );
            boolean isLastBatch = totalBatch == stepInstance.getBatch();
            if (isLastBatch) {
                stepInstanceService.updateStepExecutionInfo(
                    taskInstanceId,
                    stepInstanceId,
                    RunStatusEnum.SUCCESS,
                    startTime,
                    endTime,
                    totalTime
                );
                // 步骤执行成功后清理产生的临时文件
                clearStep(stepInstance);
            } else {
                stepInstanceService.updateStepExecutionInfo(
                    taskInstanceId,
                    stepInstanceId,
                    RunStatusEnum.ROLLING_WAITING,
                    startTime,
                    endTime,
                    totalTime
                );
            }
        } else {
            stepInstanceService.updateStepExecutionInfo(
                taskInstanceId,
                stepInstanceId,
                RunStatusEnum.SUCCESS,
                startTime,
                endTime,
                totalTime
            );
            // 步骤执行成功后清理产生的临时文件
            clearStep(stepInstance);
        }
    }

    private void onFail(StepInstanceDTO stepInstance) {
        long taskInstanceId = stepInstance.getTaskInstanceId();
        long stepInstanceId = stepInstance.getId();
        long endTime = System.currentTimeMillis();
        long startTime = stepInstance.getStartTime();
        long totalTime = endTime - startTime;

        // 并行错峰模式：单批次失败不中断其它批，走并发安全的完成判定；任一批失败=步骤失败
        if (stepInstance.isRollingStep()) {
            RollingConfigDTO parallelRollingConfig = rollingConfigService.getRollingConfig(
                taskInstanceId, stepInstance.getRollingConfigId());
            if (parallelRollingConfig != null && parallelRollingConfig.isParallelExecution()) {
                RunStatusEnum batchStatus = stepInstance.isIgnoreError()
                    ? RunStatusEnum.IGNORE_ERROR : RunStatusEnum.FAIL;
                finishParallelBatch(stepInstance, parallelRollingConfig, batchStatus);
                return;
            }
        }

        if (stepInstance.isIgnoreError()) {
            log.info("Ignore error for step: {}", stepInstanceId);
            stepInstanceService.updateStepExecutionInfo(
                taskInstanceId,
                stepInstanceId,
                RunStatusEnum.IGNORE_ERROR,
                startTime,
                endTime,
                totalTime
            );
            if (stepInstance.isRollingStep()) {
                finishRollingTask(
                    taskInstanceId,
                    stepInstanceId,
                    stepInstance.getExecuteCount(),
                    stepInstance.getBatch(),
                    RunStatusEnum.IGNORE_ERROR
                );
            }
            return;
        }

        if (stepInstance.isRollingStep()) {
            RollingConfigDTO rollingConfig = rollingConfigService.getRollingConfig(
                stepInstance.getTaskInstanceId(),
                stepInstance.getRollingConfigId()
            );
            RollingModeEnum rollingMode = rollingConfig.getModeOfStep(stepInstanceId);
            switch (rollingMode) {
                case IGNORE_ERROR:
                    log.info("Ignore error for rolling step, rollingMode: {}", rollingMode);
                    finishRollingTask(
                        taskInstanceId,
                        stepInstanceId,
                        stepInstance.getExecuteCount(),
                        stepInstance.getBatch(),
                        RunStatusEnum.IGNORE_ERROR
                    );
                    stepInstanceService.updateStepExecutionInfo(
                        taskInstanceId,
                        stepInstanceId,
                        RunStatusEnum.IGNORE_ERROR,
                        startTime,
                        endTime,
                        totalTime
                    );
                    break;
                case PAUSE_IF_FAIL:
                case MANUAL:
                    finishRollingTask(
                        taskInstanceId,
                        stepInstanceId,
                        stepInstance.getExecuteCount(),
                        stepInstance.getBatch(),
                        RunStatusEnum.FAIL
                    );
                    stepInstanceService.updateStepExecutionInfo(
                        taskInstanceId,
                        stepInstanceId,
                        RunStatusEnum.FAIL,
                        startTime,
                        endTime,
                        totalTime
                    );
                    break;
                default:
                    log.error("Invalid rolling mode: {}", rollingMode);
            }
        } else {
            stepInstanceService.updateStepExecutionInfo(
                taskInstanceId,
                stepInstanceId,
                RunStatusEnum.FAIL,
                startTime,
                endTime,
                totalTime
            );
        }
    }

    private void onStopSuccess(StepInstanceDTO stepInstance) {
        long taskInstanceId = stepInstance.getTaskInstanceId();
        long stepInstanceId = stepInstance.getId();
        long endTime = System.currentTimeMillis();
        long startTime = stepInstance.getStartTime();
        long totalTime = endTime - startTime;
        RunStatusEnum stepStatus = stepInstance.getStatus();

        // 并行错峰模式：单批次停止成功，走并发安全的完成判定
        if (stepInstance.isRollingStep()) {
            RollingConfigDTO rollingConfig = rollingConfigService.getRollingConfig(
                taskInstanceId, stepInstance.getRollingConfigId());
            if (rollingConfig != null && rollingConfig.isParallelExecution()) {
                finishParallelBatch(stepInstance, rollingConfig, RunStatusEnum.STOP_SUCCESS);
                return;
            }
        }

        if (stepStatus == RunStatusEnum.STOPPING || stepStatus == RunStatusEnum.RUNNING) {
            stepInstanceService.updateStepExecutionInfo(taskInstanceId, stepInstanceId, RunStatusEnum.STOP_SUCCESS,
                startTime, endTime, totalTime);
            if (stepInstance.isRollingStep()) {
                finishRollingTask(taskInstanceId, stepInstanceId, stepInstance.getExecuteCount(),
                    stepInstance.getBatch(), RunStatusEnum.STOP_SUCCESS);
            }
        } else {
            log.error("Refresh step fail, stepInstanceId: {}, stepStatus: {}, gseTaskStatus: {}",
                stepInstanceId, stepStatus, RunStatusEnum.STOP_SUCCESS.getValue());
        }
    }

    protected void finishStepWithAbnormalState(StepInstanceDTO stepInstance) {
        super.finishStepWithAbnormalState(stepInstance);
        if (stepInstance.isRollingStep()) {
            finishRollingTask(stepInstance.getTaskInstanceId(), stepInstance.getId(), stepInstance.getExecuteCount(),
                stepInstance.getBatch(), RunStatusEnum.ABNORMAL_STATE);
        }
    }

    protected void onAbandonState(StepInstanceDTO stepInstance) {
        finishStep(stepInstance, RunStatusEnum.ABANDONED);
        if (stepInstance.isRollingStep()) {
            finishRollingTask(stepInstance.getTaskInstanceId(), stepInstance.getId(), stepInstance.getExecuteCount(),
                stepInstance.getBatch(), RunStatusEnum.ABANDONED);
        }
    }

    /**
     * 并行错峰模式：某批次到达终态时的处理。
     * <p>
     * 通过 DAO 层锚点行锁 + 幂等闸门 + 终态计数，实现并发安全的“单批次终态跃迁 + 步骤完成判定”，
     * 只有最后一个到达终态的批次唯一收敛整个步骤（按各批聚合，任一批失败=步骤失败）。
     *
     * @param stepInstance  步骤实例
     * @param rollingConfig 滚动配置
     * @param batchStatus   当前批次终态
     */
    private void finishParallelBatch(StepInstanceDTO stepInstance,
                                     RollingConfigDTO rollingConfig,
                                     RunStatusEnum batchStatus) {
        long taskInstanceId = stepInstance.getTaskInstanceId();
        long stepInstanceId = stepInstance.getId();
        int executeCount = stepInstance.getExecuteCount();
        int batch = stepInstance.getBatch();
        int totalBatch = rollingConfig.getExecuteObjectRollingConfig().getTotalBatch();

        StepInstanceRollingTaskDTO rollingTask = stepInstanceRollingTaskService.queryRollingTask(
            taskInstanceId, stepInstanceId, executeCount, batch);
        long now = System.currentTimeMillis();
        long batchStartTime = (rollingTask != null && rollingTask.getStartTime() != null)
            ? rollingTask.getStartTime() : now;

        // GSE 回调路径：由 refreshStep 末尾统一发送 refreshJob，故此处传 dispatchRefreshJob=false 以免重复发送
        scatterStepConverger.finishBatchAndConverge(
            stepInstance, executeCount, batch, batchStatus, batchStartTime, totalBatch, false);
    }

    private void finishRollingTask(Long taskInstanceId,
                                   long stepInstanceId,
                                   int executeCount,
                                   int batch,
                                   RunStatusEnum status) {
        StepInstanceRollingTaskDTO rollingTask =
            stepInstanceRollingTaskService.queryRollingTask(taskInstanceId, stepInstanceId, executeCount, batch);
        if (rollingTask == null) {
            log.error("Rolling task is not exist, skip update! stepInstanceId: {}, executeCount: {}, batch: {}",
                stepInstanceId, executeCount, batch);
            return;
        }
        long now = System.currentTimeMillis();
        long startTime = rollingTask.getStartTime() != null ? rollingTask.getStartTime() : now;

        stepInstanceRollingTaskService.updateRollingTask(taskInstanceId, stepInstanceId, executeCount,
            batch, status, startTime, now, now - startTime);
    }
}
