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
 * 审批任务状态。
 * <p>
 * 状态机只有 6 条迁移：
 * <pre>
 * 创建     → PENDING
 * PENDING  → EXECUTING（refresh 校验链全部通过 + CAS 原子消费）
 * PENDING  → REJECTED （回查得到 REJECTED，这是 CAS 之前唯一允许的状态变更）
 * PENDING  → CANCELED （主动作废）
 * EXECUTING→ EXECUTED （下游返回成功）
 * EXECUTING→ FAILED   （下游明确返回业务失败，确定未执行）
 * </pre>
 * 不存在 PENDING → FAILED。CAS 之前的任何校验失败都不得写终态。
 * <p>
 * EXPIRED 不是持久状态：读取时用 expire_at 惰性判断，仅作为对外呈现值，不落库。
 */
public enum ApprovalStatusEnum {

    PENDING,
    EXECUTING,
    EXECUTED,
    REJECTED,
    CANCELED,
    FAILED,
    /**
     * 惰性判断出的呈现值，DB 中不存在该状态
     */
    EXPIRED;

    public static ApprovalStatusEnum valOf(String status) {
        if (status == null) {
            return null;
        }
        for (ApprovalStatusEnum statusEnum : values()) {
            if (statusEnum.name().equals(status)) {
                return statusEnum;
            }
        }
        return null;
    }

    /**
     * 是否为终态。终态任务的 refresh 直接返回既有结论，不再回查、不再执行。
     */
    public boolean isFinalStatus() {
        return this == EXECUTED || this == REJECTED || this == CANCELED || this == FAILED;
    }
}
