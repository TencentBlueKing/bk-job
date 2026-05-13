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

import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.manage.api.common.constants.EnableStatusEnum;
import com.tencent.bk.job.manage.auth.NoResourceScopeAuthService;
import com.tencent.bk.job.manage.model.dto.globalsetting.DangerousRuleDTO;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbCreateDangerousRuleV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetDangerousRuleV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbManageDangerousRuleV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbUpdateDangerousRuleV3Req;
import com.tencent.bk.job.manage.model.query.DangerousRuleQuery;
import com.tencent.bk.job.manage.service.DangerousRuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 EsbDangerousRuleV3ResourceImpl 6 个接口（5 写 + 1 查）在调用 delegate 业务方法之前先做 IAM 鉴权
 */
class EsbDangerousRuleV3ResourceImplTest {

    private static final String NORMAL_USER = "normal_user";
    private static final String ADMIN_USER = "admin";
    private static final String APP_CODE = "bk_job";

    private DangerousRuleService dangerousRuleService;
    private NoResourceScopeAuthService noResourceScopeAuthService;
    private EsbDangerousRuleV3ResourceImpl resource;

    @BeforeEach
    void setUp() {
        dangerousRuleService = mock(DangerousRuleService.class);
        noResourceScopeAuthService = mock(NoResourceScopeAuthService.class);
        resource = new EsbDangerousRuleV3ResourceImpl(dangerousRuleService, noResourceScopeAuthService);
    }

    private void mockAuthPass(String username) {
        when(noResourceScopeAuthService.authHighRiskDetectRule(eq(username))).thenReturn(AuthResult.pass());
    }

    private void mockAuthFail(String username) {
        when(noResourceScopeAuthService.authHighRiskDetectRule(eq(username))).thenReturn(AuthResult.fail());
    }

    private EsbCreateDangerousRuleV3Req buildCreateReq() {
        EsbCreateDangerousRuleV3Req req = new EsbCreateDangerousRuleV3Req();
        req.setExpression("rm -rf");
        req.setDescription("desc");
        req.setAction(1);
        return req;
    }

    private EsbUpdateDangerousRuleV3Req buildUpdateReq() {
        EsbUpdateDangerousRuleV3Req req = new EsbUpdateDangerousRuleV3Req();
        req.setId(1L);
        req.setExpression("rm -rf");
        req.setDescription("desc");
        req.setAction(1);
        return req;
    }

    private EsbManageDangerousRuleV3Req buildManageReq() {
        EsbManageDangerousRuleV3Req req = new EsbManageDangerousRuleV3Req();
        req.setId(1L);
        return req;
    }

    @Test
    @DisplayName("createDangerousRule 普通用户被拒绝，delegate 不被调用")
    void createDangerousRule_normalUserDenied() {
        mockAuthFail(NORMAL_USER);
        assertThatThrownBy(() -> resource.createDangerousRule(NORMAL_USER, APP_CODE, buildCreateReq()))
            .isInstanceOf(PermissionDeniedException.class);
        verify(dangerousRuleService, never()).createDangerousRule(anyString(), any());
    }

    @Test
    @DisplayName("createDangerousRule 管理员放行，delegate 被调用")
    void createDangerousRule_adminAllowed() {
        mockAuthPass(ADMIN_USER);
        DangerousRuleDTO dto = new DangerousRuleDTO();
        dto.setScriptType(0);
        when(dangerousRuleService.createDangerousRule(eq(ADMIN_USER), any())).thenReturn(dto);

        resource.createDangerousRule(ADMIN_USER, APP_CODE, buildCreateReq());

        verify(dangerousRuleService).createDangerousRule(eq(ADMIN_USER), any());
    }

    @Test
    @DisplayName("updateDangerousRule 普通用户被拒绝，delegate 不被调用")
    void updateDangerousRule_normalUserDenied() {
        mockAuthFail(NORMAL_USER);
        assertThatThrownBy(() -> resource.updateDangerousRule(NORMAL_USER, APP_CODE, buildUpdateReq()))
            .isInstanceOf(PermissionDeniedException.class);
        verify(dangerousRuleService, never()).updateDangerousRule(anyString(), any());
        verify(dangerousRuleService, never()).getDangerousRuleById(anyLong());
    }

    @Test
    @DisplayName("updateDangerousRule 管理员放行，delegate 被调用")
    void updateDangerousRule_adminAllowed() {
        mockAuthPass(ADMIN_USER);
        DangerousRuleDTO origin = new DangerousRuleDTO();
        origin.setStatus(EnableStatusEnum.ENABLED.getValue());
        when(dangerousRuleService.getDangerousRuleById(anyLong())).thenReturn(origin);
        DangerousRuleDTO updated = new DangerousRuleDTO();
        updated.setScriptType(0);
        when(dangerousRuleService.updateDangerousRule(eq(ADMIN_USER), any())).thenReturn(updated);

        resource.updateDangerousRule(ADMIN_USER, APP_CODE, buildUpdateReq());

        verify(dangerousRuleService).updateDangerousRule(eq(ADMIN_USER), any());
    }

