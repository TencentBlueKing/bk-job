/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 */

package com.tencent.bk.job.service.login.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sun.net.httpserver.HttpServer;
import com.tencent.bk.job.config.RestTemplateConfig;
import com.tencent.bk.job.config.web.WebAuthProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class BkTicketLoginLoggingTest {

    private static final String LOG_DIR_PROPERTY = "JOB_LOG_DIR";

    @Test
    @DisplayName("dev环境真实登录校验不会把ticket写入HTTP客户端日志")
    void shouldNotLogTicketFromUnderlyingHttpClientInDevProfile() throws Exception {
        String originalLogDir = System.getProperty(LOG_DIR_PROPERTY);
        System.setProperty(LOG_DIR_PROPERTY, Path.of("build", "test-logs").toAbsolutePath().toString());
        HttpServer server = null;
        Logger rootLogger = null;
        Logger testServerLogger = null;
        Level originalTestServerLevel = null;
        ListAppender<ILoggingEvent> appender = null;
        try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder(EmptyConfiguration.class)
            .web(WebApplicationType.NONE)
            .profiles("dev")
            .properties(
                "spring.main.banner-mode=off",
                "spring.main.log-startup-info=false"
            )
            .run()) {
            assertProtectedLoggerLevel("org.springframework.web.client.RestTemplate");
            assertProtectedLoggerLevel("sun.net.www.protocol.http.HttpURLConnection");
            assertProtectedLoggerLevel("okhttp3");
            assertThat(((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).getEffectiveLevel())
                .isEqualTo(Level.DEBUG);

            rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            appender = new ListAppender<>();
            appender.start();
            rootLogger.addAppender(appender);

            // 内嵌服务与被测客户端同处一个JVM，避免测试夹具把收到的query写入被捕获日志
            testServerLogger = (Logger) LoggerFactory.getLogger("com.sun.net.httpserver");
            originalTestServerLevel = testServerLogger.getLevel();
            testServerLogger.setLevel(Level.INFO);

            AtomicReference<String> receivedQuery = new AtomicReference<>();
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/login/api/v3/user/get_info/", exchange -> {
                receivedQuery.set(exchange.getRequestURI().getRawQuery());
                byte[] body = "{\"ret\":0,\"data\":{\"username\":\"tester\"}}"
                    .getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();

            String ticket = "issue4523-canary-" + UUID.randomUUID();
            WebAuthProperties properties = new WebAuthProperties();
            properties.getAuth().getLogin().setApiUrl(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/login/api/v3"
            );
            properties.getAuth().getLogin().getCache().setMaxSize(0);
            properties.init();
            RestTemplate loginRestTemplate = new RestTemplateConfig().loginRestTemplate(2000, 3000);

            assertThat(new BkTicketLoginServiceImpl(properties, loginRestTemplate).getUsername(ticket))
                .isEqualTo("tester");
            assertThat(receivedQuery.get()).contains("bk_ticket=" + ticket);
            assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .allSatisfy(message -> assertThat(message).doesNotContain(ticket));
        } finally {
            if (server != null) {
                server.stop(0);
            }
            if (rootLogger != null && appender != null) {
                rootLogger.detachAppender(appender);
                appender.stop();
            }
            if (testServerLogger != null) {
                testServerLogger.setLevel(originalTestServerLevel);
            }
            if (originalLogDir == null) {
                System.clearProperty(LOG_DIR_PROPERTY);
            } else {
                System.setProperty(LOG_DIR_PROPERTY, originalLogDir);
            }
        }
    }

    private void assertProtectedLoggerLevel(String loggerName) {
        Logger logger = (Logger) LoggerFactory.getLogger(loggerName);
        assertThat(logger.getEffectiveLevel().isGreaterOrEqual(Level.INFO))
            .as("%s effective level", loggerName)
            .isTrue();
    }

    @Configuration(proxyBeanMethods = false)
    static class EmptyConfiguration {
    }
}
