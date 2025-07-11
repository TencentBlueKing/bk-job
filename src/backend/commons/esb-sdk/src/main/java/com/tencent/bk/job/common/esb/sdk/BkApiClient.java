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
import com.tencent.bk.job.common.esb.constants.EsbLang;
import com.tencent.bk.job.common.esb.metrics.EsbMetricTags;
import com.tencent.bk.job.common.esb.model.BkApiAuthorization;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.OpenApiRequestInfo;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.http.HttpHelper;
import com.tencent.bk.job.common.util.http.HttpRequest;
import com.tencent.bk.job.common.util.http.HttpResponse;
import com.tencent.bk.job.common.util.json.JsonMapper;
import com.tencent.bk.job.common.util.json.JsonUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static com.tencent.bk.job.common.constant.HttpHeader.HDR_BK_LANG;
import static com.tencent.bk.job.common.i18n.locale.LocaleUtils.COMMON_LANG_HEADER;

/**
 * 蓝鲸API（组件 API（ESB）、网关 API（蓝鲸 ApiGateway)）调用客户端
 */
public class BkApiClient {

    private String lang;
    private Logger log = LoggerFactory.getLogger(this.getClass());
    private final String baseAccessUrl;
    private final HttpHelper defaultHttpHelper;
    private final MeterRegistry meterRegistry;
    private static final String BK_API_AUTH_HEADER = "X-Bkapi-Authorization";
    /**
     * API调用度量指标名称
     */
    private final String metricName;
    private JsonMapper jsonMapper = JsonMapper.nonNullMapper();

    /**
     * @param meterRegistry     MeterRegistry
     * @param metricName        API http 请求指标名称
     * @param baseAccessUrl     API 服务访问地址
     * @param defaultHttpHelper http 请求处理客户端
     */
    public BkApiClient(MeterRegistry meterRegistry,
                       String metricName,
                       String baseAccessUrl,
                       HttpHelper defaultHttpHelper) {
        this.meterRegistry = meterRegistry;
        this.metricName = metricName;
        this.baseAccessUrl = baseAccessUrl;
        this.defaultHttpHelper = defaultHttpHelper;
    }

    public BkApiClient(MeterRegistry meterRegistry,
                       String metricName,
                       String baseAccessUrl,
                       HttpHelper defaultHttpHelper,
                       String lang) {
        this(meterRegistry, metricName, baseAccessUrl, defaultHttpHelper);
        this.lang = lang;
    }

    /**
     * 配置自定义的 JsonMapper, 用于序列化 Json 数据
     *
     * @param jsonMapper jsonMapper
     */
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * 配置自定义的日志 logger
     *
     * @param logger logger
     */
    public void setLogger(Logger logger) {
        this.log = logger;
    }

    public <T, R> EsbResp<R> doRequest(OpenApiRequestInfo<T> requestInfo,
                                       TypeReference<EsbResp<R>> typeReference,
                                       HttpHelper httpHelper) {
        return doRequest(requestInfo, typeReference, null, httpHelper);
    }

    public <T, R> EsbResp<R> doRequest(OpenApiRequestInfo<T> requestInfo,
                                       TypeReference<EsbResp<R>> typeReference) {
        return doRequest(requestInfo, typeReference, null);
    }

