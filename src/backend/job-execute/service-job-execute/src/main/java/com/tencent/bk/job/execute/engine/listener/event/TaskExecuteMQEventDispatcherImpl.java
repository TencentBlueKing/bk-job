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
import com.tencent.bk.job.execute.engine.consts.GseStepActionEnum;
import com.tencent.bk.job.execute.engine.consts.JobActionEnum;
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
import java.util.UUID;

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

    @Override
    public void startTask(long taskInstanceId) {
        log.info("Begin to send start task control message, taskInstanceId={}", taskInstanceId);
        JobEvent msg = new JobEvent();
        msg.setTaskInstanceId(taskInstanceId);
        msg.setAction(JobActionEnum.START.getValue());
        msg.setTime(LocalDateTime.now());
        taskOutput.send(MessageBuilder.withPayload(msg).build());
        log.info("Send start task control message successfully, taskInstanceId={}", taskInstanceId);
    }

    @Override
    public void stopTask(long taskInstanceId) {
        log.info("Begin to send stop task control message, taskInstanceId={}", taskInstanceId);
        JobEvent msg = new JobEvent();
        msg.setTaskInstanceId(taskInstanceId);
        msg.setAction(JobActionEnum.STOP.getValue());
        msg.setTime(LocalDateTime.now());
        taskOutput.send(MessageBuilder.withPayload(msg).build());
        log.info("Send stop task control message successfully, taskInstanceId={}", taskInstanceId);
    }

    @Override
    public void restartTask(long taskInstanceId) {
        log.info("Begin to send restart task control message, taskInstanceId={}", taskInstanceId);
        JobEvent msg = new JobEvent();
        msg.setTaskInstanceId(taskInstanceId);
        msg.setAction(JobActionEnum.RESTART.getValue());
        msg.setTime(LocalDateTime.now());
        taskOutput.send(MessageBuilder.withPayload(msg).build());
        log.info("Send restart task control message successfully, taskInstanceId={}", taskInstanceId);
    }

    @Override
    public void refreshTask(long taskInstanceId) {
        log.info("Begin to send refresh task control message, taskInstanceId={}", taskInstanceId);
        JobEvent msg = new JobEvent();
        msg.setTaskInstanceId(taskInstanceId);
        msg.setAction(JobActionEnum.REFRESH.getValue());
        msg.setTime(LocalDateTime.now());
        taskOutput.send(MessageBuilder.withPayload(msg).build());
        log.info("Send refresh task control message successfully, taskInstanceId={}", taskInstanceId);
    }

    @Override
    public void ignoreStepError(long stepInstanceId) {
        log.info("Begin to send ignore-error step control message, stepInstanceId={}", stepInstanceId);
        StepEvent msg = new StepEvent();
        msg.setStepInstanceId(stepInstanceId);
        msg.setAction(StepActionEnum.IGNORE_ERROR.getValue());
        msg.setTime(LocalDateTime.now());
        stepOutput.send(MessageBuilder.withPayload(msg).build());
        log.info("Send ignore-error step control message successfully, stepInstanceId={}", stepInstanceId);
    }

    @Override
    public void nextStep(long stepInstanceId) {
        log.info("Begin to send next-step step control message, stepInstanceId={}", stepInstanceId);
        StepEvent msg = new StepEvent();
        msg.setStepInstanceId(stepInstanceId);
        msg.setAction(StepActionEnum.NEXT_STEP.getValue());
        msg.setTime(LocalDateTime.now());
        stepOutput.send(MessageBuilder.withPayload(msg).build());
        log.info("Send next-step step control message successfully, stepInstanceId={}", stepInstanceId);
    }

    @Override
    public void confirmStepContinue(long stepInstanceId) {
        log.info("Begin to send confirm-continue step control message, stepInstanceId={}", stepInstanceId);
        StepEvent msg = new StepEvent();
        msg.setStepInstanceId(stepInstanceId);
        msg.setAction(StepActionEnum.CONFIRM_CONTINUE.getValue());
        msg.setTime(LocalDateTime.now());
        stepOutput.send(MessageBuilder.withPayload(msg).build());
        log.info("Send confirm-continue step control message successfully, stepInstanceId={}", stepInstanceId);
    }

    @Override
    public void confirmStepTerminate(long stepInstanceId) {
        log.info("Begin to send confirm-terminate step control message, stepInstanceId={}", stepInstanceId);
        StepEvent msg = new StepEvent();
        msg.setStepInstanceId(stepInstanceId);
        msg.setAction(StepActionEnum.CONFIRM_TERMINATE.getValue());
        msg.setTime(LocalDateTime.now());
        stepOutput.send(MessageBuilder.withPayload(msg).build());
        log.info("Send confirm-terminate step control message successfully, stepInstanceId={}", stepInstanceId);
    }

    @Override
    public void confirmStepRestart(long stepInstanceId) {
        log.info("Begin to send confirm-restart step control message, stepInstanceId={}", stepInstanceId);
        StepEvent msg = new StepEvent();
        msg.setStepInstanceId(stepInstanceId);
        msg.setAction(StepActionEnum.CONFIRM_RESTART.getValue());
        msg.setTime(LocalDateTime.now());
        stepOutput.send(MessageBuilder.withPayload(msg).build());
        log.info("Send confirm-restart step control message successfully, stepInstanceId={}", stepInstanceId);
    }

    @Override
    public void startStep(long stepInstanceId) {
        StepEvent event = new StepEvent();
        event.setStepInstanceId(stepInstanceId);
        event.setAction(StepActionEnum.START.getValue());
        event.setTime(LocalDateTime.now());
        stepOutput.send(MessageBuilder.withPayload(event).build());
        log.info("Send start step event successfully, event={}", event);
    }

    @Override
    public void skipStep(long stepInstanceId) {
        log.info("Begin to send skip step control message successfully, stepInstanceId={}", stepInstanceId);
        StepEvent msg = new StepEvent();
        msg.setStepInstanceId(stepInstanceId);
        msg.setAction(StepActionEnum.SKIP.getValue());
        msg.setTime(LocalDateTime.now());
        stepOutput.send(MessageBuilder.withPayload(msg).build());
        log.info("Send skip step control message successfully, stepInstanceId={}", stepInstanceId);
    }

    @Override
    public void stopStep(long stepInstanceId) {
        log.info("Begin to send stop step control message successfully, stepInstanceId={}", stepInstanceId);
        StepEvent msg = new StepEvent();
        msg.setStepInstanceId(stepInstanceId);
        msg.setAction(StepActionEnum.STOP.getValue());
        msg.setTime(LocalDateTime.now());
        stepOutput.send(MessageBuilder.withPayload(msg).build());
        log.info("Send stop step control message successfully, stepInstanceId={}", stepInstanceId);
    }

    @Override
    public void retryStepFail(long stepInstanceId) {
        log.info("Begin to send retry-step-fail step control message successfully, stepInstanceId={}", stepInstanceId);
        StepEvent msg = new StepEvent();
        msg.setStepInstanceId(stepInstanceId);
        msg.setAction(StepActionEnum.RETRY_FAIL.getValue());
        msg.setTime(LocalDateTime.now());
        stepOutput.send(MessageBuilder.withPayload(msg).build());
        log.info("Send retry-step-fail step control message successfully, stepInstanceId={}", stepInstanceId);
    }

    @Override
    public void retryStepAll(long stepInstanceId) {
        log.info("Begin to send retry-step-all step control message successfully, stepInstanceId={}", stepInstanceId);
        StepEvent msg = new StepEvent();
        msg.setStepInstanceId(stepInstanceId);
        msg.setAction(StepActionEnum.RETRY_ALL.getValue());
        msg.setTime(LocalDateTime.now());
        stepOutput.send(MessageBuilder.withPayload(msg).build());
        log.info("Send retry-step-all step control message successfully, stepInstanceId={}", stepInstanceId);
    }

    @Override
    public void continueGseFileStep(long stepInstanceId) {
        log.info("Begin to send continue-gse-file-step step control message successfully, stepInstanceId={}",
            stepInstanceId);
        StepEvent msg = new StepEvent();
        msg.setStepInstanceId(stepInstanceId);
        msg.setAction(StepActionEnum.CONTINUE_FILE_PUSH.getValue());
        msg.setTime(LocalDateTime.now());
        stepOutput.send(MessageBuilder.withPayload(msg).build());
        log.info("Send continue-gse-file-step step control message successfully, stepInstanceId={}", stepInstanceId);
    }

    @Override
    public void clearStep(long stepInstanceId) {
        log.info("Begin to send clear-step step control message successfully, stepInstanceId={}", stepInstanceId);
        StepEvent msg = new StepEvent();
        msg.setStepInstanceId(stepInstanceId);
        msg.setAction(StepActionEnum.CLEAR.getValue());
        msg.setTime(LocalDateTime.now());
        stepOutput.send(MessageBuilder.withPayload(msg).build());
        log.info("Send clear-step step control message successfully, stepInstanceId={}", stepInstanceId);
    }

    @Override
    public void startGseStep(long stepInstanceId) {
        log.info("Begin to send start gse step control message successfully, stepInstanceId={}", stepInstanceId);
        StepEvent msg = new StepEvent();
        msg.setStepInstanceId(stepInstanceId);
        msg.setAction(GseStepActionEnum.START.getValue());
        msg.setTime(LocalDateTime.now());
        msg.setRequestId(UUID.randomUUID().toString());
        gseTaskOutput.send(MessageBuilder.withPayload(msg).build());
        log.info("Send start gse step control message successfully, stepInstanceId={}", stepInstanceId);
    }

    @Override
    public void resumeGseStep(long stepInstanceId, int executeCount, String requestId) {
        log.info("Begin to send resume gse step control message successfully, stepInstanceId={}, executeCount={}, " +
                "requestId={}",
            stepInstanceId, executeCount, requestId);
        StepEvent msg = new StepEvent();
        msg.setStepInstanceId(stepInstanceId);
        msg.setExecuteCount(executeCount);
        msg.setAction(StepActionEnum.RESUME.getValue());
        msg.setTime(LocalDateTime.now());
        msg.setRequestId(requestId);
        resultHandleTaskResumeOutput.send(MessageBuilder.withPayload(msg).build());
        log.info("Send resume gse step control message successfully, stepInstanceId={}, executeCount={}, requestId={}",
            stepInstanceId, executeCount, requestId);
    }

    @Override
    public void retryGseStepFail(long stepInstanceId) {
        log.info("Begin to send retry gse step fail control message successfully, stepInstanceId={}", stepInstanceId);
        StepEvent msg = new StepEvent();
        msg.setStepInstanceId(stepInstanceId);
        msg.setAction(GseStepActionEnum.RETRY_FAIL.getValue());
        msg.setTime(LocalDateTime.now());
        msg.setRequestId(UUID.randomUUID().toString());
        gseTaskOutput.send(MessageBuilder.withPayload(msg).build());
        log.info("Send start gse step fail control message successfully, stepInstanceId={}", stepInstanceId);
    }

    @Override
    public void retryGseStepAll(long stepInstanceId) {
        log.info("Begin to send retry gse step all control message successfully, stepInstanceId={}", stepInstanceId);
        StepEvent msg = new StepEvent();
        msg.setStepInstanceId(stepInstanceId);
        msg.setAction(GseStepActionEnum.RETRY_ALL.getValue());
        msg.setTime(LocalDateTime.now());
        msg.setRequestId(UUID.randomUUID().toString());
        gseTaskOutput.send(MessageBuilder.withPayload(msg).build());
        log.info("Send start gse step all control message successfully, stepInstanceId={}", stepInstanceId);
    }

    @Override
    public void stopGseStep(long stepInstanceId) {
        log.info("Begin to send stop gse step control message successfully, stepInstanceId={}", stepInstanceId);
        StepEvent msg = new StepEvent();
        msg.setStepInstanceId(stepInstanceId);
        msg.setAction(GseStepActionEnum.STOP.getValue());
        msg.setTime(LocalDateTime.now());
        msg.setRequestId(UUID.randomUUID().toString());
        gseTaskOutput.send(MessageBuilder.withPayload(msg).build());
        log.info("Send stop gse step control message successfully, stepInstanceId={}", stepInstanceId);
    }

    @Override
    public void asyncSendNotifyMsg(TaskNotifyDTO notification) {
        log.info("Async send notification msg:{}", JsonUtils.toJson(notification));
        notifyMsgOutput.send(MessageBuilder.withPayload(JsonUtils.toJson(notification)).build());
    }

    @Override
    public void sendCallback(JobCallbackDTO jobCallback) {
        log.info("Async invoke callback url, callback:{}", JsonUtils.toJson(jobCallback));
        callbackOutput.send(MessageBuilder.withPayload(JsonUtils.toJson(jobCallback)).build());
    }

    @Override
    public void refreshStep(long stepInstanceId) {
        log.info("Begin to send refresh step event, stepInstanceId={}", stepInstanceId);
        StepEvent stepEvent = new StepEvent();
        stepEvent.setStepInstanceId(stepInstanceId);
        stepEvent.setAction(StepActionEnum.REFRESH.getValue());
        stepEvent.setTime(LocalDateTime.now());
        stepOutput.send(MessageBuilder.withPayload(stepEvent).build());
        log.info("Send refresh step event successfully, stepInstanceId={}", stepInstanceId);
    }
}
