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

import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.Container;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.model.dto.KubeClusterObjectDTO;
import com.tencent.bk.job.common.model.dto.KubeContainerFilter;
import com.tencent.bk.job.common.model.dto.KubeNamespaceObjectDTO;
import com.tencent.bk.job.common.model.dto.KubeTopoDTO;
import com.tencent.bk.job.execute.model.DynamicServerGroupDTO;
import com.tencent.bk.job.execute.model.DynamicServerTopoNodeDTO;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.service.TopoService;
import com.tencent.bk.sdk.iam.constants.AttributeKey;
import com.tencent.bk.sdk.iamv4.dto.V4AuthResourceDTO;
import com.tencent.bk.sdk.iamv4.service.IamV4AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * v4 执行目标的 {@code _bk_iam_path_} 构造。逐段匹配下路径首段与层级必须精确，
 * 否则会造成跨实例视图的越权或漏权（见 openspec design.md D3 / D10）。
 */
@DisplayName("V4ExecuteAuthExecutor：执行目标的 v4 路径构造")
class V4ExecuteAuthExecutorPathTest {

    private V4ExecuteAuthExecutor executor;
    private AppResourceScope bizScope;
    private TopoService topoService;

    @BeforeEach
    void setUp() {
        topoService = mock(TopoService.class);
        IamV4AuthService iamV4AuthService = mock(IamV4AuthService.class);
        executor = new V4ExecuteAuthExecutor(
            iamV4AuthService, new ExecuteTargetInstanceBuilder(topoService));
        bizScope = new AppResourceScope(ResourceScopeTypeEnum.BIZ, "2", 2L);
    }

    private static String pathOf(V4AuthResourceDTO resource) {
        if (resource.getAttributes() == null) {
            return null;
        }
        return (String) resource.getAttributes().get(AttributeKey.BK_IAM_PATH);
    }

    @Test
    @DisplayName("业务下静态主机：路径只含祖先，不含主机自身")
    void bizStaticHostPathContainsAncestorsOnly() {
        ExecuteTargetDTO target = new ExecuteTargetDTO();
        target.setStaticIpList(Collections.singletonList(new HostDTO(100L)));

        List<V4AuthResourceDTO> resources = executor.buildHostAuthResources(bizScope, target);

        assertThat(resources).hasSize(1);
        assertThat(resources.get(0).getId()).isEqualTo("100");
        assertThat(pathOf(resources.get(0))).isEqualTo("/biz,2/");
    }

    @Test
    @DisplayName("业务集视图：叶子即业务集本身，实例 ID 传通配 *")
    void bizSetStaticHostPath() {
        AppResourceScope bizSetScope = new AppResourceScope(ResourceScopeTypeEnum.BIZ_SET, "5", 5L);
        ExecuteTargetDTO target = new ExecuteTargetDTO();
        target.setStaticIpList(Collections.singletonList(new HostDTO(100L)));

        List<V4AuthResourceDTO> resources = executor.buildHostAuthResources(bizSetScope, target);

        assertThat(resources).hasSize(1);
        assertThat(resources.get(0).getId()).isEqualTo("*");
        assertThat(pathOf(resources.get(0))).isEqualTo("/biz_set,5/");
        assertThat(resources.get(0).getCorrelationId()).isEqualTo("/biz_set,5/");
    }

    @Test
    @DisplayName("租户集视图：叶子即租户集本身，实例 ID 传通配 *")
    void tenantSetStaticHostPath() {
        AppResourceScope tenantSetScope = new AppResourceScope(ResourceScopeTypeEnum.TENANT_SET, "1", 1L);
        ExecuteTargetDTO target = new ExecuteTargetDTO();
        target.setStaticIpList(Collections.singletonList(new HostDTO(100L)));

        List<V4AuthResourceDTO> resources = executor.buildHostAuthResources(tenantSetScope, target);

        assertThat(resources).hasSize(1);
        assertThat(resources.get(0).getId()).isEqualTo("*");
        assertThat(pathOf(resources.get(0))).isEqualTo("/tenant_set,1/");
        assertThat(resources.get(0).getCorrelationId()).isEqualTo("/tenant_set,1/");
    }

