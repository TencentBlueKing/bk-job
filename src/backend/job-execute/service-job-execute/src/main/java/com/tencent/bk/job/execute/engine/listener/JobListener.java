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
import com.tencent.bk.job.common.constant.RollingModeEnum;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.util.TaskCostCalculator;
import com.tencent.bk.job.execute.engine.consts.JobActionEnum;
import com.tencent.bk.job.execute.engine.listener.event.JobEvent;
import com.tencent.bk.job.execute.engine.listener.event.StepEvent;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.model.JobCallbackDTO;
import com.tencent.bk.job.execute.model.RollingConfigDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.db.RollingConfigDetailDO;
import com.tencent.bk.job.execute.service.NotifyService;
import com.tencent.bk.job.execute.service.RollingConfigService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.statistics.StatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * 执行引擎事件处理-作业
 */
@Component
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
    public void handleEvent(JobEvent jobEvent) {
        log.info("Handle job event, event: {}, duration: {}ms", jobEvent, jobEvent.duration());
        long jobInstanceId = jobEvent.getJobInstanceId();
        JobActionEnum action = JobActionEnum.valueOf(jobEvent.getAction());
        try {
            TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(jobInstanceId);
            switch (action) {
                case START:
                    startJob(taskInstance);
                    break;
                case STOP:
                    stopJob(taskInstance);
                    break;
                case RESTART:
                    restartJob(taskInstance);
                    break;
                case REFRESH:
                    refreshJob(taskInstance);
                    break;
                default:
                    log.error("Invalid job action: {}", action);
            }
        } catch (Throwable e) {
            String errorMsg = "Handle job event error, jobInstanceId=" + jobInstanceId;
            log.error(errorMsg, e);
        }
    }

    /**
     * 启动作业
     *
     * @param taskInstance 作业实例
     */
    private void startJob(TaskInstanceDTO taskInstance) {
        long jobInstanceId = taskInstance.getId();
        // 首先验证作业的状态，只有状态为“未执行”的作业可以启动
        if (RunStatusEnum.BLANK == taskInstance.getStatus()) {
            StepInstanceBaseDTO stepInstance = taskInstanceService.getFirstStepInstance(jobInstanceId);
            taskInstanceService.updateTaskExecutionInfo(jobInstanceId, RunStatusEnum.RUNNING, stepInstance.getId(),
                DateUtils.currentTimeMillis(), null, null);
            startStep(stepInstance);

            // 触发任务开始统计分析
            statisticsService.updateStartJobStatistics(taskInstance);
        } else {
            log.error("Unsupported job instance run status for starting job, jobInstanceId={}, status={}",
                jobInstanceId, taskInstance.getStatus());
        }
    }

    /**
     * 强制终止作业
     *
     * @param taskInstance 作业实例
     */
    private void stopJob(TaskInstanceDTO taskInstance) {
        long jobInstanceId = taskInstance.getId();
        RunStatusEnum taskStatus = taskInstance.getStatus();

        if (RunStatusEnum.RUNNING == taskStatus || RunStatusEnum.WAITING_USER == taskStatus) {
            taskInstanceService.updateTaskStatus(jobInstanceId, RunStatusEnum.STOPPING.getValue());
            long currentStepInstanceId = taskInstance.getCurrentStepInstanceId();
            taskExecuteMQEventDispatcher.dispatchStepEvent(StepEvent.stopStep(currentStepInstanceId));
        } else {
            log.warn("Unsupported job instance run status for stop task, jobInstanceId={}, status={}",
                jobInstanceId, taskInstance.getStatus());
        }
    }

    /**
     * 重头执行作业
     *
     * @param taskInstance 作业实例
     */
    private void restartJob(TaskInstanceDTO taskInstance) {
        long jobInstanceId = taskInstance.getId();
        RunStatusEnum taskStatus = taskInstance.getStatus();
        // 验证作业状态，只有“执行失败”和“等待用户”的作业可以重头执行
        if (RunStatusEnum.WAITING_USER == taskStatus || RunStatusEnum.FAIL == taskStatus) {

            // 重置作业状态
            taskInstanceService.resetTaskStatus(jobInstanceId);
            taskInstanceService.addStepInstanceExecuteCount(jobInstanceId);

            // 重置作业下步骤的状态、开始时间和结束时间等。
            List<Long> stepInstanceIdList = taskInstanceService.getTaskStepIdList(jobInstanceId);
            for (long stepInstanceId : stepInstanceIdList) {
                taskInstanceService.updateStepStatus(stepInstanceId, RunStatusEnum.BLANK.getValue());
                taskInstanceService.resetStepStatus(stepInstanceId);
            }

            taskExecuteMQEventDispatcher.dispatchJobEvent(JobEvent.startJob(jobInstanceId));
        } else {
            log.warn("Unsupported job instance run status for restart task, jobInstanceId={}, status={}",
                jobInstanceId, taskInstance.getStatus());
        }
    }

    /**
     * 作业状态流转
     *
     * @param taskInstance 作业实例
     */
    private void refreshJob(TaskInstanceDTO taskInstance) {
        long jobInstanceId = taskInstance.getId();
        RunStatusEnum taskStatus = taskInstance.getStatus();

        long currentStepInstanceId = taskInstance.getCurrentStepInstanceId();
        StepInstanceBaseDTO currentStepInstance = taskInstanceService.getBaseStepInstance(currentStepInstanceId);
        RunStatusEnum stepStatus = currentStepInstance.getStatus();

        // 验证作业状态，只有'正在执行'、'强制终止中'的作业可以刷新状态进入下一步或者结束
        if (RunStatusEnum.STOPPING == taskStatus) {
            // 非正在执行的步骤可以直接终止
            if (RunStatusEnum.RUNNING != stepStatus) {
                finishJob(taskInstance, currentStepInstance, RunStatusEnum.STOP_SUCCESS);
            } else {
                log.error("Unsupported job instance run status for refresh task, jobInstanceId={}, taskStatus={}, " +
                    "stepStatus: {}", jobInstanceId, taskStatus, stepStatus);
            }
        } else if (RunStatusEnum.RUNNING == taskStatus) {
            // 步骤状态为成功、跳过、设为忽略错误、滚动等待，可以进入下一步
            if (RunStatusEnum.SUCCESS == stepStatus
                || RunStatusEnum.SKIPPED == stepStatus
                || RunStatusEnum.IGNORE_ERROR == stepStatus
                || RunStatusEnum.ROLLING_WAITING == stepStatus) {
                nextStep(taskInstance, currentStepInstance);
            } else if (RunStatusEnum.FAIL == stepStatus
                || RunStatusEnum.ABNORMAL_STATE == stepStatus
                || RunStatusEnum.ABANDONED == stepStatus) {
                // 步骤失败，任务结束
                finishJob(taskInstance, currentStepInstance, stepStatus);
            } else {
                log.warn("Unsupported job instance run status for refresh task, jobInstanceId={}, status={}",
                    jobInstanceId, taskInstance.getStatus());
            }
        } else {
            log.warn("Unsupported job instance run status for refresh task, jobInstanceId={}, status={}",
                jobInstanceId, taskInstance.getStatus());
        }
    }

    private void nextStep(TaskInstanceDTO taskInstance, StepInstanceBaseDTO currentStepInstance) {
        if (currentStepInstance.isRollingStep()) {
            RollingConfigDTO taskInstanceRollingConfig =
                rollingConfigService.getRollingConfig(currentStepInstance.getRollingConfigId());
            RollingConfigDetailDO rollingConfig = taskInstanceRollingConfig.getConfigDetail();
            StepInstanceBaseDTO nextStepInstance = getNextStepInstance(taskInstance, currentStepInstance,
                rollingConfig);
            if (nextStepInstance == null) {
                finishJob(taskInstance, currentStepInstance, RunStatusEnum.SUCCESS);
            } else {
                if (rollingConfig.getMode().equals(RollingModeEnum.MANUAL.getValue())
                    && rollingConfig.isFirstRollingStep(nextStepInstance.getId())) {
                    log.info("Manual mode for rolling step[{}], pause and wait for user confirmation",
                        nextStepInstance.getId());
                    taskInstanceService.updateStepStatus(nextStepInstance.getId(),
                        RunStatusEnum.WAITING_USER.getValue());
                    taskInstanceService.updateTaskStatus(taskInstance.getId(), RunStatusEnum.WAITING_USER.getValue());
                } else {
                    // 执行下一步骤
                    startStep(nextStepInstance);
                }
            }
        } else {
            StepInstanceBaseDTO nextStepInstance = getNextStepInstance(taskInstance, currentStepInstance, null);
            if (nextStepInstance == null) {
                finishJob(taskInstance, currentStepInstance, RunStatusEnum.SUCCESS);
            } else {
                // 执行下一步骤
                startStep(nextStepInstance);
            }
        }

    }

    private void finishJob(TaskInstanceDTO taskInstance,
                           StepInstanceBaseDTO stepInstance,
                           RunStatusEnum jobStatus) {
        long jobInstanceId = taskInstance.getId();
        long stepInstanceId = stepInstance.getId();
        Long endTime = DateUtils.currentTimeMillis();
        long totalTime = TaskCostCalculator.calculate(taskInstance.getStartTime(), endTime,
            taskInstance.getTotalTime());
        taskInstance.setEndTime(endTime);
        taskInstance.setTotalTime(totalTime);
        taskInstance.setStatus(jobStatus);
        taskInstanceService.updateTaskExecutionInfo(jobInstanceId, jobStatus, null, null, endTime, totalTime);

        // 作业执行结果消息通知
        if (RunStatusEnum.SUCCESS == jobStatus || RunStatusEnum.IGNORE_ERROR == jobStatus) {
            notifyService.asyncSendMQSuccessTaskNotification(taskInstance, stepInstance);
        } else {
            notifyService.asyncSendMQFailTaskNotification(taskInstance, stepInstance);
        }

        // 触发作业结束统计分析
        statisticsService.updateEndJobStatistics(taskInstance);

        // 作业执行完成回调
        callback(taskInstance, jobInstanceId, jobStatus.getValue(), stepInstanceId, stepInstance.getStatus());
    }

    /**
     * 获取下一个执行的步骤实例
     *
     * @param taskInstance        作业实例
     * @param currentStepInstance 当前步骤实例
     * @param rollingConfig       滚动配置，仅当当前步骤为滚动步骤时才需要传入
     * @return 步骤实例;如果当前步骤已经是最后一个步骤，那么返回null
     */
    private StepInstanceBaseDTO getNextStepInstance(TaskInstanceDTO taskInstance,
                                                    StepInstanceBaseDTO currentStepInstance,
                                                    RollingConfigDetailDO rollingConfig) {
        StepInstanceBaseDTO nextStepInstance = null;
        if (currentStepInstance.isRollingStep()) {
            int currentBatch = currentStepInstance.getBatch();
            List<Long> includeStepInstanceIdList = rollingConfig.getIncludeStepInstanceIdList();
            boolean isLastRollingStep = rollingConfig.isLastRollingStep(currentStepInstance.getId());
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
        if (stepInstance.isRollingStep()) {
            taskInstanceService.updateStepStatus(stepInstance.getId(), RunStatusEnum.ROLLING_WAITING.getValue());
            taskExecuteMQEventDispatcher.dispatchStepEvent(StepEvent.startStep(stepInstance.getId(),
                stepInstance.getBatch() + 1));
        } else {
            taskInstanceService.updateStepStatus(stepInstance.getId(), RunStatusEnum.BLANK.getValue());
            taskExecuteMQEventDispatcher.dispatchStepEvent(StepEvent.startStep(stepInstance.getId(), null));
        }
    }

    private void callback(TaskInstanceDTO taskInstance, long jobInstanceId, int taskStatus, long currentStepId,
                          RunStatusEnum stepStatus) {
        if (StringUtils.isNotBlank(taskInstance.getCallbackUrl())) {
            JobCallbackDTO callback = new JobCallbackDTO();
            callback.setId(jobInstanceId);
            callback.setStatus(taskStatus);
            callback.setCallbackUrl(taskInstance.getCallbackUrl());
            Collection<JobCallbackDTO.StepInstanceStatus> stepInstanceList = Lists.newArrayList();
            JobCallbackDTO.StepInstanceStatus stepInstance = new JobCallbackDTO.StepInstanceStatus();
            stepInstance.setId(currentStepId);
            stepInstance.setStatus(stepStatus.getValue());
            stepInstanceList.add(stepInstance);
            callback.setStepInstances(stepInstanceList);
            taskExecuteMQEventDispatcher.dispatchCallbackMsg(callback);
        }
    }

}
