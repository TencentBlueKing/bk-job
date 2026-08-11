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

import com.tencent.bk.job.analysis.config.ApprovalProperties;
import com.tencent.bk.job.analysis.model.dto.ApprovalTaskDTO;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 审批链路的可观测代理指标。
 * <p>
 * 这两个计数器<b>只观测、不拦截</b>，是渠道接入契约中"平台无法机器校验"那几条的唯一事后发现手段：
 * <ul>
 *     <li>{@link #NAME_DISPATCHED_WITHOUT_TICKET_FETCH}：渠道从未取过单却拿到了 APPROVED，
 *     说明审批人看到的单据不是作业平台生成的（"单据由作业平台生成"这条支柱失效）；</li>
 *     <li>{@link #NAME_FAST_APPROVED}：近乎瞬时通过是自动审批的典型特征，意味着"人在环上"已崩塌。</li>
 * </ul>
 * <b>期望值是持续为 0</b>，一旦非零就要人工核查对应渠道的接入配置。
 * <p>
 * 埋点位置固定在放行成功路径（markDispatched 之后），不能挪到校验链前段 —— 挪了就会把未放行的
 * 请求也统计进来，指标从"接入违约信号"退化成噪声。同时<b>任何打点异常都不得影响放行</b>。
 */
@Slf4j
@Component
public class ApprovalMetrics {

    public static final String NAME_DISPATCHED_WITHOUT_TICKET_FETCH =
        "job.analysis.approval.dispatched.without.ticket.fetch";

    public static final String NAME_FAST_APPROVED = "job.analysis.approval.fast.approved";

    private static final String TAG_OPERATION_TYPE = "operation_type";
    private static final String TAG_APPROVAL_CHANNEL = "approval_channel";
    private static final String TAG_APP_CODE = "app_code";

    /**
     * 标签值缺失时的占位，避免出现空标签值
     */
    private static final String TAG_VALUE_NONE = "none";

    private final MeterRegistry meterRegistry;
    private final ApprovalProperties approvalProperties;

    public ApprovalMetrics(MeterRegistry meterRegistry, ApprovalProperties approvalProperties) {
        this.meterRegistry = meterRegistry;
        this.approvalProperties = approvalProperties;
    }

    /**
     * 在放行成功（已 markDispatched）后记录两个代理指标
     */
    public void recordDispatched(ApprovalTaskDTO task) {
        try {
            Tags tags = buildTags(task);
            if (task.getTicketFetchedAt() == null) {
                log.warn("Approval task {} dispatched without ticket fetched, channel: {}, appCode: {}",
                    task.getApprovalTaskId(), task.getApprovalChannel(), task.getAppCode());
                increment(NAME_DISPATCHED_WITHOUT_TICKET_FETCH, tags,
                    "Approval tasks dispatched without the ticket ever fetched by the approval channel");
            }
            if (isFastApproved(task)) {
                log.warn("Approval task {} approved within {}ms, suspected auto approval, channel: {}",
                    task.getApprovalTaskId(), task.getApprovedAt() - task.getCreateTime(),
                    task.getApprovalChannel());
                increment(NAME_FAST_APPROVED, tags,
                    "Approval tasks approved within the fast-approve threshold, suspected auto approval");
            }
        } catch (Exception e) {
            // 打点失败绝不影响放行结果：观测手段不该成为新的故障点
            log.warn("Record approval metrics failed, approvalTaskId: {}", task.getApprovalTaskId(), e);
        }
    }

    private boolean isFastApproved(ApprovalTaskDTO task) {
        if (task.getApprovedAt() == null || task.getCreateTime() == null) {
            return false;
        }
        Long threshold = approvalProperties.getFastApproveThresholdMillis();
        if (threshold == null || threshold <= 0) {
            return false;
        }
        return task.getApprovedAt() - task.getCreateTime() < threshold;
    }

    private void increment(String name, Tags tags, String description) {
        Counter.builder(name)
            .description(description)
            .tags(tags)
            .register(meterRegistry)
            .increment();
    }

    private Tags buildTags(ApprovalTaskDTO task) {
        return Tags.of(
            TAG_OPERATION_TYPE, tagValue(task.getOperationType()),
            TAG_APPROVAL_CHANNEL, tagValue(task.getApprovalChannel()),
            TAG_APP_CODE, tagValue(task.getAppCode())
        );
    }

    private String tagValue(String value) {
        return StringUtils.isBlank(value) ? TAG_VALUE_NONE : value;
    }
}
