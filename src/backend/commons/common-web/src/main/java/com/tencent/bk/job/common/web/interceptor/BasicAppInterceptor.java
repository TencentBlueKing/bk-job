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

package com.tencent.bk.job.common.web.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tencent.bk.job.common.annotation.JobInterceptor;
import com.tencent.bk.job.common.constant.HttpRequestSourceEnum;
import com.tencent.bk.job.common.constant.InterceptorOrder;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.model.BasicApp;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.CommonAppService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.RequestUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.common.util.toggle.ToggleEvaluateContext;
import com.tencent.bk.job.common.util.toggle.feature.FeatureIdConstants;
import com.tencent.bk.job.common.util.toggle.feature.FeatureToggle;
import com.tencent.bk.job.common.validation.ScopeValidator;
import com.tencent.bk.job.common.web.model.RepeatableReadWriteHttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tencent.bk.job.common.constant.JobConstants.JOB_BUILD_IN_BIZ_SET_ID_MAX;
import static com.tencent.bk.job.common.constant.JobConstants.JOB_BUILD_IN_BIZ_SET_ID_MIN;

/**
 * Job 业务信息处理拦截器
 */
@Slf4j
@JobInterceptor(pathPatterns = {"/web/**", "/service/**", "/esb/api/**"},
    order = InterceptorOrder.Init.REWRITE_REQUEST)
public class BasicAppInterceptor implements AsyncHandlerInterceptor {
    private static final Pattern SCOPE_PATTERN = Pattern.compile("/scope/(\\w+)/(\\d+)");

    private static final Pattern APP_PATTERN = Pattern.compile("/app/(\\d+)");

    private final CommonAppService appService;

    private final AppParser webAppParser;

    private final AppParser esbAppParser;

    private final AppParser internalAppParser;

    public BasicAppInterceptor(CommonAppService appService) {
        this.appService = appService;
        this.webAppParser = new WebAppParser();
        this.esbAppParser = new EsbAppParser();
        this.internalAppParser = new InternalAppParser();
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if (!shouldFilter(request)) {
            return true;
        }
        addApp(request);

        return true;
    }

