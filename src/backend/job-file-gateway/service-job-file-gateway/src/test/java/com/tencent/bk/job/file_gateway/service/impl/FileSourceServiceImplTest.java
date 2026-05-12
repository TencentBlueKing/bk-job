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
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.file_gateway.auth.FileSourceAuthService;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceTypeDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileWorkerDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 FileSourceServiceImpl#saveFileSource / updateFileSourceById 在 credentialId 非空时校验 USE_TICKET。
 *
 * <p>覆盖 save/update × {credentialId 为空 / 有 USE_TICKET / 无 USE_TICKET} 共 6 个用例。</p>
 */
class FileSourceServiceImplTest {

    private static final String USERNAME = "u1";
    private static final Long APP_ID = 1001L;
    private static final Integer FILE_SOURCE_ID = 5;
    private static final String CODE = "fs-code";
    private static final String ALIAS = "fs-alias";
    private static final String CREDENTIAL_ID = "cred-1";

    private FileSourceAuthService fileSourceAuthService;
    private FileSourceDAO fileSourceDAO;
    private FileSourceServiceImpl service;
    private ApplicationContextRegister contextRegister;

    @BeforeEach
    void setUp() {
        FileSourceTypeDAO fileSourceTypeDAO = mock(FileSourceTypeDAO.class);
        fileSourceDAO = mock(FileSourceDAO.class);
        FileWorkerDAO fileWorkerDAO = mock(FileWorkerDAO.class);
        fileSourceAuthService = mock(FileSourceAuthService.class);

        service = new FileSourceServiceImpl(
            fileSourceTypeDAO,
            fileSourceDAO,
            fileWorkerDAO,
            fileSourceAuthService
        );

        // updateFileSourceById 末尾的审计上下文会通过 ApplicationContextRegister 获取 AppScopeMappingService，
        // 单元测试场景下注入一个最小可用的 ApplicationContext。
        AppScopeMappingService appScopeMappingService = mock(AppScopeMappingService.class);
        when(appScopeMappingService.getScopeByAppId(eq(APP_ID)))
            .thenReturn(new ResourceScope(ResourceScopeTypeEnum.BIZ.getValue(), "2001"));
        ApplicationContext mockContext = mock(ApplicationContext.class);
        when(mockContext.getBean(AppScopeMappingService.class)).thenReturn(appScopeMappingService);
        contextRegister = new ApplicationContextRegister();
        contextRegister.setApplicationContext(mockContext);

        when(fileSourceAuthService.authCreateFileSource(eq(USERNAME), any(AppResourceScope.class)))
            .thenReturn(AuthResult.pass());
        when(fileSourceAuthService.authManageFileSource(eq(USERNAME), any(AppResourceScope.class),
            anyInt(), any())).thenReturn(AuthResult.pass());
        when(fileSourceDAO.existsCode(anyLong(), anyString())).thenReturn(false);
        when(fileSourceDAO.existsCodeExceptId(anyLong(), anyString(), anyInt())).thenReturn(false);
        when(fileSourceDAO.checkFileSourceExists(anyLong(), anyString())).thenReturn(false);
        when(fileSourceDAO.insertFileSource(any(FileSourceDTO.class))).thenReturn(FILE_SOURCE_ID);
        when(fileSourceDAO.getFileSourceById(eq(FILE_SOURCE_ID))).thenAnswer(inv -> {
            FileSourceDTO fs = new FileSourceDTO();
            fs.setId(FILE_SOURCE_ID);
            fs.setAlias(ALIAS);
            fs.setAppId(APP_ID);
            fs.setEnable(true);
            fs.setPublicFlag(false);
            fs.setStatus(0);
            return fs;
        });
        when(fileSourceAuthService.registerFileSource(anyString(), anyInt(), anyString())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        contextRegister.setApplicationContext(null);
    }

    private FileSourceDTO buildFileSource(String credentialId, boolean withId) {
        FileSourceDTO fileSource = new FileSourceDTO();
        if (withId) {
            fileSource.setId(FILE_SOURCE_ID);
        }
        fileSource.setAppId(APP_ID);
        fileSource.setCode(CODE);
        fileSource.setAlias(ALIAS);
        fileSource.setCredentialId(credentialId);
        // toEsbFileSourceV3DTO 会对这些 Boolean/Integer 字段做拆箱，提前赋值避免 NPE
        fileSource.setEnable(true);
        fileSource.setPublicFlag(false);
        fileSource.setStatus(0);
        return fileSource;
    }

    @Test
    @DisplayName("saveFileSource：credentialId 为空时不触发 USE_TICKET 校验")
    void saveFileSource_blankCredentialIdSkipsUseTicketCheck() {
        service.saveFileSource(USERNAME, APP_ID, buildFileSource(null, false));

        verify(fileSourceAuthService, never())
            .authUseTicket(anyString(), any(AppResourceScope.class), anyString());
    }

    @Test
    @DisplayName("saveFileSource：credentialId 非空且有 USE_TICKET 权限时通过")
    void saveFileSource_hasUseTicketPermission() {
        when(fileSourceAuthService.authUseTicket(eq(USERNAME), any(AppResourceScope.class), eq(CREDENTIAL_ID)))
            .thenReturn(AuthResult.pass());

        service.saveFileSource(USERNAME, APP_ID, buildFileSource(CREDENTIAL_ID, false));

        verify(fileSourceAuthService).authUseTicket(eq(USERNAME), any(AppResourceScope.class), eq(CREDENTIAL_ID));
    }

    @Test
    @DisplayName("saveFileSource：credentialId 非空且无 USE_TICKET 权限时抛 PermissionDeniedException")
    void saveFileSource_noUseTicketPermission() {
        when(fileSourceAuthService.authUseTicket(eq(USERNAME), any(AppResourceScope.class), eq(CREDENTIAL_ID)))
            .thenReturn(AuthResult.fail());

        assertThatThrownBy(() -> service.saveFileSource(USERNAME, APP_ID, buildFileSource(CREDENTIAL_ID, false)))
            .isInstanceOf(PermissionDeniedException.class);
        verify(fileSourceDAO, never()).insertFileSource(any(FileSourceDTO.class));
    }

    @Test
    @DisplayName("updateFileSourceById：credentialId 为空时不触发 USE_TICKET 校验")
    void updateFileSourceById_blankCredentialIdSkipsUseTicketCheck() {
        service.updateFileSourceById(USERNAME, APP_ID, buildFileSource(null, true));

        verify(fileSourceAuthService, never())
            .authUseTicket(anyString(), any(AppResourceScope.class), anyString());
    }

    @Test
    @DisplayName("updateFileSourceById：credentialId 非空且有 USE_TICKET 权限时通过")
    void updateFileSourceById_hasUseTicketPermission() {
        when(fileSourceAuthService.authUseTicket(eq(USERNAME), any(AppResourceScope.class), eq(CREDENTIAL_ID)))
            .thenReturn(AuthResult.pass());

        service.updateFileSourceById(USERNAME, APP_ID, buildFileSource(CREDENTIAL_ID, true));

        verify(fileSourceAuthService).authUseTicket(eq(USERNAME), any(AppResourceScope.class), eq(CREDENTIAL_ID));
    }

    @Test
    @DisplayName("updateFileSourceById：credentialId 非空且无 USE_TICKET 权限时抛 PermissionDeniedException")
    void updateFileSourceById_noUseTicketPermission() {
        when(fileSourceAuthService.authUseTicket(eq(USERNAME), any(AppResourceScope.class), eq(CREDENTIAL_ID)))
            .thenReturn(AuthResult.fail());

        assertThatThrownBy(() -> service.updateFileSourceById(USERNAME, APP_ID, buildFileSource(CREDENTIAL_ID, true)))
            .isInstanceOf(PermissionDeniedException.class);
        verify(fileSourceDAO, never()).updateFileSource(any(FileSourceDTO.class));
    }
}
