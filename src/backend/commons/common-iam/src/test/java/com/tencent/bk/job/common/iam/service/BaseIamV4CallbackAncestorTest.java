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

package com.tencent.bk.job.common.iam.service;

import com.tencent.bk.job.common.iam.constant.IamV4ResourceTypeId;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.sdk.iamv4.callback.dto.request.V4CallbackFilterDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.request.V4CallbackParentDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.request.V4CallbackRequestDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.response.V4InstanceInfoDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.response.V4ListInstanceDataDTO;
import com.tencent.bk.sdk.iamv4.service.IamV4AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * list_instance 从 ancestors 解析 bizId / parent 实例 ID；
 * fetch_instance_info 从 root_resource_id 解析 bizId。
 */
class BaseIamV4CallbackAncestorTest {

    private TestCallbackService service;

    @BeforeEach
    void setUp() {
        service = new TestCallbackService(
            mock(AppScopeMappingService.class),
            mock(TenantEnvService.class),
            mock(IamV4AuthService.class));
    }

    @Test
    @DisplayName("module 祖先链：从 type=biz 取出 bizId，不依赖位置")
    void parseBizFromModuleAncestors() {
        V4CallbackFilterDTO filter = filterOf(
            parent(IamV4ResourceTypeId.SET, "104"),
            parent(IamV4ResourceTypeId.BIZ, "20"),
            parent(IamV4ResourceTypeId.SET, "104"));

        assertThat(service.parseAncestorBizId(filter)).isEqualTo(20L);
        assertThat(service.parseParentInstanceId(filter.getParent(), IamV4ResourceTypeId.SET)).isEqualTo(104L);
    }

    @Test
    @DisplayName("namespace 祖先链：biz_container 也映射为 CMDB bizId")
    void parseBizFromNamespaceAncestors() {
        V4CallbackFilterDTO filter = filterOf(
            parent(IamV4ResourceTypeId.CLUSTER, "9"),
            parent(IamV4ResourceTypeId.BIZ_CONTAINER, "20"),
            parent(IamV4ResourceTypeId.CLUSTER, "9"));

        assertThat(service.parseAncestorBizId(filter)).isEqualTo(20L);
        assertThat(service.parseParentInstanceId(filter.getParent(), IamV4ResourceTypeId.CLUSTER)).isEqualTo(9L);
    }

    @Test
    @DisplayName("缺少业务祖先时抛 IllegalArgumentException")
    void missingBizAncestor() {
        V4CallbackFilterDTO filter = filterOf(
            parent(IamV4ResourceTypeId.SET, "104"),
            parent(IamV4ResourceTypeId.SET, "104"));

        assertThatThrownBy(() -> service.parseAncestorBizId(filter))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Biz ancestor");
    }

    @Test
    @DisplayName("parent 类型不符时抛 IllegalArgumentException")
    void unexpectedParentType() {
        V4CallbackParentDTO parent = parent(IamV4ResourceTypeId.CLUSTER, "9");

        assertThatThrownBy(() -> service.parseParentInstanceId(parent, IamV4ResourceTypeId.SET))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unexpected parent type");
    }

    @Test
    @DisplayName("fetch_instance_info 从 root_resource_id 解析 bizId")
    void parseBizFromRootResourceId() {
        V4CallbackFilterDTO filter = new V4CallbackFilterDTO();
        filter.setRootResourceId("20");

        assertThat(service.parseRootResourceBizId(filter)).isEqualTo(20L);
        assertThat(service.buildIamPath(IamV4ResourceTypeId.BIZ, "20", IamV4ResourceTypeId.SET, "104"))
            .isEqualTo("/biz,20/set,104/");
    }

    @Test
    @DisplayName("缺少或非法的 root_resource_id 抛 IllegalArgumentException")
    void missingOrIllegalRootResourceId() {
        assertThatThrownBy(() -> service.parseRootResourceBizId(new V4CallbackFilterDTO()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("root_resource_id");

        V4CallbackFilterDTO illegal = new V4CallbackFilterDTO();
        illegal.setRootResourceId("biz-20");
        assertThatThrownBy(() -> service.parseRootResourceBizId(illegal))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Illegal root_resource_id");
    }

    private static V4CallbackFilterDTO filterOf(V4CallbackParentDTO parent, V4CallbackParentDTO... ancestors) {
        V4CallbackFilterDTO filter = new V4CallbackFilterDTO();
        filter.setParent(parent);
        filter.setAncestors(Arrays.asList(ancestors));
        return filter;
    }

    private static V4CallbackParentDTO parent(String type, String id) {
        V4CallbackParentDTO dto = new V4CallbackParentDTO();
        dto.setType(type);
        dto.setId(id);
        return dto;
    }

    private static class TestCallbackService extends BaseIamV4CallbackService {

        TestCallbackService(AppScopeMappingService appScopeMappingService,
                            TenantEnvService tenantEnvService,
                            IamV4AuthService iamV4AuthService) {
            super(appScopeMappingService, tenantEnvService, iamV4AuthService);
        }

        @Override
        public String getResourceType() {
            return "test";
        }

        @Override
        protected V4ListInstanceDataDTO listInstance(V4CallbackRequestDTO request) {
            return new V4ListInstanceDataDTO();
        }

        @Override
        protected List<V4InstanceInfoDTO> fetchInstanceInfo(V4CallbackRequestDTO request) {
            return Collections.emptyList();
        }

        @Override
        public long parseAncestorBizId(V4CallbackFilterDTO filter) {
            return super.parseAncestorBizId(filter);
        }

        @Override
        public long parseParentInstanceId(V4CallbackParentDTO parent, String expectedType) {
            return super.parseParentInstanceId(parent, expectedType);
        }

        @Override
        public long parseRootResourceBizId(V4CallbackFilterDTO filter) {
            return super.parseRootResourceBizId(filter);
        }

        @Override
        public String buildIamPath(String... typeAndIds) {
            return super.buildIamPath(typeAndIds);
        }
    }
}
