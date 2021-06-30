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
import com.tencent.bk.job.common.i18n.locale.LocaleUtils;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.web.controller.AbstractJobController;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @since 6/11/2019 10:46
 */
@Slf4j
@Component
public class JobCommonInterceptor extends HandlerInterceptorAdapter {
    private static final Pattern APP_ID_PATTERN = Pattern.compile("/app/(\\d+)");
    private final Tracer tracer;

    @Autowired
    public JobCommonInterceptor(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws Exception {
        JobContextUtil.setStartTime();

        String traceId = tracer.currentSpan().context().traceIdString();
        JobContextUtil.setRequestId(traceId);

        if (!shouldFilter(request)) {
            return true;
        }

        JobContextUtil.setRequest(request);
        JobContextUtil.setResponse(response);

        String username = request.getHeader("username");
        if (StringUtils.isNotBlank(username)) {
            JobContextUtil.setUsername(username);
        }

        String userLang = request.getHeader(LocaleUtils.COMMON_LANG_HEADER);

        if (StringUtils.isNotBlank(userLang)) {
            JobContextUtil.setUserLang(userLang);
        } else {
            JobContextUtil.setUserLang(LocaleUtils.LANG_ZH_CN);
        }

        long appId = parseAppId(request.getRequestURI());
        JobContextUtil.setAppId(appId);
        return preService(request, response, handler);
    }

    private boolean shouldFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // 只拦截web/service/esb的API请求
        if (uri.startsWith("/web/") || uri.startsWith("/service/") || uri.startsWith("/esb/")) {
            return true;
        }
        return false;
    }

    private long parseAppId(String requestURI) {
        Matcher matcher = APP_ID_PATTERN.matcher(requestURI);
        if (matcher.find()) {
            String appIdStr = matcher.group(1);
            long appId = 0;
            try {
                appId = Long.parseLong(appIdStr);
            } catch (NumberFormatException e) {
                log.error("Error while parse app id!|{}|{}", requestURI, appIdStr);
            }
            return appId;
        }
        return 0;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) {
        if (log.isDebugEnabled()) {
            log.debug("Post handler|{}|{}|{}|{}|{}", JobContextUtil.getRequestId(), JobContextUtil.getAppId(),
                JobContextUtil.getUsername(), System.currentTimeMillis() - JobContextUtil.getStartTime(),
                request.getRequestURI());
        }
        if (handler instanceof HandlerMethod) {
            postService(request, response, (HandlerMethod) handler);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        int status = response.getStatus();
        if (status >= 400) {
            log.warn("status {} given by {}", status, handler);
        }
        if (ex != null) {
            log.error("After completion|{}|{}|{}|{}|{}|{}", JobContextUtil.getRequestId(), response.getStatus(),
                JobContextUtil.getAppId(),
                JobContextUtil.getUsername(), System.currentTimeMillis() - JobContextUtil.getStartTime(),
                request.getRequestURI(), ex);
        } else {
            log.debug("After completion|{}|{}|{}|{}|{}|{}", JobContextUtil.getRequestId(), response.getStatus(),
                JobContextUtil.getAppId(),
                JobContextUtil.getUsername(), System.currentTimeMillis() - JobContextUtil.getStartTime(),
                request.getRequestURI());
        }
        JobContextUtil.unsetContext();
    }

    private AbstractJobController getControllerInstance(HandlerMethod handler) {
        Class<?> declaringClass = handler.getMethod().getDeclaringClass();
        Object declaringClassInstance = ApplicationContextRegister.getBean(declaringClass);
        if (declaringClassInstance instanceof AbstractJobController) {
            return (AbstractJobController) declaringClassInstance;
        }
        return null;
    }

    private boolean preService(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod) {
            try {
                AbstractJobController controllerInstance = getControllerInstance((HandlerMethod) handler);
                if (controllerInstance != null) {
                    return controllerInstance.preService(request, response, (HandlerMethod) handler);
                }
            } catch (Exception e) {
                log.error("Error while calling pre service method!", e);
            }
            return true;
        } else {
            return true;
        }
    }

    private void postService(HttpServletRequest request, HttpServletResponse response, HandlerMethod handler) {
        try {
            AbstractJobController controllerInstance = getControllerInstance(handler);
            if (controllerInstance != null) {
                controllerInstance.postService(request, response, handler);
            }
        } catch (Exception e) {
            log.error("Error while calling post service method!", e);
        }
    }

}
