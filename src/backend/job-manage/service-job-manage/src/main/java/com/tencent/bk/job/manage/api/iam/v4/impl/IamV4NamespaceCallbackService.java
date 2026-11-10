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

package com.tencent.bk.job.manage.api.iam.v4.impl;

import com.tencent.bk.job.common.cc.model.container.KubeNamespaceDTO;
import com.tencent.bk.job.common.cc.model.container.KubeNodeDTO;
import com.tencent.bk.job.common.cc.model.container.KubeTopologyDTO;
import com.tencent.bk.job.common.cc.model.query.NamespaceQuery;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.constant.KubeTopoNodeTypeEnum;
import com.tencent.bk.job.common.iam.constant.IamV4ResourceTypeId;
import com.tencent.bk.job.common.iam.service.BaseIamV4CallbackService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.manage.service.ContainerService;
import com.tencent.bk.sdk.iamv4.callback.dto.request.V4CallbackFilterDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.request.V4CallbackRequestDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.response.V4BaseInstanceDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.response.V4InstanceInfoDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.response.V4ListInstanceDataDTO;
import com.tencent.bk.sdk.iamv4.service.IamV4AuthService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 命名空间的 IAM v4 回调实现。
 *
 * <p>实例 ID 用 CMDB 原生命名空间 ID。{@code list_instance} 从 {@code filter.ancestors} 取 bizId、
 * 从 parent 取 clusterId，优先走 {@link ContainerService} 的业务容器拓扑本地缓存（Caffeine 5min），
 * 缓存里没有该集群再回源 CMDB。{@code fetch_instance_info} 用 {@code filter.root_resource_id}
 * 作为 bizId，同样优先走拓扑缓存拿名称与所属集群。</p>
 */
@Slf4j
@Component
public class IamV4NamespaceCallbackService extends BaseIamV4CallbackService {

    private final ContainerService containerService;
    private final IBizCmdbClient bizCmdbClient;

    @Autowired
    public IamV4NamespaceCallbackService(ContainerService containerService,
                                              IBizCmdbClient bizCmdbClient,
                                              AppScopeMappingService appScopeMappingService,
                                              TenantEnvService tenantEnvService,
                                              IamV4AuthService iamV4AuthService) {
        super(appScopeMappingService, tenantEnvService, iamV4AuthService);
        this.containerService = containerService;
        this.bizCmdbClient = bizCmdbClient;
    }

    @Override
    public String getResourceType() {
        return IamV4ResourceTypeId.NAMESPACE;
    }

    @Override
    protected V4ListInstanceDataDTO listInstance(V4CallbackRequestDTO request) {
        V4CallbackFilterDTO filter = request.getFilter();
        long bizId = parseAncestorBizId(filter);
        long clusterId = parseParentInstanceId(
            filter == null ? null : filter.getParent(), IamV4ResourceTypeId.CLUSTER);

        List<KubeNamespaceDTO> namespaces = listNamespaces(bizId, clusterId);
        String keyword = filter == null ? null : filter.getKeyword();
        List<KubeNamespaceDTO> matched = new ArrayList<>();
        for (KubeNamespaceDTO ns : namespaces) {
            if (StringUtils.isNotBlank(keyword)
                && (ns.getName() == null || !ns.getName().contains(keyword))) {
                continue;
            }
            matched.add(ns);
        }

        BaseSearchCondition condition = buildSearchCondition(request.getPage());
        int start = Math.min(condition.getStart(), matched.size());
        int end = Math.min(start + condition.getLength(), matched.size());
        List<V4BaseInstanceDTO> results = new ArrayList<>(end - start);
        for (KubeNamespaceDTO ns : matched.subList(start, end)) {
            results.add(new V4BaseInstanceDTO(String.valueOf(ns.getId()), ns.getName()));
        }

        V4ListInstanceDataDTO data = new V4ListInstanceDataDTO();
        data.setCount((long) matched.size());
        data.setResults(results);
        return data;
    }

    @Override
    protected List<V4InstanceInfoDTO> fetchInstanceInfo(V4CallbackRequestDTO request) {
        V4CallbackFilterDTO filter = request.getFilter();
        if (filter == null || CollectionUtils.isEmpty(filter.getIds())) {
            return Collections.emptyList();
        }
        List<Long> nsIds = parseLongIds(filter.getIds());
        if (nsIds.isEmpty()) {
            return Collections.emptyList();
        }
        long bizId = parseRootResourceBizId(filter);
        List<KubeNamespaceDTO> namespaces = fetchNamespaces(bizId, nsIds);

        boolean needPath = isPathRequired(request);
        List<V4InstanceInfoDTO> result = new ArrayList<>(namespaces.size());
        for (KubeNamespaceDTO ns : namespaces) {
            V4InstanceInfoDTO instance = new V4InstanceInfoDTO();
            instance.setId(String.valueOf(ns.getId()));
            instance.setDisplayName(ns.getName());
            if (needPath && ns.getBkClusterId() != null) {
                instance.setBkIamPath(buildIamPath(
                    IamV4ResourceTypeId.BIZ_CONTAINER, String.valueOf(bizId),
                    IamV4ResourceTypeId.CLUSTER, String.valueOf(ns.getBkClusterId())));
            }
            result.add(instance);
        }
        if (result.size() < nsIds.size()) {
            log.warn("Some namespaces not found in IAM v4 callback, may be deleted|bizId={}|requested={}|found={}",
                bizId, nsIds.size(), result.size());
        }
        return result;
    }

