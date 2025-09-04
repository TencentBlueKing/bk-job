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

package com.tencent.bk.job.gateway.filter.web;

import com.tencent.bk.job.common.service.config.JobCommonConfig;
import com.tencent.bk.job.common.util.RequestUtil;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.gateway.common.consts.HeaderConsts;
import com.tencent.bk.job.gateway.common.util.UrlUtil;
import com.tencent.bk.job.gateway.config.CsrfCheckProperties;
import com.tencent.bk.job.gateway.web.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
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
    private final JobCommonConfig jobCommonConfig;
    private final LoginService loginService;
    private final CsrfCheckProperties csrfCheckProperties;
    private String domain = "";

    @Autowired
    public CsrfCheckGatewayFilterFactory(JobCommonConfig jobCommonConfig,
                                         LoginService loginService,
                                         CsrfCheckProperties csrfCheckProperties) {
        super(Config.class);
        this.jobCommonConfig = jobCommonConfig;
        this.loginService = loginService;
        this.csrfCheckProperties = csrfCheckProperties;
    }

    private static String createCsrfKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public GatewayFilter apply(Config config) {
        if (csrfCheckProperties.isEnabled()) {
            return this::filter;
        }
        return (exchange, chain) -> chain.filter(exchange);
    }

    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (isCsrfTokenValid(request)) {
            return chain.filter(exchange);
        }
        ServerHttpResponse response = exchange.getResponse();
        if (!needToPreventByCsrf(request)) {
            addCsrfKeyCookie(response);
            return chain.filter(exchange.mutate().response(response).build());
        }
        return buildUnAuthorizedRespWithCsrfCookie(response);
    }

    /**
     * 检查Csrf Token是否合法
     *
     * @param request 请求
     * @return 布尔值
     */
    private boolean isCsrfTokenValid(ServerHttpRequest request) {
        String csrfTokenFromHeader = RequestUtil.getHeaderValue(request, HEADER_CSRF_TOKEN_NAME);
        if (StringUtils.isBlank(csrfTokenFromHeader)) {
            log.warn(
                "Empty csrfToken from header, request={}",
                tryToGetRequestDesc(request)
            );
            tryToLogRequestBody(request);
            return false;
        }
        String csrfTokenFromCookie = RequestUtil.getCookieValue(request, COOKIE_CSRF_KEY_NAME);
        if (StringUtils.isBlank(csrfTokenFromCookie)) {
            log.info("Empty csrfKey from cookie, first access");
            return false;
        }
        if (!csrfTokenFromHeader.equals(csrfTokenFromCookie)) {
            log.warn(
                "Invalid csrfToken, not match with csrfKey from cookie, request={}",
                tryToGetRequestDesc(request)
            );
            tryToLogRequestBody(request);
            return false;
        }
        return true;
    }

    /**
     * 尝试获取请求描述
     *
     * @param request 请求
     * @return 请求描述
     */
    @SuppressWarnings("StringBufferReplaceableByString")
    private String tryToGetRequestDesc(ServerHttpRequest request) {
        if (request == null) {
            return "null";
        }
        int maxLength = 2048;
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("[");
            builder.append("uri=").append(request.getURI()).append(",");
            builder.append("headers=").append(getHeadersDesc(request));
            builder.append("]");
            String requestDesc = builder.toString();
            return StringUtil.substring(requestDesc, maxLength);
        } catch (Throwable t) {
            log.warn("tryToGetRequestDesc error", t);
            return StringUtil.substring(request.toString(), maxLength);
        }
    }

    /**
     * 尝试打印请求体
     *
     * @param request 请求
     */
    private void tryToLogRequestBody(ServerHttpRequest request) {
        try {
            getBodyDesc(request)
                .doOnNext(bodyDesc -> {
                    // 处理请求体描述
                    log.info("body=" + bodyDesc);
                })
                .doOnError(error -> {
                    // 处理错误
                    log.warn("getBodyDesc error", error);
                })
                // 订阅以触发处理
                .subscribe();
        } catch (Throwable t) {
            log.error("tryToLogRequestBody error", t);
        }
    }

    /**
     * 获取请求体描述
     *
     * @param request 请求
     * @return 请求体描述
     */
    private Mono<String> getBodyDesc(ServerHttpRequest request) {
        Flux<DataBuffer> body = request.getBody();
        // 获取第一个 DataBuffer并转换为字符串
        return body
            .map(dataBuffer -> {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                return new String(bytes, StandardCharsets.UTF_8);
            })
            .next();
    }

    /**
     * 获取请求头描述
     *
     * @param request 请求
     * @return 请求头描述
     */
    private String getHeadersDesc(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        return headers.toString();
    }

    /**
     * CSRF校验不通过的情况下，是否需要拦截该请求
     *
     * @param request 请求
     * @return 布尔值
     */
    private boolean needToPreventByCsrf(ServerHttpRequest request) {
        HttpMethod method = request.getMethod();
        // 仅对会实际产生数据更改的请求进行拦截
        return HttpMethod.POST == method
            || HttpMethod.PUT == method
            || HttpMethod.PATCH == method
            || HttpMethod.DELETE == method;
    }

    /**
     * 构建CSRF校验失败的响应，给出登录地址并添加含csrfKey的cookie
     *
     * @param response 响应
     * @return 响应Mono
     */
    private Mono<Void> buildUnAuthorizedRespWithCsrfCookie(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HeaderConsts.KEY_LOGIN_URL, loginService.getLoginRedirectUrl());
        addCsrfKeyCookie(response);
        return response.setComplete();
    }

    /**
     * 给响应添加csrfKey的cookie
     *
     * @param response 响应
     */
    private void addCsrfKeyCookie(ServerHttpResponse response) {
        String cookieValue = createCsrfKey();
        ResponseCookie responseCookie = ResponseCookie.from(COOKIE_CSRF_KEY_NAME, cookieValue)
            .path("/")
            .domain(getDomain())
            .httpOnly(false)
            .maxAge(Duration.ofDays(30))
            .sameSite("Lax")
            .build();
        response.addCookie(responseCookie);
    }

    private String getDomain() {
        if (StringUtils.isNotBlank(this.domain)) {
            return this.domain;
        }
        String webUrl = jobCommonConfig.getJobWebUrl();
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
