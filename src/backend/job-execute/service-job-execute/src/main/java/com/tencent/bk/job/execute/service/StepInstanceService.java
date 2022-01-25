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

import com.tencent.bk.job.execute.engine.consts.IpStatus;

import java.util.Map;

/**
 * 作业步骤执行实例 Service
 */
public interface StepInstanceService {
    /**
     * 更新步骤实例的当前滚动执行批次
     *
     * @param stepInstanceId 步骤实例ID
     * @param batch          滚动执行批次
     */
    void updateStepCurrentBatch(long stepInstanceId, int batch);

    /**
     * 更新步骤实例的当前滚动执行批次
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     */
    void updateStepCurrentExecuteCount(long stepInstanceId, int executeCount);

    /**
     * 更新步骤实例的滚动配置ID
     *
     * @param stepInstanceId  步骤实例ID
     * @param rollingConfigId 滚动配置ID
     */
    void updateStepRollingConfigId(long stepInstanceId, long rollingConfigId);

    /**
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   步骤执行次数
     * @return 根据GSE Agent 任务状态分组计数结果
     */
    Map<IpStatus, Integer> countStepGseAgentTaskGroupByStatus(long stepInstanceId, int executeCount);


}
