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
import com.tencent.bk.job.common.paas.config.condition.ConditionalOnCustomLoginDisable;
import com.tencent.bk.job.common.util.http.HttpConPoolUtil;
import com.tencent.bk.job.common.util.http.HttpResponse;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.config.PublicAppProperties;
import com.tencent.bk.job.manage.model.dto.PersonalAccessTokenDTO;
import com.tencent.bk.job.manage.service.token.PersonalAccessTokenProvider;
import com.tencent.bk.job.manage.service.token.model.BkSsmTokenResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 社区版个人访问凭证生成实现，直连 bkssm（不走网关）。
 * 仅在未开启自定义登录（paas.login.custom.enabled=false 或缺省，使用 bk_token）时装配。
 */
@Slf4j
@Component
@ConditionalOnCustomLoginDisable
public class BkSsmAccessTokenProvider implements PersonalAccessTokenProvider {

    private static final String API_ACCESS_TOKENS = "/api/v1/auth/access-tokens";

    private final PublicAppProperties publicAppProperties;
    private final String bkSsmUrl;

    public BkSsmAccessTokenProvider(PublicAppProperties publicAppProperties,
                                    @Value("${bkSsmUrl:}") String bkSsmUrl) {
        this.publicAppProperties = publicAppProperties;
        this.bkSsmUrl = bkSsmUrl;
    }

    @Override
    public PersonalAccessTokenDTO generate(String username, String bkTicket, String bkToken) {
        if (StringUtils.isBlank(bkToken)) {
            throw new InvalidParamException(ErrorCode.PERSONAL_ACCESS_TOKEN_LOGIN_TICKET_MISSING);
        }
        if (StringUtils.isBlank(bkSsmUrl)) {
            throw new FailedPreconditionException(ErrorCode.PUBLIC_APP_PERSONAL_TOKEN_NOT_AVAILABLE);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("grant_type", "authorization_code");
        body.put("id_provider", "bk_login");
        body.put("bk_token", bkToken);

        String url = StringUtils.stripEnd(bkSsmUrl, "/") + API_ACCESS_TOKENS;
        BkSsmTokenResp resp = requestToken(url, body);
        if (resp == null || resp.getCode() == null || resp.getCode() != 0 || resp.getData() == null) {
            String reason = resp != null ? resp.getMessage() : "empty response";
            log.warn("Generate personal access token by bkssm fail, username={}, reason={}", username, reason);
            throw new InternalException(
                ErrorCode.GENERATE_PERSONAL_ACCESS_TOKEN_FAIL,
                new Object[]{reason}
            );
        }

        BkSsmTokenResp.TokenData data = resp.getData();
        return PersonalAccessTokenDTO.builder()
            .accessToken(data.getAccessToken())
            .expiresIn(data.getExpiresIn())
            .refreshToken(data.getRefreshToken())
            .build();
    }

    private BkSsmTokenResp requestToken(String url, Map<String, Object> body) {
        String respStr;
        String bodyStr = JsonUtils.toJson(body);
        try {
            HttpResponse response = HttpConPoolUtil.post(
                url,
                bodyStr,
                new BasicHeader("Content-Type", "application/json"),
                new BasicHeader("X-Bk-App-Code", publicAppProperties.getCode()),
                new BasicHeader("X-Bk-App-Secret", publicAppProperties.getSecret())
            );
            int statusCode = response != null ? response.getStatusCode() : -1;
            respStr = response != null ? response.getEntity() : null;
            if (log.isDebugEnabled()) {
                log.debug(
                    "Request bkssm token, url={}, body={}, statusCode={}, resp={}",
                    url,
                    bodyStr,
                    statusCode,
                    respStr
                );
            }
            if (response == null || response.getStatusCode() != HttpStatus.SC_OK
                || StringUtils.isBlank(response.getEntity())) {
                log.warn("Request bkssm token fail, url={}, statusCode={}", url, statusCode);
                throw new InternalException(
                    ErrorCode.GENERATE_PERSONAL_ACCESS_TOKEN_FAIL,
                    new Object[]{"http status " + statusCode}
                );
            }
            return JsonUtils.fromJson(respStr, BkSsmTokenResp.class);
        } catch (InternalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Request bkssm token error, url={}", url, e);
            throw new InternalException(
                e,
                ErrorCode.GENERATE_PERSONAL_ACCESS_TOKEN_FAIL,
                new Object[]{e.getMessage()}
            );
        }
    }
}
