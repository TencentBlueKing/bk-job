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

package com.tencent.bk.job.common.iam.http;

import com.tencent.bk.job.common.retry.RetryExecutor;
import com.tencent.bk.sdk.iam.service.HttpClientService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * 为 IAM v4 的 HTTP 客户端增加重试能力的装饰器。
 *
 * <p>v4 的鉴权/申请/token 等调用都经由 SDK 注入的 {@link HttpClientService}，此装饰器在该传输层
 * 用 {@link RetryExecutor}（指数退避 + 熔断 + metrics）对每次幂等 HTTP 请求做重试。</p>
 */
@Slf4j
public class RetryableIamV4HttpClientService implements HttpClientService {

    private final HttpClientService delegate;
    private final RetryExecutor retryExecutor;

    public RetryableIamV4HttpClientService(HttpClientService delegate, RetryExecutor retryExecutor) {
        this.delegate = delegate;
        this.retryExecutor = retryExecutor;
    }

    @Override
    public String doHttpGet(String tenantId, String uri, List<Pair<String, String>> headerList) {
        return retryExecutor.executeWithRetry(
            () -> delegate.doHttpGet(tenantId, uri, headerList), "iamV4HttpGet");
    }

    @Override
    public String doHttpPost(String tenantId, String uri, List<Pair<String, String>> headerList, Object body) {
        return retryExecutor.executeWithRetry(
            () -> delegate.doHttpPost(tenantId, uri, headerList, body), "iamV4HttpPost");
    }

    @Override
    public String doHttpPut(String tenantId, String uri, List<Pair<String, String>> headerList, Object body) {
        return retryExecutor.executeWithRetry(
            () -> delegate.doHttpPut(tenantId, uri, headerList, body), "iamV4HttpPut");
    }

    @Override
    public String doHttpDelete(String tenantId, String uri, List<Pair<String, String>> headerList) {
        return retryExecutor.executeWithRetry(
            () -> delegate.doHttpDelete(tenantId, uri, headerList), "iamV4HttpDelete");
    }
}
