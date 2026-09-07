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

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 单元测试 - IMate 渠道 Mock 实现。
 * <p>
 * 锁定 Mock 的两条约束：未登记的 ID 一律 PENDING（不存在任何把默认结论改成 APPROVED 的开关）、
 * 任务 ID 与单据 ID 任一命中即通过。
 */
class MockImateApprovalChannelTest {

    private static final String TASK_ID = "3f8a9b1c2d3e4f5061728394a5b6c7d8";
    private static final String TICKET_ID = "IMATE-0001";

    @Test
    @DisplayName("未登记任何 ID 时一律返回 PENDING，绝不自动放行")
    void givenNoApprovedIdThenReturnPending() {
        MockImateApprovalChannel channel = buildChannel(new ApprovalProperties());

        ApprovalResult result = channel.queryResult(buildTask(), TICKET_ID);

        assertThat(result.getStatus()).isEqualTo(ApprovalResultStatusEnum.PENDING);
        assertThat(result.getApprovalTaskId()).isNull();
        assertThat(result.getApprover()).isNull();
        assertThat(channel.getChannelType()).isEqualTo(ApprovalChannelEnum.IMATE);
    }

    @Test
    @DisplayName("登记了别的 ID 时仍然返回 PENDING：只有命中才算通过")
    void givenOtherApprovedIdThenReturnPending() {
        MockImateApprovalChannel channel = buildChannel(buildProperties("some-other-id"));

        ApprovalResult result = channel.queryResult(buildTask(), TICKET_ID);

        assertThat(result.getStatus()).isEqualTo(ApprovalResultStatusEnum.PENDING);
    }

    @Test
    @DisplayName("命中单据 ID 即视为通过：单据 ID 由调用方自由指定，配一个约定值就能反复自测")
    void givenApprovedTicketIdThenApproved() {
        MockImateApprovalChannel channel = buildChannel(buildProperties(TICKET_ID));

        ApprovalResult result = channel.queryResult(buildTask(), TICKET_ID);

        assertThat(result.getStatus()).isEqualTo(ApprovalResultStatusEnum.APPROVED);
        // Mock 期绑定证明与 approver == creator 自动满足，这两项校验只有对接真实渠道后才真正被检验
        assertThat(result.getApprovalTaskId()).isEqualTo(TASK_ID);
        assertThat(result.getApprover()).isEqualTo("admin");
        assertThat(result.getApprovedAt()).isNotNull();
    }

    @Test
    @DisplayName("命中任务 ID 也视为通过：适合精确放行某一单")
    void givenApprovedTaskIdThenApproved() {
        MockImateApprovalChannel channel = buildChannel(buildProperties(TASK_ID));

        ApprovalResult result = channel.queryResult(buildTask(), TICKET_ID);

        assertThat(result.getStatus()).isEqualTo(ApprovalResultStatusEnum.APPROVED);
        assertThat(result.getApprovalTaskId()).isEqualTo(TASK_ID);
    }

    @Test
    @DisplayName("通过 ID 列表每次回查都重新读取，改配置后无需重启即可生效")
    void givenApprovedIdsChangedThenTakeEffectWithoutRestart() {
        ApprovalProperties properties = new ApprovalProperties();
        MockImateApprovalChannel channel = buildChannel(properties);
        assertThat(channel.queryResult(buildTask(), TICKET_ID).getStatus())
            .isEqualTo(ApprovalResultStatusEnum.PENDING);

        properties.getChannels().getImate().getMock().setApprovedIds(Collections.singletonList(TICKET_ID));

        assertThat(channel.queryResult(buildTask(), TICKET_ID).getStatus())
            .isEqualTo(ApprovalResultStatusEnum.APPROVED);
    }

    private MockImateApprovalChannel buildChannel(ApprovalProperties properties) {
        return new MockImateApprovalChannel(properties);
    }

    private ApprovalProperties buildProperties(String approvedId) {
        ApprovalProperties properties = new ApprovalProperties();
        properties.getChannels().getImate().getMock().setEnabled(true);
        properties.getChannels().getImate().getMock().setApprovedIds(Collections.singletonList(approvedId));
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
