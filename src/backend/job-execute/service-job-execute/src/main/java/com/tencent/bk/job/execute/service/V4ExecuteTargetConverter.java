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

package com.tencent.bk.job.execute.service;

import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.model.openapi.v4.OpenApiKubeContainerFilterDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.model.DynamicServerGroupDTO;
import com.tencent.bk.job.execute.model.DynamicServerTopoNodeDTO;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.KubeClusterFilter;
import com.tencent.bk.job.execute.model.KubeContainerFilter;
import com.tencent.bk.job.execute.model.KubeContainerPropFilter;
import com.tencent.bk.job.execute.model.KubeNamespaceFilter;
import com.tencent.bk.job.execute.model.KubePodFilter;
import com.tencent.bk.job.execute.model.KubeWorkloadFilter;
import com.tencent.bk.job.execute.model.LabelSelectExprDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.OpenApiV4HostDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteTargetDTO;
import com.tencent.bk.job.execute.util.label.selector.LabelSelectorParse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class V4ExecuteTargetConverter {

    public static ExecuteTargetDTO v4ToExecuteTargetDTO(V4ExecuteTargetDTO v4ExecuteTargetDTO) {
        if (v4ExecuteTargetDTO == null || v4ExecuteTargetDTO.isTargetEmpty()) {
            return null;
        }

        ExecuteTargetDTO target = new ExecuteTargetDTO();

        // 主机列表
        if (CollectionUtils.isNotEmpty(v4ExecuteTargetDTO.getHostList())) {
            List<HostDTO> staticHostList = new ArrayList<>();
            for (OpenApiV4HostDTO host : v4ExecuteTargetDTO.getHostList()) {
                // 优先使用hostId
                if (host.getBkHostId() != null) {
                    staticHostList.add(HostDTO.fromHostId(host.getBkHostId()));
                } else {
                    staticHostList.add(new HostDTO(host.getBkCloudId(), host.getIp()));
                }
            }
            target.setStaticIpList(staticHostList);
        }

        // 动态分组
        if (CollectionUtils.isNotEmpty(v4ExecuteTargetDTO.getDynamicGroups())) {
            List<DynamicServerGroupDTO> dynamicServerGroups = new ArrayList<>();
            v4ExecuteTargetDTO.getDynamicGroups().forEach(
                group -> dynamicServerGroups.add(new DynamicServerGroupDTO(group.getId())));
            target.setDynamicServerGroups(dynamicServerGroups);
        }

        // 静态拓扑节点
        if (CollectionUtils.isNotEmpty(v4ExecuteTargetDTO.getTopoNodes())) {
            List<DynamicServerTopoNodeDTO> topoNodes = new ArrayList<>();
            v4ExecuteTargetDTO.getTopoNodes().forEach(topoNode ->
                topoNodes.add(new DynamicServerTopoNodeDTO(topoNode.getId(), topoNode.getNodeType()))
            );
            target.setTopoNodes(topoNodes);
        }

        // 容器过滤器
        if (CollectionUtils.isNotEmpty(v4ExecuteTargetDTO.getKubeContainerFilters())) {
            target.setContainerFilters(convertToKubeContainerFilter(v4ExecuteTargetDTO.getKubeContainerFilters()));
        }

        return target;
    }

    private static List<KubeContainerFilter> convertToKubeContainerFilter(
        List<OpenApiKubeContainerFilterDTO> originContainerFilters) {
        List<KubeContainerFilter> kubeContainerFilters = new ArrayList<>();

        originContainerFilters.forEach(originContainerFilter -> {
            KubeContainerFilter containerFilter = new KubeContainerFilter();
            if (originContainerFilter.getClusterFilter() != null) {
                KubeClusterFilter clusterFilter = new KubeClusterFilter();
                clusterFilter.setClusterUIDs(originContainerFilter.getClusterFilter().getClusterUIDs());
                containerFilter.setClusterFilter(clusterFilter);
            }
            if (originContainerFilter.getNamespaceFilter() != null) {
                KubeNamespaceFilter namespaceFilter = new KubeNamespaceFilter();
                namespaceFilter.setNamespaces(originContainerFilter.getNamespaceFilter().getNamespaces());
                containerFilter.setNamespaceFilter(namespaceFilter);
            }
            if (originContainerFilter.getWorkloadFilter() != null) {
                KubeWorkloadFilter workloadFilter = new KubeWorkloadFilter();
                workloadFilter.setKind(originContainerFilter.getWorkloadFilter().getKind());
                workloadFilter.setWorkloadNames(originContainerFilter.getWorkloadFilter().getWorkloadNames());
                containerFilter.setWorkloadFilter(workloadFilter);
            }
            if (originContainerFilter.getPodFilter() != null) {
                KubePodFilter podFilter = new KubePodFilter();
                podFilter.setPodNames(originContainerFilter.getPodFilter().getPodNames());
                if (CollectionUtils.isNotEmpty(originContainerFilter.getPodFilter().getLabelSelector())) {
                    // 优先解析自定义的 Label Selector
                    podFilter.setLabelSelector(
                        originContainerFilter.getPodFilter().getLabelSelector()
                            .stream()
                            .map(labelSelectExpr -> new LabelSelectExprDTO(
                                labelSelectExpr.getKey(),
                                labelSelectExpr.getOperator(),
                                labelSelectExpr.getValues()))
                            .collect(Collectors.toList()));

                } else if (StringUtils.isNotBlank(originContainerFilter.getPodFilter().getLabelSelectorExpr())) {
                    // 解析 label selector 表达式
                    List<LabelSelectExprDTO> labelSelectExprList = LabelSelectorParse.parseToLabelSelectExprList(
                        originContainerFilter.getPodFilter().getLabelSelectorExpr());
                    log.info("Parse kubernetes label selector expr, expr: {}, result: {}",
                        originContainerFilter.getPodFilter().getLabelSelectorExpr(),
                        JsonUtils.toJson(labelSelectExprList));
                    podFilter.setLabelSelector(labelSelectExprList);
                }
                containerFilter.setPodFilter(podFilter);
            }
            if (originContainerFilter.getContainerPropFilter() != null) {
                KubeContainerPropFilter containerPropFilter = new KubeContainerPropFilter();
                containerPropFilter.setContainerNames(
                    originContainerFilter.getContainerPropFilter().getContainerNames());
                containerFilter.setContainerPropFilter(containerPropFilter);
            }
            containerFilter.setEmptyFilter(originContainerFilter.isEmptyFilter());
            containerFilter.setFetchAnyOneContainer(originContainerFilter.isFetchAnyOneContainer());

            kubeContainerFilters.add(containerFilter);
        });

        return kubeContainerFilters;
    }
}
