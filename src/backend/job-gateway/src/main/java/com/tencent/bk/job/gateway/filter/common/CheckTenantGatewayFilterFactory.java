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

package com.tencent.bk.job.gateway.filter.common;

import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.constant.TenantIdConstants;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.RequestUtil;
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
 * 检查请求中的租户信息
 */
@Slf4j
@Component
public class CheckTenantGatewayFilterFactory
    extends AbstractGatewayFilterFactory<CheckTenantGatewayFilterFactory.Config> {

    private final TenantEnvService tenantEnvService;

    @Autowired
    public CheckTenantGatewayFilterFactory(TenantEnvService tenantEnvService) {
        super(Config.class);
        this.tenantEnvService = tenantEnvService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpResponse response = exchange.getResponse();
            ServerHttpRequest request = exchange.getRequest();

            String tenantId = extractTenantId(request);
            if (tenantId == null) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }

            // set header
            request = request.mutate()
                .header(JobCommonHeaders.BK_TENANT_ID, tenantId)
                .build();
            return chain.filter(exchange.mutate().request(request).build());
        };
    }

    private String extractTenantId(ServerHttpRequest request) {
        if (tenantEnvService.isTenantEnabled()) {
            String tenantId = RequestUtil.getHeaderValue(request, JobCommonHeaders.BK_TENANT_ID);
            if (StringUtils.isEmpty(tenantId)) {
                log.error("Missing tenant header");
                return null;
            } else {
                return tenantId;
            }
        } else {
            // 如果未开启多租户特性，设置默认租户 default（蓝鲸约定）
            return TenantIdConstants.DEFAULT_TENANT_ID;
        }
    }

    static class Config {

    }

}
