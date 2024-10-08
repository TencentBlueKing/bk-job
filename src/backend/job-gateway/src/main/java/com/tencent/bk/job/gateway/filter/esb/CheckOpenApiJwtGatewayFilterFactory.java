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

package com.tencent.bk.job.gateway.filter.esb;

import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.crypto.util.RSAUtils;
import com.tencent.bk.job.common.security.autoconfigure.ServiceSecurityProperties;
import com.tencent.bk.job.common.service.SpringProfile;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.RequestUtil;
import com.tencent.bk.job.gateway.model.esb.EsbJwtInfo;
import com.tencent.bk.job.gateway.service.OpenApiJwtService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

/**
 * OPEN API JWT 解析与校验
 */
@Slf4j
@Component
public class CheckOpenApiJwtGatewayFilterFactory
    extends AbstractGatewayFilterFactory<CheckOpenApiJwtGatewayFilterFactory.Config> {
    private final OpenApiJwtService openApiJwtService;
    private final SpringProfile springProfile;
    private final ServiceSecurityProperties securityProperties;

    @Autowired
    public CheckOpenApiJwtGatewayFilterFactory(OpenApiJwtService openApiJwtService,
                                               SpringProfile springProfile,
                                               ServiceSecurityProperties securityProperties) {
        super(Config.class);
        this.openApiJwtService = openApiJwtService;
        this.springProfile = springProfile;
        this.securityProperties = securityProperties;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpResponse response = exchange.getResponse();
            ServerHttpRequest request = exchange.getRequest();

            String requestFrom = RequestUtil.getHeaderValue(request, JobCommonHeaders.BK_GATEWAY_FROM);
            if (log.isDebugEnabled()) {
                log.debug("Open api request from : {}",
                    StringUtils.isNotEmpty(requestFrom) ? requestFrom : "bk-job-esb");
            }
            JobContextUtil.setRequestFrom(requestFrom);
            String token = RequestUtil.getHeaderValue(request, JobCommonHeaders.BK_GATEWAY_JWT);
            if (StringUtils.isEmpty(token)) {
                log.warn("Jwt token is empty! requestFrom={}", requestFrom);
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }

            EsbJwtInfo authInfo;
            if (isOpenApiTestActive(request)) {
                // 如果是 OpenApi 测试请求，使用 Job 的 JWT 认证方式，不使用 ESB JWT（避免依赖 ESB)
                authInfo = openApiJwtService.extractFromJwt(token,
                    RSAUtils.getPublicKey(securityProperties.getPublicKeyBase64()));
            } else {
                authInfo = openApiJwtService.extractFromJwt(token);
            }

            if (authInfo == null) {
                log.warn("Untrusted esb request, request-id:{}", RequestUtil.getHeaderValue(request,
                    JobCommonHeaders.BK_GATEWAY_REQUEST_ID));
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }

            // set app code header
            request.mutate().header(JobCommonHeaders.APP_CODE, new String[]{authInfo.getAppCode()}).build();
            request.mutate().header(JobCommonHeaders.USERNAME, new String[]{authInfo.getUsername()}).build();
            return chain.filter(exchange.mutate().request(request).build());
        };
    }

    private boolean isOpenApiTestActive(ServerHttpRequest request) {
        if (!springProfile.isProfileActive("openApiTestEnv")) {
            return false;
        }
        String value = RequestUtil.getHeaderValue(request, "X-JOB-OPENAPI-TEST");
        return StringUtils.isNotEmpty(value) && value.equalsIgnoreCase("true");
    }

    static class Config {

    }

}
