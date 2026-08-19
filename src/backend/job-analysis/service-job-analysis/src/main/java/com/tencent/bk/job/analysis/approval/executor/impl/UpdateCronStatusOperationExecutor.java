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
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.crontab.api.esb.v4.OpenApiUpdateCronStatusV4Resource;
import com.tencent.bk.job.crontab.model.esb.v4.req.V4UpdateCronStatusRequest;
import com.tencent.bk.job.crontab.model.esb.v4.resp.V4CronJobDTO;
import org.springframework.stereotype.Component;

/**
 * 定时任务启停的出站分发。预检与放行都直接复用对外的 OpenAPI，不再另立一套内部执行接口
 */
@Component
public class UpdateCronStatusOperationExecutor extends AbstractOperationExecutor<V4UpdateCronStatusRequest> {

    private final OpenApiUpdateCronStatusV4Resource updateCronStatusResource;

    public UpdateCronStatusOperationExecutor(OpenApiUpdateCronStatusV4Resource updateCronStatusResource) {
        this.updateCronStatusResource = updateCronStatusResource;
    }

    @Override
    public ApprovalOperationTypeEnum getOperationType() {
        return ApprovalOperationTypeEnum.UPDATE_CRON_STATUS;
    }

    @Override
    public Class<V4UpdateCronStatusRequest> getParamsClass() {
        return V4UpdateCronStatusRequest.class;
    }

    /**
     * 操作人只能取任务的 creator，且该值只能来自 DB。
     * <p>
     * 轻量化部署下 Feign 调用会退化成本地方法调用，请求头不再经过拦截器，下游取到的是<b>本次请求线程上
     * 已有的操作人</b>；该值等于 creator 由 refresh 的归属校验保证（调用方必须就是任务发起人本人），
     * 放宽那处校验会同时破坏这里的身份正确性。
     */
    @Override
    public DryRunResult<?> invoke(V4UpdateCronStatusRequest params, ApprovalTaskDTO task, boolean dryRun) {
        EsbV4Response<V4CronJobDTO> response = updateCronStatusResource.updateCronStatus(
            task.getCreator(), task.getAppCode(), dryRun, params);
        return unwrap(response, dryRun);
    }
}
