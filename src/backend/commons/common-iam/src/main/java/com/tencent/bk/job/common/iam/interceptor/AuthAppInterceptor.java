/*
 * Tencent is pleased to support the open source community by making BK-JOB 蓝鲸作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB 蓝鲸作业平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.job.common.iam.interceptor;

import com.tencent.bk.job.common.annotation.JobInterceptor;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.InterceptorOrder;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.BusinessAuthService;
import com.tencent.bk.job.common.model.BasicApp;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.util.JobContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@JobInterceptor(pathPatterns = {"/web/**", "/esb/api/**"},
    order = InterceptorOrder.AUTH.AUTH_GLOBAL)
public class AuthAppInterceptor extends HandlerInterceptorAdapter {

    private final BusinessAuthService businessAuthService;

    public AuthAppInterceptor(BusinessAuthService businessAuthService) {
        this.businessAuthService = businessAuthService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String url = request.getRequestURI();
        User user = JobContextUtil.getUser();
        if (user == null) {
            log.debug("Can not find user for url:{}", url);
            return true;
        }
        BasicApp app = JobContextUtil.getApp();
        if (app != null) {
            checkAppTenant(app, user.getTenantId());
            AppResourceScope appResourceScope = new AppResourceScope(app.getId(), app.getScope());
            log.debug("Auth {} access_business {}", user.getUsername(), appResourceScope);
            AuthResult authResult = businessAuthService.authAccessBusiness(user, appResourceScope);
            if (!authResult.isPass()) {
                throw new PermissionDeniedException(authResult);
            }
        } else {
            log.debug("Ignore auth {} access_business public scope", user.getUsername());
        }
        return true;
    }

    private void checkAppTenant(BasicApp app, String tenantId) {
        if (!app.getTenantId().equals(tenantId)) {
            log.warn("App is not belong to tenant, app: {}, userTenantId: {}",
                app, tenantId);
            throw new NotFoundException(ErrorCode.APP_NOT_EXIST);
        }
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) {
    }
}
