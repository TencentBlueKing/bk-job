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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("步骤文件信息")
public class ExecuteFileStepVO {

    @ApiModelProperty("源文件列表")
    @JsonProperty("fileSourceList")
    private List<ExecuteFileSourceInfoVO> fileSourceList;

    @ApiModelProperty("目标信息")
    @JsonProperty("fileDestination")
    private ExecuteFileDestinationInfoVO fileDestination;

    @ApiModelProperty("超时")
    private Integer timeout;

    @ApiModelProperty("上传文件限速")
    @JsonProperty("uploadSpeedLimit")
    private Integer originSpeedLimit;

    @ApiModelProperty("下载文件限速")
    @JsonProperty("downloadSpeedLimit")
    private Integer targetSpeedLimit;

    /**
     * 传输模式
     */
    @ApiModelProperty(value = "传输模式： 1 - 严谨模式； 2 - 强制模式；3 - 安全模式", required = true)
    private Integer transferMode;

    @ApiModelProperty("忽略错误 0 - 不忽略 1 - 忽略")
    private Integer ignoreError;
}
