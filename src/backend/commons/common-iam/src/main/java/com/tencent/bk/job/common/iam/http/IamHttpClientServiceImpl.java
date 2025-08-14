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
import com.tencent.bk.job.common.esb.constants.ApiGwConsts;
import com.tencent.bk.job.common.esb.model.BkApiAuthorization;
import com.tencent.bk.job.common.exception.InternalIamException;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.paas.user.IVirtualAdminAccountProvider;
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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import java.util.List;

@Slf4j
public class IamHttpClientServiceImpl implements HttpClientService {

    private final HttpHelper httpHelper = HttpHelperFactory.getDefaultHttpHelper();
    private final IamConfiguration iamConfiguration;
    protected final IVirtualAdminAccountProvider virtualAdminAccountProvider;

    public IamHttpClientServiceImpl(IamConfiguration iamConfiguration,
                                    IVirtualAdminAccountProvider virtualAdminAccountProvider) {
        this.iamConfiguration = iamConfiguration;
        this.virtualAdminAccountProvider = virtualAdminAccountProvider;
        log.debug("IamHttpClientServiceImpl init");
    }

    @Override
    public String doHttpGet(String tenantId, String uri, List<Pair<String, String>> headerList) {
        String respStr = null;
        try {
            HttpMetricUtil.setHttpMetricName(CommonMetricNames.IAM_API_HTTP);
            HttpMetricUtil.addTagForCurrentMetric(Tag.of("api_name", uri));
            respStr = httpHelper.requestForSuccessResp(
                    HttpRequest.builder(HttpMethodEnum.GET, buildUrl(uri))
                        .setHeaders(buildHeaders(tenantId, headerList))
                        .build())
                .getEntity();
            return respStr;
        } catch (Exception e) {
            throw new InternalIamException(e, ErrorCode.IAM_API_DATA_ERROR, null);
        } finally {
            HttpMetricUtil.clearHttpMetric();
            logRequest(HttpMethodEnum.GET, tenantId, uri, headerList, null, respStr);
        }
    }

    @Override
    public String doHttpPost(String tenantId, String uri, List<Pair<String, String>> headerList, Object body) {
        String respStr = null;
        try {
            HttpMetricUtil.setHttpMetricName(CommonMetricNames.IAM_API_HTTP);
            HttpMetricUtil.addTagForCurrentMetric(Tag.of("api_name", uri));
            respStr = httpHelper.requestForSuccessResp(
                    HttpRequest.builder(HttpMethodEnum.POST, buildUrl(uri))
                        .setHeaders(buildHeaders(tenantId, headerList))
                        .setStringEntity(JsonUtils.toJson(body))
                        .build())
                .getEntity();
            return respStr;
        } catch (Exception e) {
            log.error("Fail to request IAM", e);
            throw new InternalIamException(e, ErrorCode.IAM_API_DATA_ERROR, null);
        } finally {
            HttpMetricUtil.clearHttpMetric();
            logRequest(HttpMethodEnum.POST, tenantId, uri, headerList, body, respStr);
        }
    }

    @Override
    public String doHttpPut(String tenantId, String uri, List<Pair<String, String>> headerList, Object body) {
        String respStr = null;
        try {
            HttpMetricUtil.setHttpMetricName(CommonMetricNames.IAM_API_HTTP);
            HttpMetricUtil.addTagForCurrentMetric(Tag.of("api_name", uri));
            respStr = httpHelper.requestForSuccessResp(
                    HttpRequest.builder(HttpMethodEnum.PUT, buildUrl(uri))
                        .setHeaders(buildHeaders(tenantId, headerList))
                        .setStringEntity(JsonUtils.toJson(body))
                        .build())
                .getEntity();
            return respStr;
        } catch (Exception e) {
            log.error("Fail to request IAM", e);
            throw new InternalIamException(e, ErrorCode.IAM_API_DATA_ERROR, null);
        } finally {
            HttpMetricUtil.clearHttpMetric();
            logRequest(HttpMethodEnum.PUT, tenantId, uri, headerList, body, respStr);
        }
    }

    @Override
    public String doHttpDelete(String tenantId, String uri, List<Pair<String, String>> headerList) {
        String respStr = null;
        try {
            HttpMetricUtil.setHttpMetricName(CommonMetricNames.IAM_API_HTTP);
            HttpMetricUtil.addTagForCurrentMetric(Tag.of("api_name", uri));
            respStr = httpHelper.requestForSuccessResp(
                    HttpRequest.builder(HttpMethodEnum.DELETE, buildUrl(uri))
                        .setHeaders(buildHeaders(tenantId, headerList))
                        .build())
                .getEntity();
            return respStr;
        } catch (Exception e) {
            throw new InternalIamException(e, ErrorCode.IAM_API_DATA_ERROR, null);
        } finally {
            HttpMetricUtil.clearHttpMetric();
            logRequest(HttpMethodEnum.DELETE, tenantId, uri, headerList, null, respStr);
        }
    }

    private String buildUrl(String uri) {
        return iamConfiguration.getIamBaseUrl() + uri;
    }

    private Header[] buildHeaders(String tenantId, List<Pair<String, String>> headerList) {
        int headerSize = (CollectionUtils.isEmpty(headerList) ? 0 : headerList.size()) + 2;
        String username = virtualAdminAccountProvider.getVirtualAdminUsername(tenantId);
        BkApiAuthorization authorization = BkApiAuthorization.appAuthorization(
            iamConfiguration.getAppCode(),
            iamConfiguration.getAppSecret(),
            username
        );
        Header[] headers = new Header[headerSize];
        headers[0] = buildTenantIdHeader(tenantId);
        headers[1] = new BasicHeader(ApiGwConsts.HEADER_BK_API_AUTH, JsonUtils.toJson(authorization));
        if (CollectionUtils.isNotEmpty(headerList)) {
            int index = 2;
            for (Pair<String, String> header : headerList) {
                headers[index++] = new BasicHeader(header.getKey(), header.getValue());
            }
        }
        return headers;
    }

    private Header buildTenantIdHeader(String tenantId) {
        return new BasicHeader(HttpHeader.KEY_BK_TENANT_ID, tenantId);
    }

    private void logRequest(HttpMethodEnum method,
                            String tenantId,
                            String uri,
                            List<Pair<String, String>> headerList,
                            Object body,
                            String respStr) {
        log.info(
            "requestIAM, method={}, tenantId={}, uri={}, headerList={}, body={}, respStr={}",
            method.name(),
            tenantId,
            uri,
            headerList == null ? null : JsonUtils.toJson(headerList),
            body == null ? null : JsonUtils.toJson(body),
            respStr
        );
    }
}
