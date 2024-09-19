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

package com.tencent.bk.job.common.web.listener;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.ProtocolHandler;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.ApplicationListener;

import java.util.concurrent.Executor;

/**
 * 监听WebServer初始化完成事件，执行注册线程池监控指标等动作
 */
@Slf4j
public class ServletWebServerInitializedEventListener implements ApplicationListener<ServletWebServerInitializedEvent> {

    private final MeterRegistry registry;

    public ServletWebServerInitializedEventListener(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onApplicationEvent(ServletWebServerInitializedEvent event) {
        log.info("ServletWebServerInitializedEvent caught");
        WebServer webServer = event.getSource();
        initTomcatExecutorMetrics(webServer);
    }

    /**
     * 注册Tomcat线程池监控指标
     *
     * @param webServer Web服务器
     */
    private void initTomcatExecutorMetrics(WebServer webServer) {
        if (!(webServer instanceof TomcatWebServer)) {
            log.info("Unknown web server type: {}, ignore tomcat executor metrics", webServer.getClass().getName());
            return;
        }
        TomcatWebServer tomcatWebServer = (TomcatWebServer) webServer;
        ProtocolHandler protocolHandler = tomcatWebServer.getTomcat().getConnector().getProtocolHandler();
        log.debug("protocolHandler: {}", protocolHandler);
        if (protocolHandler == null) {
            return;
        }
        Executor executor = protocolHandler.getExecutor();
        log.debug("executor: {}", executor);
        if (executor == null) {
            return;
        }
        if (executor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor threadExecutor = (ThreadPoolExecutor) executor;
            registry.gauge("tomcat.threads.max", threadExecutor, ThreadPoolExecutor::getMaximumPoolSize);
            registry.gauge("tomcat.threads.current", threadExecutor, ThreadPoolExecutor::getPoolSize);
            registry.gauge("tomcat.threads.busy", threadExecutor, ThreadPoolExecutor::getActiveCount);
            log.info("Tomcat thread pool metrics inited");
        } else {
            log.warn("Unknown executor type: {}, ignore tomcat executor metrics", executor.getClass().getName());
        }
    }
}
