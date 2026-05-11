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

package com.tencent.bk.job.manage.api.esb.impl.v3;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.discovery.ServiceInfoProvider;
import com.tencent.bk.job.common.discovery.model.ServiceInstanceInfoDTO;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.auth.NoResourceScopeAuthService;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbServiceVersionV3DTO;
import com.tencent.bk.job.manage.service.impl.ServiceInfoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 ESB 服务信息接口的权限校验与多租户校验，
 * 与 Web 接口 /web/serviceInfo/** 的鉴权拦截器及 WebServiceInfoResourceImpl 原内联租户校验保持一致。
 */
@DisplayName("EsbServiceInfoV3ResourceImpl 权限/租户校验测试")
class EsbServiceInfoV3ResourceImplTest {

    private ServiceInfoProvider serviceInfoProvider;
    private NoResourceScopeAuthService noResourceScopeAuthService;
    private ServiceInfoService serviceInfoService;
    private EsbServiceInfoV3ResourceImpl resource;

    private static final String NORMAL_USER = "normal_user";
    private static final String ADMIN_USER = "admin_user";

    @BeforeEach
    void setUp() {
        serviceInfoProvider = mock(ServiceInfoProvider.class);
        noResourceScopeAuthService = mock(NoResourceScopeAuthService.class);
        serviceInfoService = mock(ServiceInfoService.class);
        resource = new EsbServiceInfoV3ResourceImpl(
            serviceInfoProvider, noResourceScopeAuthService, serviceInfoService
        );
    }

    @AfterEach
    void tearDown() {
        JobContextUtil.unsetContext();
    }

    private User mockUser(String username) {
        User user = new User("default", username, username);
        JobContextUtil.setUser(user);
        return user;
    }

    private void givenNoPermission(User user) {
        when(noResourceScopeAuthService.authViewServiceState(any()))
            .thenReturn(AuthResult.fail(user));
    }

    private void givenHasPermission(User user) {
        when(noResourceScopeAuthService.authViewServiceState(any()))
            .thenReturn(AuthResult.pass(user));
    }

    private void givenTenantAccessAllowed() {
        doNothing().when(serviceInfoService).checkTenantAccess();
    }

    private void givenTenantAccessDenied() {
        doThrow(new NotFoundException(ErrorCode.NOT_SUPPORT_FEATURE))
            .when(serviceInfoService).checkTenantAccess();
    }

    private ServiceInstanceInfoDTO buildInstance(String version) {
        ServiceInstanceInfoDTO dto = new ServiceInstanceInfoDTO();
        dto.setVersion(version);
        return dto;
    }

    @Test
    @DisplayName("普通用户调用 POST 获取最新服务版本被拒绝且不进入业务逻辑")
    void normalUserGetLatestServiceVersionPostShouldBeDenied() {
        User user = mockUser(NORMAL_USER);
        givenNoPermission(user);

        assertThatThrownBy(() -> resource.getLatestServiceVersionUsingPost(NORMAL_USER, "test_app"))
            .isInstanceOf(PermissionDeniedException.class);

        verify(serviceInfoService, never()).checkTenantAccess();
        verify(serviceInfoProvider, never()).listServiceInfo();
    }

    @Test
    @DisplayName("普通用户调用 GET 获取最新服务版本同样被拒绝（GET 委托给 POST）")
    void normalUserGetLatestServiceVersionGetShouldBeDenied() {
        User user = mockUser(NORMAL_USER);
        givenNoPermission(user);

        assertThatThrownBy(() -> resource.getLatestServiceVersion(NORMAL_USER, "test_app"))
            .isInstanceOf(PermissionDeniedException.class);

        verify(serviceInfoService, never()).checkTenantAccess();
        verify(serviceInfoProvider, never()).listServiceInfo();
    }

    @Test
    @DisplayName("管理员在非系统租户访问被拒（多租户启用且非 system 租户）")
    void adminUserNonSystemTenantShouldBeDenied() {
        User user = mockUser(ADMIN_USER);
        givenHasPermission(user);
        givenTenantAccessDenied();

        assertThatThrownBy(() -> resource.getLatestServiceVersionUsingPost(ADMIN_USER, "test_app"))
            .isInstanceOf(NotFoundException.class)
            .extracting(e -> ((ServiceException) e).getErrorCode())
            .isEqualTo(ErrorCode.NOT_SUPPORT_FEATURE);

        verify(serviceInfoProvider, never()).listServiceInfo();
    }

    @Test
    @DisplayName("管理员在系统租户/未启用多租户场景下成功获取最新服务版本")
    void adminUserShouldGetLatestServiceVersion() {
        User user = mockUser(ADMIN_USER);
        givenHasPermission(user);
        givenTenantAccessAllowed();
        when(serviceInfoProvider.listServiceInfo()).thenReturn(Arrays.asList(
            buildInstance("3.12.1"),
            buildInstance("3.12.3"),
            buildInstance("3.12.2")
        ));

        EsbResp<EsbServiceVersionV3DTO> resp = resource.getLatestServiceVersionUsingPost(
            ADMIN_USER, "test_app");

        assertThat(resp.getData()).isNotNull();
        assertThat(resp.getData().getVersion()).isEqualTo("3.12.3");
        verify(serviceInfoService, times(1)).checkTenantAccess();
        verify(serviceInfoProvider, times(1)).listServiceInfo();
    }

    @Test
    @DisplayName("管理员通过 GET 路径同样会触发鉴权与租户校验，并返回最新版本")
    void adminUserGetLatestServiceVersionViaGetShouldPass() {
        User user = mockUser(ADMIN_USER);
        givenHasPermission(user);
        givenTenantAccessAllowed();
        when(serviceInfoProvider.listServiceInfo()).thenReturn(Arrays.asList(
            buildInstance("3.12.1"),
            buildInstance("3.12.5")
        ));

        EsbResp<EsbServiceVersionV3DTO> resp = resource.getLatestServiceVersion(
            ADMIN_USER, "test_app");

        assertThat(resp.getData().getVersion()).isEqualTo("3.12.5");
        verify(noResourceScopeAuthService, times(1)).authViewServiceState(any());
        verify(serviceInfoService, times(1)).checkTenantAccess();
    }
}
