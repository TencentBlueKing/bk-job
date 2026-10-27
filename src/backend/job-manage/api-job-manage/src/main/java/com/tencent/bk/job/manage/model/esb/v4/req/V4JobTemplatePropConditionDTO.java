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
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * OpenAPI V4 作业模板容器字段筛选条件。
 * <p>
 * 不暴露运算符：每个字段的匹配方式是固定的（容器名称、Pod 名称按包含匹配，容器 UID 与 Pod 标签按等值匹配），
 * 由服务端按字段补齐。这样接口写入的条件与页面写入的完全同形，互相都能正常回显。
 */
@Getter
@Setter
@NoArgsConstructor
public class V4JobTemplatePropConditionDTO {

    /**
     * 筛选字段，取值见 {@code QueryableContainerField}。
     */
    @JsonProperty("field")
    @NotBlank(message = "{validation.constraints.InvalidTemplatePropConditionField.message}")
    private String field;

    /**
     * 取值。容器名称、Pod 名称、容器 UID 支持英文逗号分隔的多值；Pod 标签为 K8s 标签选择器表达式，
     * 其中的逗号是表达式自身的 AND 分隔符，不做多值拆分。
     */
    @JsonProperty("value")
    @NotBlank(message = "{validation.constraints.InvalidTemplatePropConditionValue.message}")
    private String value;

    public V4JobTemplatePropConditionDTO(String field, String value) {
        this.field = field;
        this.value = value;
    }
}
