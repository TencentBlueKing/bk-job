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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.HttpMethodEnum;
import com.tencent.bk.job.common.exception.InternalIamException;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.util.http.HttpHelper;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import com.tencent.bk.job.common.util.http.HttpMetricUtil;
import com.tencent.bk.job.common.util.http.HttpRequest;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.sdk.iam.config.IamConfiguration;
import com.tencent.bk.sdk.iam.constants.HttpHeader;
import com.tencent.bk.sdk.iam.service.HttpClientService;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import java.util.ArrayList;
import java.util.List;

import static com.tencent.bk.job.common.constant.JobConstants.DEFAULT_TENANT_ID;

@Slf4j
public class IamHttpClientServiceImpl implements HttpClientService {

    private final String DEFAULT_CHARSET = "UTF-8";
    private final HttpHelper httpHelper;
    private final IamConfiguration iamConfiguration;

    public IamHttpClientServiceImpl(IamConfiguration iamConfiguration) {
        this(iamConfiguration, true);
    }

    public IamHttpClientServiceImpl(IamConfiguration iamConfiguration, boolean sslVerifyEnabled) {
        this.iamConfiguration = iamConfiguration;
        this.httpHelper = HttpHelperFactory.getDefaultHttpHelper(sslVerifyEnabled);
        log.debug("IamHttpClientServiceImpl init, sslVerifyEnabled={}", sslVerifyEnabled);
    }

    @Override
    public String doHttpGet(String tenantId, String uri, List<Pair<String, String>> headers) {
        try {
            HttpMetricUtil.setHttpMetricName(CommonMetricNames.IAM_API_HTTP);
            HttpMetricUtil.addTagForCurrentMetric(Tag.of("api_name", uri));
            return httpHelper.requestForSuccessResp(
                HttpRequest.builder(HttpMethodEnum.GET, buildUrl(uri))
                    .setHeaders(buildHeaders(headers))
                    .build())
                .getEntity();
        } catch (Exception e) {
            throw new InternalIamException(e, ErrorCode.IAM_API_DATA_ERROR, null);
        } finally {
            HttpMetricUtil.clearHttpMetric();
        }
    }

    @Override
    public String doHttpPost(String tenantId, String uri, List<Pair<String, String>> headers, Object body) {
        try {
            HttpMetricUtil.setHttpMetricName(CommonMetricNames.IAM_API_HTTP);
            HttpMetricUtil.addTagForCurrentMetric(Tag.of("api_name", uri));
            return httpHelper.requestForSuccessResp(
                HttpRequest.builder(HttpMethodEnum.POST, buildUrl(uri))
                    .setHeaders(buildHeaders(headers))
                    .setStringEntity(JsonUtils.toJson(body))
                    .build())
                .getEntity();
        } catch (Exception e) {
            log.error("Fail to request IAM", e);
            throw new InternalIamException(e, ErrorCode.IAM_API_DATA_ERROR, null);
        } finally {
            HttpMetricUtil.clearHttpMetric();
        }
    }

    @Override
    public String doHttpPut(String tenantId, String uri, List<Pair<String, String>> headers, Object body) {
        try {
            HttpMetricUtil.setHttpMetricName(CommonMetricNames.IAM_API_HTTP);
            HttpMetricUtil.addTagForCurrentMetric(Tag.of("api_name", uri));
            return httpHelper.requestForSuccessResp(
                HttpRequest.builder(HttpMethodEnum.PUT, buildUrl(uri))
                    .setHeaders(buildHeaders(headers))
                    .setStringEntity(JsonUtils.toJson(body))
                    .build())
                .getEntity();
        } catch (Exception e) {
            log.error("Fail to request IAM", e);
            throw new InternalIamException(e, ErrorCode.IAM_API_DATA_ERROR, null);
        } finally {
            HttpMetricUtil.clearHttpMetric();
        }
    }

    @Override
    public String doHttpDelete(String tenantId, String uri, List<Pair<String, String>> headers) {
        try {
            HttpMetricUtil.setHttpMetricName(CommonMetricNames.IAM_API_HTTP);
            HttpMetricUtil.addTagForCurrentMetric(Tag.of("api_name", uri));
            return httpHelper.requestForSuccessResp(
                HttpRequest.builder(HttpMethodEnum.DELETE, buildUrl(uri))
                    .setHeaders(buildHeaders(headers))
                    .build())
                .getEntity();
        } catch (Exception e) {
            throw new InternalIamException(e, ErrorCode.IAM_API_DATA_ERROR, null);
        } finally {
            HttpMetricUtil.clearHttpMetric();
        }
    }

    private String buildUrl(String uri) {
        return iamConfiguration.getIamBaseUrl() + uri;
    }

    private Header[] buildHeaders(List<Pair<String, String>> extraHeaders) {
        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader(HttpHeader.BK_APP_CODE, iamConfiguration.getAppCode()));
        headers.add(new BasicHeader(HttpHeader.BK_APP_SECRET, iamConfiguration.getAppSecret()));
        headers.add(new BasicHeader(HttpHeader.KEY_BK_TENANT_ID, DEFAULT_TENANT_ID));
        if (extraHeaders != null) {
            extraHeaders.forEach(header -> headers.add(new BasicHeader(header.getLeft(), header.getRight())));
        }
        return headers.toArray(new Header[0]);
    }

}
