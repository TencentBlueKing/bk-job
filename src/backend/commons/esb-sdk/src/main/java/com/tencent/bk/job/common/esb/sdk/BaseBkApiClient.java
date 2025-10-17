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
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.HttpMethodEnum;
import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.constant.TenantIdConstants;
import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.constants.EsbLang;
import com.tencent.bk.job.common.esb.exception.BkOpenApiException;
import com.tencent.bk.job.common.esb.interceptor.LogBkApiRequestIdInterceptor;
import com.tencent.bk.job.common.esb.metrics.EsbMetricTags;
import com.tencent.bk.job.common.esb.model.BkApiAuthorization;
import com.tencent.bk.job.common.esb.model.OpenApiRequestInfo;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.http.HttpHelper;
import com.tencent.bk.job.common.util.http.HttpRequest;
import com.tencent.bk.job.common.util.http.HttpResponse;
import com.tencent.bk.job.common.util.json.JsonMapper;
import com.tencent.bk.job.common.util.json.JsonUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.tencent.bk.job.common.constant.HttpHeader.HDR_BK_LANG;
import static com.tencent.bk.job.common.i18n.locale.LocaleUtils.COMMON_LANG_HEADER;

/**
 * 蓝鲸API（组件 API（ESB）、网关 API（蓝鲸 ApiGateway)）调用客户端基础类
 */
public class BaseBkApiClient {

    private String lang;
    private Logger log = LoggerFactory.getLogger(this.getClass());
    private final String baseAccessUrl;
    private final HttpHelper defaultHttpHelper;
    private final MeterRegistry meterRegistry;
    protected final TenantEnvService tenantEnvService;
    private static final String BK_API_AUTH_HEADER = "X-Bkapi-Authorization";
    /**
     * API调用度量指标名称
     */
    private final String metricName;

    @Setter
    private JsonMapper jsonMapper = JsonMapper.nonNullMapper();

    /**
     * @param meterRegistry     MeterRegistry
     * @param metricName        API http 请求指标名称
     * @param baseAccessUrl     API 服务访问地址
     * @param defaultHttpHelper http 请求处理客户端
     */
    public BaseBkApiClient(MeterRegistry meterRegistry,
                           String metricName,
                           String baseAccessUrl,
                           HttpHelper defaultHttpHelper,
                           TenantEnvService tenantEnvService) {
        this.meterRegistry = meterRegistry;
        this.metricName = metricName;
        this.baseAccessUrl = baseAccessUrl;
        this.defaultHttpHelper = defaultHttpHelper;
        this.tenantEnvService = tenantEnvService;
    }

    public BaseBkApiClient(MeterRegistry meterRegistry,
                           String metricName,
                           String baseAccessUrl,
                           HttpHelper defaultHttpHelper,
                           String lang,
                           TenantEnvService tenantEnvService) {
        this(meterRegistry, metricName, baseAccessUrl, defaultHttpHelper, tenantEnvService);
        this.lang = lang;
    }

    /**
     * 配置自定义的日志 logger
     *
     * @param logger logger
     */
    public void setLogger(Logger logger) {
        this.log = logger;
    }

    public <T, R> R doRequest(OpenApiRequestInfo<T> requestInfo,
                              TypeReference<R> typeReference,
                              HttpHelper httpHelper) throws BkOpenApiException {
        return doRequest(requestInfo, typeReference, null, httpHelper);
    }

    public <T, R> R doRequest(OpenApiRequestInfo<T> requestInfo,
                              TypeReference<R> typeReference) throws BkOpenApiException {
        return doRequest(requestInfo, typeReference, null);
    }

    public <T, R> R doRequest(OpenApiRequestInfo<T> requestInfo,
                              TypeReference<R> typeReference,
                              BkApiLogStrategy logStrategy,
                              HttpHelper httpHelper) throws BkOpenApiException {
        HttpMethodEnum httpMethod = requestInfo.getMethod();
        BkApiContext<T, R> apiContext = new BkApiContext<>(
            httpMethod.name(),
            requestInfo.getUri(),
            requestInfo.getBody(),
            requestInfo.buildQueryParamUrl(),
            null,
            null,
            0,
            false
        );

        String tenantId = extractBkTenantId(requestInfo);
        if (logStrategy != null) {
            logStrategy.logReq(log, apiContext);
        } else {
            if (log.isInfoEnabled()) {
                log.info(
                    "[BaseBkApiClient] Request|tenantId={}|method={}|uri={}|reqStr={}",
                    tenantId,
                    httpMethod.name(),
                    requestInfo.buildFinalUri(),
                    requestInfo.getBody() != null ?
                        JsonUtils.toJsonWithoutSkippedFields(requestInfo.getBody()) : null
                );
            }
        }

        try {
            return requestApiAndWrapResponse(requestInfo, apiContext, typeReference, httpHelper);
        } finally {
            if (logStrategy != null) {
                logStrategy.logResp(log, apiContext);
            } else {
                if (log.isInfoEnabled()) {
                    log.info(
                        "[BaseBkApiClient] Response|tenantId={}|bkApiRequestId={}|method={}|uri={}|success={}"
                            + "|costTime={}|resp={}",
                        tenantId,
                        apiContext.getRequestId(),
                        httpMethod.name(),
                        requestInfo.getUri(),
                        apiContext.isSuccess(),
                        apiContext.getCostTime(),
                        apiContext.getOriginResp()
                    );
                }
            }
            if (apiContext.getCostTime() > 5000L) {
                log.info("SlowBkApiRequest|totalCost={}|requestCost={}|deserializeCost={}" +
                        "|responseBodyLength={}",
                    apiContext.getCostTime(),
                    apiContext.getRequestCostTime(),
                    apiContext.getDeserializeCostTime(),
                    apiContext.getOriginResp() != null ? apiContext.getOriginResp().length() : 0L
                );
            }
        }
    }

