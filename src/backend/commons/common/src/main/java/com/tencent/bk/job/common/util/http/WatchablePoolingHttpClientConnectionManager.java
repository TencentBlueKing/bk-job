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

import org.apache.http.config.Registry;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.HttpConnectionFactory;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.impl.conn.DefaultHttpClientConnectionOperator;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.pool.PoolEntryCallback;

import java.util.concurrent.TimeUnit;

/**
 * 可对连接租用失败时连接池状态进行观测的连接池扩展类
 * 开放enumLeased方法，重写requestConnection用于生成可观测的连接请求
 */
public class WatchablePoolingHttpClientConnectionManager extends PoolingHttpClientConnectionManager {

    public WatchablePoolingHttpClientConnectionManager(final Registry<ConnectionSocketFactory> socketFactoryRegistry,
                                                       final HttpConnectionFactory<HttpRoute,
                                                           ManagedHttpClientConnection> connFactory,
                                                       final SchemePortResolver schemePortResolver,
                                                       final DnsResolver dnsResolver,
                                                       final long timeToLive, final TimeUnit timeUnit) {
        super(
            new DefaultHttpClientConnectionOperator(socketFactoryRegistry, schemePortResolver, dnsResolver),
            connFactory,
            timeToLive,
            timeUnit
        );
    }

    public void enumLeasedConnections(PoolEntryCallback<HttpRoute, ManagedHttpClientConnection> callback) {
        super.enumLeased(callback);
    }

    @Override
    public ConnectionRequest requestConnection(final HttpRoute route,
                                               final Object state) {
        return new WatchableConnectionRequestProxy(
            super.requestConnection(route, state),
            this
        );
    }
}
