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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("任务步骤信息")
public class ExecuteStepVO {

    @ApiModelProperty("步骤 ID")
    private Long id;

    @ApiModelProperty("步骤类型 1-脚本 2-文件 3-人工确认")
    private Integer type;

    @ApiModelProperty("步骤名称")
    private String name;

    @ApiModelProperty("脚本步骤信息")
    private ExecuteScriptStepVO scriptStepInfo;

    @ApiModelProperty("文件步骤信息")
    private ExecuteFileStepVO fileStepInfo;

    @ApiModelProperty("审批步骤信息")
    private ExecuteApprovalStepVO approvalStepInfo;

    @ApiModelProperty(value = "是否启用滚动执行")
    private boolean rollingEnabled;

    @ApiModelProperty(value = "滚动配置")
    private RollingConfigVO rollingConfig;

}
