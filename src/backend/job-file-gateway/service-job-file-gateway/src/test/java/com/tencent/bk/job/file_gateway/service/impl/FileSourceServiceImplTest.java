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

package com.tencent.bk.job.file_gateway.service.impl;

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.file_gateway.auth.FileSourceAuthService;
import com.tencent.bk.job.file_gateway.dao.filesource.CurrentTenantFileSourceDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceTypeDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileWorkerDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证文件源创建/更新时对绑定凭据 ID 的 USE_TICKET 权限校验，
 * 与 #4280 同范畴的"权限校验不正确"修复。
 *
 * <p>覆盖维度：</p>
 * <ul>
 *     <li>save: credentialId 为空 -> 不校验 USE_TICKET，正常落库</li>
 *     <li>save: 有 file_source 创建权限但无 use_ticket 权限 -> 抛 PermissionDeniedException 且不写库</li>
 *     <li>save: 同时具备两类权限 -> 正常落库</li>
 *     <li>update: credentialId 为空 -> 不校验 USE_TICKET，正常更新</li>
 *     <li>update: 有 file_source 管理权限但无 use_ticket 权限 -> 抛 PermissionDeniedException 且不写库</li>
 *     <li>update: 同时具备两类权限 -> 正常更新</li>
 * </ul>
 */
@DisplayName("FileSourceServiceImpl USE_TICKET 校验测试")
class FileSourceServiceImplTest {

    private FileSourceTypeDAO fileSourceTypeDAO;
    private CurrentTenantFileSourceDAO currentTenantFileSourceDAO;
    private FileWorkerDAO fileWorkerDAO;
    private FileSourceAuthService fileSourceAuthService;
    private FileSourceServiceImpl fileSourceService;

    private MockedStatic<ApplicationContextRegister> applicationContextRegisterMock;

    private static final String TENANT_ID = "default";
    private static final String NORMAL_USER = "normal_user";
    private static final long APP_ID = 100L;
    private static final String CREDENTIAL_ID = "credential-1";
    private static final int FILE_SOURCE_ID = 999;

    @BeforeEach
    void setUp() {
        fileSourceTypeDAO = mock(FileSourceTypeDAO.class);
        currentTenantFileSourceDAO = mock(CurrentTenantFileSourceDAO.class);
        fileWorkerDAO = mock(FileWorkerDAO.class);
        fileSourceAuthService = mock(FileSourceAuthService.class);
        fileSourceService = new FileSourceServiceImpl(
            fileSourceTypeDAO,
            currentTenantFileSourceDAO,
            fileWorkerDAO,
            fileSourceAuthService
        );

        // updateFileSourceById 中通过 ActionAuditContext.current().setOriginInstance(FileSourceDTO.toEsbFileSourceV3DTO(..))
        // 间接调用 ApplicationContextRegister.getBean(AppScopeMappingService.class)，
        // 在纯单测无 Spring 上下文，需 mockStatic 提供 AppScopeMappingService。
        applicationContextRegisterMock = Mockito.mockStatic(ApplicationContextRegister.class);
        AppScopeMappingService appScopeMappingService = mock(AppScopeMappingService.class);
        when(appScopeMappingService.getScopeByAppId(anyLong()))
            .thenReturn(new com.tencent.bk.job.common.model.dto.ResourceScope(
                ResourceScopeTypeEnum.BIZ, "100"));
        applicationContextRegisterMock.when(() -> ApplicationContextRegister.getBean(AppScopeMappingService.class))
            .thenReturn(appScopeMappingService);
    }

    @AfterEach
    void tearDown() {
        if (applicationContextRegisterMock != null) {
            applicationContextRegisterMock.close();
        }
    }

    private User mockUser() {
        return new User(TENANT_ID, NORMAL_USER, NORMAL_USER);
    }

    private FileSourceDTO buildFileSourceDTO(String credentialId, Integer id) {
        FileSourceDTO dto = new FileSourceDTO();
        dto.setId(id);
        dto.setAppId(APP_ID);
        dto.setCode("file_source_code");
        dto.setAlias("alias");
        dto.setCredentialId(credentialId);
        dto.setCreator(NORMAL_USER);
        dto.setLastModifyUser(NORMAL_USER);
        dto.setCreateTime(System.currentTimeMillis());
        dto.setLastModifyTime(System.currentTimeMillis());
        // publicFlag/enable 是基础类型字段（Boolean -> boolean 自动拆箱），需显式赋值避免 NPE
        dto.setPublicFlag(false);
        dto.setEnable(true);
        return dto;
    }

