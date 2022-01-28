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

import com.tencent.bk.job.execute.engine.model.JobCallbackDTO;
import com.tencent.bk.job.execute.model.TaskNotifyDTO;

/**
 * 作业执行MQ事件分发
 */
public interface TaskExecuteMQEventDispatcher {

    /**
     * 分发作业事件
     *
     * @param jobEvent 作业事件
     */
    void dispatchJobEvent(JobEvent jobEvent);

    /**
     * 分发步骤事件
     *
     * @param stepEvent 步骤事件
     */
    void dispatchStepEvent(StepEvent stepEvent);

    /**
     * 分发GSE任务事件
     *
     * @param gseTaskEvent GSE任务事件
     */
    void dispatchGseTaskEvent(GseTaskEvent gseTaskEvent);

    /**
     * 分发结果处理任务恢复事件
     *
     * @param event 结果处理任务恢复事件
     */
    void dispatchResultHandleTaskResumeEvent(GseTaskResultHandleTaskResumeEvent event);

    /**
     * 异步发送消息通知事件
     *
     * @param notification 消息内容
     */
    void asyncSendNotifyMsg(TaskNotifyDTO notification);

    /**
     * 发送回调信息事件
     *
     * @param jobCallbackDto 回调内容
     */
    void sendCallback(JobCallbackDTO jobCallbackDto);

    /**
     * 触发步骤结果刷新事件
     *
     * @param stepInstanceId 步骤实例ID
     */
    void refreshStep(long stepInstanceId);
}
