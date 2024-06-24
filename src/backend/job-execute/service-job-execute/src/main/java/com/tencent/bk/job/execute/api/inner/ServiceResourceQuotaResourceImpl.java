/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.execute.api.inner;

import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.resource.quota.ResourceQuotaLimit;
import com.tencent.bk.job.common.service.quota.ResourceQuotaStore;
import com.tencent.bk.job.execute.engine.quota.limit.RunningJobResourceQuotaManager;
import com.tencent.bk.job.execute.model.inner.RunningJobQuotaUsage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
public class ServiceResourceQuotaResourceImpl implements ServiceResourceQuotaResource {

    private final ResourceQuotaStore resourceQuotaStore;

    private final RunningJobResourceQuotaManager runningJobResourceQuotaManager;

    public ServiceResourceQuotaResourceImpl(ResourceQuotaStore resourceQuotaStore,
                                            RunningJobResourceQuotaManager runningJobResourceQuotaManager) {
        this.resourceQuotaStore = resourceQuotaStore;
        this.runningJobResourceQuotaManager = runningJobResourceQuotaManager;
    }

    @Override
    public InternalResponse<Map<String, ResourceQuotaLimit>> getResourceQuotaConfig() {
        return InternalResponse.buildSuccessResp(resourceQuotaStore.getAll());
    }

    @Override
    public InternalResponse<RunningJobQuotaUsage> getRunningJobQuotaUsage() {

        RunningJobQuotaUsage runningJobQuotaUsage = new RunningJobQuotaUsage();
        runningJobQuotaUsage.setTotal(runningJobResourceQuotaManager.getRunningJobTotal());
        runningJobQuotaUsage.setAppCount(runningJobResourceQuotaManager.getAppRunningJobCount());
        runningJobQuotaUsage.setResourceScopeCount(runningJobResourceQuotaManager.getResourceScopeRunningJobCount());
        return InternalResponse.buildSuccessResp(runningJobQuotaUsage);
    }
}
