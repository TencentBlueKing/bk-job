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

package com.tencent.bk.job.common.util.feature;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Supplier;

/**
 * 特性运行上下文
 */
public class FeatureExecutionContext {
    public static FeatureExecutionContext EMPTY = new FeatureExecutionContext();

    /**
     * 运行时参数
     */
    private final Map<String, Object> params = new HashMap<>();

    /**
     * 运行时参数 Supplier, 用于延迟计算可选参数
     */
    private final Map<String, Supplier<Object>> paramSuppliers = new HashMap<>();

    public static FeatureExecutionContext builder() {
        return new FeatureExecutionContext();
    }

    private FeatureExecutionContext() {
    }

    public Object getParam(String paramName) {
        Object value = this.params.get(paramName);
        if (value != null) {
            return value;
        }
        Supplier<Object> paramSupplier = this.paramSuppliers.get(paramName);
        if (paramSupplier != null) {
            value = paramSupplier.get();
            params.put(paramName, value);
        }
        return value;
    }

    public FeatureExecutionContext addContextParam(String paramName, Object value) {
        this.params.put(paramName, value);
        return this;
    }

    public FeatureExecutionContext addContextParam(String paramName, Supplier<Object> paramValueSupplier) {
        this.paramSuppliers.put(paramName, paramValueSupplier);
        return this;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FeatureExecutionContext.class.getSimpleName() + "[", "]")
            .add("params=" + params)
            .toString();
    }
}
