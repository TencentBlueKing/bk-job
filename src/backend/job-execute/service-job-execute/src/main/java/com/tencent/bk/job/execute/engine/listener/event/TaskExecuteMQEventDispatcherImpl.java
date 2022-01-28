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

package com.tencent.bk.job.execute.engine.listener.event;

import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.engine.consts.StepActionEnum;
import com.tencent.bk.job.execute.engine.message.CallbackProcessor;
import com.tencent.bk.job.execute.engine.message.GseTaskProcessor;
import com.tencent.bk.job.execute.engine.message.NotifyMsgProcessor;
import com.tencent.bk.job.execute.engine.message.StepProcessor;
import com.tencent.bk.job.execute.engine.message.TaskProcessor;
import com.tencent.bk.job.execute.engine.message.TaskResultHandleResumeProcessor;
import com.tencent.bk.job.execute.engine.model.JobCallbackDTO;
import com.tencent.bk.job.execute.model.TaskNotifyDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class TaskExecuteMQEventDispatcherImpl implements TaskExecuteMQEventDispatcher {
    /**
     * 消息通道-作业
     */
    private final MessageChannel taskOutput;
    /**
     * 消息通道-步骤
     */
    private final MessageChannel stepOutput;
    /**
     * 消息通道-GSE任务
     */
    private final MessageChannel gseTaskOutput;

    /**
     * 消息通道-消息通知
     */
    private final MessageChannel notifyMsgOutput;
    /**
     * 消息通道-作业执行完成回调
     */
    private final MessageChannel callbackOutput;

    /**
     * 消息通道-作业执行完成回调
     */
    private final MessageChannel resultHandleTaskResumeOutput;

    @Autowired
    public TaskExecuteMQEventDispatcherImpl(@Qualifier(TaskProcessor.OUTPUT) MessageChannel taskOutput,
                                            @Qualifier(StepProcessor.OUTPUT) MessageChannel stepOutput,
                                            @Qualifier(GseTaskProcessor.OUTPUT) MessageChannel gseTaskOutput,
                                            @Qualifier(NotifyMsgProcessor.OUTPUT) MessageChannel notifyMsgOutput,
                                            @Qualifier(CallbackProcessor.OUTPUT) MessageChannel callbackOutput,
                                            @Qualifier(TaskResultHandleResumeProcessor.OUTPUT) MessageChannel resultHandleTaskResumeOutput) {
        this.taskOutput = taskOutput;
        this.stepOutput = stepOutput;
        this.gseTaskOutput = gseTaskOutput;
        this.notifyMsgOutput = notifyMsgOutput;
        this.callbackOutput = callbackOutput;
        this.resultHandleTaskResumeOutput = resultHandleTaskResumeOutput;
    }

    public void dispatchJobEvent(JobEvent jobEvent) {
        log.info("Begin to dispatch job event, event: {}", jobEvent);
        taskOutput.send(MessageBuilder.withPayload(jobEvent).build());
        log.info("Dispatch job event successfully, event: {}", jobEvent);
    }

    @Override
    public void dispatchStepEvent(StepEvent stepEvent) {
        log.info("Begin to dispatch step event, event: {}", stepEvent);
        stepOutput.send(MessageBuilder.withPayload(stepEvent).build());
        log.info("Dispatch step event successfully, event: {}", stepEvent);
    }

    @Override
    public void dispatchGseTaskEvent(GseTaskEvent gseTaskEvent) {
        log.info("Begin to dispatch gse task event, event: {}", gseTaskEvent);
        gseTaskOutput.send(MessageBuilder.withPayload(gseTaskEvent).build());
        log.info("Dispatch gse task event successfully, event: {}", gseTaskEvent);
    }

    public void dispatchResultHandleTaskResumeEvent(GseTaskResultHandleTaskResumeEvent event) {
        log.info("Begin to dispatch gse task result handle resume event, event: {}", event);
        resultHandleTaskResumeOutput.send(MessageBuilder.withPayload(event).build());
        log.info("Dispatch gse task result handle resume event successfully, event: {}", event);
    }

    @Override
    public void asyncSendNotifyMsg(TaskNotifyDTO notification) {
        log.info("Async send notification event:{}", JsonUtils.toJson(notification));
        notifyMsgOutput.send(MessageBuilder.withPayload(JsonUtils.toJson(notification)).build());
    }

    @Override
    public void sendCallback(JobCallbackDTO jobCallback) {
        log.info("Async invoke callback url, callback:{}", JsonUtils.toJson(jobCallback));
        callbackOutput.send(MessageBuilder.withPayload(JsonUtils.toJson(jobCallback)).build());
    }

    @Override
    public void refreshStep(long stepInstanceId) {
        log.info("Begin to send refresh step event, stepInstanceId: {}", stepInstanceId);
        StepEvent stepEvent = new StepEvent();
        stepEvent.setStepInstanceId(stepInstanceId);
        stepEvent.setAction(StepActionEnum.REFRESH.getValue());
        stepEvent.setTime(LocalDateTime.now());
        stepOutput.send(MessageBuilder.withPayload(stepEvent).build());
        log.info("Send refresh step event successfully, event: {}", stepEvent);
    }
}