    private <T, R> R requestApiAndWrapResponse(OpenApiRequestInfo<T> requestInfo,
                                               BkApiContext<T, R> apiContext,
                                               TypeReference<R> typeReference,
                                               HttpHelper httpHelper) throws BkOpenApiException {
        String uri = apiContext.getUri();
        R responseBody;
        String respStr;
        String status = EsbMetricTags.VALUE_STATUS_OK;
        HttpMethodEnum httpMethod = requestInfo.getMethod();
        long startTimestamp = System.currentTimeMillis();
        long responseTimestamp;
        try {
            HttpResponse response = requestApi(httpHelper, requestInfo);
            responseTimestamp = System.currentTimeMillis();
            apiContext.setRequestCostTime(responseTimestamp - startTimestamp);
            apiContext.setRequestId(extractBkApiRequestId(response));
            respStr = response.getEntity();
            apiContext.setOriginResp(response.getEntity());

            if (StringUtils.isBlank(respStr)) {
                String errorMsg = httpMethod.name() + " " + uri + ", error: " + "Response is blank";
                log.warn(
                    "[BaseBkApiClient] fail: Response is blank| bkApiRequestId={}|method={}|uri={}",
                    apiContext.getRequestId(),
                    httpMethod.name(),
                    uri
                );
                status = EsbMetricTags.VALUE_STATUS_ERROR;
                throw new InternalException(errorMsg, ErrorCode.API_ERROR);
            }

            long deserializeStartTimestamp = System.currentTimeMillis();
            responseBody = jsonMapper.fromJson(respStr, typeReference);
            apiContext.setDeserializeCostTime(System.currentTimeMillis() - deserializeStartTimestamp);
            apiContext.setResp(responseBody);
            if (isResponseError(response, responseBody)) {
                status = EsbMetricTags.VALUE_STATUS_ERROR;
                apiContext.setSuccess(false);
                handleResponseError(response, responseBody);
            } else {
                apiContext.setSuccess(true);
            }
            return responseBody;
        } catch (BkOpenApiException e) {
            throw e;
        } catch (Throwable e) {
            String errorMsg = "Fail to request api|method=" + httpMethod.name() + "|uri=" + uri;
            log.error(errorMsg, e);
            apiContext.setSuccess(false);
            status = EsbMetricTags.VALUE_STATUS_ERROR;
            throw new InternalException("Request bk open api error", ErrorCode.API_ERROR);
        } finally {
            long cost = System.currentTimeMillis() - startTimestamp;
            apiContext.setCostTime(cost);
            if (meterRegistry != null) {
                meterRegistry.timer(metricName, buildMetricTags(uri, status))
                    .record(cost, TimeUnit.MILLISECONDS);
            }
        }
    }

    private String extractBkTenantId(OpenApiRequestInfo<?> requestInfo) {
        if (requestInfo.getHeaders() == null) {
            return "";
        }
        for (Header header : requestInfo.getHeaders()) {
            if (JobCommonHeaders.BK_TENANT_ID.equalsIgnoreCase(header.getName())) {
                return header.getValue();
            }
        }
        return "";
    }

    private String extractBkApiRequestId(HttpResponse response) {
        if (response.getHeaders() == null) {
            return "";
        }
        for (Header header : response.getHeaders()) {
            if (JobCommonHeaders.BK_GATEWAY_REQUEST_ID.equalsIgnoreCase(header.getName())) {
                return header.getValue();
            }
        }
        return "";
    }

    private Iterable<Tag> buildMetricTags(String uri, String status) {
        Tags tags = Tags.of(EsbMetricTags.KEY_API_NAME, uri).and(EsbMetricTags.KEY_STATUS, status);
        Collection<Tag> extraTags = getExtraMetricsTags();
        if (CollectionUtils.isNotEmpty(extraTags)) {
            extraTags.forEach(tags::and);
        }
        return tags;
    }

