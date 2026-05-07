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

package com.tencent.bk.job.execute.model.web.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.constant.CompatibleType;
import com.tencent.bk.job.common.util.json.DecimalFormatJsonSerializer;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "步骤执行详情-废弃")
@Data
@Deprecated
@CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.DEPLOY,
    explain = "使用 StepExecuteDetailV2VO 参数替换。发布完成后可以删除")
public class StepExecutionDetailVO {
    @Schema(description = "步骤实例ID")
    private Long stepInstanceId;
    @Schema(description = "执行次数,默认为0")
    private Integer retryCount;
    @Schema(description = "步骤执行是否结束")
    private boolean finished;
    @Schema(description = "步骤名称")
    private String name;
    @Schema(description = "开始时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long startTime;
    @Schema(description = "结束时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long endTime;
    @Schema(description = "总耗时")
    @JsonSerialize(using = DecimalFormatJsonSerializer.class)
    private Long totalTime;
    @Schema(description = "步骤状态,1-等待执行，2-正在执行，3-执行成功，4-执行失败，5-跳过，6-忽略错误，7-等待用户，8-手动结束，9-状态异常" +
        "，10-强制终止中，11-强制终止成功，12-强制终止失败，13-确认终止，14-被丢弃，15-滚动等待")
    private Integer status;
    @Schema(description = "步骤状态描述")
    private String statusDesc;
    @Schema(description = "Agent作业执行结果分组")
    private List<ExecutionResultGroupVO> resultGroups;
    @Schema(description = "是否是作业中最后一个步骤")
    private Boolean isLastStep;
    @Schema(description = "步骤类型，1-脚本，2-文件，3-人工确认")
    private Integer type;
    /**
     * 步骤执行模式
     *
     * @see com.tencent.bk.job.execute.common.constants.StepRunModeEnum
     */
    @Schema(description = "步骤执行模式。1-单次全量执行(非滚动步骤)；2-滚动全量执行(滚动步骤)；3-滚动分批执行(滚动步骤)")
    private Integer runMode;
    @Schema(description = "步骤包含的滚动任务;如果非滚动步骤，那么该值为空")
    private List<RollingStepBatchTaskVO> rollingTasks;
}
