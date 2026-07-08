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
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.manage.config.PublicAppProperties;
import com.tencent.bk.job.manage.model.dto.PersonalAccessTokenDTO;
import com.tencent.bk.job.manage.model.web.vo.PersonalAccessTokenVO;
import com.tencent.bk.job.manage.service.token.PersonalAccessTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PersonalAccessTokenServiceImpl 测试")
class PersonalAccessTokenServiceImplTest {

    private PublicAppProperties publicAppProperties;
    private PersonalAccessTokenProvider provider;
    private PersonalAccessTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        publicAppProperties = new PublicAppProperties();
        provider = mock(PersonalAccessTokenProvider.class);
        service = new PersonalAccessTokenServiceImpl(publicAppProperties, provider);
    }

    @Test
    @DisplayName("公共应用未启用时抛出 FailedPreconditionException 且不调用 provider")
    void notEnabledShouldThrow() {
        publicAppProperties.setEnabled(false);

        assertThatThrownBy(() -> service.generatePersonalAccessToken("admin", "ticket", null))
            .isInstanceOf(FailedPreconditionException.class)
            .matches(e -> ((ServiceException) e).getErrorCode()
                == ErrorCode.PUBLIC_APP_PERSONAL_TOKEN_NOT_AVAILABLE);

        verify(provider, never()).generate(any(), any(), any());
    }

    @Test
    @DisplayName("启用后将 provider 返回的 DTO 转换为 VO 并计算 expireAt")
    void enabledShouldConvertDtoToVo() {
        publicAppProperties.setEnabled(true);
        PersonalAccessTokenDTO dto = PersonalAccessTokenDTO.builder()
            .accessToken("access-token-001")
            .expiresIn(3600L)
            .refreshToken("refresh-token-001")
            .build();
        when(provider.generate(eq("admin"), eq("ticket"), eq("token"))).thenReturn(dto);

        long before = System.currentTimeMillis();
        PersonalAccessTokenVO vo = service.generatePersonalAccessToken("admin", "ticket", "token");
        long after = System.currentTimeMillis();

        assertThat(vo.getAccessToken()).isEqualTo("access-token-001");
        assertThat(vo.getExpiresIn()).isEqualTo(3600L);
        assertThat(vo.getRefreshToken()).isEqualTo("refresh-token-001");
        // expireAt 为 yyyy-MM-dd HH:mm:ss 字符串，该格式按字典序与时间单调一致，可直接做区间断言
        String expectedLow = DateUtils.formatUnixTimestamp(before + 3600L * 1000L, ChronoUnit.MILLIS);
        String expectedHigh = DateUtils.formatUnixTimestamp(after + 3600L * 1000L, ChronoUnit.MILLIS);
        assertThat(vo.getExpireAt())
            .matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
            .isBetween(expectedLow, expectedHigh);
    }

    @Test
    @DisplayName("expiresIn 为空时 expireAt 不计算")
    void nullExpiresInShouldNotComputeExpireAt() {
        publicAppProperties.setEnabled(true);
        PersonalAccessTokenDTO dto = PersonalAccessTokenDTO.builder()
            .accessToken("access-token-002")
            .expiresIn(null)
            .refreshToken("refresh-token-002")
            .build();
        when(provider.generate(any(), any(), any())).thenReturn(dto);

        PersonalAccessTokenVO vo = service.generatePersonalAccessToken("admin", "ticket", "token");

        assertThat(vo.getAccessToken()).isEqualTo("access-token-002");
        assertThat(vo.getExpireAt()).isNull();
    }
}
