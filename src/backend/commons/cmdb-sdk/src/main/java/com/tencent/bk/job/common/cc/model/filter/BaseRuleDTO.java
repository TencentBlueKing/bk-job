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

package com.tencent.bk.job.common.cc.model.filter;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * cmdb 查询规则
 */
@Getter
@Setter
@ToString
public class BaseRuleDTO implements IRule {
    /**
     * 字段名
     */
    private String field;
    /**
     * 操作符,可选值 equal,not_equal,in,not_in,less,less_or_equal,greater,greater_or_equal,between,not_between,contains
     */
    private String operator;
    /**
     * 操作值，不同的operator对应不同的value格式
     */
    private Object value;

    public BaseRuleDTO() {
    }

    public BaseRuleDTO(String field, String operator, Object value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

    public static BaseRuleDTO in(String field, Object value) {
        return new BaseRuleDTO(field, RuleOperatorEnum.IN.getOperator(), value);
    }

    public static BaseRuleDTO notIn(String field, Object value) {
        return new BaseRuleDTO(field, RuleOperatorEnum.NOT_IN.getOperator(), value);
    }

    public static BaseRuleDTO equals(String field, Object value) {
        return new BaseRuleDTO(field, RuleOperatorEnum.EQUAL.getOperator(), value);
    }

    public static BaseRuleDTO notEquals(String field, Object value) {
        return new BaseRuleDTO(field, RuleOperatorEnum.NOT_EQUAL.getOperator(), value);
    }

    public static BaseRuleDTO contains(String field, Object value) {
        return new BaseRuleDTO(field, RuleOperatorEnum.CONTAINS.getOperator(), value);
    }

    public static BaseRuleDTO filterObject(String field, Object value) {
        return new BaseRuleDTO(field, RuleOperatorEnum.FILTER_OBJECT.getOperator(), value);
    }

    public static BaseRuleDTO exists(String field) {
        // cmdb 统一解析逻辑会解析value，没填的话会报错;传入 true(对过滤结果没有影响)临时规避该问题
        return new BaseRuleDTO(field, RuleOperatorEnum.EXIST.getOperator(), true);
    }

    public static BaseRuleDTO notExists(String field) {
        // cmdb 统一解析逻辑会解析value，没填的话会报错;传入 true(对过滤结果没有影响)临时规避该问题
        return new BaseRuleDTO(field, RuleOperatorEnum.NOT_EXIST.getOperator(), true);
    }

    public static BaseRuleDTO greaterThan(String field, Object value) {
        return new BaseRuleDTO(field, RuleOperatorEnum.GREATER.getOperator(), value);
    }

    public static BaseRuleDTO lessThan(String field, Object value) {
        return new BaseRuleDTO(field, RuleOperatorEnum.LESS.getOperator(), value);
    }

    @Override
    protected BaseRuleDTO clone() {
        return new BaseRuleDTO(field, operator, value);
    }
}
