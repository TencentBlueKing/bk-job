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

package com.tencent.bk.job.analysis.approval.channel.model;

import com.tencent.bk.job.analysis.approval.consts.ApprovalResultStatusEnum;
import lombok.Data;

/**
 * 审批渠道的回查结论。
 * <p>
 * <b>这是整条链路上唯一可信的授权来源</b>：它只能来自作业平台主动回查审批渠道，
 * 绝不由调用方（AI）传入 —— refresh 接口的请求体只有两个 ID，没有任何结论字段。
 */
@Data
public class ApprovalResult {

    /**
     * 审批结论
     */
    private ApprovalResultStatusEnum status;

    /**
     * 审批人。放行时必须严格等于审批任务的 creator
     */
    private String approver;

    private Long approvedAt;

    /**
     * <b>绑定证明（强制字段）</b>：渠道在建单时固化的审批任务 ID。
     * 放行时必须严格等于本次请求的 approvalTaskId，否则拿"另一个任务真实批过的单据"就能放行本任务。
     * 为空即视为绑定关系无从验证，一律拒绝放行。
     */
    private String approvalTaskId;

    private String comment;
}
