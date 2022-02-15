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
import com.tencent.bk.job.execute.common.util.TaskCostCalculator;
import com.tencent.bk.job.execute.engine.consts.JobActionEnum;
import com.tencent.bk.job.execute.engine.listener.event.JobEvent;
import com.tencent.bk.job.execute.engine.listener.event.StepEvent;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.message.TaskProcessor;
import com.tencent.bk.job.execute.engine.model.JobCallbackDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceRollingConfigDTO;
import com.tencent.bk.job.execute.model.db.RollingConfigDO;
import com.tencent.bk.job.execute.service.NotifyService;
import com.tencent.bk.job.execute.service.RollingConfigService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.statistics.StatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * 执行引擎事件处理-作业
 */
@Component
@EnableBinding({TaskProcessor.class})
@Slf4j
public class JobListener {

    private final TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;
    private final StatisticsService statisticsService;
    private final TaskInstanceService taskInstanceService;
    private final StepInstanceService stepInstanceService;
    private final RollingConfigService rollingConfigService;
    private final NotifyService notifyService;

    @Autowired
    public JobListener(TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
                       StatisticsService statisticsService,
                       TaskInstanceService taskInstanceService,
                       StepInstanceService stepInstanceService,
                       RollingConfigService rollingConfigService,
                       NotifyService notifyService) {
        this.taskExecuteMQEventDispatcher = taskExecuteMQEventDispatcher;
        this.statisticsService = statisticsService;
        this.taskInstanceService = taskInstanceService;
        this.stepInstanceService = stepInstanceService;
        this.rollingConfigService = rollingConfigService;
        this.notifyService = notifyService;
    }


    /**
     * 处理作业执行相关的事件
     *
     * @param jobEvent 作业执行相关的事件
     */
    @StreamListener(TaskProcessor.INPUT)
    public void handleEvent(JobEvent jobEvent) {
        log.info("Handle job event, event: {}", jobEvent);
        long taskInstanceId = jobEvent.getTaskInstanceId();
        int action = jobEvent.getAction();
        try {
            TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(taskInstanceId);
            if (JobActionEnum.START.getValue() == action) {
                startJob(taskInstance);
            } else if (JobActionEnum.STOP.getValue() == action) {
                stopJob(taskInstance);
            } else if (JobActionEnum.RESTART.getValue() == action) {
                restartJob(taskInstance);
            } else if (JobActionEnum.REFRESH.getValue() == action) {
                refreshJob(taskInstance);
            } else {
                log.error("Invalid job action: {}", action);
            }
        } catch (Throwable e) {
            String errorMsg = "Handle job event error, taskInstanceId=" + taskInstanceId;
            log.error(errorMsg, e);
        }
    }

    /**
     * 启动作业
     *
     * @param taskInstance 作业实例
     */
    private void startJob(TaskInstanceDTO taskInstance) {
        long taskInstanceId = taskInstance.getId();
        log.info("Start job, taskInstanceId={}", taskInstanceId);
        // 首先验证作业的状态，只有状态为“未执行”的作业可以启动
        if (RunStatusEnum.BLANK.getValue().equals(taskInstance.getStatus())) {
            StepInstanceBaseDTO stepInstance = taskInstanceService.getFirstStepInstance(taskInstanceId);
            taskInstanceService.updateTaskExecutionInfo(taskInstanceId, RunStatusEnum.RUNNING, stepInstance.getId(),
                DateUtils.currentTimeMillis(), null, null);
            startStep(stepInstance);

            // 触发任务开始统计分析
            statisticsService.updateStartJobStatistics(taskInstance);
        } else {
            log.error("Unsupported task instance run status for starting job, taskInstanceId={}, status={}",
                taskInstanceId, taskInstance.getStatus());
        }
    }

    /**
     * 强制终止作业
     *
     * @param taskInstance 作业实例
     */
    private void stopJob(TaskInstanceDTO taskInstance) {
        long taskInstanceId = taskInstance.getId();
        log.info("Stop job, taskInstanceId={}", taskInstanceId);
        int taskStatus = taskInstance.getStatus();

        if (RunStatusEnum.RUNNING.getValue() == taskStatus) {
            taskInstanceService.updateTaskStatus(taskInstanceId, RunStatusEnum.STOPPING.getValue());
        } else {
            log.warn("Unsupported task instance run status for stop task, taskInstanceId={}, status={}",
                taskInstanceId, taskInstance.getStatus());
        }
    }

