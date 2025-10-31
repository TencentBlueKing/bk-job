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

import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.BkApiGatewayProperties;
import com.tencent.bk.job.common.gse.config.GseV2Properties;
import com.tencent.bk.job.common.gse.v2.model.FileTaskResult;
import com.tencent.bk.job.common.gse.v2.model.GetExecuteScriptResultRequest;
import com.tencent.bk.job.common.gse.v2.model.GetTransferFileResultRequest;
import com.tencent.bk.job.common.gse.v2.model.ScriptTaskResult;
import com.tencent.bk.job.common.gse.v2.model.req.ListAgentStateReq;
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;
import com.tencent.bk.job.common.retry.RetryUtils;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;

/**
 * 支持重试的GSE V2 API客户端
 */
@Slf4j
public class RetryableGseV2ApiClient extends GseV2ApiClient {

    /**
     * 含重试的最大执行次数
     */
    private final Integer maxAttempts;
    /**
     * 重试间隔（单位：秒）
     */
    private final Integer intervalSeconds;

    public RetryableGseV2ApiClient(MeterRegistry meterRegistry,
                                   AppProperties appProperties,
                                   BkApiGatewayProperties bkApiGatewayProperties,
                                   GseV2Properties gseV2Properties,
                                   TenantEnvService tenantEnvService) {
        super(meterRegistry, appProperties, bkApiGatewayProperties, tenantEnvService);
        this.maxAttempts = gseV2Properties.getRetry().getMaxAttempts();
        this.intervalSeconds = gseV2Properties.getRetry().getIntervalSeconds();
    }

    @Override
    public ScriptTaskResult getExecuteScriptResult(GetExecuteScriptResultRequest request) {
        return RetryUtils.executeWithRetry(
            () -> super.getExecuteScriptResult(request),
            maxAttempts,
            Duration.ofSeconds(intervalSeconds)
        );
    }

    @Override
    public List<AgentState> listAgentState(ListAgentStateReq req) {
        return RetryUtils.executeWithRetry(
            () -> super.listAgentState(req),
            maxAttempts,
            Duration.ofSeconds(intervalSeconds)
        );
    }

    @Override
    public FileTaskResult getTransferFileResult(GetTransferFileResultRequest request) {
        return RetryUtils.executeWithRetry(
            () -> super.getTransferFileResult(request),
            maxAttempts,
            Duration.ofSeconds(intervalSeconds)
        );
    }
}
