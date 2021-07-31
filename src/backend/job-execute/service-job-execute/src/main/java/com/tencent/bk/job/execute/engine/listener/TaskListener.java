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

import com.google.common.collect.Lists;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.util.TaskCostCalculator;
import com.tencent.bk.job.execute.engine.TaskExecuteControlMsgSender;
import com.tencent.bk.job.execute.engine.consts.JobActionEnum;
import com.tencent.bk.job.execute.engine.message.TaskProcessor;
import com.tencent.bk.job.execute.engine.model.JobCallbackDTO;
import com.tencent.bk.job.execute.engine.model.TaskControlMessage;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.TaskNotifyDTO;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.statistics.TaskStatisticsMsgSender;
import com.tencent.bk.job.execute.statistics.consts.StatisticsActionEnum;
import com.tencent.bk.job.execute.statistics.model.TaskStatisticsCmd;
import com.tencent.bk.job.manage.common.consts.notify.ExecuteStatusEnum;
import com.tencent.bk.job.manage.common.consts.notify.ResourceTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 执行引擎流程处理-作业
 */
@Component
@EnableBinding({TaskProcessor.class})
@Slf4j
public class TaskListener {

    private final TaskExecuteControlMsgSender taskExecuteControlMsgSender;
    private final TaskStatisticsMsgSender taskStatisticsMsgSender;
    private final TaskInstanceService taskInstanceService;

    @Autowired
    public TaskListener(TaskExecuteControlMsgSender taskExecuteControlMsgSender,
                        TaskStatisticsMsgSender taskStatisticsMsgSender, TaskInstanceService taskInstanceService) {
        this.taskExecuteControlMsgSender = taskExecuteControlMsgSender;
        this.taskStatisticsMsgSender = taskStatisticsMsgSender;
        this.taskInstanceService = taskInstanceService;
    }


    /**
     * 处理和作业相关的控制消息：启动作业、停止作业、重启作业、忽略错误和作业状态刷新
     */
    @StreamListener(TaskProcessor.INPUT)
    public void handleMessage(TaskControlMessage taskControlMessage) {
        log.info("Receive task control message, taskInstanceId={}, action={}, msgSendTime={}",
            taskControlMessage.getTaskInstanceId(),
            taskControlMessage.getAction(), taskControlMessage.getTime());
        long taskInstanceId = taskControlMessage.getTaskInstanceId();
        int action = taskControlMessage.getAction();
        try {
            TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(taskInstanceId);
            if (JobActionEnum.START.getValue() == action) {
                log.info("Start task, taskInstanceId={}", taskInstanceId);
                startJob(taskInstance);
            } else if (JobActionEnum.STOP.getValue() == action) {
                log.info("Stop task, taskInstanceId={}", taskInstanceId);
                stopJob(taskInstance);
            } else if (JobActionEnum.RESTART.getValue() == action) {
                log.info("Restart task, taskInstanceId={}", taskInstanceId);
                restartJob(taskInstance);
            } else if (JobActionEnum.REFRESH.getValue() == action) {
                log.info("Refresh task, taskInstanceId={}", taskInstanceId);
                refreshJob(taskInstance);
            } else {
                log.warn("Error task control action:{}", action);
            }
        } catch (Exception e) {
            String errorMsg = "Handling task control message error,taskInstanceId=" + taskInstanceId;
            log.warn(errorMsg, e);
        }
    }

    /**
     * 作业启动，触发一次任务统计
     *
     * @param taskInstanceId
     */
    private void triggerStartJobStatistics(long taskInstanceId) {
        TaskStatisticsCmd taskStatisticsCmd = new TaskStatisticsCmd();
        taskStatisticsCmd.setAction(StatisticsActionEnum.START_JOB.name());
        taskStatisticsCmd.setTaskInstanceId(taskInstanceId);
        taskStatisticsCmd.setTime(LocalDateTime.now());
        taskStatisticsMsgSender.sendTaskStatisticsCmd(taskStatisticsCmd);
    }

    /**
     * 作业结束，触发一次任务统计
     *
     * @param taskInstanceId
     */
    private void triggerEndJobStatistics(long taskInstanceId) {
        TaskStatisticsCmd taskStatisticsCmd = new TaskStatisticsCmd();
        taskStatisticsCmd.setAction(StatisticsActionEnum.END_JOB.name());
        taskStatisticsCmd.setTaskInstanceId(taskInstanceId);
        taskStatisticsCmd.setTime(LocalDateTime.now());
        taskStatisticsMsgSender.sendTaskStatisticsCmd(taskStatisticsCmd);
    }

