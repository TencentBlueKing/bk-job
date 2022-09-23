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
import org.apache.http.HttpClientConnection;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.routing.HttpRoute;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 可观测连接租用失败时连接池状态的ConnectionRequest实现类，具体连接的获取通过内部代理对象实现
 */
@Slf4j
public class WatchableConnectionRequestProxy implements ConnectionRequest {
    private final ConnectionRequest delegate;
    private final WatchablePoolingHttpClientConnectionManager connectionManager;

    public WatchableConnectionRequestProxy(ConnectionRequest delegate,
                                           WatchablePoolingHttpClientConnectionManager connectionManager) {
        this.delegate = delegate;
        this.connectionManager = connectionManager;
    }

    private void tryToLogConnectionStatus() {
        try {
            logConnectionStatus();
        } catch (Throwable t) {
            log.warn("Fail to logConnectionStatus", t);
        }
    }

    private void logConnectionStatus() {
        // 连接池整体状态统计数据
        for (HttpRoute route : connectionManager.getRoutes()) {
            log.info(
                "http conn pool:{}:{}",
                route,
                connectionManager.getStats(route)
            );
        }
        // 被占用的连接具体信息
        connectionManager.enumLeasedConnections(entry -> {
            HttpRoute route = entry.getRoute();
            log.info(
                "leased:route={}:connectionId={}",
                route,
                entry.getConnection().getId()
            );
        });
    }

    @Override
    public HttpClientConnection get(long timeout,
                                    TimeUnit timeUnit) throws InterruptedException, ExecutionException,
        ConnectionPoolTimeoutException {
        try {
            HttpClientConnection connection = delegate.get(timeout, timeUnit);
            if (connection instanceof ManagedHttpClientConnection) {
                ManagedHttpClientConnection manageConnection = (ManagedHttpClientConnection) connection;
                log.info("get connection success:id={}", manageConnection.getId());
            }
            return connection;
        } catch (Exception e) {
            tryToLogConnectionStatus();
            throw e;
        }
    }

    @Override
    public boolean cancel() {
        return delegate.cancel();
    }
}
