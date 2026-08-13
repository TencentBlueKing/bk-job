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

import lombok.Data;

/**
 * 由作业平台渲染、交给审批渠道展示给审批人的审批内容。
 * <p>
 * 这是审批人做判断的唯一信息来源，必须自包含：标题、发起人、风险等级等都已渲染进
 * {@link #approvalContent}，不再单独给结构化字段。
 */
@Data
public class ApprovalContent {

    private String approvalTaskId;

    /**
     * 过期时刻（毫秒）。过期后不可再放行，渠道应据此提示审批人
     */
    private Long expireAt;

    /**
     * 审批内容，Markdown 格式。
     * <p>
     * <b>敏感字段一律只出现占位符</b>，明文与密文都不在其中；脚本内容是唯一例外，原样展示 ——
     * 不展示则审批人无从判断风险。
     */
    private String approvalContent;
}
