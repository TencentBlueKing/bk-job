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

package com.tencent.bk.job.gateway.filter.web;

import com.tencent.bk.job.common.util.RequestUtil;
import com.tencent.bk.job.gateway.common.util.UrlUtil;
import com.tencent.bk.job.gateway.config.BkConfig;
import com.tencent.bk.job.gateway.web.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.time.Duration;
import java.util.UUID;

/**
 * web csrf检查
 */
@Slf4j
@Component
public class CsrfCheckGatewayFilterFactory extends AbstractGatewayFilterFactory<CsrfCheckGatewayFilterFactory.Config> {


    private static final String COOKIE_CSRF_KEY_NAME = "job_csrf_key";
    private static final String HEADER_CSRF_TOKEN_NAME = "X-CSRF-Token";
    private final BkConfig bkConfig;
    private final LoginService loginService;
    private String domain = "";

    @Autowired
    public CsrfCheckGatewayFilterFactory(BkConfig bkConfig, LoginService loginService) {
        super(Config.class);
        this.bkConfig = bkConfig;
        this.loginService = loginService;
    }

    private static String createCsrfKey() {
        return String.valueOf(csrfHashCode(UUID.randomUUID().toString()));
    }

    private static int csrfHashCode(String skey) {
        int hashCode = 5381;
        for (int i = 0; i < skey.length(); i++) {
            hashCode += (hashCode << 5) + skey.charAt(i);
        }
        return hashCode & 0x7fffffff;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> chain.filter(exchange);
//        return this::filter;
    }

    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        ServerHttpResponse response = exchange.getResponse();
        String csrfToken = RequestUtil.getHeaderValue(request, HEADER_CSRF_TOKEN_NAME);
        if (StringUtils.isBlank(csrfToken)) {
            log.warn("Empty csrfToken");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().add("x-login-url", loginService.getLoginRedirectUrl());
            addCsrfKeyCookie(response);
            return response.setComplete();
        }
        String csrfKey = RequestUtil.getCookieValue(request, COOKIE_CSRF_KEY_NAME);
        if (StringUtils.isBlank(csrfKey)) {
            log.warn("Empty csrfKey");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().add("x-login-url", loginService.getLoginRedirectUrl());
            addCsrfKeyCookie(response);
            return response.setComplete();
        }

        String csrfTokenFromCookie = String.valueOf(csrfHashCode(csrfKey));
        if (!csrfToken.equals(csrfTokenFromCookie)) {
            log.warn("Invalid csrfToken");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().add("x-login-url", loginService.getLoginRedirectUrl());
            addCsrfKeyCookie(response);
            return response.setComplete();
        }

        return chain.filter(exchange.mutate().request(request).build());
    }

    private void addCsrfKeyCookie(ServerHttpResponse response) {
        String cookieValue = createCsrfKey();
        ResponseCookie responseCookie =
            ResponseCookie.from(COOKIE_CSRF_KEY_NAME, cookieValue).path("/").domain(getDomain())
            .httpOnly(false).maxAge(Duration.ofDays(7)).build();
        response.addCookie(responseCookie);
    }

    private String getDomain() {
        if (StringUtils.isNotBlank(this.domain)) {
            return this.domain;
        }
        String webUrl = bkConfig.getJobWebUrl();
        if (StringUtils.isEmpty(webUrl)) {
            throw new IllegalArgumentException("job.web.url");
        }
        String[] urls = webUrl.split(",");
        if (urls.length < 1) {
            throw new IllegalArgumentException("job.web.url");
        }

        try {
            this.domain = UrlUtil.getDomain(urls[0]);
            return this.domain;
        } catch (MalformedURLException e) {
            log.error("Invalid job web url: {}", urls[0]);
            throw new IllegalArgumentException("job.web.url");
        }
    }

    public static class Config {

    }
}
