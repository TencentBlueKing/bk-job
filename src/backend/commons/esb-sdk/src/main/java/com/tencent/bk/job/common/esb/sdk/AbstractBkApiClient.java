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
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.HttpMethodEnum;
import com.tencent.bk.job.common.esb.constants.EsbLang;
import com.tencent.bk.job.common.esb.metrics.EsbMetricTags;
import com.tencent.bk.job.common.esb.model.ApiRequestInfo;
import com.tencent.bk.job.common.esb.model.BkApiAuthorization;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.http.ExtHttpHelper;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static com.tencent.bk.job.common.constant.HttpHeader.HDR_BK_LANG;
import static com.tencent.bk.job.common.i18n.locale.LocaleUtils.COMMON_LANG_HEADER;

/**
 * 蓝鲸API（组件 API（ESB）、网关 API（蓝鲸 ApiGateway)）调用客户端
 */
public abstract class AbstractBkApiClient {

    private String lang;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final String baseAccessUrl;
    private final ExtHttpHelper defaultHttpHelper = HttpHelperFactory.getDefaultHttpHelper();
    private final MeterRegistry meterRegistry;
    private static final String BK_API_AUTH_HEADER = "X-Bkapi-Authorization";
    /**
     * API调用度量指标名称
     */
    private final String metricName;
    private JsonMapper jsonMapper = JsonMapper.nonNullMapper();

    /**
     * @param meterRegistry MeterRegistry
     * @param metricName    API http 请求指标名称
     * @param baseAccessUrl API 服务访问地址
     */
    public AbstractBkApiClient(MeterRegistry meterRegistry,
                               String metricName,
                               String baseAccessUrl) {
        this.meterRegistry = meterRegistry;
        this.metricName = metricName;
        this.baseAccessUrl = baseAccessUrl;
    }

