/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bk.job.execute.dao;

import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.constant.CompatibleType;
import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.ResultGroupBaseDTO;

import java.util.Collection;
import java.util.List;

/**
 * ScriptAgentTaskDAO
 */
@Deprecated
@CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA)
public interface ScriptAgentTaskDAO {
    /**
     * 批量保存Agent任务
     *
     * @param agentTasks Agent任务
     */
    void batchSaveAgentTasks(Collection<ExecuteObjectTask> agentTasks);

    /**
     * 批量更新Agent任务
     *
     * @param agentTasks Agent任务
     */
    void batchUpdateAgentTasks(Collection<ExecuteObjectTask> agentTasks);

    /**
     * 获取步骤成功执行的Agent任务数量
     *
     * @param taskInstanceId 作业实例 ID
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @return 步骤成功执行的Agent任务数量
     */
    int getSuccessAgentTaskCount(Long taskInstanceId, long stepInstanceId, int executeCount);

    /**
     * 查询执行结果分组
     *
     * @param taskInstanceId 作业实例 ID
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次；如果传入null或者0，忽略该参数
     * @return 执行结果分组
     */
    List<ResultGroupBaseDTO> listResultGroups(Long taskInstanceId,
                                              long stepInstanceId,
                                              int executeCount,
                                              Integer batch);

    /**
     * 根据执行结果查询Agent任务
     *
     * @param taskInstanceId 作业实例 ID
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次；如果传入null或者0，忽略该参数
     * @param status         任务状态
     * @param tag            用户自定义分组标签
     * @return Agent任务
     */
    List<ExecuteObjectTask> listAgentTaskByResultGroup(Long taskInstanceId,
                                                       Long stepInstanceId,
                                                       Integer executeCount,
                                                       Integer batch,
                                                       Integer status,
                                                       String tag);

    /**
     * 根据执行结果查询Agent任务(排序、限制返回数量)
     *
     * @param taskInstanceId 作业实例 ID
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次；如果传入null或者0，忽略该参数
     * @param status         任务状态
     * @param tag            用户自定义分组标签
     * @param limit          最大返回数量
     * @param orderField     排序字段
     * @param order          排序方式
     * @return Agent任务
     */
    List<ExecuteObjectTask> listAgentTaskByResultGroup(Long taskInstanceId,
                                                       Long stepInstanceId,
                                                       Integer executeCount,
                                                       Integer batch,
                                                       Integer status,
                                                       String tag,
                                                       Integer limit,
                                                       String orderField,
                                                       Order order);

    /**
     * 获取Agent任务
     *
     * @param taskInstanceId 作业实例 ID
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次；传入null或者0将忽略该参数
     * @return Agent任务信息
     */
    List<ExecuteObjectTask> listAgentTasks(Long taskInstanceId,
                                           Long stepInstanceId,
                                           Integer executeCount,
                                           Integer batch);

    /**
     * 根据GSE任务ID获取Agent任务
     *
     * @param taskInstanceId 作业实例 ID
     * @param gseTaskId      GSE任务ID
     * @return Agent任务
     */
    List<ExecuteObjectTask> listAgentTasksByGseTaskId(Long taskInstanceId, Long gseTaskId);

    /**
     * 根据hostId查询Agent任务
     *
     * @param taskInstanceId 作业实例 ID
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次；传入null或者0将忽略该参数
     * @param hostId         主机ID
     * @return Agent任务
     */
    ExecuteObjectTask getAgentTaskByHostId(Long taskInstanceId,
                                           Long stepInstanceId,
                                           Integer executeCount,
                                           Integer batch,
                                           long hostId);

    /**
     * 部分更新AgentTask的字段
     *
     * @param taskInstanceId     作业实例 ID
     * @param stepInstanceId     条件 - 步骤实例ID
     * @param executeCount       条件 - 重试次数
     * @param batch              条件 - 滚动执行批次；传入null将忽略该条件
     * @param actualExecuteCount 值 - Agent任务实际执行的步骤重试次数；如果传入null，则不更新
     * @param gseTaskId          值 - Agent任务对应的GSE_TASK_ID；如果传入null，则不更新
     */
    void updateAgentTaskFields(Long taskInstanceId,
                               long stepInstanceId,
                               int executeCount,
                               Integer batch,
                               Integer actualExecuteCount,
                               Long gseTaskId);
}
