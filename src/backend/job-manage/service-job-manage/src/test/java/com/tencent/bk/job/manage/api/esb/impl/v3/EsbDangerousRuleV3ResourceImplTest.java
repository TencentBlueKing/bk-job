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
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.api.common.constants.EnableStatusEnum;
import com.tencent.bk.job.manage.auth.NoResourceScopeAuthService;
import com.tencent.bk.job.manage.model.dto.globalsetting.DangerousRuleDTO;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbCreateDangerousRuleV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetDangerousRuleV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbManageDangerousRuleV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbUpdateDangerousRuleV3Req;
import com.tencent.bk.job.manage.service.CurrentTenantDangerousRuleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证高危语句规则相关 ESB 接口的权限校验，
 * 与 Web 接口 /web/dangerousRule/** 的鉴权拦截器保持一致。
 */
@DisplayName("EsbDangerousRuleV3ResourceImpl 权限校验测试")
class EsbDangerousRuleV3ResourceImplTest {

    private CurrentTenantDangerousRuleService currentTenantDangerousRuleService;
    private NoResourceScopeAuthService noResourceScopeAuthService;
    private EsbDangerousRuleV3ResourceImpl resource;

    private static final String TENANT_ID = "default";
    private static final String NORMAL_USER = "normal_user";
    private static final String ADMIN_USER = "admin_user";

    @BeforeEach
    void setUp() {
        currentTenantDangerousRuleService = mock(CurrentTenantDangerousRuleService.class);
        noResourceScopeAuthService = mock(NoResourceScopeAuthService.class);
        resource = new EsbDangerousRuleV3ResourceImpl(
            currentTenantDangerousRuleService,
            noResourceScopeAuthService
        );
    }

    @AfterEach
    void tearDown() {
        JobContextUtil.unsetContext();
    }

    private User mockUser(String username) {
        User user = new User(TENANT_ID, username, username);
        JobContextUtil.setUser(user);
        return user;
    }

    private void givenNoPermission(User user) {
        when(noResourceScopeAuthService.authHighRiskDetectRule(any()))
            .thenReturn(AuthResult.fail(user));
    }

    private void givenHasPermission(User user) {
        when(noResourceScopeAuthService.authHighRiskDetectRule(any()))
            .thenReturn(AuthResult.pass(user));
    }

    private EsbCreateDangerousRuleV3Req buildCreateReq() {
        EsbCreateDangerousRuleV3Req req = new EsbCreateDangerousRuleV3Req();
        req.setExpression("rm -rf");
        req.setDescription("desc");
        req.setScriptTypeList(Collections.singletonList((byte) 1));
        req.setAction(1);
        return req;
    }

    private EsbUpdateDangerousRuleV3Req buildUpdateReq() {
        EsbUpdateDangerousRuleV3Req req = new EsbUpdateDangerousRuleV3Req();
        req.setId(1L);
        req.setExpression("rm -rf");
        req.setDescription("desc");
        req.setScriptTypeList(Collections.singletonList((byte) 1));
        req.setAction(1);
        return req;
    }

    private EsbManageDangerousRuleV3Req buildManageReq(long id) {
        EsbManageDangerousRuleV3Req req = new EsbManageDangerousRuleV3Req();
        req.setId(id);
        return req;
    }

    @Test
    @DisplayName("普通用户创建高危语句规则被拒绝且不进入业务逻辑")
    void normalUserCreateDangerousRuleShouldBeDenied() {
        User user = mockUser(NORMAL_USER);
        givenNoPermission(user);
        EsbCreateDangerousRuleV3Req req = buildCreateReq();

        assertThatThrownBy(() -> resource.createDangerousRule(NORMAL_USER, "test_app", req))
            .isInstanceOf(PermissionDeniedException.class);

        verify(currentTenantDangerousRuleService, never()).createDangerousRule(any(), any());
    }

