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
import com.tencent.bk.job.common.cc.model.filter.IRule;
import com.tencent.bk.job.common.cc.model.filter.PropertyFilterDTO;
import com.tencent.bk.job.common.cc.model.filter.RuleConditionEnum;
import com.tencent.bk.job.common.cc.model.filter.RuleOperatorEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.KubeContainerOperator;
import com.tencent.bk.job.common.constant.QueryableContainerField;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.dto.KubePropCondition;
import com.tencent.bk.job.common.model.dto.LabelSelectExprDTO;
import com.tencent.bk.job.common.util.label.selector.LabelSelectorParse;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * 多值分隔符 {@code |}（仅适用于以下字段+运算符组合，与前端"多标签输入框"配套；K8s 名称与 UID 均不允许 {@code |}，
 * 语义无歧义）：
 * <ul>
 *   <li>{@link QueryableContainerField#CONTAINER_NAME} + {@code equal / contains}</li>
 *   <li>{@link QueryableContainerField#POD_NAME} + {@code equal / contains}</li>
 *   <li>{@link QueryableContainerField#CONTAINER_CONTAINER_UID} + {@code equal}</li>
 * </ul>
 * 拆分规则：value 按 {@code |} 切分 → 逐段 trim → 去空段 → 保序去重。规则化后：
 * <ul>
 *   <li>{@code contains} 单值 → {@code contains}；多值 → {@link RuleConditionEnum#OR} 折叠 N 条 contains</li>
 *   <li>{@code equal} 单值 → {@code equal}；多值 → {@code in([...])}（语义等价，更简洁）</li>
 * </ul>
 * 空/仅 {@code |}/仅空段等规则化后为空的输入，直接抛 {@link InvalidParamException}（400）。
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

    /**
     * 多值分隔符（仅在 {@link #PIPE_SPLIT_OPERATORS} 声明的 字段+运算符 组合下生效）。
     */
    private static final String MULTI_VALUE_DELIMITER = "|";

    /**
     * 支持 {@code |} 多值拆分的 字段 → 允许运算符 白名单。
     * 其它字段/运算符组合一律走标量翻译，避免误伤（例如 pod_labels 表达式、数值/时间字段等）。
     */
    private static final Map<QueryableContainerField, Set<KubeContainerOperator>> PIPE_SPLIT_OPERATORS;

    static {
        PIPE_SPLIT_OPERATORS = new EnumMap<>(QueryableContainerField.class);
        PIPE_SPLIT_OPERATORS.put(QueryableContainerField.CONTAINER_NAME,
            EnumSet.of(KubeContainerOperator.EQUAL, KubeContainerOperator.CONTAINS));
        PIPE_SPLIT_OPERATORS.put(QueryableContainerField.POD_NAME,
            EnumSet.of(KubeContainerOperator.EQUAL, KubeContainerOperator.CONTAINS));
        PIPE_SPLIT_OPERATORS.put(QueryableContainerField.CONTAINER_CONTAINER_UID,
            EnumSet.of(KubeContainerOperator.EQUAL));
    }

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

            // pod_labels 走标签选择器表达式特殊翻译（不适用 | 多值语义）
            if (field == QueryableContainerField.POD_LABELS) {
                podFilter.addRule(buildPodLabelsFilterRule(condition.getValue()));
                continue;
            }

            String fieldName = field.getFieldName();
            String cmdbField = fieldName.substring(fieldName.indexOf('_') + 1);

            IRule rule;
            if (isPipeSplitEnabled(field, operator)) {
                rule = toCmdbRuleWithPipeSplit(field, cmdbField, operator, condition.getValue());
            } else {
                rule = toCmdbRule(cmdbField, operator, condition.getValue());
            }

            if (fieldName.startsWith("container_")) {
                containerFilter.addRule(rule);
            } else {
                podFilter.addRule(rule);
            }
        }
    }

    private static boolean isPipeSplitEnabled(QueryableContainerField field, KubeContainerOperator operator) {
        Set<KubeContainerOperator> allowed = PIPE_SPLIT_OPERATORS.get(field);
        return allowed != null && allowed.contains(operator);
    }

    /**
     * 处理白名单字段的 {@code |} 多值语义。规则化后的关键字列表：
     * <ul>
     *   <li>为空 → 抛 {@link InvalidParamException}（400）</li>
     *   <li>单个 → 退化为原运算符（{@code equal} / {@code contains}）单值规则</li>
     *   <li>多个 →
     *     <ul>
     *       <li>{@code equal} 折叠为 {@code in([...])}</li>
     *       <li>{@code contains} 折叠为 {@link RuleConditionEnum#OR} 组合的 N 条 {@code contains}</li>
     *     </ul>
     *   </li>
     * </ul>
     */
    private static IRule toCmdbRuleWithPipeSplit(QueryableContainerField field,
                                                 String cmdbField,
                                                 KubeContainerOperator operator,
                                                 Object value) {
        List<String> keywords = splitPipeKeywords(value);
        if (keywords.isEmpty()) {
            throw new InvalidParamException(
                ErrorCode.ILLEGAL_PARAM_WITH_REASON,
                new Object[]{"field [" + field.getFieldName() + "] operator [" + operator.getValue()
                    + "] requires a non-empty value (after '|' split & trim)"});
        }
        if (keywords.size() == 1) {
            String single = keywords.get(0);
            return (operator == KubeContainerOperator.CONTAINS)
                ? BaseRuleDTO.contains(cmdbField, single)
                : BaseRuleDTO.equals(cmdbField, single);
        }
        // 多值
        if (operator == KubeContainerOperator.EQUAL) {
            // equal 多值等价 in
            return BaseRuleDTO.in(cmdbField, keywords);
        }
        // contains 多值折叠为 OR(contains, contains, ...)
        ComposeRuleDTO or = new ComposeRuleDTO(RuleConditionEnum.OR.getCondition());
        for (String kw : keywords) {
            or.addRule(BaseRuleDTO.contains(cmdbField, kw));
        }
        return or;
    }

    /**
     * 按 {@code |} 切分并规范化：trim → 丢弃空段 → 保序去重。
     */
    private static List<String> splitPipeKeywords(Object rawValue) {
        if (rawValue == null) {
            return new ArrayList<>();
        }
        String[] parts = rawValue.toString().split("\\" + MULTI_VALUE_DELIMITER, -1);
        Set<String> result = new LinkedHashSet<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return new ArrayList<>(result);
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
