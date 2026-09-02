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

import com.tencent.bk.job.analysis.api.esb.v4.OpenApiApprovalContentV4Resource;
import com.tencent.bk.job.analysis.approval.ApprovalTaskService;
import com.tencent.bk.job.analysis.approval.channel.model.ApprovalContent;
import com.tencent.bk.job.analysis.model.esb.v4.resp.V4ApprovalContentDTO;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import org.springframework.web.bind.annotation.RestController;

/**
 * 取审批内容。
 * <p>
 * 归属校验全部在 Service 侧完成，本类只负责组装调用上下文与返回体。
 * 取内容不产出审计事件：审批链路只在审批通过并放行时产出一条审计事件。
 */
@RestController
public class OpenApiApprovalContentV4ResourceImpl implements OpenApiApprovalContentV4Resource {

    private final ApprovalTaskService approvalTaskService;
    private final ApprovalV4ApiSupport approvalV4ApiSupport;

    public OpenApiApprovalContentV4ResourceImpl(ApprovalTaskService approvalTaskService,
                                                ApprovalV4ApiSupport approvalV4ApiSupport) {
        this.approvalTaskService = approvalTaskService;
        this.approvalV4ApiSupport = approvalV4ApiSupport;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v4_system_get_approval_content"})
    public EsbV4Response<V4ApprovalContentDTO> getApprovalContent(String username,
                                                                 String appCode,
                                                                 String approvalTaskId) {
        ApprovalContent content = approvalTaskService.getApprovalContent(
            approvalTaskId, approvalV4ApiSupport.workflowCaller(appCode));
        return EsbV4Response.success(approvalV4ApiSupport.toContentDTO(content));
    }
}
