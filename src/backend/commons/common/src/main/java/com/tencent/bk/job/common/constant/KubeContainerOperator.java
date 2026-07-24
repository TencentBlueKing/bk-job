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

package com.tencent.bk.job.common.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 动态条件过滤器支持的运算符全集。
 * 命名/含义对齐 bk-cmdb filter，去掉 *_i 大小写变体；对外协议使用全小写名。
 * 包含普通比较/字符串匹配/存在性/集合长度/对象过滤等能力，实际对外暴露的子集由 OperatorDispatcher 控制。
 */
public enum KubeContainerOperator {
    EQUAL("equal"),
    NOT_EQUAL("not_equal"),
    IN("in"),
    NOT_IN("not_in"),
    LESS("less"),
    LESS_OR_EQUAL("less_or_equal"),
    GREATER("greater"),
    GREATER_OR_EQUAL("greater_or_equal"),
    BEGINS_WITH("begins_with"),
    NOT_BEGINS_WITH("not_begins_with"),
    CONTAINS("contains"),
    NOT_CONTAINS("not_contains"),
    ENDS_WITH("ends_with"),
    NOT_ENDS_WITH("not_ends_with"),
    DATETIME_LESS("datetime_less"),
    DATETIME_LESS_OR_EQUAL("datetime_less_or_equal"),
    DATETIME_GREATER("datetime_greater"),
    DATETIME_GREATER_OR_EQUAL("datetime_greater_or_equal"),
    IS_NULL("is_null"),
    IS_NOT_NULL("is_not_null"),
    EXIST("exist"),
    NOT_EXIST("not_exist"),
    IS_EMPTY("is_empty"),
    IS_NOT_EMPTY("is_not_empty"),
    SIZE("size"),
    FILTER_OBJECT("filter_object");

    @JsonValue
    private final String value;

    KubeContainerOperator(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @JsonCreator
    public static KubeContainerOperator fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (KubeContainerOperator op : values()) {
            if (op.value.equals(value)) {
                return op;
            }
        }
        throw new IllegalArgumentException("Unknown KubeContainerOperator: " + value);
    }
}
