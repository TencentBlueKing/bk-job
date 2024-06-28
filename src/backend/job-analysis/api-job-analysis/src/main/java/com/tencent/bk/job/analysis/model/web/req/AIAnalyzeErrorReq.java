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

package com.tencent.bk.job.analysis.model.web.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@AllArgsConstructor
@NoArgsConstructor
@ApiModel("AI分析报错信息请求体")
@Data
public class AIAnalyzeErrorReq {

    @ApiModelProperty(value = "任务ID")
    private Long taskInstanceId;

    @ApiModelProperty(value = "步骤ID")
    private Long stepInstanceId;

    @ApiModelProperty(value = "执行次数")
    private Integer executeCount;

    @ApiModelProperty(value = "滚动批次，非滚动步骤不需要传入")
    private Integer batch;

    @ApiModelProperty(value = "执行对象类型")
    private Integer executeObjectType;

    @ApiModelProperty(value = "执行对象资源 ID")
    private Long executeObjectResourceId;

    @ApiModelProperty(value = "文件任务上传下载标识,0-上传,1-下载")
    private Integer mode;

    @ApiModelProperty(value = "报错信息内容")
    @NotEmpty(message = "{validation.constraints.AIAnalyzeErrorContent_empty.message}")
    private String content;
}
