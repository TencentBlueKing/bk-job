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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalUserManageException;
import com.tencent.bk.job.common.i18n.locale.LocaleUtils;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.BkUserDTO;
import com.tencent.bk.job.common.paas.exception.AppPermissionDeniedException;
import com.tencent.bk.job.common.util.RequestUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.gateway.config.LoginExemptionConfig;
import com.tencent.bk.job.gateway.web.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.tencent.bk.job.common.i18n.locale.LocaleUtils.BLUEKING_LANG_HEADER;

/**
 * 用户token校验
 */
@Slf4j
@Component
public class AuthorizeGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthorizeGatewayFilterFactory.Config> {

    private final LoginService loginService;
    private final LoginExemptionConfig loginExemptionConfig;

    @Autowired
    public AuthorizeGatewayFilterFactory(LoginService loginService, LoginExemptionConfig loginExemptionConfig) {
        super(Config.class);
        this.loginService = loginService;
        this.loginExemptionConfig = loginExemptionConfig;
    }

    private GatewayFilter getLoginExemptionFilter() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String username = loginExemptionConfig.getDefaultUser();
            log.info("loginExemption enabled, use default user:{}", username);
            request.mutate().header("username", new String[]{username}).build();
            return chain.filter(exchange.mutate().request(request).build());
        };
    }

    private GatewayFilter getLoginFilter() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();
            String tokenCookieName = loginService.getCookieNameForToken();
            List<String> bkTokenList = RequestUtil.getCookieValuesFromHeader(request, tokenCookieName);
            String lang = RequestUtil.getCookieValue(request, BLUEKING_LANG_HEADER);
            if (StringUtils.isBlank(lang)) {
                lang = LocaleUtils.LANG_EN;
                log.warn("Cannot find blueking_language in cookie, use en");
            }
            LocaleContextHolder.setLocale(LocaleUtils.getLocale(lang), true);
            if (CollectionUtils.isEmpty(bkTokenList)) {
                log.warn("Fail to parse token from headers, please check");
                String bkToken = RequestUtil.getCookieValue(request, tokenCookieName);
                if (StringUtils.isNotBlank(bkToken)) {
                    bkTokenList.add(bkToken);
                }
            }
            if (CollectionUtils.isEmpty(bkTokenList)) {
                log.warn("Cookie {} is empty,illegal request!", tokenCookieName);
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                response.getHeaders().add("x-login-url", loginService.getLoginRedirectUrl());
                return response.setComplete();
            }
            BkUserDTO user;
            try {
                user = getUserByTokenList(bkTokenList, lang);
            } catch (InternalUserManageException e) {
                Throwable cause = e.getCause();
                if (cause instanceof AppPermissionDeniedException) {
                    return getUserAccessAppForbiddenResp(response, cause.getMessage());
                } else {
                    throw e;
                }
            }
            if (user == null) {
                log.warn("Invalid user token");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                response.getHeaders().add("x-login-url", loginService.getLoginRedirectUrl());
                return response.setComplete();
            }
            String username = user.getUsername();

            request.mutate().header("username", new String[]{username}).build();
            return chain.filter(exchange.mutate().request(request).build());
        };
    }

    private Mono<Void> getUserAccessAppForbiddenResp(ServerHttpResponse response, String data) {
        Response<?> resp = new Response<>(ErrorCode.USER_ACCESS_APP_FORBIDDEN, data);
        response.setStatusCode(HttpStatus.FORBIDDEN);
        String body = JsonUtils.toJson(resp);
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer dataBuffer = response.bufferFactory().wrap(bodyBytes);
        response.getHeaders().setContentLength(bodyBytes.length);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.writeWith(Mono.just(dataBuffer)).subscribe();
        return response.setComplete();
    }

    private BkUserDTO getUserByTokenList(List<String> bkTokenList, String lang) {
        // 遍历所有传入token找出当前环境的
        for (String bkToken : bkTokenList) {
            BkUserDTO user = loginService.getUser(bkToken, lang);
            if (user != null) {
                return user;
            }
        }
        return null;
    }

    @Override
    public GatewayFilter apply(Config config) {
        if (loginExemptionConfig.isEnableLoginExemption()) {
            return getLoginExemptionFilter();
        }
        return getLoginFilter();
    }

    static class Config {

    }

}
