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

package com.tencent.bk.job.gateway.filter;

import com.tencent.bk.job.gateway.config.BkConfig;
import com.tencent.bk.job.gateway.web.service.LoginService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

import static org.mockito.Mockito.*;

public class CsrfCheckGatewayFilterFactoryTest {
    private static final String COOKIE_CSRF_KEY_NAME = "job_csrf_key";
    private static final String HEADER_CSRF_TOKEN_NAME = "X-CSRF-Token";
    private CsrfCheckGatewayFilterFactory factory;

    @BeforeEach
    public void init() {
        BkConfig bkConfig = mock(BkConfig.class);
        when(bkConfig.getJobWebUrl()).thenReturn("http://jobv3.com");
        LoginService loginService = mock(LoginService.class);
        factory = new CsrfCheckGatewayFilterFactory(bkConfig, loginService);
    }

    @AfterEach
    public void destroy() {
        factory = null;
    }

    @Test
    @DisplayName("csrf校验通过，forward请求")
    public void whenCsrfKeyCheckSuccessThenForward() {
        ServerWebExchange mockExchange = spy(ServerWebExchange.class);
        ServerHttpRequest mockRequest = mock(ServerHttpRequest.class);
        ServerHttpResponse mockResponse = mock(ServerHttpResponse.class);

        when(mockExchange.getResponse()).thenReturn(mockResponse);
        when(mockExchange.getRequest()).thenReturn(mockRequest);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HEADER_CSRF_TOKEN_NAME, "1864608314");
        when(mockRequest.getHeaders()).thenReturn(httpHeaders);

        HttpCookie httpCookie = new HttpCookie(COOKIE_CSRF_KEY_NAME, "1959189290");
        MultiValueMap<String, HttpCookie> cookieMultiValueMap = new LinkedMultiValueMap<>();
        cookieMultiValueMap.add(COOKIE_CSRF_KEY_NAME, httpCookie);
        when(mockRequest.getCookies()).thenReturn(cookieMultiValueMap);

        GatewayFilterChain mockChain = mock(GatewayFilterChain.class);
        factory.filter(mockExchange, mockChain);

        verify(mockChain).filter(any(ServerWebExchange.class));
    }

    @Test
    public void whenHeaderCsrfTokenIsEmptyThenReturnUnauthorizedResponseStatusAndCsrfCookie() {
        ServerWebExchange mockExchange = spy(ServerWebExchange.class);
        ServerHttpRequest mockRequest = mock(ServerHttpRequest.class);
        ServerHttpResponse mockResponse = mock(ServerHttpResponse.class);

        when(mockExchange.getResponse()).thenReturn(mockResponse);
        when(mockExchange.getRequest()).thenReturn(mockRequest);

        HttpHeaders emptyHttpHeaders = new HttpHeaders();
        when(mockRequest.getHeaders()).thenReturn(emptyHttpHeaders);

        HttpCookie httpCookie = new HttpCookie(COOKIE_CSRF_KEY_NAME, "1959189290");
        MultiValueMap<String, HttpCookie> cookieMultiValueMap = new LinkedMultiValueMap<>();
        cookieMultiValueMap.add(COOKIE_CSRF_KEY_NAME, httpCookie);
        when(mockRequest.getCookies()).thenReturn(cookieMultiValueMap);

        HttpHeaders responseHeaders = new HttpHeaders();
        when(mockResponse.getHeaders()).thenReturn(responseHeaders);

        GatewayFilterChain mockChain = mock(GatewayFilterChain.class);
        factory.filter(mockExchange, mockChain);

        verify(mockResponse).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(mockResponse).addCookie(any(ResponseCookie.class));
        verify(mockChain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    public void whenCookieCsrfKeyIsEmptyThenReturnUnauthorizedResponseStatusAndCsrfCookie() {
        ServerWebExchange mockExchange = spy(ServerWebExchange.class);
        ServerHttpRequest mockRequest = mock(ServerHttpRequest.class);
        ServerHttpResponse mockResponse = mock(ServerHttpResponse.class);

        when(mockExchange.getResponse()).thenReturn(mockResponse);
        when(mockExchange.getRequest()).thenReturn(mockRequest);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HEADER_CSRF_TOKEN_NAME, "1864608314");
        when(mockRequest.getHeaders()).thenReturn(httpHeaders);

        MultiValueMap<String, HttpCookie> cookieMultiValueMap = new LinkedMultiValueMap<>();
        when(mockRequest.getCookies()).thenReturn(cookieMultiValueMap);

        HttpHeaders responseHeaders = new HttpHeaders();
        when(mockResponse.getHeaders()).thenReturn(responseHeaders);

        GatewayFilterChain mockChain = mock(GatewayFilterChain.class);
        factory.filter(mockExchange, mockChain);

        verify(mockResponse).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(mockResponse).addCookie(any(ResponseCookie.class));
        verify(mockChain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    public void whenCsrfTokenIsWrongThenReturnUnauthorizedResponseStatusAndCsrfCookie() {
        ServerWebExchange mockExchange = spy(ServerWebExchange.class);
        ServerHttpRequest mockRequest = mock(ServerHttpRequest.class);
        ServerHttpResponse mockResponse = mock(ServerHttpResponse.class);

        when(mockExchange.getResponse()).thenReturn(mockResponse);
        when(mockExchange.getRequest()).thenReturn(mockRequest);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HEADER_CSRF_TOKEN_NAME, "1864608314");
        when(mockRequest.getHeaders()).thenReturn(httpHeaders);

        HttpCookie httpCookie = new HttpCookie(COOKIE_CSRF_KEY_NAME, "1959189291");
        MultiValueMap<String, HttpCookie> cookieMultiValueMap = new LinkedMultiValueMap<>();
        cookieMultiValueMap.add(COOKIE_CSRF_KEY_NAME, httpCookie);
        when(mockRequest.getCookies()).thenReturn(cookieMultiValueMap);

        HttpHeaders responseHeaders = new HttpHeaders();
        when(mockResponse.getHeaders()).thenReturn(responseHeaders);

        GatewayFilterChain mockChain = mock(GatewayFilterChain.class);
        factory.filter(mockExchange, mockChain);

        verify(mockResponse, times(1)).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(mockResponse, times(1)).addCookie(any(ResponseCookie.class));
        verify(mockChain, never()).filter(any(ServerWebExchange.class));
    }
}
