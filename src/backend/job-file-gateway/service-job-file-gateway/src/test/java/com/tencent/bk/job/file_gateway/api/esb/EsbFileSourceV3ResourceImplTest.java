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
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.JobContextUtil;
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
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 ESB 获取文件源详情接口（GET/POST `/get_file_source_detail`）的 IAM 鉴权与业务隔离，
 * 与 #4280 同范畴的"权限校验不正确"修复。
 *
 * <p>修复要点：</p>
 * <ul>
 *     <li>POST 入口先调用 {@code req.fillAppResourceScope(appScopeMappingService)} 补全 appId；</li>
 *     <li>按 (appId, code) 联合查询 DAO，跨业务/不存在统一抛 {@link NotFoundException}；</li>
 *     <li>查到文件源后再做 {@link com.tencent.bk.job.common.iam.constant.ActionId#VIEW_FILE_SOURCE} 鉴权，
 *         未通过即抛 {@link PermissionDeniedException}；</li>
 *     <li>GET 入口委托给 POST，复用同一份隔离与鉴权逻辑。</li>
 * </ul>
 */
@DisplayName("EsbFileSourceV3ResourceImpl 文件源详情查询鉴权测试")
class EsbFileSourceV3ResourceImplTest {

    private FileSourceService fileSourceService;
    private AppScopeMappingService appScopeMappingService;
    private FileSourceValidateService fileSourceValidateService;
    private FileSourceAuthService fileSourceAuthService;
    private EsbFileSourceV3ResourceImpl resource;

    private MockedStatic<ApplicationContextRegister> applicationContextRegisterMock;

    private static final String TENANT_ID = "default";
    private static final String USERNAME = "test_user";
    private static final long APP_ID = 100L;
    private static final long OTHER_APP_ID = 200L;
    private static final String SCOPE_TYPE = "biz";
    private static final String SCOPE_ID = "100";
    private static final String FILE_SOURCE_CODE = "fs_code_1";
    private static final int FILE_SOURCE_ID = 999;
    private static final String FILE_SOURCE_ALIAS = "fs_alias";
    private static final String CREDENTIAL_ID = "credential_1";

    @BeforeEach
    void setUp() {
        fileSourceService = mock(FileSourceService.class);
        appScopeMappingService = mock(AppScopeMappingService.class);
        fileSourceValidateService = mock(FileSourceValidateService.class);
        fileSourceAuthService = mock(FileSourceAuthService.class);
        resource = new EsbFileSourceV3ResourceImpl(
            fileSourceService,
            appScopeMappingService,
            fileSourceValidateService,
            fileSourceAuthService
        );

        // appScopeMappingService.getAppIdByScope(scopeType, scopeId) → APP_ID
        when(appScopeMappingService.getAppIdByScope(eq(SCOPE_TYPE), eq(SCOPE_ID))).thenReturn(APP_ID);

        // toEsbFileSourceV3DTO 内部依赖 ApplicationContextRegister.getBean(AppScopeMappingService.class)
        // 通过 mockStatic 注入同一份 appScopeMappingService 满足该静态查找
        applicationContextRegisterMock = Mockito.mockStatic(ApplicationContextRegister.class);
        when(appScopeMappingService.getScopeByAppId(anyLong()))
            .thenReturn(new ResourceScope(ResourceScopeTypeEnum.BIZ, SCOPE_ID));
        applicationContextRegisterMock.when(() -> ApplicationContextRegister.getBean(AppScopeMappingService.class))
            .thenReturn(appScopeMappingService);

        User user = new User(TENANT_ID, USERNAME, USERNAME);
        JobContextUtil.setUser(user);
    }

    @AfterEach
    void tearDown() {
        if (applicationContextRegisterMock != null) {
            applicationContextRegisterMock.close();
        }
        JobContextUtil.unsetContext();
    }

    private EsbGetFileSourceDetailV3Req buildReq() {
        EsbGetFileSourceDetailV3Req req = new EsbGetFileSourceDetailV3Req();
        req.setScopeType(SCOPE_TYPE);
        req.setScopeId(SCOPE_ID);
        req.setCode(FILE_SOURCE_CODE);
        return req;
    }

    private FileSourceDTO buildFileSourceDTO() {
        FileSourceDTO dto = new FileSourceDTO();
        dto.setId(FILE_SOURCE_ID);
        dto.setAppId(APP_ID);
        dto.setCode(FILE_SOURCE_CODE);
        dto.setAlias(FILE_SOURCE_ALIAS);
        dto.setCredentialId(CREDENTIAL_ID);
        dto.setPublicFlag(false);
        dto.setEnable(true);
        dto.setCreator(USERNAME);
        dto.setLastModifyUser(USERNAME);
        dto.setCreateTime(System.currentTimeMillis());
        dto.setLastModifyTime(System.currentTimeMillis());
        return dto;
    }

    // ============================ POST 入口 ============================

    @Test
    @DisplayName("POST: 文件源不存在时抛 NotFoundException 且不进入 IAM 鉴权")
    void postShouldThrowNotFoundWhenFileSourceNotFound() {
        when(fileSourceService.getFileSourceByCode(eq(APP_ID), eq(FILE_SOURCE_CODE))).thenReturn(null);

        assertThatThrownBy(() -> resource.getFileSourceDetailUsingPost(USERNAME, "appCode", buildReq()))
            .isInstanceOf(NotFoundException.class);

        verify(fileSourceAuthService, never())
            .authViewFileSource(any(), any(AppResourceScope.class), anyInt(), anyString());
        verify(fileSourceAuthService, never())
            .authViewFileSource(any(), any(AppResourceScope.class), anyInt(), isNull());
    }

    @Test
    @DisplayName("POST: 跨业务（错误 appId）查不到文件源同样抛 NotFoundException")
    void postShouldThrowNotFoundForCrossBusinessQuery() {
        // 模拟用户传入的 scope 仍解析为 APP_ID，但用户实际想查的是属于 OTHER_APP_ID 的文件源
        // 由 DAO 层 (appId, code) 联合过滤天然隔离，返回 null
        when(fileSourceService.getFileSourceByCode(eq(APP_ID), eq(FILE_SOURCE_CODE))).thenReturn(null);
        FileSourceDTO otherAppFileSource = buildFileSourceDTO();
        otherAppFileSource.setAppId(OTHER_APP_ID);
        when(fileSourceService.getFileSourceByCode(eq(OTHER_APP_ID), eq(FILE_SOURCE_CODE)))
            .thenReturn(otherAppFileSource);

        assertThatThrownBy(() -> resource.getFileSourceDetailUsingPost(USERNAME, "appCode", buildReq()))
            .isInstanceOf(NotFoundException.class);

        // DAO 层只用请求里的 APP_ID 过滤，永远查不到 OTHER_APP_ID 的数据
        verify(fileSourceService, times(1)).getFileSourceByCode(eq(APP_ID), eq(FILE_SOURCE_CODE));
        verify(fileSourceService, never()).getFileSourceByCode(eq(OTHER_APP_ID), anyString());
        verify(fileSourceAuthService, never())
            .authViewFileSource(any(), any(AppResourceScope.class), anyInt(), anyString());
    }

    @Test
    @DisplayName("POST: 文件源存在但用户无 view_file_source 权限时抛 PermissionDeniedException")
    void postShouldThrowPermissionDeniedWhenNoViewPermission() {
        when(fileSourceService.getFileSourceByCode(eq(APP_ID), eq(FILE_SOURCE_CODE)))
            .thenReturn(buildFileSourceDTO());
        when(fileSourceAuthService.authViewFileSource(
            any(), any(AppResourceScope.class), eq(FILE_SOURCE_ID), eq(FILE_SOURCE_ALIAS)))
            .thenReturn(AuthResult.fail(new User(TENANT_ID, USERNAME, USERNAME)));

        assertThatThrownBy(() -> resource.getFileSourceDetailUsingPost(USERNAME, "appCode", buildReq()))
            .isInstanceOf(PermissionDeniedException.class);

        verify(fileSourceAuthService, times(1))
            .authViewFileSource(any(), any(AppResourceScope.class), eq(FILE_SOURCE_ID), eq(FILE_SOURCE_ALIAS));
    }

    @Test
    @DisplayName("POST: 文件源存在且用户有权限时正常返回，响应中 credential_id 字段被序列化")
    void postShouldReturnDetailWhenAuthorized() {
        when(fileSourceService.getFileSourceByCode(eq(APP_ID), eq(FILE_SOURCE_CODE)))
            .thenReturn(buildFileSourceDTO());
        when(fileSourceAuthService.authViewFileSource(
            any(), any(AppResourceScope.class), eq(FILE_SOURCE_ID), eq(FILE_SOURCE_ALIAS)))
            .thenReturn(AuthResult.pass(new User(TENANT_ID, USERNAME, USERNAME)));

        EsbResp<EsbFileSourceV3DTO> resp = resource.getFileSourceDetailUsingPost(
            USERNAME, "appCode", buildReq());

        assertThat(resp).isNotNull();
        EsbFileSourceV3DTO data = resp.getData();
        assertThat(data).isNotNull();
        assertThat(data.getId()).isEqualTo(FILE_SOURCE_ID);
        assertThat(data.getCode()).isEqualTo(FILE_SOURCE_CODE);
        assertThat(data.getAlias()).isEqualTo(FILE_SOURCE_ALIAS);
        // 重点回归：响应体中 credential_id 字段被正确写入（之前缺失鉴权暴露的就是这个字段）
        assertThat(data.getCredentialId()).isEqualTo(CREDENTIAL_ID);

        verify(fileSourceAuthService, times(1))
            .authViewFileSource(any(), any(AppResourceScope.class), eq(FILE_SOURCE_ID), eq(FILE_SOURCE_ALIAS));
    }

    // ============================ GET 入口 ============================

    @Test
    @DisplayName("GET: 委托给 POST，文件源存在且有权限时同样返回详情")
    void getShouldDelegateToPostAndReturnDetail() {
        when(fileSourceService.getFileSourceByCode(eq(APP_ID), eq(FILE_SOURCE_CODE)))
            .thenReturn(buildFileSourceDTO());
        when(fileSourceAuthService.authViewFileSource(
            any(), any(AppResourceScope.class), eq(FILE_SOURCE_ID), eq(FILE_SOURCE_ALIAS)))
            .thenReturn(AuthResult.pass(new User(TENANT_ID, USERNAME, USERNAME)));

        EsbResp<EsbFileSourceV3DTO> resp = resource.getFileSourceDetail(
            USERNAME, "appCode", null, SCOPE_TYPE, SCOPE_ID, FILE_SOURCE_CODE);

        assertThat(resp).isNotNull();
        assertThat(resp.getData()).isNotNull();
        assertThat(resp.getData().getCredentialId()).isEqualTo(CREDENTIAL_ID);

        verify(fileSourceAuthService, times(1))
            .authViewFileSource(any(), any(AppResourceScope.class), eq(FILE_SOURCE_ID), eq(FILE_SOURCE_ALIAS));
    }

    @Test
    @DisplayName("GET: 委托给 POST，无权限时同样抛 PermissionDeniedException")
    void getShouldDelegateToPostAndDenyWithoutPermission() {
        when(fileSourceService.getFileSourceByCode(eq(APP_ID), eq(FILE_SOURCE_CODE)))
            .thenReturn(buildFileSourceDTO());
        when(fileSourceAuthService.authViewFileSource(
            any(), any(AppResourceScope.class), eq(FILE_SOURCE_ID), eq(FILE_SOURCE_ALIAS)))
            .thenReturn(AuthResult.fail(new User(TENANT_ID, USERNAME, USERNAME)));

        assertThatThrownBy(() -> resource.getFileSourceDetail(
            USERNAME, "appCode", null, SCOPE_TYPE, SCOPE_ID, FILE_SOURCE_CODE))
            .isInstanceOf(PermissionDeniedException.class);
    }
}