    /*
     * 启动作业
     */
    private void startJob(TaskInstanceDTO taskInstance) {
        long taskInstanceId = taskInstance.getId();
        // 首先验证作业的状态，只有状态为“未执行”的作业可以启动
        if (RunStatusEnum.BLANK.getValue().equals(taskInstance.getStatus())) {
            long firstStepId = taskInstanceService.getTaskStepIdList(taskInstanceId).get(0);
            taskInstanceService.updateTaskExecutionInfo(taskInstanceId, RunStatusEnum.RUNNING, firstStepId,
                DateUtils.currentTimeMillis(), null, null);
            taskExecuteControlMsgSender.startStep(firstStepId);
            // 触发任务开始统计分析
            triggerStartJobStatistics(taskInstanceId);
        } else {
            log.warn("Unsupported task instance run status for starting task, taskInstanceId={}, status={}",
                taskInstanceId, taskInstance.getStatus());
        }
    }

    /*
     * 强制终止作业
     */
    private void stopJob(TaskInstanceDTO taskInstance) {
        long taskInstanceId = taskInstance.getId();
        int taskStatus = taskInstance.getStatus();

        if (RunStatusEnum.RUNNING.getValue() == taskStatus) {
            taskInstanceService.updateTaskStatus(taskInstanceId, RunStatusEnum.STOPPING.getValue());
        } else {
            log.warn("Unsupported task instance run status for stop task, taskInstanceId={}, status={}",
                taskInstanceId, taskInstance.getStatus());
        }
    }

    /*
     * 重头执行作业
     */
    private void restartJob(TaskInstanceDTO taskInstance) {
        long taskInstanceId = taskInstance.getId();
        int taskStatus = taskInstance.getStatus();
        // 验证作业状态，只有“执行失败”和“等待用户”的作业可以重头执行
        if (RunStatusEnum.WAITING.getValue() == taskStatus || RunStatusEnum.FAIL.getValue() == taskStatus) {

            // 重置作业状态
            taskInstanceService.resetTaskStatus(taskInstanceId);
            taskInstanceService.addTaskExecuteCount(taskInstanceId);

            // 重置作业下步骤的状态、开始时间和结束时间等。
            List<Long> stepInstanceIdList = taskInstanceService.getTaskStepIdList(taskInstanceId);
            for (long stepInstanceId : stepInstanceIdList) {
                taskInstanceService.updateStepStatus(stepInstanceId, RunStatusEnum.BLANK.getValue());
                taskInstanceService.resetStepStatus(stepInstanceId);
            }

            taskExecuteControlMsgSender.startTask(taskInstanceId);
        } else {
            log.warn("Unsupported task instance run status for restart task, taskInstanceId={}, status={}",
                taskInstanceId, taskInstance.getStatus());
        }
    }

