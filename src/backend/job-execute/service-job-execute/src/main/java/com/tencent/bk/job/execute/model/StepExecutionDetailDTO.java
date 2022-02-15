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

package com.tencent.bk.job.execute.model;

import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepRunModeEnum;
import com.tencent.bk.job.execute.common.util.TaskCostCalculator;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 步骤执行详情
 */
@Data
@NoArgsConstructor
public class StepExecutionDetailDTO {
    /**
     * 任务执行分组状态
     */
    List<AgentTaskResultGroupDTO> resultGroups;
    /**
     * 步骤实例ID
     */
    private Long stepInstanceId;
    /**
     * 执行次数
     */
    private Integer executeCount;
    /**
     * 滚动任务-最新批次
     */
    private Integer latestBatch;
    /**
     * 步骤执行是否结束
     */
    private boolean finished;
    /**
     * 步骤名称
     */
    private String name;
    /**
     * 开始时间
     */
    private Long startTime;
    /**
     * 结束时间
     */
    private Long endTime;
    /**
     * 总耗时
     */
    private Long totalTime;
    /**
     * 执行状态
     */
    private int status;
    /**
     * GSE TASK ID
     */
    private String gseTaskId;
    /**
     * 是否是作业的最后一个步骤
     */
    private boolean lastStep;
    /**
     * 步骤类型
     *
     * @see com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum
     */
    private Integer stepType;
    /**
     * 步骤执行模式
     *
     */
    private StepRunModeEnum runMode;
    /**
     * 步骤包含的滚动任务;如果非滚动步骤，那么该值为空
     */
    private List<StepInstanceRollingTaskDTO> rollingTasks;

    public StepExecutionDetailDTO(StepInstanceBaseDTO stepInstance) {
        this.stepInstanceId = stepInstance.getId();
        this.executeCount = stepInstance.getExecuteCount();
        this.name = stepInstance.getName();
        this.setFinished(RunStatusEnum.isFinishedStatus(RunStatusEnum.valueOf(stepInstance.getStatus())));
        this.status = stepInstance.getStatus();
        this.startTime = stepInstance.getStartTime();
        this.endTime = stepInstance.getEndTime();
        this.totalTime = TaskCostCalculator.calculate(stepInstance.getStartTime(), stepInstance.getEndTime(),
            stepInstance.getTotalTime());
        this.lastStep = stepInstance.isLastStep();
        this.stepType = stepInstance.getStepType();
    }
}
