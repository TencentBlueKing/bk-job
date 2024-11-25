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
import com.tencent.bk.job.execute.common.exception.MessageHandleException;
import com.tencent.bk.job.execute.common.exception.MessageHandlerUnavailableException;
import com.tencent.bk.job.execute.engine.consts.GseTaskActionEnum;
import com.tencent.bk.job.execute.engine.executor.GseTaskManager;
import com.tencent.bk.job.execute.engine.listener.event.EventSource;
import com.tencent.bk.job.execute.engine.listener.event.GseTaskEvent;
import com.tencent.bk.job.execute.engine.listener.event.JobMessage;
import com.tencent.bk.job.execute.engine.listener.event.StepEvent;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.monitor.metrics.GseTasksExceptionCounter;
import com.tencent.bk.job.execute.service.GseTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * 执行引擎事件处理-GSE任务
 */
@Component
@Slf4j
public class GseTaskListener extends BaseJobMqListener {
    private final GseTaskManager gseTaskManager;
    private final GseTaskService gseTaskService;
    private final GseTasksExceptionCounter gseTasksExceptionCounter;
    private final TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;

    @Autowired
    public GseTaskListener(GseTaskManager gseTaskManager,
                           GseTaskService gseTaskService,
                           GseTasksExceptionCounter gseTasksExceptionCounter,
                           TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher) {
        this.gseTaskManager = gseTaskManager;
        this.gseTaskService = gseTaskService;
        this.gseTasksExceptionCounter = gseTasksExceptionCounter;
        this.taskExecuteMQEventDispatcher = taskExecuteMQEventDispatcher;
    }

    /**
     * 处理GSE任务相关的事件
     *
     * @param message 消息
     */
    @Override
    public void handleEvent(Message<? extends JobMessage> message) {
        GseTaskEvent gseTaskEvent = (GseTaskEvent) message.getPayload();
        GseTaskDTO gseTask = null;
        try {
            log.info("Handle gse task event: {}, duration: {}ms", gseTaskEvent, gseTaskEvent.duration());
            gseTask = gseTaskService.getGseTask(gseTaskEvent.getJobInstanceId(), gseTaskEvent.getGseTaskId());
            String requestId = gseTaskEvent.getRequestId();
            int action = gseTaskEvent.getAction();
            if (GseTaskActionEnum.START.getValue() == action) {
                gseTaskManager.startTask(gseTask, requestId);
            } else if (GseTaskActionEnum.STOP.getValue() == action) {
                gseTaskManager.stopTask(gseTask);
            } else {
                log.error("Error gse task action:{}", action);
            }
        } catch (Throwable e) {
            String errorMsg = "Handling gse task event error, event:" + gseTaskEvent;
            log.error(errorMsg, e);
            handleException(gseTask, e);
        }
    }

    private void handleException(GseTaskDTO gseTask, Throwable e) throws MessageHandleException {
        // 服务关闭，消息被拒绝，重新入队列
        if (e instanceof MessageHandlerUnavailableException) {
            throw (MessageHandlerUnavailableException) e;
        }

        updateGseTaskResult(gseTask);

        gseTasksExceptionCounter.increment();

        taskExecuteMQEventDispatcher.dispatchStepEvent(
            StepEvent.refreshStep(
                gseTask.getTaskInstanceId(),
                gseTask.getStepInstanceId(),
                EventSource.buildGseTaskEventSource(
                    gseTask.getTaskInstanceId(),
                    gseTask.getStepInstanceId(),
                    gseTask.getExecuteCount(),
                    gseTask.getBatch(),
                    gseTask.getId()
                )
            )
        );
    }

    private void updateGseTaskResult(GseTaskDTO gseTask) {
        gseTask.setStatus(RunStatusEnum.ABNORMAL_STATE.getValue());
        if (gseTask.getStartTime() == null) {
            gseTask.setStartTime(System.currentTimeMillis());
        }
        long endTime = System.currentTimeMillis();
        gseTask.setEndTime(endTime);
        gseTask.setTotalTime(endTime - gseTask.getStartTime());
        gseTaskService.updateGseTask(gseTask);
    }
}
