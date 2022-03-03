/*
 * Tencent is pleased to support the open source community by making BK-JOB 蓝鲸作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

import com.tencent.bk.job.common.app.ResourceScope;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.iam.util.IamUtil;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class AuthAppInterceptor extends HandlerInterceptorAdapter {

    private final AuthService authService;

    @Autowired
    public AuthAppInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String url = request.getRequestURI();
        Pair<String, ResourceScope> userScopePair = null;
        userScopePair = findUserAndScope();
        if (userScopePair != null) {
            String username = userScopePair.getLeft();
            ResourceScope resourceScope = userScopePair.getRight();
            if (resourceScope != null && Long.parseLong(resourceScope.getId()) != JobConstants.PUBLIC_APP_ID) {
                log.debug("auth {} access_business {}", username, resourceScope);
                PathInfoDTO pathInfo = new PathInfoDTO();
                pathInfo.setType(IamUtil.getIamResourceTypeIdForResourceScope(resourceScope));
                pathInfo.setId(resourceScope.getId());
                AuthResult authResult = authService.auth(true, username, ActionId.LIST_BUSINESS,
                                                         ResourceTypeEnum.BUSINESS, resourceScope.getId(), pathInfo);
                if (!authResult.isPass()) {
                    throw new PermissionDeniedException(authResult);
                }
            } else {
                log.info("ignore auth {} access_business public scope {}", username, resourceScope);
            }
        } else {
            log.debug("can not find username/scope for url:{}", url);
        }
        return true;
    }

    private Pair<String, ResourceScope> findUserAndScope() {
        String username = JobContextUtil.getUsername();
        ResourceScope resourceScope = JobContextUtil.getScope();
        if (username != null && resourceScope != null) {
            return Pair.of(username, resourceScope);
        }
        return null;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) {
    }
}
