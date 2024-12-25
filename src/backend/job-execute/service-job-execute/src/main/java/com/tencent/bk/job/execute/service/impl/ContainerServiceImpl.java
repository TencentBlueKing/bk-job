/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.cc.constants.KubeTopoNodeTypeEnum;
import com.tencent.bk.job.common.cc.model.container.ContainerDTO;
import com.tencent.bk.job.common.cc.model.container.ContainerDetailDTO;
import com.tencent.bk.job.common.cc.model.container.KubeClusterDTO;
import com.tencent.bk.job.common.cc.model.container.KubeNamespaceDTO;
import com.tencent.bk.job.common.cc.model.container.KubeNodeID;
import com.tencent.bk.job.common.cc.model.container.KubeWorkloadDTO;
import com.tencent.bk.job.common.cc.model.container.PodDTO;
import com.tencent.bk.job.common.cc.model.filter.BaseRuleDTO;
import com.tencent.bk.job.common.cc.model.filter.ComposeRuleDTO;
import com.tencent.bk.job.common.cc.model.filter.PropertyFilterDTO;
import com.tencent.bk.job.common.cc.model.filter.RuleConditionEnum;
import com.tencent.bk.job.common.cc.model.query.KubeClusterQuery;
import com.tencent.bk.job.common.cc.model.query.NamespaceQuery;
import com.tencent.bk.job.common.cc.model.query.WorkloadQuery;
import com.tencent.bk.job.common.cc.model.req.ListKubeContainerByTopoReq;
import com.tencent.bk.job.common.cc.sdk.BizCmdbClient;
import com.tencent.bk.job.common.constant.LabelSelectorOperatorEnum;
import com.tencent.bk.job.common.model.dto.Container;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.RandomUtil;
import com.tencent.bk.job.execute.model.KubeClusterFilter;
import com.tencent.bk.job.execute.model.KubeContainerFilter;
import com.tencent.bk.job.execute.model.KubeContainerPropFilter;
import com.tencent.bk.job.execute.model.KubeNamespaceFilter;
import com.tencent.bk.job.execute.model.KubePodFilter;
import com.tencent.bk.job.execute.model.KubeWorkloadFilter;
import com.tencent.bk.job.execute.model.LabelSelectExprDTO;
import com.tencent.bk.job.execute.service.ContainerService;
import com.tencent.bk.job.execute.service.HostService;
import com.tencent.bk.job.manage.model.inner.ServiceListAppHostResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ContainerServiceImpl implements ContainerService {
    private final HostService hostService;
    private final BizCmdbClient cmdbClient;
    private final AppScopeMappingService appScopeMappingService;

    @Autowired
    public ContainerServiceImpl(HostService hostService, BizCmdbClient cmdbClient,
                                AppScopeMappingService appScopeMappingService) {
        this.hostService = hostService;
        this.cmdbClient = cmdbClient;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    public List<Container> listContainerByIds(long appId, Collection<Long> ids) {
        ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(appId);
        List<ContainerDetailDTO> containerDetailList =
            cmdbClient.listKubeContainerByIds(Long.parseLong(resourceScope.getId()), ids);
        if (CollectionUtils.isEmpty(containerDetailList)) {
            return Collections.emptyList();
        }
        List<Container> containers = containerDetailList.stream()
            .map(ContainerDetailDTO::toContainer).collect(Collectors.toList());

        fillNodeHostInfo(appId, containers);

        return containers;
    }

    private void fillNodeHostInfo(long appId, List<Container> containers) {
        ServiceListAppHostResultDTO hostResult =
            hostService.batchGetAppHosts(
                appId,
                containers.stream()
                    .map(container -> new HostDTO(container.getNodeHostId()))
                    .collect(Collectors.toList()),
                false);
        Map<Long, HostDTO> hostMap = hostResult.getValidHosts().stream()
            .collect(Collectors.toMap(HostDTO::getHostId, host -> host, (oldValue, newValue) -> newValue));

        containers.forEach(container -> {
            HostDTO nodeHost = hostMap.get(container.getNodeHostId());
            if (nodeHost == null) {
                log.error("Could not found node host for container: {}", container.getId());
                return;
            }
            container.setNodeAgentId(nodeHost.getAgentId());
            container.setNodeIp(nodeHost.getPrimaryIp());
        });
    }

    @Override
    public List<Container> listContainerByContainerFilter(long appId, KubeContainerFilter filter) {
        ListKubeContainerByTopoReq req = new ListKubeContainerByTopoReq();
        long bizId = convertToBizId(appId);
        req.setBizId(bizId);
        if (!filter.isEmptyFilter()) {
            if (filter.hasKubeNodeFilter()) {
                List<KubeNodeID> kubeNodeIDS = computeKubeTopoNode(bizId, filter);
                if (CollectionUtils.isEmpty(kubeNodeIDS)) {
                    // 如果根据条件查询，没有匹配的容器拓扑节点，无需进一步处理；直接返回空的容器列表
                    return Collections.emptyList();
                }
                req.setNodeIdList(kubeNodeIDS);
            }

            if (filter.getPodFilter() != null) {
                KubePodFilter kubePodFilter = filter.getPodFilter();

                PropertyFilterDTO podPropFilter = new PropertyFilterDTO();
                podPropFilter.setCondition(RuleConditionEnum.AND.getCondition());

                if (CollectionUtils.isNotEmpty(kubePodFilter.getPodNames())) {
                    podPropFilter.addRule(BaseRuleDTO.in(PodDTO.Fields.NAME, kubePodFilter.getPodNames()));
                }
                if (CollectionUtils.isNotEmpty(kubePodFilter.getLabelSelector())) {
                    ComposeRuleDTO labelsComposeRule = new ComposeRuleDTO(RuleConditionEnum.AND.getCondition());
                    kubePodFilter.getLabelSelector().forEach(
                        labelSelectExpr -> labelsComposeRule.addRule(buildLabelFilterRule(labelSelectExpr)));

                    podPropFilter.addRule(BaseRuleDTO.filterObject(PodDTO.Fields.LABELS, labelsComposeRule));
                }

                req.setPodFilter(podPropFilter);
            }

            if (filter.getContainerPropFilter() != null) {
                KubeContainerPropFilter containerPropFilter = filter.getContainerPropFilter();

                PropertyFilterDTO containerFilter = new PropertyFilterDTO();
                containerFilter.setCondition(RuleConditionEnum.AND.getCondition());
                if (CollectionUtils.isNotEmpty(containerPropFilter.getContainerNames())) {
                    containerFilter.addRule(BaseRuleDTO.in(ContainerDTO.Fields.NAME,
                        containerPropFilter.getContainerNames()));
                }

                req.setContainerFilter(containerFilter);
            }
        }

        List<ContainerDetailDTO> containerDetailList = cmdbClient.listKubeContainerByTopo(req);
        if (CollectionUtils.isEmpty(containerDetailList)) {
            return Collections.emptyList();
        }

        List<Container> containers = containerDetailList.stream()
            .map(ContainerDetailDTO::toContainer).collect(Collectors.toList());

        fillNodeHostInfo(appId, containers);

        if (filter.isFetchAnyOneContainer()) {
            // 随机选择一个容器
            int index = RandomUtil.nextInt(containers.size());
            return Collections.singletonList(containers.get(index));
        } else {
            return containers;
        }
    }

    private BaseRuleDTO buildLabelFilterRule(LabelSelectExprDTO labelSelectExpr) {
        LabelSelectorOperatorEnum operator = labelSelectExpr.getOperator();
        switch (operator) {
            case EQUALS:
                return BaseRuleDTO.equals(labelSelectExpr.getKey(), labelSelectExpr.getValues().get(0));
            case NOT_EQUALS:
                return BaseRuleDTO.notEquals(labelSelectExpr.getKey(), labelSelectExpr.getValues().get(0));
            case IN:
                return BaseRuleDTO.in(labelSelectExpr.getKey(), labelSelectExpr.getValues());
            case NOT_IN:
                return BaseRuleDTO.notIn(labelSelectExpr.getKey(), labelSelectExpr.getValues());
            case EXISTS:
                return BaseRuleDTO.exists(labelSelectExpr.getKey());
            case NOT_EXISTS:
                return BaseRuleDTO.notExists(labelSelectExpr.getKey());
            case LESS_THAN:
                return BaseRuleDTO.lessThan(labelSelectExpr.getKey(), labelSelectExpr.getValues().get(0));
            case GREATER_THAN:
                return BaseRuleDTO.greaterThan(labelSelectExpr.getKey(), labelSelectExpr.getValues().get(0));
            default:
                throw new IllegalArgumentException("Invalid label select operator: " + operator);
        }
    }

    private Long convertToBizId(long appId) {
        ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(appId);
        if (!resourceScope.isBiz()) {
            throw new IllegalArgumentException("Invalid appId");
        }
        return Long.parseLong(resourceScope.getId());
    }

    private List<KubeNodeID> computeKubeTopoNode(long bizId, KubeContainerFilter filter) {
        List<KubeNodeID> kubeNodes;
        if (filter.getWorkloadFilter() != null) {
            kubeNodes = computeKubeWorkloadTopoNodes(bizId, filter.getClusterFilter(), filter.getNamespaceFilter(),
                filter.getWorkloadFilter());
        } else if (filter.getNamespaceFilter() != null) {
            kubeNodes = computeKubeNamespaceTopoNodes(bizId, filter.getClusterFilter(), filter.getNamespaceFilter());
        } else if (filter.getClusterFilter() != null) {
            kubeNodes = computeKubeClusterTopoNodes(bizId, filter.getClusterFilter());
        } else {
            throw new IllegalStateException("Invalid KubeContainerFilter for compute kube topo node");
        }
        return kubeNodes;
    }

    private List<KubeNodeID> computeKubeWorkloadTopoNodes(long bizId,
                                                          KubeClusterFilter clusterFilter,
                                                          KubeNamespaceFilter namespaceFilter,
                                                          KubeWorkloadFilter workloadFilter) {
        List<KubeClusterDTO> matchClusters = null;
        List<KubeNamespaceDTO> matchNamespaces = null;

        if (clusterFilter != null) {
            matchClusters = cmdbClient.listKubeClusters(
                KubeClusterQuery.Builder.builder(bizId).bkClusterUIDs(clusterFilter.getClusterUIDs()).build());
            if (CollectionUtils.isEmpty(matchClusters)) {
                return Collections.emptyList();
            }
        }

        if (namespaceFilter != null && CollectionUtils.isNotEmpty(namespaceFilter.getNamespaces())) {
            matchNamespaces = cmdbClient.listKubeNamespaces(
                NamespaceQuery.Builder.builder(bizId).names(namespaceFilter.getNamespaces()).build());
            if (CollectionUtils.isEmpty(matchNamespaces)) {
                return Collections.emptyList();
            }
        }

        List<KubeWorkloadDTO> workloads = cmdbClient.listKubeWorkloads(
            WorkloadQuery.Builder
                .builder(bizId, workloadFilter.getKind())
                .bkClusterIds(matchClusters == null ? null :
                    matchClusters.stream().map(KubeClusterDTO::getId).collect(Collectors.toList()))
                .bkNamespaceIds(matchNamespaces == null ? null :
                    matchNamespaces.stream().map(KubeNamespaceDTO::getId).collect(Collectors.toList()))
                .names(workloadFilter.getWorkloadNames())
                .build());

        return workloads == null ? Collections.emptyList() : workloads.stream()
            .map(workload -> new KubeNodeID(workload.getKind(), workload.getId())).collect(Collectors.toList());
    }

    private List<KubeNodeID> computeKubeNamespaceTopoNodes(long bizId,
                                                           KubeClusterFilter clusterFilter,
                                                           KubeNamespaceFilter namespaceFilter) {
        List<KubeClusterDTO> matchClusters = null;

        if (clusterFilter != null) {
            matchClusters = cmdbClient.listKubeClusters(
                KubeClusterQuery.Builder.builder(bizId).bkClusterUIDs(clusterFilter.getClusterUIDs()).build());
            if (CollectionUtils.isEmpty(matchClusters)) {
                return Collections.emptyList();
            }
        }

        List<KubeNamespaceDTO> namespaces = cmdbClient.listKubeNamespaces(
            NamespaceQuery.Builder.builder(bizId)
                .bkClusterIds(matchClusters == null ? null :
                    matchClusters.stream().map(KubeClusterDTO::getId).collect(Collectors.toList()))
                .names(namespaceFilter.getNamespaces())
                .build());


        return namespaces == null ? Collections.emptyList() : namespaces.stream()
            .map(namespace -> new KubeNodeID(KubeTopoNodeTypeEnum.NAMESPACE.getValue(), namespace.getId()))
            .collect(Collectors.toList());
    }

    private List<KubeNodeID> computeKubeClusterTopoNodes(long bizId,
                                                         KubeClusterFilter clusterFilter) {

        List<KubeClusterDTO> clusters = cmdbClient.listKubeClusters(
            KubeClusterQuery.Builder.builder(bizId)
                .bkClusterUIDs(clusterFilter.getClusterUIDs())
                .build());


        return clusters == null ? Collections.emptyList() : clusters.stream()
            .map(cluster -> new KubeNodeID(KubeTopoNodeTypeEnum.CLUSTER.getValue(), cluster.getId()))
            .collect(Collectors.toList());
    }
}
