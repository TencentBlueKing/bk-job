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

package com.tencent.bk.job.manage.model.esb.v4.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.validation.CheckEnum;
import com.tencent.bk.job.common.validation.NoXss;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * OpenAPI V4 作业模板全局变量。变量以 name 为标识，不支持改名。
 */
@Getter
@Setter
public class V4JobTemplateGlobalVarReq {

    @JsonProperty("name")
    @NotBlank(message = "{validation.constraints.InvalidTemplateGlobalVarName_empty.message}")
    @Size(max = 60, message = "{validation.constraints.InvalidTemplateGlobalVarName_outOfLength.message}")
    @NoXss(fieldName = "global_var_list[].name")
    private String name;

    /**
     * 变量类型。1 字符串、2 命名空间、3 主机列表、4 密文、5 关联数组、6 索引数组、7 执行账号。
     */
    @JsonProperty("type")
    @NotNull(message = "{validation.constraints.InvalidTemplateGlobalVarType.message}")
    @CheckEnum(enumClass = TaskVariableTypeEnum.class,
        message = "{validation.constraints.InvalidTemplateGlobalVarType.message}")
    private Integer type;

    @JsonProperty("description")
    private String description;

    /**
     * 是否必填。1 是、0 否，缺省为 0。
     */
    @JsonProperty("required")
    private Integer required;

    /**
     * 变量默认值。主机列表类型请改用 execute_target。
     */
    @JsonProperty("value")
    private String value;

    /**
     * 主机列表类型变量的默认值。
     */
    @JsonProperty("execute_target")
    @Valid
    private V4JobTemplateExecuteTargetReq executeTarget;
}
