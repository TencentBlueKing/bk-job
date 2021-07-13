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
import com.tencent.bk.job.common.util.RequestUtil;
import com.tencent.bk.job.gateway.model.esb.EsbJwtInfo;
import com.tencent.bk.job.gateway.service.EsbJwtService;
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
 * ESB JWT 解析与校验，用于确认ESB-API调用方式来自于ESB
 */
@Slf4j
@Component
public class CheckEsbJwtGatewayFilterFactory
    extends AbstractGatewayFilterFactory<CheckEsbJwtGatewayFilterFactory.Config> {
    private EsbJwtService esbJwtService;

    @Autowired
    public CheckEsbJwtGatewayFilterFactory(EsbJwtService esbJwtService) {
        super(Config.class);
        this.esbJwtService = esbJwtService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpResponse response = exchange.getResponse();
            ServerHttpRequest request = exchange.getRequest();
            String token = RequestUtil.getHeaderValue(request, "X-Bkapi-JWT");
            if (StringUtils.isEmpty(token)) {
                log.warn("Esb token is empty!");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }
            EsbJwtInfo authInfo = esbJwtService.extractFromJwt(token);
            if (authInfo == null) {
                log.warn("Untrusted esb request, request-id:{}", RequestUtil.getHeaderValue(request,
                    "X-Bkapi-Request-Id"));
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }

            // set app code header
            request.mutate().header(JobCommonHeaders.APP_CODE, new String[]{authInfo.getAppCode()}).build();
            request.mutate().header(JobCommonHeaders.USERNAME, new String[]{authInfo.getUsername()}).build();
            return chain.filter(exchange.mutate().request(request).build());
        };
    }

    static class Config {

    }

}