    private void givenFileSourceCreatePermission() {
        when(fileSourceAuthService.authCreateFileSource(any(), any()))
            .thenReturn(AuthResult.pass(mockUser()));
    }

    private void givenFileSourceManagePermission() {
        when(fileSourceAuthService.authManageFileSource(any(), any(), anyInt(), any()))
            .thenReturn(AuthResult.pass(mockUser()));
    }

    private void givenUseTicketPermission() {
        when(fileSourceAuthService.authUseTicket(any(), any(AppResourceScope.class), anyString()))
            .thenReturn(AuthResult.pass(mockUser()));
    }

    private void givenNoUseTicketPermission() {
        when(fileSourceAuthService.authUseTicket(any(), any(AppResourceScope.class), anyString()))
            .thenReturn(AuthResult.fail(mockUser()));
    }

    // ============================ save 场景 ============================

    @Test
    @DisplayName("save：credentialId 为空时跳过 USE_TICKET 校验，正常入库")
    void saveWithBlankCredentialIdShouldSkipUseTicketAuth() {
        User user = mockUser();
        givenFileSourceCreatePermission();
        when(currentTenantFileSourceDAO.existsCode(eq(APP_ID), anyString())).thenReturn(false);
        when(currentTenantFileSourceDAO.checkFileSourceExists(eq(APP_ID), anyString())).thenReturn(false);
        when(currentTenantFileSourceDAO.insertFileSource(any(FileSourceDTO.class))).thenReturn(FILE_SOURCE_ID);
        when(currentTenantFileSourceDAO.getFileSourceById(eq(FILE_SOURCE_ID)))
            .thenReturn(buildFileSourceDTO(null, FILE_SOURCE_ID));
        when(fileSourceAuthService.registerFileSource(any(), eq(FILE_SOURCE_ID), anyString())).thenReturn(true);

        FileSourceDTO toSave = buildFileSourceDTO(null, null);

        assertThatCode(() -> fileSourceService.saveFileSource(user, APP_ID, toSave))
            .doesNotThrowAnyException();

        verify(fileSourceAuthService, never())
            .authUseTicket(any(), any(AppResourceScope.class), anyString());
        verify(currentTenantFileSourceDAO, times(1)).insertFileSource(any(FileSourceDTO.class));
    }

    @Test
    @DisplayName("save：有 file_source 权限但无 use_ticket 权限时被拒，且不写库")
    void saveWithoutUseTicketPermissionShouldDeny() {
        User user = mockUser();
        givenFileSourceCreatePermission();
        givenNoUseTicketPermission();

        FileSourceDTO toSave = buildFileSourceDTO(CREDENTIAL_ID, null);

        assertThatThrownBy(() -> fileSourceService.saveFileSource(user, APP_ID, toSave))
            .isInstanceOf(PermissionDeniedException.class);

        verify(fileSourceAuthService, times(1))
            .authUseTicket(any(), any(AppResourceScope.class), eq(CREDENTIAL_ID));
        verify(currentTenantFileSourceDAO, never()).insertFileSource(any(FileSourceDTO.class));
    }

