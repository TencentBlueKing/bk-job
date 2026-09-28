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
import com.tencent.bk.job.common.iam.constant.IamV4ResourceTypeId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.service.impl.AbstractV4AuthExecutor;
import com.tencent.bk.job.common.iam.util.IamV4ResourceTypeMapper;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.Container;
import com.tencent.bk.job.common.model.dto.KubeContainerFilter;
import com.tencent.bk.job.common.model.dto.KubeTopoDTO;
import com.tencent.bk.job.execute.auth.ExecuteAuthDecision;
import com.tencent.bk.job.execute.auth.ExecuteAuthExecutor;
import com.tencent.bk.job.execute.auth.ExecuteAuthResource;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.sdk.iam.dto.InstanceDTO;
import com.tencent.bk.sdk.iamv4.dto.V4AuthResourceDTO;
import com.tencent.bk.sdk.iamv4.service.IamV4AuthService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 执行类鉴权的 v4 执行器。
 *
 * <p>与 v3 的差别来自 v4 模型本身：v4 每个操作只关联单一资源类型，执行类操作统一绑 {@code host}，
 * 因此本执行器把鉴权拆成两步——</p>
 * <ol>
 *     <li><b>执行目标维度</b>：对全部执行目标主机做批量鉴权，要求<b>全部通过</b>（与 v3 的 AND 语义一致）；</li>
 *     <li><b>资源维度</b>：v4 模型没有 v3 的依赖操作，脚本 / 方案 / 模板的可见性需显式校验伴随的 view
 *     操作（见 {@link ExecuteAuthResource#getCompanionViewActionId()}）。无伴随操作的场景（如公共脚本执行）跳过。</li>
 * </ol>
 *
 * <p><b>路径构造不能复用 v3 的实例路径</b>，有三处语义差异：① 不同实例视图的首段必须区分，动态分组用
 * {@code biz_dynamic_group}、容器用 {@code biz_container}，否则逐段匹配会跨视图误判（见 design.md D3）；
 * ② 默认视图（业务 → 静态主机）的 {@code _bk_iam_path_} <b>不含</b>主机自身，而 v3 的静态主机路径带
 * {@code host,{hostId}} 末段；③ 扩展视图的实例 ID 传通配 {@code *} 且路径<b>含</b>叶子段
 * （见 {@link #buildHostAuthResources} 与 design.md D15）。故本类按执行目标类型分别构造。</p>
 */
@Slf4j
public class V4ExecuteAuthExecutor extends AbstractV4AuthExecutor implements ExecuteAuthExecutor {

    private final ExecuteTargetInstanceBuilder executeTargetInstanceBuilder;

    public V4ExecuteAuthExecutor(IamV4AuthService iamV4AuthService,
                                 ExecuteTargetInstanceBuilder executeTargetInstanceBuilder) {
        super(iamV4AuthService);
        this.executeTargetInstanceBuilder = executeTargetInstanceBuilder;
    }

    @Override
    public ExecuteAuthDecision authExecute(User user,
                                           String actionId,
                                           AppResourceScope appResourceScope,
                                           ExecuteAuthResource resource,
                                           ExecuteTargetDTO executeTarget) {
        if (!isExecuteTargetAllowed(user, actionId, appResourceScope, executeTarget)) {
            return ExecuteAuthDecision.fail(actionId, ExecuteAuthDecision.FailureScope.EXECUTE_TARGET);
        }
        if (resource != null && StringUtils.isNotBlank(resource.getCompanionViewActionId())
            && !isResourceViewAllowed(user, appResourceScope, resource)) {
            return ExecuteAuthDecision.fail(
                resource.getCompanionViewActionId(), ExecuteAuthDecision.FailureScope.RESOURCE);
        }
        return ExecuteAuthDecision.pass();
    }

    /**
     * 执行目标维度鉴权：全部主机资源都通过才算通过。
     */
    private boolean isExecuteTargetAllowed(User user,
                                           String actionId,
                                           AppResourceScope appResourceScope,
                                           ExecuteTargetDTO executeTarget) {
        List<V4AuthResourceDTO> hostResources = buildHostAuthResources(appResourceScope, executeTarget);
        if (CollectionUtils.isEmpty(hostResources)) {
            // 没有执行目标时退化为无资源鉴权，与 v3 传空主机列表的语义一致
            return iamV4AuthService.auth(user.getTenantId(), user.getUsername(), actionId);
        }
        if (hostResources.size() == 1) {
            return iamV4AuthService.auth(
                user.getTenantId(), user.getUsername(), actionId, hostResources.get(0));
        }
        Map<String, Boolean> resultMap = iamV4AuthService.authByResources(
            user.getTenantId(), user.getUsername(), actionId, hostResources);
        if (log.isDebugEnabled()) {
            log.debug("Auth execute target by iam v4, username:{}, actionId:{}, result:{}",
                user.getUsername(), actionId, resultMap);
        }
        if (resultMap == null || resultMap.size() < hostResources.size()) {
            log.warn("Incomplete v4 batch auth result, treat as denied|actionId={}|expect={}|actual={}",
                actionId, hostResources.size(), resultMap == null ? 0 : resultMap.size());
            return false;
        }
        return resultMap.values().stream().allMatch(Boolean.TRUE::equals);
    }

    /**
     * 资源维度鉴权：显式校验伴随的 view 操作，替代 v3 模型里的依赖操作。
     */
    private boolean isResourceViewAllowed(User user,
                                          AppResourceScope appResourceScope,
                                          ExecuteAuthResource resource) {
        V4AuthResourceDTO authResource = buildV4Resource(
            resource.getResourceType().getId(),
            resource.getResourceId(),
            resource.getParentResourceType() == null
                ? ExecuteIamPathBuilder.buildAppScopePath(appResourceScope)
                : ExecuteIamPathBuilder.buildAppScopeResourcePath(
                    appResourceScope, resource.getParentResourceType(), resource.getParentResourceId()));
        boolean allowed = iamV4AuthService.auth(
            user.getTenantId(), user.getUsername(), resource.getCompanionViewActionId(), authResource);
        if (log.isDebugEnabled()) {
            log.debug("Auth companion view by iam v4, username:{}, actionId:{}, resource:{}, allowed:{}",
                user.getUsername(), resource.getCompanionViewActionId(), resource, allowed);
        }
        return allowed;
    }

    /**
     * 按执行目标类型分别构造 v4 主机资源。祖先路径去重，避免同一路径重复鉴权。
     *
     * <p><b>两种形态不能混</b>（见类注释与 openspec design.md D15）：</p>
     * <ul>
     *     <li><b>默认视图（业务 → 静态主机）</b>：叶子就是主机实例本身，故传<b>真实主机 ID</b>，
     *     {@code _bk_iam_path_} 只到父级、<b>不含</b>主机自身，如
     *     {@code id="100"}、{@code "/biz,1/"}；</li>
     *     <li><b>扩展视图（业务集 / 租户集 / 拓扑节点 / 动态分组 / 容器）</b>：叶子是上级节点、
     *     代表"该节点下的全部主机"，具体是哪台主机无从指定，故传<b>通配 {@code *}</b>，
     *     {@code _bk_iam_path_} <b>含</b>该视图的叶子段，如
     *     {@code id="*"}、{@code "/biz_dynamic_group,1/dynamic_group,2/"}。</li>
     * </ul>
     */
    List<V4AuthResourceDTO> buildHostAuthResources(AppResourceScope appResourceScope,
                                                   ExecuteTargetDTO executeTarget) {
        List<V4AuthResourceDTO> resources = new ArrayList<>();
        String scopePath = buildScopePath(appResourceScope);

        if (CollectionUtils.isNotEmpty(executeTarget.getStaticIpList())) {
            if (appResourceScope.getType() == ResourceScopeTypeEnum.BIZ) {
                // 默认视图：叶子即主机，传真实主机 ID，路径只到业务
                executeTarget.getStaticIpList().forEach(host -> resources.add(buildV4ResourceWithPath(
                    ResourceTypeEnum.HOST.getId(),
                    String.valueOf(host.getHostId()),
                    scopePath)));
            } else {
                // 业务集 / 租户集视图：叶子就是业务集 / 租户集本身，只能整体鉴权
                resources.add(buildV4ResourceWithPath(
                    ResourceTypeEnum.HOST.getId(), ANY_INSTANCE_ID, scopePath));
            }
        }

        // 拓扑节点视图：v3 构造的路径已含 biz→set→module 的完整链路，末段 module 即该视图的叶子
        if (CollectionUtils.isNotEmpty(executeTarget.getTopoNodes())) {
            List<InstanceDTO> topoInstances = executeTargetInstanceBuilder.buildAppTopoNodeHostInstances(
                appResourceScope, executeTarget.getTopoNodes());
            for (InstanceDTO topoInstance : topoInstances) {
                if (topoInstance.getPath() == null) {
                    continue;
                }
                resources.add(buildV4Resource(
                    ResourceTypeEnum.HOST.getId(), ANY_INSTANCE_ID, topoInstance.getPath()));
            }
        }

        // 动态分组视图：首段须为 biz_dynamic_group 以区别于业务下主机视图，末段 dynamic_group 即叶子
        if (CollectionUtils.isNotEmpty(executeTarget.getDynamicServerGroups())) {
            executeTarget.getDynamicServerGroups().forEach(serverGroup -> {
                String path = "/" + IamV4ResourceTypeId.BIZ_DYNAMIC_GROUP + "," + appResourceScope.getId() + "/"
                    + IamV4ResourceTypeId.DYNAMIC_GROUP + "," + serverGroup.getGroupId() + "/";
                resources.add(buildV4ResourceWithPath(
                    ResourceTypeEnum.HOST.getId(), ANY_INSTANCE_ID, path));
            });
        }

        // 容器视图：首段须为 biz_container，末段 namespace（无法定位时降级到 cluster / biz_container）即叶子
        for (String containerPath : buildContainerPaths(appResourceScope, executeTarget)) {
            resources.add(buildV4ResourceWithPath(
                ResourceTypeEnum.HOST.getId(), ANY_INSTANCE_ID, containerPath));
        }
        return resources;
    }

    private String buildScopePath(AppResourceScope appResourceScope) {
        return "/" + IamV4ResourceTypeMapper.getV4ResourceTypeIdForScope(appResourceScope)
            + "," + appResourceScope.getId() + "/";
    }

    /**
     * 构造容器执行目标的祖先路径，尽可能细化到命名空间。
     *
     * <p>静态容器自带 clusterId / namespaceId，可直接定位；动态条件过滤器只有在指定了集群 / 命名空间时
     * 才能细化，否则退化到集群或业务维度。路径去重后返回。</p>
     */
    private Set<String> buildContainerPaths(AppResourceScope appResourceScope, ExecuteTargetDTO executeTarget) {
        Set<String> paths = new LinkedHashSet<>();
        long bizId = Long.parseLong(appResourceScope.getId());

        if (CollectionUtils.isNotEmpty(executeTarget.getStaticContainerList())) {
            for (Container container : executeTarget.getStaticContainerList()) {
                paths.add(buildContainerPath(bizId, container.getClusterId(), container.getNamespaceId()));
            }
        }
        if (CollectionUtils.isNotEmpty(executeTarget.getContainerFilters())) {
            for (KubeContainerFilter filter : executeTarget.getContainerFilters()) {
                if (CollectionUtils.isEmpty(filter.getKubeTopoList())) {
                    // 未限定容器拓扑，只能按业务维度鉴权
                    paths.add(buildContainerPath(bizId, null, null));
                    continue;
                }
                for (KubeTopoDTO kubeTopo : filter.getKubeTopoList()) {
                    Long clusterId = kubeTopo.getCluster() == null ? null : kubeTopo.getCluster().getId();
                    Long namespaceId = kubeTopo.getNamespace() == null ? null : kubeTopo.getNamespace().getId();
                    paths.add(buildContainerPath(bizId, clusterId, namespaceId));
                }
            }
        }
        return paths;
    }

    /**
     * 容器视图的祖先路径，按可定位的层级逐级降级：命名空间 → 集群 → 业务。
     * 各段均用 CMDB 原生实例 ID，须与容器回调返回给权限中心的实例 ID 保持一致。
     */
    private String buildContainerPath(long bizId, Long clusterId, Long namespaceId) {
        StringBuilder path = new StringBuilder()
            .append('/').append(IamV4ResourceTypeId.BIZ_CONTAINER).append(',').append(bizId).append('/');
        if (clusterId == null) {
            return path.toString();
        }
        path.append(IamV4ResourceTypeId.CLUSTER).append(',').append(clusterId).append('/');
        if (namespaceId == null) {
            return path.toString();
        }
        path.append(IamV4ResourceTypeId.NAMESPACE).append(',').append(namespaceId).append('/');
        return path.toString();
    }
}
