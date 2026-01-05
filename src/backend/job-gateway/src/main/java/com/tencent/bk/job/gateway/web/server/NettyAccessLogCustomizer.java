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

package com.tencent.bk.job.gateway.web.server;

import com.tencent.bk.job.gateway.web.server.utils.AccessLogValueSafeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import reactor.netty.http.server.logging.AccessLog;
import reactor.netty.http.server.logging.AccessLogArgProvider;
import reactor.netty.http.server.logging.AccessLogFactory;

import java.util.Map;

/**
 * 自定义Netty服务的访问日志
 */
@Slf4j
public class NettyAccessLogCustomizer implements NettyFactoryCustomizer {
    private final AccessLogMetadataCollector collector;
    private final AccessLogFormatter formatter;

    @Autowired
    public NettyAccessLogCustomizer(AccessLogMetadataCollector collector,
                                    AccessLogFormatter formatter) {
        this.collector = collector;
        this.formatter = formatter;
    }

    @Override
    public void customize(NettyReactiveWebServerFactory factory) {
        log.info("Registering Netty AccessLog customizer.");
        factory.addServerCustomizers(httpServer -> {
            httpServer = httpServer.wiretap(false);
            return httpServer.accessLog(true,
                AccessLogFactory.createFilter(provider -> true,
                    provider -> {
                        try {
                            Map<String, Object> metadata = collector.collect(provider);
                            return AccessLog.create(formatter.format(metadata));
                        } catch (Exception e) {
                            log.error("Failed to build AccessLog.", e);
                            //默认AccessLog,如果返回null会被框架忽略没有日志
                            return createDefaultAccessLog(provider);
                        }
                    }));
        });
    }

    private AccessLog createDefaultAccessLog(AccessLogArgProvider provider) {
        return AccessLog.create(AccessLogConstants.Format.DEFAULT_LOG,
            AccessLogValueSafeUtil.clientIP(provider.connectionInformation()),
            provider.user(),
            AccessLogValueSafeUtil.dateTime(provider.accessDateTime(), AccessLogConstants.Format.DEFAULT_TIME),
            provider.method(),
            provider.uri(),
            provider.protocol(),
            provider.status(),
            provider.contentLength() > -1 ? provider.contentLength() : AccessLogConstants.Default.MISSING,
            provider.duration()
        );
    }
}
