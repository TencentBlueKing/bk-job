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

import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.AgentTaskDetailDTO;
import com.tencent.bk.job.execute.model.AgentTaskResultGroupBaseDTO;
import com.tencent.bk.job.execute.model.AgentTaskResultGroupDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;

import java.util.Collection;
import java.util.List;

/**
 * GSE Agent 任务 Service
 */
public interface AgentTaskService {

    /**
     * 批量保存（insert/update) GSE Agent 任务
     *
     * @param agentTasks GSE Agent 任务列表
     */
    void batchSaveAgentTasks(Collection<AgentTaskDTO> agentTasks);

    /**
     * 批量更新Agent任务
     *
     * @param agentTasks Agent任务
     */
    void batchUpdateAgentTasks(Collection<AgentTaskDTO> agentTasks);

    /**
     * 获取执行成功的Agent任务数量
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   步骤执行次数
     * @return 执行成功的Agent数量
     */
    int getSuccessAgentTaskCount(long stepInstanceId, int executeCount);


    /**
     * 根据GSE任务ID获取Agent任务
     *
     * @param gseTaskId GSE任务ID
     * @return Agent任务
     */
    List<AgentTaskDTO> listAgentTasksByGseTaskId(Long gseTaskId);

    /**
     * 获取Agent任务
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次；传入null或者0将忽略该参数
     * @return Agent任务
     */
    List<AgentTaskDTO> listAgentTasks(Long stepInstanceId,
                                      Integer executeCount,
                                      Integer batch);

    /**
     * 获取Agent任务详情并分组
     *
     * @param stepInstance 步骤实例
     * @param executeCount 执行次数
     * @param batch        滚动执行批次；如果传入null或者0，将忽略该参数
     */
    List<AgentTaskResultGroupDTO> listAndGroupAgentTasks(StepInstanceBaseDTO stepInstance,
                                                         int executeCount,
                                                         Integer batch);

    /**
     * 获取任务结果分组
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次；传入null或者0将忽略该参数
     * @return Agent任务
     */
    List<AgentTaskResultGroupBaseDTO> listResultGroups(long stepInstanceId,
                                                       int executeCount,
                                                       Integer batch);

    /**
     * 根据执行结果查询Agent任务详情(排序、限制返回数量) - 包含主机详情
     *
     * @param stepInstance 步骤实例
     * @param executeCount 执行次数
     * @param batch        滚动执行批次；如果传入null或者0，忽略该参数
     * @param status       任务状态
     * @param tag          用户自定义分组标签
     * @param limit        最大返回数量
     * @param orderField   排序字段
     * @param order        排序方式
     * @return Agent任务
     */
    List<AgentTaskDetailDTO> listAgentTaskDetailByResultGroup(StepInstanceBaseDTO stepInstance,
                                                              Integer executeCount,
                                                              Integer batch,
                                                              Integer status,
                                                              String tag,
                                                              Integer limit,
                                                              String orderField,
                                                              Order order);

    /**
     * 根据结果分组获取Agent任务详情 - 包含主机详情
     *
     * @param stepInstance 步骤实例
     * @param executeCount 执行次数
     * @param batch        滚动执行批次；如果传入null或者0，忽略该参数
     * @param status       任务状态
     * @param tag          用户自定义分组标签
     * @return Agent任务
     */
    List<AgentTaskDetailDTO> listAgentTaskDetailByResultGroup(StepInstanceBaseDTO stepInstance,
                                                              Integer executeCount,
                                                              Integer batch,
                                                              Integer status,
                                                              String tag);

    /**
     * 获取Agent任务详情 - 包含主机详情
     *
     * @param stepInstance 步骤实例
     * @param executeCount 执行次数
     * @param batch        滚动执行批次；传入null或者0将忽略该参数
     * @return Agent任务
     */
    List<AgentTaskDetailDTO> listAgentTaskDetail(StepInstanceBaseDTO stepInstance,
                                                 Integer executeCount,
                                                 Integer batch);

    /**
     * 获取Agent任务实际执行成功的executeCount值(重试场景,兼容历史数据)
     *
     * @param stepInstanceId 步骤实例ID
     * @param cloudIp        云区域+ip
     * @return Agent任务实际执行成功的executeCount值
     */
    @CompatibleImplementation(name = "rolling_execution", explain = "兼容历史数据", version = "3.7.x")
    int getActualSuccessExecuteCount(long stepInstanceId, String cloudIp);

    /**
     * 批量更新AgentTask的字段
     *
     * @param stepInstanceId     条件 - 步骤实例ID
     * @param executeCount       条件 - 重试次数
     * @param batch              条件 - 滚动执行批次；传入null将忽略该条件
     * @param actualExecuteCount 值 - Agent任务实际执行的步骤重试次数；如果传入null，则不更新
     * @param gseTaskId          值 - Agent任务对应的GSE_TASK_ID；如果传入null，则不更新
     */
    void updateAgentTaskFields(long stepInstanceId,
                               int executeCount,
                               Integer batch,
                               Integer actualExecuteCount,
                               Long gseTaskId);


}
