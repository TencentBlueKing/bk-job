/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 */

package com.tencent.bk.job.service.login.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.benmanes.caffeine.cache.Ticker;
import com.tencent.bk.job.config.web.WebAuthProperties;
import com.tencent.bk.job.service.login.LoginServiceUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 统一登录服务的ticket校验与纯本地有界缓存。
 */
class BkTicketLoginServiceImplTest {

    private static final String API_URL = "http://login.example.com/login/api/v3";

    private RestTemplate loginRestTemplate;

    private MutableTicker ticker;

    private Logger logger;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        loginRestTemplate = mock(RestTemplate.class);
        ticker = new MutableTicker();
        logger = (Logger) LoggerFactory.getLogger(BkTicketLoginServiceImpl.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
        listAppender.stop();
    }

    private WebAuthProperties properties(long maxSize, int validSeconds, int invalidSeconds) {
        return properties(API_URL, maxSize, validSeconds, invalidSeconds);
    }

    private WebAuthProperties properties(String apiUrl, long maxSize, int validSeconds, int invalidSeconds) {
        WebAuthProperties properties = new WebAuthProperties();
        WebAuthProperties.Login login = properties.getAuth().getLogin();
        login.setApiUrl(apiUrl);
        login.getCache().setMaxSize(maxSize);
        login.getCache().setValidSeconds(validSeconds);
        login.getCache().setInvalidSeconds(invalidSeconds);
        properties.init();
        return properties;
    }

    private BkTicketLoginServiceImpl newService(long maxSize, int validSeconds, int invalidSeconds) {
        return new BkTicketLoginServiceImpl(
            properties(maxSize, validSeconds, invalidSeconds),
            loginRestTemplate,
            ticker
        );
    }

