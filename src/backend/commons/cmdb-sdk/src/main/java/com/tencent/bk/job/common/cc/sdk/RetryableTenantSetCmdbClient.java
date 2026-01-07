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

package com.tencent.bk.job.common.cc.sdk;

import com.tencent.bk.job.common.cc.model.tenantset.TenantSetInfo;
import com.tencent.bk.job.common.retry.ExponentialBackoffRetryPolicy;
import com.tencent.bk.job.common.retry.RetryExecutor;
import com.tencent.bk.job.common.retry.metrics.RetryMetricsConstants;
import com.tencent.bk.job.common.retry.metrics.RetryMetricsRecorder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 支持重试的 CMDB 租户集客户端
 * <p>
 * 使用代理模式，对原有的 ITenantSetCmdbClient 进行封装，为幂等查询接口添加指数退避重试能力
 * </p>
 */
@Slf4j
public class RetryableTenantSetCmdbClient implements ITenantSetCmdbClient {

    private final ITenantSetCmdbClient delegate;
    private final RetryExecutor retryExecutor;

    public RetryableTenantSetCmdbClient(ITenantSetCmdbClient delegate,
                                        ExponentialBackoffRetryPolicy retryPolicy,
                                        RetryMetricsRecorder metricsRecorder) {
        this.delegate = delegate;
        this.retryExecutor = new RetryExecutor(
            retryPolicy,
            metricsRecorder,
            RetryMetricsConstants.TAG_VALUE_SYSTEM_CMDB
        );
    }

    @Override
    public List<TenantSetInfo> listAllTenantSet() {
        return retryExecutor.executeWithRetry(
            delegate::listAllTenantSet,
            "listAllTenantSet"
        );
    }
}
