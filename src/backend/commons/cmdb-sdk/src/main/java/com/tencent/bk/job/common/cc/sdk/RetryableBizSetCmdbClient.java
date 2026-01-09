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

import com.tencent.bk.job.common.cc.model.bizset.BizSetInfo;
import com.tencent.bk.job.common.cc.model.result.BizSetEventDetail;
import com.tencent.bk.job.common.cc.model.result.BizSetRelationEventDetail;
import com.tencent.bk.job.common.cc.model.result.ResourceWatchResult;
import com.tencent.bk.job.common.retry.RetryExecutor;
import com.tencent.bk.job.common.retry.RetryPolicy;
import com.tencent.bk.job.common.retry.circuitbreaker.SystemCircuitBreakerManager;
import com.tencent.bk.job.common.retry.metrics.RetryMetricsConstants;
import com.tencent.bk.job.common.retry.metrics.RetryMetricsRecorder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;

/**
 * 支持重试的 CMDB 业务集客户端
 * <p>
 * 使用代理模式，对原有的 IBizSetCmdbClient 进行封装，为幂等查询接口添加指数退避重试能力
 * </p>
 */
@Slf4j
public class RetryableBizSetCmdbClient implements IBizSetCmdbClient {

    private final IBizSetCmdbClient delegate;
    private final RetryExecutor retryExecutor;

    public RetryableBizSetCmdbClient(IBizSetCmdbClient delegate,
                                     RetryPolicy retryPolicy,
                                     RetryMetricsRecorder metricsRecorder,
                                     SystemCircuitBreakerManager circuitBreakerManager) {
        this.delegate = delegate;
        this.retryExecutor = new RetryExecutor(
            retryPolicy,
            metricsRecorder,
            RetryMetricsConstants.TAG_VALUE_SYSTEM_CMDB,
            circuitBreakerManager
        );
    }

    @Override
    public BizSetInfo queryBizSet(String tenantId, Long bizSetId) {
        return retryExecutor.executeWithRetry(
            () -> delegate.queryBizSet(tenantId, bizSetId),
            "queryBizSet"
        );
    }

    @Override
    public List<BizSetInfo> listAllBizSets(String tenantId) {
        return retryExecutor.executeWithRetry(
            () -> delegate.listAllBizSets(tenantId),
            "listAllBizSets"
        );
    }

    @Override
    public List<BizSetInfo> listBizSetByIds(String tenantId, List<Long> bizSetIds) {
        return retryExecutor.executeWithRetry(
            () -> delegate.listBizSetByIds(tenantId, bizSetIds),
            "listBizSetByIds"
        );
    }

    @Override
    public ResourceWatchResult<BizSetEventDetail> getBizSetEvents(String tenantId, Long startTime, String cursor) {
        return retryExecutor.executeWithRetry(
            () -> delegate.getBizSetEvents(tenantId, startTime, cursor),
            "getBizSetEvents"
        );
    }

    @Override
    public ResourceWatchResult<BizSetRelationEventDetail> getBizSetRelationEvents(String tenantId,
                                                                                  Long startTime,
                                                                                  String cursor) {
        return retryExecutor.executeWithRetry(
            () -> delegate.getBizSetRelationEvents(tenantId, startTime, cursor),
            "getBizSetRelationEvents"
        );
    }

    @Override
    public Set<String> listUsersByRole(String tenantId, Long bizId, String role) {
        return retryExecutor.executeWithRetry(
            () -> delegate.listUsersByRole(tenantId, bizId, role),
            "listUsersByRole"
        );
    }
}
