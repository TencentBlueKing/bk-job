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
 * 结论只看一件事：<b>审批任务 ID 或审批单据 ID 是否命中配置里登记的"视为审批通过"列表</b>，命中即 APPROVED，
 * 其余一律 PENDING。之所以两个 ID 都认，是因为单据 ID 由调用方在放行时自由指定，配一个约定值就能反复自测，
 * 而任务 ID 要发起之后才知道、适合精确放行某一单。
 * <p>
 * <b>放宽只到这里为止，两条底线不能动</b>：
 * <ol>
 *     <li><b>默认一律返回 PENDING，绝不返回 APPROVED</b>：配置里不存在任何能把默认结论改成 APPROVED 的开关；</li>
 *     <li>开关默认 false，生产 values 模板中不出现 mock 节点；一旦在生产 profile 下被开启，
 *     <b>直接启动失败</b>，把误配置从"线上静默降级"变成"部署期暴露"；开启时打显著 WARN 日志便于巡检发现。</li>
 * </ol>
 * <p>
 * <b>注意：Mock 期间"绑定证明"与"approver == creator"这两项校验会自动满足</b> —— 本类回带的
 * approvalTaskId 就取自任务本身、approver 就取自任务的 creator。这是为自测便利付出的代价：
 * <b>Mock 下跑通不等于放行校验链有效</b>，那两项校验只有对接真实渠道后才真正被检验。
 * 反过来，本类的放宽<b>只发生在渠道实现内部</b>，放行校验链一行都没改，切换到真实实现时也不需要改。
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
        result.setStatus(ApprovalResultStatusEnum.APPROVED);
        result.setApprovalTaskId(task.getApprovalTaskId());
        result.setApprover(task.getCreator());
        result.setApprovedAt(System.currentTimeMillis());
        return result;
    }

    private boolean containsId(List<String> approvedIds, String id) {
        return id != null && CollectionUtils.isNotEmpty(approvedIds) && approvedIds.contains(id);
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
