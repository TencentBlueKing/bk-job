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

package com.tencent.bk.job.common.esb.sdk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.esb.exception.BkOpenApiException;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.OpenApiRequestInfo;
import com.tencent.bk.job.common.esb.model.OpenApiV1Error;
import com.tencent.bk.job.common.util.http.HttpHelper;
import com.tencent.bk.job.common.util.http.HttpResponse;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * 蓝鲸API（组件 API（ESB）、网关 API（蓝鲸 ApiGateway)）调用客户端 (v1版本)
 */
public class BkApiV1Client extends BaseBkApiClient {

    /**
     * @param meterRegistry     MeterRegistry
     * @param metricName        API http 请求指标名称
     * @param baseAccessUrl     API 服务访问地址
     * @param defaultHttpHelper http 请求处理客户端
     */
    public BkApiV1Client(MeterRegistry meterRegistry,
                         String metricName,
                         String baseAccessUrl,
                         HttpHelper defaultHttpHelper) {
        super(meterRegistry, metricName, baseAccessUrl, defaultHttpHelper);
    }

    /**
     * @param meterRegistry     MeterRegistry
     * @param metricName        API http 请求指标名称
     * @param baseAccessUrl     API 服务访问地址
     * @param defaultHttpHelper http 请求处理客户端
     * @param lang              语言
     */
    public BkApiV1Client(MeterRegistry meterRegistry,
                         String metricName,
                         String baseAccessUrl,
                         HttpHelper defaultHttpHelper,
                         String lang) {
        super(meterRegistry, metricName, baseAccessUrl, defaultHttpHelper, lang);
    }

    public <T, V> EsbResp<V> request(OpenApiRequestInfo<T> requestInfo,
                                     TypeReference<EsbResp<V>> typeReference,
                                     HttpHelper httpHelper) throws BkOpenApiException {
        return doRequest(requestInfo, typeReference, null, httpHelper);
    }

    public <T, V> EsbResp<V> request(OpenApiRequestInfo<T> requestInfo,
                                     TypeReference<EsbResp<V>> typeReference) throws BkOpenApiException {
        return doRequest(requestInfo, typeReference, null);
    }

    @Override
    protected boolean isResponseError(HttpResponse httpResponse, Object responseBody) {
        EsbResp<?> esbResp = (EsbResp<?>) responseBody;
        return super.isResponseError(httpResponse, responseBody) && !esbResp.isSuccess();
    }

    @Override
    protected void handleResponseError(HttpResponse httpResponse, Object responseBody)
        throws BkOpenApiException {
        EsbResp<?> esbResp = (EsbResp<?>) responseBody;
        OpenApiV1Error error = new OpenApiV1Error();
        error.setCode(esbResp.getCode());
        error.setMessage(esbResp.getMessage());
        throw new BkOpenApiException(httpResponse.getStatusCode(), error);
    }
}
