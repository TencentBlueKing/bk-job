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

import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.util.TaskCostCalculator;
import com.tencent.bk.job.execute.engine.TaskExecuteControlMsgSender;
import com.tencent.bk.job.execute.engine.message.StepProcessor;
import com.tencent.bk.job.execute.engine.model.StepControlMessage;
import com.tencent.bk.job.execute.engine.third.FileSourceTaskManager;
import com.tencent.bk.job.execute.model.*;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.manage.common.consts.notify.ExecuteStatusEnum;
import com.tencent.bk.job.manage.common.consts.notify.ResourceTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Component;

import static com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum.EXECUTE_SCRIPT;
import static com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum.EXECUTE_SQL;
import static com.tencent.bk.job.execute.engine.consts.StepActionEnum.*;

/**
 * 执行引擎流程处理-步骤
 */
@Component
@EnableBinding({StepProcessor.class})
@Slf4j
public class StepListener {
    private final TaskInstanceService taskInstanceService;
    private final TaskExecuteControlMsgSender taskControlMsgSender;
    private final FileSourceTaskManager fileSourceTaskManager;

    @Autowired
    public StepListener(TaskInstanceService taskInstanceService,
                        TaskExecuteControlMsgSender taskControlMsgSender,
                        FileSourceTaskManager fileSourceTaskManager) {
        this.taskInstanceService = taskInstanceService;
        this.taskControlMsgSender = taskControlMsgSender;
        this.fileSourceTaskManager = fileSourceTaskManager;
    }

    /**
     * 处理步骤控制相关消息，包含：预启动步骤、启动步骤、重新执行步骤和跳过步骤
     */
    @StreamListener(StepProcessor.INPUT)
    public void handleMessage(StepControlMessage stepControlMessage) {
        log.info("Receive step control message, stepInstanceId={}, action={}, msgSendTime={}",
            stepControlMessage.getStepInstanceId(),
            stepControlMessage.getAction(), stepControlMessage.getTime());
        long stepInstanceId = stepControlMessage.getStepInstanceId();
        try {
            int action = stepControlMessage.getAction();
            StepInstanceBaseDTO stepInstance = taskInstanceService.getBaseStepInstance(stepInstanceId);
            if (START.getValue() == action) {
                log.info("Start step, stepInstanceId={}", stepInstanceId);
                startStep(stepInstance);
            } else if (SKIP.getValue() == action) {
                TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(stepInstance.getTaskInstanceId());
                if (taskInstance.getCurrentStepId() == stepInstanceId) {
                    log.info("Skip step, stepInstanceId={}", stepInstanceId);
                    skipStep(stepInstance);
                } else {
                    log.warn("Only current running step is support for skipping, stepInstanceId={}", stepInstanceId);
                }
            } else if (RETRY_FAIL.getValue() == action) {
                log.info("Retry step fail, stepInstanceId={}", stepInstanceId);
                retryStepFail(stepInstance);
            } else if (RETRY_ALL.getValue() == action) {
                log.info("Retry step all, stepInstanceId={}", stepInstanceId);
                retryStepAll(stepInstance);
            } else if (STOP.getValue() == action) {
                log.info("Force stop step, stepInstanceId={}", stepInstanceId);
                stopStep(stepInstance);
            } else if (IGNORE_ERROR.getValue() == action) {
                log.info("Ignore step error, stepInstanceId={}", stepInstanceId);
                ignoreError(stepInstance);
            } else if (NEXT_STEP.getValue() == action) {
                log.info("Next step, stepInstanceId={}", stepInstanceId);
                nextStep(stepInstance);
            } else if (CONFIRM_TERMINATE.getValue() == action) {
                log.info("Confirm step terminate, stepInstanceId={}", stepInstanceId);
                confirmStepTerminate(stepInstance);
            } else if (CONFIRM_RESTART.getValue() == action) {
                log.info("Confirm step restart, stepInstanceId={}", stepInstanceId);
                confirmStepRestart(stepInstance);
            } else if (CONFIRM_CONTINUE.getValue() == action) {
                log.info("Confirm step continue, stepInstanceId={}", stepInstanceId);
                confirmStepContinue(stepInstance);
            } else if (CONTINUE_FILE_PUSH.getValue() == action) {
                log.info("continue file push step, stepInstanceId={}", stepInstanceId);
                continueGseFileStep(stepInstance);
            } else if (CLEAR.getValue() == action) {
                log.info("clear step, stepInstanceId={}", stepInstanceId);
                clearStep(stepInstance);
            } else {
                log.warn("Error step control action:{}", action);
            }
        } catch (Exception e) {
            String errorMsg = "Handling step control message error,stepInstanceId:" + stepInstanceId;
            log.warn(errorMsg, e);
        }

    }

