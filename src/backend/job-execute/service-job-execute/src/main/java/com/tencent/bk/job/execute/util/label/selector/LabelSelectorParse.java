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

package com.tencent.bk.job.execute.util.label.selector;

import com.tencent.bk.job.common.constant.LabelSelectorOperatorEnum;
import com.tencent.bk.job.execute.model.LabelSelectExprDTO;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Kubernetes label selector 运算表达式解析。参考 Kubernetes 官方开源代码：
 * <p>
 * https://github.com/kubernetes/kubernetes/blob/master/staging/src/k8s.io/apimachinery/pkg/labels/selector.go
 */
public class LabelSelectorParse {

    public static List<Requirement> parse(String selector) {
        Parser parser = new Parser(selector);
        return parser.parse();
    }

    public static List<LabelSelectExprDTO> parseToLabelSelectExprList(String selector) {
        Parser parser = new Parser(selector);
        List<Requirement> requirements = parser.parse();

        if (CollectionUtils.isEmpty(requirements)) {
            return Collections.emptyList();
        }

        return requirements.stream()
            .map(requirement -> new LabelSelectExprDTO(
                requirement.getKey(),
                mapToLabelSelectorOperatorEnum(requirement.getOperator()),
                requirement.getValues()))
            .collect(Collectors.toList());
    }

    private static LabelSelectorOperatorEnum mapToLabelSelectorOperatorEnum(Operator operator) {
        switch (operator) {
            case Equals:
            case DoubleEquals:
                return LabelSelectorOperatorEnum.EQUALS;
            case In:
                return LabelSelectorOperatorEnum.IN;
            case NotIn:
                return LabelSelectorOperatorEnum.NOT_IN;
            case Exists:
                return LabelSelectorOperatorEnum.EXISTS;
            case DoesNotExist:
                return LabelSelectorOperatorEnum.NOT_EXISTS;
            case NotEquals:
                return LabelSelectorOperatorEnum.NOT_EQUALS;
            case GreaterThan:
                return LabelSelectorOperatorEnum.GREATER_THAN;
            case LessThan:
                return LabelSelectorOperatorEnum.LESS_THAN;
            default:
                throw new IllegalArgumentException("Invalid operator: " + operator);
        }
    }
}
