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

package com.tencent.bk.job.common.cc.model.bizset;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CMDB接口请求实体类，定义业务集过滤业务的规则
 */
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class Rule {

    public static final String OPERATOR_EQUAL = "equal";
    public static final String OPERATOR_NOT_EQUAL = "not_equal";
    public static final String OPERATOR_IN = "in";
    public static final String OPERATOR_NOT_IN = "not_in";
    public static final String OPERATOR_LESS = "less";
    public static final String OPERATOR_LESS_OR_EQUAL = "less_or_equal";
    public static final String OPERATOR_GREATER = "greater";
    public static final String OPERATOR_GREATER_OR_EQUAL = "greater_or_equal";
    public static final String OPERATOR_BETWEEN = "between";
    public static final String OPERATOR_NOT_BETWEEN = "not_between";

    /**
     * 业务字段名
     */
    private String field;

    /**
     * 操作符，可选值 equal,not_equal,in,not_in,
     * less,less_or_equal,greater,greater_or_equal,
     * between,not_between
     */
    private String operator;

    /**
     * 业务字段取值，根据字段不同可为不同类型
     */
    private Object value;
}
