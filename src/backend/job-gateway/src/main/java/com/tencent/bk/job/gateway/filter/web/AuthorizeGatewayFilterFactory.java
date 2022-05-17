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

import com.tencent.bk.job.common.model.dto.BkUserDTO;
import com.tencent.bk.job.common.util.RequestUtil;
import com.tencent.bk.job.gateway.config.LoginExemptionConfig;
import com.tencent.bk.job.gateway.web.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

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
            if (CollectionUtils.isEmpty(bkTokenList)) {
                log.warn("Fail to parse token from headers, please check");
                bkTokenList.add(RequestUtil.getCookieValue(request, tokenCookieName));
            }
            if (CollectionUtils.isEmpty(bkTokenList)) {
                log.warn("Cookie {} is empty,illegal request!", tokenCookieName);
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                response.getHeaders().add("x-login-url", loginService.getLoginRedirectUrl());
                return response.setComplete();
            }
            BkUserDTO user = null;
            // 遍历所有传入token找出当前环境的
            for (String bkToken : bkTokenList) {
                if (user == null) {
                    user = loginService.getUser(bkToken);
                } else {
                    break;
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
