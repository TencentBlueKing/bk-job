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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

/**
 * 蓝鲸API调用客户端 - for BK API GATEWAY
 */
@Slf4j
public abstract class AbstractBkApiClient {

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

    protected <T> String doPostAndGetRespStr(String uri, T body) {
        return doPostAndGetRespStr(uri, body, null);
    }

    protected <T> String doPostAndGetRespStr(String uri, T body, ExtHttpHelper httpHelper) {

        if (httpHelper == null) {
            httpHelper = defaultHttpHelper;
        }
        boolean error = false;
        long start = System.currentTimeMillis();
        String responseBody = null;
        try {
            String url;
            if (!bkApiGatewayUrl.endsWith("/") && !uri.startsWith("/")) {
                url = bkApiGatewayUrl + "/" + uri;
            } else {
                url = bkApiGatewayUrl + uri;
            }

            Header[] header = buildBkApiRequestHeaders();

            responseBody = httpHelper.post(url, "UTF-8", buildPostBody(body), header);
            return responseBody;
        } catch (Exception e) {
            log.error("Post url {}| params={}| exception={}", uri, JsonUtils.toJsonWithoutSkippedFields(body),
                e.getMessage());
            error = true;
            throw e;
        } finally {
            log.info("Post url {}| error={}| params={}| time={}| resp={}", uri, error,
                JsonUtils.toJsonWithoutSkippedFields(body), (System.currentTimeMillis() - start), responseBody);
        }
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
        return doHttpPost(uri, reqBody, typeReference, null);
    }

    public <T, R> EsbResp<R> doHttpPost(String uri,
                                        T reqBody,
                                        TypeReference<EsbResp<R>> typeReference,
                                        ExtHttpHelper httpHelper) {
        String respStr = null;
        try {
            respStr = doPostAndGetRespStr(uri, reqBody, httpHelper);
            if (StringUtils.isBlank(respStr)) {
                String errorMsg = "Post " + uri + ", error: " + "Response is blank";
                log.error(errorMsg);
                throw new InternalException(errorMsg, ErrorCode.API_ERROR);
            } else {
                log.debug("Success|method={}|uri={}|reqStr={}|respStr={}", "POST", uri, reqBody, respStr);
            }
            EsbResp<R> esbResp = JsonUtils.fromJson(respStr, typeReference);
            if (esbResp == null) {
                String errorMsg = "Post " + uri + ", error: " + "Response is blank after parse";
                log.error(errorMsg);
                throw new InternalException(errorMsg, ErrorCode.API_ERROR);
            }

            if (esbResp.getData() == null) {
                log.warn(
                    "Request bk api resp data is null|code: {}, message: {}, method: {}, uri: {}, req: {}, resp: {}",
                    esbResp.getCode(), esbResp.getMessage(), "Post", uri, reqBody, respStr
                );
            }
            return esbResp;
        } catch (Throwable e) {
            String errorMsg = "Fail to request api|method=" + "POST"
                + "|uri=" + uri
                + "|reqStr=" + reqBody
                + "|respStr=" + respStr;
            log.error(errorMsg, e);
            throw new InternalException("Fail to request bk api", e, ErrorCode.API_ERROR);
        }
    }
}
