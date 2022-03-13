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

import brave.Tracer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tencent.bk.job.common.annotation.DeprecatedAppLogic;
import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.i18n.locale.LocaleUtils;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.common.web.model.RepeatableReadWriteHttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Job通用拦截器
 */
@Slf4j
@Component
public class JobCommonInterceptor extends HandlerInterceptorAdapter {
    private static final Pattern SCOPE_PATTERN = Pattern.compile("/scope/(\\w+)/(\\d+)");
    @DeprecatedAppLogic
    private static final Pattern APP_ID_PATTERN = Pattern.compile("/app/(\\d+)");

    private final Tracer tracer;

    private AppScopeMappingService appScopeMappingService;

    /**
     * 通过Set方式，同时使用@Lazy，避免Bean循环依赖
     *
     * @param appScopeMappingService 依赖的AppScopeMappingService
     */
    @Autowired(required = false)
    @Lazy
    public void setAppScopeMappingService(AppScopeMappingService appScopeMappingService) {
        this.appScopeMappingService = appScopeMappingService;
    }

    @Autowired
    public JobCommonInterceptor(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        JobContextUtil.setStartTime();
        JobContextUtil.setRequest(request);
        JobContextUtil.setResponse(response);

        addRequestId();

        if (!shouldFilter(request)) {
            return true;
        }

        addUsername(request);
        addLang(request);
        addAppResourceScope(request);

        return true;
    }

    private boolean shouldFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // 只拦截web/service/esb的API请求
        return uri.startsWith("/web/") || uri.startsWith("/service/") || uri.startsWith("/esb/");
    }

    private void addRequestId() {
        String traceId = tracer.currentSpan().context().traceIdString();
        JobContextUtil.setRequestId(traceId);
    }

    private void addUsername(HttpServletRequest request) {
        // Web接口Header
        String username = request.getHeader("username");
        // ESB接口Header
        // 网关从JWT中解析出的Username最高优先级
        if (StringUtils.isBlank(username)) {
            username = request.getHeader(JobCommonHeaders.USERNAME);
            log.debug("username from gateway:{}", username);
        }
        // QueryString/Body中的Username次优先
        if (StringUtils.isBlank(username)) {
            username = parseUsernameFromQueryStringOrBody(request);
            log.debug("username from query/body:{}", username);
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

    private void addAppResourceScope(HttpServletRequest request) {
        AppResourceScope appResourceScope = parseAppResourceScopeFromPath(request.getRequestURI());
        log.debug("Scope from path:{}", appResourceScope);
        if (appResourceScope != null) {
            JobContextUtil.setAppResourceScope(appResourceScope);
        }
    }

    private AppResourceScope parseAppResourceScopeFromPath(String requestURI) {
        ResourceScope resourceScope = parseResourceScopeFromURI(requestURI);
        if (resourceScope != null) {
            return buildAppResourceScope(resourceScope);
        }

        // 兼容当前业务ID路径模式
        Long appId = parseAppIdFromURI(requestURI);
        if (appId != null) {
            return buildAppResourceScope(appId);
        }

        return null;
    }

    private ResourceScope parseResourceScopeFromURI(String requestURI) {
        ResourceScope resourceScope = null;
        Matcher scopeMatcher = SCOPE_PATTERN.matcher(requestURI);
        if (scopeMatcher.find()) {
            resourceScope = new ResourceScope(scopeMatcher.group(1), scopeMatcher.group(2));
        }
        return resourceScope;
    }

    private Long parseAppIdFromURI(String requestURI) {
        Matcher appIdMatcher = APP_ID_PATTERN.matcher(requestURI);
        Long appId = null;
        if (appIdMatcher.find()) {
            appId = Long.valueOf(appIdMatcher.group(1));
        }
        return appId;
    }

    private AppResourceScope buildAppResourceScope(ResourceScope resourceScope) {
        Long appId = appScopeMappingService.getAppIdByScope(resourceScope);
        return new AppResourceScope(appId, resourceScope);
    }

    private AppResourceScope buildAppResourceScope(Long appId) {
        ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(appId);
        return new AppResourceScope(appId, resourceScope);
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
                    String value = jsonBody.get(key) == null ? null : jsonBody.get(key).asText();
                    log.debug("parsed from POST/PUT: {}={}", key, value);
                    return value;
                }
            } else if (request.getMethod().equals(HttpMethod.GET.name())) {
                String value = request.getParameter(key);
                log.debug("parsed from GET: {}={}", key, value);
                return value;
            }
        } catch (Exception e) {
            log.warn("Fail to parse {} from request", key, e);
        }
        return null;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) {
        if (log.isDebugEnabled()) {
            log.debug("Post handler|{}|{}|{}|{}|{}", JobContextUtil.getRequestId(),
                JobContextUtil.getAppResourceScope(),
                JobContextUtil.getUsername(), System.currentTimeMillis() - JobContextUtil.getStartTime(),
                request.getRequestURI());
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        try {
            if (isClientOrServerError(response)) {
                log.warn("status {} given by {}", response.getStatus(), handler);
            }
            if (ex != null) {
                log.error("After completion|{}|{}|{}|{}|{}|{}", JobContextUtil.getRequestId(), response.getStatus(),
                    JobContextUtil.getAppResourceScope(),
                    JobContextUtil.getUsername(), System.currentTimeMillis() - JobContextUtil.getStartTime(),
                    request.getRequestURI(), ex);
            } else {
                log.debug("After completion|{}|{}|{}|{}|{}|{}", JobContextUtil.getRequestId(), response.getStatus(),
                    JobContextUtil.getAppResourceScope(),
                    JobContextUtil.getUsername(), System.currentTimeMillis() - JobContextUtil.getStartTime(),
                    request.getRequestURI());
            }
        } finally {
            JobContextUtil.unsetContext();
        }
    }

    private boolean isClientOrServerError(HttpServletResponse response) {
        return response.getStatus() > HttpStatus.SC_BAD_REQUEST;
    }
}
