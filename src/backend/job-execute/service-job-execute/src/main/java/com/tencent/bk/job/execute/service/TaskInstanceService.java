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
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;

import java.util.List;

/**
 * 作业执行实例Service
 */
public interface TaskInstanceService {

    long addTaskInstance(TaskInstanceDTO taskInstance);

    TaskInstanceDTO getTaskInstance(long taskInstanceId);

    /**
     * 保存步骤实例
     *
     * @param stepInstance 步骤实例
     * @return 步骤实例ID
     */
    long addStepInstance(StepInstanceDTO stepInstance);

    /**
     * 获取作业实例详情-包含步骤信息和全局变量信息
     *
     * @param taskInstanceId 作业实例 ID
     * @return
     */
    TaskInstanceDTO getTaskInstanceDetail(long taskInstanceId);

    List<StepInstanceBaseDTO> listStepInstanceByTaskInstanceId(long taskInstanceId);

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
     * @param stepInstanceId 步骤实例ID
     * @return 步骤基本信息
     */
    StepInstanceDTO getStepInstanceDetail(long stepInstanceId);

    /**
     * 获取作业的第一个步骤实例
     *
     * @param taskInstanceId 作业实例ID
     * @return 作业第一个步骤实例
     */
    StepInstanceBaseDTO getFirstStepInstance(long taskInstanceId);

    void updateTaskStatus(long taskInstanceId, int status);

    List<Long> getTaskStepIdList(long taskInstanceId);

    void updateTaskCurrentStepId(long taskInstanceId, Long stepInstanceId);

    void resetTaskStatus(long taskInstanceId);

    void updateStepStatus(long stepInstanceId, int status);

    /**
     * 重试步骤操作-重置步骤执行状态
     *
     * @param stepInstanceId 步骤实例ID
     */
    void resetStepExecuteInfoForRetry(long stepInstanceId);

    /**
     * 作业恢复执行-重置作业执行状态
     *
     * @param taskInstanceId 作业实例ID
     */
    void resetTaskExecuteInfoForRetry(long taskInstanceId);

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
     * 更新作业的执行信息
     *
     * @param taskInstanceId 作业实例ID
     * @param status         作业状态
     * @param currentStepId  当前步骤实例ID
     * @param startTime      开始时间
     * @param endTime        结束时间
     * @param totalTime      总耗时
     */
    void updateTaskExecutionInfo(long taskInstanceId, RunStatusEnum status, Long currentStepId,
                                 Long startTime, Long endTime, Long totalTime);

    /**
     * 更新步骤的执行信息
     *
     * @param stepInstanceId 步骤实例ID
     * @param status         步骤执行状态
     * @param startTime      开始时间
     * @param endTime        结束时间
     * @param totalTime      总耗时
     */
    void updateStepExecutionInfo(long stepInstanceId, RunStatusEnum status,
                                 Long startTime, Long endTime, Long totalTime);


    /**
     * 更新解析之后的脚本参数
     *
     * @param stepInstanceId      步骤实例ID
     * @param resolvedScriptParam 解析之后的脚本参数
     */
    void updateResolvedScriptParam(long stepInstanceId, String resolvedScriptParam);

    /**
     * 更新变量解析之后的源文件
     *
     * @param stepInstanceId      步骤实例ID
     * @param resolvedFileSources
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

    Integer countTaskInstances(Long appId, Long minTotalTime, Long maxTotalTime, TaskStartupModeEnum taskStartupMode,
                               TaskTypeEnum taskType, List<Byte> runStatusList, Long fromTime, Long toTime);

    Integer countStepInstances(Long appId, List<Long> stepIdList, StepExecuteTypeEnum stepExecuteType,
                               ScriptTypeEnum scriptType, RunStatusEnum runStatus, Long fromTime, Long toTime);

    Integer countFastPushFile(Long appId, Integer transferMode, Boolean localUpload, RunStatusEnum runStatus,
                              Long fromTime, Long toTime);

    List<Long> getJoinedAppIdList();

    boolean hasExecuteHistory(Long appId, Long cronTaskId, Long fromTime, Long toTime);

    List<Long> listTaskInstanceId(Long appId, Long fromTime, Long toTime, int offset, int limit);
}
