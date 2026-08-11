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
import com.tencent.bk.job.analysis.model.dto.ApprovalTaskDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * IMate 渠道的 Mock 实现，用于 IMate 回查接口就绪之前的自测。
 * <p>
 * <b>Mock 的四条约束一条都不能松，它们是这套机制在依赖未就绪期间的全部安全保障</b>：
 * <ol>
 *     <li><b>默认一律返回 PENDING，绝不返回 APPROVED</b>。即 Mock 期间"放行"这条路径默认走不通，
 *     功能上表现为"审批中"。配置里不存在任何能把默认结论改成 APPROVED 的开关。</li>
 *     <li>放行只能靠<b>显式登记、且自带完整绑定证明</b>的桩数据：桩数据必须同时提供 approvalTaskId 与 approver，
 *     <b>Mock 只如实回传、不做任何自动填充</b>。于是放行校验链里的绑定证明与"approver == creator"
 *     在 Mock 模式下照常执行、照常可能失败 —— Mock 替换的只是"结论从哪来"，
 *     没有削弱"结论怎么校验"。桩数据里的 approvalTaskId 填错，放行依然被拒。</li>
 *     <li>开关默认 false，生产 values 模板中不出现 mock 节点；一旦在生产 profile 下被开启，
 *     <b>直接启动失败</b>，把误配置从"线上静默降级"变成"部署期暴露"；开启时打显著 WARN 日志便于巡检发现。</li>
 *     <li>切换到真实实现时，<b>放行校验链一行都不用改</b>：校验链在 Mock 期就已在真实执行，
 *     回归范围仅限渠道实现本身。</li>
 * </ol>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "job.analysis.approval.channels.imate.mock.enabled", havingValue = "true")
public class MockImateApprovalChannel implements ApprovalChannel {

    /**
     * 生产环境的 Spring profile 名。在这些 profile 下开启 Mock 一律视为误配置
     */
    private static final List<String> PRODUCTION_PROFILES = Arrays.asList("prod", "production");

    private final ApprovalProperties approvalProperties;

    public MockImateApprovalChannel(ApprovalProperties approvalProperties, Environment environment) {
        this.approvalProperties = approvalProperties;
        checkNotProductionProfile(environment);
        log.warn("!!! Approval channel IMATE is running in MOCK mode, DO NOT use it in production !!! "
            + "Approval results are NOT queried from the real approval channel. "
            + "Registered mock approved tickets: {}",
            CollectionUtils.size(approvalProperties.getChannels().getImate().getMock().getApprovedTickets()));
    }

    @Override
    public ApprovalChannelEnum getChannelType() {
        return ApprovalChannelEnum.IMATE;
    }

    @Override
    public ApprovalResult queryResult(ApprovalTaskDTO task, String approvalTicketId) {
        List<ApprovalProperties.MockApprovedTicket> approvedTickets =
            approvalProperties.getChannels().getImate().getMock().getApprovedTickets();
        ApprovalProperties.MockApprovedTicket stub = null;
        if (CollectionUtils.isNotEmpty(approvedTickets)) {
            stub = approvedTickets.stream()
                .filter(ticket -> approvalTicketId != null && approvalTicketId.equals(ticket.getTicketId()))
                .findFirst()
                .orElse(null);
        }
        if (stub == null) {
            // 未登记即"审批尚未完成"。这里绝不能兜底成 APPROVED
            log.info("Mock approval channel: ticket {} is not registered as approved, return PENDING",
                approvalTicketId);
            return buildPendingResult();
        }
        ApprovalResult result = new ApprovalResult();
        result.setStatus(ApprovalResultStatusEnum.APPROVED);
        // 原样回传桩数据，不做任何自动填充：填错的绑定证明或审批人必须让校验链拒掉
        result.setApprovalTaskId(stub.getApprovalTaskId());
        result.setApprover(stub.getApprover());
        result.setApprovedAt(stub.getApprovedAt() == null ? System.currentTimeMillis() : stub.getApprovedAt());
        result.setComment(stub.getComment());
        return result;
    }

    private ApprovalResult buildPendingResult() {
        ApprovalResult result = new ApprovalResult();
        result.setStatus(ApprovalResultStatusEnum.PENDING);
        return result;
    }

    private void checkNotProductionProfile(Environment environment) {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String activeProfile : activeProfiles) {
            if (PRODUCTION_PROFILES.contains(activeProfile.trim().toLowerCase())) {
                throw new IllegalStateException("Mock approval channel is enabled under production profile "
                    + activeProfile + ", which would make the approval mechanism useless. "
                    + "Remove job.analysis.approval.channels.imate.mock.enabled from the production config.");
            }
        }
    }
}
