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

package com.tencent.bk.job.crontab.dao;

import com.tencent.bk.job.crontab.model.dto.InnerCronJobHistoryDTO;

/**
 * @since 21/2/2020 22:28
 */
public interface InnerCronJobHistoryDAO {

    /**
     * 获取内部定时任务执行历史
     *
     * @param systemId          系统 ID
     * @param jobKey            作业 Key
     * @param scheduledFireTime 调度时间
     * @return 内部定时任务执行历史
     */
    InnerCronJobHistoryDTO getCronJobHistory(String systemId, String jobKey, long scheduledFireTime);

    /**
     * 插入内部定时任务执行历史
     *
     * @param systemId          系统 ID
     * @param jobKey            作业 Key
     * @param scheduledFireTime 调度时间
     * @return 内部定时任务执行历史 ID
     */
    long insertCronJobHistory(String systemId, String jobKey, long scheduledFireTime);

    /**
     * 更新内部定时任务执行历史状态
     *
     * @param systemId          系统 ID
     * @param jobKey            作业 Key
     * @param scheduledFireTime 调度时间
     * @param status            状态
     * @return 是否更新成功
     */
    boolean updateStatusByIdAndTime(String systemId, String jobKey, long scheduledFireTime, int status);

    /**
     * 清理内部定时任务执行历史
     *
     * @param cleanBefore 结束时间
     * @param cleanAll    是否忽略状态清理全部
     * @return 删除的记录数量
     */
    int cleanHistory(long cleanBefore, boolean cleanAll);
}
