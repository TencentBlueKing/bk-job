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

@ApiModel("作业执行情况")
@Data
public class TaskExecutionVO {
    @ApiModelProperty("作业实例 ID")
    private Long taskInstanceId;
    @ApiModelProperty("执行方案 ID")
    private Long taskId;
    @ApiModelProperty("作业模板 ID")
    private Long templateId;
    @ApiModelProperty("是否调试执行方案")
    private Boolean debugTask;
    @ApiModelProperty("作业名称")
    private String name;
    @ApiModelProperty("任务类型，0-作业执行，1-脚本执行，2-文件分发")
    private Integer type;
    @ApiModelProperty("总耗时")
    @JsonSerialize(using = DecimalFormatJsonSerializer.class)
    private Long totalTime;
    @ApiModelProperty("作业状态，1-等待执行，2-正在执行，3-执行成功，4-执行失败，5-跳过，6-忽略错误，7-等待用户，8-手动结束，9-状态异常，10-强制终止中，11-强制终止成功，12-强制终止失败")
    private Integer status;
    @ApiModelProperty("作业状态描述")
    private String statusDesc;
    @ApiModelProperty("开始时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long startTime;
    @ApiModelProperty("结束时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long endTime;
}
