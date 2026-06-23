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

package com.tencent.bk.job.manage.service.token.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.paas.config.condition.ConditionalOnCustomLoginEnable;
import com.tencent.bk.job.common.util.http.HttpConPoolUtil;
import com.tencent.bk.job.common.util.http.HttpResponse;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.config.PublicAppProperties;
import com.tencent.bk.job.manage.model.dto.PersonalAccessTokenDTO;
import com.tencent.bk.job.manage.service.token.PersonalAccessTokenProvider;
import com.tencent.bk.job.manage.service.token.model.AuthApiTokenResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 内部版个人访问凭证生成实现，调用 auth_api（bkoauth）。
 * 仅在开启自定义登录（paas.login.custom.enabled=true，使用 bk_ticket）时装配。
 */
@Slf4j
@Component
@ConditionalOnCustomLoginEnable
public class AuthApiAccessTokenProvider implements PersonalAccessTokenProvider {

    private static final String API_TOKEN = "/token/";

    private final PublicAppProperties publicAppProperties;
    private final String bkAuthApiUrl;

    public AuthApiAccessTokenProvider(PublicAppProperties publicAppProperties,
                                      @Value("${bkAuthApiUrl:}") String bkAuthApiUrl) {
        this.publicAppProperties = publicAppProperties;
        this.bkAuthApiUrl = bkAuthApiUrl;
    }

    @Override
    public PersonalAccessTokenDTO generate(String username, String bkTicket, String bkToken) {
        if (StringUtils.isBlank(bkTicket)) {
            throw new InvalidParamException(ErrorCode.PERSONAL_ACCESS_TOKEN_LOGIN_TICKET_MISSING);
        }
        if (StringUtils.isBlank(bkAuthApiUrl)) {
            throw new FailedPreconditionException(ErrorCode.PUBLIC_APP_PERSONAL_TOKEN_NOT_AVAILABLE);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("app_code", publicAppProperties.getCode());
        body.put("app_secret", publicAppProperties.getSecret());
        body.put("grant_type", "authorization_code");
        body.put("rtx", username);
        body.put("bk_ticket", bkTicket);
        body.put("need_new_token", 0);

        String url = StringUtils.stripEnd(bkAuthApiUrl, "/") + API_TOKEN;
        AuthApiTokenResp resp = requestToken(url, body);
        if (resp == null || !Boolean.TRUE.equals(resp.getResult()) || resp.getData() == null) {
            String reason = resp != null ? resp.getMessage() : "empty response";
            log.warn("Generate personal access token by auth_api fail, username={}, reason={}", username, reason);
            throw new InternalException(
                ErrorCode.GENERATE_PERSONAL_ACCESS_TOKEN_FAIL,
                new Object[]{reason}
            );
        }

        AuthApiTokenResp.TokenData data = resp.getData();
        return PersonalAccessTokenDTO.builder()
            .accessToken(data.getAccessToken())
            .expiresIn(data.getExpiresIn())
            .refreshToken(data.getRefreshToken())
            .build();
    }

    private AuthApiTokenResp requestToken(String url, Map<String, Object> body) {
        String respStr;
        String bodyStr = JsonUtils.toJson(body);
        try {
            HttpResponse response = HttpConPoolUtil.post(
                url,
                bodyStr,
                new BasicHeader("Content-Type", "application/json")
            );
            int statusCode = response != null ? response.getStatusCode() : -1;
            respStr = response != null ? response.getEntity() : null;
            if (log.isDebugEnabled()) {
                log.debug(
                    "Request auth_api token, url={}, body={}, statusCode={}, resp={}",
                    url,
                    bodyStr,
                    statusCode,
                    respStr
                );
            }
            if (response == null || response.getStatusCode() != HttpStatus.SC_OK
                || StringUtils.isBlank(response.getEntity())) {
                log.warn("Request auth_api token fail, url={}, statusCode={}", url, statusCode);
                throw new InternalException(
                    ErrorCode.GENERATE_PERSONAL_ACCESS_TOKEN_FAIL,
                    new Object[]{"http status " + statusCode}
                );
            }
            return JsonUtils.fromJson(respStr, AuthApiTokenResp.class);
        } catch (InternalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Request auth_api token error, url={}", url, e);
            throw new InternalException(
                e,
                ErrorCode.GENERATE_PERSONAL_ACCESS_TOKEN_FAIL,
                new Object[]{e.getMessage()}
            );
        }
    }
}
