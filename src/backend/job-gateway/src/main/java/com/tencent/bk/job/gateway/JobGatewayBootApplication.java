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

package com.tencent.bk.job.gateway;

import com.tencent.bk.job.common.service.boot.JobBootApplication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.availability.ApplicationAvailabilityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.embedded.NettyWebServerFactoryCustomizer;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.http.server.reactive.HttpHandler;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Job Gateway Spring Boot Application
 */
@JobBootApplication(scanBasePackages = "com.tencent.bk.job.gateway",
    exclude = {ApplicationAvailabilityAutoConfiguration.class},
    excludeName = {"org.springframework.cloud.kubernetes.client.discovery.KubernetesDiscoveryClientAutoConfiguration"})
@Slf4j
@EnableFeignClients
public class JobGatewayBootApplication {
    private final HttpHandler httpHandler;

    private WebServer httpWebServer;

    private final NettyWebServerFactoryCustomizer nettyWebServerFactoryCustomizer;

    @Value("${server.http.enabled}")
    private Boolean httpEnabled;

    @Value("${server.http.port:}")
    private Integer httpPort;

    public JobGatewayBootApplication(@Autowired
                                         HttpHandler httpHandler,
                                     @Autowired(required = false)
                                         NettyWebServerFactoryCustomizer nettyWebServerFactoryCustomizer) {
        this.httpHandler = httpHandler;
        this.nettyWebServerFactoryCustomizer = nettyWebServerFactoryCustomizer;
    }

    public static void main(String[] args) {
        SpringApplication.run(JobGatewayBootApplication.class, args);
    }

    @PostConstruct
    public void startHttpWebServer() {
        if (httpEnabled && httpPort != null) {
            NettyReactiveWebServerFactory factory = new NettyReactiveWebServerFactory(httpPort);
            if (nettyWebServerFactoryCustomizer != null) {
                nettyWebServerFactoryCustomizer.customize(factory);
            }
            this.httpWebServer = factory.getWebServer(this.httpHandler);
            this.httpWebServer.start();
        }
    }

    @PreDestroy
    public void stopHttpWebServer() {
        if (httpEnabled && httpPort != null) {
            this.httpWebServer.stop();
        }
    }
}
