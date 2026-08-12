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

import com.tencent.bk.job.common.model.dto.ResourceScope;
import lombok.Data;

/**
 * 审批单据。
 * <p>
 * <b>这是审批人做判断的唯一信息来源</b>：{@link #approvalContent} 里看不清"要在哪些机器上执行什么"，
 * 就等于逼审批人盲签，本 Issue 的安全价值也随之归零。因此单据必须自包含，关键信息不依赖 AI 对话上下文。
 * <p>
 * <b>单据内容由作业平台渲染成一份 Markdown</b>，而不是交给渠道自行拼装字段：只有作业平台知道哪些信息
 * 是判断风险必需的、哪些必须脱敏，把排版权交出去就等于把"会不会看清"交给了各渠道的实现质量。
 * <p>
 * <b>本期一任务一单据，不做批次合并展示</b>：模型中刻意不引入批次 ID 与聚合渲染概念。
 */
@Data
public class ApprovalTicket {

    private String approvalTaskId;

    /**
     * 单据标题，形如「快速执行脚本 - 某业务 - 37台主机」
     */
    private String title;

    /**
     * 风险等级，取值见 ApprovalRiskLevelEnum
     */
    private String riskLevel;

    private String operationType;

    private ResourceScope scope;

    /**
     * 发起人。审批人必须为发起人本人，渠道据此校验
     */
    private String creator;

    /**
     * 过期时刻（毫秒）。过期后不可再放行，渠道应据此提示审批人
     */
    private Long expireAt;

    /**
     * 单据内容，Markdown 格式。
     * <p>
     * <b>敏感字段一律只出现占位符</b>，明文与密文都不在其中；脚本内容是唯一例外，原样展示 ——
     * 不展示则审批人无从判断风险。
     */
    private String approvalContent;
}
