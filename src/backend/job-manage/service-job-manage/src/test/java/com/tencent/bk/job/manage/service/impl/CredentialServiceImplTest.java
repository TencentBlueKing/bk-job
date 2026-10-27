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
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.CommonCredential;
import com.tencent.bk.job.file_gateway.api.inner.ServiceFileSourceResource;
import com.tencent.bk.job.manage.api.common.constants.CredentialTypeEnum;
import com.tencent.bk.job.manage.auth.TicketAuthService;
import com.tencent.bk.job.manage.dao.CredentialDAO;
import com.tencent.bk.job.manage.model.dto.CredentialDTO;
import com.tencent.bk.job.manage.model.web.request.CredentialCreateUpdateReq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 CredentialServiceImpl 更新/删除凭证时必须先按 (appId, id) 校验真实归属，防止跨业务对象级越权。
 */
@DisplayName("CredentialServiceImpl 跨业务越权更新/删除修复测试")
class CredentialServiceImplTest {

    private static final Long OWNER_APP_ID = 175L;
    private static final Long ATTACKER_APP_ID = 173L;
    private static final String CREDENTIAL_ID = "cred-001";
    private static final String ATTACKER_USER = "attacker";
    private static final String OWNER_USER = "owner";

    private CredentialDAO credentialDAO;
    private TicketAuthService ticketAuthService;
    private ServiceFileSourceResource fileSourceService;
    private CredentialServiceImpl credentialService;

    @BeforeEach
    void setUp() {
        credentialDAO = mock(CredentialDAO.class);
        ticketAuthService = mock(TicketAuthService.class);
        fileSourceService = mock(ServiceFileSourceResource.class);
        credentialService = new CredentialServiceImpl(
            credentialDAO,
            ticketAuthService,
            fileSourceService
        );
    }

    private CredentialCreateUpdateReq buildUpdateReq() {
        CredentialCreateUpdateReq req = new CredentialCreateUpdateReq();
        req.setId(CREDENTIAL_ID);
        req.setName("new-name");
        req.setType(CredentialTypeEnum.PASSWORD);
        req.setDescription("desc");
        req.setValue1("new-password");
        return req;
    }

    private CredentialDTO buildCredentialDTO(Long appId, String id, String name) {
        CommonCredential commonCredential = new CommonCredential();
        commonCredential.setType(CredentialTypeEnum.PASSWORD.name());
        commonCredential.setPassword("secret");
        return new CredentialDTO(
            id,
            appId,
            name,
            CredentialTypeEnum.PASSWORD.name(),
            "desc",
            commonCredential,
            OWNER_USER,
            1000L,
            OWNER_USER,
            1000L
        );
    }

    @Test
    @DisplayName("跨业务更新凭证：按 (attackerAppId, id) 查不到记录时直接抛 CREDENTIAL_NOT_EXIST，不触发 IAM 鉴权与 DAO 更新")
    void updateCredentialCrossAppShouldFailBeforeAuthAndUpdate() {
        when(credentialDAO.getCredentialById(eq(ATTACKER_APP_ID), eq(CREDENTIAL_ID))).thenReturn(null);

        CredentialCreateUpdateReq req = buildUpdateReq();

        assertThatThrownBy(() -> credentialService.updateCredential(ATTACKER_USER, ATTACKER_APP_ID, req))
            .isInstanceOf(NotFoundException.class)
            .extracting(e -> ((ServiceException) e).getErrorCode())
            .isEqualTo(ErrorCode.CREDENTIAL_NOT_EXIST);

        verify(credentialDAO, times(1)).getCredentialById(eq(ATTACKER_APP_ID), eq(CREDENTIAL_ID));
        verify(credentialDAO, never()).getCredentialById(eq(CREDENTIAL_ID));
        verify(ticketAuthService, never()).authManageTicket(any(), any(), any(), any());
        verify(credentialDAO, never()).updateCredentialById(any());
    }

