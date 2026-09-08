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

package com.tencent.bk.job.execute.model.esb.v4.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import jakarta.validation.Valid;

/**
 * v4 全局变量。
 * <p>
 * 与 v3 的 EsbGlobalVarV3DTO 的区别：主机类变量的取值用 v4 的 {@link V4ExecuteTargetDTO}（支持容器执行对象），
 * 不再用 v3 的 server 结构。
 */
@Data
public class V4GlobalVarDTO {

    /**
     * 变量 ID，与 name 二者至少填一个
     */
    @JsonProperty("id")
    private Long id;

    /**
     * 变量名，与 id 二者至少填一个
     */
    @JsonProperty("name")
    private String name;

    /**
     * 变量值。主机类变量用 executeTarget 传值，其余类型用该字段
     */
    @JsonProperty("value")
    private String value;

    /**
     * 主机类变量的取值
     */
    @JsonProperty("execute_target")
    @Valid
    private V4ExecuteTargetDTO executeTarget;
}