    @Test
    @DisplayName("普通用户更新高危语句规则被拒绝且不进入业务逻辑")
    void normalUserUpdateDangerousRuleShouldBeDenied() {
        User user = mockUser(NORMAL_USER);
        givenNoPermission(user);
        EsbUpdateDangerousRuleV3Req req = buildUpdateReq();

        assertThatThrownBy(() -> resource.updateDangerousRule(NORMAL_USER, "test_app", req))
            .isInstanceOf(PermissionDeniedException.class);

        verify(currentTenantDangerousRuleService, never()).getDangerousRuleById(anyLong());
        verify(currentTenantDangerousRuleService, never()).updateDangerousRule(any(), any());
    }

    @Test
    @DisplayName("普通用户删除高危语句规则被拒绝且不进入业务逻辑")
    void normalUserDeleteDangerousRuleShouldBeDenied() {
        User user = mockUser(NORMAL_USER);
        givenNoPermission(user);

        assertThatThrownBy(() -> resource.deleteDangerousRule(NORMAL_USER, "test_app", buildManageReq(1L)))
            .isInstanceOf(PermissionDeniedException.class);

        verify(currentTenantDangerousRuleService, never()).deleteDangerousRuleById(any(), anyLong());
    }

    @Test
    @DisplayName("普通用户启用高危语句规则被拒绝且不进入业务逻辑")
    void normalUserEnableDangerousRuleShouldBeDenied() {
        User user = mockUser(NORMAL_USER);
        givenNoPermission(user);

        assertThatThrownBy(() -> resource.enableDangerousRule(NORMAL_USER, "test_app", buildManageReq(1L)))
            .isInstanceOf(PermissionDeniedException.class);

        verify(currentTenantDangerousRuleService, never())
            .updateDangerousRuleStatus(any(), anyLong(), any());
    }

    @Test
    @DisplayName("普通用户停用高危语句规则被拒绝且不进入业务逻辑")
    void normalUserDisableDangerousRuleShouldBeDenied() {
        User user = mockUser(NORMAL_USER);
        givenNoPermission(user);

        assertThatThrownBy(() -> resource.disableDangerousRule(NORMAL_USER, "test_app", buildManageReq(1L)))
            .isInstanceOf(PermissionDeniedException.class);

        verify(currentTenantDangerousRuleService, never())
            .updateDangerousRuleStatus(any(), anyLong(), any());
    }

    @Test
    @DisplayName("管理员创建高危语句规则正常进入业务逻辑")
    void adminUserCreateDangerousRuleShouldPass() {
        User user = mockUser(ADMIN_USER);
        givenHasPermission(user);
        DangerousRuleDTO dto = buildDangerousRuleDTO();
        when(currentTenantDangerousRuleService.createDangerousRule(any(), any())).thenReturn(dto);

        assertThatCode(() -> resource.createDangerousRule(ADMIN_USER, "test_app", buildCreateReq()))
            .doesNotThrowAnyException();

        verify(currentTenantDangerousRuleService, times(1)).createDangerousRule(any(), any());
    }

    @Test
    @DisplayName("管理员更新高危语句规则正常进入业务逻辑")
    void adminUserUpdateDangerousRuleShouldPass() {
        User user = mockUser(ADMIN_USER);
        givenHasPermission(user);
        DangerousRuleDTO dto = buildDangerousRuleDTO();
        when(currentTenantDangerousRuleService.getDangerousRuleById(anyLong())).thenReturn(dto);
        when(currentTenantDangerousRuleService.updateDangerousRule(any(), any())).thenReturn(dto);

        assertThatCode(() -> resource.updateDangerousRule(ADMIN_USER, "test_app", buildUpdateReq()))
            .doesNotThrowAnyException();

        verify(currentTenantDangerousRuleService, times(1)).updateDangerousRule(any(), any());
    }

