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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.KubeContainerOperator;
import com.tencent.bk.job.common.constant.QueryableContainerField;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.dto.KubePropCondition;
import com.tencent.bk.job.common.util.OperatorDispatcher;
import com.tencent.bk.job.common.util.label.selector.LabelSelectorParse;
import com.tencent.bk.job.common.util.label.selector.LabelSelectorParseException;
import org.apache.commons.lang3.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 动态条件过滤器中单条 {@link KubePropCondition} 的程序式校验：
 * <ol>
 *   <li>字段在 {@link QueryableContainerField} 白名单且被 {@link OperatorDispatcher} 对外暴露</li>
 *   <li>运算符与字段独立配置一致（按字段维度派发）</li>
 *   <li>value 形态与运算符匹配：valueless（exist/not_exist）/ 集合（in/not_in）/ 标量（其余）；
 *       数值字段值需可解析为数字，时间字段值需为 ISO-8601 时间串；
 *       {@link QueryableContainerField#POD_LABELS} 的 value 需为合法 K8s label selector 表达式。</li>
 * </ol>
 * 校验失败抛 {@link InvalidParamException}，由全局异常处理转为标准错误响应。
 */
public final class KubePropConditionValidator {

    /**
     * 不需要 value 的运算符（仅判断字段存在与否）。
     */
    private static final Set<KubeContainerOperator> VALUELESS_OPERATORS = EnumSet.of(
        KubeContainerOperator.EXIST, KubeContainerOperator.NOT_EXIST);

    /**
     * value 必须是非空集合的运算符。
     */
    private static final Set<KubeContainerOperator> COLLECTION_OPERATORS = EnumSet.of(
        KubeContainerOperator.IN, KubeContainerOperator.NOT_IN);

    private KubePropConditionValidator() {
    }

    public static void validate(List<KubePropCondition> conditions) {
        if (conditions == null) {
            return;
        }
        for (KubePropCondition condition : conditions) {
            validate(condition);
        }
    }

    public static void validate(KubePropCondition condition) {
        if (condition == null) {
            throwInvalid("propCondition can not be null");
        }
        QueryableContainerField field = resolveField(condition.getField());
        KubeContainerOperator operator = resolveOperator(condition.getOperator());
        if (!OperatorDispatcher.isOperatorAllowed(field, operator)) {
            throwInvalid("operator [" + condition.getOperator()
                + "] is not allowed for field [" + condition.getField() + "]");
        }
        validateValueShape(field, operator, condition.getValue());
    }

    private static QueryableContainerField resolveField(String fieldName) {
        if (StringUtils.isBlank(fieldName)) {
            throwInvalid("propCondition.field can not be blank");
        }
        QueryableContainerField field = QueryableContainerField.fromFieldName(fieldName);
        if (field == null || !OperatorDispatcher.isFieldExposed(field)) {
            throwInvalid("unsupported propCondition.field [" + fieldName + "]");
        }
        return field;
    }

    private static KubeContainerOperator resolveOperator(String operatorName) {
        if (StringUtils.isBlank(operatorName)) {
            throwInvalid("propCondition.operator can not be blank");
        }
        try {
            return KubeContainerOperator.fromValue(operatorName);
        } catch (IllegalArgumentException e) {
            throw new InvalidParamException(
                ErrorCode.ILLEGAL_PARAM_WITH_REASON,
                new Object[]{"unsupported propCondition.operator [" + operatorName + "]"});
        }
    }

    private static void validateValueShape(QueryableContainerField field,
                                           KubeContainerOperator operator,
                                           Object value) {
        if (VALUELESS_OPERATORS.contains(operator)) {
            // exist/not_exist 不关心 value，传了也忽略
            return;
        }
        if (COLLECTION_OPERATORS.contains(operator)) {
            if (!(value instanceof Collection) || ((Collection<?>) value).isEmpty()) {
                throwInvalid("operator [" + operator.getValue()
                    + "] requires a non-empty array value");
            }
            for (Object element : (Collection<?>) value) {
                validateScalarType(field, element);
            }
            return;
        }
        // 其余运算符要求标量值（pod_labels equal 也是标量字符串：标签选择器表达式）
        if (value == null || value instanceof Collection) {
            throwInvalid("operator [" + operator.getValue() + "] requires a single scalar value");
        }
        validateScalarType(field, value);
    }

    /**
     * 按字段类型校验单个标量值的形态：
     * <ul>
     *   <li>数值字段：值需可解析为数字</li>
     *   <li>时间字段：值需为 ISO-8601 时间串</li>
     *   <li>{@link QueryableContainerField#POD_LABELS}：值需为合法 K8s label selector 表达式</li>
     *   <li>其余类型不做强约束</li>
     * </ul>
     */
    private static void validateScalarType(QueryableContainerField field, Object value) {
        if (value == null) {
            throwInvalid("propCondition value element can not be null for field [" + field.getFieldName() + "]");
        }
        if (field == QueryableContainerField.POD_LABELS) {
            validatePodLabelsExpression(value);
            return;
        }
        switch (field.getFieldType()) {
            case NUMERIC:
                if (!isNumeric(value)) {
                    throwInvalid("field [" + field.getFieldName() + "] requires numeric value, but got: " + value);
                }
                break;
            case DATETIME:
            case TIMESTAMP:
                if (!isIsoDateTime(value)) {
                    throwInvalid("field [" + field.getFieldName()
                        + "] requires ISO-8601 datetime value, but got: " + value);
                }
                break;
            default:
                break;
        }
    }

    /**
     * 校验 pod_labels 的 K8s label selector 表达式合法性。
     * 复用 {@link LabelSelectorParse} 进行词法/语法/语义（key、value 的 DNS 规则）一体化校验。
     */
    private static void validatePodLabelsExpression(Object value) {
        if (!(value instanceof String)) {
            throwInvalid("field [pod_labels] requires a label selector expression string, but got: "
                + value.getClass().getSimpleName());
        }
        String expression = ((String) value).trim();
        if (expression.isEmpty()) {
            throwInvalid("field [pod_labels] requires a non-empty label selector expression");
        }
        try {
            LabelSelectorParse.parseToLabelSelectExprList(expression);
        } catch (LabelSelectorParseException e) {
            throwInvalid("field [pod_labels] has invalid label selector expression: " + e.getMessage());
        } catch (RuntimeException e) {
            // Parser 内部对个别非法构造可能抛 IllegalArgumentException / IndexOutOfBoundsException 等；
            // 一律归一为参数非法
            throwInvalid("field [pod_labels] has invalid label selector expression: " + e.getMessage());
        }
    }

    private static boolean isNumeric(Object value) {
        if (value instanceof Number) {
            return true;
        }
        if (value instanceof String) {
            try {
                Double.parseDouble(((String) value).trim());
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    private static boolean isIsoDateTime(Object value) {
        if (!(value instanceof String)) {
            return false;
        }
        String text = ((String) value).trim();
        try {
            DateTimeFormatter.ISO_DATE_TIME.parse(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void throwInvalid(String reason) {
        throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_REASON, new Object[]{reason});
    }
}
