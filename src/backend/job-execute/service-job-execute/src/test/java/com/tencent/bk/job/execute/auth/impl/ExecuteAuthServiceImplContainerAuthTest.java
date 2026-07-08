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

package com.tencent.bk.job.execute.auth.impl;

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.iam.service.ResourceNameQueryService;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.Container;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.model.dto.KubeClusterObjectDTO;
import com.tencent.bk.job.common.model.dto.KubeContainerFilter;
import com.tencent.bk.job.common.model.dto.KubePropCondition;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.service.TopoService;
import com.tencent.bk.sdk.iam.constants.SystemId;
import com.tencent.bk.sdk.iam.dto.InstanceDTO;
import com.tencent.bk.sdk.iam.helper.AuthHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * <ul>
 *   <li>A：仅 containerFilters → 1 个业务维度 InstanceDTO + 1 个 container PermissionResource</li>
 *   <li>B：仅 staticContainerList → 1 个业务维度 InstanceDTO + 1 个 container PermissionResource</li>
 *   <li>C：两者并存 → 仍然只有 1 个，不重复</li>
 *   <li>D：两者都为 null → 0 个</li>
 * </ul>
 * <p>
 * 私有方法通过反射访问；其它依赖（authService / appAuthService / topoService 等）用 Mockito 桩起，
 * 不参与本测试断言路径。
 */
@DisplayName("ExecuteAuthServiceImpl: containerFilters / staticContainerList 容器鉴权（hole 修复）")
class ExecuteAuthServiceImplContainerAuthTest {

    private ExecuteAuthServiceImpl authServiceImpl;
    private AppResourceScope bizScope;
    private Method buildHostInstancesMethod;
    private Method convertHostsToResourceMethod;

    @BeforeEach
    void setUp() throws Exception {
        ResourceNameQueryService resourceNameQueryService = mock(ResourceNameQueryService.class);
        lenient().when(resourceNameQueryService.getResourceName(ResourceTypeEnum.BUSINESS, "2")).thenReturn("biz-2");

        AuthHelper authHelper = mock(AuthHelper.class);
        AuthService authService = mock(AuthService.class);
        AppAuthService appAuthService = mock(AppAuthService.class);
        JobExecuteConfig jobExecuteConfig = mock(JobExecuteConfig.class);
        TopoService topoService = mock(TopoService.class);

        authServiceImpl = new ExecuteAuthServiceImpl(
            authHelper, resourceNameQueryService, authService, appAuthService, jobExecuteConfig, topoService);

        bizScope = new AppResourceScope(ResourceScopeTypeEnum.BIZ, "2", 2L);

        buildHostInstancesMethod = ExecuteAuthServiceImpl.class
            .getDeclaredMethod("buildHostInstances", AppResourceScope.class, ExecuteTargetDTO.class);
        buildHostInstancesMethod.setAccessible(true);
        convertHostsToResourceMethod = ExecuteAuthServiceImpl.class
            .getDeclaredMethod("convertHostsToPermissionResourceList", AppResourceScope.class, ExecuteTargetDTO.class);
        convertHostsToResourceMethod.setAccessible(true);
    }

    @Test
    @DisplayName("A：仅 containerFilters → 业务维度 host instance + container permission resource 各 1 条")
    void onlyContainerFilters() throws Exception {
        ExecuteTargetDTO target = new ExecuteTargetDTO();
        target.setContainerFilters(Collections.singletonList(buildContainerFilter()));

        @SuppressWarnings("unchecked")
        List<InstanceDTO> hostInstances = (List<InstanceDTO>) buildHostInstancesMethod.invoke(
            authServiceImpl, bizScope, target);
        @SuppressWarnings("unchecked")
        List<PermissionResource> resources = (List<PermissionResource>) convertHostsToResourceMethod.invoke(
            authServiceImpl, bizScope, target);

        assertThat(hostInstances).hasSize(1);
        assertThat(hostInstances.get(0).getType()).isEqualTo(ResourceTypeEnum.HOST.getId());
        assertThat(hostInstances.get(0).getSystem()).isEqualTo(SystemId.CMDB);

        assertThat(resources).hasSize(1);
        assertThat(resources.get(0).getSubResourceType()).isEqualTo("container");
        assertThat(resources.get(0).getResourceId()).isEqualTo("2");
    }

    @Test
    @DisplayName("B：仅 staticContainerList → 行为与 A 完全一致")
    void onlyStaticContainerList() throws Exception {
        ExecuteTargetDTO target = new ExecuteTargetDTO();
        target.setStaticContainerList(Collections.singletonList(buildContainer(101L)));

        @SuppressWarnings("unchecked")
        List<InstanceDTO> hostInstances = (List<InstanceDTO>) buildHostInstancesMethod.invoke(
            authServiceImpl, bizScope, target);
        @SuppressWarnings("unchecked")
        List<PermissionResource> resources = (List<PermissionResource>) convertHostsToResourceMethod.invoke(
            authServiceImpl, bizScope, target);

        assertThat(hostInstances).hasSize(1);
        assertThat(resources).hasSize(1);
        assertThat(resources.get(0).getSubResourceType()).isEqualTo("container");
    }

    @Test
    @DisplayName("C：两者并存 → 仍只生成 1 条业务维度实例，不重复鉴权")
    void bothPresent() throws Exception {
        ExecuteTargetDTO target = new ExecuteTargetDTO();
        target.setContainerFilters(Collections.singletonList(buildContainerFilter()));
        target.setStaticContainerList(Collections.singletonList(buildContainer(101L)));

        @SuppressWarnings("unchecked")
        List<InstanceDTO> hostInstances = (List<InstanceDTO>) buildHostInstancesMethod.invoke(
            authServiceImpl, bizScope, target);
        @SuppressWarnings("unchecked")
        List<PermissionResource> resources = (List<PermissionResource>) convertHostsToResourceMethod.invoke(
            authServiceImpl, bizScope, target);

        assertThat(hostInstances).hasSize(1);
        assertThat(resources).hasSize(1);
    }

    @Test
    @DisplayName("D：两者均为空 → 不生成容器维度的 host instance / permission resource")
    void neitherPresent() throws Exception {
        ExecuteTargetDTO target = new ExecuteTargetDTO();
        target.setStaticIpList(Collections.singletonList(new HostDTO(100L)));

        @SuppressWarnings("unchecked")
        List<InstanceDTO> hostInstances = (List<InstanceDTO>) buildHostInstancesMethod.invoke(
            authServiceImpl, bizScope, target);
        @SuppressWarnings("unchecked")
        List<PermissionResource> resources = (List<PermissionResource>) convertHostsToResourceMethod.invoke(
            authServiceImpl, bizScope, target);

        // 主机静态 IP 仍按现状生成 1 条；容器维度无新增
        assertThat(hostInstances).hasSize(1);
        assertThat(resources).extracting(PermissionResource::getSubResourceType).doesNotContain("container");
    }

    private static KubeContainerFilter buildContainerFilter() {
        KubeContainerFilter cf = new KubeContainerFilter();
        cf.setClusterNodes(Collections.singletonList(new KubeClusterObjectDTO(1000L)));
        cf.setPropConditions(Collections.singletonList(
            new KubePropCondition("pod_name", "equal", "pod-a")));
        return cf;
    }

    private static Container buildContainer(long id) {
        Container container = new Container();
        container.setId(id);
        return container;
    }
}
