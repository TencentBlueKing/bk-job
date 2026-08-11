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

import com.tencent.bk.job.analysis.approval.channel.model.ApprovalResult;
import com.tencent.bk.job.analysis.approval.consts.ApprovalChannelEnum;
import com.tencent.bk.job.analysis.approval.consts.ApprovalResultStatusEnum;
import com.tencent.bk.job.analysis.config.ApprovalProperties;
import com.tencent.bk.job.analysis.model.dto.ApprovalTaskDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 单元测试 - IMate 渠道 Mock 实现。
 * <p>
 * 锁定 Mock 的四条约束：默认 PENDING、桩数据原样回传不自动填充、生产 profile 下启动失败。
 */
class MockImateApprovalChannelTest {

    private static final String TASK_ID = "approval-task-1";
    private static final String TICKET_ID = "IMATE-0001";

    @Test
    @DisplayName("未登记的单据一律返回 PENDING，绝不自动放行")
    void givenUnregisteredTicketThenReturnPending() {
        MockImateApprovalChannel channel = buildChannel(new ApprovalProperties());

        ApprovalResult result = channel.queryResult(buildTask(), TICKET_ID);

        assertThat(result.getStatus()).isEqualTo(ApprovalResultStatusEnum.PENDING);
        assertThat(result.getApprovalTaskId()).isNull();
        assertThat(result.getApprover()).isNull();
        assertThat(channel.getChannelType()).isEqualTo(ApprovalChannelEnum.IMATE);
    }

    @Test
    @DisplayName("桩数据原样回传，不自动填充 approvalTaskId 与 approver")
    void givenRegisteredTicketThenReturnStubAsIs() {
        ApprovalProperties properties = buildPropertiesWithStub("wrong-task-id", "someone_else");
        MockImateApprovalChannel channel = buildChannel(properties);

        ApprovalResult result = channel.queryResult(buildTask(), TICKET_ID);

        assertThat(result.getStatus()).isEqualTo(ApprovalResultStatusEnum.APPROVED);
        // 填错的绑定证明与审批人被原样带出，交由放行校验链拒绝，Mock 不做任何"修正"
        assertThat(result.getApprovalTaskId()).isEqualTo("wrong-task-id");
        assertThat(result.getApprover()).isEqualTo("someone_else");
    }

    @Test
    @DisplayName("生产 profile 下开启 Mock 直接启动失败")
    void givenProductionProfileThenFailFast() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        ApprovalProperties properties = new ApprovalProperties();

        assertThatThrownBy(() -> new MockImateApprovalChannel(properties, environment))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("production profile");
    }

    private MockImateApprovalChannel buildChannel(ApprovalProperties properties) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        return new MockImateApprovalChannel(properties, environment);
    }

    private ApprovalProperties buildPropertiesWithStub(String approvalTaskId, String approver) {
        ApprovalProperties.MockApprovedTicket ticket = new ApprovalProperties.MockApprovedTicket();
        ticket.setTicketId(TICKET_ID);
        ticket.setApprovalTaskId(approvalTaskId);
        ticket.setApprover(approver);
        ApprovalProperties properties = new ApprovalProperties();
        properties.getChannels().getImate().getMock().setEnabled(true);
        properties.getChannels().getImate().getMock().setApprovedTickets(Collections.singletonList(ticket));
        return properties;
    }

    private ApprovalTaskDTO buildTask() {
        ApprovalTaskDTO task = new ApprovalTaskDTO();
        task.setApprovalTaskId(TASK_ID);
        task.setCreator("admin");
        task.setApprovalChannel(ApprovalChannelEnum.IMATE.name());
        return task;
    }
}
