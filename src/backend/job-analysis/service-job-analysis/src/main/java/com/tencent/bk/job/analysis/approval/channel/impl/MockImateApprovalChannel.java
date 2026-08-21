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

package com.tencent.bk.job.analysis.approval.channel.impl;

import com.tencent.bk.job.analysis.approval.channel.ApprovalChannel;
import com.tencent.bk.job.analysis.approval.channel.model.ApprovalResult;
import com.tencent.bk.job.analysis.approval.consts.ApprovalChannelEnum;
import com.tencent.bk.job.analysis.approval.consts.ApprovalResultStatusEnum;
import com.tencent.bk.job.analysis.config.ApprovalProperties;
import com.tencent.bk.job.analysis.config.condition.ConditionalOnMockImateApprovalEnabled;
import com.tencent.bk.job.analysis.model.dto.ApprovalTaskDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * IMate 渠道的 Mock 实现，用于 IMate 回查接口就绪之前的自测。
 * <p>
 * 审批任务 ID 或渠道单据 ID 命中配置里登记的"视为审批通过"列表即 APPROVED，其余一律 PENDING。
 * <p>
 * <b>底线不能动</b>：未命中一律返回 PENDING，绝不兜底成 APPROVED。开启后审批形同虚设，
 * 因此开关默认 false，且开启期间每次放行都会打出告警，由运维确保它不出现在生产配置中。
 * <p>
 * Mock 期间"绑定证明"与"approver == creator"两项校验会自动满足，
 * <b>Mock 下跑通不等于放行校验链有效</b>。
 */
@Slf4j
@Component
@ConditionalOnMockImateApprovalEnabled
public class MockImateApprovalChannel implements ApprovalChannel {

    private final ApprovalProperties approvalProperties;

    public MockImateApprovalChannel(ApprovalProperties approvalProperties) {
        this.approvalProperties = approvalProperties;
        log.warn("!!! Approval channel IMATE is running in MOCK mode, DO NOT use it in production !!! "
            + "Approval results are NOT queried from the real approval channel. "
            + "Registered mock approved id count: {}",
            CollectionUtils.size(approvalProperties.getChannels().getImate().getMock().getApprovedIds()));
    }

    @Override
    public ApprovalChannelEnum getChannelType() {
        return ApprovalChannelEnum.IMATE;
    }

    @Override
    public ApprovalResult queryResult(ApprovalTaskDTO task, String approvalTicketId) {
        List<String> approvedIds = approvalProperties.getChannels().getImate().getMock().getApprovedIds();
        boolean approved = containsId(approvedIds, task.getApprovalTaskId())
            || containsId(approvedIds, approvalTicketId);
        ApprovalResult result = new ApprovalResult();
        if (!approved) {
            // 未登记即"审批尚未完成"。这里绝不能兜底成 APPROVED
            log.info("Mock approval channel: neither approvalTaskId {} nor approvalTicketId {} is registered "
                + "as approved, return PENDING", task.getApprovalTaskId(), approvalTicketId);
            result.setStatus(ApprovalResultStatusEnum.PENDING);
            return result;
        }
        // 这条放行没有经过任何人审批，审计时须能一眼看出
        log.warn("!!! Approval task {} is APPROVED by MOCK channel, NOT by a real approver !!!",
            task.getApprovalTaskId());
        result.setStatus(ApprovalResultStatusEnum.APPROVED);
        result.setApprovalTaskId(task.getApprovalTaskId());
        result.setApprover(task.getCreator());
        result.setApprovedAt(System.currentTimeMillis());
        return result;
    }

    private boolean containsId(List<String> approvedIds, String id) {
        return id != null && CollectionUtils.isNotEmpty(approvedIds) && approvedIds.contains(id);
    }
}
