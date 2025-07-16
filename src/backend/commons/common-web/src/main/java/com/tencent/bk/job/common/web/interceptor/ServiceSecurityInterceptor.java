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

import com.tencent.bk.job.common.annotation.JobInterceptor;
import com.tencent.bk.job.common.constant.InterceptorOrder;
import com.tencent.bk.job.common.jwt.JwtManager;
import com.tencent.bk.job.common.security.consts.JwtConsts;
import com.tencent.bk.job.common.security.exception.ServiceNoAuthException;
import com.tencent.bk.job.common.service.SpringProfile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 服务间调用请求认证拦截器
 */
@Slf4j
@JobInterceptor(pathPatterns = "/**", order = InterceptorOrder.Init.CHECK_VALID)
public class ServiceSecurityInterceptor extends HandlerInterceptorAdapter {
    private final SpringProfile springProfile;
    private final JwtManager jwtManager;

    @Autowired
    public ServiceSecurityInterceptor(JwtManager jwtManager, SpringProfile springProfile) {
        this.jwtManager = jwtManager;
        this.springProfile = springProfile;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (shouldFilter(request)) {
            String jwt = request.getHeader(JwtConsts.HEADER_KEY_SERVICE_JWT_TOKEN);
            if (StringUtils.isEmpty(jwt)) {
                log.error("Invalid request, jwt is empty! url: {}", request.getRequestURI());
                throw new ServiceNoAuthException();
            }
            boolean checkResult = jwtManager.verifyJwt(jwt);
            if (!checkResult) {
                log.error("Invalid request, jwt is invalid or expired! url: {}", request.getRequestURI());
                throw new ServiceNoAuthException();
            }
        }
        return true;
    }

    public boolean shouldFilter(HttpServletRequest request) {
        // dev环境需要支持swagger，请求无需认证
        if (springProfile.isDevProfileActive()) {
            return false;
        }

        String uri = request.getServletPath();
        // 只拦截web/service/esb/remote的API请求
        return uri.startsWith("/web/")
            || uri.startsWith("/service/")
            || uri.startsWith("/esb/")
            || uri.startsWith("/remote/");
    }
}
