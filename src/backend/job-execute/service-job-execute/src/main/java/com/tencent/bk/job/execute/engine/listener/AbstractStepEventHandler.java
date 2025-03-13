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

import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.util.TaskCostCalculator;
import com.tencent.bk.job.execute.engine.listener.event.EventSource;
import com.tencent.bk.job.execute.engine.listener.event.JobEvent;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import lombok.extern.slf4j.Slf4j;

/**
 * 步骤事件处理-抽象实现
 */
@Slf4j
public abstract class AbstractStepEventHandler implements StepEventHandler {

    protected final TaskInstanceService taskInstanceService;
    protected final StepInstanceService stepInstanceService;
    protected final TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;


    public AbstractStepEventHandler(TaskInstanceService taskInstanceService,
                                    StepInstanceService stepInstanceService,
                                    TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher) {
        this.taskInstanceService = taskInstanceService;
        this.stepInstanceService = stepInstanceService;
        this.taskExecuteMQEventDispatcher = taskExecuteMQEventDispatcher;
    }

    /**
     * 当步骤处理异常，安全地结束作业步骤执行的调度
     */
    protected void safelyFinishStepWhenCaughtException(StepInstanceDTO stepInstance) {
        try {
            finishStepWithAbnormalState(stepInstance);
            taskExecuteMQEventDispatcher.dispatchJobEvent(
                JobEvent.refreshJob(stepInstance.getTaskInstanceId(),
                    EventSource.buildStepEventSource(stepInstance.getTaskInstanceId(), stepInstance.getId())));
        } catch (Throwable e) {
            log.error("Finish step instance safely caught exception", e);
        }
    }

    protected void finishStepWithAbnormalState(StepInstanceDTO stepInstance) {
        finishStep(stepInstance, RunStatusEnum.ABNORMAL_STATE);
    }

    protected void finishStep(StepInstanceDTO stepInstance, RunStatusEnum status) {
        long endTime = System.currentTimeMillis();
        if (!RunStatusEnum.isFinishedStatus(stepInstance.getStatus())) {
            long totalTime = TaskCostCalculator.calculate(stepInstance.getStartTime(), endTime,
                stepInstance.getTotalTime());
            stepInstanceService.updateStepExecutionInfo(
                stepInstance.getTaskInstanceId(),
                stepInstance.getId(),
                status,
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

    protected void onAbandonState(StepInstanceDTO stepInstance) {
        finishStep(stepInstance, RunStatusEnum.ABANDONED);
    }


}
