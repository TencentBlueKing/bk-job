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

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.HttpHandlerDecoratorFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

/**
 * 清除上游系统传入的 W3C Trace Context 请求头（traceparent/tracestate），
 * 确保 job-gateway 作为链路追踪的起点，由框架的 context-propagation 自动生成新的 traceId。
 *
 * <p>为什么使用 {@link HttpHandlerDecoratorFactory} 而非 WebFilter？</p>
 * <p>在 Spring 6.1+ / Spring Boot 3.x 中，HTTP server 的 observation（tracing）不再通过 WebFilter 实现，
 * 而是在 {@code HttpWebHandlerAdapter.handle()} 中通过 Reactor 的 {@code .tap()} 操作符处理。
 * {@code ServerRequestObservationContext} 在 WebFilter 链执行之前就已创建，并持有原始 request 的引用。
 * 因此，即使在 WebFilter 中 mutate request 清除了 traceparent，observation 仍然会从原始 request 中读取它。</p>
 *
 * <p>{@link HttpHandlerDecoratorFactory} 创建的装饰器包装在 {@code HttpWebHandlerAdapter} 之外
 * （参见 {@code WebHttpHandlerBuilder.build()}），会在 {@code HttpWebHandlerAdapter.handle()} 之前执行。
 * 这样在 {@code ServerRequestObservationContext} 创建时，request 中已经没有 traceparent 了，
 * {@code PropagatingReceiverTracingObservationHandler} 就无法从中提取 parent trace context，
 * 从而由框架自动生成全新的 traceId。</p>
 *
 * <p>典型场景：别的系统 -> bkapigw -> job-gateway -> job-execute，
 * 外部系统请求时携带了 traceparent，如果不清除，job-gateway 及后续微服务都会使用该 traceId，
 * 这不符合预期（job 应该有自己独立的 traceId）。</p>
 */
@Component
@Slf4j
public class StripUpstreamTraceHttpHandlerDecorator implements HttpHandlerDecoratorFactory {

    private static final String HEADER_TRACEPARENT = "traceparent";
    private static final String HEADER_TRACESTATE = "tracestate";

    @Override
    public HttpHandler apply(HttpHandler httpHandler) {
        return (ServerHttpRequest request, ServerHttpResponse response) -> {
            // 仅当请求中包含 traceparent 时才进行清理
            if (request.getHeaders().containsKey(HEADER_TRACEPARENT)) {
                if (log.isDebugEnabled()) {
                    log.debug("Stripping upstream trace headers (traceparent/tracestate) from request: {}",
                        request.getURI().getPath());
                }
                ServerHttpRequest mutatedRequest = request.mutate()
                    .headers(headers -> {
                        headers.remove(HEADER_TRACEPARENT);
                        headers.remove(HEADER_TRACESTATE);
                    })
                    .build();
                return httpHandler.handle(mutatedRequest, response);
            }
            return httpHandler.handle(request, response);
        };
    }
}
