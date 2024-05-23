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
import com.tencent.bk.job.execute.engine.model.JobCallbackDTO;
import com.tencent.bk.job.execute.model.TaskNotifyDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

/**
 * 作业执行MQ事件分发.
 * <p>
 * 基于Spring cloud stream + spring cloud function 实现。文档链接:
 * https://docs.spring.io/spring-cloud-stream/docs/3.1.0/reference/html/spring-cloud-stream.html#spring_cloud_function
 */
@Component
@Slf4j
public class TaskExecuteMQEventDispatcher {

    private final StreamBridge streamBridge;

    @Autowired
    public TaskExecuteMQEventDispatcher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    /**
     * 分发作业事件
     *
     * @param jobEvent 作业事件
     */
    public void dispatchJobEvent(JobEvent jobEvent) {
        log.info("Begin to dispatch job event, event: {}", jobEvent);
        String taskOutput = "task-out-0";
        streamBridge.send(taskOutput, jobEvent);
        log.info("Dispatch job event successfully, event: {}", jobEvent);
    }

    /**
     * 分发步骤事件
     *
     * @param stepEvent 步骤事件
     */
    public void dispatchStepEvent(StepEvent stepEvent) {
        log.info("Begin to dispatch step event, event: {}", stepEvent);
        String stepOutput = "step-out-0";
        streamBridge.send(stepOutput, stepEvent);
        log.info("Dispatch step event successfully, event: {}", stepEvent);
    }

    /**
     * 分发GSE任务事件
     *
     * @param gseTaskEvent GSE任务事件
     */
    public void dispatchGseTaskEvent(GseTaskEvent gseTaskEvent) {
        log.info("Begin to dispatch gse task event, event: {}", gseTaskEvent);
        String gseTaskOutput = "gseTask-out-0";
        streamBridge.send(gseTaskOutput, gseTaskEvent);
        log.info("Dispatch gse task event successfully, event: {}", gseTaskEvent);
    }

    /**
     * 分发结果处理任务恢复事件
     *
     * @param event 结果处理任务恢复事件
     */
    public void dispatchResultHandleTaskResumeEvent(ResultHandleTaskResumeEvent event) {
        log.info("Begin to dispatch gse task result handle resume event, event: {}", event);
        String resultHandleTaskResumeOutput = "resultHandleTaskResume-out-0";
        streamBridge.send(resultHandleTaskResumeOutput, event);
        log.info("Dispatch gse task result handle resume event successfully, event: {}", event);
    }

    /**
     * 异步发送消息通知事件
     *
     * @param notification 消息内容
     */
    public void dispatchNotifyMsg(TaskNotifyDTO notification) {
        log.info("Begin to dispatch notification event:{}", JsonUtils.toJson(notification));
        String notifyMsgOutput = "notifyMsg-out-0";
        streamBridge.send(notifyMsgOutput, notification);
        log.info("Dispatch notification event successfully");
    }

    /**
     * 发送回调信息事件
     *
     * @param jobCallback 回调内容
     */
    public void dispatchCallbackMsg(JobCallbackDTO jobCallback) {
        log.info("Begin to dispatch job callback event, callback:{}", JsonUtils.toJson(jobCallback));
        String callbackOutput = "callback-out-0";
        streamBridge.send(callbackOutput, jobCallback);
        log.info("Dispatch job callback event successfully");
    }
}
