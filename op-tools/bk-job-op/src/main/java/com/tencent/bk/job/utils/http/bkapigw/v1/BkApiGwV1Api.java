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

package com.tencent.bk.job.utils.http.bkapigw.v1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.utils.http.HttpMethodEnum;
import com.tencent.bk.job.utils.http.api.BaseApi;
import com.tencent.bk.job.utils.json.JsonUtils;
import com.tencent.bk.job.utils.log.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
public class BkApiGwV1Api extends BaseApi {

    private final Authorization authorization;
    private final String baseUrl;

    public BkApiGwV1Api(RestTemplate restTemplate,
                        String bkAppCode,
                        String bkAppSecret,
                        String username,
                        String baseUrl) {
        super(restTemplate);
        authorization = Authorization.build(
            bkAppCode,
            bkAppSecret,
            username
        );
        this.baseUrl = baseUrl;
    }

    /**
     * 请求API
     * @param method HTTP方法
     * @param uri URI
     * @param params URL参数
     * @param body 请求体
     * @param typeReference 返回值类型
     * @param throwExIfStatusNotOk 状态非OK时是否抛出异常
     * @return 响应对象
     */
    public <T> ApiGwResp<T> requestApi(HttpMethodEnum method,
                                       String uri,
                                       Map<String, String> params,
                                       Object body,
                                       TypeReference<ApiGwResp<T>> typeReference,
                                       boolean throwExIfStatusNotOk) {

        String respStr = logAndRequestApiForStr(method, uri, params, body);
        ApiGwResp<T> resp = JsonUtils.fromJson(respStr, typeReference);
        if (throwExIfStatusNotOk) {
            checkRespAndThrow(uri, resp);
        }
        return resp;
    }

    public <T> ApiGwResp<T> requestApi(HttpMethodEnum method,
                                       String uri,
                                       Map<String, String> params,
                                       Object body,
                                       TypeReference<ApiGwResp<T>> typeReference) {

        return requestApi(method, uri, params, body, typeReference, true);
    }

    private <T> void checkRespAndThrow(String uri, ApiGwResp<T> resp) {
        if (resp.getCode() != 0) {
            String errMsg = String.format(
                "BkApiGwV1Api request error, uri=%s, message=%s",
                uri,
                JsonUtils.toJson(resp));
            throw new BkApiException(errMsg);
        }
    }

    protected String logAndRequestApiForStr(HttpMethodEnum method,
                                            String uri,
                                            Map<String, String> params,
                                            Object body) {
        String bodyJson = body == null ? "null" : JsonUtils.toJson(body);
        log.info(
            "[BkApiGwV1Api]do request, method={}, uri={}, params={}, body={}",
            method,
            uri,
            params,
            LogUtils.truncate(bodyJson));
        String respStr = null;
        String url = buildUrl(uri);
        switch (method) {
            case GET:
                respStr = doGetAndGetResponseStr(url, params, buildBkAuthHeader());
                break;
            case POST:
                respStr = doPostAndGetResponseStr(url, params, body, buildBkAuthHeader());
                break;
            default:
                // TODO: 更多HTTP方法
                log.error("[BkApiGwV1Api]do request error, unsupported method={}", method);
                throw new IllegalArgumentException("Unsupported http method: " + method);
        }
        log.info("[BkApiGwV1Api]response, method={}, uri={}, respStr={}", method, uri, LogUtils.truncate(respStr));
        return respStr;
    }

    protected HttpHeaders buildBkAuthHeader() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(Authorization.HEADER_KEY_AUTHORIZATION, JsonUtils.toJson(authorization));
        return httpHeaders;
    }

    protected String buildUrl(String uri) {
        StringBuilder finalUrlSb = new StringBuilder();
        if (baseUrl.endsWith("/")) {
            finalUrlSb.append(baseUrl);
        } else {
            finalUrlSb.append(baseUrl).append("/");
        }

        if (uri.startsWith("/")) {
            finalUrlSb.append(uri.substring(1));
        } else {
            finalUrlSb.append(uri);
        }

        return finalUrlSb.toString();
    }
}
