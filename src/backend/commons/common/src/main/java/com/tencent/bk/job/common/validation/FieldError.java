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

package com.tencent.bk.job.common.validation;

import java.util.Collection;

/**
 * 字段验证错误
 */
public class FieldError {

    /**
     * 字段校验错误类型
     */
    private final ErrorType type;
    /**
     * 字段名
     */
    private final Path field;
    /**
     * 校验不通过的值
     */
    private final Object badValue;
    /**
     * 详细错误
     */
    private final String detail;

    private FieldError(ErrorType type, Path field, Object badValue, String detail) {
        this.type = type;
        this.field = field;
        this.badValue = badValue;
        this.detail = detail;
    }

    enum ErrorType {
        /**
         * 必填字段没有值（比如空字符、null, 空的列表）
         */
        Required("Required value"),
        /**
         * 字段值重复
         */
        Duplicate("Duplicate value"),
        /**
         * 不合法-通用
         */
        Invalid("Invalid value"),
        /**
         * 无法识别的枚举值
         */
        NotSupported("Unsupported value"),
        /**
         * 字段对应的字符超长
         */
        TooLong("Too long"),
        /**
         * 字段对应的列表包含太多的元素
         */
        TooMany("Too many"),
        /**
         * 表示值与该字段的 schema 定义不匹配
         */
        TypeInvalid("Invalid value");

        private final String value;

        ErrorType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public static FieldError invalid(Path field, Object value, String detail) {
        return new FieldError(ErrorType.Invalid, field, value, detail);
    }

    public static FieldError required(Path field, String detail) {
        return new FieldError(ErrorType.Required, field, "", detail);
    }

    public static FieldError duplicate(Path field, Object value) {
        return new FieldError(ErrorType.Duplicate, field, value, "");
    }

    public static FieldError notSupported(Path field, Object value, Collection<String> validValues) {
        String detail = "supported values: " + String.join(", ", validValues);
        return new FieldError(ErrorType.NotSupported, field, value, detail);
    }

    public static FieldError tooLong(Path field, Object value, int maxLength) {
        String detail = "must have at most " + maxLength + " chars";
        return new FieldError(ErrorType.TooLong, field, value, detail);
    }

    public static FieldError tooMany(Path field, int actualSize, int maxSize) {
        String detail = "must have at most " + maxSize + " items";
        return new FieldError(ErrorType.TooMany, field, actualSize, detail);
    }

    public ErrorType getType() {
        return type;
    }

    public Path getField() {
        return field;
    }

    public Object getBadValue() {
        return badValue;
    }

    public String getDetail() {
        return detail;
    }

    @Override
    public String toString() {
        String s = field.toString();
        if (type == ErrorType.Required || type == ErrorType.TooLong) {
            s += ": " + type.toString();
        } else {
            s += ": " + type.toString() + ": " + (badValue == null ? "null" : badValue.toString());
        }
        if (detail != null && !detail.isEmpty()) {
            s += ": " + detail;
        }
        return s;
    }
}
