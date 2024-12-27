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

import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceQuery;

import java.util.Collection;
import java.util.List;

/**
 * 作业执行实例DAO
 */
public interface TaskInstanceDAO {
    Long addTaskInstance(TaskInstanceDTO taskInstance);

    TaskInstanceDTO getTaskInstance(long taskInstanceId);

    void updateTaskStatus(long taskInstanceId, int status);

    void updateTaskCurrentStepId(Long taskInstanceId, Long stepInstanceId);

    void resetTaskStatus(Long taskInstanceId);

    /**
     * 分页查询作业执行实例
     *
     * @param taskQuery           查询
     * @param baseSearchCondition 基础查询条件
     * @return 分页结果
     */
    PageData<TaskInstanceDTO> listPageTaskInstance(TaskInstanceQuery taskQuery,
                                                   BaseSearchCondition baseSearchCondition);


    /**
     * 获取定时作业执行情况
     *
     * @param appId               业务ID
     * @param cronTaskId          定时作业ID
     * @param latestTimeInSeconds 时间范围
     * @param status              任务状态,如果为NULL,那么返回所有状态
     * @param limit               返回记录个数；如果未NULL,那么不限制返回数量
     * @return 作业实例列表
     */
    List<TaskInstanceDTO> listLatestCronTaskInstance(long appId,
                                                     Long cronTaskId,
                                                     Long latestTimeInSeconds,
                                                     RunStatusEnum status,
                                                     Integer limit);

    /**
     * 更新作业的执行信息
     *
     * @param taskInstanceId 作业实例ID
     * @param status         作业状态
     * @param currentStepId  当前步骤实例ID
     * @param startTime      开始时间
     * @param endTime        结束时间
     * @param totalTime      总耗时
     */
    void updateTaskExecutionInfo(long taskInstanceId,
                                 RunStatusEnum status,
                                 Long currentStepId,
                                 Long startTime,
                                 Long endTime,
                                 Long totalTime);

    /**
     * 重置作业执行状态
     *
     * @param taskInstanceId 作业实例ID
     */
    void resetTaskExecuteInfoForRetry(long taskInstanceId);

    /**
     * 查询大于某个时间的定时任务执行记录对应的业务Id
     * ！！！调用时需要注意查询效率，可能慢查询
     *
     * @param inAppIdList   指定appId的范围
     * @param cronTaskId    定时任务Id
     * @param minCreateTime 最小创建时间
     * @return 业务 ID 列表
     */
    List<Long> listTaskInstanceAppId(List<Long> inAppIdList, Long cronTaskId, Long minCreateTime);

    /**
     * 在某段时间内某业务是否存在执行记录
     *
     * @param appId      业务Id
     * @param cronTaskId 定时任务Id
     * @param fromTime   起始时间
     * @param toTime     终止时间
     * @return 是否存在执行记录
     */
    boolean hasExecuteHistory(Long appId, Long cronTaskId, Long fromTime, Long toTime);

    List<Long> listTaskInstanceId(Long appId, Long fromTime, Long toTime, int offset, int limit);

    /**
     * 保存作业实例与主机的关系，便于根据ip/ipv6检索作业实例
     *
     * @param appId          业务 ID
     * @param taskInstanceId 作业实例ID
     * @param hosts          主机列表
     */
    void saveTaskInstanceHosts(long appId, long taskInstanceId, Collection<HostDTO> hosts);

}
