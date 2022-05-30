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

import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.execute.constants.TaskOperationEnum;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.model.FastTaskDTO;
import com.tencent.bk.job.execute.model.StepOperationDTO;
import com.tencent.bk.job.execute.model.TaskExecuteParam;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;

import java.util.List;

/**
 * 作业执行Service
 */
public interface TaskExecuteService {
    /**
     * 快速执行作业
     *
     * @param fastTask 快速执行作业
     * @return 作业实例 ID
     */
    Long executeFastTask(FastTaskDTO fastTask);

    /**
     * 重做快速作业实例
     *
     * @param fastTask 快速作业
     * @return 作业实例 ID
     */
    Long redoFastTask(FastTaskDTO fastTask);

    /**
     * 启动作业
     *
     * @param taskInstanceId 作业实例 ID
     */
    void startTask(long taskInstanceId);

    /**
     * 创建作业实例
     *
     * @param executeParam 作业执行参数
     * @return 创建的作业实例
     */
    TaskInstanceDTO executeJobPlan(TaskExecuteParam executeParam);

    /**
     * 创建重做作业实例
     *
     * @param appId                 业务 ID
     * @param taskInstanceId        作业实例 ID
     * @param operator              操作者
     * @param executeVariableValues 全局变量
     * @return
     */
    TaskInstanceDTO createTaskInstanceForRedo(Long appId, Long taskInstanceId, String operator,
                                              List<TaskVariableDTO> executeVariableValues)
        throws ServiceException;

    /**
     * 步骤操作
     *
     * @param appId         业务 ID
     * @param operator      操作者
     * @param stepOperation 步骤操作
     * @return 执行次数
     * @throws ServiceException
     */
    Integer doStepOperation(Long appId, String operator, StepOperationDTO stepOperation) throws ServiceException;

    /**
     * 终止作业
     *
     * @param username       操作者
     * @param appId          业务ID
     * @param taskInstanceId 作业实例ID
     * @throws ServiceException 终止操作异常的时候抛出
     */
    void terminateJob(String username, Long appId, Long taskInstanceId) throws ServiceException;

    /**
     * 作业操作
     *
     * @param appId          业务 ID
     * @param operator       操作者
     * @param taskInstanceId 作业实例 ID
     * @param operation      操作类型
     * @throws ServiceException
     */
    void doTaskOperation(Long appId, String operator, long taskInstanceId,
                         TaskOperationEnum operation) throws ServiceException;

    /**
     * 作业执行方案执行鉴权
     *
     * @param executeParam 作业执行参数
     */
    void authExecuteJobPlan(TaskExecuteParam executeParam) throws ServiceException;
}
