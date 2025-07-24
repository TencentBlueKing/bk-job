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

package com.tencent.bk.job.analysis.service.login.impl;

import com.tencent.bk.job.analysis.service.login.LoginTokenService;
import com.tencent.bk.job.common.aidev.config.CustomPaasLoginProperties;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.util.JobContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@Service
public class LoginTokenServiceImpl implements LoginTokenService {
    private final CustomPaasLoginProperties customPaasLoginProperties;

    @Autowired
    public LoginTokenServiceImpl(CustomPaasLoginProperties customPaasLoginProperties) {
        this.customPaasLoginProperties = customPaasLoginProperties;
    }

    @Override
    public String getToken() {
        String token = customPaasLoginProperties.getToken();
        if (StringUtils.isNotBlank(token)) {
            return token;
        }
        if (!customPaasLoginProperties.isEnabled()) {
            return getBkToken();
        }
        return getBkTicket();
    }

    private String getBkToken() {
        return getCookieValue("bk_token");
    }

    private String getBkTicket() {
        return getCookieValue("bk_ticket");
    }

    /**
     * 从请求中获取cookie
     *
     * @param cookieName cookie名称
     * @return cookie值
     */
    private String getCookieValue(String cookieName) {
        HttpServletRequest request = JobContextUtil.getRequest();
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            String message = "Cookies is null, please check request in thread context";
            throw new InternalException(message, ErrorCode.INTERNAL_ERROR);
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        if (log.isDebugEnabled()) {
            StringBuilder cookiesSb = new StringBuilder();
            for (Cookie cookie : cookies) {
                cookiesSb.append(cookie.toString());
                cookiesSb.append(",");
            }
            log.debug("Cookies={}", cookiesSb);
        }
        String message = "Cannot find cookie by name: " + cookieName;
        throw new InternalException(message, ErrorCode.INTERNAL_ERROR);
    }
}
