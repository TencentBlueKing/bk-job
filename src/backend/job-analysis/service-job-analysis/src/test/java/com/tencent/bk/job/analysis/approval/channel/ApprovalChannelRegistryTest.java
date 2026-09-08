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
import com.tencent.bk.job.analysis.config.ApprovalProperties;
import com.tencent.bk.job.analysis.model.dto.ApprovalTaskDTO;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 单元测试 - 审批渠道注册表。
 * <p>
 * 重点是"依赖未就绪时的姿态"：一个渠道实现都没有时，应用照常启动，但审批功能整体不可用 ——
 * 安全机制在依赖缺失时必须不可用，而不是放开。
 */
class ApprovalChannelRegistryTest {

    @Test
    @DisplayName("没有任何渠道实现时应用照常启动，取渠道时才报不支持")
    void givenNoChannelThenFailOnUse() {
        ApprovalChannelRegistry registry = buildRegistry();

        assertThatThrownBy(() -> registry.getChannel(ApprovalChannelEnum.IMATE))
            .isInstanceOf(FailedPreconditionException.class)
            .satisfies(e -> assertThat(((ServiceException) e).getErrorCode())
                .isEqualTo(ErrorCode.APPROVAL_CHANNEL_NOT_SUPPORTED));
    }

    @Test
    @DisplayName("按任务指派的渠道名解析；非法渠道名一律拒绝，不做跨渠道轮询试探")
    void givenChannelNameThenResolveStrictly() {
        ApprovalChannel imateChannel = buildChannel(ApprovalChannelEnum.IMATE);
        ApprovalChannelRegistry registry = buildRegistry(imateChannel);

        assertThat(registry.getChannelByName(ApprovalChannelEnum.IMATE.name())).isSameAs(imateChannel);
        assertThatThrownBy(() -> registry.getChannelByName("NOT_A_CHANNEL"))
            .isInstanceOf(FailedPreconditionException.class);
        assertThatThrownBy(() -> registry.getChannelByName(null))
            .isInstanceOf(FailedPreconditionException.class);
    }

    @Test
    @DisplayName("同一渠道存在两个实现时启动失败：回查目标不确定必须在部署期暴露")
    void givenDuplicatedChannelThenFailFast() {
        ApprovalChannel first = buildChannel(ApprovalChannelEnum.IMATE);
        ApprovalChannel second = buildChannel(ApprovalChannelEnum.IMATE);

        assertThatThrownBy(() -> buildRegistry(first, second))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Duplicated ApprovalChannel");
    }

    @Test
    @DisplayName("未指定渠道时给出服务端配置的默认渠道")
    void givenNoSpecifiedChannelThenReturnDefault() {
        assertThat(buildRegistry().getDefaultChannel()).isEqualTo(ApprovalChannelEnum.IMATE);
    }

    @SuppressWarnings("unchecked")
    private ApprovalChannelRegistry buildRegistry(ApprovalChannel... channels) {
        ObjectProvider<ApprovalChannel> provider = mock(ObjectProvider.class);
        when(provider.orderedStream()).thenReturn(Stream.of(channels));
        return new ApprovalChannelRegistry(provider, new ApprovalProperties());
    }

    private ApprovalChannel buildChannel(ApprovalChannelEnum channelEnum) {
        return new ApprovalChannel() {
            @Override
            public ApprovalChannelEnum getChannelType() {
                return channelEnum;
            }

            @Override
            public ApprovalResult queryResult(ApprovalTaskDTO task, String approvalTicketId) {
                return null;
            }
        };
    }
}
