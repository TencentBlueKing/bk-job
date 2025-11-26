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

package com.tencent.bk.job.gateway.web.server.filter;

import com.tencent.bk.job.gateway.web.server.AccessLogConstants;
import com.tencent.bk.job.gateway.web.server.RouteServerInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * 后端服务信息过滤器，获取后端路由信息供日志使用
 */
@Component
@Slf4j
public class RouteServerContextFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        Response<ServiceInstance> resp =
            exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR);
        RouteServerInfo rs = buildRouteInfo(exchange, resp.getServer());
        request.mutate().header(AccessLogConstants.Header.GATEWAY_UPSTREAM,
                rs != null ? rs.toString() : AccessLogConstants.Default.MISSING)
            .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    private RouteServerInfo buildRouteInfo(ServerWebExchange exchange, ServiceInstance serviceInstance) {
        if (serviceInstance == null) {
            log.debug("instance is null, No backend selected.");
            return null;
        }
        RouteServerInfo serverInfo = new RouteServerInfo();
        serverInfo.setServiceName(serviceInstance.getServiceId());
        serverInfo.setNameSpace(serviceInstance.getMetadata()
            .getOrDefault(AccessLogConstants.Default.KEY_META_NAMESPACE, AccessLogConstants.Default.MISSING));
        serverInfo.setHost(serviceInstance.getHost());
        serverInfo.setPort(serviceInstance.getPort());
        URI uri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        serverInfo.setPath(uri.getPath());
        return serverInfo;
    }

    @Override
    public int getOrder() {
        return ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER + 1;
    }
}