    /**
     * 重头执行作业
     *
     * @param taskInstance 作业实例
     */
    private void restartJob(TaskInstanceDTO taskInstance) {
        long taskInstanceId = taskInstance.getId();
        log.info("Restart job, taskInstanceId={}", taskInstanceId);
        int taskStatus = taskInstance.getStatus();
        // 验证作业状态，只有“执行失败”和“等待用户”的作业可以重头执行
        if (RunStatusEnum.WAITING.getValue() == taskStatus || RunStatusEnum.FAIL.getValue() == taskStatus) {

            // 重置作业状态
            taskInstanceService.resetTaskStatus(taskInstanceId);
            taskInstanceService.addStepInstanceExecuteCount(taskInstanceId);

            // 重置作业下步骤的状态、开始时间和结束时间等。
            List<Long> stepInstanceIdList = taskInstanceService.getTaskStepIdList(taskInstanceId);
            for (long stepInstanceId : stepInstanceIdList) {
                taskInstanceService.updateStepStatus(stepInstanceId, RunStatusEnum.BLANK.getValue());
                taskInstanceService.resetStepStatus(stepInstanceId);
            }

            taskExecuteMQEventDispatcher.dispatchJobEvent(JobEvent.startJob(taskInstanceId));
        } else {
            log.warn("Unsupported task instance run status for restart task, taskInstanceId={}, status={}",
                taskInstanceId, taskInstance.getStatus());
        }
    }

    /**
     * 作业状态流转
     *
     * @param taskInstance 作业实例
     */
    private void refreshJob(TaskInstanceDTO taskInstance) {
        long taskInstanceId = taskInstance.getId();
        log.info("Refresh job, taskInstanceId={}", taskInstanceId);
        int taskStatus = taskInstance.getStatus();

        long currentStepInstanceId = taskInstance.getCurrentStepInstanceId();
        StepInstanceBaseDTO currentStepInstance = taskInstanceService.getBaseStepInstance(currentStepInstanceId);
        int stepStatus = currentStepInstance.getStatus();

        // 验证作业状态，只有'正在执行'、'强制终止中'的作业可以刷新状态进入下一步或者结束
        if (RunStatusEnum.STOPPING.getValue() == taskStatus) {
            if (RunStatusEnum.STOP_SUCCESS.getValue() == stepStatus
                || RunStatusEnum.SUCCESS.getValue() == stepStatus
                || RunStatusEnum.FAIL.getValue() == stepStatus) {

                finishJob(taskInstance, currentStepInstance, RunStatusEnum.STOP_SUCCESS);
            } else {
                log.error("Unsupported task instance run status for refresh task, taskInstanceId={}, taskStatus={}, " +
                    "stepStatus: {}", taskInstanceId, taskStatus, stepStatus);
            }
        } else if (RunStatusEnum.RUNNING.getValue() == taskStatus) {
            // 步骤状态为成功、跳过、设为忽略错误、滚动等待，可以进入下一步
            if (RunStatusEnum.SUCCESS.getValue() == stepStatus
                || RunStatusEnum.SKIPPED.getValue() == stepStatus
                || RunStatusEnum.IGNORE_ERROR.getValue() == stepStatus
                || RunStatusEnum.ROLLING_WAITING.getValue() == stepStatus) {

                StepInstanceBaseDTO nextStepInstance = getNextStepInstance(taskInstance, currentStepInstance);
                if (nextStepInstance == null) {
                    finishJob(taskInstance, currentStepInstance, RunStatusEnum.SUCCESS);
                } else {
                    // 执行下一步骤
                    startStep(nextStepInstance);
                }
            } else if (RunStatusEnum.FAIL.getValue() == stepStatus) {
                // 步骤失败，任务结束
                finishJob(taskInstance, currentStepInstance, RunStatusEnum.FAIL);
            } else {
                log.warn("Unsupported task instance run status for refresh task, taskInstanceId={}, status={}",
                    taskInstanceId, taskInstance.getStatus());
            }
        } else {
            log.warn("Unsupported task instance run status for refresh task, taskInstanceId={}, status={}",
                taskInstanceId, taskInstance.getStatus());
        }
    }

