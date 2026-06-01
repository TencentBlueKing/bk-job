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

package com.tencent.bk.job.file_gateway.api.esb;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.file_gateway.auth.FileSourceAuthService;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.model.req.esb.v3.EsbGetFileSourceDetailV3Req;
import com.tencent.bk.job.file_gateway.model.resp.esb.v3.EsbFileSourceV3DTO;
import com.tencent.bk.job.file_gateway.service.FileSourceService;
import com.tencent.bk.job.file_gateway.service.validation.FileSourceValidateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 EsbFileSourceV3ResourceImpl#getFileSourceDetail(UsingPost) 修复了 IAM 鉴权缺失与跨业务隔离问题。
 */
class EsbFileSourceV3ResourceImplTest {

    private static final String USERNAME = "u1";
    private static final String APP_CODE = "bk_job";
    private static final Long APP_ID = 1001L;
    private static final String SCOPE_TYPE = ResourceScopeTypeEnum.BIZ.getValue();
    private static final String SCOPE_ID = "2001";
    private static final String CODE = "fs-code";
    private static final Integer FILE_SOURCE_ID = 5;
    private static final String ALIAS = "fs-alias";

    private FileSourceService fileSourceService;
    private FileSourceAuthService fileSourceAuthService;
    private AppScopeMappingService appScopeMappingService;
    private EsbFileSourceV3ResourceImpl resource;
    private ApplicationContextRegister contextRegister;

    @BeforeEach
    void setUp() {
        fileSourceService = mock(FileSourceService.class);
        appScopeMappingService = mock(AppScopeMappingService.class);
        FileSourceValidateService fileSourceValidateService = mock(FileSourceValidateService.class);
        fileSourceAuthService = mock(FileSourceAuthService.class);
        resource = new EsbFileSourceV3ResourceImpl(
            fileSourceService, appScopeMappingService, fileSourceValidateService, fileSourceAuthService);

        when(appScopeMappingService.getAppIdByScope(eq(SCOPE_TYPE), eq(SCOPE_ID))).thenReturn(APP_ID);

        // FileSourceDTO.toEsbFileSourceV3DTO 通过 ApplicationContextRegister 获取 AppScopeMappingService
        when(appScopeMappingService.getScopeByAppId(eq(APP_ID)))
            .thenReturn(new ResourceScope(SCOPE_TYPE, SCOPE_ID));
        ApplicationContext mockContext = mock(ApplicationContext.class);
        when(mockContext.getBean(AppScopeMappingService.class)).thenReturn(appScopeMappingService);
        contextRegister = new ApplicationContextRegister();
        contextRegister.setApplicationContext(mockContext);
    }

    @AfterEach
    void tearDown() {
        contextRegister.setApplicationContext(null);
    }

    private EsbGetFileSourceDetailV3Req buildReq() {
        EsbGetFileSourceDetailV3Req req = new EsbGetFileSourceDetailV3Req();
        req.setScopeType(SCOPE_TYPE);
        req.setScopeId(SCOPE_ID);
        req.setCode(CODE);
        req.fillAppResourceScope(appScopeMappingService);
        return req;
    }

    private FileSourceDTO buildFileSourceDTO() {
        FileSourceDTO fs = new FileSourceDTO();
        fs.setId(FILE_SOURCE_ID);
        fs.setAppId(APP_ID);
        fs.setAlias(ALIAS);
        fs.setCode(CODE);
        fs.setCredentialId("cred-1");
        fs.setEnable(true);
        fs.setPublicFlag(false);
        fs.setStatus(0);
        return fs;
    }

    @Test
    @DisplayName("文件源不存在时抛 NotFoundException(FAIL_TO_FIND_FILE_SOURCE_BY_CODE)，且不再做 IAM 鉴权")
    void getFileSourceDetailUsingPost_notFound() {
        when(fileSourceService.getFileSourceByCode(eq(APP_ID), eq(CODE))).thenReturn(null);

        assertThatThrownBy(() -> resource.getFileSourceDetailUsingPost(USERNAME, APP_CODE, buildReq()))
            .isInstanceOf(NotFoundException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FAIL_TO_FIND_FILE_SOURCE_BY_CODE);
        verify(fileSourceAuthService, never())
            .authViewFileSource(any(), any(AppResourceScope.class), anyInt(), any());
    }

