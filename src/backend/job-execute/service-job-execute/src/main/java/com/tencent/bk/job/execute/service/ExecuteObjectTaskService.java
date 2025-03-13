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

import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.ResultGroupBaseDTO;
import com.tencent.bk.job.execute.model.ResultGroupDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;

import java.util.Collection;
import java.util.List;

/**
 * 执行对象任务 Service
 */
public interface ExecuteObjectTaskService {

    /**
     * 批量保存（insert/update) 任务
     *
     * @param tasks 任务列表
     */
    void batchSaveTasks(Collection<ExecuteObjectTask> tasks);

    /**
     * 批量更新执行对象任务
     *
     * @param tasks 执行对象任务
     */
    void batchUpdateTasks(Collection<ExecuteObjectTask> tasks);

    /**
     * 获取执行成功的执行对象任务数量
     *
     * @param taskInstanceId 作业实例 ID
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   步骤执行次数
     * @return 执行成功的任务数量
     */
    int getSuccessTaskCount(Long taskInstanceId, long stepInstanceId, int executeCount);


    /**
     * 根据GSE任务ID获取执行对象任务
     *
     * @param stepInstance 步骤实例
     * @param gseTaskId    GSE任务ID
     * @return 执行对象任务
     */
    List<ExecuteObjectTask> listTasksByGseTaskId(StepInstanceBaseDTO stepInstance, Long gseTaskId);

    /**
     * 获取执行对象任务
     *
     * @param stepInstance 步骤实例
     * @param executeCount 执行次数
     * @param batch        滚动执行批次；传入null或者0将忽略该参数
     * @return 执行对象任务
     */
    List<ExecuteObjectTask> listTasks(StepInstanceBaseDTO stepInstance,
                                      Integer executeCount,
                                      Integer batch);

    /**
     * 获取执行对象任务详情并分组
     *
     * @param stepInstance 步骤实例
     * @param executeCount 执行次数
     * @param batch        滚动执行批次；如果传入null或者0，将忽略该参数
     */
    List<ResultGroupDTO> listAndGroupTasks(StepInstanceBaseDTO stepInstance,
                                           int executeCount,
                                           Integer batch);

    /**
     * 获取任务结果分组
     *
     * @param stepInstance 步骤实例
     * @param executeCount 执行次数
     * @param batch        滚动执行批次；传入null或者0将忽略该参数
     * @return 执行对象任务
     */
    List<ResultGroupBaseDTO> listResultGroups(StepInstanceBaseDTO stepInstance,
                                              int executeCount,
                                              Integer batch);

    /**
     * 根据执行结果查询执行对象任务详情(排序、限制返回数量) - 包含主机详情
     *
     * @param stepInstance 步骤实例
     * @param executeCount 执行次数
     * @param batch        滚动执行批次；如果传入null或者0，忽略该参数
     * @param status       任务状态
     * @param tag          用户自定义分组标签
     * @param limit        最大返回数量
     * @param orderField   排序字段
     * @param order        排序方式
     * @return 执行对象任务
     */
    List<ExecuteObjectTask> listTaskByResultGroup(StepInstanceBaseDTO stepInstance,
                                                  Integer executeCount,
                                                  Integer batch,
                                                  Integer status,
                                                  String tag,
                                                  Integer limit,
                                                  String orderField,
                                                  Order order);

    /**
     * 根据结果分组获取执行对象任务详情 - 包含主机详情
     *
     * @param stepInstance 步骤实例
     * @param executeCount 执行次数
     * @param batch        滚动执行批次；如果传入null或者0，忽略该参数
     * @param status       任务状态
     * @param tag          用户自定义分组标签
     * @return 执行对象任务
     */
    List<ExecuteObjectTask> listTaskByResultGroup(StepInstanceBaseDTO stepInstance,
                                                  Integer executeCount,
                                                  Integer batch,
                                                  Integer status,
                                                  String tag);


    /**
     * 批量更新任务的字段
     *
     * @param stepInstance       条件 - 步骤实例
     * @param executeCount       条件 - 重试次数
     * @param batch              条件 - 滚动执行批次；传入null将忽略该条件
     * @param actualExecuteCount 值 - 执行对象任务实际执行的步骤重试次数；如果传入null，则不更新
     * @param gseTaskId          值 - 执行对象任务对应的GSE_TASK_ID；如果传入null，则不更新
     */
    void updateTaskFields(StepInstanceBaseDTO stepInstance,
                          int executeCount,
                          Integer batch,
                          Integer actualExecuteCount,
                          Long gseTaskId);


}
