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

import com.tencent.bk.job.gateway.web.server.AccessLogFormatter;
import com.tencent.bk.job.gateway.web.server.AccessLogMetadataCollector;
import com.tencent.bk.job.gateway.web.server.GatewayWebServerFactoryCustomizer;
import com.tencent.bk.job.gateway.web.server.NettyAccessLogCustomizer;
import com.tencent.bk.job.gateway.web.server.NettyFactoryCustomizer;
import com.tencent.bk.job.gateway.web.server.WebServerRoleEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.List;

@Configuration
@ConditionalOnCustomAccessLogEnabled
@Slf4j
public class GatewayWebServerConfig {

    @Bean
    public GatewayWebServerFactoryCustomizer gatewayWebServerFactoryCustomizer(
        Environment environment,
        ServerProperties serverProperties,
        AccessLogMetadataCollector collector,
        AccessLogFormatter formatter
    ) {
        log.debug("Initializing gateway web server factory customizer.");
        List<NettyFactoryCustomizer> customizers = Arrays.asList(
            new NettyAccessLogCustomizer(collector, formatter, WebServerRoleEnum.BUSINESS)
        );
        log.debug("Gateway web server customizers count={}", customizers.size());
        return new GatewayWebServerFactoryCustomizer(
            environment,
            serverProperties,
            customizers
        );
    }
}