    @Test
    @DisplayName("跨业务（appId 与文件源 code 不匹配，返回 null）等价为不存在")
    void getFileSourceDetailUsingPost_crossApp() {
        when(fileSourceService.getFileSourceByCode(eq(APP_ID), eq(CODE))).thenReturn(null);

        assertThatThrownBy(() -> resource.getFileSourceDetailUsingPost(USERNAME, APP_CODE, buildReq()))
            .isInstanceOf(NotFoundException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FAIL_TO_FIND_FILE_SOURCE_BY_CODE);
    }

    @Test
    @DisplayName("文件源存在但无 view_file_source 权限时抛 PermissionDeniedException")
    void getFileSourceDetailUsingPost_noViewPermission() {
        when(fileSourceService.getFileSourceByCode(eq(APP_ID), eq(CODE))).thenReturn(buildFileSourceDTO());
        when(fileSourceAuthService.authViewFileSource(eq(USERNAME), any(AppResourceScope.class),
            eq(FILE_SOURCE_ID), eq(ALIAS))).thenReturn(AuthResult.fail());

        assertThatThrownBy(() -> resource.getFileSourceDetailUsingPost(USERNAME, APP_CODE, buildReq()))
            .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    @DisplayName("文件源存在且有权限时返回完整的 EsbFileSourceV3DTO（含 credential_id）")
    void getFileSourceDetailUsingPost_success() {
        when(fileSourceService.getFileSourceByCode(eq(APP_ID), eq(CODE))).thenReturn(buildFileSourceDTO());
        when(fileSourceAuthService.authViewFileSource(eq(USERNAME), any(AppResourceScope.class),
            eq(FILE_SOURCE_ID), eq(ALIAS))).thenReturn(AuthResult.pass());

        EsbResp<EsbFileSourceV3DTO> resp = resource.getFileSourceDetailUsingPost(USERNAME, APP_CODE, buildReq());

        assertThat(resp.getData()).isNotNull();
        assertThat(resp.getData().getId()).isEqualTo(FILE_SOURCE_ID);
        assertThat(resp.getData().getAlias()).isEqualTo(ALIAS);
        assertThat(resp.getData().getCredentialId()).isEqualTo("cred-1");
        verify(fileSourceAuthService)
            .authViewFileSource(eq(USERNAME), any(AppResourceScope.class), eq(FILE_SOURCE_ID), eq(ALIAS));
    }

    @Test
    @DisplayName("POST 入口被直接调用（req 未预填 appId）时由 fillAppResourceScope 补全 appId 后正常返回")
    void getFileSourceDetailUsingPost_unFilledReqFillsAppId() {
        EsbGetFileSourceDetailV3Req req = new EsbGetFileSourceDetailV3Req();
        req.setScopeType(SCOPE_TYPE);
        req.setScopeId(SCOPE_ID);
        req.setCode(CODE);
        // 故意不调用 req.fillAppResourceScope，模拟 ESB 直接走 POST 入口的场景
        when(fileSourceService.getFileSourceByCode(eq(APP_ID), eq(CODE))).thenReturn(buildFileSourceDTO());
        when(fileSourceAuthService.authViewFileSource(eq(USERNAME), any(AppResourceScope.class),
            eq(FILE_SOURCE_ID), eq(ALIAS))).thenReturn(AuthResult.pass());

        EsbResp<EsbFileSourceV3DTO> resp = resource.getFileSourceDetailUsingPost(USERNAME, APP_CODE, req);

        assertThat(resp.getData()).isNotNull();
        assertThat(resp.getData().getId()).isEqualTo(FILE_SOURCE_ID);
        verify(fileSourceService).getFileSourceByCode(eq(APP_ID), eq(CODE));
        verify(fileSourceAuthService)
            .authViewFileSource(eq(USERNAME), any(AppResourceScope.class), eq(FILE_SOURCE_ID), eq(ALIAS));
    }
}
