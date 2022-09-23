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

import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

/**
 * 用于创建自定义Http连接池的工厂类
 */
public class JobHttpClientConnectionManagerFactory {

    private static final String CHARSET = "UTF-8";
    private static final ConnectionConfig defaultConnectionConfig = ConnectionConfig.custom()
        .setBufferSize(102400)
        .setCharset(Charset.forName(CHARSET))
        .build();

    /**
     * 创建可对连接租用失败时的状态进行观测的连接池
     *
     * @param maxConnPerRoute  单个Route最大连接数
     * @param maxConnTotal     最大总连接数量
     * @param sslSocketFactory SSLSocket创建工厂
     * @return 连接池
     */
    public static HttpClientConnectionManager createWatchableConnectionManager(
        int maxConnPerRoute,
        int maxConnTotal,
        LayeredConnectionSocketFactory sslSocketFactory
    ) {
        // esb的keep-alive时间为90s，需要<90s,防止连接超时抛出org.apache.http.NoHttpResponseException:
        // The target server failed to respond
        // 因此，timeToLive使用34s
        final PoolingHttpClientConnectionManager poolingmgr =
            new WatchablePoolingHttpClientConnectionManager(
                RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslSocketFactory)
                    .build(),
                new WatchableManagedHttpClientConnectionFactory(),
                null,
                null,
                34,
                TimeUnit.SECONDS
            );
        if (defaultConnectionConfig != null) {
            poolingmgr.setDefaultConnectionConfig(defaultConnectionConfig);
        }
        if (maxConnTotal > 0) {
            poolingmgr.setMaxTotal(maxConnTotal);
        }
        if (maxConnPerRoute > 0) {
            poolingmgr.setDefaultMaxPerRoute(maxConnPerRoute);
        }
        return poolingmgr;
    }
}
