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

import com.tencent.bk.job.common.discovery.ServiceInfoProvider;
import com.tencent.bk.job.common.discovery.model.ServiceInstanceInfoDTO;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.manage.auth.NoResourceScopeAuthService;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbServiceVersionV3DTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 EsbServiceInfoV3ResourceImpl#getLatestServiceVersion(UsingPost) 在调用 delegate 业务方法之前先做 IAM 鉴权
 */
class EsbServiceInfoV3ResourceImplTest {

    private static final String NORMAL_USER = "normal_user";
    private static final String ADMIN_USER = "admin";
    private static final String APP_CODE = "bk_job";

    private ServiceInfoProvider serviceInfoProvider;
    private NoResourceScopeAuthService noResourceScopeAuthService;
    private EsbServiceInfoV3ResourceImpl resource;

    @BeforeEach
    void setUp() {
        serviceInfoProvider = mock(ServiceInfoProvider.class);
        noResourceScopeAuthService = mock(NoResourceScopeAuthService.class);
        resource = new EsbServiceInfoV3ResourceImpl(serviceInfoProvider, noResourceScopeAuthService);
    }

    @Test
    @DisplayName("getLatestServiceVersionUsingPost 普通用户被拒绝，delegate 不被调用")
    void getLatestServiceVersionUsingPost_normalUserDenied() {
        when(noResourceScopeAuthService.authViewServiceState(eq(NORMAL_USER))).thenReturn(AuthResult.fail());

        assertThatThrownBy(() -> resource.getLatestServiceVersionUsingPost(NORMAL_USER, APP_CODE))
            .isInstanceOf(PermissionDeniedException.class);
        verify(serviceInfoProvider, never()).listServiceInfo();
    }

    @Test
    @DisplayName("getLatestServiceVersionUsingPost 管理员放行，delegate 被调用")
    void getLatestServiceVersionUsingPost_adminAllowed() {
        when(noResourceScopeAuthService.authViewServiceState(eq(ADMIN_USER))).thenReturn(AuthResult.pass());
        ServiceInstanceInfoDTO instance = new ServiceInstanceInfoDTO();
        instance.setVersion("3.10.5");
        when(serviceInfoProvider.listServiceInfo()).thenReturn(Collections.singletonList(instance));

        EsbResp<EsbServiceVersionV3DTO> resp = resource.getLatestServiceVersionUsingPost(ADMIN_USER, APP_CODE);

        verify(serviceInfoProvider).listServiceInfo();
        assertThat(resp.getData().getVersion()).isEqualTo("3.10.5");
    }

    @Test
    @DisplayName("getLatestServiceVersion(GET) 委托 POST，普通用户同样被拒绝")
    void getLatestServiceVersion_getEndpointDelegatesAndDenied() {
        when(noResourceScopeAuthService.authViewServiceState(eq(NORMAL_USER))).thenReturn(AuthResult.fail());

        assertThatThrownBy(() -> resource.getLatestServiceVersion(NORMAL_USER, APP_CODE))
            .isInstanceOf(PermissionDeniedException.class);
        verify(serviceInfoProvider, never()).listServiceInfo();
    }

    @Test
    @DisplayName("getLatestServiceVersion(GET) 委托 POST，管理员放行")
    void getLatestServiceVersion_getEndpointDelegatesAndAllowed() {
        when(noResourceScopeAuthService.authViewServiceState(eq(ADMIN_USER))).thenReturn(AuthResult.pass());
        when(serviceInfoProvider.listServiceInfo()).thenReturn(Collections.emptyList());

        EsbResp<EsbServiceVersionV3DTO> resp = resource.getLatestServiceVersion(ADMIN_USER, APP_CODE);

        verify(serviceInfoProvider).listServiceInfo();
        assertThat(resp.getData().getVersion()).isNull();
    }
}
