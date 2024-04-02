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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Requirement contains values, a key, and an operator that relates the key and values.
 */
public class Requirement {
    private final String key;
    private final Operator operator;
    private final List<String> values;

    public Requirement(String key, Operator operator, List<String> values) {
        this.key = key;
        this.operator = operator;
        this.values = values;
    }

    public String getKey() {
        return key;
    }

    public Operator getOperator() {
        return operator;
    }

    public List<String> getValues() {
        return values;
    }

    public boolean matches(Map<String, String> labels) {
        if (!labels.containsKey(key)) {
            return false;
        }
        String value = labels.get(key);
        switch (operator) {
            case In:
            case Equals:
            case DoubleEquals:
                return values.contains(value);
            case NotIn:
            case NotEquals:
                return !values.contains(value);
            case Exists:
                return true;
            case GreaterThan:
            case LessThan:
                if (!values.isEmpty()) {
                    long labelValue = Long.parseLong(value);
                    long requirementValue = Long.parseLong(values.iterator().next());
                    return (operator == Operator.GreaterThan && labelValue > requirementValue) ||
                        (operator == Operator.LessThan && labelValue < requirementValue);
                }
                return false;
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(key);
        switch (operator) {
            case Equals:
                sb.append("=");
                break;
            case DoubleEquals:
                sb.append("==");
                break;
            case NotEquals:
                sb.append("!=");
                break;
            case In:
                sb.append(" in ");
                break;
            case NotIn:
                sb.append(" notin ");
                break;
            case GreaterThan:
                sb.append(">");
                break;
            case LessThan:
                sb.append("<");
                break;
            case Exists:
            case DoesNotExist:
                return sb.toString();
        }
        if (operator == Operator.In || operator == Operator.NotIn) {
            sb.append("(");
        }
        List<String> sortedValues = new ArrayList<>(values);
        Collections.sort(sortedValues);
        sb.append(String.join(",", sortedValues));
        if (operator == Operator.In || operator == Operator.NotIn) {
            sb.append(")");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Requirement that = (Requirement) o;
        return key.equals(that.key) &&
            operator == that.operator && listEquals(values, that.values);
    }

    private boolean listEquals(List<String> thisList, List<String> thatList) {
        if (thisList == null && thatList == null) {
            return true;
        } else if (thisList != null && thatList != null) {
            return thisList.equals(thatList);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, operator, values);
    }
}
