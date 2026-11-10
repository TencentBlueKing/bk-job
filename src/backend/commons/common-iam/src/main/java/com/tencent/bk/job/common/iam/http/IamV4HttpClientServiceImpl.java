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
import com.tencent.bk.sdk.iam.constants.HttpHeader;
import com.tencent.bk.sdk.iam.service.HttpClientService;
import com.tencent.bk.sdk.iamv4.config.IamV4Configuration;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;

import java.util.List;

/**
 * IAM v4 专用的 HTTP 客户端（走蓝鲸 APIGW 应用认证），基础地址与应用凭证均取自 {@link IamV4Configuration}。
 */
@Slf4j
public class IamV4HttpClientServiceImpl implements HttpClientService {

    private final HttpHelper httpHelper;
    private final IamV4Configuration iamV4Configuration;
    private final IVirtualAdminAccountProvider virtualAdminAccountProvider;

    public IamV4HttpClientServiceImpl(IamV4Configuration iamV4Configuration,
                                      IVirtualAdminAccountProvider virtualAdminAccountProvider,
                                      boolean sslVerifyEnabled) {
        this.iamV4Configuration = iamV4Configuration;
        this.virtualAdminAccountProvider = virtualAdminAccountProvider;
        this.httpHelper = HttpHelperFactory.getDefaultHttpHelper(sslVerifyEnabled);
        log.debug("IamV4HttpClientServiceImpl init, sslVerifyEnabled={}", sslVerifyEnabled);
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
            logRequest(HttpMethodEnum.GET, tenantId, uri, null, respStr);
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
                        .setHttpEntity(jsonEntity(body))
                        .build())
                .getEntity();
            return respStr;
        } catch (Exception e) {
            log.error("Fail to request IAM v4", e);
            throw new InternalIamException(e, ErrorCode.IAM_API_DATA_ERROR, null);
        } finally {
            HttpMetricUtil.clearHttpMetric();
            logRequest(HttpMethodEnum.POST, tenantId, uri, body, respStr);
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
                        .setHttpEntity(jsonEntity(body))
                        .build())
                .getEntity();
            return respStr;
        } catch (Exception e) {
            log.error("Fail to request IAM v4", e);
            throw new InternalIamException(e, ErrorCode.IAM_API_DATA_ERROR, null);
        } finally {
            HttpMetricUtil.clearHttpMetric();
            logRequest(HttpMethodEnum.PUT, tenantId, uri, body, respStr);
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
            logRequest(HttpMethodEnum.DELETE, tenantId, uri, null, respStr);
        }
    }

    private String buildUrl(String uri) {
        return iamV4Configuration.getIamV4BaseUrl() + uri;
    }

    /**
     * 带上 json 的 Content-Type
     */
    private StringEntity jsonEntity(Object body) {
        return new StringEntity(JsonUtils.toJson(body), ContentType.APPLICATION_JSON);
    }

    private Header[] buildHeaders(String tenantId, List<Pair<String, String>> headerList) {
        int headerSize = (CollectionUtils.isEmpty(headerList) ? 0 : headerList.size()) + 2;
        String username = virtualAdminAccountProvider.getVirtualAdminUsername(tenantId);
        BkApiAuthorization authorization = BkApiAuthorization.appAuthorization(
            iamV4Configuration.getAppCode(),
            iamV4Configuration.getAppSecret(),
            username
        );
        Header[] headers = new Header[headerSize];
        headers[0] = new BasicHeader(HttpHeader.KEY_BK_TENANT_ID, tenantId);
        headers[1] = new BasicHeader(ApiGwConsts.HEADER_BK_API_AUTH, JsonUtils.toJson(authorization));
        if (CollectionUtils.isNotEmpty(headerList)) {
            int index = 2;
            for (Pair<String, String> header : headerList) {
                headers[index++] = new BasicHeader(header.getKey(), header.getValue());
            }
        }
        return headers;
    }

    private void logRequest(HttpMethodEnum method,
                            String tenantId,
                            String uri,
                            Object body,
                            String respStr) {
        log.info(
            "requestIamV4, method={}, tenantId={}, uri={}, body={}, respStr={}",
            method.name(),
            tenantId,
            uri,
            body == null ? null : JsonUtils.toJson(body),
            isSensitiveUri(uri) ? "<redacted>" : respStr
        );
    }

    /**
     * 获取系统auth-token的请求不打印响应
     * @param uri 请求uri，可能是鉴权/获取auth-token 等
     * @return 是否是获取系统auth-token的请求
     */
    private boolean isSensitiveUri(String uri) {
        return uri != null && uri.contains("auth-token");
    }
}
