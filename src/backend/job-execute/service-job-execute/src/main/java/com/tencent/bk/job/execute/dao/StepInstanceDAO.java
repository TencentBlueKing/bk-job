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

import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.model.ConfirmStepInstanceDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.FileStepInstanceDTO;
import com.tencent.bk.job.execute.model.ScriptStepInstanceDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;

import java.util.List;
import java.util.Map;

/**
 * 任务步骤实例 DAO
 */
public interface StepInstanceDAO {
    Long addStepInstanceBase(StepInstanceBaseDTO stepInstance);

    void addScriptStepInstance(StepInstanceDTO stepInstance);

    void addFileStepInstance(StepInstanceDTO stepInstance);

    void addConfirmStepInstance(StepInstanceDTO stepInstance);

    StepInstanceBaseDTO getStepInstanceBase(Long taskInstanceId, long stepInstanceId);

    /**
     * 获取步骤信息。注意，在分库分表的部署架构下，由于缺少 taskInstanceId （分片键）作为查询条件，该 SQL 执行性能较低；慎用！！
     *
     * @param stepInstanceId 步骤实例 ID
     */
    StepInstanceBaseDTO getStepInstanceBase(long stepInstanceId);

    /**
     * 获取作业的第一个步骤实例
     *
     * @param taskInstanceId 作业实例ID
     * @return 作业第一个步骤实例
     */
    StepInstanceBaseDTO getFirstStepInstanceBase(Long taskInstanceId);

    /**
     * 获取下一个步骤实例
     *
     * @param taskInstanceId   作业实例ID
     * @param currentStepOrder 当前步骤的顺序
     * @return 步骤实例；如果当前为最后一个步骤实例，那么返回null
     */
    StepInstanceBaseDTO getNextStepInstance(Long taskInstanceId, int currentStepOrder);

    ScriptStepInstanceDTO getScriptStepInstance(Long taskInstanceId, long stepInstanceId);

    FileStepInstanceDTO getFileStepInstance(Long taskInstanceId, long stepInstanceId);

    ConfirmStepInstanceDTO getConfirmStepInstance(Long taskInstanceId, long stepInstanceId);

    List<StepInstanceBaseDTO> listStepInstanceBaseByTaskInstanceId(Long taskInstanceId);

    void resetStepStatus(Long taskInstanceId, long stepInstanceId);

    void resetStepExecuteInfoForRetry(Long taskInstanceId, long stepInstanceId);

    void addStepExecuteCount(Long taskInstanceId, long stepInstanceId);

    void updateStepStatus(Long taskInstanceId, long stepInstanceId, int status);

    void updateStepStartTime(Long taskInstanceId, long stepInstanceId, Long startTime);

    /**
     * 更新步骤启动时间 - 仅当启动时间为空的的场景
     *
     * @param stepInstanceId 步骤实例ID
     * @param startTime      启动时间
     */
    void updateStepStartTimeIfNull(Long taskInstanceId, long stepInstanceId, Long startTime);

    void updateStepEndTime(Long taskInstanceId, long stepInstanceId, Long endTime);

    void addStepInstanceExecuteCount(Long taskInstanceId, long stepInstanceId);

    void updateStepTotalTime(Long taskInstanceId, long stepInstanceId, long totalTime);

    /**
     * 更新步骤的执行信息
     *
     * @param taskInstanceId 任务实例 ID
     * @param stepInstanceId 步骤实例ID
     * @param status         步骤执行状态
     * @param startTime      开始时间
     * @param endTime        结束时间
     * @param totalTime      总耗时
     */
    void updateStepExecutionInfo(Long taskInstanceId,
                                 long stepInstanceId,
                                 RunStatusEnum status,
                                 Long startTime,
                                 Long endTime,
                                 Long totalTime);

    /**
     * 更新解析之后的脚本参数
     *
     * @param taskInstanceId      任务实例 ID
     * @param stepInstanceId      步骤实例ID
     * @param isSecureParam       是否为敏感参数
     * @param resolvedScriptParam 解析之后的脚本参数
     */
    void updateResolvedScriptParam(Long taskInstanceId,
                                   long stepInstanceId,
                                   boolean isSecureParam,
                                   String resolvedScriptParam);

    /**
     * 更新变量解析之后的源文件
     *
     * @param taskInstanceId      任务实例 ID
     * @param stepInstanceId      步骤实例ID
     * @param resolvedFileSources 解析后的源文件信息
     */
    void updateResolvedSourceFile(Long taskInstanceId,
                                  long stepInstanceId,
                                  List<FileSourceDTO> resolvedFileSources);

    /**
     * 更新变量解析之后的目标路径
     *
     * @param taskInstanceId     任务实例 ID
     * @param stepInstanceId     步骤实例ID
     * @param resolvedTargetPath 解析之后的目标路径
     */
    void updateResolvedTargetPath(Long taskInstanceId, long stepInstanceId, String resolvedTargetPath);

    /**
     * 更新确认理由
     *
     * @param taskInstanceId 任务实例 ID
     * @param stepInstanceId 步骤实例ID
     * @param confirmReason  确认理由
     */
    void updateConfirmReason(Long taskInstanceId, long stepInstanceId, String confirmReason);

    /**
     * 更新步骤实例操作者
     *
     * @param taskInstanceId 任务实例 ID
     * @param stepInstanceId 步骤实例ID
     * @param operator       操作者
     */
    void updateStepOperator(Long taskInstanceId, long stepInstanceId, String operator);

    /**
     * 获取前一个可执行步骤实例
     *
     * @param taskInstanceId   任务实例ID
     * @param currentStepOrder 当前步骤的顺序
     * @return 可执行步骤实例
     */
    StepInstanceBaseDTO getPreExecutableStepInstance(Long taskInstanceId, int currentStepOrder);

    /**
     * 获取步骤实例 ID 和步骤顺序的映射关系
     *
     * @param taskInstanceId 任务实例ID
     * @return 步骤实例 ID 和步骤顺序的映射关系
     */
    Map<Long, Integer> listStepInstanceIdAndStepOrderMapping(Long taskInstanceId);

    /**
     * 根据taskInstanceId获取一个stepInstanceId，用于快速脚本/文件任务
     *
     * @param taskInstanceId 作业实例ID
     * @return 步骤实例ID
     */
    Long getStepInstanceId(Long taskInstanceId);

    /**
     * 根据stepInstanceId获取脚本类型
     *
     * @param taskInstanceId 任务实例ID
     * @param stepInstanceId 步骤实例ID
     * @return 脚本类型
     */
    Byte getScriptTypeByStepInstanceId(Long taskInstanceId, long stepInstanceId);

    /**
     * 更新步骤实例的当前滚动执行批次
     *
     * @param taskInstanceId 任务实例ID
     * @param stepInstanceId 步骤实例ID
     * @param batch          滚动执行批次
     */
    void updateStepCurrentBatch(Long taskInstanceId, long stepInstanceId, int batch);

    /**
     * 更新步骤实例的当前滚动执行批次
     *
     * @param taskInstanceId 任务实例ID
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     */
    void updateStepCurrentExecuteCount(Long taskInstanceId, long stepInstanceId, int executeCount);

    /**
     * 更新步骤实例的滚动配置ID
     *
     * @param taskInstanceId  任务实例ID
     * @param stepInstanceId  步骤实例ID
     * @param rollingConfigId 滚动配置ID
     */
    void updateStepRollingConfigId(Long taskInstanceId, long stepInstanceId, long rollingConfigId);

    List<Long> getTaskStepInstanceIdList(Long taskInstanceId);
}
