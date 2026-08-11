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

package com.tencent.bk.job.manage.api.inner;

import com.tencent.bk.job.common.annotation.InternalAPI;
import com.tencent.bk.job.common.api.model.DryRunResult;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.manage.model.esb.v4.OpenApiV4JobPlanDTO;
import com.tencent.bk.job.manage.model.inner.request.ServiceApprovalCreateJobPlanRequest;
import com.tentent.bk.job.common.api.feign.annotation.SmartFeignClient;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 带审批的执行方案操作API-服务内部调用。
 * <p>
 * 供 job-analysis 的审批链路调用：创建审批任务时以 dryRun=true 预检，审批通过放行时以 dryRun=false 创建，
 * 两次走的是同一段校验与转换代码。
 * <p>
 * <b>硬性契约</b>：校验失败一律以 HTTP 200 + {@link DryRunResult}（valid=false，带 errorCode）返回，
 * 不得抛异常 —— 否则会被 FeignErrorDecoder 吞成 InternalException，用户看不到具体原因。
 */
@Tag(name = "job-manage:service:Approval_Job_Plan")
@SmartFeignClient(value = "job-manage", contextId = "approvalJobPlanResource")
@InternalAPI
public interface ServiceApprovalJobPlanResource {

    /**
     * 预检或创建执行方案
     */
    @PostMapping("/service/job-plan/approval/create")
    InternalResponse<DryRunResult<OpenApiV4JobPlanDTO>> createJobPlan(
        @RequestBody ServiceApprovalCreateJobPlanRequest request);
}
