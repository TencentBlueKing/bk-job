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

package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.manage.config.PublicAppProperties;
import com.tencent.bk.job.manage.model.dto.PersonalAccessTokenDTO;
import com.tencent.bk.job.manage.model.web.vo.PersonalAccessTokenVO;
import com.tencent.bk.job.manage.service.PersonalAccessTokenService;
import com.tencent.bk.job.manage.service.token.PersonalAccessTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;

/**
 * 个人访问凭证服务实现。注入当前登录方式装配的 Provider，将其返回的内部 DTO 转换为对外 VO。
 */
@Slf4j
@Service
public class PersonalAccessTokenServiceImpl implements PersonalAccessTokenService {

    private final PublicAppProperties publicAppProperties;
    private final PersonalAccessTokenProvider personalAccessTokenProvider;

    @Autowired
    public PersonalAccessTokenServiceImpl(PublicAppProperties publicAppProperties,
                                          PersonalAccessTokenProvider personalAccessTokenProvider) {
        this.publicAppProperties = publicAppProperties;
        this.personalAccessTokenProvider = personalAccessTokenProvider;
    }

    @Override
    public PersonalAccessTokenVO generatePersonalAccessToken(String username, String bkTicket, String bkToken) {
        if (!publicAppProperties.isEnabled()) {
            throw new FailedPreconditionException(ErrorCode.PUBLIC_APP_PERSONAL_TOKEN_NOT_AVAILABLE);
        }
        PersonalAccessTokenDTO tokenDTO = personalAccessTokenProvider.generate(username, bkTicket, bkToken);
        return convertToVO(tokenDTO);
    }

    private PersonalAccessTokenVO convertToVO(PersonalAccessTokenDTO tokenDTO) {
        PersonalAccessTokenVO vo = new PersonalAccessTokenVO();
        vo.setAccessToken(tokenDTO.getAccessToken());
        vo.setExpiresIn(tokenDTO.getExpiresIn());
        vo.setRefreshToken(tokenDTO.getRefreshToken());
        if (tokenDTO.getExpiresIn() != null) {
            long expireAtMillis = System.currentTimeMillis() + tokenDTO.getExpiresIn() * 1000L;
            vo.setExpireAt(DateUtils.formatUnixTimestamp(expireAtMillis, ChronoUnit.MILLIS));
        }
        return vo;
    }
}
