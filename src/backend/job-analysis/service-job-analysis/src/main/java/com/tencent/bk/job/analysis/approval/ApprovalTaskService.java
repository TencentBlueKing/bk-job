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

package com.tencent.bk.job.analysis.approval;

import com.tencent.bk.job.analysis.approval.channel.model.ApprovalTicket;
import com.tencent.bk.job.analysis.approval.consts.ApprovalChannelEnum;
import com.tencent.bk.job.analysis.approval.consts.ApprovalOperationTypeEnum;
import com.tencent.bk.job.analysis.approval.model.ApprovalCallerContext;
import com.tencent.bk.job.analysis.model.dto.ApprovalTaskDTO;

/**
 * 审批任务的核心编排：创建 / 查询 / 放行 / 作废。
 * <p>
 * 6 个发起接口全部下沉到 {@link #create}，参数快照、渠道路由、状态机、放行校验链<b>只有一份实现</b>。
 */
public interface ApprovalTaskService {

    /**
     * 创建审批任务。
     * <p>
     * 先以 dryRun=true 走一遍下游的真实校验路径（脚本、账号、主机权限、高危规则、配额都在业务层，
     * Bean Validation 挡不住），校验通过才冻结参数快照落库 —— 否则用户会在审批通过之后才拿到失败。
     *
     * @param operationType 操作类型
     * @param params        已 fillAppResourceScope 的原始 v4 请求体
     * @param channel       调用方指定的渠道枚举，为空则用服务端默认渠道；<b>调用方只能选枚举，不能传地址</b>
     * @param caller        调用上下文
     * @return 已落库的审批任务
     */
    ApprovalTaskDTO create(ApprovalOperationTypeEnum operationType,
                           Object params,
                           ApprovalChannelEnum channel,
                           ApprovalCallerContext caller);

    /**
     * 查询审批任务。返回对象的 status 为<b>呈现状态</b>，可能是 DB 中不存在的 EXPIRED
     */
    ApprovalTaskDTO get(String approvalTaskId, ApprovalCallerContext caller);

    /**
     * 放行：回查审批结论，通过全部校验后 CAS 消费并以 dryRun=false 执行。
     * <p>
     * <b>入参只有两个 ID，不接受任何审批结论字段</b> —— 审批结论只能由作业平台自己回查渠道得出，
     * 绝不能由调用方声明。这是整套机制的立命之本：一旦允许调用方传"已通过"，
     * 整条链路就退化成"调用方自己说批了就算批了"。
     *
     * @param approvalTaskId   审批任务ID
     * @param approvalTicketId 审批渠道单据ID
     * @param caller           调用上下文，仅用于归属校验
     * @return 放行处理后的审批任务（status 为呈现状态）
     */
    ApprovalTaskDTO refresh(String approvalTaskId, String approvalTicketId, ApprovalCallerContext caller);

    /**
     * 主动作废：PENDING → CANCELED。只作废本地任务，不反向通知渠道
     */
    ApprovalTaskDTO cancel(String approvalTaskId, ApprovalCallerContext caller);

    /**
     * 应用态取单：审批渠道拉取单据内容用于建单。
     * <p>
     * <b>调用方必须是该任务指派的那个渠道</b>，否则不同渠道之间可以互相枚举单据内容 ——
     * 单据里有脚本明文。租户与渠道 appCode 任一不符都按"任务不存在"返回，不区分"不存在"与"无权访问"。
     * <p>
     * 单据内容<b>只从 DB 读</b>（resolved_summary 与 operation_params），不做实时 dryRun：
     * 每次取单都重新解析既慢，又会让"用户看到的"与"当初批准的"产生新的差异。
     *
     * @param approvalTaskId 审批任务ID
     * @param tenantId       请求头中的租户ID，必须与任务所属租户严格相等
     * @param appCode        调用方应用编码，必须等于该任务 approval_channel 配置的 appCode
     * @return 已脱敏的审批单据
     */
    ApprovalTicket getTicket(String approvalTaskId, String tenantId, String appCode);
}
