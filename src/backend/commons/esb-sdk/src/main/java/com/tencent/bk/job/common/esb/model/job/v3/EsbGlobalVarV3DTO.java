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

package com.tencent.bk.job.common.esb.model.job.v3;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

import javax.validation.constraints.Min;

/**
 * 全局变量
 */
@Data
public class EsbGlobalVarV3DTO {
    /**
     * 全局变量ID
     */
    @Min(value = 1L, message = "{validation.constraints.InvalidGlobalVarId.message}")
    @JsonPropertyDescription("Global variable id")
    private Long id;

    /**
     * 全局变量名称
     */
    @JsonPropertyDescription("Global variable name")
    private String name;

    /**
     * 全局变量值，当变量类型为字符、密码、数组时，此变量有效
     */
    @JsonPropertyDescription("Global variable value")
    private String value;

    @JsonProperty("server")
    @JsonPropertyDescription("Value for host variable")
    private EsbServerV3DTO server;

    /**
     * 变量描述
     */
    @JsonPropertyDescription("description")
    private String description;

    /**
     * 变量类型
     */
    @JsonPropertyDescription("Variable type")
    private Integer type;

    /**
     * 变量是否必填
     */
    @JsonPropertyDescription("Required, 1: YES; 0: NO")
    private Integer required;

    /**
     * 变量是否被引用
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean used;

}