    private DangerousRuleDTO buildDangerousRuleDTO() {
        DangerousRuleDTO dto = new DangerousRuleDTO();
        dto.setId(1L);
        dto.setExpression("rm -rf");
        dto.setDescription("desc");
        dto.setScriptType(1);
        dto.setAction(1);
        dto.setStatus(EnableStatusEnum.DISABLED.getValue());
        return dto;
    }

    @Test
    @DisplayName("管理员删除高危语句规则正常进入业务逻辑")
    void adminUserDeleteDangerousRuleShouldPass() {
        User user = mockUser(ADMIN_USER);
        givenHasPermission(user);

        assertThatCode(() -> resource.deleteDangerousRule(ADMIN_USER, "test_app", buildManageReq(1L)))
            .doesNotThrowAnyException();

        verify(currentTenantDangerousRuleService, times(1))
            .deleteDangerousRuleById(any(), eq(1L));
    }

    @Test
    @DisplayName("管理员启用高危语句规则正常进入业务逻辑")
    void adminUserEnableDangerousRuleShouldPass() {
        User user = mockUser(ADMIN_USER);
        givenHasPermission(user);
        DangerousRuleDTO dto = new DangerousRuleDTO();
        dto.setId(1L);
        dto.setStatus(EnableStatusEnum.ENABLED.getValue());
        when(currentTenantDangerousRuleService.updateDangerousRuleStatus(any(), anyLong(), any()))
            .thenReturn(dto);

        assertThatCode(() -> resource.enableDangerousRule(ADMIN_USER, "test_app", buildManageReq(1L)))
            .doesNotThrowAnyException();

        verify(currentTenantDangerousRuleService, times(1))
            .updateDangerousRuleStatus(any(), eq(1L), eq(EnableStatusEnum.ENABLED));
    }

    @Test
    @DisplayName("管理员停用高危语句规则正常进入业务逻辑")
    void adminUserDisableDangerousRuleShouldPass() {
        User user = mockUser(ADMIN_USER);
        givenHasPermission(user);
        DangerousRuleDTO dto = new DangerousRuleDTO();
        dto.setId(1L);
        dto.setStatus(EnableStatusEnum.DISABLED.getValue());
        when(currentTenantDangerousRuleService.updateDangerousRuleStatus(any(), anyLong(), any()))
            .thenReturn(dto);

        assertThatCode(() -> resource.disableDangerousRule(ADMIN_USER, "test_app", buildManageReq(1L)))
            .doesNotThrowAnyException();

        verify(currentTenantDangerousRuleService, times(1))
            .updateDangerousRuleStatus(any(), eq(1L), eq(EnableStatusEnum.DISABLED));
    }

    @Test
    @DisplayName("普通用户查询高危语句规则列表被拒绝且不进入业务逻辑")
    void normalUserGetDangerousRuleListShouldBeDenied() {
        User user = mockUser(NORMAL_USER);
        givenNoPermission(user);

        EsbGetDangerousRuleV3Req req = new EsbGetDangerousRuleV3Req();
        assertThatThrownBy(() -> resource.getDangerousRuleListUsingPost(NORMAL_USER, "test_app", req))
            .isInstanceOf(PermissionDeniedException.class);

        verify(currentTenantDangerousRuleService, never()).listDangerousRules(any());
    }

    @Test
    @DisplayName("管理员查询高危语句规则列表正常进入业务逻辑")
    void adminUserGetDangerousRuleListShouldPass() {
        User user = mockUser(ADMIN_USER);
        givenHasPermission(user);
        when(currentTenantDangerousRuleService.listDangerousRules(any()))
            .thenReturn(Collections.emptyList());

        EsbGetDangerousRuleV3Req req = new EsbGetDangerousRuleV3Req();
        assertThatCode(() -> resource.getDangerousRuleListUsingPost(ADMIN_USER, "test_app", req))
            .doesNotThrowAnyException();

        verify(currentTenantDangerousRuleService, times(1)).listDangerousRules(any());
    }
}
