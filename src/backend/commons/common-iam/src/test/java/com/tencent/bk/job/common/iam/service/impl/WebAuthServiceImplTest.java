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

import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.permission.AuthResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebAuthServiceImplTest {

    private static final User USER = new User("default", "tester", "测试用户");

    private WebAuthServiceImpl webAuthService;

    @BeforeEach
    void setUp() {
        MessageI18nService i18nService = mock(MessageI18nService.class);
        when(i18nService.getI18n(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if ("resource.biz_set.name".equals(key)) {
                return "业务集";
            }
            if ("resource.tenant_set.name".equals(key)) {
                return "租户集";
            }
            if ("resource.host.name".equals(key)) {
                return "主机";
            }
            return key;
        });
        webAuthService = new WebAuthServiceImpl(i18nService, mock(AuthService.class));
    }

    @Test
    void bizSetHost_shouldUseScopeName() {
        AuthResultVO vo = toVo(hostResource("biz_set", "9991001", "蓝鲸业务集"));

        assertThat(resourceName(vo)).isEqualTo("蓝鲸业务集");
    }

    @Test
    void bizSetHostWithoutName_shouldFallBackToCount() {
        AuthResultVO vo = toVo(hostResource("biz_set", "9991001", null));

        assertThat(resourceName(vo)).isEqualTo("1 业务集");
    }

    @Test
    void tenantSetHost_shouldUseScopeName() {
        AuthResultVO vo = toVo(hostResource("tenant_set", "9990001", "租户集A"));

        assertThat(resourceName(vo)).isEqualTo("租户集A");
    }

    @Test
    void staticHost_shouldKeepCountText() {
        AuthResultVO vo = toVo(hostResource("host", "101", "10.0.0.1"));

        assertThat(resourceName(vo)).isEqualTo("1 主机");
    }

    private AuthResultVO toVo(PermissionResource resource) {
        AuthResult authResult = AuthResult.fail(USER);
        authResult.addRequiredPermissions(ActionId.QUICK_EXECUTE_SCRIPT, Collections.singletonList(resource));
        return webAuthService.toAuthResultVO(false, authResult);
    }

    private PermissionResource hostResource(String subResourceType, String resourceId, String resourceName) {
        PermissionResource resource = new PermissionResource();
        resource.setResourceType(ResourceTypeEnum.HOST);
        resource.setResourceId(resourceId);
        resource.setSubResourceType(subResourceType);
        resource.setResourceName(resourceName);
        return resource;
    }

    private String resourceName(AuthResultVO vo) {
        return vo.getRequiredPermissions().get(0).getRelatedResources().get(0).getResourceName();
    }
}
