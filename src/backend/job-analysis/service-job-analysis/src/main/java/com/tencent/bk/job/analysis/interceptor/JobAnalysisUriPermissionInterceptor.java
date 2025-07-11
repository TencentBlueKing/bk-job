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

package com.tencent.bk.job.analysis.interceptor;

import com.tencent.bk.job.analysis.consts.AnalysisConsts;
import com.tencent.bk.job.common.annotation.JobInterceptor;
import com.tencent.bk.job.common.constant.InterceptorOrder;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.util.JobContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Uri权限控制拦截
 */
@Slf4j
@Component
@JobInterceptor(pathPatterns = {"/web/statistics/**"},
    order = InterceptorOrder.AUTH.AUTH_COMMON)
public class JobAnalysisUriPermissionInterceptor extends HandlerInterceptorAdapter {
    private final String URI_PATTERN_WEB_STATISTICS = "/web/statistics/**";
    private final AuthService authService;
    private final PathMatcher pathMatcher;

    @Autowired
    public JobAnalysisUriPermissionInterceptor(AuthService authService) {
        this.authService = authService;
        this.pathMatcher = new AntPathMatcher();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String username = JobContextUtil.getUsername();
        String uri = request.getRequestURI();
        if (pathMatcher.match(URI_PATTERN_WEB_STATISTICS, uri)) {
            AuthResult authResult = authService.auth(
                username,
                ActionId.DASHBOARD_VIEW,
                ResourceTypeEnum.DASHBOARD_VIEW,
                AnalysisConsts.GLOBAL_DASHBOARD_VIEW_ID,
                null
            );
            if (!authResult.isPass()) {
                throw new PermissionDeniedException(authResult);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {

    }
}
