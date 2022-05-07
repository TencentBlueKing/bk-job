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

package com.tencent.bk.job.execute.dao;

import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.execute.engine.consts.IpStatus;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.AgentTaskResultGroupDTO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * FileAgentTaskDAO
 */
public interface FileAgentTaskDAO {
    /**
     * 批量新增/更新Agent任务
     *
     * @param agentTasks Agent任务
     */
    void batchSaveAgentTasks(List<AgentTaskDTO> agentTasks);

    /**
     * 批量更新Agent任务
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param cloudIp        ip
     * @param startTime      任务开始时间
     * @param endTime        任务结束时间
     * @param ipStatus       任务状态
     */
    void batchUpdateAgentTasks(long stepInstanceId,
                               int executeCount,
                               Collection<String> cloudIp,
                               Long startTime,
                               Long endTime,
                               IpStatus ipStatus);

    /**
     * 获取步骤成功执行的Agent任务数量
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @return 步骤成功执行的Agent任务数量
     */
    int getSuccessIpCount(long stepInstanceId, int executeCount);

    /**
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   步骤执行次数
     * @return 根据GSE Agent 任务状态分组计数结果
     */
    Map<IpStatus, Integer> countStepAgentTaskGroupByStatus(long stepInstanceId, int executeCount);

    /**
     * 查询执行结果分组
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次；如果传入null或者0，忽略该参数
     * @return 执行结果分组
     */
    List<AgentTaskResultGroupDTO> listResultGroups(long stepInstanceId, int executeCount, Integer batch);

    /**
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次；如果传入null或者0，忽略该参数
     * @param status         任务状态
     * @return Agent任务
     */
    List<AgentTaskDTO> listAgentTaskByResultGroup(Long stepInstanceId,
                                                  Integer executeCount,
                                                  Integer batch,
                                                  Integer status);

    /**
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次；如果传入null或者0，忽略该参数
     * @param status         任务状态
     * @param orderField     排序字段
     * @param order          排序方式
     * @return Agent任务
     * @parma limit 最大返回数量
     */
    List<AgentTaskDTO> listAgentTaskByResultGroup(Long stepInstanceId,
                                                  Integer executeCount,
                                                  Integer batch,
                                                  Integer status,
                                                  Integer limit,
                                                  String orderField,
                                                  Order order);

    /**
     * 获取agent任务
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次；传入null或者0将忽略该参数
     * @return agent任务信息
     */
    List<AgentTaskDTO> listAgentTasks(Long stepInstanceId,
                                      Integer executeCount,
                                      Integer batch);

    /**
     * 根据GSE任务ID获取agent任务
     *
     * @param gseTaskId GSE任务ID
     * @return agent任务
     */
    List<AgentTaskDTO> listAgentTasksByGseTaskId(Long gseTaskId);

    AgentTaskDTO getAgentTaskByIp(Long stepInstanceId, Integer executeCount, String ip);

    List<AgentTaskDTO> listAgentTasksByIps(Long stepInstanceId, Integer executeCount, String[] ipArray);

    void deleteAllAgentTasks(long stepInstanceId, int executeCount);

    int getSuccessRetryCount(long stepInstanceId, String cloudAreaAndIp);

    /**
     * 获取任务目标ip - 根据ip模糊匹配
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param searchIp       用于检索的IP
     * @return 匹配的任务目标IP列表
     */
    List<String> fuzzySearchTargetIpsByIp(Long stepInstanceId, Integer executeCount, String searchIp);

    /**
     * 获取文件任务源ip
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @return 文件任务源ip
     */
    List<String> getTaskFileSourceIps(Long stepInstanceId, Integer executeCount);

}
