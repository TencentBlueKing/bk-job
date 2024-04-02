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

package com.tencent.bk.job.common.web.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tencent.bk.job.common.annotation.JobInterceptor;
import com.tencent.bk.job.common.constant.HttpRequestSourceEnum;
import com.tencent.bk.job.common.constant.InterceptorOrder;
import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.i18n.locale.LocaleUtils;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.RequestUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.common.web.model.RepeatableReadWriteHttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Job通用拦截器
 */
@Slf4j
@JobInterceptor(order = InterceptorOrder.Init.HIGHEST, pathPatterns = "/**")
public class JobCommonInterceptor implements AsyncHandlerInterceptor {

    private final Tracer tracer;
    private Tracer.SpanInScope spanInScope = null;

    @Autowired
    public JobCommonInterceptor(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        JobContextUtil.setStartTime();
        JobContextUtil.setRequest(request);
        JobContextUtil.setResponse(response);

        initSpanAndAddRequestId();

        if (!shouldFilter(request)) {
            return true;
        }

        addUsername(request);
        addLang(request);

        return true;
    }

    private boolean shouldFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // 只拦截web/service/esb的API请求
        return uri.startsWith("/web/") || uri.startsWith("/service/") || uri.startsWith("/esb/");
    }

    private void initSpanAndAddRequestId() {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan == null) {
            currentSpan = tracer.nextSpan().start();
        }
        spanInScope = tracer.withSpan(currentSpan);
        String traceId = currentSpan.context().traceId();
        JobContextUtil.setRequestId(traceId);
    }

    private void addUsername(HttpServletRequest request) {
        HttpRequestSourceEnum requestSource = RequestUtil.parseHttpRequestSource(request);
        if (requestSource == HttpRequestSourceEnum.UNKNOWN) {
            return;
        }

        String username = null;
        switch (requestSource) {
            case WEB:
                username = request.getHeader("username");
                break;
            case ESB:
                // 网关从ESB JWT中解析出的Username最高优先级
                username = request.getHeader(JobCommonHeaders.USERNAME);
                log.debug("username from gateway:{}", username);
                // QueryString/Body中的Username次优先
                if (StringUtils.isBlank(username)) {
                    username = parseUsernameFromQueryStringOrBody(request);
                    log.debug("username from query/body:{}", username);
                }
                break;
        }

        if (StringUtils.isNotBlank(username)) {
            JobContextUtil.setUsername(username);
        }
    }

    private void addLang(HttpServletRequest request) {
        String userLang = request.getHeader(LocaleUtils.COMMON_LANG_HEADER);

        if (StringUtils.isNotBlank(userLang)) {
            JobContextUtil.setUserLang(userLang);
        } else {
            JobContextUtil.setUserLang(LocaleUtils.LANG_ZH_CN);
        }
    }

    private String parseUsernameFromQueryStringOrBody(HttpServletRequest request) {
        return parseValueFromQueryStringOrBody(request, "bk_username");
    }

    private String parseValueFromQueryStringOrBody(HttpServletRequest request, String key) {
        try {
            if (request.getMethod().equals(HttpMethod.POST.name())
                || request.getMethod().equals(HttpMethod.PUT.name())) {
                if (!(request instanceof RepeatableReadWriteHttpServletRequest)) {
                    return null;
                }
                RepeatableReadWriteHttpServletRequest wrapperRequest =
                    (RepeatableReadWriteHttpServletRequest) request;
                if (StringUtils.isNotBlank(wrapperRequest.getBody())) {
                    ObjectNode jsonBody = (ObjectNode) JsonUtils.toJsonNode(wrapperRequest.getBody());
                    if (jsonBody == null) {
                        return null;
                    }
                    JsonNode valueNode = jsonBody.get(key);
                    String value = (valueNode == null || valueNode.isNull()) ? null : jsonBody.get(key).asText();
                    log.debug("Parsed from POST/PUT: {}={}", key, value);
                    return value;
                }
            } else if (request.getMethod().equals(HttpMethod.GET.name())) {
                String value = request.getParameter(key);
                log.debug("Parsed from GET: {}={}", key, value);
                return value;
            }
        } catch (Exception e) {
            String msg = MessageFormatter.format(
                "Fail to parse {} from request",
                key
            ).getMessage();
            log.warn(msg, e);
        }
        return null;
    }

    @Override
    public void postHandle(@NonNull HttpServletRequest request,
                           @NonNull HttpServletResponse response,
                           @NonNull Object handler,
                           ModelAndView modelAndView) {
        if (log.isDebugEnabled()) {
            log.debug("Post handler|{}|{}|{}|{}|{}", JobContextUtil.getRequestId(),
                JobContextUtil.getAppResourceScope(),
                JobContextUtil.getUsername(), System.currentTimeMillis() - JobContextUtil.getStartTime(),
                request.getRequestURI());
        }
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        try {
            if (isClientOrServerError(response)) {
                log.warn("status {} given by {}", response.getStatus(), handler);
            }
            if (ex != null) {
                log.error(
                    "After completion|{}|{}|{}|{}|{}|{}",
                    JobContextUtil.getRequestId(),
                    response.getStatus(),
                    JobContextUtil.getUsername(),
                    System.currentTimeMillis() - JobContextUtil.getStartTime(),
                    request.getRequestURI(),
                    ex.getMessage()
                );
            } else {
                log.debug(
                    "After completion|{}|{}|{}|{}|{}",
                    JobContextUtil.getRequestId(),
                    response.getStatus(),
                    JobContextUtil.getUsername(),
                    System.currentTimeMillis() - JobContextUtil.getStartTime(),
                    request.getRequestURI()
                );
            }
        } finally {
            if (spanInScope != null) {
                spanInScope.close();
            }
            JobContextUtil.unsetContext();
        }
    }

    private boolean isClientOrServerError(HttpServletResponse response) {
        return response.getStatus() >= HttpStatus.SC_BAD_REQUEST;
    }
}
