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

import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.util.TaskCostCalculator;
import com.tencent.bk.job.execute.engine.consts.IpStatus;
import com.tencent.bk.job.execute.engine.consts.StepActionEnum;
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
                    startStep(stepInstance);
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
                    refreshStep(stepInstance);
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

    private void startStep(StepInstanceDTO stepInstance) {
        long stepInstanceId = stepInstance.getId();
        log.info("Start step, stepInstanceId={}", stepInstanceId);

        int stepStatus = stepInstance.getStatus();
        // 只有当步骤状态为“等待用户”、“未执行”、“滚动等待”时可以启动步骤
        if (RunStatusEnum.BLANK.getValue() == stepStatus
            || RunStatusEnum.WAITING.getValue() == stepStatus
            || RunStatusEnum.ROLLING_WAITING.getValue() == stepStatus) {

            TaskInstanceRollingConfigDTO rollingConfig = null;
            if (stepInstance.hasRollingConfig()) {
                rollingConfig = rollingConfigService.getRollingConfig(stepInstance.getRollingConfigId());
            }
            saveInitialGseAgentTasks(stepInstance, rollingConfig);

            taskInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.RUNNING,
                stepInstance.getStartTime() == null ? DateUtils.currentTimeMillis() : null, null, null);

            if (stepInstance.hasRollingConfig()) {
                int currentRollingBatch = stepInstance.getBatch() + 1;
                stepInstance.setBatch(currentRollingBatch);
                // 更新滚动进度
                stepInstanceService.updateStepCurrentBatch(stepInstanceId, currentRollingBatch);
                // 初始化步骤滚动任务
                saveInitialStepInstanceRollingTask(stepInstance);
            }

            if (stepInstance.isScriptStep()) {
                taskExecuteMQEventDispatcher.startGseStep(stepInstanceId, stepInstance.hasRollingConfig() ?
                    stepInstance.getBatch() : null);
            } else if (stepInstance.isFileStep()) {
                // 如果不是滚动步骤或者是第一批次滚动执行，那么需要为后续的分发阶段准备本地/第三方源文件
                if (!stepInstance.hasRollingConfig() || stepInstance.isFirstRollingBatch()) {
                    filePrepareService.prepareFileForGseTask(stepInstanceId);
                }
            }
        } else {
            log.warn("Unsupported step instance run status for starting step, stepInstanceId={}, status={}",
                stepInstanceId, stepStatus);
        }
    }

    /**
     * 初始化的GSE Agent 任务
     *
     * @param stepInstance  步骤实例
     * @param rollingConfig 滚动配置
     */
    private void saveInitialGseAgentTasks(StepInstanceDTO stepInstance, TaskInstanceRollingConfigDTO rollingConfig) {
        List<AgentTaskDTO> agentTasks = new ArrayList<>();

        long stepInstanceId = stepInstance.getId();
        int executeCount = stepInstance.getExecuteCount();

        if (stepInstance.hasRollingConfig()) {
            if (rollingConfig.isRollingStep(stepInstanceId)) {
                List<RollingServerBatchDO> serverBatchList = rollingConfig.getConfig().getServerBatchList();
                serverBatchList.forEach(serverBatch -> agentTasks.addAll(buildGseAgentTasks(stepInstanceId,
                    executeCount, serverBatch.getBatch(), serverBatch.getServers(), IpStatus.WAITING)));
            } else {
                agentTasks.addAll(buildGseAgentTasks(stepInstanceId, executeCount, stepInstance.getBatch(),
                    stepInstance.getTargetServers().getIpList(), IpStatus.WAITING));
            }
        } else {
            agentTasks.addAll(buildGseAgentTasks(stepInstanceId, executeCount, 0,
                stepInstance.getTargetServers().getIpList(), IpStatus.WAITING));
        }

        // 无效主机
        if (CollectionUtils.isNotEmpty(stepInstance.getTargetServers().getInvalidIpList())) {
            agentTasks.addAll(buildGseAgentTasks(stepInstanceId, executeCount, 0,
                stepInstance.getTargetServers().getInvalidIpList(), IpStatus.HOST_NOT_EXIST));
        }
        agentTaskService.batchSaveAgentTasks(agentTasks);
    }

    private List<AgentTaskDTO> buildGseAgentTasks(long stepInstanceId,
                                                  int executeCount,
                                                  int batch,
                                                  List<IpDTO> servers,
                                                  IpStatus status) {
        return servers.stream()
            .map(server -> buildGseAgentTask(stepInstanceId, executeCount, batch, server, status))
            .collect(Collectors.toList());
    }

    protected AgentTaskDTO buildGseAgentTask(long stepInstanceId,
                                             int executeCount,
                                             int batch,
                                             IpDTO server,
                                             IpStatus status) {
        AgentTaskDTO agentTask = new AgentTaskDTO();
        agentTask.setStepInstanceId(stepInstanceId);
        agentTask.setExecuteCount(executeCount);
        agentTask.setBatch(batch);
        agentTask.setStatus(status.getValue());
        agentTask.setTargetServer(true);
        agentTask.setIp(server.getIp());
        agentTask.setCloudAreaAndIp(server.convertToStrIp());
        agentTask.setCloudAreaId(server.getCloudAreaId());
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
        stepInstanceRollingTask.setStatus(RunStatusEnum.RUNNING.getValue());
        stepInstanceRollingTask.setStepInstanceId(stepInstance.getId());
        stepInstanceRollingTask.setExecuteCount(stepInstance.getExecuteCount());
        stepInstanceRollingTask.setBatch(stepInstance.getBatch());
        stepInstanceRollingTask.setStartTime(DateUtils.currentTimeMillis());
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
            taskExecuteMQEventDispatcher.refreshJob(taskInstanceId);
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

        taskInstanceService.updateStepStatus(stepInstance.getId(), RunStatusEnum.IGNORE_ERROR.getValue());
        taskInstanceService.resetTaskExecuteInfoForResume(stepInstance.getTaskInstanceId());
        taskExecuteMQEventDispatcher.refreshJob(stepInstance.getTaskInstanceId());
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
            taskExecuteMQEventDispatcher.refreshJob(taskInstanceId);
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

        // 如果是滚动步骤，需要更新滚动进度
        if (stepInstance.hasRollingConfig()) {
            int currentRollingBatch = stepInstance.getBatch() + 1;
            stepInstance.setBatch(currentRollingBatch);
            stepInstanceService.updateStepCurrentBatch(stepInstance.getId(), currentRollingBatch);
        }
        taskExecuteMQEventDispatcher.startGseStep(stepInstance.getId(),
            stepInstance.hasRollingConfig() ? stepInstance.getBatch() : null);
    }

    /**
     * 重新执行步骤失败的任务
     */
    private void retryStepFail(StepInstanceDTO stepInstance) {
        log.info("Retry step fail, stepInstanceId={}", stepInstance.getId());
        resetStatusForRetry(stepInstance);
        filePrepareService.retryPrepareFile(stepInstance.getId());
        taskExecuteMQEventDispatcher.retryGseStepFail(stepInstance.getId());
    }

    /**
     * 从头执行步骤
     */
    private void retryStepAll(StepInstanceDTO stepInstance) {
        log.info("Retry step all, stepInstanceId={}", stepInstance.getId());
        resetStatusForRetry(stepInstance);
        filePrepareService.retryPrepareFile(stepInstance.getId());
        taskExecuteMQEventDispatcher.retryGseStepAll(stepInstance.getId());
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

    private void resetStatusForRetry(StepInstanceDTO stepInstance) {
        long stepInstanceId = stepInstance.getId();
        long taskInstanceId = stepInstance.getTaskInstanceId();

        taskInstanceService.resetStepExecuteInfoForRetry(stepInstanceId);
        taskInstanceService.resetTaskExecuteInfoForResume(taskInstanceId);
    }

    private void refreshStep(StepInstanceDTO stepInstance) {
        log.info("Refresh step, stepInstanceId: {}", stepInstance.getId());

        long stepInstanceId = stepInstance.getId();
        int stepStatus = stepInstance.getStatus();

        GseTaskDTO gseTask = gseTaskService.getGseTask(stepInstance.getId(), stepInstance.getExecuteCount(),
            stepInstance.getBatch());
        RunStatusEnum gseTaskStatus = RunStatusEnum.valueOf(gseTask.getStatus());
        if (gseTaskStatus == null) {
            log.error("Refresh step fail, invalid gse task status, stepInstanceId: {}, status: {}",
                stepInstance, stepStatus);
            return;
        }

        long endTime = System.currentTimeMillis();
        long startTime = stepInstance.getStartTime();
        long totalTime = endTime - startTime;

        switch (gseTaskStatus) {
            case SUCCESS:
                if (stepInstance.hasRollingConfig()) {
                    TaskInstanceRollingConfigDTO rollingConfig =
                        rollingConfigService.getRollingConfig(stepInstance.getRollingConfigId());
                    stepInstanceRollingTaskService.updateRollingTask(stepInstanceId, stepInstance.getExecuteCount(),
                        stepInstance.getBatch(), RunStatusEnum.SUCCESS, startTime, endTime, totalTime);
                    int totalBatch = rollingConfig.getConfig().getServerBatchList().size();
                    boolean isLastBatch = totalBatch == stepInstance.getBatch();
                    if (isLastBatch) {
                        taskInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.SUCCESS,
                            startTime, endTime, totalTime);
                        // 步骤执行成功后清理产生的临时文件
                        clearStep(stepInstance);
                    } else {
                        taskInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.ROLLING_WAITING,
                            startTime, endTime, totalTime);
                        return;
                    }
                } else {
                    taskInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.SUCCESS,
                        startTime, endTime, totalTime);
                    // 步骤执行成功后清理产生的临时文件
                    clearStep(stepInstance);
                }
                break;
            case FAIL:
                if (stepInstance.isIgnoreError()) {
                    taskInstanceService.updateStepStatus(stepInstanceId, RunStatusEnum.IGNORE_ERROR.getValue());
                }
                if (stepInstance.hasRollingConfig()) {
                    TaskInstanceRollingConfigDTO rollingConfig =
                        rollingConfigService.getRollingConfig(stepInstance.getRollingConfigId());
                    stepInstanceRollingTaskService.updateRollingTask(stepInstanceId, stepInstance.getExecuteCount(),
                        stepInstance.getBatch(), RunStatusEnum.FAIL, startTime, endTime, totalTime);
                }
                taskInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.FAIL,
                    startTime, endTime, totalTime);
                break;
            case STOP_SUCCESS:
                if (stepStatus == RunStatusEnum.STOPPING.getValue() || stepStatus == RunStatusEnum.RUNNING.getValue()) {
                    taskInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.STOP_SUCCESS,
                        startTime, endTime, totalTime);
                    if (stepInstance.hasRollingConfig()) {
                        stepInstanceRollingTaskService.updateRollingTask(stepInstanceId, stepInstance.getExecuteCount(),
                            stepInstance.getBatch(), RunStatusEnum.STOP_SUCCESS, startTime, endTime, totalTime);
                    }
                } else {
                    log.error("Refresh step fail, stepInstanceId: {}, stepStatus: {}, gseTaskStatus: {}",
                        stepInstanceId, stepStatus, RunStatusEnum.STOP_SUCCESS.getValue());
                    return;
                }
                break;
            case ABNORMAL_STATE:
                setAbnormalStatusForStep(stepInstance);
                if (stepInstance.hasRollingConfig()) {
                    stepInstanceRollingTaskService.updateRollingTask(stepInstanceId, stepInstance.getExecuteCount(),
                        stepInstance.getBatch(), RunStatusEnum.ABNORMAL_STATE, startTime, endTime, totalTime);
                }
                break;
            default:
                log.error("Refresh step fail, stepInstanceId: {}, stepStatus: {}, gseTaskStatus: {}", stepInstanceId,
                    stepStatus, gseTaskStatus.getValue());
                return;
        }
        taskExecuteMQEventDispatcher.refreshJob(stepInstance.getTaskInstanceId());
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
