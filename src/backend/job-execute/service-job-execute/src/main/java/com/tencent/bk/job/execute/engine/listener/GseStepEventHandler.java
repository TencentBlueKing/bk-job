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
import com.tencent.bk.job.execute.engine.listener.event.StepEvent;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.engine.prepare.FilePrepareService;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.RollingConfigDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepInstanceRollingTaskDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.db.RollingExecuteObjectsBatchDO;
import com.tencent.bk.job.execute.service.FileExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.GseTaskService;
import com.tencent.bk.job.execute.service.RollingConfigService;
import com.tencent.bk.job.execute.service.ScriptExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.StepInstanceRollingTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
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

    @Autowired
    public GseStepEventHandler(TaskInstanceService taskInstanceService,
                               StepInstanceService stepInstanceService,
                               TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
                               FilePrepareService filePrepareService,
                               GseTaskService gseTaskService,
                               RollingConfigService rollingConfigService,
                               StepInstanceRollingTaskService stepInstanceRollingTaskService,
                               ScriptExecuteObjectTaskService scriptExecuteObjectTaskService,
                               FileExecuteObjectTaskService fileExecuteObjectTaskService) {
        super(taskInstanceService, stepInstanceService, taskExecuteMQEventDispatcher);
        this.filePrepareService = filePrepareService;
        this.gseTaskService = gseTaskService;
        this.rollingConfigService = rollingConfigService;
        this.stepInstanceRollingTaskService = stepInstanceRollingTaskService;
        this.scriptExecuteObjectTaskService = scriptExecuteObjectTaskService;
        this.fileExecuteObjectTaskService = fileExecuteObjectTaskService;
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
                rollingConfig = rollingConfigService.getRollingConfig(stepInstance.getRollingConfigId());
                log.info("Rolling config: {}", rollingConfig);
                // 更新滚动进度
                stepInstanceService.updateStepCurrentBatch(stepInstanceId, stepInstance.getBatch());
                // 初始化步骤滚动任务
                saveInitialStepInstanceRollingTask(stepInstance);
            }

            Long gseTaskId = saveInitialGseTask(stepInstance);
            saveExecuteObjectTasksForStartStep(gseTaskId, stepInstance, rollingConfig);

            stepInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.RUNNING,
                stepInstance.getStartTime() == null ? DateUtils.currentTimeMillis() : null, null, null);
            if (isRollingStep) {
                stepInstanceRollingTaskService.updateRollingTask(stepInstanceId, stepInstance.getExecuteCount(),
                    stepInstance.getBatch(), RunStatusEnum.RUNNING, System.currentTimeMillis(), null, null);
            }

            startGseTask(stepInstance, gseTaskId);
        } else {
            log.warn("Unsupported step instance run status for starting step, stepInstanceId={}, status={}",
                stepInstanceId, stepStatus);
        }
    }

    /**
     * 启动GSE任务
     *
     * @param stepInstance 步骤实例
     * @param gseTaskId    Gse任务ID
     */
    private void startGseTask(StepInstanceDTO stepInstance, Long gseTaskId) {
        if (stepInstance.isScriptStep()) {
            taskExecuteMQEventDispatcher.dispatchGseTaskEvent(GseTaskEvent.startGseTask(
                stepInstance.getId(), stepInstance.getExecuteCount(), stepInstance.getBatch(), gseTaskId, null));
        } else if (stepInstance.isFileStep()) {
            if (filePrepareService.needToPrepareSourceFilesForGseTask(stepInstance)) {
                filePrepareService.prepareFileForGseTask(stepInstance);
            } else {
                taskExecuteMQEventDispatcher.dispatchGseTaskEvent(GseTaskEvent.startGseTask(
                    stepInstance.getId(), stepInstance.getExecuteCount(), stepInstance.getBatch(), gseTaskId,
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
        GseTaskDTO gseTask = new GseTaskDTO(stepInstance.getId(),
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
                buildInitialExecuteObjectTasks(stepInstance.getTaskInstanceId(), stepInstanceId, executeCount,
                    executeCount, batch, gseTaskId,
                    stepInstance.getTargetExecuteObjects().getExecuteObjectsCompatibly()));
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
            if (rollingConfig.isBatchRollingStep(stepInstanceId)) {
                List<RollingExecuteObjectsBatchDO> executeObjectsBatchList =
                    rollingConfig.getConfigDetail().getExecuteObjectsBatchListCompatibly();
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
                executeObjectTask.setStatus(ExecuteObjectTaskStatusEnum.WAITING);
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
            stepInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.SKIPPED, null, endTime,
                totalTime);
            taskExecuteMQEventDispatcher.dispatchJobEvent(
                JobEvent.refreshJob(taskInstanceId, EventSource.buildStepEventSource(stepInstanceId)));
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

        long stepInstanceId = stepInstance.getId();
        boolean isRollingStep = stepInstance.isRollingStep();
        if (isRollingStep) {
            log.info("Retry-fail for rolling step, stepInstanceId={}, batch: {}", stepInstanceId,
                stepInstance.getBatch());
        } else {
            log.info("Retry-fail for step, stepInstanceId={}", stepInstanceId);
        }

        stepInstanceService.updateStepStatus(stepInstance.getId(), RunStatusEnum.IGNORE_ERROR.getValue());
        taskInstanceService.resetTaskExecuteInfoForRetry(stepInstance.getTaskInstanceId());
        if (isRollingStep) {
            StepInstanceRollingTaskDTO stepInstanceRollingTask =
                stepInstanceRollingTaskService.queryRollingTask(stepInstanceId, stepInstance.getExecuteCount(),
                    stepInstance.getBatch());
            if (stepInstanceRollingTask != null) {
                finishRollingTask(stepInstanceId, stepInstance.getExecuteCount(), stepInstance.getBatch(),
                    RunStatusEnum.IGNORE_ERROR);
            }
        }

        taskExecuteMQEventDispatcher.dispatchJobEvent(
            JobEvent.refreshJob(stepInstance.getTaskInstanceId(),
                EventSource.buildStepEventSource(stepInstance.getId())));
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
            stepInstanceService.updateStepStartTimeIfNull(stepInstanceId, now);
            stepInstanceService.updateStepStatus(stepInstanceId, RunStatusEnum.SKIPPED.getValue());
            stepInstanceService.updateStepEndTime(stepInstanceId, now);

            taskInstanceService.updateTaskStatus(taskInstanceId, RunStatusEnum.RUNNING.getValue());
            taskExecuteMQEventDispatcher.dispatchJobEvent(
                JobEvent.refreshJob(taskInstanceId, EventSource.buildStepEventSource(stepInstanceId)));
        } else {
            log.warn("Unsupported step instance run status for skipping step, stepInstanceId={}, status={}",
                stepInstanceId, stepStatus);
        }
    }

    private void stopStep(StepInstanceDTO stepInstance) {
        long stepInstanceId = stepInstance.getId();
        log.info("Force stop step, stepInstanceId={}", stepInstanceId);

        RunStatusEnum stepStatus = stepInstance.getStatus();
        if (stepStatus == RunStatusEnum.WAITING_USER) {
            log.info("Step status is WAITING_USER, set step status stop_success directly!");
            // 等待用户的步骤可以直接结束
            long endTime = DateUtils.currentTimeMillis();
            stepInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.STOP_SUCCESS,
                null, endTime, TaskCostCalculator.calculate(stepInstance.getStartTime(), endTime,
                    stepInstance.getTotalTime()));
            taskExecuteMQEventDispatcher.dispatchJobEvent(JobEvent.refreshJob(stepInstance.getTaskInstanceId(),
                EventSource.buildStepEventSource(stepInstanceId)));
        } else if (stepStatus == RunStatusEnum.RUNNING) {
            // 正在运行中的任务无法立即结束，需要等待任务调度引擎检测到停止状态;这里只需要处理设置步骤状态即可
            stepInstanceService.updateStepStatus(stepInstanceId, RunStatusEnum.STOPPING.getValue());
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
            gseTaskService.getGseTask(stepInstance.getId(), stepInstance.getExecuteCount(), stepInstance.getBatch());
        taskExecuteMQEventDispatcher.dispatchGseTaskEvent(GseTaskEvent.startGseTask(
            gseTask.getStepInstanceId(), gseTask.getExecuteCount(), gseTask.getBatch(), gseTask.getId(), null));
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

    private boolean isStepSupportRetry(RunStatusEnum stepStatus) {
        return RunStatusEnum.FAIL == stepStatus || RunStatusEnum.ABNORMAL_STATE == stepStatus
            || RunStatusEnum.STOP_SUCCESS == stepStatus;
    }

    private void saveExecuteObjectTasksForRetryFail(StepInstanceBaseDTO stepInstance, int executeCount, Integer batch,
                                                    Long gseTaskId) {
        List<ExecuteObjectTask> retryExecuteObjectTasks = listTargetExecuteObjectTasks(stepInstance, executeCount - 1);

        for (ExecuteObjectTask retryExecuteObjectTask : retryExecuteObjectTasks) {
            retryExecuteObjectTask.setExecuteCount(executeCount);
            if (batch != null && retryExecuteObjectTask.getBatch() != batch) {
                continue;
            }
            // 只有失败的目标主机才需要参与重试
            if (!ExecuteObjectTaskStatusEnum.isSuccess(retryExecuteObjectTask.getStatus())) {
                retryExecuteObjectTask.setActualExecuteCount(executeCount);
                retryExecuteObjectTask.resetTaskInitialStatus();
                retryExecuteObjectTask.setGseTaskId(gseTaskId);
            }
        }

        saveExecuteObjectTasks(stepInstance, retryExecuteObjectTasks);
    }


    private void saveExecuteObjectTasksForRetryAll(StepInstanceBaseDTO stepInstance, int executeCount, Integer batch,
                                                   Long gseTaskId) {
        List<ExecuteObjectTask> retryExecuteObjectTasks = listTargetExecuteObjectTasks(stepInstance, executeCount - 1);

        for (ExecuteObjectTask retryExecuteObjectTask : retryExecuteObjectTasks) {
            retryExecuteObjectTask.setExecuteCount(executeCount);
            if (batch != null && retryExecuteObjectTask.getBatch() != batch) {
                continue;
            }
            retryExecuteObjectTask.setActualExecuteCount(executeCount);
            retryExecuteObjectTask.resetTaskInitialStatus();
            retryExecuteObjectTask.setGseTaskId(gseTaskId);
        }

        saveExecuteObjectTasks(stepInstance, retryExecuteObjectTasks);
    }

    private List<ExecuteObjectTask> listTargetExecuteObjectTasks(StepInstanceBaseDTO stepInstance, int executeCount) {
        List<ExecuteObjectTask> executeObjectTasks = Collections.emptyList();
        if (stepInstance.isScriptStep()) {
            executeObjectTasks = scriptExecuteObjectTaskService.listTasks(stepInstance, executeCount, null);
        } else if (stepInstance.isFileStep()) {
            executeObjectTasks = fileExecuteObjectTaskService.listTasks(stepInstance, executeCount, null,
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
            filePrepareService.clearPreparedTmpFile(stepInstance.getId());
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

        stepInstanceService.resetStepExecuteInfoForRetry(stepInstanceId);
        taskInstanceService.resetTaskExecuteInfoForRetry(taskInstanceId);
    }

    private void refreshStep(StepEvent stepEvent, StepInstanceDTO stepInstance) {

        long stepInstanceId = stepInstance.getId();
        EventSource eventSource = stepEvent.getSource();

        GseTaskDTO gseTask = gseTaskService.getGseTask(eventSource.getGseTaskId());

        RunStatusEnum gseTaskStatus = RunStatusEnum.valueOf(gseTask.getStatus());
        log.info("Refresh step according to gse task status, stepInstanceId: {}, gseTaskStatus: {}",
            stepInstance.getId(), gseTaskStatus.name());

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
            JobEvent.refreshJob(stepInstance.getTaskInstanceId(), EventSource.buildStepEventSource(stepInstanceId)));
    }

    private void onSuccess(StepInstanceDTO stepInstance) {
        long stepInstanceId = stepInstance.getId();
        long endTime = System.currentTimeMillis();
        long startTime = stepInstance.getStartTime();
        long totalTime = endTime - startTime;

        if (stepInstance.isRollingStep()) {
            RollingConfigDTO rollingConfig =
                rollingConfigService.getRollingConfig(stepInstance.getRollingConfigId());
            finishRollingTask(stepInstanceId, stepInstance.getExecuteCount(), stepInstance.getBatch(),
                RunStatusEnum.SUCCESS);
            int totalBatch = rollingConfig.getConfigDetail().getTotalBatch();
            boolean isLastBatch = totalBatch == stepInstance.getBatch();
            if (isLastBatch) {
                stepInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.SUCCESS,
                    startTime, endTime, totalTime);
                // 步骤执行成功后清理产生的临时文件
                clearStep(stepInstance);
            } else {
                stepInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.ROLLING_WAITING,
                    startTime, endTime, totalTime);
            }
        } else {
            stepInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.SUCCESS,
                startTime, endTime, totalTime);
            // 步骤执行成功后清理产生的临时文件
            clearStep(stepInstance);
        }
    }

    private void onFail(StepInstanceDTO stepInstance) {
        long stepInstanceId = stepInstance.getId();
        long endTime = System.currentTimeMillis();
        long startTime = stepInstance.getStartTime();
        long totalTime = endTime - startTime;
        if (stepInstance.isIgnoreError()) {
            log.info("Ignore error for step: {}", stepInstanceId);
            stepInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.IGNORE_ERROR,
                startTime, endTime, totalTime);
            if (stepInstance.isRollingStep()) {
                finishRollingTask(stepInstanceId, stepInstance.getExecuteCount(), stepInstance.getBatch(),
                    RunStatusEnum.IGNORE_ERROR);
            }
            return;
        }

        if (stepInstance.isRollingStep()) {
            RollingConfigDTO rollingConfig =
                rollingConfigService.getRollingConfig(stepInstance.getRollingConfigId());
            RollingModeEnum rollingMode = RollingModeEnum.valOf(rollingConfig.getConfigDetail().getMode());
            switch (rollingMode) {
                case IGNORE_ERROR:
                    log.info("Ignore error for rolling step, rollingMode: {}", rollingMode);
                    finishRollingTask(stepInstanceId, stepInstance.getExecuteCount(), stepInstance.getBatch(),
                        RunStatusEnum.IGNORE_ERROR);
                    stepInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.IGNORE_ERROR,
                        startTime, endTime, totalTime);
                    break;
                case PAUSE_IF_FAIL:
                case MANUAL:
                    finishRollingTask(stepInstanceId, stepInstance.getExecuteCount(), stepInstance.getBatch(),
                        RunStatusEnum.FAIL);
                    stepInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.FAIL,
                        startTime, endTime, totalTime);
                    break;
                default:
                    log.error("Invalid rolling mode: {}", rollingMode);
            }
        } else {
            stepInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.FAIL,
                startTime, endTime, totalTime);
        }
    }

    private void onStopSuccess(StepInstanceDTO stepInstance) {
        long stepInstanceId = stepInstance.getId();
        long endTime = System.currentTimeMillis();
        long startTime = stepInstance.getStartTime();
        long totalTime = endTime - startTime;
        RunStatusEnum stepStatus = stepInstance.getStatus();

        if (stepStatus == RunStatusEnum.STOPPING || stepStatus == RunStatusEnum.RUNNING) {
            stepInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.STOP_SUCCESS,
                startTime, endTime, totalTime);
            if (stepInstance.isRollingStep()) {
                finishRollingTask(stepInstanceId, stepInstance.getExecuteCount(), stepInstance.getBatch(),
                    RunStatusEnum.STOP_SUCCESS);
            }
        } else {
            log.error("Refresh step fail, stepInstanceId: {}, stepStatus: {}, gseTaskStatus: {}",
                stepInstanceId, stepStatus, RunStatusEnum.STOP_SUCCESS.getValue());
        }
    }

    protected void finishStepWithAbnormalState(StepInstanceDTO stepInstance) {
        super.finishStepWithAbnormalState(stepInstance);
        if (stepInstance.isRollingStep()) {
            finishRollingTask(stepInstance.getId(), stepInstance.getExecuteCount(), stepInstance.getBatch(),
                RunStatusEnum.ABNORMAL_STATE);
        }
    }

    private void finishRollingTask(long stepInstanceId, int executeCount, int batch, RunStatusEnum status) {
        StepInstanceRollingTaskDTO rollingTask =
            stepInstanceRollingTaskService.queryRollingTask(stepInstanceId, executeCount, batch);
        if (rollingTask == null) {
            log.error("Rolling task is not exist, skip update! stepInstanceId: {}, executeCount: {}, batch: {}",
                stepInstanceId, executeCount, batch);
            return;
        }
        long now = System.currentTimeMillis();
        long startTime = rollingTask.getStartTime() != null ? rollingTask.getStartTime() : now;

        stepInstanceRollingTaskService.updateRollingTask(stepInstanceId, executeCount,
            batch, status, startTime, now, now - startTime);
    }
}