    private boolean shouldFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // 只拦截web/service/esb的API请求
        return uri.startsWith("/web/") || uri.startsWith("/service/") || uri.startsWith("/esb/");
    }

    private void addApp(HttpServletRequest request) {
        HttpRequestSourceEnum requestSource = RequestUtil.parseHttpRequestSource(request);
        if (requestSource == HttpRequestSourceEnum.UNKNOWN) {
            return;
        }

        BasicApp app = null;
        switch (requestSource) {
            case WEB:
                app = webAppParser.parseApp(request);
                break;
            case ESB:
                app = esbAppParser.parseApp(request);
                break;
            case INTERNAL:
                app = internalAppParser.parseApp(request);
                break;
            default:
                log.debug("Ignore invalid app: {}", requestSource);
                break;
        }

        if (log.isDebugEnabled()) {
            log.debug("Parse app, requestSource: {}, app: {}", requestSource.name(), app);
        }

        if (app != null) {
            request.setAttribute("appResourceScope", app.getAppResourceScope());
            JobContextUtil.setApp(app);
        } else {
            log.debug("Parsed app is empty");
        }
    }

    /**
     * 从 http 请求中解析 BasicApp
     */
    public interface AppParser {
        BasicApp parseApp(HttpServletRequest request);
    }

    /**
     * 从 http 请求中解析 BasicApp - Web 请求
     */
    public class WebAppParser implements AppParser {
        @Override
        public BasicApp parseApp(HttpServletRequest request) {
            ResourceScope resourceScope = parseResourceScopeFromURI(request.getRequestURI());
            if (resourceScope != null) {
                return appService.getApp(resourceScope);
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
    }

    /**
     * 从 http 请求中解析 BasicApp - ESB/蓝鲸网关请求
     */
    public class EsbAppParser implements AppParser {
        @Override
        public BasicApp parseApp(HttpServletRequest request) {
            return parseAppResourceScopeFromQueryStringOrBody(request);
        }

        private BasicApp parseAppResourceScopeFromQueryStringOrBody(HttpServletRequest request) {
            Map<String, String> params = parseMultiValueFromQueryStringOrBody(request,
                "bk_scope_type", "bk_scope_id", "bk_biz_id");
            String scopeType = params.get("bk_scope_type");
            String scopeId = params.get("bk_scope_id");
            String bizIdStr = params.get("bk_biz_id");

            if (StringUtils.isNotBlank(scopeType) && StringUtils.isNotBlank(scopeId)) {
                ScopeValidator.validate(null, scopeType, scopeId);
                // 优先使用 bk_scope_type & bk_scope_id
                return appService.getApp(new ResourceScope(scopeType, scopeId));
            }

            // 如果兼容bk_biz_id参数
            if (FeatureToggle.checkFeature(FeatureIdConstants.FEATURE_BK_BIZ_ID_COMPATIBLE,
                ToggleEvaluateContext.EMPTY)) {
                ResourceScope resourceScope = null;
                // 兼容当前业务ID参数
                if (StringUtils.isNotBlank(bizIdStr)) {
                    long bizId = Long.parseLong(bizIdStr);
                    // [8000000,9999999]是迁移业务集之前约定的业务集ID范围。为了兼容老的API调用方，在这个范围内的bizId解析为业务集
                    scopeId = bizIdStr;
                    if (bizId >= JOB_BUILD_IN_BIZ_SET_ID_MIN && bizId <= JOB_BUILD_IN_BIZ_SET_ID_MAX) {
                        resourceScope = new ResourceScope(ResourceScopeTypeEnum.BIZ_SET, scopeId);
                    } else {
                        resourceScope = new ResourceScope(ResourceScopeTypeEnum.BIZ, scopeId);
                    }
                    return appService.getApp(resourceScope);
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
                            String value = (valueNode == null || valueNode.isNull()) ? null :
                                jsonBody.get(key).asText();
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
                String msg = MessageFormatter.format(
                    "Fail to parse keys: {} from request",
                    keys
                ).getMessage();
                log.warn(msg, e);
            }
            return params;
        }
    }

    /**
     * 从 http 请求中解析 BasicApp - Job 内部请求
     */
    public class InternalAppParser implements AppParser {
        @Override
        public BasicApp parseApp(HttpServletRequest request) {
            // 优先从 path 解析
            Long appId = parseAppIdFromPath(request.getRequestURI());
            if (appId == null) {
                // 从 QueryParam解析
                appId = parseAppIdFromQueryParam(request);
            }
            if (appId == null) {
                // 从 Request Body 解析
                appId = parseAppIdFromRequestBody(request);
            }
            if (appId == null) {
                return null;
            }
            return appService.getApp(appId);
        }

        private Long parseAppIdFromPath(String requestURI) {
            Long appId = null;
            Matcher appIdMatcher = APP_PATTERN.matcher(requestURI);
            if (appIdMatcher.find()) {
                appId = Long.parseLong(appIdMatcher.group(1));
            }
            return appId;
        }

        private Long parseAppIdFromQueryParam(HttpServletRequest request) {
            String queryParam = "appId";
            String value = request.getParameter(queryParam);
            log.debug("Parsed from GET: {}={}", queryParam, value);
            return value != null ? Long.parseLong(value) : null;
        }

        private Long parseAppIdFromRequestBody(HttpServletRequest request) {
            if (!request.getMethod().equals(HttpMethod.POST.name())
                && !request.getMethod().equals(HttpMethod.PUT.name())) {
                return null;
            }
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
                String fieldName = "appId";
                JsonNode valueNode = jsonBody.get(fieldName);
                String value = (valueNode == null || valueNode.isNull()) ? null :
                    jsonBody.get(fieldName).asText();
                log.debug("Parsed from POST/PUT: {}={}", fieldName, value);
                return value != null ? Long.parseLong(value) : null;
            } else {
                return null;
            }
        }
    }
}