    @Test
    @DisplayName("动态分组：首段为 biz_dynamic_group，与业务下主机视图区分")
    void dynamicGroupPathUsesDedicatedFirstSegment() {
        ExecuteTargetDTO target = new ExecuteTargetDTO();
        DynamicServerGroupDTO group = new DynamicServerGroupDTO();
        group.setGroupId("dg-1");
        target.setDynamicServerGroups(Collections.singletonList(group));

        List<V4AuthResourceDTO> resources = executor.buildHostAuthResources(bizScope, target);

        assertThat(resources).hasSize(1);
        assertThat(resources.get(0).getId()).isEqualTo("*");
        assertThat(pathOf(resources.get(0)))
            .isEqualTo("/biz_dynamic_group,2/dynamic_group,dg-1/");
        assertThat(resources.get(0).getCorrelationId())
            .isEqualTo("/biz_dynamic_group,2/dynamic_group,dg-1/");
    }

    @Test
    @DisplayName("静态容器：细化到命名空间，各段用 CMDB 原生实例 ID")
    void staticContainerPathReachesNamespace() {
        ExecuteTargetDTO target = new ExecuteTargetDTO();
        Container container = new Container();
        container.setId(1L);
        container.setClusterId(100L);
        container.setNamespaceId(45L);
        target.setStaticContainerList(Collections.singletonList(container));

        List<V4AuthResourceDTO> resources = executor.buildHostAuthResources(bizScope, target);

        assertThat(resources).hasSize(1);
        assertThat(resources.get(0).getId()).isEqualTo("*");
        assertThat(pathOf(resources.get(0)))
            .isEqualTo("/biz_container,2/cluster,100/namespace,45/");
        assertThat(resources.get(0).getCorrelationId())
            .isEqualTo("/biz_container,2/cluster,100/namespace,45/");
    }

    @Test
    @DisplayName("容器过滤器指定了集群与命名空间：同样细化到命名空间")
    void containerFilterWithNamespace() {
        ExecuteTargetDTO target = new ExecuteTargetDTO();
        KubeContainerFilter filter = new KubeContainerFilter();
        filter.setKubeTopoList(Collections.singletonList(new KubeTopoDTO(
            new KubeClusterObjectDTO(100L), new KubeNamespaceObjectDTO(45L), null)));
        target.setContainerFilters(Collections.singletonList(filter));

        List<V4AuthResourceDTO> resources = executor.buildHostAuthResources(bizScope, target);

        assertThat(resources).hasSize(1);
        assertThat(pathOf(resources.get(0)))
            .isEqualTo("/biz_container,2/cluster,100/namespace,45/");
    }

    @Test
    @DisplayName("容器过滤器只指定集群：降级到集群维度")
    void containerFilterWithoutNamespaceFallsBackToCluster() {
        ExecuteTargetDTO target = new ExecuteTargetDTO();
        KubeContainerFilter filter = new KubeContainerFilter();
        filter.setKubeTopoList(Collections.singletonList(new KubeTopoDTO(
            new KubeClusterObjectDTO(100L), null, null)));
        target.setContainerFilters(Collections.singletonList(filter));

        List<V4AuthResourceDTO> resources = executor.buildHostAuthResources(bizScope, target);

        assertThat(resources).hasSize(1);
        assertThat(pathOf(resources.get(0))).isEqualTo("/biz_container,2/cluster,100/");
    }

    @Test
    @DisplayName("容器过滤器未限定拓扑：降级到业务维度")
    void containerFilterWithoutTopoFallsBackToBiz() {
        ExecuteTargetDTO target = new ExecuteTargetDTO();
        target.setContainerFilters(Collections.singletonList(new KubeContainerFilter()));

        List<V4AuthResourceDTO> resources = executor.buildHostAuthResources(bizScope, target);

        assertThat(resources).hasSize(1);
        assertThat(pathOf(resources.get(0))).isEqualTo("/biz_container,2/");
    }

