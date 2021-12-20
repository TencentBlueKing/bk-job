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

package com.tencent.bk.job.gateway.config;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.BkUserDTO;
import com.tencent.bk.job.gateway.web.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpCookie;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

@Configuration
public class RouteConfig {
    @Bean
    public RouterFunction<ServerResponse> loginUserRouting(@Autowired UserHandler userHandler) {
        return RouterFunctions.route(GET("/user/current"), userHandler::getUserByBkToken);
    }

    @Bean
    public UserHandler userHandler(@Autowired LoginService loginService, @Autowired MessageI18nService i18nService, @Autowired LoginExemptionConfig loginExemptionConfig) {
        return new UserHandler(loginService, i18nService, loginExemptionConfig);
    }

    static class UserHandler {
        private LoginService loginService;
        private MessageI18nService i18nService;
        private LoginExemptionConfig loginExemptionConfig;

        UserHandler(
            LoginService loginService,
            MessageI18nService i18nService,
            LoginExemptionConfig loginExemptionConfig
        ) {
            this.loginService = loginService;
            this.i18nService = i18nService;
            this.loginExemptionConfig = loginExemptionConfig;
        }

        private BkUserDTO getLoginExemptUser() {
            BkUserDTO bkUserDTO = new BkUserDTO();
            bkUserDTO.setId(1L);
            bkUserDTO.setUsername(loginExemptionConfig.getDefaultUser());
            bkUserDTO.setDisplayName(loginExemptionConfig.getDefaultUser());
            bkUserDTO.setUid(loginExemptionConfig.getDefaultUser());
            return bkUserDTO;
        }

        Mono<ServerResponse> getUserByBkToken(ServerRequest request) {
            if (loginExemptionConfig.isEnableLoginExemption()) {
                Response<BkUserDTO> resp = Response.buildSuccessResp(getLoginExemptUser());
                return ServerResponse.ok().body(Mono.just(resp), Response.class);
            }
            MultiValueMap<String, HttpCookie> cookieMap = request.cookies();
            HttpCookie bkTokenCookie = cookieMap.get(loginService.getCookieNameForToken()).get(0);

            String bkToken = bkTokenCookie.getValue();
            BkUserDTO user = loginService.getUser(bkToken);
            if (user == null) {
                Response<?> resp = Response.buildCommonFailResp(ErrorCode.USER_NOT_EXIST_OR_NOT_LOGIN_IN);
                return ServerResponse.ok().body(Mono.just(resp), Response.class);
            }
            Response<BkUserDTO> resp = Response.buildSuccessResp(user);
            return ServerResponse.ok().body(Mono.just(resp), Response.class);
        }
    }
}
