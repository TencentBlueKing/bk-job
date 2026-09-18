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

package com.tencent.bk.job.common.iam.service.impl;

import com.tencent.bk.job.common.iam.constant.IamV4ResourceTypeId;
import com.tencent.bk.job.common.iam.constant.IamV4RoleId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.sdk.iam.constants.SystemId;
import com.tencent.bk.sdk.iamv4.dto.V4AuthorizationResourceDTO;
import com.tencent.bk.sdk.iamv4.dto.request.V4AuthorizationDTO;
import com.tencent.bk.sdk.iamv4.service.IamV4AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 资源创建者授权（design.md D17）：v4 无资源实例注册接口，改为授予对应的单资源角色 + 新建实例。
 */
@DisplayName("v4 资源创建者授权")
class V4ActionAuthExecutorCreatorAuthTest {

    private static final String TENANT_ID = "tenant-1";
    private static final String CREATOR = "creator-user";

    private IamV4AuthService iamV4AuthService;
    private V4ActionAuthExecutor executor;

    @BeforeEach
    void setUp() {
        iamV4AuthService = mock(IamV4AuthService.class);
        executor = new V4ActionAuthExecutor(iamV4AuthService);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @DisplayName("9 种资源类型各自映射到对应的创建者角色")
    @CsvSource({
        ResourceTypeId.SCRIPT + ", " + IamV4RoleId.SCRIPT_MANAGER,
        ResourceTypeId.TEMPLATE + ", " + IamV4RoleId.JOB_TEMPLATE_MANAGER,
        ResourceTypeId.PLAN + ", " + IamV4RoleId.JOB_PLAN_MANAGER,
        ResourceTypeId.TAG + ", " + IamV4RoleId.TAG_MANAGER,
        ResourceTypeId.CRON + ", " + IamV4RoleId.CRON_MANAGER,
        ResourceTypeId.FILE_SOURCE + ", " + IamV4RoleId.FILE_SOURCE_MANAGER,
        ResourceTypeId.TICKET + ", " + IamV4RoleId.TICKET_MANAGER,
        ResourceTypeId.ACCOUNT + ", " + IamV4RoleId.ACCOUNT_MANAGER,
        ResourceTypeId.PUBLIC_SCRIPT + ", " + IamV4RoleId.PUBLIC_SCRIPT_MANAGER
    })
    void registerResourceGrantsMatchedRole(String resourceType, String expectedRoleId) {
        boolean result = executor.registerResource(
            buildCreator(), "100", "name-of-100", resourceType, null);

        assertThat(result).isTrue();
        V4AuthorizationDTO authorization = captureAuthorization();
        assertThat(authorization.getRoleId()).isEqualTo(expectedRoleId);
        assertThat(authorization.getRelatedResourceTypeId()).isEqualTo(resourceType);
    }

    @Test
    @DisplayName("授权对象为创建者本人，操作人为系统")
    void authorizeSubjectIsCreatorAndOperatorIsSystem() {
        executor.registerResource(buildCreator(), "s-1", "my-script", ResourceTypeId.SCRIPT, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<V4AuthorizationDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(iamV4AuthService).authorize(eq(TENANT_ID), eq(SystemId.JOB), captor.capture());

        V4AuthorizationDTO authorization = captor.getValue().get(0);
        assertThat(authorization.getSubject().getType()).isEqualTo("user");
        assertThat(authorization.getSubject().getId()).isEqualTo(CREATOR);
    }

    @Test
    @DisplayName("只授权新建的那一个实例")
    void authorizeOnlyTheCreatedInstance() {
        executor.registerResource(buildCreator(), "s-1", "my-script", ResourceTypeId.SCRIPT, null);

        List<V4AuthorizationResourceDTO> resources = captureAuthorization().getResources();
        assertThat(resources).hasSize(1);
        assertThat(resources.get(0).getType()).isEqualTo(IamV4ResourceTypeId.SCRIPT);
        assertThat(resources.get(0).getId()).isEqualTo("s-1");
    }

    @Test
    @DisplayName("过期时间取 v4 上限 365 天")
    void expiredAtIsCappedAt365Days() {
        long before = Instant.now().plus(365, ChronoUnit.DAYS).getEpochSecond();
        executor.registerResource(buildCreator(), "s-1", "my-script", ResourceTypeId.SCRIPT, null);
        long after = Instant.now().plus(365, ChronoUnit.DAYS).getEpochSecond();

        assertThat(captureAuthorization().getExpiredAt()).isBetween(before, after);
    }

    @Test
    @DisplayName("无对应角色的资源类型跳过授权，不抛异常")
    void unknownResourceTypeIsSkipped() {
        boolean result = executor.registerResource(
            buildCreator(), "2", "biz-2", IamV4ResourceTypeId.BIZ, null);

        assertThat(result).isFalse();
        verifyNoInteractions(iamV4AuthService);
    }

    @Test
    @DisplayName("授权抛异常时返回 false，不向上传播")
    void authorizeFailureReturnsFalse() {
        doThrow(new RuntimeException("iam unavailable"))
            .when(iamV4AuthService).authorize(anyString(), anyString(), anyList());

        boolean result = executor.registerResource(
            buildCreator(), "s-1", "my-script", ResourceTypeId.SCRIPT, null);

        assertThat(result).isFalse();
    }

    private User buildCreator() {
        return new User(TENANT_ID, CREATOR, CREATOR);
    }

    private V4AuthorizationDTO captureAuthorization() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<V4AuthorizationDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(iamV4AuthService).authorize(any(), any(), captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        return captor.getValue().get(0);
    }
}
