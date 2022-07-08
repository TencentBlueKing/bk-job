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
import org.springframework.stereotype.Component;

/**
 * 作业执行MQ事件分发
 */
@Component
@Slf4j
public class TaskExecuteMQEventDispatcher {
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
    public TaskExecuteMQEventDispatcher(@Qualifier(TaskProcessor.OUTPUT) MessageChannel taskOutput,
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

    /**
     * 分发作业事件
     *
     * @param jobEvent 作业事件
     */
    public void dispatchJobEvent(JobEvent jobEvent) {
        log.info("Begin to dispatch job event, event: {}", jobEvent);
        taskOutput.send(MessageBuilder.withPayload(jobEvent).build());
        log.info("Dispatch job event successfully, event: {}", jobEvent);
    }

    /**
     * 分发步骤事件
     *
     * @param stepEvent 步骤事件
     */
    public void dispatchStepEvent(StepEvent stepEvent) {
        log.info("Begin to dispatch step event, event: {}", stepEvent);
        stepOutput.send(MessageBuilder.withPayload(stepEvent).build());
        log.info("Dispatch step event successfully, event: {}", stepEvent);
    }

    /**
     * 分发GSE任务事件
     *
     * @param gseTaskEvent GSE任务事件
     */
    public void dispatchGseTaskEvent(GseTaskEvent gseTaskEvent) {
        log.info("Begin to dispatch gse task event, event: {}", gseTaskEvent);
        gseTaskOutput.send(MessageBuilder.withPayload(gseTaskEvent).build());
        log.info("Dispatch gse task event successfully, event: {}", gseTaskEvent);
    }

    /**
     * 分发结果处理任务恢复事件
     *
     * @param event 结果处理任务恢复事件
     */
    public void dispatchResultHandleTaskResumeEvent(ResultHandleTaskResumeEvent event) {
        log.info("Begin to dispatch gse task result handle resume event, event: {}", event);
        resultHandleTaskResumeOutput.send(MessageBuilder.withPayload(event).build());
        log.info("Dispatch gse task result handle resume event successfully, event: {}", event);
    }

    /**
     * 异步发送消息通知事件
     *
     * @param notification 消息内容
     */
    public void asyncSendNotifyMsg(TaskNotifyDTO notification) {
        log.info("Async send notification event:{}", JsonUtils.toJson(notification));
        notifyMsgOutput.send(MessageBuilder.withPayload(JsonUtils.toJson(notification)).build());
    }

    /**
     * 发送回调信息事件
     *
     * @param jobCallback 回调内容
     */
    public void sendCallback(JobCallbackDTO jobCallback) {
        log.info("Async invoke callback url, callback:{}", JsonUtils.toJson(jobCallback));
        callbackOutput.send(MessageBuilder.withPayload(JsonUtils.toJson(jobCallback)).build());
    }
}
