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

import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.PermissionActionResource;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.sdk.iamv4.dto.V4ApplyResourceDTO;
import com.tencent.bk.sdk.iamv4.dto.V4PermissionDTO;
import com.tencent.bk.sdk.iamv4.service.IamV4AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 无权限申请资源：顶层资源不传 ancestors，避免 IAM 拒绝 ancestors: null。
 */
@DisplayName("v4 申请权限资源 ancestors")
class AbstractV4AuthExecutorApplyResourceTest {

    private static final String TENANT_ID = "system";

    private IamV4AuthService iamV4AuthService;
    private V4ActionAuthExecutor executor;

    @BeforeEach
    void setUp() {
        iamV4AuthService = mock(IamV4AuthService.class);
        executor = new V4ActionAuthExecutor(iamV4AuthService);
    }

    @Test
    @DisplayName("顶层资源（业务）不设置 ancestors")
    void topLevelResourceOmitsAncestors() {
        PermissionResource biz = new PermissionResource(ResourceTypeEnum.BUSINESS, "1", "biz-1");
        PermissionActionResource actionResource = new PermissionActionResource();
        actionResource.setActionId(ActionId.ACCESS_BUSINESS);
        actionResource.addResource(biz);

        executor.getApplyUrl(TENANT_ID, Collections.singletonList(actionResource));

        V4ApplyResourceDTO applyResource = captureFirstApplyResource();
        assertThat(applyResource.getId()).isEqualTo("1");
        assertThat(applyResource.getType()).isEqualTo("biz");
        assertThat(applyResource.getAncestors()).isNull();
        assertThat(JsonUtils.toJson(applyResource)).doesNotContain("ancestors");
    }

    @Test
    @DisplayName("有父级时才传 ancestors")
    void childResourceKeepsAncestors() {
        PermissionResource parent = new PermissionResource(ResourceTypeEnum.BUSINESS, "1", "biz-1");
        PermissionResource script = new PermissionResource(ResourceTypeEnum.SCRIPT, "100", "script-100");
        script.setParentHierarchicalResources(Collections.singletonList(parent));
        PermissionActionResource actionResource = new PermissionActionResource();
        actionResource.setActionId(ActionId.VIEW_SCRIPT);
        actionResource.addResource(script);

        executor.getApplyUrl(TENANT_ID, Collections.singletonList(actionResource));

        V4ApplyResourceDTO applyResource = captureFirstApplyResource();
        assertThat(applyResource.getAncestors()).hasSize(1);
        assertThat(applyResource.getAncestors().get(0).getId()).isEqualTo("1");
        assertThat(applyResource.getAncestors().get(0).getType()).isEqualTo("biz");
        assertThat(JsonUtils.toJson(applyResource)).contains("ancestors");
    }

    @SuppressWarnings("unchecked")
    private V4ApplyResourceDTO captureFirstApplyResource() {
        ArgumentCaptor<List<V4PermissionDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(iamV4AuthService).getApplyUrl(eq(TENANT_ID), captor.capture());
        return captor.getValue().get(0).getResources().get(0);
    }
}