    @Test
    @DisplayName("save：同时具备 file_source 与 use_ticket 权限时正常入库")
    void saveWithFullPermissionShouldPass() {
        User user = mockUser();
        givenFileSourceCreatePermission();
        givenUseTicketPermission();
        when(currentTenantFileSourceDAO.existsCode(eq(APP_ID), anyString())).thenReturn(false);
        when(currentTenantFileSourceDAO.checkFileSourceExists(eq(APP_ID), anyString())).thenReturn(false);
        when(currentTenantFileSourceDAO.insertFileSource(any(FileSourceDTO.class))).thenReturn(FILE_SOURCE_ID);
        when(currentTenantFileSourceDAO.getFileSourceById(eq(FILE_SOURCE_ID)))
            .thenReturn(buildFileSourceDTO(CREDENTIAL_ID, FILE_SOURCE_ID));
        when(fileSourceAuthService.registerFileSource(any(), eq(FILE_SOURCE_ID), anyString())).thenReturn(true);

        FileSourceDTO toSave = buildFileSourceDTO(CREDENTIAL_ID, null);

        assertThatCode(() -> fileSourceService.saveFileSource(user, APP_ID, toSave))
            .doesNotThrowAnyException();

        verify(fileSourceAuthService, times(1))
            .authUseTicket(any(), any(AppResourceScope.class), eq(CREDENTIAL_ID));
        verify(currentTenantFileSourceDAO, times(1)).insertFileSource(any(FileSourceDTO.class));
    }

    // ============================ update 场景 ============================

    @Test
    @DisplayName("update：credentialId 为空时跳过 USE_TICKET 校验，正常更新")
    void updateWithBlankCredentialIdShouldSkipUseTicketAuth() {
        User user = mockUser();
        givenFileSourceManagePermission();
        when(currentTenantFileSourceDAO.existsCodeExceptId(eq(APP_ID), anyString(), eq(FILE_SOURCE_ID)))
            .thenReturn(false);
        when(currentTenantFileSourceDAO.getFileSourceById(eq(FILE_SOURCE_ID)))
            .thenReturn(buildFileSourceDTO(null, FILE_SOURCE_ID));
        when(currentTenantFileSourceDAO.updateFileSource(any(FileSourceDTO.class))).thenReturn(1);

        FileSourceDTO toUpdate = buildFileSourceDTO(null, FILE_SOURCE_ID);

        assertThatCode(() -> fileSourceService.updateFileSourceById(user, APP_ID, toUpdate))
            .doesNotThrowAnyException();

        verify(fileSourceAuthService, never())
            .authUseTicket(any(), any(AppResourceScope.class), anyString());
        verify(currentTenantFileSourceDAO, times(1)).updateFileSource(any(FileSourceDTO.class));
    }

    @Test
    @DisplayName("update：有 file_source 权限但无 use_ticket 权限时被拒，且不写库")
    void updateWithoutUseTicketPermissionShouldDeny() {
        User user = mockUser();
        givenFileSourceManagePermission();
        givenNoUseTicketPermission();

        FileSourceDTO toUpdate = buildFileSourceDTO(CREDENTIAL_ID, FILE_SOURCE_ID);

        assertThatThrownBy(() -> fileSourceService.updateFileSourceById(user, APP_ID, toUpdate))
            .isInstanceOf(PermissionDeniedException.class);

        verify(fileSourceAuthService, times(1))
            .authUseTicket(any(), any(AppResourceScope.class), eq(CREDENTIAL_ID));
        verify(currentTenantFileSourceDAO, never()).updateFileSource(any(FileSourceDTO.class));
    }

    @Test
    @DisplayName("update：同时具备 file_source 与 use_ticket 权限时正常更新")
    void updateWithFullPermissionShouldPass() {
        User user = mockUser();
        givenFileSourceManagePermission();
        givenUseTicketPermission();
        when(currentTenantFileSourceDAO.existsCodeExceptId(eq(APP_ID), anyString(), eq(FILE_SOURCE_ID)))
            .thenReturn(false);
        when(currentTenantFileSourceDAO.getFileSourceById(eq(FILE_SOURCE_ID)))
            .thenReturn(buildFileSourceDTO(CREDENTIAL_ID, FILE_SOURCE_ID));
        when(currentTenantFileSourceDAO.updateFileSource(any(FileSourceDTO.class))).thenReturn(1);

        FileSourceDTO toUpdate = buildFileSourceDTO(CREDENTIAL_ID, FILE_SOURCE_ID);

        assertThatCode(() -> fileSourceService.updateFileSourceById(user, APP_ID, toUpdate))
            .doesNotThrowAnyException();

        verify(fileSourceAuthService, times(1))
            .authUseTicket(any(), any(AppResourceScope.class), eq(CREDENTIAL_ID));
        verify(currentTenantFileSourceDAO, times(1)).updateFileSource(any(FileSourceDTO.class));
    }
}
