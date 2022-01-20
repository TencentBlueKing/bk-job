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

package com.tencent.bk.job.execute.model.web.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.common.util.json.DecimalFormatJsonSerializer;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@ApiModel("步骤执行详情")
@Data
public class StepExecutionDetailVO {
    @ApiModelProperty("步骤实例ID")
    private Long stepInstanceId;
    @ApiModelProperty("执行次数,默认为0")
    private Integer retryCount;
    @ApiModelProperty("步骤执行是否结束")
    private boolean finished;
    @ApiModelProperty("步骤名称")
    private String name;
    @ApiModelProperty("开始时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long startTime;
    @ApiModelProperty("结束时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long endTime;
    @ApiModelProperty("总耗时")
    @JsonSerialize(using = DecimalFormatJsonSerializer.class)
    private Long totalTime;
    @ApiModelProperty("步骤状态,1-等待执行，2-正在执行，3-执行成功，4-执行失败，5-跳过，6-忽略错误，7-等待用户，8-手动结束，9-状态异常，10-强制终止中，11-强制终止成功，12-强制终止失败")
    private Integer status;
    @ApiModelProperty("步骤状态描述")
    private String statusDesc;
    @ApiModelProperty("gseTaskId")
    private String gseTaskId;
    @ApiModelProperty("Agent作业执行结果分组")
    private List<ExecutionResultGroupVO> resultGroups;
    @ApiModelProperty("是否是作业中最后一个步骤")
    private Boolean isLastStep;
    @ApiModelProperty("步骤类型，1-脚本，2-文件，3-人工确认")
    private Integer type;
    /**
     * 步骤执行模式
     *
     * @see com.tencent.bk.job.execute.common.constants.StepRunModeEnum
     */
    @ApiModelProperty("是否滚动执行步骤。1-单次全量执行；2-滚动全量执行；3-滚动分批执行")
    private Integer runMode;
    @ApiModelProperty("步骤包含的滚动任务;如果非滚动步骤，那么该值为空")
    private List<StepRollingTaskVO> rollingTasks;
}
