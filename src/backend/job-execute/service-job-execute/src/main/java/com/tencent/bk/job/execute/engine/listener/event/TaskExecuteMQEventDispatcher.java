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
     * 发送启动作业事件
     *
     * @param taskInstanceId 作业实例ID
     */
    void startJob(long taskInstanceId);

    /**
     * 发送停止作业事件
     *
     * @param taskInstanceId 作业实例ID
     */
    void stopJob(long taskInstanceId);

    /**
     * 发送重头执行作业事件
     *
     * @param taskInstanceId 作业实例ID
     */
    void restartJob(long taskInstanceId);

    /**
     * 触发作业继续后续步骤事件
     *
     * @param taskInstanceId 作业实例ID
     */
    void refreshJob(long taskInstanceId);

    /**
     * 触发忽略错误事件
     *
     * @param stepInstanceId 步骤实例ID
     */
    void ignoreStepError(long stepInstanceId);

    /**
     * 发送进入下一步骤事件
     *
     * @param stepInstanceId 步骤实例ID
     */
    void nextStep(long stepInstanceId);

    /**
     * 发送进入下一步骤事件
     *
     * @param stepInstanceId 步骤实例ID
     */
    void confirmStepContinue(long stepInstanceId);

    /**
     * 人工确认-终止流程事件
     *
     * @param stepInstanceId 步骤实例ID
     */
    void confirmStepTerminate(long stepInstanceId);

    /**
     * 人工确认-重新发起确认事件
     *
     * @param stepInstanceId 步骤实例ID
     */
    void confirmStepRestart(long stepInstanceId);

    /**
     * 发送启动步骤事件
     *
     * @param stepInstanceId 步骤实例ID
     */
    void startStep(long stepInstanceId);

    /**
     * 发送跳过步骤事件
     *
     * @param stepInstanceId 步骤实例ID
     */
    void skipStep(long stepInstanceId);

    /**
     * 发送强制终止步骤事件
     *
     * @param stepInstanceId 步骤实例ID
     */
    void stopStep(long stepInstanceId);

    /**
     * 重新执行步骤中失败的ip事件
     *
     * @param stepInstanceId 步骤实例ID
     */
    void retryStepFail(long stepInstanceId);

    /**
     * 重新执行步骤事件
     *
     * @param stepInstanceId 步骤实例ID
     */
    void retryStepAll(long stepInstanceId);

    /**
     * 发送继续GSE文件分发步骤事件
     *
     * @param stepInstanceId 步骤实例ID
     */
    void continueGseFileStep(long stepInstanceId);

    /**
     * 发送执行gse步骤事件
     *
     * @param stepInstanceId 步骤实例ID
     * @param batch          滚动批次；如果非滚动步骤，传入null
     */
    void startGseStep(long stepInstanceId, Integer batch);

    /**
     * 恢复GSE任务执行事件
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param requestId      请求ID
     */
    void resumeGseStep(long stepInstanceId, int executeCount, String requestId);

    /**
     * 重新执行步骤中失败的ip事件
     *
     * @param stepInstanceId 步骤实例ID
     */
    void retryGseStepFail(long stepInstanceId);

    /**
     * 重新执行步骤事件
     *
     * @param stepInstanceId 步骤实例ID
     */
    void retryGseStepAll(long stepInstanceId);

    /**
     * 发送强制终止GSE步骤事件
     *
     * @param stepInstanceId 步骤实例ID
     */
    void stopGseStep(long stepInstanceId);

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
