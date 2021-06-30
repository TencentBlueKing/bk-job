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

package com.tencent.bk.job.execute.engine;

import com.tencent.bk.job.execute.engine.model.JobCallbackDTO;
import com.tencent.bk.job.execute.model.TaskNotifyDTO;

/**
 * 作业执行控制消息发送
 */
public interface TaskExecuteControlMsgSender {

    /**
     * 发送启动作业的作业控制消息
     *
     * @param taskInstanceId 作业实例ID
     */
    void startTask(long taskInstanceId);

    /**
     * 发送停止作业的作业控制消息
     *
     * @param taskInstanceId 作业实例ID
     */
    void stopTask(long taskInstanceId);

    /**
     * 发送重头执行作业的作业控制消息
     *
     * @param taskInstanceId 作业实例ID
     */
    void restartTask(long taskInstanceId);

    /**
     * 触发作业继续后续步骤的作业控制消息
     *
     * @param taskInstanceId 作业实例ID
     */
    void refreshTask(long taskInstanceId);

    /**
     * 触发忽略错误的作业控制消息
     *
     * @param stepInstanceId 步骤实例ID
     */
    void ignoreStepError(long stepInstanceId);

    /**
     * 发送进入下一步骤的作业控制消息
     *
     * @param stepInstanceId 步骤实例ID
     */
    void nextStep(long stepInstanceId);

    /**
     * 发送进入下一步骤的作业控制消息
     *
     * @param stepInstanceId 步骤实例ID
     */
    void confirmStepContinue(long stepInstanceId);

    /**
     * 人工确认-终止流程
     *
     * @param stepInstanceId 步骤实例ID
     */
    void confirmStepTerminate(long stepInstanceId);

    /**
     * 人工确认-重新发起确认
     *
     * @param stepInstanceId 步骤实例ID
     */
    void confirmStepRestart(long stepInstanceId);

    /**
     * 发送启动步骤的步骤控制消息
     *
     * @param stepInstanceId 步骤实例ID
     */
    void startStep(long stepInstanceId);

    /**
     * 发送跳过步骤的步骤控制消息
     *
     * @param stepInstanceId 步骤实例ID
     */
    void skipStep(long stepInstanceId);

    /**
     * 发送强制终止步骤的步骤控制消息
     *
     * @param stepInstanceId 步骤实例ID
     */
    void stopStep(long stepInstanceId);

    /**
     * 重新执行步骤中失败的ip
     *
     * @param stepInstanceId 步骤实例ID
     */
    void retryStepFail(long stepInstanceId);

    /**
     * 重新执行步骤
     *
     * @param stepInstanceId 步骤实例ID
     */
    void retryStepAll(long stepInstanceId);

    /**
     * 发送继续GSE文件分发步骤的步骤控制消息
     *
     * @param stepInstanceId 步骤实例ID
     */
    void continueGseFileStep(long stepInstanceId);

    /**
     * 发送清理步骤的步骤控制消息：清理步骤中产生的临时文件等
     *
     * @param stepInstanceId 步骤实例ID
     */
    void clearStep(long stepInstanceId);

    /**
     * 发送执行gse步骤的消息
     *
     * @param stepInstanceId 步骤实例ID
     */
    void startGseStep(long stepInstanceId);

    /**
     * 恢复GSE任务执行
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param requestId      请求ID
     */
    void resumeGseStep(long stepInstanceId, int executeCount, String requestId);

    /**
     * 重新执行步骤中失败的ip
     *
     * @param stepInstanceId 步骤实例ID
     */
    void retryGseStepFail(long stepInstanceId);

    /**
     * 重新执行步骤
     *
     * @param stepInstanceId 步骤实例ID
     */
    void retryGseStepAll(long stepInstanceId);

    /**
     * 发送强制终止TSC步骤的消息
     *
     * @param stepInstanceId 步骤实例ID
     */
    void stopGseStep(long stepInstanceId);

    /**
     * 异步发送消息通知
     *
     * @param notification 消息内容
     */
    void asyncSendNotifyMsg(TaskNotifyDTO notification);

    /**
     * 发送回调信息
     *
     * @param jobCallbackDto
     */
    void sendCallback(JobCallbackDTO jobCallbackDto);
}
