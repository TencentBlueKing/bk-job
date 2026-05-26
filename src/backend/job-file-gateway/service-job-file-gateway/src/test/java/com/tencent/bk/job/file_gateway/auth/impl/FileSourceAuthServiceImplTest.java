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
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 FileSourceAuthServiceImpl 中新增的 USE_TICKET 校验路由到 AuthService.auth(USE_TICKET, TICKET, ...)。
 *
 * <p>说明：service-job-file-gateway 模块未依赖 service-job-manage 模块，
 * 因此 USE_TICKET 校验直接在本模块内通过 AuthService 公共能力实现，
 * 测试需要保证：1) actionId/resourceType 正确；2) 资源路径携带 appResourceScope 信息。</p>
 */
@DisplayName("FileSourceAuthServiceImpl USE_TICKET 鉴权路由测试")
class FileSourceAuthServiceImplTest {

    private AuthService authService;
    private AppAuthService appAuthService;
    private FileSourceAuthServiceImpl fileSourceAuthService;

    private static final String TENANT_ID = "default";
    private static final String USERNAME = "tester";
    private static final long APP_ID = 100L;
    private static final String TICKET_ID = "ticket-1";

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        appAuthService = mock(AppAuthService.class);
        fileSourceAuthService = new FileSourceAuthServiceImpl(authService, appAuthService);
    }

    @Test
    @DisplayName("authUseTicket 应当调用 AuthService.auth 且 actionId=USE_TICKET、resourceType=TICKET")
    void authUseTicketShouldRouteToAuthServiceWithCorrectActionAndResourceType() {
        User user = new User(TENANT_ID, USERNAME, USERNAME);
        AppResourceScope scope = new AppResourceScope(APP_ID);
        scope.setType(ResourceScopeTypeEnum.BIZ);
        scope.setId("100");
        AuthResult expected = AuthResult.pass(user);
        when(authService.auth(eq(user), eq(ActionId.USE_TICKET), eq(ResourceTypeEnum.TICKET),
            eq(TICKET_ID), any(PathInfoDTO.class))).thenReturn(expected);

        AuthResult result = fileSourceAuthService.authUseTicket(user, scope, TICKET_ID);

        assertThat(result).isSameAs(expected);

        ArgumentCaptor<PathInfoDTO> pathCaptor = ArgumentCaptor.forClass(PathInfoDTO.class);
        verify(authService, times(1)).auth(eq(user), eq(ActionId.USE_TICKET), eq(ResourceTypeEnum.TICKET),
            eq(TICKET_ID), pathCaptor.capture());
        PathInfoDTO path = pathCaptor.getValue();
        assertThat(path).isNotNull();
        assertThat(path.getId()).isEqualTo("100");
    }

    @Test
    @DisplayName("authUseTicket 校验失败时透传 AuthResult.fail()")
    void authUseTicketShouldPropagateFailResult() {
        User user = new User(TENANT_ID, USERNAME, USERNAME);
        AppResourceScope scope = new AppResourceScope(APP_ID);
        scope.setType(ResourceScopeTypeEnum.BIZ);
        scope.setId("100");
        AuthResult failResult = AuthResult.fail(user);
        when(authService.auth(eq(user), eq(ActionId.USE_TICKET), eq(ResourceTypeEnum.TICKET),
            eq(TICKET_ID), any(PathInfoDTO.class))).thenReturn(failResult);

        AuthResult result = fileSourceAuthService.authUseTicket(user, scope, TICKET_ID);

        assertThat(result.isPass()).isFalse();
        verify(authService, times(1)).auth(eq(user), eq(ActionId.USE_TICKET), eq(ResourceTypeEnum.TICKET),
            eq(TICKET_ID), any(PathInfoDTO.class));
    }
}
