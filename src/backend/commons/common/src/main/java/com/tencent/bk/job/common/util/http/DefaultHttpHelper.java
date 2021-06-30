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

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;

import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DefaultHttpHelper extends AbstractHttpHelper {
    private static final String CHARSET = "UTF-8";

    private static final CloseableHttpClient DEFAULT_HTTP_CLIENT;

    static {
        HttpClientBuilder longHttpClientBuilder = HttpClientBuilder.create()
            .setDefaultConnectionConfig(
                ConnectionConfig.custom().setBufferSize(102400).setCharset(Charset.forName(CHARSET)).build())
            .setDefaultRequestConfig(RequestConfig.custom().setConnectionRequestTimeout(15000).setConnectTimeout(15000)
                .setSocketTimeout(15000).build())
            // esb的keep-alive时间为90s，需要<90s,防止连接超时抛出org.apache.http.NoHttpResponseException: The target server failed to
            // respond
            .setConnectionTimeToLive(34, TimeUnit.SECONDS).evictExpiredConnections()
            .evictIdleConnections(5, TimeUnit.SECONDS).disableAutomaticRetries().disableAuthCaching()
            .disableCookieManagement().setMaxConnPerRoute(500).setMaxConnTotal(1000);
        CloseableHttpClient tmp;
        try {
            tmp = longHttpClientBuilder.setSSLSocketFactory(new SSLConnectionSocketFactory(
                SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build())).build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            log.error("", e);
            tmp = longHttpClientBuilder.build();
        }
        DEFAULT_HTTP_CLIENT = tmp;
    }

    @Override
    CloseableHttpClient getHttpClient() {
        return DEFAULT_HTTP_CLIENT;
    }
}
