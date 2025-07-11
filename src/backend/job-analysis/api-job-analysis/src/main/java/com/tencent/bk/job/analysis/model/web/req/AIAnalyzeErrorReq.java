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

package com.tencent.bk.job.analysis.model.web.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@ApiModel("AI分析报错信息请求体")
@Data
public class AIAnalyzeErrorReq {

    @ApiModelProperty(value = "任务ID")
    @NotNull(message = "{validation.constraints.AIAnalyzeError_taskInstanceIdEmpty.message}")
    private Long taskInstanceId;

    @ApiModelProperty("步骤执行类型：1-脚本，2-文件")
//    @NotNull(message = "{validation.constraints.AIAnalyzeError_stepExecuteTypeEmpty.message}")
    private Integer stepExecuteType;

    @ApiModelProperty(value = "步骤ID")
    @NotNull(message = "{validation.constraints.AIAnalyzeError_stepInstanceIdEmpty.message}")
    private Long stepInstanceId;

    @ApiModelProperty(value = "执行次数")
    @NotNull(message = "{validation.constraints.AIAnalyzeError_executeCountEmpty.message}")
    private Integer executeCount;

    @ApiModelProperty(value = "滚动批次，非滚动步骤不需要传入")
    @NotNull(message = "{validation.constraints.AIAnalyzeError_batchEmpty.message}")
    private Integer batch;

    @ApiModelProperty(value = "执行对象类型")
    @NotNull(message = "{validation.constraints.AIAnalyzeError_executeObjectTypeEmpty.message}")
    private Integer executeObjectType;

    @ApiModelProperty(value = "执行对象资源 ID")
    @NotNull(message = "{validation.constraints.AIAnalyzeError_executeObjectResourceIdEmpty.message}")
    private Long executeObjectResourceId;

    @ApiModelProperty(value = "文件任务上传下载标识,0-上传,1-下载")
    private Integer mode;

    @ApiModelProperty(value = "脚本任务报错信息内容")
    private String content;
}
