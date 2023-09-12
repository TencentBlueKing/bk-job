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

package com.tencent.bk.job.gateway.web.service.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.i18n.locale.LocaleUtils;
import com.tencent.bk.job.common.model.dto.BkUserDTO;
import com.tencent.bk.job.common.paas.config.LoginConfiguration;
import com.tencent.bk.job.common.paas.login.ILoginClient;
import com.tencent.bk.job.gateway.web.service.LoginService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class LoginServiceImpl implements LoginService {
    private final LoginConfiguration loginConfig;
    private final String tokenName;
    private final String loginUrl;
    private final ILoginClient enLoginClient;
    private final ILoginClient cnLoginClient;
    private final LoadingCache<BkTokenWithLang, Optional<BkUserDTO>> onlineUserCache = CacheBuilder.newBuilder()
        .maximumSize(200).expireAfterWrite(10, TimeUnit.SECONDS).build(
            new CacheLoader<BkTokenWithLang, Optional<BkUserDTO>>() {
                @Override
                public Optional<BkUserDTO> load(@NotNull BkTokenWithLang bkTokenWithLang) {
                    String normalLang = bkTokenWithLang.getNormalLang();
                    String bkToken = bkTokenWithLang.getBkToken();
                    BkUserDTO userDto;
                    if (LocaleUtils.LANG_ZH_CN.equals(normalLang)) {
                        userDto = cnLoginClient.getUserInfoByToken(bkToken);
                    } else {
                        userDto = enLoginClient.getUserInfoByToken(bkToken);
                    }
                    return Optional.ofNullable(userDto);
                }
            }
        );

    @Autowired
    public LoginServiceImpl(LoginConfiguration loginConfig,
                            @Qualifier("enLoginClient") ILoginClient enLoginClient,
                            @Qualifier("cnLoginClient") ILoginClient cnLoginClient) {
        this.loginConfig = loginConfig;
        this.enLoginClient = enLoginClient;
        this.cnLoginClient = cnLoginClient;
        this.loginUrl = getLoginUrlProp();
        this.tokenName = loginConfig.isCustomPaasLoginEnabled() ? loginConfig.getCustomLoginToken() : "bk_token";
        log.info("Init login service, customLoginEnabled:{}, loginClient:{}, loginUrl:{}, tokenName:{}",
            loginConfig.isCustomPaasLoginEnabled(), enLoginClient.getClass(), loginUrl, tokenName);
    }

    private String getLoginUrlProp() {
        String loginUrl;
        if (loginConfig.isCustomPaasLoginEnabled()) {
            loginUrl = loginConfig.getCustomLoginUrl();
        } else {
            loginUrl = loginConfig.getLoginUrl();
        }
        if (!loginUrl.endsWith("?")) {
            loginUrl = loginUrl + "?";
        }
        return loginUrl;
    }

    @Override
    public void deleteUser(String bkToken) {
        if (StringUtils.isBlank(bkToken)) {
            return;
        }
        onlineUserCache.invalidate(new BkTokenWithLang(bkToken, LocaleUtils.LANG_EN));
        onlineUserCache.invalidate(new BkTokenWithLang(bkToken, LocaleUtils.LANG_ZH_CN));
    }

    @Getter
    static class BkTokenWithLang {
        final private String bkToken;
        final private String normalLang;

        BkTokenWithLang(String bkToken, String normalLang) {
            this.bkToken = bkToken;
            this.normalLang = normalLang;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BkTokenWithLang)) return false;
            BkTokenWithLang that = (BkTokenWithLang) o;
            return Objects.equals(bkToken, that.bkToken) &&
                Objects.equals(normalLang, that.normalLang);
        }

        @Override
        public int hashCode() {
            return Objects.hash(bkToken, normalLang);
        }
    }

    @Override
    public BkUserDTO getUser(String bkToken, String bkLang) {
        if (StringUtils.isBlank(bkToken)) {
            return null;
        }
        if (StringUtils.isBlank(bkLang)) {
            log.warn("getUser: bkLang is null or blank, use default en");
            bkLang = LocaleUtils.LANG_EN;
        }
        try {
            String normalLang = LocaleUtils.getNormalLang(bkLang);
            BkTokenWithLang bkTokenWithLang = new BkTokenWithLang(bkToken, normalLang);
            Optional<BkUserDTO> userDto = onlineUserCache.get(bkTokenWithLang);
            return userDto.orElse(null);
        } catch (ExecutionException | UncheckedExecutionException e) {
            log.warn("Error occur when get user from paas!");
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new InternalException("Query userinfo from paas fail", e, ErrorCode.INTERNAL_ERROR);
            }
        }
    }

    @Override
    public String getLoginRedirectUrl() {
        return loginUrl;
    }

    @Override
    public String getCookieNameForToken() {
        return this.tokenName;
    }
}
