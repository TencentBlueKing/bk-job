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

package com.tencent.bk.job.execute.engine.listener;

import com.tencent.bk.job.common.constant.RollingModeEnum;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.util.TaskCostCalculator;
import com.tencent.bk.job.execute.engine.consts.IpStatus;
import com.tencent.bk.job.execute.engine.consts.StepActionEnum;
import com.tencent.bk.job.execute.engine.listener.event.EventSource;
import com.tencent.bk.job.execute.engine.listener.event.GseTaskEvent;
import com.tencent.bk.job.execute.engine.listener.event.JobEvent;
import com.tencent.bk.job.execute.engine.listener.event.StepEvent;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.prepare.FilePrepareService;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepInstanceRollingTaskDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceRollingConfigDTO;
import com.tencent.bk.job.execute.model.db.RollingServerBatchDO;
import com.tencent.bk.job.execute.service.AgentTaskService;
import com.tencent.bk.job.execute.service.GseTaskService;
import com.tencent.bk.job.execute.service.RollingConfigService;
import com.tencent.bk.job.execute.service.StepInstanceRollingTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GSE 任务(脚本执行/文件分发)步骤事件处理
 */
@Component
@Slf4j
public class GseStepEventHandler implements StepEventHandler {

    private final TaskInstanceService taskInstanceService;
    private final StepInstanceService stepInstanceService;
    private final TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;
    private final FilePrepareService filePrepareService;
    private final GseTaskService gseTaskService;
    private final RollingConfigService rollingConfigService;
    private final StepInstanceRollingTaskService stepInstanceRollingTaskService;
    private final AgentTaskService agentTaskService;

    @Autowired
    public GseStepEventHandler(TaskInstanceService taskInstanceService,
                               StepInstanceService stepInstanceService,
                               TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
                               FilePrepareService filePrepareService,
                               GseTaskService gseTaskService,
                               RollingConfigService rollingConfigService,
                               StepInstanceRollingTaskService stepInstanceRollingTaskService,
                               AgentTaskService agentTaskService) {
        this.taskInstanceService = taskInstanceService;
        this.stepInstanceService = stepInstanceService;
        this.taskExecuteMQEventDispatcher = taskExecuteMQEventDispatcher;
        this.filePrepareService = filePrepareService;
        this.gseTaskService = gseTaskService;
        this.rollingConfigService = rollingConfigService;
        this.stepInstanceRollingTaskService = stepInstanceRollingTaskService;
        this.agentTaskService = agentTaskService;
    }

