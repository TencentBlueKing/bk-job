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

package com.tencent.bk.job.analysis.api.esb.v4.impl;

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.audit.annotations.AuditRequestBody;
import com.tencent.bk.job.analysis.api.esb.v4.OpenApiApprovalTaskV4Resource;
import com.tencent.bk.job.analysis.approval.ApprovalTaskService;
import com.tencent.bk.job.analysis.model.dto.ApprovalTaskDTO;
import com.tencent.bk.job.analysis.model.esb.v4.req.V4CancelApprovalTaskRequest;
import com.tencent.bk.job.analysis.model.esb.v4.req.V4RefreshApprovalTaskRequest;
import com.tencent.bk.job.analysis.model.esb.v4.resp.V4ApprovalTaskDTO;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审批任务流转接口实现。
 * <p>
 * 三个方法上的 {@code @AuditEntry} <b>都不写 actionId</b>：一个流转接口要服务 6 种操作类型，
 * actionId 沿用各操作原有的权限点、由 {@code ApprovalAuditor} 依任务的 operation_type 在运行时决定
 * （并显式设置 scope，因为这三个请求体不带资源范围）。写死 actionId 会让其余 5 种操作的审计事件
 * 被 SDK 直接丢弃。
 * <p>
 * {@code refresh} 只把两个 ID 交给 Service：审批结论一律由作业平台自己回查渠道得出。
 */
@RestController
public class OpenApiApprovalTaskV4ResourceImpl implements OpenApiApprovalTaskV4Resource {

    private final ApprovalTaskService approvalTaskService;
    private final ApprovalV4ApiSupport approvalV4ApiSupport;

    public OpenApiApprovalTaskV4ResourceImpl(ApprovalTaskService approvalTaskService,
                                             ApprovalV4ApiSupport approvalV4ApiSupport) {
        this.approvalTaskService = approvalTaskService;
        this.approvalV4ApiSupport = approvalV4ApiSupport;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v4_get_approval_task"})
    public EsbV4Response<V4ApprovalTaskDTO> getApprovalTask(String username,
                                                           String appCode,
                                                           String approvalTaskId) {
        ApprovalTaskDTO task = approvalTaskService.get(
            approvalTaskId, approvalV4ApiSupport.workflowCaller(appCode));
        return EsbV4Response.success(approvalV4ApiSupport.toTaskDTO(task));
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v4_refresh_approval_task"})
    @AuditEntry
    public EsbV4Response<V4ApprovalTaskDTO> refreshApprovalTask(
        String username,
        String appCode,
        @AuditRequestBody V4RefreshApprovalTaskRequest request
    ) {
        ApprovalTaskDTO task = approvalTaskService.refresh(
            request.getApprovalTaskId(),
            request.getApprovalTicketId(),
            approvalV4ApiSupport.workflowCaller(appCode)
        );
        return EsbV4Response.success(approvalV4ApiSupport.toTaskDTO(task));
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v4_cancel_approval_task"})
    @AuditEntry
    public EsbV4Response<V4ApprovalTaskDTO> cancelApprovalTask(
        String username,
        String appCode,
        @AuditRequestBody V4CancelApprovalTaskRequest request
    ) {
        ApprovalTaskDTO task = approvalTaskService.cancel(
            request.getApprovalTaskId(), approvalV4ApiSupport.workflowCaller(appCode));
        return EsbV4Response.success(approvalV4ApiSupport.toTaskDTO(task));
    }
}
