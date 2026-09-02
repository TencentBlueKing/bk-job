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

package com.tencent.bk.job.analysis.approval.consts;

/**
 * 审批内容的风险等级，由高危命中与目标规模推导，渲染在审批内容中供审批人参考。
 * <p>
 * 它只是<b>给审批人的提示</b>，不参与放行校验：等级低不代表可以不看内容。
 */
public enum ApprovalRiskLevelEnum {

    /**
     * 命中高危脚本规则，或执行对象数超过 {@link #HIGH_RISK_EXECUTE_OBJECT_COUNT}
     */
    HIGH,

    /**
     * 执行对象数超过 {@link #MEDIUM_RISK_EXECUTE_OBJECT_COUNT}
     */
    MEDIUM,

    LOW;

    /**
     * 执行对象数超过该值即视为大规模操作
     */
    public static final int HIGH_RISK_EXECUTE_OBJECT_COUNT = 100;

    /**
     * 执行对象数超过该值即视为规模较大。取值偏保守：宁可多提示，不可漏提示
     */
    public static final int MEDIUM_RISK_EXECUTE_OBJECT_COUNT = 10;

    public String getNameI18nKey() {
        return "task.approval.riskLevel." + name().toLowerCase();
    }
}
