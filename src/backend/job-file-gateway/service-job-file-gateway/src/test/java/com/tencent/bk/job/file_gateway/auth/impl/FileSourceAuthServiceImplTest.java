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

package com.tencent.bk.job.file_gateway.auth.impl;

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 FileSourceAuthServiceImpl#authUseTicket 调用 AuthService.auth(USE_TICKET, TICKET, ...)
 */
class FileSourceAuthServiceImplTest {

    private static final String USERNAME = "u1";
    private static final String TICKET_ID = "ticket-1";
    private static final Long APP_ID = 1001L;

    private AuthService authService;
    private FileSourceAuthServiceImpl resource;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        AppAuthService appAuthService = mock(AppAuthService.class);
        resource = new FileSourceAuthServiceImpl(authService, appAuthService);
    }

    @Test
    @DisplayName("authUseTicket 委托给 AuthService.auth(USE_TICKET, TICKET, ticketId, appScopePath)")
    void authUseTicket_invokesUnderlyingAuthService() {
        AppResourceScope scope = new AppResourceScope(ResourceScopeTypeEnum.BIZ.getValue(), "2001", APP_ID);
        when(authService.auth(
            eq(USERNAME),
            eq(ActionId.USE_TICKET),
            eq(ResourceTypeEnum.TICKET),
            eq(TICKET_ID),
            any(PathInfoDTO.class)
        )).thenReturn(AuthResult.pass());

        AuthResult result = resource.authUseTicket(USERNAME, scope, TICKET_ID);

        assertThat(result.isPass()).isTrue();
        verify(authService).auth(
            eq(USERNAME),
            eq(ActionId.USE_TICKET),
            eq(ResourceTypeEnum.TICKET),
            eq(TICKET_ID),
            any(PathInfoDTO.class)
        );
    }
}