    private void confirmStepTerminate(StepInstanceBaseDTO stepInstance) {
        long stepInstanceId = stepInstance.getId();
        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(stepInstance.getTaskInstanceId());
        if (RunStatusEnum.WAITING.getValue().equals(stepInstance.getStatus())) {
            Long endTime = DateUtils.currentTimeMillis();
            long taskTotalTime = TaskCostCalculator.calculate(taskInstance.getStartTime(), endTime,
                taskInstance.getTotalTime());
            taskInstanceService.updateTaskExecutionInfo(taskInstance.getId(), RunStatusEnum.CONFIRM_TERMINATED, null,
                null, endTime, taskTotalTime);
            long stepTotalTime = TaskCostCalculator.calculate(stepInstance.getStartTime(), endTime,
                stepInstance.getTotalTime());
            taskInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.CONFIRM_TERMINATED, null,
                endTime, stepTotalTime);
        } else {
            log.warn("Unsupported step instance status for confirm step terminate action, stepInstanceId:{}, " +
                "status:{}", stepInstanceId, stepInstance.getStatus());
        }
    }

    private void confirmStepRestart(StepInstanceBaseDTO stepInstance) {
        long stepInstanceId = stepInstance.getId();
        if (RunStatusEnum.CONFIRM_TERMINATED.getValue().equals(stepInstance.getStatus())) {
            executeConfirmStep(stepInstance);
        } else {
            log.warn("Unsupported step instance status for confirm-step-restart action, stepInstanceId:{}, status:{}"
                , stepInstanceId, stepInstance.getStatus());
        }
    }

    private void nextStep(StepInstanceBaseDTO stepInstance) {
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
            taskControlMsgSender.refreshTask(taskInstanceId);
        } else {
            log.warn("Unsupported step instance status for next step action, stepInstanceId:{}, status:{}",
                stepInstanceId, stepInstance.getStatus());
        }
    }

    private void confirmStepContinue(StepInstanceBaseDTO stepInstance) {
        long taskInstanceId = stepInstance.getTaskInstanceId();
        long stepInstanceId = stepInstance.getId();
        int stepStatus = stepInstance.getStatus();

        if (RunStatusEnum.WAITING.getValue() == stepStatus) {
            taskInstanceService.updateTaskStatus(taskInstanceId, RunStatusEnum.RUNNING.getValue());
            long endTime = DateUtils.currentTimeMillis();
            long totalTime = TaskCostCalculator.calculate(stepInstance.getStartTime(), endTime,
                stepInstance.getTotalTime());
            // 人工确认通过，该步骤状态标识为成功；终止成功的步骤保持状态不变
            taskInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.SUCCESS, null, endTime,
                totalTime);
            taskControlMsgSender.refreshTask(taskInstanceId);
        } else {
            log.warn("Unsupported step instance status for confirm-step-continue step action, stepInstanceId:{}, " +
                "status:{}", stepInstanceId, stepInstance.getStatus());
        }
    }

    private void ignoreError(StepInstanceBaseDTO stepInstance) {
        if (!stepInstance.getStatus().equals(RunStatusEnum.FAIL.getValue())) {
            log.warn("Current step status does not support ignore error operation! stepInstanceId:{}, status:{}",
                stepInstance.getId(), stepInstance.getStatus());
            return;
        }

        taskInstanceService.updateStepStatus(stepInstance.getId(), RunStatusEnum.IGNORE_ERROR.getValue());
        taskInstanceService.resetTaskExecuteInfoForResume(stepInstance.getTaskInstanceId());
        taskControlMsgSender.refreshTask(stepInstance.getTaskInstanceId());
    }

    private void startStep(StepInstanceBaseDTO stepInstance) {
        int stepStatus = stepInstance.getStatus();
        long stepInstanceId = stepInstance.getId();
        long taskInstanceId = stepInstance.getTaskInstanceId();

        // 只有当步骤状态为'等待用户'和'未执行'时可以启动步骤
        if (RunStatusEnum.BLANK.getValue() == stepStatus || RunStatusEnum.WAITING.getValue() == stepStatus) {
            taskInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.RUNNING,
                DateUtils.currentTimeMillis(), null, null);
            taskInstanceService.updateTaskStatus(taskInstanceId, RunStatusEnum.RUNNING.getValue());

            int stepType = stepInstance.getExecuteType();
            if (EXECUTE_SCRIPT.getValue() == stepType || StepExecuteTypeEnum.EXECUTE_SQL.getValue() == stepType) {
                taskControlMsgSender.startGseStep(stepInstanceId);
            } else if (TaskStepTypeEnum.FILE.getValue() == stepType) {
                fileSourceTaskManager.checkThirdFileAndPrepareForGseTask(stepInstanceId);
            } else if (TaskStepTypeEnum.APPROVAL.getValue() == stepType) {
                executeConfirmStep(stepInstance);
            } else {
                log.warn("Unsupported step type, skip it! stepInstanceId={}, stepType={}", stepInstanceId, stepType);
                taskInstanceService.updateTaskStatus(taskInstanceId, RunStatusEnum.SKIPPED.getValue());
                taskControlMsgSender.refreshTask(taskInstanceId);
            }
        } else {
            log.warn("Unsupported step instance run status for starting step, stepInstanceId={}, status={}",
                stepInstanceId, stepStatus);
        }
    }

    private void skipStep(StepInstanceBaseDTO stepInstance) {
        int stepStatus = stepInstance.getStatus();
        long stepInstanceId = stepInstance.getId();
        long taskInstanceId = stepInstance.getTaskInstanceId();

        // 只有当步骤状态为'终止中'时可以跳过步骤
        if (RunStatusEnum.STOPPING.getValue() == stepStatus) {
            long now = DateUtils.currentTimeMillis();
            taskInstanceService.updateStepStartTimeIfNull(stepInstanceId, now);
            taskInstanceService.updateStepStatus(stepInstanceId, RunStatusEnum.SKIPPED.getValue());
            taskInstanceService.updateStepEndTime(stepInstanceId, now);

            taskInstanceService.updateTaskStatus(taskInstanceId, RunStatusEnum.RUNNING.getValue());
            taskControlMsgSender.refreshTask(taskInstanceId);
        } else {
            log.warn("Unsupported step instance run status for skipping step, stepInstanceId={}, status={}",
                stepInstanceId, stepStatus);
        }
    }

    private void stopStep(StepInstanceBaseDTO stepInstance) {
        long stepInstanceId = stepInstance.getId();
        long taskInstanceId = stepInstance.getTaskInstanceId();

        int executeType = stepInstance.getExecuteType();
        if (TaskStepTypeEnum.SCRIPT.getValue() == executeType || TaskStepTypeEnum.FILE.getValue() == executeType
            || EXECUTE_SQL.getValue().equals(executeType)) {
            taskInstanceService.updateTaskStatus(taskInstanceId, RunStatusEnum.STOPPING.getValue());
        } else {
            log.warn("Not gse step type, can not stop! stepInstanceId={}, stepType={}", stepInstanceId, executeType);
        }
    }

    /**
     * 第三方文件源文件拉取完成后继续GSE文件分发
     *
     * @param stepInstance 步骤实例
     */
    private void continueGseFileStep(StepInstanceBaseDTO stepInstance) {
        taskControlMsgSender.startGseStep(stepInstance.getId());
    }

    /**
     * 重新执行步骤失败的任务
     */
    private void retryStepFail(StepInstanceBaseDTO stepInstance) {
        resetStatusForRetry(stepInstance);
        fileSourceTaskManager.retryThirdFilePulling(stepInstance.getId());
        taskControlMsgSender.retryGseStepFail(stepInstance.getId());
    }

    /**
     * 从头执行步骤
     */
    private void retryStepAll(StepInstanceBaseDTO stepInstance) {
        resetStatusForRetry(stepInstance);
        fileSourceTaskManager.retryThirdFilePulling(stepInstance.getId());
        taskControlMsgSender.retryGseStepAll(stepInstance.getId());
    }

    /**
     * 清理执行完的步骤
     */
    private void clearStep(StepInstanceBaseDTO stepInstance) {
        int executeType = stepInstance.getExecuteType();
        // 当前仅有文件分发类步骤需要清理中间文件
        if (TaskStepTypeEnum.FILE.getValue() == executeType) {
            fileSourceTaskManager.clearThirdFileTask(stepInstance.getId());
        }
    }

    private void resetStatusForRetry(StepInstanceBaseDTO stepInstance) {
        long stepInstanceId = stepInstance.getId();
        long taskInstanceId = stepInstance.getTaskInstanceId();

        taskInstanceService.resetStepExecuteInfoForRetry(stepInstanceId);
        taskInstanceService.resetTaskExecuteInfoForResume(taskInstanceId);
    }

    /**
     * 人工确认步骤
     */
    private void executeConfirmStep(StepInstanceBaseDTO stepInstance) {
        long stepInstanceId = stepInstance.getId();
        long taskInstanceId = stepInstance.getTaskInstanceId();

        // 只有“未执行”和“确认终止”状态的，才可以重新执行人工确认步骤
        if (RunStatusEnum.BLANK.getValue().equals(stepInstance.getStatus())
            || RunStatusEnum.CONFIRM_TERMINATED.getValue().equals(stepInstance.getStatus())) {
            // 发送页面确认信息
            TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(taskInstanceId);
            String stepOperator = stepInstance.getOperator();

            if (StringUtils.isBlank(stepOperator)) {
                log.info("The operator is empty, continue run step! stepInstanceId={}", stepInstanceId);
                stepOperator = taskInstance.getOperator();
                stepInstance.setOperator(stepOperator);
            }
            taskInstanceService.updateStepStatus(stepInstanceId, RunStatusEnum.WAITING.getValue());
            taskInstanceService.updateTaskStatus(taskInstanceId, RunStatusEnum.WAITING.getValue());
            asyncNotifyConfirm(taskInstance, stepInstance);
        } else {
            log.warn("Unsupported step instance run status for executing confirm step, stepInstanceId={}, status={}",
                stepInstanceId, stepInstance.getStatus());
        }
    }

    private void asyncNotifyConfirm(TaskInstanceDTO taskInstance, StepInstanceBaseDTO stepInstance) {
        StepInstanceDTO stepInstanceDetail = taskInstanceService.getStepInstanceDetail(stepInstance.getId());
        if (stepInstanceDetail == null) {
            log.warn("StepInstance is not exist, stepInstanceId: {}", stepInstance.getId());
            return;
        }
        TaskNotifyDTO taskNotifyDTO = buildCommonTaskNotification(taskInstance, stepInstance);
        taskNotifyDTO.setResourceExecuteStatus(ExecuteStatusEnum.READY.getStatus());
        taskNotifyDTO.setStepName(stepInstance.getName());
        taskNotifyDTO.setConfirmMessage(stepInstanceDetail.getConfirmMessage());
        NotifyDTO notifyDTO = new NotifyDTO();
        notifyDTO.setReceiverUsers(stepInstanceDetail.getConfirmUsers());
        notifyDTO.setReceiverRoles(stepInstanceDetail.getConfirmRoles());
        notifyDTO.setChannels(stepInstanceDetail.getNotifyChannels());
        notifyDTO.setTriggerUser(stepInstance.getOperator());
        taskNotifyDTO.setNotifyDTO(notifyDTO);
        taskNotifyDTO.setResourceId(String.valueOf(taskInstance.getTaskId()));
        taskNotifyDTO.setResourceType(ResourceTypeEnum.JOB.getType());
        taskNotifyDTO.setOperator(stepInstance.getOperator());
        taskControlMsgSender.asyncSendNotifyMsg(taskNotifyDTO);
    }

    private TaskNotifyDTO buildCommonTaskNotification(TaskInstanceDTO taskInstance, StepInstanceBaseDTO stepInstance) {
        TaskNotifyDTO taskNotifyDTO = new TaskNotifyDTO();
        taskNotifyDTO.setAppId(taskInstance.getAppId());
        String operator = getOperator(taskInstance.getOperator(), stepInstance.getOperator());
        taskNotifyDTO.setStartupMode(taskInstance.getStartupMode());
        taskNotifyDTO.setOperator(operator);
        taskNotifyDTO.setTaskInstanceId(taskInstance.getId());
        taskNotifyDTO.setTaskInstanceName(taskInstance.getName());
        taskNotifyDTO.setTaskId(taskInstance.getTaskId());
        return taskNotifyDTO;
    }

    private String getOperator(String taskOperator, String stepOperator) {
        return StringUtils.isNotEmpty(stepOperator) ? stepOperator : taskOperator;
    }
}
