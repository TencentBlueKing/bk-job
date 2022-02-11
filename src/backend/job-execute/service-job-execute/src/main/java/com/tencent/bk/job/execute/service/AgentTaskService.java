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

import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.execute.engine.consts.IpStatus;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.AgentTaskResultGroupDTO;

import java.util.Collection;
import java.util.List;

/**
 * GSE Agent任务 Service
 */
public interface AgentTaskService {

    /**
     * 批量保存 GSE Agent 任务
     *
     * @param agentTasks GSE Agent 任务列表
     */
    void batchSaveAgentTasks(List<AgentTaskDTO> agentTasks);

    /**
     * 批量更新 GSE Agent 任务
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   步骤执行次数
     * @param cloudIps       Agent ip 列表
     * @param startTime      任务开始时间
     * @param endTime        任务结束时间
     * @param status         任务状态
     */
    void batchUpdateAgentTasks(long stepInstanceId,
                               int executeCount,
                               Collection<String> cloudIps,
                               Long startTime,
                               Long endTime,
                               IpStatus status);

    /**
     * 获取执行成功的Agent数量
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   步骤执行次数
     * @return 执行成功的Agent数量
     */
    int getSuccessAgentTaskCount(long stepInstanceId, int executeCount);

    /**
     * 获取步骤执行结果并分组
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次；如果传入null或者0，将忽略该参数
     */
    List<AgentTaskResultGroupDTO> listAndGroupAgentTasks(long stepInstanceId, int executeCount, Integer batch);

    /**
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次；如果传入null或者0，忽略该参数
     * @param status         任务状态
     * @param tag            用户自定义分组标签
     * @return Agent任务
     */
    List<AgentTaskDTO> listAgentTasksByResultGroup(Long stepInstanceId,
                                                   Integer executeCount,
                                                   Integer batch,
                                                   Integer status,
                                                   String tag);

    /**
     * 获取agent任务
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次；传入null或者0将忽略该参数
     * @param onlyTargetIp   是否仅返回目标服务器IP
     * @return agent任务
     */
    List<AgentTaskDTO> listAgentTasks(Long stepInstanceId,
                                      Integer executeCount,
                                      Integer batch,
                                      boolean onlyTargetIp);

    /**
     * 根据GSE任务ID获取agent任务
     *
     * @param gseTaskId GSE任务ID
     * @return agent任务
     */
    List<AgentTaskDTO> listAgentTasksByGseTaskId(Long gseTaskId);

    /**
     * 获取agent任务
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param cloudIp        ip
     * @return agent任务
     */
    AgentTaskDTO getAgentTask(Long stepInstanceId, Integer executeCount, String cloudIp);

    /**
     * 获取文件任务源ip
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @return 文件任务源ip
     */
    List<IpDTO> getTaskFileSourceIps(Long stepInstanceId, Integer executeCount);
}
