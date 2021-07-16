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

import com.tencent.bk.job.common.security.autoconfigure.ServiceSecurityProperties;
import com.tencent.bk.job.common.util.jwt.JwtManager;
import com.tencent.bk.job.common.web.exception.ServiceNoAuthException;
import com.tencent.bk.job.common.web.util.ProfileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 服务认证拦截器
 */
@Slf4j
@Component
public class ServiceSecurityInterceptor extends HandlerInterceptorAdapter {
    private final JwtManager jwtManager;
    private final ProfileUtil profileUtil;
    private final ServiceSecurityProperties securityProperties;

    @Autowired
    public ServiceSecurityInterceptor(JwtManager jwtManager, ProfileUtil profileUtil,
                                      ServiceSecurityProperties securityProperties) {
        this.jwtManager = jwtManager;
        this.profileUtil = profileUtil;
        this.securityProperties = securityProperties;
    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (shouldFilter(request)) {
            String jwt = request.getHeader("x-job-auth-token");
            if (StringUtils.isEmpty(jwt)) {
                log.warn("Invalid request, jwt is empty! url: {}", request.getRequestURI());
                throw new ServiceNoAuthException();
            }
            boolean checkResult = jwtManager.verifyJwt(jwt);
            if (!checkResult) {
                log.warn("Invalid request, jwt is invalid or expired! url: {}", request.getRequestURI());
                throw new ServiceNoAuthException();
            }
        }
        return true;
    }

    private boolean shouldFilter(HttpServletRequest request) {
        // dev环境需要支持swagger，请求无需认证
        if (profileUtil.isDevProfileActive()) {
            return false;
        }

        String uri = request.getRequestURI();
        // 只拦截web/service/esb的API请求
        return uri.startsWith("/web/") || uri.startsWith("/service/") || uri.startsWith("/esb/");
    }
}
