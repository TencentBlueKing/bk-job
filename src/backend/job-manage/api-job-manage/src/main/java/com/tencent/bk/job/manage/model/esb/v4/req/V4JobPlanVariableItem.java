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
import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteTargetDTO;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;

/**
 * OpenAPI V4 创建执行方案时的变量覆盖项。
 * 仅支持按变量名定位模板变量，并覆盖其默认值；其他字段（描述/类型/是否必填等）由模板决定，传入时被忽略。
 */
@Getter
@Setter
public class V4JobPlanVariableItem {

    /**
     * 变量名，按模板变量名进行匹配；必填且不可为空白。
     */
    @JsonProperty("name")
    @NotBlank(message = "{validation.constraints.InvalidJobPlanVariableName_empty.message}")
    private String name;

    /**
     * 变量值。可选，变量类型为字符串/命名空间/密文/关联数组/索引数组/执行账号时填写；执行账号填写账号 ID 字符串。
     */
    @JsonProperty("value")
    private String value;

    /**
     * 执行目标。可选，变量类型为执行目标时填写。
     */
    @JsonProperty("execute_target")
    private V4ExecuteTargetDTO executeTarget;

    /**
     * 是否沿用模板默认值，默认 false。
     * 为 true 时忽略 value，保留模板的变量默认值；与 value 不互斥。
     */
    @JsonProperty("follow_template")
    private Boolean followTemplate;

    public boolean isFollowTemplate() {
        return Boolean.TRUE.equals(followTemplate);
    }
}
