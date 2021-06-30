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
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("操作日志")
public class TaskOperationLogVO {
    @ApiModelProperty("操作日志ID")
    private Long id;
    @ApiModelProperty("作业实例ID")
    private Long taskInstanceId;
    @ApiModelProperty("操作者")
    private String operator;
    @ApiModelProperty("操作名称")
    private String operationName;
    @ApiModelProperty("操作对应的Code,1-失败重试，2-忽略错误，3-手动跳过，4-强制终止，5-全部重试，6-启动作业，7-人工确认，8-进入下一步")
    private Integer operationCode;
    @ApiModelProperty("步骤实例ID")
    private Long stepInstanceId;
    @ApiModelProperty("步骤执行次数")
    private Integer retry;
    @ApiModelProperty("步骤名称")
    private String stepName;
    @ApiModelProperty("操作时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long createTime;
    @ApiModelProperty("详情-文本")
    private String detail;
}
