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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("任务脚本步骤信息")
public class ExecuteScriptStepVO {

    @ApiModelProperty(value = "脚本类型 1-本地脚本 2-引用业务脚本 3-引用公共脚本")
    private Integer scriptSource;

    @ApiModelProperty("脚本 ID")
    private String scriptId;

    @ApiModelProperty("脚本版本 ID")
    private Long scriptVersionId;

    @ApiModelProperty("脚本内容,BASE64编码")
    private String content;

    @ApiModelProperty(value = "脚本类型，1：shell，2：bat，3：perl，4：python，5：powershell，6：sql")
    private Integer scriptLanguage;

    @ApiModelProperty("脚本参数")
    private String scriptParam;

    @ApiModelProperty("脚本超时时间")
    private Integer timeout;

    @ApiModelProperty("执行账户")
    @JsonProperty("account")
    private Long accountId;

    @ApiModelProperty("执行账户名称")
    private String accountName;

    @ApiModelProperty("执行目标")
    private ExecuteTargetVO executeTarget;

    @ApiModelProperty("敏感参数")
    private Integer secureParam;

    @ApiModelProperty("忽略错误 0 - 不忽略 1 - 忽略")
    private Integer ignoreError;
}
