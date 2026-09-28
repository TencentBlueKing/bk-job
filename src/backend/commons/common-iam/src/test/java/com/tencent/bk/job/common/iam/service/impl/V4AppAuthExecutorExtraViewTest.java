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

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.IamV4ResourceTypeId;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.sdk.iam.constants.AttributeKey;
import com.tencent.bk.sdk.iamv4.dto.V4AuthResourceDTO;
import com.tencent.bk.sdk.iamv4.service.IamV4AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 访问业务时，业务集 / 租户集是业务的额外实例视图：type 仍为 biz，id 传 *。
 */
@DisplayName("v4 业务范围鉴权：额外实例视图")
class V4AppAuthExecutorExtraViewTest {

    private static final String TENANT_ID = "system";
    private static final String USERNAME = "tester";
    private static final User TEST_USER = new User(TENANT_ID, USERNAME, USERNAME);

    private IamV4AuthService iamV4AuthService;
    private V4AppAuthExecutor executor;

    @BeforeEach
    void setUp() {
        iamV4AuthService = mock(IamV4AuthService.class);
        executor = new V4AppAuthExecutor(iamV4AuthService);
    }

    @Test
    @DisplayName("默认业务视图：id 为具体业务 ID，不带路径")
    void bizUsesConcreteIdWithoutPath() {
        AppResourceScope bizScope = new AppResourceScope(ResourceScopeTypeEnum.BIZ, "2", 2L);
        when(iamV4AuthService.auth(eq(TENANT_ID), eq(USERNAME), eq(ActionId.ACCESS_BUSINESS),
            any(V4AuthResourceDTO.class))).thenReturn(true);

        executor.auth(TEST_USER, ActionId.ACCESS_BUSINESS, bizScope);

        V4AuthResourceDTO resource = captureAuthResource();
        assertThat(resource.getType()).isEqualTo(IamV4ResourceTypeId.BIZ);
        assertThat(resource.getId()).isEqualTo("2");
        assertThat(resource.getAttributes()).isNull();
    }

    @Test
    @DisplayName("业务集是业务的额外实例视图：id 传 *，路径为 /biz_set,{id}/")
    void bizSetUsesWildcardIdAndViewPath() {
        AppResourceScope bizSetScope = new AppResourceScope(ResourceScopeTypeEnum.BIZ_SET, "5", 5L);
        when(iamV4AuthService.auth(eq(TENANT_ID), eq(USERNAME), eq(ActionId.ACCESS_BUSINESS),
            any(V4AuthResourceDTO.class))).thenReturn(true);

        executor.auth(TEST_USER, ActionId.ACCESS_BUSINESS, bizSetScope);

        V4AuthResourceDTO resource = captureAuthResource();
        assertThat(resource.getType()).isEqualTo(IamV4ResourceTypeId.BIZ);
        assertThat(resource.getId()).isEqualTo("*");
        assertThat(pathOf(resource)).isEqualTo("/biz_set,5/");
    }

    @Test
    @DisplayName("租户集是业务的额外实例视图：id 传 *，路径为 /tenant_set,{id}/")
    void tenantSetUsesWildcardIdAndViewPath() {
        AppResourceScope tenantSetScope = new AppResourceScope(ResourceScopeTypeEnum.TENANT_SET, "1", 1L);
        when(iamV4AuthService.auth(eq(TENANT_ID), eq(USERNAME), eq(ActionId.ACCESS_BUSINESS),
            any(V4AuthResourceDTO.class))).thenReturn(true);

        executor.auth(TEST_USER, ActionId.ACCESS_BUSINESS, tenantSetScope);

        V4AuthResourceDTO resource = captureAuthResource();
        assertThat(resource.getType()).isEqualTo(IamV4ResourceTypeId.BIZ);
        assertThat(resource.getId()).isEqualTo("*");
        assertThat(pathOf(resource)).isEqualTo("/tenant_set,1/");
    }

    private V4AuthResourceDTO captureAuthResource() {
        ArgumentCaptor<V4AuthResourceDTO> captor = ArgumentCaptor.forClass(V4AuthResourceDTO.class);
        verify(iamV4AuthService).auth(eq(TENANT_ID), eq(USERNAME), eq(ActionId.ACCESS_BUSINESS), captor.capture());
        return captor.getValue();
    }

    private static String pathOf(V4AuthResourceDTO resource) {
        if (resource.getAttributes() == null) {
            return null;
        }
        return (String) resource.getAttributes().get(AttributeKey.BK_IAM_PATH);
    }
}
