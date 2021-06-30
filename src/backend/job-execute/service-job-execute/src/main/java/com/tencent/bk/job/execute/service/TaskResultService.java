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
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.execute.model.*;
import com.tencent.bk.job.execute.model.inner.ServiceCronTaskExecuteResultStatistics;

import java.util.List;
import java.util.Map;

/**
 * 作业执行结果服务
 */
public interface TaskResultService {
    /**
     * 分页获取作业实例
     *
     * @param taskQuery           任务实例查询条件
     * @param baseSearchCondition 基本查询条件
     * @return 作业实例列表
     */
    PageData<TaskInstanceDTO> listPageTaskInstance(TaskInstanceQuery taskQuery,
                                                   BaseSearchCondition baseSearchCondition);

    /**
     * 获取作业执行结果
     *
     * @param username       用户名
     * @param appId          业务ID
     * @param taskInstanceId 任务实例ID
     * @return 作业执行结果
     */
    TaskExecuteResultDTO getTaskExecutionResult(String username, Long appId, Long taskInstanceId);

    /**
     * 获取步骤执行详情
     *
     * @param username       用户名
     * @param appId          业务ID
     * @param taskInstanceId 作业实例ID
     * @param query          查询条件
     * @return 步骤执行详情
     */
    StepExecutionDetailDTO getFastTaskStepExecutionResult(String username, Long appId, Long taskInstanceId,
                                                          StepExecutionResultQuery query);

    /**
     * 获取步骤执行详情
     *
     * @param username 用户名
     * @param appId    业务ID
     * @param query    查询条件
     * @return 执行详情
     * @throws ServiceException
     */
    StepExecutionDetailDTO getStepExecutionResult(String username, Long appId, StepExecutionResultQuery query)
        throws ServiceException;

    /**
     * 获取定时任务执行结果统计
     *
     * @param appId          业务ID
     * @param cronTaskIdList 定时任务ID列表
     * @return 统计结果，Map<定时任务ID, 统计结果>
     */
    Map<Long, ServiceCronTaskExecuteResultStatistics> getCronTaskExecuteResultStatistics(long appId,
                                                                                         List<Long> cronTaskIdList);

    /**
     * 根据执行结果分组获取主机信息
     *
     * @param username       用户名
     * @param appId          业务ID
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param resultType     结果
     * @param tag            结果输出分类标签
     * @param keyword        脚本日志关键字
     * @return
     */
    List<IpDTO> getHostsByResultType(String username, Long appId, Long stepInstanceId,
                                     Integer executeCount, Integer resultType,
                                     String tag, String keyword);

    /**
     * 获取步骤执行历史
     *
     * @param username       用户名
     * @param appId          业务ID
     * @param stepInstanceId 步骤实例ID
     * @return 执行历史
     */
    List<StepExecutionRecordDTO> listStepExecutionHistory(String username, Long appId, Long stepInstanceId);

}
