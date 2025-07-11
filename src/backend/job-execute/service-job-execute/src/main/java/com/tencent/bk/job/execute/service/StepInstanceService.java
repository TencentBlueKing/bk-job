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

package com.tencent.bk.job.execute.service;

import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.ExecuteObjectCompositeKey;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;

import java.util.Collection;
import java.util.List;
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
     * 获取步骤包含的执行对象(源+目标)
     *
     * @param stepInstance 步骤实例
     * @return 步骤实例包含的主机列表
     */
    <K> Map<K, ExecuteObject> computeStepExecuteObjects(StepInstanceBaseDTO stepInstance,
                                                        Function<? super ExecuteObject, K> keyMapper);

    /**
     * 获取步骤实例
     *
     * @param stepInstanceId 步骤实例ID
     * @return 步骤实例
     */
    StepInstanceBaseDTO getStepInstanceBase(long stepInstanceId);

    /**
     * 保存步骤实例
     *
     * @param stepInstance 步骤实例
     * @return 步骤实例ID
     */
    long addStepInstance(StepInstanceDTO stepInstance);

    /**
     * 获取作业实例下的所有步骤实例
     *
     * @param taskInstanceId 作业实例 ID
     * @return 步骤实例列表
     */
    List<StepInstanceBaseDTO> listBaseStepInstanceByTaskInstanceId(long taskInstanceId);

    /**
     * 获取作业实例下的所有步骤实例(详细)
     *
     * @param taskInstanceId 作业实例 ID
     * @return 步骤实例列表
     */
    List<StepInstanceDTO> listStepInstanceByTaskInstanceId(long taskInstanceId);

    /**
     * 获取步骤基本信息
     *
     * @param stepInstanceId 步骤实例ID
     * @return 步骤基本信息
     */
    StepInstanceBaseDTO getBaseStepInstance(long stepInstanceId);

    /**
     * 获取步骤基本信息
     *
     * @param appId          业务 ID
     * @param stepInstanceId 步骤实例ID
     * @return 步骤基本信息
     */
    StepInstanceBaseDTO getBaseStepInstance(long appId, long stepInstanceId);

    /**
     * 获取步骤基本信息
     *
     * @param appId          业务 ID
     * @param taskInstanceId 作业实例ID
     * @param stepInstanceId 步骤实例ID
     * @return 步骤基本信息
     */
    StepInstanceBaseDTO getBaseStepInstance(long appId, long taskInstanceId, long stepInstanceId)
        throws NotFoundException;

    /**
     * 获取步骤基本信息
     *
     * @param stepInstanceId 步骤实例ID
     * @return 步骤基本信息
     */
    StepInstanceDTO getStepInstanceDetail(long stepInstanceId) throws NotFoundException;

    /**
     * 获取步骤基本信息
     *
     * @param appId          业务 ID
     * @param stepInstanceId 步骤实例ID
     * @return 步骤基本信息
     */
    StepInstanceDTO getStepInstanceDetail(long appId, long stepInstanceId) throws NotFoundException;

    /**
     * 获取步骤基本信息
     *
     * @param appId          业务 ID
     * @param taskInstanceId 作业实例ID
     * @param stepInstanceId 步骤实例ID
     * @return 步骤基本信息
     */
    StepInstanceDTO getStepInstanceDetail(long appId, long taskInstanceId, long stepInstanceId)
        throws NotFoundException;

    /**
     * 获取作业的第一个步骤实例
     *
     * @param taskInstanceId 作业实例ID
     * @return 作业第一个步骤实例
     */
    StepInstanceBaseDTO getFirstStepInstance(long taskInstanceId);

    List<Long> getTaskStepIdList(long taskInstanceId);

    void updateStepStatus(long stepInstanceId, int status);

    /**
     * 重试步骤操作-重置步骤执行状态
     *
     * @param stepInstanceId 步骤实例ID
     */
    void resetStepExecuteInfoForRetry(long stepInstanceId);

    void resetStepStatus(long stepInstanceId);

    void updateStepStartTime(long stepInstanceId, Long startTime);

    /**
     * 更新步骤启动时间 - 仅当启动时间为空
     *
     * @param stepInstanceId 步骤实例ID
     * @param startTime      启动时间
     */
    void updateStepStartTimeIfNull(long stepInstanceId, Long startTime);

    void updateStepEndTime(long stepInstanceId, Long endTime);

    /**
     * 步骤重试次数+1
     *
     * @param stepInstanceId 步骤实例ID
     */
    void addStepInstanceExecuteCount(long stepInstanceId);

    void updateStepTotalTime(long stepInstanceId, long totalTime);

    /**
     * 更新步骤的执行信息
     *
     * @param stepInstanceId 步骤实例ID
     * @param status         步骤执行状态
     * @param startTime      开始时间
     * @param endTime        结束时间
     * @param totalTime      总耗时
     */
    void updateStepExecutionInfo(long stepInstanceId,
                                 RunStatusEnum status,
                                 Long startTime,
                                 Long endTime,
                                 Long totalTime);


    /**
     * 更新解析之后的脚本参数
     *
     * @param stepInstanceId      步骤实例ID
     * @param isSecureParam       是否为敏感参数
     * @param resolvedScriptParam 解析之后的脚本参数
     */
    void updateResolvedScriptParam(long stepInstanceId, boolean isSecureParam, String resolvedScriptParam);

    /**
     * 更新变量解析之后的源文件
     *
     * @param stepInstanceId      步骤实例ID
     * @param resolvedFileSources 解析后的源文件信息
     */
    void updateResolvedSourceFile(long stepInstanceId, List<FileSourceDTO> resolvedFileSources);

    /**
     * 更新变量解析之后的目标路径
     *
     * @param stepInstanceId     步骤实例ID
     * @param resolvedTargetPath 解析之后的目标路径
     */
    void updateResolvedTargetPath(long stepInstanceId, String resolvedTargetPath);

    /**
     * 更新确认理由
     *
     * @param stepInstanceId 步骤实例ID
     * @param confirmReason  确认理由
     */
    void updateConfirmReason(long stepInstanceId, String confirmReason);

    /**
     * 更新步骤操作人
     *
     * @param stepInstanceId 步骤实例ID
     * @param operator       操作人
     */
    void updateStepOperator(long stepInstanceId, String operator);

    /**
     * 获取上一步骤实例(可执行的，不包含人工确认这种)
     *
     * @param taskInstanceId 任务实例ID
     * @param stepInstanceId 当前步骤实例ID
     * @return 上一步骤实例
     */
    StepInstanceDTO getPreExecutableStepInstance(long taskInstanceId, long stepInstanceId);

    /**
     * 根据 taskInstanceId 获取快速任务步骤实例详情
     *
     * @param taskInstanceId 任务实例ID
     * @return 步骤详情
     */
    StepInstanceDTO getStepInstanceByTaskInstanceId(long taskInstanceId);

    /**
     * 根据 executeObjectCompositeKey 获取步骤实例中的执行对象
     *
     * @param stepInstance              步骤实例
     * @param executeObjectCompositeKey 执行对象复合 KEY
     */
    ExecuteObject findExecuteObjectByCompositeKey(StepInstanceBaseDTO stepInstance,
                                                  ExecuteObjectCompositeKey executeObjectCompositeKey);

    /**
     * 根据 executeObjectCompositeKeys 批量获取步骤实例中的执行对象
     *
     * @param stepInstance               步骤实例
     * @param executeObjectCompositeKeys 执行对象复合 KEYS
     */
    List<ExecuteObject> findExecuteObjectByCompositeKeys(
        StepInstanceBaseDTO stepInstance,
        Collection<ExecuteObjectCompositeKey> executeObjectCompositeKeys
    );

    /**
     * 根据 appId,stepInstanceId 获取所属任务实例ID
     *
     * @param appId          Job业务ID
     * @param stepInstanceId 步骤实例ID
     * @return 任务实例ID
     */
    Long getStepTaskInstanceId(long appId, long stepInstanceId);
}
