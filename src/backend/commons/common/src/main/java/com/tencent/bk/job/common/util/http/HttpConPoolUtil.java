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
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * HTTP请求工具,包装了HTTPClient连接池
 */
@Slf4j
public class HttpConPoolUtil {

    private static final String CHARSET = "UTF-8";

    private final static CloseableHttpClient HTTP_CLIENT;

    static {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
            .setDefaultConnectionConfig(
                ConnectionConfig.custom().setBufferSize(102400).setCharset(Charset.forName(CHARSET)).build())
            .setDefaultRequestConfig(RequestConfig.custom().setConnectionRequestTimeout(15000).setConnectTimeout(15000)
                .setSocketTimeout(15000).build())
            // esb的keep-alive时间为90s，需要<90s,防止连接超时抛出org.apache.http.NoHttpResponseException: The target server failed
            // to respond
            .setConnectionTimeToLive(80, TimeUnit.SECONDS).evictExpiredConnections()
            .evictIdleConnections(60, TimeUnit.SECONDS).disableAutomaticRetries().disableAuthCaching()
            .disableCookieManagement().setMaxConnPerRoute(500).setMaxConnTotal(1000);

        CloseableHttpClient tmp;
        try {
            tmp = httpClientBuilder.setSSLSocketFactory(new SSLConnectionSocketFactory(
                SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build())).build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            log.error("Init http client error", e);
            tmp = httpClientBuilder.build();
        }
        HTTP_CLIENT = tmp;
    }

    /**
     * 提交POST请求，并返回数据，用encoding参数解析
     *
     * @param url         提交的地址
     * @param content     提交的内容字符串
     * @param contentType 默认传null则为"application/x-www-form-urlencoded"
     * @return 响应
     */
    public static String post(String url, String content, String contentType) {
        return post(url, CHARSET, content, contentType);
    }

    /**
     * 提交POST请求，并返回数据，用encoding参数解析
     *
     * @param url         提交的地址
     * @param charset     字符集，用于解析返回的字符串
     * @param content     提交的内容字符串
     * @param contentType 默认传null则为"application/x-www-form-urlencoded"
     * @return 响应
     */
    public static String post(String url, String charset, String content, String contentType) {
        try {
            byte[] resp = post(url, content.getBytes(charset), contentType);
            if (null == resp) {
                return null;
            }
            return new String(resp, charset);
        } catch (IOException e) {
            log.error("Post request fail", e);
            throw new InternalException(e, ErrorCode.API_ERROR);
        }
    }

    /**
     * 提交POST请求，并返回数据（默认采用encoding常量指定的字符集解析）
     *
     * @param url     提交的地址
     * @param content 提交的内容字符串
     * @return 响应
     */
    public static String post(String url, String content) {
        return post(url, CHARSET, content, "application/x-www-form-urlencoded");
    }

    /**
     * 支持自定义头的POST请求
     *
     * @param url     提交的地址
     * @param content 提交的内容字符串
     * @param headers 自定义请求头
     * @return 响应
     */
    public static String post(String url, String content, Header... headers) {
        return post(url, CHARSET, content, headers);
    }

    /**
     * 支持自定义头的POST请求
     *
     * @param url     提交的地址
     * @param charset 字符集，用于解析返回的字符串
     * @param content 提交的内容字符串
     * @param headers 自定义请求头
     * @return 响应
     */
    public static String post(String url, String charset, String content, Header... headers) {
        try {
            byte[] resp = post(url, new ByteArrayEntity(content.getBytes(charset)), headers);
            if (null == resp) {
                return null;
            }
            return new String(resp, charset);
        } catch (IOException e) {
            log.error("Post request fail", e);
            throw new InternalException(e, ErrorCode.API_ERROR);
        }
    }

