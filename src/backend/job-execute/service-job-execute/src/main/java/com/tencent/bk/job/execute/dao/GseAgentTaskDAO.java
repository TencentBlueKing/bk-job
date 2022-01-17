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
import com.tencent.bk.job.execute.model.GseAgentTaskDTO;
import com.tencent.bk.job.execute.model.ResultGroupBaseDTO;

import java.util.Collection;
import java.util.List;

/**
 * GseAgentTaskDAO
 */
public interface GseAgentTaskDAO {
    void batchSaveGseAgentTasks(List<GseAgentTaskDTO> gseAgentTasks);

    void batchUpdateGseAgentTasks(long stepInstanceId, int executeCount, Collection<String> cloudAreaAndIps, Long startTime,
                                  Long endTime, IpStatus ipStatus);

    int getSuccessIpCount(long stepInstanceId, int executeCount);

    List<GseAgentTaskDTO> getSuccessGseTaskIp(long stepInstanceId, int executeCount);

    /**
     * 查询执行结果分组
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @return 执行结果分组
     */
    List<ResultGroupBaseDTO> listResultGroups(long stepInstanceId, int executeCount);

    List<GseAgentTaskDTO> listAgentTaskByResultType(Long stepInstanceId, Integer executeCount, Integer resultType,
                                                    String tag);

    List<GseAgentTaskDTO> listAgentTaskByResultType(Long stepInstanceId, Integer executeCount, Integer resultType,
                                                    String tag, Integer limit, String orderField, Order order);

    /**
     * 获取agent任务信息
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param onlyTargetIp   是否仅返回目标服务器IP
     * @return agent任务信息
     */
    List<GseAgentTaskDTO> listGseAgentTasks(Long stepInstanceId, Integer executeCount, boolean onlyTargetIp);

    GseAgentTaskDTO getGseAgentTaskByIp(Long stepInstanceId, Integer executeCount, String ip);

    List<GseAgentTaskDTO> listAgentTasksByIps(Long stepInstanceId, Integer executeCount, String[] ipArray);

    void deleteAllGseAgentTasks(long stepInstanceId, int executeCount);

    int getSuccessRetryCount(long stepInstanceId, String cloudAreaAndIp);

    /**
     * 获取文件任务源ip
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @return 文件任务源ip
     */
    List<String> getTaskFileSourceIps(Long stepInstanceId, Integer executeCount);

    /**
     * 获取任务目标ip - 根据ip模糊匹配
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param searchIp       用于检索的IP
     * @return 匹配的任务目标IP列表
     */
    List<String> fuzzySearchTargetIpsByIp(Long stepInstanceId, Integer executeCount, String searchIp);


}