    @Test
    @DisplayName("deleteDangerousRule 普通用户被拒绝，delegate 不被调用")
    void deleteDangerousRule_normalUserDenied() {
        mockAuthFail(NORMAL_USER);
        assertThatThrownBy(() -> resource.deleteDangerousRule(NORMAL_USER, APP_CODE, buildManageReq()))
            .isInstanceOf(PermissionDeniedException.class);
        verify(dangerousRuleService, never()).deleteDangerousRuleById(anyString(), anyLong());
    }

    @Test
    @DisplayName("deleteDangerousRule 管理员放行，delegate 被调用")
    void deleteDangerousRule_adminAllowed() {
        mockAuthPass(ADMIN_USER);

        resource.deleteDangerousRule(ADMIN_USER, APP_CODE, buildManageReq());

        verify(dangerousRuleService).deleteDangerousRuleById(eq(ADMIN_USER), eq(1L));
    }

    @Test
    @DisplayName("enableDangerousRule 普通用户被拒绝，delegate 不被调用")
    void enableDangerousRule_normalUserDenied() {
        mockAuthFail(NORMAL_USER);
        assertThatThrownBy(() -> resource.enableDangerousRule(NORMAL_USER, APP_CODE, buildManageReq()))
            .isInstanceOf(PermissionDeniedException.class);
        verify(dangerousRuleService, never()).updateDangerousRuleStatus(anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("enableDangerousRule 管理员放行，delegate 被调用")
    void enableDangerousRule_adminAllowed() {
        mockAuthPass(ADMIN_USER);
        when(dangerousRuleService.updateDangerousRuleStatus(eq(ADMIN_USER), eq(1L), eq(EnableStatusEnum.ENABLED)))
            .thenReturn(new DangerousRuleDTO());

        resource.enableDangerousRule(ADMIN_USER, APP_CODE, buildManageReq());

        verify(dangerousRuleService).updateDangerousRuleStatus(eq(ADMIN_USER), eq(1L), eq(EnableStatusEnum.ENABLED));
    }

    @Test
    @DisplayName("disableDangerousRule 普通用户被拒绝，delegate 不被调用")
    void disableDangerousRule_normalUserDenied() {
        mockAuthFail(NORMAL_USER);
        assertThatThrownBy(() -> resource.disableDangerousRule(NORMAL_USER, APP_CODE, buildManageReq()))
            .isInstanceOf(PermissionDeniedException.class);
        verify(dangerousRuleService, never()).updateDangerousRuleStatus(anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("disableDangerousRule 管理员放行，delegate 被调用")
    void disableDangerousRule_adminAllowed() {
        mockAuthPass(ADMIN_USER);
        when(dangerousRuleService.updateDangerousRuleStatus(eq(ADMIN_USER), eq(1L), eq(EnableStatusEnum.DISABLED)))
            .thenReturn(new DangerousRuleDTO());

        resource.disableDangerousRule(ADMIN_USER, APP_CODE, buildManageReq());

        verify(dangerousRuleService).updateDangerousRuleStatus(eq(ADMIN_USER), eq(1L), eq(EnableStatusEnum.DISABLED));
    }

    @Test
    @DisplayName("getDangerousRuleListUsingPost 普通用户被拒绝，delegate 不被调用")
    void getDangerousRuleListUsingPost_normalUserDenied() {
        mockAuthFail(NORMAL_USER);
        assertThatThrownBy(() -> resource.getDangerousRuleListUsingPost(NORMAL_USER, APP_CODE,
            new EsbGetDangerousRuleV3Req()))
            .isInstanceOf(PermissionDeniedException.class);
        verify(dangerousRuleService, never()).listDangerousRules(any(DangerousRuleQuery.class));
    }

    @Test
    @DisplayName("getDangerousRuleListUsingPost 管理员放行，delegate 被调用")
    void getDangerousRuleListUsingPost_adminAllowed() {
        mockAuthPass(ADMIN_USER);
        when(dangerousRuleService.listDangerousRules(any(DangerousRuleQuery.class)))
            .thenReturn(Collections.emptyList());

        resource.getDangerousRuleListUsingPost(ADMIN_USER, APP_CODE, new EsbGetDangerousRuleV3Req());

        verify(dangerousRuleService).listDangerousRules(any(DangerousRuleQuery.class));
    }
}
