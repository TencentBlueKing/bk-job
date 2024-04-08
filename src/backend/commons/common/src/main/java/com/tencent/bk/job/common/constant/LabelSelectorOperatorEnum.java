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

package com.tencent.bk.job.common.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Label selector 操作符
 */
public enum LabelSelectorOperatorEnum {
    NOT_EXISTS("not_exists"),
    EQUALS("equals"),
    IN("in"),
    NOT_EQUALS("not_equals"),
    NOT_IN("not_in"),
    EXISTS("exists"),
    GREATER_THAN("gt"),
    LESS_THAN("lt");

    private final String op;

    LabelSelectorOperatorEnum(String op) {
        this.op = op;
    }

    @JsonValue
    public String getOp() {
        return op;
    }

    public static LabelSelectorOperatorEnum valOf(String op) {
        for (LabelSelectorOperatorEnum operatorEnum : values()) {
            if (operatorEnum.getOp().equals(op)) {
                return operatorEnum;
            }
        }
        throw new IllegalArgumentException("No LabelSelectorOperatorEnum constant: " + op);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static LabelSelectorOperatorEnum forOp(String op) {
        return LabelSelectorOperatorEnum.valOf(op);
    }
}
