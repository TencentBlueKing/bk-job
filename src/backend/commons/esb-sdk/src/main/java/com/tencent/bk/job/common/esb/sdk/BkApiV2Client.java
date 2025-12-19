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

package com.tencent.bk.job.common.esb.sdk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.esb.exception.BkOpenApiException;
import com.tencent.bk.job.common.esb.model.OpenApiRequestInfo;
import com.tencent.bk.job.common.esb.model.OpenApiResponse;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.http.HttpHelper;
import com.tencent.bk.job.common.util.http.HttpResponse;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * 网关 API（蓝鲸 ApiGateway)）调用客户端 (v2版本)
 */
public class BkApiV2Client extends BaseBkApiClient {

    /**
     * @param meterRegistry     MeterRegistry
     * @param metricName        API http 请求指标名称
     * @param baseAccessUrl     API 服务访问地址
     * @param defaultHttpHelper http 请求处理客户端
     * @param tenantEnvService  租户环境信息 Service
     */
    public BkApiV2Client(MeterRegistry meterRegistry,
                         String metricName,
                         String baseAccessUrl,
                         HttpHelper defaultHttpHelper,
                         TenantEnvService tenantEnvService) {
        super(meterRegistry, metricName, baseAccessUrl, defaultHttpHelper, tenantEnvService);
    }

    public <T, V> OpenApiResponse<V> request(OpenApiRequestInfo<T> requestInfo,
                                             TypeReference<OpenApiResponse<V>> typeReference,
                                             HttpHelper httpHelper) throws BkOpenApiException {
        return doRequest(requestInfo, typeReference, null, httpHelper);
    }

    public <T, V> OpenApiResponse<V> request(
        OpenApiRequestInfo<T> requestInfo,
        TypeReference<OpenApiResponse<V>> typeReference) throws BkOpenApiException {

        return doRequest(requestInfo, typeReference, null);
    }

    @Override
    protected void handleResponseError(HttpResponse httpResponse, Object responseBody)
        throws BkOpenApiException {
        OpenApiResponse<?> response = (OpenApiResponse<?>) responseBody;
        throw new BkOpenApiException(httpResponse.getStatusCode(), response.getError());
    }
}