    /**
     * 提交POST请求，并返回字节数组
     *
     * @param url         提交的地址
     * @param content     提交的内容字节数据
     * @param contentType 默认传null则为"application/x-www-form-urlencoded"
     * @return 返回字节数组
     */
    public static byte[] post(String url, byte[] content, String contentType) {
        return post(url, new ByteArrayEntity(content), contentType);
    }

    /**
     * 提交POST请求，并返回字节数组
     *
     * @param url           提交的地址
     * @param requestEntity 封装好的请求实体
     * @param contentType   默认传null则为"application/x-www-form-urlencoded"
     * @return 返回字节数组
     */
    public static byte[] post(String url, HttpEntity requestEntity, String contentType) {
        return post(url, requestEntity,
            new BasicHeader("Content-Type", contentType == null ? "application/x-www-form-urlencoded" : contentType));
    }

    public static byte[] post(String url, HttpEntity requestEntity, Header... headers) {
        HttpPost post = new HttpPost(url);
        // 设置为长连接，服务端判断有此参数就不关闭连接。
        post.setHeader("Connection", "Keep-Alive");
        post.setHeaders(headers);
        post.setEntity(requestEntity);
        try (CloseableHttpResponse httpResponse = HTTP_CLIENT.execute(post)) {
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            log.info("Post url: {}, statusCode: {}", url, statusCode);
            if (statusCode != HttpStatus.SC_OK) {
                String errorMsg = buildErrorMessage("Post", url, statusCode,
                    httpResponse.getStatusLine().getReasonPhrase());
                log.error(errorMsg);
                throw new InternalException(ErrorCode.API_ERROR, errorMsg);
            }
            HttpEntity entity = httpResponse.getEntity();
            return EntityUtils.toByteArray(entity);
        } catch (IOException e) {
            log.error("Post request fail", e);
            throw new InternalException(e, ErrorCode.API_ERROR);
        }
    }

    private static String buildErrorMessage(String method, String url, int statusCode, String message) {
        return method + " " + url + ", statusCode=" + statusCode + ", error=" + message;
    }

    /**
     * GET请求
     *
     * @param url 提交的地址
     * @return 响应
     */
    public static String get(String url) {
        return get(url, null);
    }

    public static String get(String url, Header[] header) {
        return get(true, url, header);
    }

    public static String get(boolean keepAlive, String url, Header[] header) {
        HttpGet get = new HttpGet(url);
        if (keepAlive) {
            get.setHeader("Connection", "Keep-Alive");
        }
        if (header != null && header.length > 0) {
            get.setHeaders(header);
        }
        try (CloseableHttpResponse response = HTTP_CLIENT.execute(get)) {
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity, CHARSET);
        } catch (IOException e) {
            log.error("Get request fail", e);
            throw new InternalException(e, ErrorCode.API_ERROR);
        }
    }

    public static String delete(String url, String content, Header... headers) {
        FakeHttpDelete delete = new FakeHttpDelete(url);
        try (CloseableHttpResponse httpResponse = HTTP_CLIENT.execute(delete)) {
            HttpEntity requestEntity = new ByteArrayEntity(content.getBytes(CHARSET));
            delete.setEntity(requestEntity);
            delete.setHeaders(headers);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                String errorMsg = buildErrorMessage("Delete", url, statusCode,
                    httpResponse.getStatusLine().getReasonPhrase());
                log.error(errorMsg);
                throw new InternalException(ErrorCode.API_ERROR, errorMsg);
            }
            HttpEntity entity = httpResponse.getEntity();
            byte[] respBytes = EntityUtils.toByteArray(entity);
            if (respBytes == null) {
                return null;
            }
            return new String(EntityUtils.toByteArray(entity), CHARSET);
        } catch (IOException e) {
            log.error("Delete request fail", e);
            throw new InternalException(e, ErrorCode.API_ERROR);
        }
    }

    private static class FakeHttpDelete extends HttpPost {
        public FakeHttpDelete(String url) {
            super(url);
        }

        @Override
        public String getMethod() {
            return "DELETE";
        }
    }
}
