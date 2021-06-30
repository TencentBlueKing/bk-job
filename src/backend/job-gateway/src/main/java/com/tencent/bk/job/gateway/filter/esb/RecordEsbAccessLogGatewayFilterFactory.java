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

import com.tencent.bk.job.common.util.RequestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * ESB 请求与响应日志
 */
@Slf4j
@Component
public class RecordEsbAccessLogGatewayFilterFactory
    extends AbstractGatewayFilterFactory<RecordEsbAccessLogGatewayFilterFactory.Config> {

    @Autowired
    public RecordEsbAccessLogGatewayFilterFactory() {
        super(RecordEsbAccessLogGatewayFilterFactory.Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String appCode = RequestUtil.getHeaderValue(request, "Bk-App-Code");
            String username = RequestUtil.getHeaderValue(request, "Bk-Username");

            String uri = exchange.getRequest().getURI().getPath();
            String apiName = getApiNameFromUri(uri);
            exchange.getAttributes().put("start_time", System.currentTimeMillis());

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                Long startTime = exchange.getAttribute("start_time");

                long costTime = 0L;
                if (startTime != null) {
                    costTime = (System.currentTimeMillis() - startTime);
                }
                HttpStatus status = exchange.getResponse().getStatusCode();
                int statusValue = -1;
                if (status != null) {
                    statusValue = status.value();
                }
                log.info("API:{}|uri:{}|appCode:{}|username:{}|respStatus:{}|cost:{}", apiName, uri, appCode, username,
                    statusValue, costTime);
            }));
        };
    }

    private String getApiNameFromUri(String uri) {
        String[] pathPart = uri.split("/");
        return pathPart[pathPart.length - 1];
    }


    static class Config {

    }
}
