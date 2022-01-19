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

import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.model.StepInstanceRollingTaskDTO;

/**
 * 步骤滚动任务 Service
 */
public interface StepInstanceRollingTaskService {

    /**
     * 查询步骤滚动任务
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   步骤执行次数
     * @param batch          滚动批次
     * @return 步骤滚动任务
     */
    StepInstanceRollingTaskDTO queryRollingTask(long stepInstanceId,
                                                int executeCount,
                                                int batch);

    /**
     * 保存步骤滚动任务
     *
     * @param rollingTask 滚动任务
     * @return 步骤滚动任务ID
     */
    long saveRollingTask(StepInstanceRollingTaskDTO rollingTask);

    /**
     * 更新滚动任务
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   步骤执行次数
     * @param batch          滚动执行批次
     * @param status         任务状态；如果不更新传入null
     * @param startTime      任务开始时间；如果不更新传入null
     * @param endTime        任务结束时间；如果不更新传入null
     * @param totalTime      任务执行总时间；如果不更新传入null
     */
    void updateRollingTask(long stepInstanceId,
                           int executeCount,
                           int batch,
                           RunStatusEnum status,
                           Long startTime,
                           Long endTime, Long totalTime);


}
