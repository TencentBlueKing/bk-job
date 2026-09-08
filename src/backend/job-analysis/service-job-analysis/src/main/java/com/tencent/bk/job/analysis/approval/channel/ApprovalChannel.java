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

package com.tencent.bk.job.analysis.approval.channel;

import com.tencent.bk.job.analysis.approval.channel.model.ApprovalResult;
import com.tencent.bk.job.analysis.approval.consts.ApprovalChannelEnum;
import com.tencent.bk.job.analysis.model.dto.ApprovalTaskDTO;

/**
 * 审批渠道出站 SPI。
 * <p>
 * <b>纯 pull 模式</b>：渠道自己来 get_approval_content 取内容建单，入站方向<b>不存在</b>审批结果回调接口。
 *
 * <h2>渠道准入条件（人工评审门禁，无一可由作业平台机器校验，接入前须由渠道方书面确认）</h2>
 * <ol>
 *     <li>查询接口<b>能回带 approval_task_id</b>，否则放行链的绑定证明一步失效。</li>
 *     <li><b>绑定关系在建单时确立</b>，其值必须等于渠道取内容时使用的 approval_task_id，
 *     否则可被诱导建出"张冠李戴"的单据。</li>
 *     <li><b>绑定关系事后不可被调用方更改</b>，否则已批准单据可被改指到另一个任务。</li>
 *     <li><b>不得对该类单据配置自动审批 / 免审 / 代批规则</b>，否则"approver == creator"自动满足，
 *     放行链只剩绑定证明一道。</li>
 *     <li>审批内容（尤其 script_content）<b>不得写入可检索的日志或搜索索引</b>。</li>
 *     <li>取内容时<b>必须以任务发起人本人的身份调用</b>，否则会被按"任务不存在"拒绝。</li>
 * </ol>
 */
public interface ApprovalChannel {

    /**
     * 渠道类型，与 {@link ApprovalChannelEnum} 一一对应
     */
    ApprovalChannelEnum getChannelType();

    /**
     * 回查审批结论 —— 唯一可信的授权来源。
     * <p>
     * 返回的 {@link ApprovalResult#getApprovalTaskId()} 为强制字段，用于证明单据与任务的绑定关系。
     * <b>任何异常/超时一律向上抛出</b>，由调用方 fail-closed 处理（保持 PENDING、不放行），
     * <b>禁止在实现内部兜底为 APPROVED</b>，也禁止把"查不到"当成"已通过"。
     *
     * @param task             审批任务，渠道实现可用其 tenantId / creator 等信息构造请求
     * @param approvalTicketId 待回查的单据 ID
     * @return 审批结论，不返回 null（无法确定时抛异常而不是返回空）
     */
    ApprovalResult queryResult(ApprovalTaskDTO task, String approvalTicketId);
}
