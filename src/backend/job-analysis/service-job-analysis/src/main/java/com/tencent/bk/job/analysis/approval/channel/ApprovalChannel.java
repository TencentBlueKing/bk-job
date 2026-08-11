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
 * <b>纯 pull 模式，只有一个方法</b>：建单由渠道自己来 get_approval_ticket 取内容完成，作业平台不主动推送；
 * 作废也只作废本地任务、不反向通知渠道。入站方向<b>不存在</b>审批结果回调接口 ——
 * 少一个入站结论入口，就少一整类防伪造与防重放的攻击面。
 *
 * <h2>渠道准入条件（接入前必须逐条人工确认，有一条不满足即不予接入）</h2>
 * <ol>
 *     <li>查询接口<b>能回带 approval_task_id</b>。不满足则绑定关系无从验证，放行校验链的绑定证明一步失效。
 *     <i>可被动发现</i>：回查响应为空时该步会拒绝。</li>
 *     <li><b>绑定关系在建单时确立</b>，且其值必须等于渠道调用 get_approval_ticket 时使用的 approval_task_id
 *     （渠道不得拿 A 的单据内容建单、却登记 B 的 task id）。不满足则可被诱导建出"张冠李戴"的单据。
 *     <i>不可校验，纯文档约定</i>。</li>
 *     <li><b>绑定关系事后不可被调用方更改</b>。渠道若提供"更新单据关联业务 ID"之类的接口，必须对本类单据禁用。
 *     不满足则已批准单据可被改指到另一个任务。<i>不可校验，纯文档约定</i>。</li>
 *     <li><b>渠道不得对该类单据配置自动审批 / 免审 / 代批规则</b>。不满足则 approver 会被自动填成发起人本人，
 *     "approver == creator"自动满足，放行链实际只剩绑定证明一道，机制形同虚设。
 *     <i>不可校验，纯文档约定</i>；仅有"疑似秒批"代理指标可事后观测。</li>
 *     <li>单据内容（尤其 script_content）<b>不得写入可检索的日志或搜索索引</b>。
 *     <i>不可校验，纯文档约定</i>。</li>
 *     <li>多租户环境下，渠道调用取单接口时<b>必须透传 X-Bk-Tenant-Id</b>。
 *     <i>可被动发现</i>：不传时渠道自己会收到 401。</li>
 * </ol>
 * <b>关于这 6 条的执行力度</b>：它们全部只是文档约定，<b>无一可由作业平台机器校验</b>。
 * 条件 1 与 6 能被动发现，条件 2/3/4/5 完全无法从服务端观测或验证，只能靠接入时由渠道方书面确认
 * 并写入接入记录。"有一条不满足即不予接入"描述的是<b>人工评审门禁，不是技术控制</b>。
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
