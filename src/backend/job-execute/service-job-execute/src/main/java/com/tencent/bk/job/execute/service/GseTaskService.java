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

import com.tencent.bk.job.execute.model.GseTaskDTO;

/**
 * GSE 任务 Service
 */
public interface GseTaskService {

    /**
     * 保存 GSE 任务
     *
     * @param gseTask GSE 任务
     */
    Long saveGseTask(GseTaskDTO gseTask);

    /**
     * 更新 GSE 任务
     *
     * @param gseTask GSE 任务
     * @return 是否更新成功
     */
    boolean updateGseTask(GseTaskDTO gseTask);

    /**
     * 获取 GSE 任务
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   步骤执行次数
     * @param batch          滚动执行批次
     * @return GSE 任务
     */
    GseTaskDTO getGseTask(long stepInstanceId, int executeCount, Integer batch);

    /**
     * 获取 GSE 任务
     *
     * @param gseTaskId GSE任务ID
     * @return GSE 任务
     */
    GseTaskDTO getGseTask(long gseTaskId);
}
