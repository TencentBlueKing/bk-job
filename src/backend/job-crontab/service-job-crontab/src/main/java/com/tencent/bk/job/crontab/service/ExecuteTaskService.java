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

package com.tencent.bk.job.crontab.service;

import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.crontab.exception.TaskExecuteAuthFailedException;
import com.tencent.bk.job.execute.model.inner.ServiceTaskExecuteResult;
import com.tencent.bk.job.execute.model.inner.ServiceTaskVariable;

import java.util.List;

/**
 * @since 17/2/2020 19:58
 */
public interface ExecuteTaskService {
    /**
     * 调用执行引擎执行作业
     *
     * @param appId        业务 ID
     * @param taskId       执行方案 ID
     * @param cronTaskId   定时任务 ID
     * @param cronName     定时任务名称
     * @param variableList 变量信息列表
     * @param operator     执行人
     * @return 执行结果
     */
    InternalResponse<ServiceTaskExecuteResult> executeTask(
        long appId, long taskId, long cronTaskId, String cronName,
        List<ServiceTaskVariable> variableList,
        String operator
    );

    /**
     * 校验执行方案的执行权限
     * <p>
     * 弱无权限则抛异常
     *
     * @param appId        业务 ID
     * @param taskId       执行方案 ID
     * @param cronTaskId   定时任务 ID
     * @param cronName     定时任务名称
     * @param variableList 变量信息列表
     * @param operator     执行人
     * @throws TaskExecuteAuthFailedException 鉴权失败抛出异常
     */
    void authExecuteTask(
        long appId, long taskId, long cronTaskId, String cronName,
        List<ServiceTaskVariable> variableList, String operator
    ) throws TaskExecuteAuthFailedException;
}
