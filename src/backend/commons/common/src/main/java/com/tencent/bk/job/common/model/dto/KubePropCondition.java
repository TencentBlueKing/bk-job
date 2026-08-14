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

package com.tencent.bk.job.common.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.annotation.PersistenceObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 动态过滤器中的一条「字段 + 运算符 + 值」AND 条件。
 * 既参与 Web 入参绑定（间接：Web 层载体内嵌该类型），也参与持久化与运行时流转。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@PersistenceObject
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class KubePropCondition implements Cloneable {

    /**
     * 字段名，对应 QueryableContainerField 中某个枚举值的 fieldName。
     * 仅做白名单校验时映射回枚举；持久化为字符串以保证字段下线后仍可反序列化。
     */
    @JsonProperty("field")
    private String field;

    /**
     * 运算符名，对应 KubeContainerOperator 中某个枚举值的 value。
     */
    @JsonProperty("operator")
    private String operator;

    /**
     * 运算符对应的值，形态由 operator 决定：
     * scalar / List / null（valueless ops）/ 非负整数（size）/ ISO 时间（datetime_*）/ Map（filter_object）。
     */
    @JsonProperty("value")
    private Object value;

    @Override
    public KubePropCondition clone() {
        return new KubePropCondition(field, operator, cloneValue(value));
    }

    /**
     * 拷贝 value：集合/Map 做一层防御性拷贝（元素假定为 JSON 标量等不可变类型）；
     * 标量（String/Number/Boolean 等）不可变，直接共享安全。
     */
    private static Object cloneValue(Object value) {
        if (value instanceof Collection) {
            return new ArrayList<>((Collection<?>) value);
        }
        if (value instanceof Map) {
            return new LinkedHashMap<>((Map<?, ?>) value);
        }
        return value;
    }
}
