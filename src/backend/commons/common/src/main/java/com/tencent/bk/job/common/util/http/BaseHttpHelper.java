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
import com.tencent.bk.job.common.exception.InternalException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

@Slf4j
public class BaseHttpHelper implements HttpHelper {
    private final String CHARSET = "UTF-8";

    private final CloseableHttpClient httpClient;

    protected BaseHttpHelper(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public CloseableHttpResponse getRawResp(boolean keepAlive, String url, Header[] header) {
        HttpGet get = new HttpGet(url);
        if (keepAlive) {
            get.setHeader("Connection", "Keep-Alive");
        }
        if (header != null && header.length > 0) {
            get.setHeaders(header);
        }
        try {
            return getHttpClient().execute(get);
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
    public Pair<Integer, String> get(boolean keepAlive, String url, Header[] header) {
        HttpGet get = new HttpGet(url);
        if (keepAlive) {
            get.setHeader("Connection", "Keep-Alive");
        }
        if (header != null && header.length > 0) {
            get.setHeaders(header);
        }
        int httpStatusCode = -1;
        String respStr = null;
        try (CloseableHttpResponse response = getHttpClient().execute(get)) {
            httpStatusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            respStr = EntityUtils.toString(entity, CHARSET);
            return Pair.of(httpStatusCode, respStr);
        } catch (IOException e) {
            log.error("Get request fail", e);
            throw new InternalException(e, ErrorCode.API_ERROR);
        } finally {
            if (log.isDebugEnabled()) {
                log.debug(
                    "get:keepAlive={},url={},headers={},httpStatusCode={},respStr={}",
                    keepAlive,
                    url,
                    header,
                    httpStatusCode,
                    respStr
                );
            }
        }
    }

    @Override
    public Pair<Integer, byte[]> post(String url, HttpEntity requestEntity, Header... headers) {
        HttpPost post = new HttpPost(url);
        // 设置为长连接，服务端判断有此参数就不关闭连接。
        post.setHeader("Connection", "Keep-Alive");
        post.setHeaders(headers);
        post.setEntity(requestEntity);
        int httpStatusCode = -1;
        String respStr = null;
        try (CloseableHttpResponse httpResponse = getHttpClient().execute(post)) {
            httpStatusCode = httpResponse.getStatusLine().getStatusCode();
            if (httpStatusCode != HttpStatus.SC_OK) {
                String message = httpResponse.getStatusLine().getReasonPhrase();
                HttpEntity entity = httpResponse.getEntity();
                if (entity != null && entity.getContent() != null) {
                    respStr = new String(EntityUtils.toByteArray(entity), CHARSET);
                }
                log.warn(
                    "Post request fail, httpStatusCode={}, errorReason={}, body={}, url={}, headers={}",
                    httpStatusCode,
                    message,
                    respStr,
                    url,
                    headers
                );
                throw new InternalException(message, ErrorCode.API_ERROR);
            }
            HttpEntity entity = httpResponse.getEntity();
            return Pair.of(httpStatusCode, EntityUtils.toByteArray(entity));
        } catch (IOException e) {
            log.error("Post request fail", e);
            throw new InternalException(e, ErrorCode.API_ERROR);
        } finally {
            if (log.isDebugEnabled()) {
                log.debug(
                    "post:url={},headers={},requestEntity={},httpStatusCode={},respStr={}",
                    url,
                    headers,
                    requestEntity,
                    httpStatusCode,
                    respStr
                );
            }
        }
    }

    @Override
    public Pair<Integer, String> put(String url, HttpEntity requestEntity, Header... headers) {
        HttpPut put = new HttpPut(url);
        // 设置为长连接，服务端判断有此参数就不关闭连接。
        put.setHeader("Connection", "Keep-Alive");
        put.setHeaders(headers);
        put.setEntity(requestEntity);
        int httpStatusCode = -1;
        String respStr = null;
        try (CloseableHttpResponse httpResponse = getHttpClient().execute(put)) {
            httpStatusCode = httpResponse.getStatusLine().getStatusCode();
            HttpEntity entity = httpResponse.getEntity();
            respStr = new String(EntityUtils.toByteArray(entity), CHARSET);
            if (httpStatusCode != HttpStatus.SC_OK) {
                String message = httpResponse.getStatusLine().getReasonPhrase();
                log.warn(
                    "Put request fail, httpStatusCode={}, errorReason={}, respStr={}",
                    httpStatusCode,
                    message,
                    respStr
                );
                throw new InternalException(message, ErrorCode.API_ERROR);
            }
            return Pair.of(httpStatusCode, respStr);
        } catch (IOException e) {
            log.error("Put request fail", e);
            throw new InternalException(e, ErrorCode.API_ERROR);
        } finally {
            if (log.isDebugEnabled()) {
                log.debug(
                    "put:url={},headers={},requestEntity={},httpStatusCode={},respStr={}",
                    url,
                    headers,
                    requestEntity,
                    httpStatusCode,
                    respStr
                );
            }
        }
    }

    @Override
    public Pair<Integer, String> delete(String url, String content, Header... headers) {
        FakeHttpDelete delete = new FakeHttpDelete(url);
        if (content != null) {
            HttpEntity requestEntity;
            try {
                requestEntity = new ByteArrayEntity(content.getBytes(CHARSET));
            } catch (UnsupportedEncodingException e) {
                log.error("Fail to get ByteArrayEntity", e);
                throw new InternalException(e, ErrorCode.INTERNAL_ERROR);
            }
            delete.setEntity(requestEntity);
        }
        delete.setHeaders(headers);
        int httpStatusCode = -1;
        String respStr = null;
        try (CloseableHttpResponse httpResponse = getHttpClient().execute(delete)) {
            httpStatusCode = httpResponse.getStatusLine().getStatusCode();
            if (httpStatusCode != HttpStatus.SC_OK) {
                String message = httpResponse.getStatusLine().getReasonPhrase();
                log.info("Delete request fail, url={}, httpStatusCode={}, errorReason={}", url, httpStatusCode,
                    message);
                throw new InternalException(String.format("url=%s,httpStatusCode=%s" +
                    "，message=%s", url, httpStatusCode, message), ErrorCode.API_ERROR);
            }
            HttpEntity entity = httpResponse.getEntity();
            byte[] respBytes = EntityUtils.toByteArray(entity);
            if (respBytes == null) {
                return null;
            }
            respStr = new String(respBytes, CHARSET);
            return Pair.of(httpStatusCode, respStr);
        } catch (IOException e) {
            log.error("Delete request fail", e);
            throw new InternalException(e, ErrorCode.API_ERROR);
        } finally {
            if (log.isDebugEnabled()) {
                log.debug(
                    "delete:url={},headers={},body={},httpStatusCode={},respStr={}",
                    url,
                    headers,
                    content,
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
