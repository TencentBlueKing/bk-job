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

package com.tencent.bk.job.analysis.approval.executor.impl;

import com.tencent.bk.job.analysis.approval.consts.ApprovalOperationTypeEnum;
import com.tencent.bk.job.analysis.approval.executor.AbstractOperationExecutor;
import com.tencent.bk.job.analysis.model.dto.ApprovalTaskDTO;
import com.tencent.bk.job.common.api.model.DryRunResult;
import com.tencent.bk.job.execute.api.inner.ServiceApprovalExecuteResource;
import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteJobPlanRequest;
import com.tencent.bk.job.execute.model.inner.request.ServiceApprovalExecuteJobPlanRequest;
import org.springframework.stereotype.Component;

/**
 * 启动执行方案的出站分发。
 * <p>
 * 走的是新增的 ServiceApprovalExecuteResource.executeJobPlan，该入口把 cronTaskId 固化为 null，
 * 而下游 buildExecuteParam 把 skipAuth 收窄为 {@code cronTaskId != null && skipAuth}，
 * 因此<b>审批路径在结构上就拿不到 skipAuth</b>。这比在共享入口里加断言牢固 ——
 * 不要为了"复用"改回 ServiceExecuteTaskResource.executeTask，那会把这层结构性保证拆掉。
 */
@Component
public class ExecuteJobPlanOperationExecutor extends AbstractOperationExecutor<V4ExecuteJobPlanRequest> {

    private final ServiceApprovalExecuteResource approvalExecuteResource;

    public ExecuteJobPlanOperationExecutor(ServiceApprovalExecuteResource approvalExecuteResource) {
        this.approvalExecuteResource = approvalExecuteResource;
    }

    @Override
    public ApprovalOperationTypeEnum getOperationType() {
        return ApprovalOperationTypeEnum.EXECUTE_JOB_PLAN;
    }

    @Override
    public Class<V4ExecuteJobPlanRequest> getParamsClass() {
        return V4ExecuteJobPlanRequest.class;
    }

    @Override
    public DryRunResult<?> invoke(V4ExecuteJobPlanRequest params, ApprovalTaskDTO task, boolean dryRun) {
        ServiceApprovalExecuteJobPlanRequest request = new ServiceApprovalExecuteJobPlanRequest();
        request.setRequest(params);
        request.setOperator(task.getCreator());
        request.setAppCode(task.getAppCode());
        request.setDryRun(dryRun);
        return unwrap(approvalExecuteResource.executeJobPlan(request));
    }
}
