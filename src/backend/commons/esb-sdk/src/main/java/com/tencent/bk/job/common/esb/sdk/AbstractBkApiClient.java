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
import com.tencent.bk.job.common.esb.model.BkApiAuthorization;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.util.http.ExtHttpHelper;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import com.tencent.bk.job.common.util.json.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

/**
 * 蓝鲸API调用客户端 - for BK API Gateway
 */
public abstract class AbstractBkApiClient {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final String bkApiGatewayUrl;
    private final String appSecret;
    private final String appCode;
    private final ExtHttpHelper defaultHttpHelper = HttpHelperFactory.getDefaultHttpHelper();

    public AbstractBkApiClient(String bkApiGatewayUrl,
                               String appCode,
                               String appSecret) {
        this.bkApiGatewayUrl = bkApiGatewayUrl;
        this.appCode = appCode;
        this.appSecret = appSecret;
    }

    private <T> String postForString(String uri, T body, ExtHttpHelper httpHelper) {

        if (httpHelper == null) {
            httpHelper = defaultHttpHelper;
        }
        String responseBody;
        String url;
        if (!bkApiGatewayUrl.endsWith("/") && !uri.startsWith("/")) {
            url = bkApiGatewayUrl + "/" + uri;
        } else {
            url = bkApiGatewayUrl + uri;
        }
        Header[] header = buildBkApiRequestHeaders();
        responseBody = httpHelper.post(url, "UTF-8", buildPostBody(body), header);
        return responseBody;
    }

    private Header[] buildBkApiRequestHeaders() {
        Header[] header = new Header[2];
        header[0] = new BasicHeader("Content-Type", "application/json");
        header[1] = buildBkApiAuthorizationHeader();
        return header;
    }

    private Header buildBkApiAuthorizationHeader() {
        BkApiAuthorization authorization = new BkApiAuthorization(appCode, appSecret);
        return new BasicHeader("X-Bkapi-Authorization", JsonUtils.toJson(authorization));
    }

    protected <T> String buildPostBody(T params) {
        return JsonUtils.toJson(params);
    }


    public <T, R> EsbResp<R> doHttpPost(String uri,
                                        T reqBody,
                                        TypeReference<EsbResp<R>> typeReference) {
        return doHttpPost(uri, reqBody, typeReference, null, null);
    }

    public <T, R> EsbResp<R> doHttpPost(String uri,
                                        T reqBody,
                                        TypeReference<EsbResp<R>> typeReference,
                                        ExtHttpHelper httpHelper,
                                        BkApiLogStrategy logStrategy) {
        String reqStr = JsonUtils.toJsonWithoutSkippedFields(reqBody);
        long startTime = System.currentTimeMillis();
        BkApiContext<T, R> apiContext
            = new BkApiContext<>(HttpMethod.POST.name(), uri, reqBody, null, null, 0, false);

        if (logStrategy != null) {
            logStrategy.logReq(log, apiContext);
        } else {
            if (log.isInfoEnabled()) {
                log.info("[AbstractBkApiClient] Request|method={}|uri={}|reqStr={}", HttpMethod.POST.name(), uri,
                    reqStr);
            }
        }

        try {
            return requestApiAndWrapResponse(HttpMethod.POST, apiContext, typeReference, httpHelper);
        } finally {
            apiContext.setCostTime(System.currentTimeMillis() - startTime);
            if (logStrategy != null) {
                logStrategy.logResp(log, apiContext);
            } else {
                if (log.isInfoEnabled()) {
                    log.info("[AbstractBkApiClient] Response|method={}|uri={}|success={}|costTime={}|resp={}",
                        HttpMethod.POST.name(), uri, apiContext.isSuccess(), apiContext.getCostTime(),
                        apiContext.getOriginResp());
                }
            }
        }
    }

    private <T, R> EsbResp<R> requestApiAndWrapResponse(HttpMethod httpMethod,
                                                        BkApiContext<T, R> apiContext,
                                                        TypeReference<EsbResp<R>> typeReference,
                                                        ExtHttpHelper httpHelper) {
        String uri = apiContext.getUri();
        T reqBody = apiContext.getReq();
        String reqStr = JsonUtils.toJsonWithoutSkippedFields(apiContext.getReq());
        EsbResp<R> esbResp;
        String respStr = null;
        try {
            respStr = requestApi(httpMethod, uri, reqBody, httpHelper);
            apiContext.setOriginResp(respStr);

            if (StringUtils.isBlank(respStr)) {
                String errorMsg = "[AbstractBkApiClient] " + httpMethod.name() + " "
                    + uri + ", error: " + "Response is blank";
                log.error(errorMsg);
                throw new InternalException(errorMsg, ErrorCode.API_ERROR);
            }

            esbResp = JsonUtils.fromJson(respStr, typeReference);
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
                    reqStr,
                    respStr
                );
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
                    reqStr,
                    respStr
                );
            }
            apiContext.setSuccess(true);
            return esbResp;
        } catch (Throwable e) {
            String errorMsg = "Fail to request api|method=" + httpMethod.name()
                + "|uri=" + uri
                + "|reqStr=" + reqStr
                + "|respStr=" + respStr;
            log.error(errorMsg, e);
            apiContext.setSuccess(false);
            throw new InternalException("Fail to request bk api", e, ErrorCode.API_ERROR);
        }
    }

    private <T> String requestApi(HttpMethod httpMethod,
                                  String uri,
                                  T reqBody,
                                  ExtHttpHelper httpHelper) {
        String respStr = null;
        switch (httpMethod) {
            case POST:
                respStr = postForString(uri, reqBody, httpHelper);
                break;
            default:
                log.warn("Unimplemented http method: {}", httpMethod.name());
                break;
        }
        return respStr;
    }

    public <T, R> EsbResp<R> doHttpPost(String uri,
                                        T reqBody,
                                        TypeReference<EsbResp<R>> typeReference,
                                        ExtHttpHelper httpHelper) {
        return doHttpPost(uri, reqBody, typeReference, httpHelper, null);
    }

}
