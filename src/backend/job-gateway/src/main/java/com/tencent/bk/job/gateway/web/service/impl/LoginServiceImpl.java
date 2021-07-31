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
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.dto.BkUserDTO;
import com.tencent.bk.job.common.paas.login.ILoginClient;
import com.tencent.bk.job.gateway.config.BkConfig;
import com.tencent.bk.job.gateway.web.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.UnknownHostException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class LoginServiceImpl implements LoginService {
    private final BkConfig bkConfig;
    private final String tokenName;
    private final String loginUrl;
    private final ILoginClient loginClient;
    private LoadingCache<String, Optional<BkUserDTO>> onlineUserCache = CacheBuilder.newBuilder()
        .maximumSize(200).expireAfterWrite(10, TimeUnit.SECONDS).build(
            new CacheLoader<String, Optional<BkUserDTO>>() {
                @Override
                public Optional<BkUserDTO> load(String bkToken) throws Exception {
                    try {
                        BkUserDTO userDto = loginClient.getUserInfoByToken(bkToken);
                        return Optional.ofNullable(userDto);
                    } catch (Exception e) {
                        return Optional.empty();
                    }
                }
            }
        );

    @Autowired
    public LoginServiceImpl(BkConfig bkConfig, ILoginClient loginClient) {
        this.bkConfig = bkConfig;
        this.loginClient = loginClient;
        this.loginUrl = getLoginUrlProp();
        this.tokenName = bkConfig.isCustomPaasLoginEnabled() ? bkConfig.getCustomLoginToken() : "bk_token";
        log.info("Init login service, customLoginEnabled:{}, loginClient:{}, loginUrl:{}, tokenName:{}",
            bkConfig.isCustomPaasLoginEnabled(), loginClient.getClass(), loginUrl, tokenName);
    }

    private String getLoginUrlProp() {
        String loginUrl;
        if (bkConfig.isCustomPaasLoginEnabled()) {
            loginUrl = bkConfig.getCustomLoginUrl();
        } else {
            loginUrl = bkConfig.getLoginUrl();
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
        try {
            onlineUserCache.invalidate(bkToken);
        } catch (Exception e) {
            log.warn("Error occur when invalidate bkToken:{}", bkToken);
            throw caughtException(e);
        }
    }

    @Override
    public BkUserDTO getUser(String bkToken) {
        if (StringUtils.isBlank(bkToken)) {
            return null;
        }
        try {
            Optional<BkUserDTO> userDto = onlineUserCache.get(bkToken);
            return userDto.orElse(null);
        } catch (Exception e) {
            log.warn("Error occur when get user from paas!");
            throw caughtException(e);
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

    private ServiceException caughtException(Exception e) {
        int errorCode = ErrorCode.PAAS_API_DATA_ERROR;
        if (e instanceof UnknownHostException) {
            errorCode = ErrorCode.PAAS_UNREACHABLE_SERVER;
        }
        return new ServiceException(errorCode, e.getMessage());
    }
}