    public <T, R> EsbResp<R> doRequest(OpenApiRequestInfo<T> requestInfo,
                                       TypeReference<EsbResp<R>> typeReference,
                                       BkApiLogStrategy logStrategy,
                                       HttpHelper httpHelper) {
        HttpMethodEnum httpMethod = requestInfo.getMethod();
        BkApiContext<T, R> apiContext = new BkApiContext<>(httpMethod.name(), requestInfo.getUri(),
            requestInfo.getBody(), null, null, 0, false);

        if (logStrategy != null) {
            logStrategy.logReq(log, apiContext);
        } else {
            if (log.isInfoEnabled()) {
                log.info("[AbstractBkApiClient] Request|method={}|uri={}|reqStr={}",
                    httpMethod.name(), requestInfo.getUri(),
                    requestInfo.getBody() != null ? JsonUtils.toJsonWithoutSkippedFields(requestInfo.getBody()) : null);
            }
        }

        try {
            return requestApiAndWrapResponse(requestInfo, apiContext, typeReference, httpHelper);
        } finally {
            if (logStrategy != null) {
                logStrategy.logResp(log, apiContext);
            } else {
                if (log.isInfoEnabled()) {
                    log.info("[AbstractBkApiClient] Response|bkApiRequestId={}|method={}|uri={}|success={}"
                            + "|costTime={}|resp={}",
                        apiContext.getRequestId(), httpMethod.name(), requestInfo.getUri(), apiContext.isSuccess(),
                        apiContext.getCostTime(), apiContext.getOriginResp());
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

    public <T, R> R requestApiAndWrapResponse(OpenApiRequestInfo<T> requestInfo,
                                              TypeReference<R> typeReference,
                                              HttpHelper httpHelper) {
        if (log.isInfoEnabled()) {
            log.info("[AbstractBkApiClient] Request|method={}|uri={}|reqStr={}",
                requestInfo.getMethod().name(), requestInfo.getUri(),
                requestInfo.getBody() != null ? JsonUtils.toJsonWithoutSkippedFields(requestInfo.getBody()) : null);
        }
        String uri = requestInfo.getUri();
        String respStr = null;
        String status = EsbMetricTags.VALUE_STATUS_OK;
        HttpMethodEnum httpMethod = requestInfo.getMethod();
        long start = System.currentTimeMillis();
        String bkApiRequestId = null;
        boolean success = true;
        try {
            HttpResponse response = requestApi(httpHelper, requestInfo);
            bkApiRequestId = extractBkApiRequestId(response);
            respStr = response.getEntity();
            if (StringUtils.isBlank(respStr)) {
                String errorMsg = "[AbstractBkApiClient] " + httpMethod.name() + " "
                    + uri + ", error: " + "Response is blank";
                log.warn(
                    "[AbstractBkApiClient] fail: Response is blank| requestId={}|method={}|uri={}",
                    bkApiRequestId,
                    httpMethod.name(),
                    uri
                );
                status = EsbMetricTags.VALUE_STATUS_ERROR;
                throw new InternalException(errorMsg, ErrorCode.API_ERROR);
            }
            return jsonMapper.fromJson(respStr, typeReference);
        } catch (Throwable e) {
            success = false;
            String errorMsg = "Fail to request api|method=" + httpMethod.name()
                + "|uri=" + uri;
            log.error(errorMsg, e);
            status = EsbMetricTags.VALUE_STATUS_ERROR;
            throw new InternalException("Fail to request bk api", e, ErrorCode.API_ERROR);
        } finally {
            long cost = System.currentTimeMillis() - start;
            if (meterRegistry != null) {
                meterRegistry.timer(metricName, buildMetricTags(uri, status))
                    .record(cost, TimeUnit.MILLISECONDS);
            }
            if (log.isInfoEnabled()) {
                log.info("[AbstractBkApiClient] Response|requestId={}|method={}|uri={}|success={}"
                        + "|costTime={}|resp={}",
                    bkApiRequestId, httpMethod.name(), requestInfo.getUri(), success, cost, respStr);
            }
        }
    }

    private <T, R> EsbResp<R> requestApiAndWrapResponse(OpenApiRequestInfo<T> requestInfo,
                                                        BkApiContext<T, R> apiContext,
                                                        TypeReference<EsbResp<R>> typeReference,
                                                        HttpHelper httpHelper) {
        String uri = apiContext.getUri();
        EsbResp<R> esbResp;
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
                String errorMsg = "[AbstractBkApiClient] " + httpMethod.name() + " "
                    + uri + ", error: " + "Response is blank";
                log.warn("[AbstractBkApiClient] fail: Response is blank| bkApiRequestId={}|method={}|uri={}",
                    apiContext.getRequestId(), httpMethod.name(), uri);
                status = EsbMetricTags.VALUE_STATUS_ERROR;
                throw new InternalException(errorMsg, ErrorCode.API_ERROR);
            }

            long deserializeStartTimestamp = System.currentTimeMillis();
            esbResp = jsonMapper.fromJson(respStr, typeReference);
            apiContext.setDeserializeCostTime(System.currentTimeMillis() - deserializeStartTimestamp);
            apiContext.setResp(esbResp);
            if (!esbResp.isSuccess()) {
                log.warn(
                    "[AbstractBkApiClient] fail:response code!=0" +
                        "|bkApiRequestId={}|code={}|message={}|method={}|uri={}|reqStr={}|respStr={}",
                    apiContext.getRequestId(),
                    esbResp.getCode(),
                    esbResp.getMessage(),
                    httpMethod.name(),
                    uri,
                    apiContext.getReq() != null ? JsonUtils.toJsonWithoutSkippedFields(apiContext.getReq()) : null,
                    respStr
                );
                status = EsbMetricTags.VALUE_STATUS_ERROR;
            }
            if (esbResp.getData() == null) {
                log.warn(
                    "[AbstractBkApiClient] warn: response data is null" +
                        "|bkApiRequestId={}|code={}|message={}|method={}|uri={}|reqStr={}|respStr={}",
                    apiContext.getRequestId(),
                    esbResp.getCode(),
                    esbResp.getMessage(),
                    httpMethod.name(),
                    uri,
                    apiContext.getReq() != null ? JsonUtils.toJsonWithoutSkippedFields(apiContext.getReq()) : null,
                    respStr
                );
            }
            apiContext.setSuccess(true);
            return esbResp;
        } catch (Throwable e) {
            String errorMsg = "Fail to request api|method=" + httpMethod.name()
                + "|uri=" + uri;
            log.error(errorMsg, e);
            apiContext.setSuccess(false);
            status = EsbMetricTags.VALUE_STATUS_ERROR;
            throw new InternalException("Fail to request bk api", e, ErrorCode.API_ERROR);
        } finally {
            long cost = System.currentTimeMillis() - startTimestamp;
            apiContext.setCostTime(cost);
            if (meterRegistry != null) {
                meterRegistry.timer(metricName, buildMetricTags(uri, status))
                    .record(cost, TimeUnit.MILLISECONDS);
            }
        }
    }

    private String extractBkApiRequestId(HttpResponse response) {
        if (response.getHeaders() == null || response.getHeaders().length == 0) {
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

        Header[] headers = buildBkApiRequestHeaders(requestInfo.getAuthorization());
        HttpRequest httpRequest = HttpRequest.builder(requestInfo.getMethod(), url)
            .setHeaders(headers)
            .setKeepAlive(true)
            .setRetryMode(requestInfo.getRetryMode())
            .setIdempotent(requestInfo.getIdempotent())
            .setStringEntity(requestInfo.getBody() != null ? jsonMapper.toJson(requestInfo.getBody()) : null)
            .build();

        return chooseHttpHelper(httpHelper).requestForSuccessResp(httpRequest);
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

    private Header[] buildBkApiRequestHeaders(BkApiAuthorization authorization) {
        Header[] header = new Header[3];
        header[0] = new BasicHeader("Content-Type", "application/json");
        header[1] = buildBkApiAuthorizationHeader(authorization);
        if (StringUtils.isNotEmpty(lang)) {
            header[2] = new BasicHeader(HDR_BK_LANG, lang);
        } else {
            header[2] = new BasicHeader(HDR_BK_LANG, getLangFromRequest());
        }
        return header;
    }

    private Header buildBkApiAuthorizationHeader(BkApiAuthorization authorization) {
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

}
