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

package com.tencent.bk.job.common.service.quota;

import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.resource.quota.QuotaResourceId;

/**
 * 资源配额存储-正在执行中的作业数量配额限制
 */
public class RunningJobResourceQuotaStore {

    private final ResourceQuotaStore resourceQuotaStore;

    public RunningJobResourceQuotaStore(ResourceQuotaStore resourceQuotaStore) {
        this.resourceQuotaStore = resourceQuotaStore;
    }

    public boolean isQuotaLimitEnabled() {
        return resourceQuotaStore.isQuotaLimitEnabled(QuotaResourceId.JOB_INSTANCE);
    }

    /**
     * 根据资源管理空间获取正在执行作业配额限制
     *
     * @param resourceScope 资源管理空间
     * @return 最大正在执行作业限制
     */
    public long getQuotaLimitByResourceScope(ResourceScope resourceScope) {
        return resourceQuotaStore.getQuotaLimitByResourceScope(QuotaResourceId.JOB_INSTANCE, resourceScope);
    }

    /**
     * 根据应用 Code 获取正在执行作业配额限制
     *
     * @param appCode 蓝鲸应用 App Code
     * @return 最大正在执行作业限制
     */
    public long getQuotaLimitByAppCode(String appCode) {
        return resourceQuotaStore.getQuotaLimitByAppCode(QuotaResourceId.JOB_INSTANCE, appCode);
    }

    /**
     * 获取整个Job 系统 正在执行作业配额限制
     *
     * @return 最大正在执行作业限制
     */
    public long getSystemQuotaLimit() {
        return resourceQuotaStore.getSystemQuotaLimit(QuotaResourceId.JOB_INSTANCE);
    }
}
