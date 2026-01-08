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

package com.tencent.bk.job.common.gse.v2;

import com.tencent.bk.job.common.config.ExternalSystemRetryProperties;
import com.tencent.bk.job.common.gse.IGseClient;
import com.tencent.bk.job.common.gse.v2.model.FileTaskResult;
import com.tencent.bk.job.common.gse.v2.model.GetExecuteScriptResultRequest;
import com.tencent.bk.job.common.gse.v2.model.GetTransferFileResultRequest;
import com.tencent.bk.job.common.gse.v2.model.ScriptTaskResult;
import com.tencent.bk.job.common.gse.v2.model.req.ListAgentStateReq;
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;
import com.tencent.bk.job.common.retry.ExponentialBackoffRetryPolicy;
import com.tencent.bk.job.common.retry.RetryExecutor;
import com.tencent.bk.job.common.retry.metrics.RetryMetricsConstants;
import com.tencent.bk.job.common.retry.metrics.RetryMetricsRecorder;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 支持重试的GSE V2 API客户端
 * <p>
 * 使用指数退避策略进行重试，并记录重试指标
 * </p>
 */
@Slf4j
public class RetryableGseV2ApiClient implements IGseClient {

    @Delegate
    private final IGseClient delegate;
    private final RetryExecutor retryExecutor;

    /**
     * 使用新的全局配置方式创建可重试 GSE 客户端
     *
     * @param delegate        代理的GseClient对象
     * @param meterRegistry   指标注册中心
     * @param retryProperties 外部系统重试配置
     */
    public RetryableGseV2ApiClient(IGseClient delegate,
                                   MeterRegistry meterRegistry,
                                   ExternalSystemRetryProperties retryProperties) {
        this.delegate = delegate;

        // 使用 GSE 系统级配置，如果没有则使用全局配置
        ExternalSystemRetryProperties.SystemRetryProperties gseRetryProps = retryProperties.getGse();
        ExponentialBackoffRetryPolicy retryPolicy = ExponentialBackoffRetryPolicy.builder()
            .initialIntervalMs(retryProperties.getSystemInitialIntervalMs(gseRetryProps))
            .maxIntervalMs(retryProperties.getSystemMaxIntervalMs(gseRetryProps))
            .maxAttempts(retryProperties.getSystemMaxAttempts(gseRetryProps))
            .multiplier(retryProperties.getSystemMultiplier(gseRetryProps))
            .build();

        RetryMetricsRecorder metricsRecorder = new RetryMetricsRecorder(
            meterRegistry,
            retryProperties.isMetricsEnabled()
        );

        this.retryExecutor = new RetryExecutor(
            retryPolicy,
            metricsRecorder,
            RetryMetricsConstants.TAG_VALUE_SYSTEM_GSE
        );

        log.info("Init RetryableGseV2ApiClient with exponential backoff: initialInterval={}ms, maxAttempts={}, " +
                "maxInterval={}ms, multiplier={}",
            retryProperties.getSystemInitialIntervalMs(gseRetryProps),
            retryProperties.getSystemMaxAttempts(gseRetryProps),
            retryProperties.getSystemMaxIntervalMs(gseRetryProps),
            retryProperties.getSystemMultiplier(gseRetryProps));
    }

    @Override
    public ScriptTaskResult getExecuteScriptResult(GetExecuteScriptResultRequest request) {
        return retryExecutor.executeWithRetry(
            () -> delegate.getExecuteScriptResult(request),
            "getExecuteScriptResult"
        );
    }

    @Override
    public List<AgentState> listAgentState(ListAgentStateReq req) {
        return retryExecutor.executeWithRetry(
            () -> delegate.listAgentState(req),
            "listAgentState"
        );
    }

    @Override
    public FileTaskResult getTransferFileResult(GetTransferFileResultRequest request) {
        return retryExecutor.executeWithRetry(
            () -> delegate.getTransferFileResult(request),
            "getTransferFileResult"
        );
    }
}