    /**
     * 优先从业务容器拓扑本地缓存按 ID 取命名空间（同时得到所属集群），缺的再回源 CMDB。
     */
    private List<KubeNamespaceDTO> fetchNamespaces(long bizId, List<Long> nsIds) {
        Set<Long> wanted = new HashSet<>(nsIds);
        List<KubeNamespaceDTO> found = new ArrayList<>();
        KubeTopologyDTO topo = containerService.getBizKubeCacheTopo(bizId);
        if (topo != null && CollectionUtils.isNotEmpty(topo.getNodes())) {
            found.addAll(namespacesFromTopo(topo, wanted));
            for (KubeNamespaceDTO ns : found) {
                wanted.remove(ns.getId());
            }
        }
        if (!wanted.isEmpty()) {
            List<KubeNamespaceDTO> fromCmdb = bizCmdbClient.listKubeNamespaces(
                NamespaceQuery.Builder.builder(bizId).ids(new ArrayList<>(wanted)).build());
            if (CollectionUtils.isNotEmpty(fromCmdb)) {
                found.addAll(fromCmdb);
            }
        }
        return found;
    }

    private List<KubeNamespaceDTO> namespacesFromTopo(KubeTopologyDTO topo, Set<Long> wanted) {
        List<KubeNamespaceDTO> found = new ArrayList<>();
        collectNamespaces(topo.getNodes(), wanted, found, null);
        return found;
    }

    private void collectNamespaces(List<KubeNodeDTO> nodes,
                                   Set<Long> wanted,
                                   List<KubeNamespaceDTO> found,
                                   Long clusterId) {
        if (CollectionUtils.isEmpty(nodes) || wanted.isEmpty()) {
            return;
        }
        String clusterKind = KubeTopoNodeTypeEnum.CLUSTER.getValue();
        String nsKind = KubeTopoNodeTypeEnum.NAMESPACE.getValue();
        for (KubeNodeDTO node : nodes) {
            Long currentClusterId = clusterId;
            if (clusterKind.equals(node.getKind()) && node.getId() != null) {
                currentClusterId = node.getId();
            }
            if (nsKind.equals(node.getKind()) && node.getId() != null && wanted.contains(node.getId())) {
                KubeNamespaceDTO ns = new KubeNamespaceDTO();
                ns.setId(node.getId());
                ns.setName(node.getName());
                ns.setBkClusterId(currentClusterId);
                found.add(ns);
            }
            collectNamespaces(node.getNodes(), wanted, found, currentClusterId);
        }
    }

    /**
     * 优先从业务容器拓扑本地缓存取该集群下的命名空间，缓存里没有该集群再回源 CMDB。
     */
    private List<KubeNamespaceDTO> listNamespaces(long bizId, long clusterId) {
        KubeTopologyDTO topo = containerService.getBizKubeCacheTopo(bizId);
        if (topo != null && CollectionUtils.isNotEmpty(topo.getNodes())) {
            KubeNodeDTO cluster = findKubeNode(
                topo.getNodes(), KubeTopoNodeTypeEnum.CLUSTER.getValue(), clusterId);
            if (cluster != null) {
                return namespacesFromClusterNode(cluster);
            }
        }
        List<KubeNamespaceDTO> fromCmdb = bizCmdbClient.listKubeNamespaces(
            NamespaceQuery.Builder.builder(bizId)
                .bkClusterIds(Collections.singletonList(clusterId))
                .build());
        return fromCmdb == null ? Collections.emptyList() : fromCmdb;
    }

    private List<KubeNamespaceDTO> namespacesFromClusterNode(KubeNodeDTO cluster) {
        List<KubeNamespaceDTO> namespaces = new ArrayList<>();
        if (CollectionUtils.isEmpty(cluster.getNodes())) {
            return namespaces;
        }
        String namespaceKind = KubeTopoNodeTypeEnum.NAMESPACE.getValue();
        for (KubeNodeDTO child : cluster.getNodes()) {
            if (!namespaceKind.equals(child.getKind()) || child.getId() == null) {
                continue;
            }
            KubeNamespaceDTO ns = new KubeNamespaceDTO();
            ns.setId(child.getId());
            ns.setName(child.getName());
            namespaces.add(ns);
        }
        return namespaces;
    }

    private KubeNodeDTO findKubeNode(List<KubeNodeDTO> nodes, String kind, long id) {
        if (CollectionUtils.isEmpty(nodes)) {
            return null;
        }
        for (KubeNodeDTO node : nodes) {
            if (kind.equals(node.getKind()) && node.getId() != null && node.getId() == id) {
                return node;
            }
            KubeNodeDTO hit = findKubeNode(node.getNodes(), kind, id);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }
}
