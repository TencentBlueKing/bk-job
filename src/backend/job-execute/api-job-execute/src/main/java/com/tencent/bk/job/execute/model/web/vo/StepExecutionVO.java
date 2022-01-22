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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.common.util.json.DecimalFormatJsonSerializer;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@ApiModel("步骤实例执行情况")
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class StepExecutionVO {
    @ApiModelProperty("步骤实例ID")
    private Long stepInstanceId;
    @ApiModelProperty("执行次数,默认为0")
    private Integer retryCount;
    @ApiModelProperty("步骤名称")
    private String name;
    @ApiModelProperty("步骤类型,1-脚本，2-文件，3-人工确认")
    private Integer type;
    @ApiModelProperty("人工确认信息")
    private String confirmMessage;
    @ApiModelProperty("人工确认理由")
    private String confirmReason;
    @ApiModelProperty("通知方式")
    private List<String> notifyChannelNameList;
    @ApiModelProperty("确认人")
    private List<String> userList;
    @ApiModelProperty("确认角色")
    private List<String> roleNameList;
    @ApiModelProperty("步骤操作人")
    private String operator;
    @ApiModelProperty("总耗时")
    @JsonSerialize(using = DecimalFormatJsonSerializer.class)
    private Long totalTime;
    @ApiModelProperty("步骤状态,1-等待执行，2-正在执行，3-执行成功，4-执行失败，5-跳过，6-忽略错误，7-等待用户，8-手动结束，9-状态异常，10-强制终止中，11-强制终止成功，12-强制终止失败")
    private Integer status;
    @ApiModelProperty("步骤状态描述")
    private String statusDesc;
    @ApiModelProperty("开始时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long startTime;
    @ApiModelProperty("结束时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long endTime;
    @ApiModelProperty("是否当前执行步骤")
    private boolean currentStepRunning;
    @ApiModelProperty("是否最后一个步骤")
    private Boolean isLastStep;
    @ApiModelProperty("滚动执行批次总数;如果非滚动执行，那么该值为空")
    private Integer totalBatch;
    @ApiModelProperty("滚动执行当前执行批次;如果非滚动执行，那么该值为空")
    private Integer currentBatch;
    @ApiModelProperty("滚动配置名称")
    private String rollingConfigName;
}
