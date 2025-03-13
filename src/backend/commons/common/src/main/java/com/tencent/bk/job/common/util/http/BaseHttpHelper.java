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

package com.tencent.bk.job.common.util.http;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.HttpMethodEnum;
import com.tencent.bk.job.common.exception.HttpStatusException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.NotImplementedException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.helpers.MessageFormatter;

import java.io.IOException;

@Slf4j
public class BaseHttpHelper implements HttpHelper {
    private final String CHARSET = "UTF-8";

    private final CloseableHttpClient httpClient;

    public BaseHttpHelper(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public Pair<HttpRequestBase, CloseableHttpResponse> getRawResp(boolean keepAlive, String url, Header[] header) {
        HttpGet get = new HttpGet(url);
        if (keepAlive) {
            get.setHeader("Connection", "Keep-Alive");
        }
        if (header != null && header.length > 0) {
            get.setHeaders(header);
        }
        try {
            return Pair.of(get, httpClient.execute(get));
        } catch (IOException e) {
            log.error("Get request fail", e);
            throw new InternalException(e, ErrorCode.API_ERROR);
        } finally {
            if (log.isDebugEnabled()) {
                log.debug("getRawResp,url={},headers={}", url, header);
            }
        }
    }

    @Override
    public HttpResponse requestForSuccessResp(HttpRequest request) throws HttpStatusException {
        return requestInternal(request, true);
    }

    private HttpResponse requestInternal(HttpRequest request,
                                         boolean throwExceptionWhenClientOrServerError) {
        HttpMethodEnum method = request.getMethod();
        HttpContext httpContext = buildHttpContext(request);
        HttpRequestBase httpClientRequest;
        switch (method) {
            case GET:
                httpClientRequest = buildHttpGet(request);
                break;
            case POST:
                httpClientRequest = buildHttpPost(request);
                break;
            case PUT:
                httpClientRequest = buildHttpPut(request);
                break;
            case DELETE:
                httpClientRequest = buildHttpDelete(request);
                break;
            default:
                log.warn("Unsupported http method : {}", method);
                throw new NotImplementedException(ErrorCode.API_ERROR);
        }
        return execute(httpClientRequest, httpContext, throwExceptionWhenClientOrServerError);
    }

    @Override
    public HttpResponse request(HttpRequest request) {
        return requestInternal(request, false);
    }

    private HttpGet buildHttpGet(HttpRequest request) {
        HttpGet get = new HttpGet(request.getUrl());
        setCommonHttpClientRequest(request, get);

        return get;
    }

    private void setCommonHttpClientRequest(HttpRequest request, HttpRequestBase httpClientRequest) {
        setConnectionKeepAlive(request, httpClientRequest);
        if (request.getHeaders() != null && request.getHeaders().length > 0) {
            httpClientRequest.setHeaders(request.getHeaders());
        }
    }

    private HttpPost buildHttpPost(HttpRequest request) {
        HttpPost post = new HttpPost(request.getUrl());
        setCommonHttpClientRequest(request, post);
        setEntity(post, request);
        return post;
    }

    private HttpPut buildHttpPut(HttpRequest request) {
        HttpPut put = new HttpPut(request.getUrl());
        setCommonHttpClientRequest(request, put);
        setEntity(put, request);
        return put;
    }

    private FakeHttpDelete buildHttpDelete(HttpRequest request) {
        FakeHttpDelete delete = new FakeHttpDelete(request.getUrl());
        setCommonHttpClientRequest(request, delete);
        setEntity(delete, request);
        return delete;
    }

    private void setEntity(HttpEntityEnclosingRequest request, HttpRequest httpRequest) {
        if (httpRequest.getHttpEntity() != null) {
            request.setEntity(httpRequest.getHttpEntity());
        } else if (StringUtils.isNotBlank(httpRequest.getStringEntity())) {
            try {
                request.setEntity(new ByteArrayEntity(httpRequest.getStringEntity().getBytes(CHARSET)));
            } catch (IOException e) {
                throw new InternalException(e, ErrorCode.API_ERROR);
            }
        }
    }

    private HttpContext buildHttpContext(HttpRequest request) {
        HttpCoreContext httpContext = HttpCoreContext.create();
        if (request.getRetryMode() != null) {
            httpContext.setAttribute(HttpContextAttributeNames.RETRY_MODE, request.getRetryMode().getValue());
        }
        if (request.getIdempotent() != null) {
            httpContext.setAttribute(HttpContextAttributeNames.IS_IDEMPOTENT, request.getIdempotent());
        }
        return httpContext;
    }

    private void setConnectionKeepAlive(HttpRequest request, HttpRequestBase httpRequestBase) {
        if (request.isKeepAlive()) {
            // 设置为长连接，服务端判断有此参数就不关闭连接。
            httpRequestBase.setHeader("Connection", "Keep-Alive");
        }
    }

    private HttpResponse execute(HttpRequestBase httpClientRequest,
                                 HttpContext context,
                                 boolean throwExceptionWhenClientOrServerError) {
        int httpStatusCode = -1;
        String respStr = null;
        Long contentLength = null;
        try (CloseableHttpResponse httpResponse = httpClient.execute(httpClientRequest, context)) {
            httpStatusCode = httpResponse.getStatusLine().getStatusCode();
            HttpEntity entity = httpResponse.getEntity();
            if (entity != null && entity.getContent() != null) {
                contentLength = entity.getContentLength();
                respStr = new String(EntityUtils.toByteArray(entity), CHARSET);
            }
            // 状态码>=400判定为失败
            if (httpStatusCode >= HttpStatus.SC_BAD_REQUEST) {
                String reasonPhrase = httpResponse.getStatusLine().getReasonPhrase();
                log.warn(
                    "Request fail, method: {}, url={}, httpStatusCode={}, errorReason={}, body={}",
                    httpClientRequest.getMethod(),
                    httpClientRequest.getURI().getPath(),
                    httpStatusCode,
                    reasonPhrase,
                    respStr
                );
                if (throwExceptionWhenClientOrServerError) {
                    throw new HttpStatusException(
                        httpClientRequest.getURI().toString(),
                        httpStatusCode,
                        reasonPhrase,
                        respStr
                    );
                } else {
                    return new HttpResponse(httpStatusCode, respStr, httpResponse.getAllHeaders());
                }
            } else {
                return new HttpResponse(httpStatusCode, respStr, httpResponse.getAllHeaders());
            }
        } catch (IOException e) {
            String message = MessageFormatter.format(
                "Request fail, httpStatusCode={}, contentLength={}",
                httpStatusCode,
                contentLength
            ).getMessage();
            log.error(message, e);
            throw new InternalException(e, ErrorCode.API_ERROR);
        } finally {
            httpClientRequest.releaseConnection();
            if (log.isDebugEnabled()) {
                log.debug(
                    "Request done, method: {}, url={}, httpStatusCode={}, respStr={}",
                    httpClientRequest.getMethod(),
                    httpClientRequest.getURI().getPath(),
                    httpStatusCode,
                    respStr
                );
            }
        }
    }

    private static class FakeHttpDelete extends HttpPost {
        FakeHttpDelete(String url) {
            super(url);
        }

        @Override
        public String getMethod() {
            return "DELETE";
        }
    }
}