    @Test
    @DisplayName("同一命名空间的多个容器：路径去重，不重复鉴权")
    void duplicatedContainerPathsAreMerged() {
        ExecuteTargetDTO target = new ExecuteTargetDTO();
        Container first = new Container();
        first.setId(1L);
        first.setClusterId(100L);
        first.setNamespaceId(45L);
        Container second = new Container();
        second.setId(2L);
        second.setClusterId(100L);
        second.setNamespaceId(45L);
        target.setStaticContainerList(Arrays.asList(first, second));

        List<V4AuthResourceDTO> resources = executor.buildHostAuthResources(bizScope, target);

        assertThat(resources).hasSize(1);
    }

    @Test
    @DisplayName("拓扑节点视图：路径含 module 叶子段，实例 ID 传通配 *")
    void topoNodePathKeepsLeafAndUsesWildcardId() {
        ExecuteTargetDTO target = new ExecuteTargetDTO();
        target.setTopoNodes(Collections.singletonList(new DynamicServerTopoNodeDTO(300L, "module")));

        // 被选中的 module 节点，parents 为 biz → set
        InstanceTopologyDTO module = new InstanceTopologyDTO();
        module.setObjectId("module");
        module.setInstanceId(300L);
        InstanceTopologyDTO biz = new InstanceTopologyDTO();
        biz.setObjectId("biz");
        biz.setInstanceId(2L);
        InstanceTopologyDTO set = new InstanceTopologyDTO();
        set.setObjectId("set");
        set.setInstanceId(200L);
        module.setParents(Arrays.asList(biz, set));
        when(topoService.batchGetTopoNodeHierarchy(eq(2L), anyList()))
            .thenReturn(Collections.singletonList(module));

        List<V4AuthResourceDTO> resources = executor.buildHostAuthResources(bizScope, target);

        assertThat(resources).hasSize(1);
        assertThat(resources.get(0).getId()).isEqualTo("*");
        // 末段保留 module 叶子，首段是 biz（拓扑视图与业务下主机视图共用 biz 首段）
        assertThat(pathOf(resources.get(0))).isEqualTo("/biz,2/set,200/module,300/");
        assertThat(resources.get(0).getCorrelationId()).isEqualTo("/biz,2/set,200/module,300/");
    }

    @Test
    @DisplayName("混合执行目标：各维度路径分别构造，互不干扰")
    void mixedTargetsBuildSeparatePaths() {
        ExecuteTargetDTO target = new ExecuteTargetDTO();
        target.setStaticIpList(Collections.singletonList(new HostDTO(100L)));
        DynamicServerGroupDTO group = new DynamicServerGroupDTO();
        group.setGroupId("dg-1");
        target.setDynamicServerGroups(Collections.singletonList(group));
        Container container = new Container();
        container.setId(1L);
        container.setClusterId(100L);
        container.setNamespaceId(45L);
        target.setStaticContainerList(Collections.singletonList(container));

        List<V4AuthResourceDTO> resources = executor.buildHostAuthResources(bizScope, target);

        assertThat(resources).hasSize(3);
        assertThat(resources).extracting(V4ExecuteAuthExecutorPathTest::pathOf)
            .containsExactlyInAnyOrder(
                "/biz,2/",
                "/biz_dynamic_group,2/dynamic_group,dg-1/",
                "/biz_container,2/cluster,100/namespace,45/");
    }

    @Test
    @DisplayName("容器鉴权失败的申请资源：叶子是命名空间，祖先到集群")
    void containerApplyResourceUsesNamespaceLeaf() {
        AppResourceScope scope = new AppResourceScope(ResourceScopeTypeEnum.BIZ, "1", 1L);
        ExecuteTargetDTO target = new ExecuteTargetDTO();
        Container container = new Container();
        container.setId(1L);
        container.setClusterId(1L);
        container.setNamespaceId(17L);
        target.setStaticContainerList(Collections.singletonList(container));

        List<PermissionResource> resources = executor.buildContainerPermissionResources(scope, target);

        assertThat(resources).hasSize(1);
        assertThat(resources.get(0).getResourceId()).isEqualTo("17");
        assertThat(resources.get(0).getType()).isEqualTo("namespace");
        assertThat(resources.get(0).getParentHierarchicalResources())
            .extracting(PermissionResource::getType, PermissionResource::getResourceId)
            .containsExactly(
                tuple("biz_container", "1"),
                tuple("cluster", "1"));
    }
}
