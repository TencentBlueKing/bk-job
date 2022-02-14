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

package com.tencent.bk.job.manage.common.consts.rule;

import lombok.Getter;

/**
 * 规则启停状态，1:启用,0:停止
 */
@Getter
public enum HighRiskGrammarRuleStatusEnum {
    SCAN(1, "start"), INTERCEPT(0, "stop");

    private final Integer code;
    private final String name;

    HighRiskGrammarRuleStatusEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getName(Integer type) {
        for (HighRiskGrammarRuleStatusEnum highRiskGrammarRuleStatusEnum : values()) {
            if (highRiskGrammarRuleStatusEnum.code.equals(type)) {
                return highRiskGrammarRuleStatusEnum.getName();
            }
        }
        return "";
    }

    public static HighRiskGrammarRuleStatusEnum valueOf(Integer type) {
        for (HighRiskGrammarRuleStatusEnum highRiskGrammarRuleStatusEnum : values()) {
            if (highRiskGrammarRuleStatusEnum.code.equals(type)) {
                return highRiskGrammarRuleStatusEnum;
            }
        }
        return null;
    }

    /**
     * 判断参数合法性
     */
    public static boolean isValid(Integer code) {
        for (HighRiskGrammarRuleStatusEnum highRiskGrammarRuleStatusEnum : HighRiskGrammarRuleStatusEnum.values()) {
            if (highRiskGrammarRuleStatusEnum.getCode() == code) {
                return true;
            }
        }
        return false;
    }

}
