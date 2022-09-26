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

import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;

import java.util.Map;
import java.util.function.Function;

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
     * 获取下一个步骤实例
     *
     * @param taskInstanceId   作业实例ID
     * @param currentStepOrder 当前步骤的顺序
     * @return 步骤实例；如果当前为最后一个步骤实例，那么返回null
     */
    StepInstanceBaseDTO getNextStepInstance(long taskInstanceId, int currentStepOrder);

    /**
     * 获取步骤包含的主机(源+目标)
     *
     * @param stepInstance 步骤实例
     * @return 步骤实例包含的主机列表
     */
    <K> Map<K, HostDTO> computeStepHosts(StepInstanceBaseDTO stepInstance,
                                         Function<? super HostDTO, K> keyMapper);

    /**
     * 获取步骤包含的主机(源+目标)
     *
     * @param stepInstanceId 步骤实例ID
     * @return 步骤实例包含的主机列表
     */
    <K> Map<K, HostDTO> computeStepHosts(long stepInstanceId,
                                         Function<? super HostDTO, K> keyMapper);


}