    /*
     * 刷新作业流程控制
     */
    private void refreshJob(TaskInstanceDTO taskInstance) {
        long taskInstanceId = taskInstance.getId();
        int taskStatus = taskInstance.getStatus();

        long currentStepId = taskInstance.getCurrentStepId();
        StepInstanceBaseDTO currentStep = taskInstanceService.getBaseStepInstance(currentStepId);
        int stepStatus = currentStep.getStatus();

        // 验证作业状态，只有'正在执行'、'强制终止中'的作业可以刷新状态进入下一步或者结束
        if (RunStatusEnum.STOPPING.getValue() == taskStatus) {
            if (RunStatusEnum.STOP_SUCCESS.getValue() == stepStatus
                || RunStatusEnum.SUCCESS.getValue() == stepStatus
                || RunStatusEnum.FAIL.getValue() == stepStatus) {
                Long endTime = DateUtils.currentTimeMillis();
                long totalTime = TaskCostCalculator.calculate(taskInstance.getStartTime(), endTime,
                    taskInstance.getTotalTime());
                taskInstanceService.updateTaskExecutionInfo(taskInstanceId, RunStatusEnum.STOP_SUCCESS, null, null,
                    endTime, totalTime);

                int status = stepStatus;
                if (RunStatusEnum.SUCCESS.getValue() == stepStatus) {
                    status = RunStatusEnum.STOP_SUCCESS.getValue();
                    taskInstance.setTotalTime(totalTime);
                    asyncNotifySuccess(taskInstance, currentStep);
                    // 触发任务结束统计分析
                    triggerEndJobStatistics(taskInstanceId);
                } else if (RunStatusEnum.FAIL.getValue() == stepStatus) {
                    asyncNotifyFail(taskInstance, currentStep);
                    // 触发任务结束统计分析
                    triggerEndJobStatistics(taskInstanceId);
                } else {
                    taskInstanceService.updateTaskStatus(taskInstanceId, stepStatus);
                }
                callback(taskInstance, taskInstanceId, status, currentStepId, stepStatus);
            } else {
                log.warn("Unsupported task instance run status for refresh task, taskInstanceId={}, status={}",
                    taskInstanceId, taskInstance.getStatus());
            }

        } else if (RunStatusEnum.RUNNING.getValue() == taskStatus) {
            // 步骤执行成功、跳过、设为忽略错误、终止成功，可以进入下一步
            if (RunStatusEnum.SUCCESS.getValue() == stepStatus
                || RunStatusEnum.SKIPPED.getValue() == stepStatus
                || RunStatusEnum.IGNORE_ERROR.getValue() == stepStatus) {
                List<Long> taskStepIdList = taskInstanceService.getTaskStepIdList(taskInstanceId);
                long nextStepId = getNextStepId(taskStepIdList, currentStepId);
                // 当前步骤执行成功或者用户手动跳过时，均应该进入下一步
                if (nextStepId == currentStepId) { // 当前执行步骤为最后一步
                    Long endTime = DateUtils.currentTimeMillis();
                    long totalTime = TaskCostCalculator.calculate(taskInstance.getStartTime(), endTime,
                        taskInstance.getTotalTime());
                    taskInstanceService.updateTaskExecutionInfo(taskInstanceId, RunStatusEnum.SUCCESS, null, null,
                        endTime, totalTime);
                    taskInstance.setTotalTime(totalTime);
                    asyncNotifySuccess(taskInstance, currentStep);
                    callback(taskInstance, taskInstanceId, RunStatusEnum.SUCCESS.getValue(), currentStepId, stepStatus);
                    // 触发任务结束统计分析
                    triggerEndJobStatistics(taskInstanceId);
                } else { // 进入下一步
                    taskInstanceService.updateTaskCurrentStepId(taskInstanceId, nextStepId);
                    taskExecuteControlMsgSender.startStep(nextStepId);
                }
                // 步骤执行成功后清理产生的临时文件
                taskExecuteControlMsgSender.clearStep(currentStepId);
            } else if (RunStatusEnum.FAIL.getValue() == stepStatus) {
                if (currentStep.isIgnoreError()) {
                    taskInstanceService.updateStepStatus(currentStepId, RunStatusEnum.IGNORE_ERROR.getValue());
                    goToNextStep(taskInstance, currentStep);
                    return;
                }
                // 步骤失败，任务结束
                Long endTime = DateUtils.currentTimeMillis();
                long totalTime = TaskCostCalculator.calculate(taskInstance.getStartTime(), endTime,
                    taskInstance.getTotalTime());
                taskInstanceService.updateTaskExecutionInfo(taskInstanceId, RunStatusEnum.FAIL, null, null, endTime,
                    totalTime);
                asyncNotifyFail(taskInstance, currentStep);
                callback(taskInstance, taskInstanceId, RunStatusEnum.FAIL.getValue(), currentStepId, stepStatus);
                // 触发任务结束统计分析
                triggerEndJobStatistics(taskInstanceId);
            } else {
                log.warn("Unsupported task instance run status for refresh task, taskInstanceId={}, status={}",
                    taskInstanceId, taskInstance.getStatus());
            }
        } else {
            log.warn("Unsupported task instance run status for refresh task, taskInstanceId={}, status={}",
                taskInstanceId, taskInstance.getStatus());
        }
    }

    private void goToNextStep(TaskInstanceDTO taskInstance, StepInstanceBaseDTO currentStep) {
        long taskInstanceId = taskInstance.getId();
        long currentStepId = currentStep.getId();
        List<Long> taskStepIdList = taskInstanceService.getTaskStepIdList(taskInstanceId);
        long nextStepId = getNextStepId(taskStepIdList, currentStepId);
        // 当前步骤执行成功或者用户手动跳过时，均应该进入下一步
        if (nextStepId == currentStepId) { // 当前执行步骤为最后一步
            Long endTime = DateUtils.currentTimeMillis();
            long totalTime = TaskCostCalculator.calculate(taskInstance.getStartTime(), endTime,
                taskInstance.getTotalTime());
            taskInstanceService.updateTaskExecutionInfo(taskInstanceId, RunStatusEnum.SUCCESS, null, null, endTime,
                totalTime);

            asyncNotifySuccess(taskInstance, currentStep);
            callback(taskInstance, taskInstanceId, RunStatusEnum.SUCCESS.getValue(), currentStepId,
                currentStep.getStatus());
        } else { // 进入下一步
            taskInstanceService.updateTaskCurrentStepId(taskInstanceId, nextStepId);

            taskExecuteControlMsgSender.startStep(nextStepId);
        }
    }