    @Test
    @DisplayName("同业务更新凭证：按真实归属 appId 鉴权并更新")
    void updateCredentialWithinOwnerAppShouldSucceed() {
        CredentialDTO originDto = buildCredentialDTO(OWNER_APP_ID, CREDENTIAL_ID, "old-name");
        CredentialDTO updatedDto = buildCredentialDTO(OWNER_APP_ID, CREDENTIAL_ID, "new-name");

        when(credentialDAO.getCredentialById(eq(OWNER_APP_ID), eq(CREDENTIAL_ID)))
            .thenReturn(originDto)
            .thenReturn(updatedDto);
        when(ticketAuthService.authManageTicket(eq(OWNER_USER), any(AppResourceScope.class), eq(CREDENTIAL_ID), isNull()))
            .thenReturn(AuthResult.pass());

        CredentialCreateUpdateReq req = buildUpdateReq();
        CredentialDTO result = credentialService.updateCredential(OWNER_USER, OWNER_APP_ID, req);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("new-name");

        ArgumentCaptor<AppResourceScope> scopeCaptor = ArgumentCaptor.forClass(AppResourceScope.class);
        verify(ticketAuthService, times(1))
            .authManageTicket(eq(OWNER_USER), scopeCaptor.capture(), eq(CREDENTIAL_ID), isNull());
        assertThat(scopeCaptor.getValue().getAppId()).isEqualTo(OWNER_APP_ID);

        ArgumentCaptor<CredentialDTO> updateCaptor = ArgumentCaptor.forClass(CredentialDTO.class);
        verify(credentialDAO, times(1)).updateCredentialById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getAppId()).isEqualTo(OWNER_APP_ID);
        assertThat(updateCaptor.getValue().getId()).isEqualTo(CREDENTIAL_ID);
    }

    @Test
    @DisplayName("跨业务删除凭证：按 (attackerAppId, id) 查不到记录时直接抛 CREDENTIAL_NOT_EXIST，不触发 IAM 鉴权与 DAO 删除")
    void deleteCredentialCrossAppShouldFailBeforeAuthAndDelete() {
        when(credentialDAO.getCredentialById(eq(ATTACKER_APP_ID), eq(CREDENTIAL_ID))).thenReturn(null);

        assertThatThrownBy(() -> credentialService.deleteCredentialById(ATTACKER_USER, ATTACKER_APP_ID, CREDENTIAL_ID))
            .isInstanceOf(NotFoundException.class)
            .extracting(e -> ((ServiceException) e).getErrorCode())
            .isEqualTo(ErrorCode.CREDENTIAL_NOT_EXIST);

        verify(credentialDAO, times(1)).getCredentialById(eq(ATTACKER_APP_ID), eq(CREDENTIAL_ID));
        verify(credentialDAO, never()).getCredentialById(eq(CREDENTIAL_ID));
        verify(ticketAuthService, never()).authManageTicket(any(), any(), any(), any());
        verify(credentialDAO, never()).deleteCredentialById(any(), any());
    }

    @Test
    @DisplayName("同业务删除凭证：先查真实归属再鉴权，并带 (appId, id) 删除")
    void deleteCredentialWithinOwnerAppShouldSucceed() {
        CredentialDTO originDto = buildCredentialDTO(OWNER_APP_ID, CREDENTIAL_ID, "cred-name");
        when(credentialDAO.getCredentialById(eq(OWNER_APP_ID), eq(CREDENTIAL_ID))).thenReturn(originDto);
        when(ticketAuthService.authManageTicket(eq(OWNER_USER), any(AppResourceScope.class), eq(CREDENTIAL_ID), isNull()))
            .thenReturn(AuthResult.pass());
        InternalResponse<Boolean> refResp = new InternalResponse<>();
        refResp.setData(false);
        when(fileSourceService.existsFileSourceUsingCredential(eq(OWNER_APP_ID), eq(CREDENTIAL_ID)))
            .thenReturn(refResp);
        when(credentialDAO.deleteCredentialById(eq(OWNER_APP_ID), eq(CREDENTIAL_ID))).thenReturn(1);

        Integer affected = credentialService.deleteCredentialById(OWNER_USER, OWNER_APP_ID, CREDENTIAL_ID);

        assertThat(affected).isEqualTo(1);
        ArgumentCaptor<AppResourceScope> scopeCaptor = ArgumentCaptor.forClass(AppResourceScope.class);
        verify(ticketAuthService, times(1))
            .authManageTicket(eq(OWNER_USER), scopeCaptor.capture(), eq(CREDENTIAL_ID), isNull());
        assertThat(scopeCaptor.getValue().getAppId()).isEqualTo(OWNER_APP_ID);
        verify(credentialDAO, times(1)).deleteCredentialById(eq(OWNER_APP_ID), eq(CREDENTIAL_ID));
    }
}
