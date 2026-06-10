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

package com.tencent.bk.job.common.cc.util;

import com.tencent.bk.job.common.cc.model.filter.BaseRuleDTO;
import com.tencent.bk.job.common.cc.model.filter.ComposeRuleDTO;
import com.tencent.bk.job.common.cc.model.filter.PropertyFilterDTO;
import com.tencent.bk.job.common.cc.model.filter.RuleConditionEnum;
import com.tencent.bk.job.common.cc.model.filter.RuleOperatorEnum;
import com.tencent.bk.job.common.constant.KubeContainerOperator;
import com.tencent.bk.job.common.constant.QueryableContainerField;
import com.tencent.bk.job.common.model.dto.KubePropCondition;
import com.tencent.bk.job.common.model.dto.LabelSelectExprDTO;
import com.tencent.bk.job.common.util.label.selector.LabelSelectorParse;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * 动态条件 propConditions → bk-cmdb 查询规则的翻译工具。
 * <p>
 * 字段按 {@code container_} / {@code pod_} 前缀分别落到 cmdb 的 container / pod 过滤器；cmdb 模型字段名
 * 为去掉该前缀后的部分（如 {@code container_container_uid} → {@code container_uid}、{@code pod_name} → {@code name}）。
 * <p>
 * 特例：{@link QueryableContainerField#POD_LABELS} 的 value 为 K8s label selector 表达式字符串
 * （如 {@code "k1=v1,k2=v2"}），翻译时使用 {@link LabelSelectorParse} 解析为结构化表达式列表，
 * 再以 {@link RuleOperatorEnum#FILTER_OBJECT} 形态构造嵌套 cmdb 规则（条件 AND）落到 podFilter。
 * <p>
 * 未对外暴露（{@link com.tencent.bk.job.common.util.OperatorDispatcher}）的运算符在校验阶段已被拒绝；
 * 但 translator 是协议层底座，对所有 {@link KubeContainerOperator} 取值均可正确翻译，未知运算符抛
 * {@link IllegalArgumentException} 暴露漏配。
 */
public final class KubePropConditionTranslator {

    /**
     * pod_labels 在 cmdb pod 模型中的字段名（去掉 {@code pod_} 前缀）。
     */
    private static final String LABELS_CMDB_FIELD = "labels";

    private KubePropConditionTranslator() {
    }

    /**
     * 把 propConditions 翻译为 cmdb 规则并追加到对应的 container / pod 过滤器。
     */
    public static void appendRules(List<KubePropCondition> conditions,
                                   PropertyFilterDTO containerFilter,
                                   PropertyFilterDTO podFilter) {
        if (CollectionUtils.isEmpty(conditions)) {
            return;
        }
        for (KubePropCondition condition : conditions) {
            QueryableContainerField field = QueryableContainerField.fromFieldName(condition.getField());
            KubeContainerOperator operator = KubeContainerOperator.fromValue(condition.getOperator());

            // pod_labels 走标签选择器表达式特殊翻译
            if (field == QueryableContainerField.POD_LABELS) {
                podFilter.addRule(buildPodLabelsFilterRule(condition.getValue()));
                continue;
            }

            String fieldName = field.getFieldName();
            String cmdbField = fieldName.substring(fieldName.indexOf('_') + 1);
            BaseRuleDTO rule = toCmdbRule(cmdbField, operator, condition.getValue());
            if (fieldName.startsWith("container_")) {
                containerFilter.addRule(rule);
            } else {
                podFilter.addRule(rule);
            }
        }
    }

    private static BaseRuleDTO toCmdbRule(String cmdbField, KubeContainerOperator operator, Object value) {
        switch (operator) {
            case EQUAL:
                return BaseRuleDTO.equals(cmdbField, value);
            case NOT_EQUAL:
                return BaseRuleDTO.notEquals(cmdbField, value);
            case IN:
                return BaseRuleDTO.in(cmdbField, value);
            case NOT_IN:
                return BaseRuleDTO.notIn(cmdbField, value);
            case LESS:
                return new BaseRuleDTO(cmdbField, RuleOperatorEnum.LESS.getOperator(), value);
            case LESS_OR_EQUAL:
                return new BaseRuleDTO(cmdbField, RuleOperatorEnum.LESS_OR_EQUAL.getOperator(), value);
            case GREATER:
                return new BaseRuleDTO(cmdbField, RuleOperatorEnum.GREATER.getOperator(), value);
            case GREATER_OR_EQUAL:
                return new BaseRuleDTO(cmdbField, RuleOperatorEnum.GREATER_OR_EQUAL.getOperator(), value);
            case CONTAINS:
                return BaseRuleDTO.contains(cmdbField, value);
            case FILTER_OBJECT:
                return BaseRuleDTO.filterObject(cmdbField, value);
            case EXIST:
                return BaseRuleDTO.exists(cmdbField);
            case NOT_EXIST:
                return BaseRuleDTO.notExists(cmdbField);
            default:
                throw new IllegalArgumentException(
                    "Unsupported operator for cmdb container query: " + operator.getValue());
        }
    }

    /**
     * 把 pod_labels 的标签选择器表达式字符串翻译为 cmdb 嵌套 filter_object 规则。
     * <p>
     * 例：{@code "app=nginx,env in (prod,test)"} →
     * <pre>
     * filter_object("labels", ComposeRule(AND, [
     *     equal("app", "nginx"),
     *     in("env", ["prod", "test"])
     * ]))
     * </pre>
     * <p>
     * value 校验在 {@code KubePropConditionValidator#validatePodLabelsExpression} 已完成，此处直接信任入参。
     */
    private static BaseRuleDTO buildPodLabelsFilterRule(Object value) {
        String expression = (String) value;
        List<LabelSelectExprDTO> exprList = LabelSelectorParse.parseToLabelSelectExprList(expression);
        ComposeRuleDTO labelsComposeRule = new ComposeRuleDTO(RuleConditionEnum.AND.getCondition());
        for (LabelSelectExprDTO expr : exprList) {
            labelsComposeRule.addRule(buildLabelFilterRule(expr));
        }
        return BaseRuleDTO.filterObject(LABELS_CMDB_FIELD, labelsComposeRule);
    }

    /**
     * 单条标签选择器表达式 → cmdb 规则（沿用 cmdb 标签查询协议）。
     */
    private static BaseRuleDTO buildLabelFilterRule(LabelSelectExprDTO expr) {
        switch (expr.getOperator()) {
            case EQUALS:
                return BaseRuleDTO.equals(expr.getKey(), expr.getValues().get(0));
            case NOT_EQUALS:
                return BaseRuleDTO.notEquals(expr.getKey(), expr.getValues().get(0));
            case IN:
                return BaseRuleDTO.in(expr.getKey(), expr.getValues());
            case NOT_IN:
                return BaseRuleDTO.notIn(expr.getKey(), expr.getValues());
            case EXISTS:
                return BaseRuleDTO.exists(expr.getKey());
            case NOT_EXISTS:
                return BaseRuleDTO.notExists(expr.getKey());
            case LESS_THAN:
                return BaseRuleDTO.lessThan(expr.getKey(), expr.getValues().get(0));
            case GREATER_THAN:
                return BaseRuleDTO.greaterThan(expr.getKey(), expr.getValues().get(0));
            default:
                throw new IllegalArgumentException("Invalid label select operator: " + expr.getOperator());
        }
    }
}