    public AbstractBkApiClient(MeterRegistry meterRegistry,
                               String metricName,
                               String baseAccessUrl,
                               String lang) {
        this.meterRegistry = meterRegistry;
        this.metricName = metricName;
        this.baseAccessUrl = baseAccessUrl;
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

    public <T, R> EsbResp<R> doRequest(ApiRequestInfo<T> requestInfo,
                                       TypeReference<EsbResp<R>> typeReference) {
        return doRequest(requestInfo, typeReference, defaultHttpHelper, null);
    }

    public <T, R> EsbResp<R> doRequest(ApiRequestInfo<T> requestInfo,
                                       TypeReference<EsbResp<R>> typeReference,
                                       ExtHttpHelper httpHelper) {
        return doRequest(requestInfo, typeReference, httpHelper, null);
    }

    public <T, R> EsbResp<R> doRequest(ApiRequestInfo<T> requestInfo,
                                       TypeReference<EsbResp<R>> typeReference,
                                       BkApiLogStrategy logStrategy) {
        return doRequest(requestInfo, typeReference, defaultHttpHelper, logStrategy);
    }

    public <T, R> EsbResp<R> doRequest(ApiRequestInfo<T> requestInfo,
                                       TypeReference<EsbResp<R>> typeReference,
                                       ExtHttpHelper httpHelper,
                                       BkApiLogStrategy logStrategy) {
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
                    log.info("[AbstractBkApiClient] Response|method={}|uri={}|success={}|costTime={}|resp={}",
                        httpMethod.name(), requestInfo.getUri(), apiContext.isSuccess(),
                        apiContext.getCostTime(), apiContext.getOriginResp());
                }
            }
        }
    }

    private <T, R> EsbResp<R> requestApiAndWrapResponse(ApiRequestInfo<T> requestInfo,
                                                        BkApiContext<T, R> apiContext,
                                                        TypeReference<EsbResp<R>> typeReference,
                                                        ExtHttpHelper httpHelper) {
        String uri = apiContext.getUri();
        EsbResp<R> esbResp;
        String respStr;
        String status = EsbMetricTags.VALUE_STATUS_OK;
        HttpMethodEnum httpMethod = requestInfo.getMethod();
        long start = System.currentTimeMillis();
        try {
            respStr = requestApi(requestInfo, httpHelper);
            apiContext.setOriginResp(respStr);

            if (StringUtils.isBlank(respStr)) {
                String errorMsg = "[AbstractBkApiClient] " + httpMethod.name() + " "
                    + uri + ", error: " + "Response is blank";
                log.error(errorMsg);
                status = EsbMetricTags.VALUE_STATUS_ERROR;
                throw new InternalException(errorMsg, ErrorCode.API_ERROR);
            }

            esbResp = jsonMapper.fromJson(respStr, typeReference);
            apiContext.setResp(esbResp);
            if (!esbResp.isSuccess()) {
                log.warn(
                    "[AbstractBkApiClient] fail:response code!=0" +
                        "|requestId={}|code={}|message={}|method={}|uri={}|reqStr={}|respStr={}",
                    esbResp.getRequestId(),
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
                        "|requestId={}|code={}|message={}|method={}|uri={}|reqStr={}|respStr={}",
                    esbResp.getRequestId(),
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
            long cost = System.currentTimeMillis() - start;
            apiContext.setCostTime(cost);
            if (meterRegistry != null) {
                meterRegistry.timer(metricName, buildMetricTags(uri, status))
                    .record(cost, TimeUnit.MILLISECONDS);
            }
        }
    }

    private Iterable<Tag> buildMetricTags(String uri, String status) {
        Tags tags = Tags.of(EsbMetricTags.KEY_API_NAME, uri).and(EsbMetricTags.KEY_STATUS, status);
        Collection<Tag> extraTags = getExtraMetricsTags();
        if (CollectionUtils.isNotEmpty(extraTags)) {
            extraTags.forEach(tags::and);
        }
        return tags;
    }

    private <T> String requestApi(ApiRequestInfo<T> requestInfo,
                                  ExtHttpHelper httpHelper) {
        String respStr = null;
        HttpMethodEnum httpMethod = requestInfo.getMethod();
        String url = buildApiUrl(requestInfo.buildFinalUri());
        switch (httpMethod) {
            case POST:
                respStr = postForString(url, requestInfo.getBody(),
                    requestInfo.getAuthorization(), httpHelper);
                break;
            case PUT:
                respStr = putForString(url, requestInfo.getBody(),
                    requestInfo.getAuthorization(), httpHelper);
                break;
            case GET:
                respStr = getForString(url, requestInfo.getAuthorization(), httpHelper);
                break;
            case DELETE:
                respStr = deleteForString(url, requestInfo.getAuthorization(), httpHelper);
                break;
            default:
                log.warn("Unimplemented http method: {}", httpMethod.name());
                break;
        }
        return respStr;
    }

    private <T> String postForString(String url,
                                     T body,
                                     BkApiAuthorization authorization,
                                     ExtHttpHelper httpHelper) {
        if (httpHelper == null) {
            httpHelper = defaultHttpHelper;
        }
        String responseBody;
        Header[] header = buildBkApiRequestHeaders(authorization);
        responseBody = httpHelper.post(url, jsonMapper.toJson(body), header);
        return responseBody;
    }

    private <T> String putForString(String url,
                                    T body,
                                    BkApiAuthorization authorization,
                                    ExtHttpHelper httpHelper) {
        if (httpHelper == null) {
            httpHelper = defaultHttpHelper;
        }
        String responseBody;
        Header[] header = buildBkApiRequestHeaders(authorization);
        responseBody = httpHelper.put(url, "UTF-8", jsonMapper.toJson(body), Arrays.asList(header));
        return responseBody;
    }

    private String getForString(String url,
                                BkApiAuthorization authorization,
                                ExtHttpHelper httpHelper) {
        if (httpHelper == null) {
            httpHelper = defaultHttpHelper;
        }
        Header[] header = buildBkApiRequestHeaders(authorization);
        return httpHelper.get(url, header);
    }

    private String deleteForString(String url,
                                   BkApiAuthorization authorization,
                                   ExtHttpHelper httpHelper) {
        if (httpHelper == null) {
            httpHelper = defaultHttpHelper;
        }
        Header[] header = buildBkApiRequestHeaders(authorization);
        return httpHelper.delete(url, Arrays.asList(header));
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
