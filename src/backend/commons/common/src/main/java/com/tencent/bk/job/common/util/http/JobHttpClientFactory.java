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

package com.tencent.bk.job.common.util.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.ssl.SSLContexts;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * Job http client 工厂
 */
@Slf4j
public class JobHttpClientFactory {
    /**
     * 创建 HttpClient
     *
     * @param connRequestTimeout 从连接池获取到连接的超时时间，单位秒
     * @param connTimeout        建立连接的超时时间，单位秒
     * @param socketTimeout      客户端与服务端进行交互的时间，单位秒
     * @param maxConnPerRoute    每个路由的最大连接
     * @param maxConnTotal       连接池最大连接数
     * @param timeToLive         连接存活时间
     * @param retryHandler       请求重试Handler; 如果传入 null，表示不需要重试
     * @return HttpClient
     */
    public static CloseableHttpClient createHttpClient(int connRequestTimeout,
                                                       int connTimeout,
                                                       int socketTimeout,
                                                       int maxConnPerRoute,
                                                       int maxConnTotal,
                                                       int timeToLive,
                                                       boolean allowRetry,
                                                       HttpRequestRetryHandler retryHandler) {
        return createHttpClient(
            connRequestTimeout,
            connTimeout,
            socketTimeout,
            maxConnPerRoute,
            maxConnTotal,
            timeToLive,
            allowRetry,
            retryHandler,
            (httpClientBuilder) -> {
                // do nothing
            });
    }

    public static CloseableHttpClient createHttpClient(int connRequestTimeout,
                                                       int connTimeout,
                                                       int socketTimeout,
                                                       int maxConnPerRoute,
                                                       int maxConnTotal,
                                                       int timeToLive,
                                                       boolean allowRetry,
                                                       HttpRequestRetryHandler retryHandler,
                                                       HttpClientCustomizer customizer) {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
            .setDefaultRequestConfig(
                RequestConfig.custom()
                    .setConnectionRequestTimeout(connRequestTimeout)
                    .setConnectTimeout(connTimeout)
                    .setSocketTimeout(socketTimeout)
                    .build()
            )
            .evictExpiredConnections()
            .evictIdleConnections(5, TimeUnit.SECONDS)
            .disableAuthCaching()
            .disableCookieManagement();
        if (!allowRetry) {
            httpClientBuilder.disableAutomaticRetries();
        } else {
            if (retryHandler != null) {
                httpClientBuilder.setRetryHandler(retryHandler);
            } else {
                httpClientBuilder.setRetryHandler(new StandardHttpRequestRetryHandler());
            }
        }

        LayeredConnectionSocketFactory sslSocketFactory = null;
        try {
            sslSocketFactory = new SSLConnectionSocketFactory(
                SSLContexts.custom()
                    .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                    .build()
            );
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            log.error("Create HttpClient exception", e);
        }
        httpClientBuilder.setConnectionManager(
            JobHttpClientConnectionManagerFactory.createWatchableConnectionManager(
                maxConnPerRoute,
                maxConnTotal,
                timeToLive,
                TimeUnit.SECONDS,
                sslSocketFactory
            ));

        if (customizer != null) {
            customizer.customize(httpClientBuilder);
        }
        return httpClientBuilder.build();
    }

    public interface HttpClientCustomizer {
        void customize(HttpClientBuilder httpClientBuilder);
    }
}
