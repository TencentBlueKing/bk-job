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
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.manage.model.dto.CredentialDTO;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetCredentialDetailV3Req;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbCredentialV3DTO;
import com.tencent.bk.job.manage.service.CredentialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 ESB 凭据详情接口必须按 appId 隔离查询，避免跨业务越权读。
 */
@DisplayName("EsbCredentialResourceV3Impl 越权读修复测试")
class EsbCredentialResourceV3ImplTest {

    private CredentialService credentialService;
    private AppScopeMappingService appScopeMappingService;
    private EsbCredentialResourceV3Impl resource;

    private static final String CREDENTIAL_ID = "credential-id-001";
    private static final Long OWNER_APP_ID = 100L;
    private static final Long ATTACKER_APP_ID = 200L;
    private static final String SCOPE_TYPE = ResourceScopeTypeEnum.BIZ.getValue();
    private static final String OWNER_SCOPE_ID = "100";
    private static final String ATTACKER_SCOPE_ID = "200";

    @BeforeEach
    void setUp() {
        credentialService = mock(CredentialService.class);
        appScopeMappingService = mock(AppScopeMappingService.class);
        resource = new EsbCredentialResourceV3Impl(credentialService, appScopeMappingService);
    }

    private EsbGetCredentialDetailV3Req buildReq(String scopeType, String scopeId) {
        EsbGetCredentialDetailV3Req req = new EsbGetCredentialDetailV3Req();
        req.setScopeType(scopeType);
        req.setScopeId(scopeId);
        req.setId(CREDENTIAL_ID);
        return req;
    }

    @Test
    @DisplayName("跨业务（攻击者 appId 不属于凭据归属业务）查询返回 CREDENTIAL_NOT_EXIST")
    void getCredentialDetailUsingPostFromAnotherAppShouldFail() {
        when(appScopeMappingService.getAppIdByScope(eq(SCOPE_TYPE), eq(ATTACKER_SCOPE_ID)))
            .thenReturn(ATTACKER_APP_ID);
        // 仿真 service 行为：当 appId 与凭据的真实归属 appId 不一致时返回 null
        when(credentialService.getCredentialById(eq(ATTACKER_APP_ID), eq(CREDENTIAL_ID)))
            .thenReturn(null);

        EsbGetCredentialDetailV3Req req = buildReq(SCOPE_TYPE, ATTACKER_SCOPE_ID);

        assertThatThrownBy(() -> resource.getCredentialDetailUsingPost("attacker", "test_app", req))
            .isInstanceOf(NotFoundException.class)
            .extracting(e -> ((ServiceException) e).getErrorCode())
            .isEqualTo(ErrorCode.CREDENTIAL_NOT_EXIST);

        verify(credentialService, times(1)).getCredentialById(eq(ATTACKER_APP_ID), eq(CREDENTIAL_ID));
    }

    @Test
    @DisplayName("攻击者直接调用必须使用解析出来的 appId 查询，不再调用无 appId 校验的重载")
    void getCredentialDetailUsingPostShouldNotCallNoAppIdOverload() {
        when(appScopeMappingService.getAppIdByScope(eq(SCOPE_TYPE), eq(ATTACKER_SCOPE_ID)))
            .thenReturn(ATTACKER_APP_ID);
        when(credentialService.getCredentialById(eq(ATTACKER_APP_ID), eq(CREDENTIAL_ID)))
            .thenReturn(null);

        EsbGetCredentialDetailV3Req req = buildReq(SCOPE_TYPE, ATTACKER_SCOPE_ID);

        assertThatThrownBy(() -> resource.getCredentialDetailUsingPost("attacker", "test_app", req))
            .isInstanceOf(NotFoundException.class);

        // 关键：必须走带 appId 的重载，决不能再调用无 appId 的越权重载
        verify(credentialService, times(1)).getCredentialById(eq(ATTACKER_APP_ID), eq(CREDENTIAL_ID));
        verify(credentialService, times(0)).getCredentialById(eq(CREDENTIAL_ID));
    }

    @Test
    @DisplayName("同业务下查询凭据详情正常返回")
    void getCredentialDetailUsingPostFromOwnerAppShouldPass() {
        when(appScopeMappingService.getAppIdByScope(eq(SCOPE_TYPE), eq(OWNER_SCOPE_ID)))
            .thenReturn(OWNER_APP_ID);
        CredentialDTO dto = mock(CredentialDTO.class);
        EsbCredentialV3DTO esbDTO = new EsbCredentialV3DTO();
        esbDTO.setId(CREDENTIAL_ID);
        when(dto.toEsbCredentialV3DTO()).thenReturn(esbDTO);
        when(credentialService.getCredentialById(eq(OWNER_APP_ID), eq(CREDENTIAL_ID)))
            .thenReturn(dto);

        EsbGetCredentialDetailV3Req req = buildReq(SCOPE_TYPE, OWNER_SCOPE_ID);
        EsbResp<EsbCredentialV3DTO> resp = resource.getCredentialDetailUsingPost(
            "owner_user", "test_app", req);

        assertThat(resp.getData()).isNotNull();
        assertThat(resp.getData().getId()).isEqualTo(CREDENTIAL_ID);
        verify(credentialService, times(1)).getCredentialById(eq(OWNER_APP_ID), eq(CREDENTIAL_ID));
    }

    @Test
    @DisplayName("GET 入口委托给 POST，同样基于 appId 隔离查询")
    void getCredentialDetailGetShouldUseAppIdScopedQuery() {
        when(appScopeMappingService.getAppIdByScope(eq(SCOPE_TYPE), eq(ATTACKER_SCOPE_ID)))
            .thenReturn(ATTACKER_APP_ID);
        when(credentialService.getCredentialById(eq(ATTACKER_APP_ID), eq(CREDENTIAL_ID)))
            .thenReturn(null);

        assertThatThrownBy(() -> resource.getCredentialDetail(
            "attacker", "test_app", null, SCOPE_TYPE, ATTACKER_SCOPE_ID, CREDENTIAL_ID))
            .isInstanceOf(NotFoundException.class)
            .extracting(e -> ((ServiceException) e).getErrorCode())
            .isEqualTo(ErrorCode.CREDENTIAL_NOT_EXIST);

        verify(credentialService, times(1)).getCredentialById(eq(ATTACKER_APP_ID), eq(CREDENTIAL_ID));
    }
}
