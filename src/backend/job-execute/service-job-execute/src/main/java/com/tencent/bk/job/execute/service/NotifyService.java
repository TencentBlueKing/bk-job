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

package com.tencent.bk.job.execute.service;

import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.TaskNotifyDTO;

/**
 * 消息通知服务
 */
public interface NotifyService {
    /**
     * 失败通知
     *
     * @param taskNotifyDTO 通知
     */
    void notifyTaskFail(TaskNotifyDTO taskNotifyDTO);

    /**
     * 成功通知
     *
     * @param taskNotifyDTO 通知
     */
    void notifyTaskSuccess(TaskNotifyDTO taskNotifyDTO);

    /**
     * 等待用户确认通知
     *
     * @param taskNotifyDTO 通知
     */
    void notifyTaskConfirm(TaskNotifyDTO taskNotifyDTO);

    /**
     * 发送作业执行失败通知给 MQ
     *
     * @param taskInstance 作业实例
     * @param stepInstance 步骤实例
     */
    void asyncSendMQFailTaskNotification(TaskInstanceDTO taskInstance, StepInstanceBaseDTO stepInstance);

    /**
     * 发送作业执行成功通知给 MQ
     *
     * @param taskInstance 作业实例
     * @param stepInstance 步骤实例
     */
    void asyncSendMQSuccessTaskNotification(TaskInstanceDTO taskInstance, StepInstanceBaseDTO stepInstance);

    /**
     * 发送人工确认通知给 MQ
     *
     * @param taskInstance 作业实例
     * @param stepInstance 步骤实例
     */
    void asyncSendMQConfirmNotification(TaskInstanceDTO taskInstance, StepInstanceBaseDTO stepInstance);
}
