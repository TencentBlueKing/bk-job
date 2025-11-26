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

package com.tencent.bk.job.gateway.web.server.config;

import com.tencent.bk.job.gateway.web.server.AccessLogFieldRegistry;
import com.tencent.bk.job.gateway.web.server.AccessLogFormatter;
import com.tencent.bk.job.gateway.web.server.AccessLogMetadataCollector;
import com.tencent.bk.job.gateway.web.server.NettyAccessLogCustomizer;
import com.tencent.bk.job.gateway.web.server.filter.RouteServerContextFilter;
import com.tencent.bk.job.gateway.web.server.provider.AccessLogMetadataProvider;
import com.tencent.bk.job.gateway.web.server.provider.DefaultMetadataProvider;
import com.tencent.bk.job.gateway.web.server.provider.GatewayContextMetadataProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConditionalOnProperty(
    value = "job.gateway.customAccessLog.enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class CustomAccessLogConfig {
    @Bean
    public RouteServerContextFilter routeServerContextFilter() {
        return new RouteServerContextFilter();
    }

    @Bean
    public DefaultMetadataProvider defaultMetadataProvider() {
        return new DefaultMetadataProvider();
    }

    @Bean
    public GatewayContextMetadataProvider jobContextMetadataProvider() {
        return new GatewayContextMetadataProvider();
    }

    @Bean
    public AccessLogFieldRegistry accessLogFieldRegistry() {
        return new AccessLogFieldRegistry();
    }

    @Bean
    public AccessLogFormatter accessLogFormatter(AccessLogFieldRegistry registry) {
        return new AccessLogFormatter(registry);
    }

    @Bean
    public AccessLogMetadataCollector accessLogMetadataCollector(List<AccessLogMetadataProvider> providers) {
        return new AccessLogMetadataCollector(providers);
    }

    @Bean
    public NettyAccessLogCustomizer nettyAccessLogCustomizer(AccessLogMetadataCollector collector,
                                                               AccessLogFormatter formatter) {
        return new NettyAccessLogCustomizer(collector, formatter);
    }
}
