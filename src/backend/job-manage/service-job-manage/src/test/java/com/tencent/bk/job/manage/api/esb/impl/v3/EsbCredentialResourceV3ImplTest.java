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
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.manage.model.dto.CredentialDTO;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetCredentialDetailV3Req;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbCredentialV3DTO;
import com.tencent.bk.job.manage.service.CredentialService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 EsbCredentialResourceV3Impl#getCredentialDetail(UsingPost) 通过 (appId, id) 校验确保跨业务隔离
 */
class EsbCredentialResourceV3ImplTest {

    private static final String USERNAME = "u1";
    private static final String APP_CODE = "bk_job";
    private static final Long APP_ID = 1001L;
    private static final String SCOPE_TYPE = ResourceScopeTypeEnum.BIZ.getValue();
    private static final String SCOPE_ID = "2001";
    private static final String CREDENTIAL_ID = "cred-1";

    private CredentialService credentialService;
    private AppScopeMappingService appScopeMappingService;
    private EsbCredentialResourceV3Impl resource;
    private ApplicationContextRegister contextRegister;

    @BeforeEach
    void setUp() {
        credentialService = mock(CredentialService.class);
        appScopeMappingService = mock(AppScopeMappingService.class);
        resource = new EsbCredentialResourceV3Impl(credentialService, appScopeMappingService);
        when(appScopeMappingService.getAppIdByScope(eq(SCOPE_TYPE), eq(SCOPE_ID))).thenReturn(APP_ID);

        // CredentialDTO.toEsbCredentialV3DTO 内部通过 ApplicationContextRegister 拿 AppScopeMappingService，
        // 单元测试场景下注入一个最小可用的 ApplicationContext。
        ApplicationContext mockContext = mock(ApplicationContext.class);
        when(mockContext.getBean(AppScopeMappingService.class)).thenReturn(appScopeMappingService);
        when(appScopeMappingService.getScopeByAppId(eq(APP_ID)))
            .thenReturn(new ResourceScope(SCOPE_TYPE, SCOPE_ID));
        contextRegister = new ApplicationContextRegister();
        contextRegister.setApplicationContext(mockContext);
    }

    @AfterEach
    void tearDown() {
        contextRegister.setApplicationContext(null);
    }

    private EsbGetCredentialDetailV3Req buildReq() {
        EsbGetCredentialDetailV3Req req = new EsbGetCredentialDetailV3Req();
        req.setScopeType(SCOPE_TYPE);
        req.setScopeId(SCOPE_ID);
        req.setId(CREDENTIAL_ID);
        return req;
    }

    @Test
    @DisplayName("POST 路径：跨业务时 getCredentialById(appId, id) 返回 null，应抛 CREDENTIAL_NOT_EXIST")
    void getCredentialDetailUsingPost_crossAppDenied() {
        when(credentialService.getCredentialById(eq(APP_ID), eq(CREDENTIAL_ID))).thenReturn(null);

        assertThatThrownBy(() -> resource.getCredentialDetailUsingPost(USERNAME, APP_CODE, buildReq()))
            .isInstanceOf(NotFoundException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CREDENTIAL_NOT_EXIST);

        verify(credentialService).getCredentialById(eq(APP_ID), eq(CREDENTIAL_ID));
        verify(credentialService, never()).getCredentialById(anyString());
    }

    @Test
    @DisplayName("POST 路径：同业务时正常返回 EsbCredentialV3DTO")
    void getCredentialDetailUsingPost_sameAppAllowed() {
        CredentialDTO dto = new CredentialDTO();
        dto.setId(CREDENTIAL_ID);
        dto.setAppId(APP_ID);
        when(credentialService.getCredentialById(eq(APP_ID), eq(CREDENTIAL_ID))).thenReturn(dto);

        EsbResp<EsbCredentialV3DTO> resp = resource.getCredentialDetailUsingPost(USERNAME, APP_CODE, buildReq());

        assertThat(resp.getData()).isNotNull();
        assertThat(resp.getData().getId()).isEqualTo(CREDENTIAL_ID);
        verify(credentialService).getCredentialById(eq(APP_ID), eq(CREDENTIAL_ID));
        verify(credentialService, never()).getCredentialById(anyString());
    }

    @Test
    @DisplayName("GET 路径委托 POST，跨业务时同样抛 CREDENTIAL_NOT_EXIST")
    void getCredentialDetail_getEndpointDelegatesAndDenied() {
        when(credentialService.getCredentialById(eq(APP_ID), eq(CREDENTIAL_ID))).thenReturn(null);

        assertThatThrownBy(() ->
            resource.getCredentialDetail(USERNAME, APP_CODE, null, SCOPE_TYPE, SCOPE_ID, CREDENTIAL_ID))
            .isInstanceOf(NotFoundException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CREDENTIAL_NOT_EXIST);

        verify(credentialService).getCredentialById(eq(APP_ID), eq(CREDENTIAL_ID));
        verify(credentialService, never()).getCredentialById(anyString());
    }
}
