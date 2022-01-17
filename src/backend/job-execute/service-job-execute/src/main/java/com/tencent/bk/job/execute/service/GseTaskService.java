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
import com.tencent.bk.job.execute.model.AgentTaskResultGroupDTO;
import com.tencent.bk.job.execute.model.GseAgentTaskDTO;
import com.tencent.bk.job.execute.model.GseTaskDTO;

import java.util.Collection;
import java.util.List;

/**
 * GSE 任务 Service
 */
public interface GseTaskService {

    /**
     * 保存 GSE 任务
     *
     * @param gseTask GSE 任务
     */
    void saveGseTask(GseTaskDTO gseTask);

    /**
     * 获取 GSE 任务
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   步骤执行次数
     * @return GSE 任务
     */
    GseTaskDTO getGseTask(long stepInstanceId, int executeCount);

    /**
     * 批量保存 GSE Agent 任务
     *
     * @param gseAgentTasks GSE Agent 任务列表
     */
    void batchSaveGseIpTasks(List<GseAgentTaskDTO> gseAgentTasks);

    /**
     * 批量更新 GSE Agent 任务
     *
     * @param stepInstanceId    步骤实例ID
     * @param executeCount      步骤执行次数
     * @param cloudAreaIdAndIps Agent ip 列表
     * @param startTime         任务开始时间
     * @param endTime           任务结束时间
     * @param status            任务状态
     */
    void batchUpdateGseAgentTasks(long stepInstanceId,
                                  int executeCount,
                                  Collection<String> cloudAreaIdAndIps,
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
     * 获取执行成功的Agent任务
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   步骤执行次数
     * @return 执行成功的Agent任务
     */
    List<GseAgentTaskDTO> listSuccessAgentGseTask(long stepInstanceId, int executeCount);

    /**
     * 获取步骤执行结果分组信息-不包含ip详细信息
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     */
    List<AgentTaskResultGroupDTO> getGseAgentTaskStatInfo(long stepInstanceId, int executeCount);

    /**
     * 获取步骤执行结果分组信息
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @return
     */
    List<AgentTaskResultGroupDTO> getLogStatInfoWithIp(long stepInstanceId, int executeCount);

    List<GseAgentTaskDTO> listGseAgentTasksByResultType(Long stepInstanceId, Integer executeCount, Integer resultType,
                                                        String tag);

    List<GseAgentTaskDTO> getGseAgentTaskContentByResultType(Long stepInstanceId, Integer executeCount, Integer resultType,
                                                             String tag);

    /**
     * 获取agent任务信息
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param onlyTargetIp   是否仅返回目标服务器IP
     * @return agent任务信息
     */
    List<GseAgentTaskDTO> getGseAgentTask(Long stepInstanceId, Integer executeCount, boolean onlyTargetIp);

    GseAgentTaskDTO getGseAgentTask(Long stepInstanceId, Integer executeCount, String cloudAreaIdAndIp);

    /**
     * 获取文件任务源ip
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @return 文件任务源ip
     */
    List<IpDTO> getTaskFileSourceIps(Long stepInstanceId, Integer executeCount);
}
