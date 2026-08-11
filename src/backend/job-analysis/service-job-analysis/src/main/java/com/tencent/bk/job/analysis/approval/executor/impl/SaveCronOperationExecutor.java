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
import com.tencent.bk.job.crontab.api.inner.ServiceApprovalCronResource;
import com.tencent.bk.job.crontab.model.esb.v4.req.V4SaveCronRequest;
import com.tencent.bk.job.crontab.model.inner.request.ServiceApprovalSaveCronRequest;
import org.springframework.stereotype.Component;

/**
 * 新建/更新定时任务的出站分发
 */
@Component
public class SaveCronOperationExecutor extends AbstractOperationExecutor<V4SaveCronRequest> {

    private final ServiceApprovalCronResource approvalCronResource;

    public SaveCronOperationExecutor(ServiceApprovalCronResource approvalCronResource) {
        this.approvalCronResource = approvalCronResource;
    }

    @Override
    public ApprovalOperationTypeEnum getOperationType() {
        return ApprovalOperationTypeEnum.SAVE_CRON;
    }

    @Override
    public Class<V4SaveCronRequest> getParamsClass() {
        return V4SaveCronRequest.class;
    }

    @Override
    public DryRunResult<?> invoke(V4SaveCronRequest params, ApprovalTaskDTO task, boolean dryRun) {
        ServiceApprovalSaveCronRequest request = new ServiceApprovalSaveCronRequest();
        request.setRequest(params);
        request.setOperator(task.getCreator());
        request.setAppCode(task.getAppCode());
        request.setDryRun(dryRun);
        return unwrap(approvalCronResource.saveCron(request));
    }
}