    private void finishJob(TaskInstanceDTO taskInstance,
                           StepInstanceBaseDTO stepInstance,
                           RunStatusEnum jobStatus) {
        long taskInstanceId = taskInstance.getId();
        long stepInstanceId = stepInstance.getId();
        Long endTime = DateUtils.currentTimeMillis();
        long totalTime = TaskCostCalculator.calculate(taskInstance.getStartTime(), endTime,
            taskInstance.getTotalTime());
        taskInstance.setEndTime(endTime);
        taskInstance.setTotalTime(totalTime);
        taskInstance.setStatus(jobStatus.getValue());
        taskInstanceService.updateTaskExecutionInfo(taskInstanceId, jobStatus, null, null, endTime, totalTime);

        // 作业执行结果消息通知
        if (RunStatusEnum.SUCCESS == jobStatus || RunStatusEnum.IGNORE_ERROR == jobStatus) {
            notifyService.asyncSendMQSuccessTaskNotification(taskInstance, stepInstance);
        } else {
            notifyService.asyncSendMQFailTaskNotification(taskInstance, stepInstance);
        }

        // 触发作业结束统计分析
        statisticsService.updateEndJobStatistics(taskInstance);

        // 作业执行完成回调
        callback(taskInstance, taskInstanceId, jobStatus.getValue(), stepInstanceId, stepInstance.getStatus());
    }

    /**
     * 获取下一个执行的步骤实例
     *
     * @param taskInstance        作业实例
     * @param currentStepInstance 当前步骤实例
     * @return 步骤实例;如果当前步骤已经是最后一个步骤，那么返回null
     */
    private StepInstanceBaseDTO getNextStepInstance(TaskInstanceDTO taskInstance,
                                                    StepInstanceBaseDTO currentStepInstance) {
        StepInstanceBaseDTO nextStepInstance = null;
        if (currentStepInstance.isRollingStep()) {
            int currentBatch = currentStepInstance.getBatch();
            TaskInstanceRollingConfigDTO taskInstanceRollingConfig =
                rollingConfigService.getRollingConfig(currentStepInstance.getRollingConfigId());
            RollingConfigDO rollingConfig = taskInstanceRollingConfig.getConfig();
            List<Long> includeStepInstanceIdList = rollingConfig.getIncludeStepInstanceIdList();
            boolean isLastRollingStep =
                includeStepInstanceIdList.get(includeStepInstanceIdList.size() - 1).equals(currentStepInstance.getId());
            boolean isLastBatch = rollingConfig.getTotalBatch() == currentBatch;

            // 最后一个滚动步骤和滚动批次，那么该滚动任务执行结束，进入下一个步骤
            if (isLastRollingStep && isLastBatch) {
                nextStepInstance = stepInstanceService.getNextStepInstance(taskInstance.getId(),
                    currentStepInstance.getStepOrder());
            } else {
                Long nextRollingStepInstanceId;
                if (isLastRollingStep) {
                    nextRollingStepInstanceId = includeStepInstanceIdList.get(0);
                } else {
                    nextRollingStepInstanceId = getNextStepInstanceId(includeStepInstanceIdList,
                        currentStepInstance.getId());
                }
                if (nextRollingStepInstanceId != null) {
                    nextStepInstance = taskInstanceService.getBaseStepInstance(nextRollingStepInstanceId);
                }
            }
        } else {
            nextStepInstance = stepInstanceService.getNextStepInstance(taskInstance.getId(),
                currentStepInstance.getStepOrder());
        }
        return nextStepInstance;
    }

    private Long getNextStepInstanceId(List<Long> stepInstanceIdList, long currentStepInstanceId) {
        int currentStepIndex = stepInstanceIdList.indexOf(currentStepInstanceId);
        // 当前步骤为最后一个步骤
        if (currentStepIndex == stepInstanceIdList.size() - 1) {
            return null;
        }
        return stepInstanceIdList.get(currentStepIndex + 1);
    }

    private void startStep(StepInstanceBaseDTO stepInstance) {
        taskInstanceService.updateTaskCurrentStepId(stepInstance.getTaskInstanceId(), stepInstance.getId());
        taskExecuteMQEventDispatcher.dispatchStepEvent(StepEvent.startStep(stepInstance.getId(),
            stepInstance.isRollingStep() ? stepInstance.getBatch() + 1 : null));
    }

    private void callback(TaskInstanceDTO taskInstance, long taskInstanceId, int taskStatus, long currentStepId,
                          int stepStatus) {
        if (taskInstance.getCallbackUrl() != null) {
            JobCallbackDTO callback = new JobCallbackDTO();
            callback.setId(taskInstanceId);
            callback.setStatus(taskStatus);
            callback.setCallbackUrl(taskInstance.getCallbackUrl());
            Collection<JobCallbackDTO.StepInstanceStatus> stepInstanceList = Lists.newArrayList();
            JobCallbackDTO.StepInstanceStatus stepInstance = new JobCallbackDTO.StepInstanceStatus();
            stepInstance.setId(currentStepId);
            stepInstance.setStatus(stepStatus);
            stepInstanceList.add(stepInstance);
            callback.setStepInstances(stepInstanceList);
            taskExecuteMQEventDispatcher.sendCallback(callback);
        }
    }

}
