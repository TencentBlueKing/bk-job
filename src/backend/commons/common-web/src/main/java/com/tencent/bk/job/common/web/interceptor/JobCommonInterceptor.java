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
import com.tencent.bk.job.common.constant.HttpRequestSourceEnum;
import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.i18n.locale.LocaleUtils;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.feature.FeatureIdConstants;
import com.tencent.bk.job.common.util.feature.FeatureToggle;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.common.web.model.RepeatableReadWriteHttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tencent.bk.job.common.constant.JobConstants.JOB_BUILD_IN_BIZ_SET_ID_MAX;
import static com.tencent.bk.job.common.constant.JobConstants.JOB_BUILD_IN_BIZ_SET_ID_MIN;

/**
 * Job通用拦截器
 */
@Slf4j
@Component
public class JobCommonInterceptor implements AsyncHandlerInterceptor {
    private static final Pattern SCOPE_PATTERN = Pattern.compile("/scope/(\\w+)/(\\d+)");

    private final Tracer tracer;
    private Tracer.SpanInScope spanInScope = null;

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
        addAppResourceScope(request);

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
        HttpRequestSourceEnum requestSource = getHttpRequestSource(request);
        if (requestSource == null) {
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

    private HttpRequestSourceEnum getHttpRequestSource(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/web/")) {
            return HttpRequestSourceEnum.WEB;
        } else if (uri.startsWith("/esb/")) {
            return HttpRequestSourceEnum.ESB;
        } else if (uri.startsWith("/service/")) {
            return HttpRequestSourceEnum.INTERNAL;
        }
        return null;
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
        HttpRequestSourceEnum requestSource = getHttpRequestSource(request);
        if (requestSource == null) {
            return;
        }

        AppResourceScope appResourceScope = null;
        switch (requestSource) {
            case WEB:
                appResourceScope = parseAppResourceScopeFromPath(request.getRequestURI());
                log.debug("Scope from path:{}", appResourceScope);
                break;
            case ESB:
                appResourceScope = parseAppResourceScopeFromQueryStringOrBody(request);
                log.debug("Scope from query/body:{}", appResourceScope);
        }
        if (appResourceScope != null) {
            request.setAttribute("appResourceScope", appResourceScope);
            JobContextUtil.setAppResourceScope(appResourceScope);
        }
    }

    private AppResourceScope parseAppResourceScopeFromPath(String requestURI) {
        ResourceScope resourceScope = parseResourceScopeFromURI(requestURI);
        if (resourceScope != null) {
            return buildAppResourceScope(resourceScope);
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

    private AppResourceScope buildAppResourceScope(ResourceScope resourceScope) {
        Long appId = appScopeMappingService.getAppIdByScope(resourceScope);
        return new AppResourceScope(appId, resourceScope);
    }

    private String parseUsernameFromQueryStringOrBody(HttpServletRequest request) {
        return parseValueFromQueryStringOrBody(request, "bk_username");
    }

    private AppResourceScope parseAppResourceScopeFromQueryStringOrBody(HttpServletRequest request) {
        Map<String, String> params = parseMultiValueFromQueryStringOrBody(request, "bk_scope_type", "bk_scope_id",
            "bk_biz_id");
        String scopeType = params.get("bk_scope_type");
        String scopeId = params.get("bk_scope_id");
        String bizIdStr = params.get("bk_biz_id");

        if (StringUtils.isNotBlank(scopeType) && StringUtils.isNotBlank(scopeId)) {
            return new AppResourceScope(scopeType, scopeId, null);
        }

        // 如果兼容bk_biz_id参数
        if (FeatureToggle.checkFeature(FeatureIdConstants.FEATURE_BK_BIZ_ID_COMPATIBLE, null)) {
            // 兼容当前业务ID参数
            if (StringUtils.isNotBlank(bizIdStr)) {
                long bizId = Long.parseLong(bizIdStr);
                // [8000000,9999999]是迁移业务集之前约定的业务集ID范围。为了兼容老的API调用方，在这个范围内的bizId解析为业务集
                scopeId = bizIdStr;
                if (bizId >= JOB_BUILD_IN_BIZ_SET_ID_MIN && bizId <= JOB_BUILD_IN_BIZ_SET_ID_MAX) {
                    Long appId = appScopeMappingService.getAppIdByScope(ResourceScopeTypeEnum.BIZ_SET.getValue(),
                        scopeId);
                    return new AppResourceScope(ResourceScopeTypeEnum.BIZ_SET, scopeId, appId);
                } else {
                    Long appId = appScopeMappingService.getAppIdByScope(ResourceScopeTypeEnum.BIZ.getValue(), scopeId);
                    return new AppResourceScope(ResourceScopeTypeEnum.BIZ, scopeId, appId);
                }
            }
        }
        // 其他情况返回null，后续拦截器会处理null
        return null;
    }

    /**
     * 从请求的解析多个参数
     *
     * @param request http请求
     * @param keys    参数名称
     * @return Map<paramName, paramValue>
     */
    private Map<String, String> parseMultiValueFromQueryStringOrBody(HttpServletRequest request, String... keys) {
        Map<String, String> params = new HashMap<>();
        try {
            if (request.getMethod().equals(HttpMethod.POST.name())
                || request.getMethod().equals(HttpMethod.PUT.name())) {
                if (!(request instanceof RepeatableReadWriteHttpServletRequest)) {
                    return params;
                }
                RepeatableReadWriteHttpServletRequest wrapperRequest =
                    (RepeatableReadWriteHttpServletRequest) request;
                if (StringUtils.isNotBlank(wrapperRequest.getBody())) {
                    ObjectNode jsonBody = (ObjectNode) JsonUtils.toJsonNode(wrapperRequest.getBody());
                    if (jsonBody == null) {
                        return params;
                    }
                    for (String key : keys) {
                        JsonNode valueNode = jsonBody.get(key);
                        String value = (valueNode == null || valueNode.isNull()) ? null : jsonBody.get(key).asText();
                        log.debug("Parsed from POST/PUT: {}={}", key, value);
                        if (value != null) {
                            params.put(key, value);
                        }
                    }
                }
            } else if (request.getMethod().equals(HttpMethod.GET.name())) {
                for (String key : keys) {
                    String value = request.getParameter(key);
                    log.debug("Parsed from GET: {}={}", key, value);
                    if (value != null) {
                        params.put(key, value);
                    }
                }
            }
            return params;
        } catch (Exception e) {
            log.warn("Fail to parse keys: {} from request", keys, e);
        }
        return params;
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
            log.warn("Fail to parse {} from request", key, e);
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
                log.error("After completion|{}|{}|{}|{}|{}|{}|{}", JobContextUtil.getRequestId(), response.getStatus(),
                    JobContextUtil.getAppResourceScope(),
                    JobContextUtil.getUsername(), System.currentTimeMillis() - JobContextUtil.getStartTime(),
                    request.getRequestURI(), ex.getMessage());
            } else {
                log.debug("After completion|{}|{}|{}|{}|{}|{}", JobContextUtil.getRequestId(), response.getStatus(),
                    JobContextUtil.getAppResourceScope(),
                    JobContextUtil.getUsername(), System.currentTimeMillis() - JobContextUtil.getStartTime(),
                    request.getRequestURI());
            }
        } finally {
            if (spanInScope != null) {
                spanInScope.close();
            }
            JobContextUtil.unsetContext();
        }
    }

    private boolean isClientOrServerError(HttpServletResponse response) {
        return response.getStatus() > HttpStatus.SC_BAD_REQUEST;
    }
}
