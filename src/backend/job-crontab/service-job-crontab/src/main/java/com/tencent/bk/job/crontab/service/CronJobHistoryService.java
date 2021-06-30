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

package com.tencent.bk.job.crontab.service;

import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.crontab.constant.ExecuteStatusEnum;
import com.tencent.bk.job.crontab.model.dto.CronJobHistoryDTO;
import com.tencent.bk.job.crontab.model.dto.CronJobLaunchResultStatistics;

import java.util.List;
import java.util.Map;

/**
 * @since 16/2/2020 22:31
 */
public interface CronJobHistoryService {
    /**
     * 插入定时任务执行历史
     *
     * @param appId             业务 ID
     * @param cronJobId         定时任务 ID
     * @param scheduledFireTime 调度时间
     * @return 定时任务执行历史 ID
     */
    long insertHistory(long appId, long cronJobId, long scheduledFireTime);

    /**
     * 获取定时任务执行历史
     *
     * @param appId             业务 ID
     * @param cronJobId         定时任务 ID
     * @param scheduledFireTime 调度时间
     * @return 定时任务执行历史
     */
    CronJobHistoryDTO getHistoryByIdAndTime(long appId, long cronJobId, long scheduledFireTime);

    /**
     * 根据定时任务 ID 和调度时间更新执行状态
     *
     * @param appId             业务 ID
     * @param cronJobId         定时任务 ID
     * @param scheduledFireTime 调度时间
     * @param status            执行状态
     * @return 是否更新成功
     */
    boolean updateStatusByIdAndTime(long appId, long cronJobId, long scheduledFireTime, ExecuteStatusEnum status);

    /**
     * 更新调度错误码与错误信息
     *
     * @param historyId 执行历史 ID
     * @param errorCode 错误码
     * @param errorMsg  错误信息
     * @return 是否更新成功
     */
    boolean fillErrorInfo(long historyId, Long errorCode, String errorMsg);

    /**
     * 更新定时任务执行人信息
     *
     * @param historyId 执行历史 ID
     * @param executor  执行人
     * @return 是否更新成功
     */
    boolean fillExecutor(long historyId, String executor);

    /**
     * 根据条件分页查询定时任务执行历史信息
     *
     * @param historyCondition    执行历史查询条件
     * @param baseSearchCondition 通用查询条件
     * @return 分页的定时任务执行历史信息列表
     */
    PageData<CronJobHistoryDTO> listPageHistoryByCondition(CronJobHistoryDTO historyCondition,
                                                           BaseSearchCondition baseSearchCondition);

    /**
     * 批量拉取定时任务启动结果
     *
     * @param appId         业务 ID
     * @param cronJobIdList 定时任务 ID 列表
     * @return 定时任务 ID 与启动结果对应表
     */
    Map<Long, CronJobLaunchResultStatistics> getCronTaskLaunchResultStatistics(Long appId, List<Long> cronJobIdList);

    /**
     * 清理定时任务执行历史
     *
     * @param cleanBefore 结束时间
     * @param cleanAll    是否忽略状态清理全部
     * @return 删除的记录数量
     */
    int cleanHistory(long cleanBefore, boolean cleanAll);
}
