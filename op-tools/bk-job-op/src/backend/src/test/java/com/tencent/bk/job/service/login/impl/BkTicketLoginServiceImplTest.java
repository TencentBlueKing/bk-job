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

package com.tencent.bk.job.service.login.impl;

import com.tencent.bk.job.config.web.WebAuthProperties;
import com.tencent.bk.job.service.login.LoginServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 统一登录服务的ticket校验。
 *
 * <p>协议与主项目自定义登录一致：{@code GET {api-url}/user/get_info/?app_code=..&bk_ticket=..}，
 * 响应 {@code {"ret":0,"data":{"username":"xxx"}}}。</p>
 */
@SuppressWarnings("unchecked")
class BkTicketLoginServiceImplTest {

    private static final String API_URL = "http://login.example.com/login/api/v3";

    private RestTemplate loginRestTemplate;

    private RedisTemplate<String, String> redisTemplate;

    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        loginRestTemplate = mock(RestTemplate.class);
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private BkTicketLoginServiceImpl newService(String apiUrl) {
        WebAuthProperties properties = new WebAuthProperties();
        properties.getAuth().getLogin().setApiUrl(apiUrl);
        properties.init();
        return new BkTicketLoginServiceImpl(properties, loginRestTemplate, redisTemplate);
    }

    private void givenResponse(String body) {
        when(loginRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(body);
    }

    private URI capturedUri() {
        ArgumentCaptor<URI> captor = ArgumentCaptor.forClass(URI.class);
        verify(loginRestTemplate).getForObject(captor.capture(), eq(String.class));
        return captor.getValue();
    }

    @Test
    @DisplayName("ret为0且data非空时返回用户名，请求按 /user/get_info/ 协议拼装")
    void shouldReturnUsernameWhenRetIsZero() {
        givenResponse("{\"ret\":0,\"data\":{\"username\":\"admin\"}}");

        String username = newService(API_URL).getUsername("t1");

        assertThat(username).isEqualTo("admin");
        URI uri = capturedUri();
        assertThat(uri.getPath()).isEqualTo("/login/api/v3/user/get_info/");
        assertThat(uri.getQuery()).contains("app_code=job").contains("bk_ticket=t1");
    }

    @Test
    @DisplayName("api-url带结尾斜杠时不会拼出双斜杠")
    void shouldTrimTrailingSlashOfApiUrl() {
        givenResponse("{\"ret\":0,\"data\":{\"username\":\"admin\"}}");

        newService(API_URL + "/").getUsername("t1");

        assertThat(capturedUri().getPath()).isEqualTo("/login/api/v3/user/get_info/");
    }

    @Test
    @DisplayName("cookie名可配置，作为ticket的查询参数名")
    void shouldUseConfiguredTokenNameAsQueryParam() {
        givenResponse("{\"ret\":0,\"data\":{\"username\":\"admin\"}}");
        WebAuthProperties properties = new WebAuthProperties();
        properties.getAuth().getLogin().setApiUrl(API_URL);
        properties.getAuth().getLogin().setTokenName("bk_token");
        properties.init();

        new BkTicketLoginServiceImpl(properties, loginRestTemplate, redisTemplate).getUsername("t1");

        assertThat(capturedUri().getQuery()).contains("bk_token=t1");
    }

    @Test
    @DisplayName("ret非0视为未登录")
    void shouldReturnNullWhenRetIsNotZero() {
        givenResponse("{\"ret\":1302403,\"data\":null,\"msg\":\"ticket invalid\"}");

        assertThat(newService(API_URL).getUsername("bad")).isNull();
    }

    @Test
    @DisplayName("ret为0但data为空同样视为未登录")
    void shouldReturnNullWhenDataIsAbsent() {
        givenResponse("{\"ret\":0}");

        assertThat(newService(API_URL).getUsername("t1")).isNull();
    }

    @Test
    @DisplayName("响应为空时视为未登录，不抛异常")
    void shouldReturnNullWhenResponseIsBlank() {
        givenResponse("");

        assertThat(newService(API_URL).getUsername("t1")).isNull();
    }

    @Test
    @DisplayName("api-url未配置时一律视为未登录，且不发起HTTP请求")
    void shouldTreatAllAsUnauthenticatedWhenApiUrlIsNotConfigured() {
        assertThat(newService("").getUsername("t1")).isNull();

        verify(loginRestTemplate, never()).getForObject(any(URI.class), eq(String.class));
    }

    @Test
    @DisplayName("ticket为空时直接返回null，不发起HTTP请求")
    void shouldReturnNullForBlankTicket() {
        assertThat(newService(API_URL).getUsername(" ")).isNull();

        verify(loginRestTemplate, never()).getForObject(any(URI.class), eq(String.class));
    }

    @Test
    @DisplayName("登录服务不可达时抛LoginServiceUnavailableException，由调用方fail-close")
    void shouldThrowWhenLoginServiceIsUnreachable() {
        when(loginRestTemplate.getForObject(any(URI.class), eq(String.class)))
            .thenThrow(new ResourceAccessException("timeout"));

        assertThatThrownBy(() -> newService(API_URL).getUsername("t1"))
            .isInstanceOf(LoginServiceUnavailableException.class);
    }

    @Test
    @DisplayName("校验结果写入缓存：有效ticket缓存用户名，无效ticket写负缓存")
    void shouldCacheVerificationResult() {
        givenResponse("{\"ret\":0,\"data\":{\"username\":\"admin\"}}");

        newService(API_URL).getUsername("t1");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), eq(Duration.ofSeconds(300)));
        // ticket不得以明文进入缓存key
        assertThat(keyCaptor.getValue()).startsWith("job:op:login:ticket:").doesNotContain("t1");
        assertThat(valueCaptor.getValue()).isEqualTo("admin");
    }

    @Test
    @DisplayName("无效ticket写30秒负缓存")
    void shouldWriteNegativeCacheForInvalidTicket() {
        givenResponse("{\"ret\":1302403}");

        newService(API_URL).getUsername("bad");

        verify(valueOperations).set(anyString(), eq("__INVALID__"), eq(Duration.ofSeconds(30)));
    }

    @Test
    @DisplayName("命中缓存时直接返回，不再调用登录服务")
    void shouldServeFromCacheWithoutCallingLoginService() {
        when(valueOperations.get(anyString())).thenReturn("admin");

        assertThat(newService(API_URL).getUsername("t1")).isEqualTo("admin");
        verify(loginRestTemplate, never()).getForObject(any(URI.class), eq(String.class));
    }

    @Test
    @DisplayName("命中负缓存时直接判定未登录，不再调用登录服务")
    void shouldServeNegativeCacheWithoutCallingLoginService() {
        when(valueOperations.get(anyString())).thenReturn("__INVALID__");

        assertThat(newService(API_URL).getUsername("bad")).isNull();
        verify(loginRestTemplate, never()).getForObject(any(URI.class), eq(String.class));
    }

    @Test
    @DisplayName("Redis不可用时降级为直连登录服务，不阻断登录")
    void shouldFallbackToLoginServiceWhenCacheIsUnavailable() {
        when(valueOperations.get(anyString())).thenThrow(new IllegalStateException("redis down"));
        givenResponse("{\"ret\":0,\"data\":{\"username\":\"admin\"}}");

        assertThat(newService(API_URL).getUsername("t1")).isEqualTo("admin");
    }
}