    private void callback(TaskInstanceDTO taskInstance, long taskInstanceId, int taskStatus, long currentStepId,
                          int stepStatus) {
        if (taskInstance.getCallbackUrl() != null) {
            JobCallbackDTO dto = new JobCallbackDTO();
            dto.setId(taskInstanceId);
            dto.setStatus(taskStatus);
            dto.setCallbackUrl(taskInstance.getCallbackUrl());
            Collection<JobCallbackDTO.StepInstanceStatus> instances = Lists.newArrayList();
            JobCallbackDTO.StepInstanceStatus e = new JobCallbackDTO.StepInstanceStatus();
            e.setId(currentStepId);
            e.setStatus(stepStatus);
            instances.add(e);
            dto.setStepInstances(instances);
            taskExecuteControlMsgSender.sendCallback(dto);
        }
    }

    public long getNextStepId(List<Long> stepIdList, long currentStepId) {
        int currentStepIndex = stepIdList.indexOf(currentStepId);
        int nextStepIndex = Math.min(currentStepIndex + 1, stepIdList.size() - 1);
        return stepIdList.get(nextStepIndex);
    }

    private void setResourceInfo(TaskInstanceDTO taskInstance, StepInstanceBaseDTO stepInstance,
                                 TaskNotifyDTO taskNotifyDTO) {
        Long taskPlanId = taskInstance.getTaskId();
        taskNotifyDTO.setResourceId(String.valueOf(taskPlanId));
        if (taskPlanId == -1L) {
            if (stepInstance.getExecuteType().equals(StepExecuteTypeEnum.EXECUTE_SCRIPT.getValue())) {
                taskNotifyDTO.setResourceType(ResourceTypeEnum.SCRIPT.getType());
            } else if (stepInstance.getExecuteType().equals(StepExecuteTypeEnum.SEND_FILE.getValue())) {
                taskNotifyDTO.setResourceType(ResourceTypeEnum.FILE.getType());
            } else {
                log.warn("notify resourceType not supported yet:{}, use Job", stepInstance.getExecuteType());
                taskNotifyDTO.setResourceType(ResourceTypeEnum.JOB.getType());
            }
        } else {
            taskNotifyDTO.setResourceType(ResourceTypeEnum.JOB.getType());
        }
    }

    private void asyncNotifyFail(TaskInstanceDTO taskInstance, StepInstanceBaseDTO stepInstance) {
        TaskNotifyDTO taskNotifyDTO = buildCommonTaskNotification(taskInstance, stepInstance);
        taskNotifyDTO.setResourceExecuteStatus(ExecuteStatusEnum.FAIL.getStatus());
        taskNotifyDTO.setStepName(stepInstance.getName());
        setResourceInfo(taskInstance, stepInstance, taskNotifyDTO);
        taskExecuteControlMsgSender.asyncSendNotifyMsg(taskNotifyDTO);
    }

    private void asyncNotifySuccess(TaskInstanceDTO taskInstance, StepInstanceBaseDTO stepInstance) {
        TaskNotifyDTO taskNotifyDTO = buildCommonTaskNotification(taskInstance, stepInstance);
        taskNotifyDTO.setResourceExecuteStatus(ExecuteStatusEnum.SUCCESS.getStatus());
        taskNotifyDTO.setCost(taskInstance.getTotalTime());
        setResourceInfo(taskInstance, stepInstance, taskNotifyDTO);
        taskExecuteControlMsgSender.asyncSendNotifyMsg(taskNotifyDTO);
    }

    private TaskNotifyDTO buildCommonTaskNotification(TaskInstanceDTO taskInstance, StepInstanceBaseDTO stepInstance) {
        TaskNotifyDTO taskNotifyDTO = new TaskNotifyDTO();
        taskNotifyDTO.setAppId(taskInstance.getAppId());
        String operator = getOperator(taskInstance.getOperator(), stepInstance.getOperator());
        taskNotifyDTO.setOperator(operator);
        taskNotifyDTO.setTaskInstanceId(taskInstance.getId());
        taskNotifyDTO.setStartupMode(taskInstance.getStartupMode());
        taskNotifyDTO.setTaskId(taskInstance.getTaskId());
        taskNotifyDTO.setTaskInstanceName(taskInstance.getName());
        return taskNotifyDTO;
    }

    private String getOperator(String taskOperator, String stepOperator) {
        return StringUtils.isNotEmpty(stepOperator) ? stepOperator : taskOperator;
    }
}