    private void givenResponses(String... bodies) {
        when(loginRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(bodies[0],
            java.util.Arrays.copyOfRange(bodies, 1, bodies.length));
    }

    private URI capturedUri() {
        ArgumentCaptor<URI> captor = ArgumentCaptor.forClass(URI.class);
        verify(loginRestTemplate).getForObject(captor.capture(), eq(String.class));
        return captor.getValue();
    }

    @Test
    @DisplayName("ticket只通过query参数传递且成功日志不包含ticket")
    void shouldPassTicketOnlyInQueryWithoutLoggingIt() {
        String ticket = "sensitive-ticket";
        givenResponses("{\"ret\":0,\"data\":{\"username\":\"admin\"}}");

        assertThat(newService(100, 300, 30).getUsername(ticket)).isEqualTo("admin");

        URI uri = capturedUri();
        assertThat(uri.getPath()).isEqualTo("/login/api/v3/user/get_info/");
        assertThat(uri.getQuery()).contains("app_code=job").contains("bk_ticket=" + ticket);
        assertThat(listAppender.list).extracting(ILoggingEvent::getFormattedMessage)
            .allSatisfy(message -> assertThat(message).doesNotContain(ticket));
    }

    @Test
    @DisplayName("ret为0且data非空时返回用户名，请求按 /user/get_info/ 协议拼装")
    void shouldReturnUsernameWhenRetIsZero() {
        givenResponses("{\"ret\":0,\"data\":{\"username\":\"admin\"}}");
        BkTicketLoginServiceImpl service = newService(100, 300, 30);

        assertThat(service.getUsername("protocol-ticket")).isEqualTo("admin");
        assertThat(service.getUsername("protocol-ticket")).isEqualTo("admin");

        URI uri = capturedUri();
        assertThat(uri.getPath()).isEqualTo("/login/api/v3/user/get_info/");
        assertThat(uri.getQuery()).contains("app_code=job").contains("bk_ticket=protocol-ticket");
    }

    @Test
    @DisplayName("api-url带结尾斜杠时不会拼出双斜杠")
    void shouldTrimTrailingSlashOfApiUrl() {
        givenResponses("{\"ret\":0,\"data\":{\"username\":\"admin\"}}");
        WebAuthProperties properties = properties(API_URL + "/", 100, 300, 30);

        new BkTicketLoginServiceImpl(properties, loginRestTemplate, ticker)
            .getUsername("trailing-slash-ticket");

        assertThat(capturedUri().getPath()).isEqualTo("/login/api/v3/user/get_info/");
    }

    @Test
    @DisplayName("cookie名可配置，并作为ticket的查询参数名")
    void shouldUseConfiguredTokenNameAsQueryParam() {
        givenResponses("{\"ret\":0,\"data\":{\"username\":\"admin\"}}");
        WebAuthProperties properties = properties(100, 300, 30);
        properties.getAuth().getLogin().setTokenName("custom_ticket");

        new BkTicketLoginServiceImpl(properties, loginRestTemplate, ticker)
            .getUsername("custom-token-ticket");

        assertThat(capturedUri().getQuery())
            .contains("custom_ticket=custom-token-ticket")
            .doesNotContain("bk_ticket=");
    }

    @Test
    @DisplayName("ret非0视为未登录，并写入负缓存")
    void shouldReturnNullWhenRetIsNotZero() {
        givenResponses("{\"ret\":1302403,\"data\":null,\"msg\":\"ticket invalid\"}");
        BkTicketLoginServiceImpl service = newService(100, 300, 30);

        assertThat(service.getUsername("invalid-ret-ticket")).isNull();
        assertThat(service.getUsername("invalid-ret-ticket")).isNull();

        verify(loginRestTemplate, times(1)).getForObject(any(URI.class), eq(String.class));
    }

    @Test
    @DisplayName("ret为0但data为空视为未登录，并写入负缓存")
    void shouldReturnNullWhenDataIsAbsent() {
        givenResponses("{\"ret\":0}");
        BkTicketLoginServiceImpl service = newService(100, 300, 30);

        assertThat(service.getUsername("absent-data-ticket")).isNull();
        assertThat(service.getUsername("absent-data-ticket")).isNull();

        verify(loginRestTemplate, times(1)).getForObject(any(URI.class), eq(String.class));
    }

    @Test
    @DisplayName("响应为空视为未登录且不抛异常，并写入负缓存")
    void shouldReturnNullWhenResponseIsBlank() {
        givenResponses("");
        BkTicketLoginServiceImpl service = newService(100, 300, 30);

        assertThat(service.getUsername("blank-response-ticket")).isNull();
        assertThat(service.getUsername("blank-response-ticket")).isNull();

        verify(loginRestTemplate, times(1)).getForObject(any(URI.class), eq(String.class));
    }

    @Test
    @DisplayName("登录调用异常日志不输出异常中携带的完整URI或ticket")
    void shouldNotLogTicketFromRestClientException() {
        String ticket = "exception-ticket";
        when(loginRestTemplate.getForObject(any(URI.class), eq(String.class)))
            .thenThrow(new ResourceAccessException(
                "GET http://login.example.com/user/get_info/?bk_ticket=" + ticket));

        assertThatThrownBy(() -> newService(100, 300, 30).getUsername(ticket))
            .isInstanceOf(LoginServiceUnavailableException.class);
        assertThat(listAppender.list).extracting(ILoggingEvent::getFormattedMessage)
            .allSatisfy(message -> assertThat(message).doesNotContain(ticket));
    }

    @Test
    @DisplayName("正缓存命中且300秒过期后重新校验")
    void shouldCacheValidTicketUntilPositiveTtlExpires() {
        givenResponses(
            "{\"ret\":0,\"data\":{\"username\":\"admin\"}}",
            "{\"ret\":0,\"data\":{\"username\":\"admin-new\"}}"
        );
        BkTicketLoginServiceImpl service = newService(100, 300, 30);

        assertThat(service.getUsername("t1")).isEqualTo("admin");
        assertThat(service.getUsername("t1")).isEqualTo("admin");
        verify(loginRestTemplate, times(1)).getForObject(any(URI.class), eq(String.class));

        ticker.advance(301, TimeUnit.SECONDS);
        assertThat(service.getUsername("t1")).isEqualTo("admin-new");
        verify(loginRestTemplate, times(2)).getForObject(any(URI.class), eq(String.class));
    }

    @Test
    @DisplayName("负缓存命中且30秒过期后重新校验")
    void shouldCacheInvalidTicketUntilNegativeTtlExpires() {
        givenResponses(
            "{\"ret\":1302403,\"data\":null}",
            "{\"ret\":0,\"data\":{\"username\":\"admin\"}}"
        );
        BkTicketLoginServiceImpl service = newService(100, 300, 30);

        assertThat(service.getUsername("bad")).isNull();
        assertThat(service.getUsername("bad")).isNull();
        verify(loginRestTemplate, times(1)).getForObject(any(URI.class), eq(String.class));

        ticker.advance(31, TimeUnit.SECONDS);
        assertThat(service.getUsername("bad")).isEqualTo("admin");
        verify(loginRestTemplate, times(2)).getForObject(any(URI.class), eq(String.class));
    }

    @Test
    @DisplayName("缓存键为纯SHA-256摘要且不含ticket明文")
    void shouldHashTicketForCacheKey() {
        String ticket = "plain-ticket-must-not-be-a-key";
        givenResponses("{\"ret\":0,\"data\":{\"username\":\"admin\"}}");
        BkTicketLoginServiceImpl service = newService(100, 300, 30);

        service.getUsername(ticket);

        assertThat(service.cacheKeys()).hasSize(1);
        assertThat(service.cacheKeys().iterator().next())
            .hasSize(64)
            .matches("[0-9a-f]{64}")
            .doesNotContain(ticket);
    }

    @Test
    @DisplayName("超过max-size时发生容量淘汰，缓存不会无界增长")
    void shouldEvictEntriesAtMaximumSize() {
        givenResponses(
            "{\"ret\":0,\"data\":{\"username\":\"user1\"}}",
            "{\"ret\":0,\"data\":{\"username\":\"user2\"}}"
        );
        BkTicketLoginServiceImpl service = newService(1, 300, 30);

        service.getUsername("t1");
        service.getUsername("t2");

        assertThat(service.estimatedCacheSize()).isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("max-size为0时禁用缓存，每次请求都重新校验")
    void shouldDisableCacheWhenMaximumSizeIsZero() {
        givenResponses(
            "{\"ret\":0,\"data\":{\"username\":\"admin\"}}",
            "{\"ret\":0,\"data\":{\"username\":\"admin\"}}"
        );
        BkTicketLoginServiceImpl service = newService(0, 300, 30);

        assertThat(service.getUsername("t1")).isEqualTo("admin");
        assertThat(service.getUsername("t1")).isEqualTo("admin");

        verify(loginRestTemplate, times(2)).getForObject(any(URI.class), eq(String.class));
        assertThat(service.estimatedCacheSize()).isZero();
    }

    @Test
    @DisplayName("api-url未配置或ticket为空时不发起HTTP请求")
    void shouldSkipHttpWhenNotConfiguredOrTicketBlank() {
        WebAuthProperties properties = new WebAuthProperties();
        properties.init();
        BkTicketLoginServiceImpl service = new BkTicketLoginServiceImpl(
            properties,
            loginRestTemplate,
            ticker
        );

        assertThat(service.getUsername("t1")).isNull();
        assertThat(service.getUsername(" ")).isNull();
        verify(loginRestTemplate, never()).getForObject(any(URI.class), eq(String.class));
    }

    private static class MutableTicker implements Ticker {

        private final AtomicLong nanos = new AtomicLong();

        @Override
        public long read() {
            return nanos.get();
        }

        void advance(long duration, TimeUnit unit) {
            nanos.addAndGet(unit.toNanos(duration));
        }
    }
}
