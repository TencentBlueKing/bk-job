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

package com.tencent.bk.job.analysis.model.dto;

import com.tencent.bk.job.analysis.approval.consts.ApprovalStatusEnum;
import lombok.Data;

/**
 * 审批任务
 */
@Data
public class ApprovalTaskDTO {

    private Long id;

    /**
     * 对外暴露的审批任务ID（32位UUID，无连字符，不可猜测）
     */
    private String approvalTaskId;

    private String tenantId;

    private Long appId;

    /**
     * 操作类型，取值见 ApprovalOperationTypeEnum
     */
    private String operationType;

    /**
     * 操作参数快照（JSON，敏感字段已加密）。仅在 insert 时写入，DAO 层不提供 update
     */
    private String operationParams;

    /**
     * dryRun 解析出的概要（JSON）
     */
    private String resolvedSummary;

    /**
     * 发起人。放行时校验 approver == creator；下发下游时的 operator 也只能取这个值
     */
    private String creator;

    private String appCode;

    /**
     * 审批渠道，取值见 ApprovalChannelEnum
     */
    private String approvalChannel;

    /**
     * 审批渠道单据ID，首次回查确认绑定后写入，此后不可更换
     */
    private String approvalTicketId;

    /**
     * 审批渠道拉取单据的时间（毫秒），仅作观测，不参与放行校验
     */
    private Long ticketFetchedAt;

    /**
     * 状态，取值见 ApprovalStatusEnum（DB 中不存在 EXPIRED）
     */
    private String status;

    private String approver;

    private Long approvedAt;

    /**
     * 放行后的操作结果（JSON）
     */
    private String executeResult;

    private Long expireAt;

    private Long consumedAt;

    /**
     * 下发下游执行请求的时刻（毫秒），用于区分"未下发"与"已下发结果未知"
     */
    private Long dispatchedAt;

    private Long createTime;

    /**
     * 是否已过期。EXPIRED 不是持久状态，读取时按 expire_at 惰性判断
     */
    public boolean isExpired(long now) {
        return expireAt != null && expireAt <= now;
    }

    public ApprovalStatusEnum getStatusEnum() {
        return ApprovalStatusEnum.valOf(status);
    }
}