    @Override
    public void handleEvent(StepEvent stepEvent,
                            StepInstanceDTO stepInstance) {
        long stepInstanceId = stepEvent.getStepInstanceId();
        try {
            StepActionEnum action = StepActionEnum.valueOf(stepEvent.getAction());
            if (action == null) {
                log.error("Invalid step action: {}, Ignore step event!", stepEvent.getAction());
                return;
            }

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
                case CLEAR:
                    clearStep(stepInstance);
                    break;
                default:
                    log.error("Unhandled step event: {}", stepEvent);
            }
        } catch (Throwable e) {
            String errorMsg = "Handling step event error,stepInstanceId:" + stepInstanceId;
            log.error(errorMsg, e);
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

        int stepStatus = stepInstance.getStatus();
        // 只有当步骤状态为“未执行”、“滚动等待”, "等待用户"时可以启动步骤
        if (RunStatusEnum.BLANK.getValue() == stepStatus
            || RunStatusEnum.ROLLING_WAITING.getValue() == stepStatus
            || RunStatusEnum.WAITING_USER.getValue() == stepStatus) {

            TaskInstanceRollingConfigDTO rollingConfig = null;
            if (isRollingStep) {
                rollingConfig = rollingConfigService.getRollingConfig(stepInstance.getRollingConfigId());
                log.info("Rolling config: {}", rollingConfig);
                // 更新滚动进度
                stepInstanceService.updateStepCurrentBatch(stepInstanceId, stepInstance.getBatch());
                // 初始化步骤滚动任务
                saveInitialStepInstanceRollingTask(stepInstance);
            }

            long gseTaskId = saveInitialGseTask(stepInstance);
            saveInitialGseAgentTasks(gseTaskId, stepInstance, rollingConfig);

            taskInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.RUNNING,
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
    private void startGseTask(StepInstanceDTO stepInstance, long gseTaskId) {
        if (stepInstance.isScriptStep()) {
            taskExecuteMQEventDispatcher.dispatchGseTaskEvent(GseTaskEvent.startGseTask(gseTaskId, null));
        } else if (stepInstance.isFileStep()) {
            // 如果不是滚动步骤或者是第一批次滚动执行，那么需要为后续的分发阶段准备本地/第三方源文件
            if (!stepInstance.isRollingStep() || stepInstance.isFirstRollingBatch()) {
                if (filePrepareService.needToPrepareSourceFilesForGseTask(stepInstance)) {
                    filePrepareService.prepareFileForGseTask(stepInstance);
                } else {
                    taskExecuteMQEventDispatcher.dispatchGseTaskEvent(GseTaskEvent.startGseTask(gseTaskId, null));
                }
            } else {
                taskExecuteMQEventDispatcher.dispatchGseTaskEvent(GseTaskEvent.startGseTask(gseTaskId, null));
            }
        }
    }

    /**
     * 初始化的GSE任务
     *
     * @param stepInstance 步骤实例
     * @return GSE 任务ID
     */
    private long saveInitialGseTask(StepInstanceDTO stepInstance) {
        GseTaskDTO gseTask = new GseTaskDTO(stepInstance.getId(), stepInstance.getExecuteCount(),
            stepInstance.getBatch());
        gseTask.setStatus(RunStatusEnum.WAITING_USER.getValue());

        return gseTaskService.saveGseTask(gseTask);
    }

    /**
     * 初始化的GSE Agent 任务
     *
     * @param gseTaskId     GSE任务ID
     * @param stepInstance  步骤实例
     * @param rollingConfig 滚动配置
     */
    private void saveInitialGseAgentTasks(long gseTaskId,
                                          StepInstanceDTO stepInstance,
                                          TaskInstanceRollingConfigDTO rollingConfig) {
        List<AgentTaskDTO> agentTasks = new ArrayList<>();

        long stepInstanceId = stepInstance.getId();
        int executeCount = stepInstance.getExecuteCount();
        int batch = stepInstance.getBatch();

        if (stepInstance.isRollingStep()) {
            if (rollingConfig.isBatchRollingStep(stepInstanceId) && stepInstance.isFirstRollingBatch()) {
                List<RollingServerBatchDO> serverBatchList = rollingConfig.getConfig().getServerBatchList();
                serverBatchList.forEach(serverBatch -> agentTasks.addAll(buildGseAgentTasks(stepInstanceId,
                    executeCount, serverBatch.getBatch(), gseTaskId, serverBatch.getServers(), IpStatus.WAITING)));
            } else if (rollingConfig.isAllRollingStep(stepInstanceId)) {
                agentTasks.addAll(buildGseAgentTasks(stepInstanceId, executeCount, batch,
                    gseTaskId, stepInstance.getTargetServers().getIpList(), IpStatus.WAITING));
            }
        } else {
            agentTasks.addAll(buildGseAgentTasks(stepInstanceId, executeCount, batch,
                gseTaskId, stepInstance.getTargetServers().getIpList(), IpStatus.WAITING));
        }

        // 无效主机
        if (CollectionUtils.isNotEmpty(stepInstance.getTargetServers().getInvalidIpList())) {
            agentTasks.addAll(buildGseAgentTasks(stepInstanceId, executeCount, batch,
                0, stepInstance.getTargetServers().getInvalidIpList(), IpStatus.HOST_NOT_EXIST));
        }
        agentTaskService.batchSaveAgentTasks(agentTasks);
    }

    private List<AgentTaskDTO> buildGseAgentTasks(long stepInstanceId,
                                                  int executeCount,
                                                  int batch,
                                                  long gseTaskId,
                                                  List<IpDTO> servers,
                                                  IpStatus status) {
        return servers.stream()
            .map(server -> buildGseAgentTask(stepInstanceId, executeCount, batch, gseTaskId, server, status))
            .collect(Collectors.toList());
    }

    protected AgentTaskDTO buildGseAgentTask(long stepInstanceId,
                                             int executeCount,
                                             int batch,
                                             long gseTaskId,
                                             IpDTO server,
                                             IpStatus status) {
        AgentTaskDTO agentTask = new AgentTaskDTO();
        agentTask.setStepInstanceId(stepInstanceId);
        agentTask.setExecuteCount(executeCount);
        agentTask.setBatch(batch);
        agentTask.setGseTaskId(gseTaskId);
        agentTask.setStatus(status.getValue());
        agentTask.setTargetServer(true);
        agentTask.setIp(server.getIp());
        agentTask.setCloudIp(server.convertToStrIp());
        agentTask.setCloudId(server.getCloudAreaId());
        agentTask.setDisplayIp(server.getIp());
        agentTask.setSourceServer(false);
        return agentTask;
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
        stepInstanceRollingTask.setStatus(RunStatusEnum.RUNNING.getValue());
        stepInstanceRollingTask.setStartTime(System.currentTimeMillis());
        stepInstanceRollingTaskService.saveRollingTask(stepInstanceRollingTask);
    }

    private void nextStep(StepInstanceDTO stepInstance) {
        log.info("Next step, stepInstanceId={}", stepInstance.getId());

        long taskInstanceId = stepInstance.getTaskInstanceId();
        long stepInstanceId = stepInstance.getId();
        int stepStatus = stepInstance.getStatus();

        if (RunStatusEnum.STOP_SUCCESS.getValue() == stepStatus) {
            taskInstanceService.updateTaskStatus(taskInstanceId, RunStatusEnum.RUNNING.getValue());
            long endTime = DateUtils.currentTimeMillis();
            long totalTime = TaskCostCalculator.calculate(stepInstance.getStartTime(), endTime,
                stepInstance.getTotalTime());
            // 终止成功，进入下一步，该步骤设置为“跳过”
            taskInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.SKIPPED, null, endTime,
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

        if (!stepInstance.getStatus().equals(RunStatusEnum.FAIL.getValue())) {
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

        taskInstanceService.updateStepStatus(stepInstance.getId(), RunStatusEnum.IGNORE_ERROR.getValue());
        taskInstanceService.resetTaskExecuteInfoForRetry(stepInstance.getTaskInstanceId());
        taskExecuteMQEventDispatcher.dispatchJobEvent(
            JobEvent.refreshJob(stepInstance.getTaskInstanceId(),
                EventSource.buildStepEventSource(stepInstance.getId())));
    }


    private void skipStep(StepInstanceDTO stepInstance) {
        int stepStatus = stepInstance.getStatus();
        long stepInstanceId = stepInstance.getId();
        long taskInstanceId = stepInstance.getTaskInstanceId();

        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(stepInstance.getTaskInstanceId());
        if (!(taskInstance.getCurrentStepInstanceId() == stepInstanceId)) {
            log.warn("Only current running step is support for skipping, stepInstanceId={}", stepInstanceId);
            return;
        }

        log.info("Skip step, stepInstanceId={}", stepInstanceId);

        // 只有当步骤状态为'终止中'时可以跳过步骤
        if (RunStatusEnum.STOPPING.getValue() == stepStatus) {
            long now = DateUtils.currentTimeMillis();
            taskInstanceService.updateStepStartTimeIfNull(stepInstanceId, now);
            taskInstanceService.updateStepStatus(stepInstanceId, RunStatusEnum.SKIPPED.getValue());
            taskInstanceService.updateStepEndTime(stepInstanceId, now);

            taskInstanceService.updateTaskStatus(taskInstanceId, RunStatusEnum.RUNNING.getValue());
            taskExecuteMQEventDispatcher.dispatchJobEvent(
                JobEvent.refreshJob(taskInstanceId, EventSource.buildStepEventSource(stepInstanceId)));
        } else {
            log.warn("Unsupported step instance run status for skipping step, stepInstanceId={}, status={}",
                stepInstanceId, stepStatus);
        }
    }

    private void stopStep(StepInstanceDTO stepInstance) {
        log.info("Force stop step, stepInstanceId={}", stepInstance.getId());

        long taskInstanceId = stepInstance.getTaskInstanceId();

        taskInstanceService.updateTaskStatus(taskInstanceId, RunStatusEnum.STOPPING.getValue());
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
        taskExecuteMQEventDispatcher.dispatchGseTaskEvent(GseTaskEvent.startGseTask(gseTask.getId(), null));
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

        RunStatusEnum stepStatus = RunStatusEnum.valueOf(stepInstance.getStatus());
        if (isStepSupportRetry(stepStatus)) {

            resetExecutionInfoForRetry(stepInstance);

            if (isRollingStep) {
                // 初始化步骤滚动任务
                saveInitialStepInstanceRollingTask(stepInstance);
            }

            long gseTaskId = saveInitialGseTask(stepInstance);
            saveAgentTasksForRetryFail(stepInstanceId, stepInstance.getExecuteCount(), stepInstance.getBatch(),
                gseTaskId);

            if (stepInstance.isScriptStep()) {
                taskExecuteMQEventDispatcher.dispatchGseTaskEvent(GseTaskEvent.startGseTask(gseTaskId, null));
            } else if (stepInstance.isFileStep()) {
                // 如果不是滚动步骤或者是第一批次滚动执行，那么需要为后续的分发阶段准备本地/第三方源文件
                if (!stepInstance.isRollingStep() || stepInstance.isFirstRollingBatch()) {
                    filePrepareService.prepareFileForGseTask(stepInstance);
                }
            }
        } else {
            log.warn("Unsupported step instance run status for retry step, stepInstanceId={}, status={}",
                stepInstanceId, stepStatus);
        }
    }

    private boolean isStepSupportRetry(RunStatusEnum stepStatus) {
        return RunStatusEnum.FAIL == stepStatus || RunStatusEnum.ABNORMAL_STATE == stepStatus;
    }

    private void saveAgentTasksForRetryFail(long stepInstanceId, int executeCount, Integer batch,
                                            long gseTaskId) {
        List<AgentTaskDTO> latestAgentTasks =
            agentTaskService.listAgentTasks(stepInstanceId, executeCount - 1, null, false);
        for (AgentTaskDTO latestAgentTask : latestAgentTasks) {
            latestAgentTask.setExecuteCount(executeCount);
            if (!IpStatus.isSuccess(latestAgentTask.getStatus())) {
                if (batch != null && latestAgentTask.getBatch() != batch) {
                    continue;
                }
                latestAgentTask.resetTaskInitialStatus();
                latestAgentTask.setGseTaskId(gseTaskId);
            }
        }
        agentTaskService.batchSaveAgentTasks(latestAgentTasks);
    }


    private void saveAgentTasksForRetryAll(long stepInstanceId, int executeCount, Integer batch,
                                           long gseTaskId) {
        List<AgentTaskDTO> latestAgentTasks =
            agentTaskService.listAgentTasks(stepInstanceId, executeCount - 1, null, false);
        for (AgentTaskDTO latestAgentTask : latestAgentTasks) {
            latestAgentTask.setExecuteCount(executeCount);
            if (batch != null && latestAgentTask.getBatch() != batch) {
                continue;
            }
            latestAgentTask.resetTaskInitialStatus();
            latestAgentTask.setGseTaskId(gseTaskId);
        }
        agentTaskService.batchSaveAgentTasks(latestAgentTasks);
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

        RunStatusEnum stepStatus = RunStatusEnum.valueOf(stepInstance.getStatus());
        if (isStepSupportRetry(stepStatus)) {

            resetExecutionInfoForRetry(stepInstance);

            if (isRollingStep) {
                // 初始化步骤滚动任务
                saveInitialStepInstanceRollingTask(stepInstance);
            }

            long gseTaskId = saveInitialGseTask(stepInstance);
            saveAgentTasksForRetryAll(stepInstanceId, stepInstance.getExecuteCount(), stepInstance.getBatch(),
                gseTaskId);

            if (stepInstance.isScriptStep()) {
                taskExecuteMQEventDispatcher.dispatchGseTaskEvent(GseTaskEvent.startGseTask(gseTaskId, null));
            } else if (stepInstance.isFileStep()) {
                // 如果不是滚动步骤或者是第一批次滚动执行，那么需要为后续的分发阶段准备本地/第三方源文件
                if (!stepInstance.isRollingStep() || stepInstance.isFirstRollingBatch()) {
                    filePrepareService.prepareFileForGseTask(stepInstance);
                }
            }
        } else {
            log.warn("Unsupported step instance run status for retry step, stepInstanceId={}, status={}",
                stepInstanceId, stepStatus);
        }
    }

    /**
     * 清理执行完的步骤
     */
    private void clearStep(StepInstanceDTO stepInstance) {
        log.info("Clear step, stepInstanceId={}", stepInstance.getId());

        int executeType = stepInstance.getExecuteType();
        // 当前仅有文件分发类步骤需要清理中间文件
        if (TaskStepTypeEnum.FILE.getValue() == executeType) {
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

        taskInstanceService.resetStepExecuteInfoForRetry(stepInstanceId);
        taskInstanceService.resetTaskExecuteInfoForRetry(taskInstanceId);
    }

    private void refreshStep(StepEvent stepEvent, StepInstanceDTO stepInstance) {

        long stepInstanceId = stepInstance.getId();
        long gseTaskId = stepEvent.getSource().getGseTaskId();

        GseTaskDTO gseTask = gseTaskService.getGseTask(gseTaskId);
        RunStatusEnum gseTaskStatus = RunStatusEnum.valueOf(gseTask.getStatus());
        if (gseTaskStatus == null) {
            log.error("Refresh step fail, undefined gse task status, stepInstanceId: {}, gseTaskStatus: {}",
                stepInstance.getId(), gseTask.getStatus());
            return;
        }
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
                onAbnormalState(stepInstance);
                break;
            default:
                log.error("Refresh step fail because of unexpected gse task status. stepInstanceId: {}, " +
                        "gseTaskStatus: {}",
                    stepInstanceId, gseTaskStatus.getValue());
                setAbnormalStatusForStep(stepInstance);
                break;
        }

        // 更新作业状态
        taskExecuteMQEventDispatcher.dispatchJobEvent(
            JobEvent.refreshJob(stepInstance.getTaskInstanceId(), EventSource.buildStepEventSource(stepInstanceId)));
    }

    private void onSuccess(StepInstanceDTO stepInstance) {
        log.info("On success");
        long stepInstanceId = stepInstance.getId();
        long endTime = System.currentTimeMillis();
        long startTime = stepInstance.getStartTime();
        long totalTime = endTime - startTime;

        if (stepInstance.isRollingStep()) {
            log.info("rolling step");
            TaskInstanceRollingConfigDTO rollingConfig =
                rollingConfigService.getRollingConfig(stepInstance.getRollingConfigId());
            stepInstanceRollingTaskService.updateRollingTask(stepInstanceId, stepInstance.getExecuteCount(),
                stepInstance.getBatch(), RunStatusEnum.SUCCESS, startTime, endTime, totalTime);
            int totalBatch = rollingConfig.getConfig().getTotalBatch();
            log.info("rollingConfig: {}, stepInstance: {}", rollingConfig, stepInstance);
            boolean isLastBatch = totalBatch == stepInstance.getBatch();
            if (isLastBatch) {
                log.info("updateStepExecutionInfo-SUCCESS");
                taskInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.SUCCESS,
                    startTime, endTime, totalTime);
                // 步骤执行成功后清理产生的临时文件
                clearStep(stepInstance);
            } else {
                taskInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.ROLLING_WAITING,
                    startTime, endTime, totalTime);
                log.info("updateStepExecutionInfo-ROLLING_WAITING");
            }
        } else {
            log.info("not rolling step");
            taskInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.SUCCESS,
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
            taskInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.IGNORE_ERROR,
                startTime, endTime, totalTime);
            if (stepInstance.isRollingStep()) {
                stepInstanceRollingTaskService.updateRollingTask(stepInstanceId, stepInstance.getExecuteCount(),
                    stepInstance.getBatch(), RunStatusEnum.IGNORE_ERROR, startTime, endTime, totalTime);
            }
            return;
        }

        if (stepInstance.isRollingStep()) {
            TaskInstanceRollingConfigDTO rollingConfig =
                rollingConfigService.getRollingConfig(stepInstance.getRollingConfigId());
            RollingModeEnum rollingMode = RollingModeEnum.valOf(rollingConfig.getConfig().getMode());
            switch (rollingMode) {
                case IGNORE_ERROR:
                    log.info("Ignore error for rolling step, rollingMode: {}", rollingMode);
                    stepInstanceRollingTaskService.updateRollingTask(stepInstanceId, stepInstance.getExecuteCount(),
                        stepInstance.getBatch(), RunStatusEnum.IGNORE_ERROR, startTime, endTime, totalTime);
                    taskInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.IGNORE_ERROR,
                        startTime, endTime, totalTime);
                    break;
                case PAUSE_IF_FAIL:
                case MANUAL:
                    stepInstanceRollingTaskService.updateRollingTask(stepInstanceId, stepInstance.getExecuteCount(),
                        stepInstance.getBatch(), RunStatusEnum.FAIL, startTime, endTime, totalTime);
                    taskInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.FAIL,
                        startTime, endTime, totalTime);
                    break;
                default:
                    log.error("Invalid rolling mode: {}", rollingMode);
            }
        } else {
            taskInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.FAIL,
                startTime, endTime, totalTime);
        }
    }

    private void onStopSuccess(StepInstanceDTO stepInstance) {
        long stepInstanceId = stepInstance.getId();
        long endTime = System.currentTimeMillis();
        long startTime = stepInstance.getStartTime();
        long totalTime = endTime - startTime;
        int stepStatus = stepInstance.getStatus();

        if (stepStatus == RunStatusEnum.STOPPING.getValue() || stepStatus == RunStatusEnum.RUNNING.getValue()) {
            taskInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.STOP_SUCCESS,
                startTime, endTime, totalTime);
            if (stepInstance.isRollingStep()) {
                stepInstanceRollingTaskService.updateRollingTask(stepInstanceId, stepInstance.getExecuteCount(),
                    stepInstance.getBatch(), RunStatusEnum.STOP_SUCCESS, startTime, endTime, totalTime);
            }
        } else {
            log.error("Refresh step fail, stepInstanceId: {}, stepStatus: {}, gseTaskStatus: {}",
                stepInstanceId, stepStatus, RunStatusEnum.STOP_SUCCESS.getValue());
        }
    }

    private void onAbnormalState(StepInstanceDTO stepInstance) {
        long stepInstanceId = stepInstance.getId();
        long endTime = System.currentTimeMillis();
        long startTime = stepInstance.getStartTime();
        long totalTime = endTime - startTime;
        setAbnormalStatusForStep(stepInstance);
        if (stepInstance.isRollingStep()) {
            stepInstanceRollingTaskService.updateRollingTask(stepInstanceId, stepInstance.getExecuteCount(),
                stepInstance.getBatch(), RunStatusEnum.ABNORMAL_STATE, startTime, endTime, totalTime);
        }
    }

    private void setAbnormalStatusForStep(StepInstanceDTO stepInstance) {
        long endTime = System.currentTimeMillis();
        if (!RunStatusEnum.getFinishedStatusValueList().contains(stepInstance.getStatus())) {
            long totalTime = TaskCostCalculator.calculate(stepInstance.getStartTime(), endTime,
                stepInstance.getTotalTime());
            taskInstanceService.updateStepExecutionInfo(
                stepInstance.getId(),
                RunStatusEnum.ABNORMAL_STATE,
                null,
                endTime,
                totalTime
            );
        } else {
            log.info(
                "StepInstance {} already enter a final state:{}",
                stepInstance.getId(),
                stepInstance.getStatus()
            );
        }
    }
}
