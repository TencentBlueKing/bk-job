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
import com.tencent.bk.job.execute.common.util.TaskCostCalculator;
import com.tencent.bk.job.execute.engine.consts.StepActionEnum;
import com.tencent.bk.job.execute.engine.listener.event.EventSource;
import com.tencent.bk.job.execute.engine.listener.event.JobEvent;
import com.tencent.bk.job.execute.engine.listener.event.StepEvent;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.NotifyService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 人工确认步骤事件处理
 */
@Component
@Slf4j
public class ConfirmStepEventHandler implements StepEventHandler {

    private final TaskInstanceService taskInstanceService;
    private final TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;
    private final NotifyService notifyService;

    @Autowired
    public ConfirmStepEventHandler(TaskInstanceService taskInstanceService,
                                   TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
                                   NotifyService notifyService) {
        this.taskInstanceService = taskInstanceService;
        this.taskExecuteMQEventDispatcher = taskExecuteMQEventDispatcher;
        this.notifyService = notifyService;
    }

    @Override
    public void handleEvent(StepEvent stepEvent,
                            StepInstanceDTO stepInstance) {
        long stepInstanceId = stepEvent.getStepInstanceId();
        try {
            StepActionEnum action = StepActionEnum.valueOf(stepEvent.getAction());

            switch (action) {
                case START:
                    executeConfirmStep(stepInstance);
                    break;
                case CONFIRM_TERMINATE:
                    confirmStepTerminate(stepInstance);
                    break;
                case CONFIRM_RESTART:
                    confirmStepRestart(stepInstance);
                    break;
                case CONFIRM_CONTINUE:
                    confirmStepContinue(stepInstance);
                    break;
                default:
                    log.error("Unhandled step event: {}", stepEvent);
            }
        } catch (Throwable e) {
            String errorMsg = "Handling step event error,stepInstanceId:" + stepInstanceId;
            log.error(errorMsg, e);
        }
    }

    private void confirmStepTerminate(StepInstanceDTO stepInstance) {
        long stepInstanceId = stepInstance.getId();
        log.info("Confirm step terminate, stepInstanceId={}", stepInstanceId);

        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(stepInstance.getTaskInstanceId());
        if (RunStatusEnum.WAITING_USER == stepInstance.getStatus()) {
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
        log.info("Confirm step restart, stepInstanceId={}", stepInstanceId);

        if (RunStatusEnum.CONFIRM_TERMINATED == stepInstance.getStatus()) {
            executeConfirmStep(stepInstance);
        } else {
            log.warn("Unsupported step instance status for confirm-step-restart action, stepInstanceId:{}, status:{}"
                , stepInstanceId, stepInstance.getStatus());
        }
    }

    /**
     * 人工确认步骤
     */
    private void executeConfirmStep(StepInstanceBaseDTO stepInstance) {
        long stepInstanceId = stepInstance.getId();
        long taskInstanceId = stepInstance.getTaskInstanceId();

        // 只有“未执行”和“确认终止”状态的，才可以重新执行人工确认步骤
        if (RunStatusEnum.BLANK == stepInstance.getStatus()
            || RunStatusEnum.CONFIRM_TERMINATED == stepInstance.getStatus()) {
            // 发送页面确认信息
            TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(taskInstanceId);
            String stepOperator = stepInstance.getOperator();

            if (StringUtils.isBlank(stepOperator)) {
                log.info("The operator is empty, continue run step! stepInstanceId={}", stepInstanceId);
                stepOperator = taskInstance.getOperator();
                stepInstance.setOperator(stepOperator);
            }
            taskInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.WAITING_USER,
                System.currentTimeMillis(), null, null);
            taskInstanceService.updateTaskStatus(taskInstanceId, RunStatusEnum.WAITING_USER.getValue());
            notifyService.asyncSendMQConfirmNotification(taskInstance, stepInstance);
        } else {
            log.warn("Unsupported step instance run status for executing confirm step, stepInstanceId={}, status={}",
                stepInstanceId, stepInstance.getStatus());
        }
    }

    private void confirmStepContinue(StepInstanceBaseDTO stepInstance) {
        long taskInstanceId = stepInstance.getTaskInstanceId();
        long stepInstanceId = stepInstance.getId();
        log.info("Confirm step continue, stepInstanceId={}", stepInstanceId);

        RunStatusEnum stepStatus = stepInstance.getStatus();
        if (RunStatusEnum.WAITING_USER == stepStatus) {
            taskInstanceService.updateTaskStatus(taskInstanceId, RunStatusEnum.RUNNING.getValue());
            long endTime = DateUtils.currentTimeMillis();
            long totalTime = TaskCostCalculator.calculate(stepInstance.getStartTime(), endTime,
                stepInstance.getTotalTime());
            // 人工确认通过，该步骤状态标识为成功；终止成功的步骤保持状态不变
            taskInstanceService.updateStepExecutionInfo(stepInstanceId, RunStatusEnum.SUCCESS, null, endTime,
                totalTime);
            taskExecuteMQEventDispatcher.dispatchJobEvent(
                JobEvent.refreshJob(taskInstanceId, EventSource.buildStepEventSource(stepInstanceId)));
        } else {
            log.warn("Unsupported step instance status for confirm-step-continue step action, stepInstanceId:{}, " +
                "status:{}", stepInstanceId, stepInstance.getStatus());
        }
    }
}