    private <T> HttpResponse requestApi(HttpHelper httpHelper, OpenApiRequestInfo<T> requestInfo) {
        String url = buildApiUrl(requestInfo.buildFinalUri());

        Header[] headers = buildBkApiRequestHeaders(requestInfo);
        HttpRequest httpRequest = HttpRequest.builder(requestInfo.getMethod(), url)
            .setHeaders(headers)
            .setKeepAlive(true)
            .setRetryMode(requestInfo.getRetryMode())
            .setIdempotent(requestInfo.getIdempotent())
            .setStringEntity(requestInfo.getBody() != null ? jsonMapper.toJson(requestInfo.getBody()) : null)
            .build();

        return chooseHttpHelper(httpHelper).request(httpRequest);
    }

    private HttpHelper chooseHttpHelper(HttpHelper httpHelper) {
        return httpHelper != null ? httpHelper : defaultHttpHelper;
    }

    private String buildApiUrl(String uri) {
        String url;
        if (!baseAccessUrl.endsWith("/") && !uri.startsWith("/")) {
            url = baseAccessUrl + "/" + uri;
        } else {
            url = baseAccessUrl + uri;
        }
        return url;
    }

    private <T> Header[] buildBkApiRequestHeaders(OpenApiRequestInfo<T> requestInfo) {
        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Content-Type", "application/json"));
        headers.add(buildBkApiAuthorizationHeader(requestInfo.getAuthorization()));
        if (StringUtils.isNotEmpty(lang)) {
            headers.add(new BasicHeader(HDR_BK_LANG, lang));
        } else {
            headers.add(new BasicHeader(HDR_BK_LANG, getLangFromRequest()));
        }

        if (CollectionUtils.isNotEmpty(requestInfo.getHeaders())) {
            headers.addAll(requestInfo.getHeaders());
        }

        checkTenantHeader(headers);

        Header[] headerArray = new Header[headers.size()];
        return headers.toArray(headerArray);
    }

    /**
     * 检查调用蓝鲸 Open API 的请求是否包含租户 header
     *
     * @param headers 请求 headers
     */
    private void checkTenantHeader(List<Header> headers) {
        boolean containsTenantHeader = headers.stream()
            .anyMatch(header -> header.getName().equalsIgnoreCase(JobCommonHeaders.BK_TENANT_ID));
        if (!containsTenantHeader) {
            if (tenantEnvService.isTenantEnabled()) {
                throw new InternalException(
                    "Header: " + JobCommonHeaders.BK_TENANT_ID + " is required",
                    ErrorCode.API_ERROR
                );
            } else {
                headers.add(buildTenantHeader(TenantIdConstants.DEFAULT_TENANT_ID));
            }
        }
    }

    private Header buildBkApiAuthorizationHeader(BkApiAuthorization authorization) {
        if (authorization == null) {
            log.error("Bk Api authorization header is missing");
            throw new InternalException("Header: " + BK_API_AUTH_HEADER + " is required", ErrorCode.API_ERROR);
        }
        return new BasicHeader(BK_API_AUTH_HEADER, jsonMapper.toJson(authorization));
    }

    private String getLangFromRequest() {
        try {
            HttpServletRequest request = JobContextUtil.getRequest();
            String lang = null;
            if (request != null) {
                lang = request.getHeader(COMMON_LANG_HEADER);
            }

            return StringUtils.isEmpty(lang) ? EsbLang.EN : lang;
        } catch (Throwable ignore) {
            return EsbLang.EN;
        }
    }

    /**
     * 额外的监控指标 tag
     *
     * @return tag
     */
    protected Collection<Tag> getExtraMetricsTags() {
        return null;
    }

    /**
     * 获取打印APIGW RequestId的响应拦截器
     *
     * @return 响应拦截器
     */
    protected static HttpResponseInterceptor getLogBkApiRequestIdInterceptor() {
        return new LogBkApiRequestIdInterceptor();
    }

    protected boolean isResponseError(HttpResponse httpResponse, Object responseBody) {
        // http code - 2xx
        HttpStatus httpStatus = HttpStatus.resolve(httpResponse.getStatusCode());
        if (httpStatus == null) {
            return true;
        }
        return !httpStatus.is2xxSuccessful();
    }

    protected void handleResponseError(HttpResponse httpResponse, Object responseBody)
        throws BkOpenApiException {
        throw new BkOpenApiException(httpResponse.getStatusCode());
    }

    protected Header buildTenantHeader(String tenantId) {
        return new BasicHeader(JobCommonHeaders.BK_TENANT_ID, tenantId);
    }

    protected BkApiAuthorization buildAuthorization(AppProperties appProperties, String username) {
        return BkApiAuthorization.appAuthorization(
            appProperties.getCode(),
            appProperties.getSecret(),
            username
        );
    }
}

