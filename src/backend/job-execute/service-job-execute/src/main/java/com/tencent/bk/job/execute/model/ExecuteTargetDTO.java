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

package com.tencent.bk.job.execute.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.tencent.bk.job.common.annotation.PersistenceObject;
import com.tencent.bk.job.common.esb.model.job.EsbIpDTO;
import com.tencent.bk.job.common.esb.model.job.v3.EsbServerV3DTO;
import com.tencent.bk.job.common.gse.util.AgentUtils;
import com.tencent.bk.job.common.model.dto.Container;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.model.openapi.v3.EsbCmdbTopoNodeDTO;
import com.tencent.bk.job.common.model.openapi.v3.EsbDynamicGroupDTO;
import com.tencent.bk.job.common.model.openapi.v4.OpenApiExecuteTargetDTO;
import com.tencent.bk.job.common.model.vo.ContainerVO;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.common.model.vo.TaskExecuteObjectsInfoVO;
import com.tencent.bk.job.common.model.vo.TaskHostNodeVO;
import com.tencent.bk.job.common.model.vo.TaskTargetVO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.inner.ServiceExecuteTargetDTO;
import com.tencent.bk.job.execute.util.label.selector.LabelSelectorParse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 任务执行目标 DTO
 */
@Data
@PersistenceObject
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Slf4j
public class ExecuteTargetDTO implements Cloneable {
    /**
     * 如果执行目标是通过全局变量-主机列表定义的，variable 表示变量 name
     */
    private String variable;
    /**
     * 用户选择的主机列表（静态）
     */
    private List<HostDTO> staticIpList;

    /**
     * 用户选择的执行对象列表（静态）
     */
    private List<Container> staticContainerList;

    /**
     * 主机动态分组列表
     */
    private List<DynamicServerGroupDTO> dynamicServerGroups;

    /**
     * 主机拓扑节点
     */
    private List<DynamicServerTopoNodeDTO> topoNodes;

    /**
     * 主机动态分组、静态主机列表、动态主机topo的所有主机的集合。
     *
     * @deprecated 使用 executeObjects 替换
     */
    @Deprecated
    private List<HostDTO> ipList;

    /**
     * 容器过滤器
     */
    private List<KubeContainerFilter> containerFilters;

    /**
     * 执行对象列表(所有主机+容器）
     */
    private List<ExecuteObject> executeObjects;

    public ExecuteTargetDTO clone() {
        ExecuteTargetDTO clone = new ExecuteTargetDTO();
        clone.setVariable(variable);
        if (CollectionUtils.isNotEmpty(staticIpList)) {
            List<HostDTO> cloneStaticIpList = new ArrayList<>(staticIpList.size());
            staticIpList.forEach(staticIp -> cloneStaticIpList.add(staticIp.clone()));
            clone.setStaticIpList(cloneStaticIpList);
        }
        if (CollectionUtils.isNotEmpty(dynamicServerGroups)) {
            List<DynamicServerGroupDTO> cloneServerGroups = new ArrayList<>(dynamicServerGroups.size());
            dynamicServerGroups.forEach(serverGroup -> cloneServerGroups.add(serverGroup.clone()));
            clone.setDynamicServerGroups(cloneServerGroups);
        }
        if (CollectionUtils.isNotEmpty(topoNodes)) {
            clone.setTopoNodes(topoNodes);
        }
        if (CollectionUtils.isNotEmpty(staticContainerList)) {
            List<Container> cloneContainerList = new ArrayList<>(staticContainerList.size());
            staticContainerList.forEach(container -> cloneContainerList.add(container.clone()));
            clone.setStaticContainerList(cloneContainerList);
        }
        if (CollectionUtils.isNotEmpty(ipList)) {
            List<HostDTO> cloneIpList = new ArrayList<>(ipList.size());
            ipList.forEach(ip -> cloneIpList.add(ip.clone()));
            clone.setIpList(cloneIpList);
        }
        if (CollectionUtils.isNotEmpty(containerFilters)) {
            List<KubeContainerFilter> cloneContainerFilters = new ArrayList<>(containerFilters.size());
            containerFilters.forEach(containerFilter -> cloneContainerFilters.add(containerFilter.clone()));
            clone.setContainerFilters(cloneContainerFilters);
        }
        if (CollectionUtils.isNotEmpty(executeObjects)) {
            List<ExecuteObject> cloneExecuteObjectList = new ArrayList<>(executeObjects.size());
            executeObjects.forEach(executeObject -> cloneExecuteObjectList.add(executeObject.clone()));
            clone.setExecuteObjects(cloneExecuteObjectList);
        }
        return clone;
    }

    public ExecuteTargetDTO merge(ExecuteTargetDTO executeObjects) {
        if (executeObjects == null) {
            return this;
        }
        if (executeObjects.getStaticIpList() != null) {
            if (this.staticIpList == null) {
                this.staticIpList = new ArrayList<>(executeObjects.getStaticIpList());
            } else {
                executeObjects.getStaticIpList().forEach(ipDTO -> {
                    if (!this.staticIpList.contains(ipDTO)) {
                        this.staticIpList.add(ipDTO);
                    }
                });
            }
        }
        if (executeObjects.getTopoNodes() != null) {
            if (this.topoNodes == null) {
                this.topoNodes = new ArrayList<>(executeObjects.getTopoNodes());
            } else {
                executeObjects.getTopoNodes().forEach(topoNode -> {
                    if (!this.topoNodes.contains(topoNode)) {
                        this.topoNodes.add(topoNode);
                    }
                });
            }
        }
        if (executeObjects.getDynamicServerGroups() != null) {
            if (this.dynamicServerGroups == null) {
                this.dynamicServerGroups = new ArrayList<>(executeObjects.getDynamicServerGroups());
            } else {
                executeObjects.getDynamicServerGroups().forEach(dynamicServerGroup -> {
                    if (!this.dynamicServerGroups.contains(dynamicServerGroup)) {
                        this.dynamicServerGroups.add(dynamicServerGroup);
                    }
                });
            }
        }
        if (executeObjects.getStaticContainerList() != null) {
            if (this.staticContainerList == null) {
                this.staticContainerList = new ArrayList<>(executeObjects.getStaticContainerList());
            } else {
                executeObjects.getStaticContainerList().forEach(container -> {
                    if (!this.staticContainerList.contains(container)) {
                        this.staticContainerList.add(container);
                    }
                });
            }
        }
        return this;
    }

    public void addStaticHosts(Collection<HostDTO> hosts) {
        if (staticIpList == null) {
            staticIpList = new ArrayList<>();
        }
        staticIpList.addAll(hosts);
    }

    /**
     * 执行对象是否为空
     */
    @JsonIgnore
    public boolean isEmpty() {
        return CollectionUtils.isEmpty(this.staticIpList)
            && CollectionUtils.isEmpty(this.topoNodes)
            && CollectionUtils.isEmpty(this.dynamicServerGroups)
            && CollectionUtils.isEmpty(this.staticContainerList);
    }

    /**
     * 提取所有包含的主机
     *
     * @return 主机列表
     */
    public List<HostDTO> extractHosts() {
        List<HostDTO> hosts = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(staticIpList)) {
            hosts.addAll(staticIpList);
        }
        if (CollectionUtils.isNotEmpty(dynamicServerGroups)) {
            dynamicServerGroups.stream()
                .filter(group -> CollectionUtils.isNotEmpty(group.getIpList()))
                .forEach(group -> hosts.addAll(group.getIpList()));
        }
        if (CollectionUtils.isNotEmpty(topoNodes)) {
            topoNodes.stream()
                .filter(topoNode -> CollectionUtils.isNotEmpty(topoNode.getIpList()))
                .forEach(topoNode -> hosts.addAll(topoNode.getIpList()));
        }
        return hosts.stream().distinct().collect(Collectors.toList());
    }

    public TaskTargetVO convertToTaskTargetVO() {
        TaskTargetVO target = new TaskTargetVO();
        target.setVariable(variable);

        List<ExecuteObject> executeObjects = getExecuteObjectsCompatibly();
        TaskExecuteObjectsInfoVO taskExecuteObjectsInfoVO = new TaskExecuteObjectsInfoVO();
        if (CollectionUtils.isNotEmpty(executeObjects)) {
            // 主机
            List<HostInfoVO> hostInfoVOS = executeObjects.stream()
                .filter(ExecuteObject::isHostExecuteObject)
                .map(ExecuteObject::getHost)
                .map(host -> {
                    HostInfoVO hostInfoVO = host.toHostInfoVO();
                    hostInfoVO.setAgentId(AgentUtils.displayAsRealAgentId(host.getAgentId()));
                    return hostInfoVO;
                })
                .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(hostInfoVOS)) {
                taskExecuteObjectsInfoVO.setHostList(hostInfoVOS);
                TaskHostNodeVO taskHostNodeVO = new TaskHostNodeVO();
                taskHostNodeVO.setHostList(hostInfoVOS);
                // 发布兼容
                target.setHostNodeInfo(taskHostNodeVO);
            }

            // 容器
            List<ContainerVO> containerVOs = executeObjects.stream()
                .filter(ExecuteObject::isContainerExecuteObject)
                .map(ExecuteObject::getContainer)
                .map(Container::toContainerVO)
                .collect(Collectors.toList());
            taskExecuteObjectsInfoVO.setContainerList(containerVOs);
        }
        target.setExecuteObjectsInfo(taskExecuteObjectsInfoVO);

        return target;
    }

    /**
     * 转换为 EsbServerV3DTO
     */
    public EsbServerV3DTO toEsbServerV3DTO() {
        EsbServerV3DTO esbServerV3DTO = new EsbServerV3DTO();
        esbServerV3DTO.setVariable(variable);
        if (!CollectionUtils.isEmpty(staticIpList)) {
            List<EsbIpDTO> ips = staticIpList.stream().map(EsbIpDTO::fromHost).collect(Collectors.toList());
            esbServerV3DTO.setIps(ips);
        }
        if (!CollectionUtils.isEmpty(topoNodes)) {
            List<EsbCmdbTopoNodeDTO> esbTopoNodes = topoNodes.stream().map(topoNode -> {
                EsbCmdbTopoNodeDTO esbCmdbTopoNodeDTO = new EsbCmdbTopoNodeDTO();
                esbCmdbTopoNodeDTO.setId(topoNode.getTopoNodeId());
                esbCmdbTopoNodeDTO.setNodeType(topoNode.getNodeType());
                return esbCmdbTopoNodeDTO;
            }).collect(Collectors.toList());
            esbServerV3DTO.setTopoNodes(esbTopoNodes);
        }
        if (!CollectionUtils.isEmpty(dynamicServerGroups)) {
            List<EsbDynamicGroupDTO> dynamicGroups = dynamicServerGroups.stream().map(dynamicServerGroup -> {
                EsbDynamicGroupDTO esbDynamicGroupDTO = new EsbDynamicGroupDTO();
                esbDynamicGroupDTO.setId(dynamicServerGroup.getGroupId());
                return esbDynamicGroupDTO;
            }).collect(Collectors.toList());
            esbServerV3DTO.setDynamicGroups(dynamicGroups);
        }
        return esbServerV3DTO;
    }

    public ExecuteObject findExecuteObjectByCompositeKey(ExecuteObjectCompositeKey executeObjectCompositeKey) {
        ExecuteObjectCompositeKey.CompositeKeyType compositeKeyType = executeObjectCompositeKey.getCompositeKeyType();
        switch (compositeKeyType) {
            case EXECUTE_OBJECT_ID:
                return getExecuteObjectsCompatibly().stream()
                    .filter(executeObject ->
                        executeObjectCompositeKey.getExecuteObjectId().equals(executeObject.getId()))
                    .findFirst()
                    .orElse(null);
            case RESOURCE_ID:
                return getExecuteObjectsCompatibly().stream()
                    .filter(executeObject ->
                        executeObject.getType() == executeObjectCompositeKey.getExecuteObjectType()
                            && executeObject.getResourceId().equals(executeObjectCompositeKey.getResourceId()))
                    .findFirst()
                    .orElse(null);
            case HOST_CLOUD_IP:
                // 兼容使用 云区域+ip 的方式
                return getExecuteObjectsCompatibly().stream()
                    .filter(executeObject -> executeObject.isHostExecuteObject()
                        && executeObjectCompositeKey.getCloudIp().equals(executeObject.getHost().toCloudIp()))
                    .findFirst()
                    .orElse(null);
            default:
                throw new IllegalArgumentException("InvalidExecuteObjectCompositeKey");
        }
    }

    public List<ExecuteObject> findExecuteObjectByCompositeKeys(
        Collection<ExecuteObjectCompositeKey> executeObjectCompositeKeys) {
        if (CollectionUtils.isEmpty(executeObjectCompositeKeys)) {
            return Collections.emptyList();
        }
        ExecuteObjectCompositeKey anyKey = executeObjectCompositeKeys.stream().findFirst().orElse(null);
        Objects.requireNonNull(anyKey);

        ExecuteObjectCompositeKey.CompositeKeyType compositeKeyType = anyKey.getCompositeKeyType();
        switch (compositeKeyType) {
            case EXECUTE_OBJECT_ID:
                List<String> executeObjectIds =
                    executeObjectCompositeKeys.stream()
                        .map(ExecuteObjectCompositeKey::getExecuteObjectId)
                        .collect(Collectors.toList());
                return getExecuteObjectsCompatibly().stream()
                    .filter(executeObject -> executeObjectIds.contains(executeObject.getId()))
                    .collect(Collectors.toList());
            case RESOURCE_ID:
                return getExecuteObjectsCompatibly().stream()
                    .filter(executeObject ->
                        executeObjectCompositeKeys.contains(
                            executeObject.toExecuteObjectCompositeKey(
                                ExecuteObjectCompositeKey.CompositeKeyType.RESOURCE_ID)))
                    .collect(Collectors.toList());
            case HOST_CLOUD_IP:
                // 兼容使用 管控区域+ip 的方式
                return getExecuteObjectsCompatibly().stream()
                    .filter(executeObject ->
                        executeObject.isHostExecuteObject() &&
                            executeObjectCompositeKeys.contains(
                                executeObject.toExecuteObjectCompositeKey(
                                    ExecuteObjectCompositeKey.CompositeKeyType.HOST_CLOUD_IP)))
                    .collect(Collectors.toList());
            default:
                throw new IllegalArgumentException("InvalidExecuteObjectCompositeKey");
        }
    }

    /**
     * 转换TaskTargetVO 为 ExecuteObjectsDTO
     */
    public static ExecuteTargetDTO fromTaskTargetVO(TaskTargetVO target) {

        ExecuteTargetDTO executeTargetDTO = new ExecuteTargetDTO();
        executeTargetDTO.setVariable(target.getVariable());
        if (target.getExecuteObjectsInfoCompatibly() == null) {
            return executeTargetDTO;
        }

        TaskExecuteObjectsInfoVO taskExecuteObjectsInfoVO = target.getExecuteObjectsInfoCompatibly();

        // 处理主机
        if (CollectionUtils.isNotEmpty(taskExecuteObjectsInfoVO.getHostList())) {
            List<HostDTO> hostList = new ArrayList<>();
            taskExecuteObjectsInfoVO.getHostList().forEach(host -> {
                HostDTO targetHost = new HostDTO();
                if (host.getHostId() != null) {
                    targetHost.setHostId(host.getHostId());
                }
                hostList.add(targetHost);
            });
            executeTargetDTO.setStaticIpList(hostList);
        }
        if (CollectionUtils.isNotEmpty(taskExecuteObjectsInfoVO.getDynamicGroupList())) {
            List<DynamicServerGroupDTO> dynamicServerGroups = new ArrayList<>();
            taskExecuteObjectsInfoVO.getDynamicGroupList().forEach(
                group -> dynamicServerGroups.add(new DynamicServerGroupDTO(group.getId())));
            executeTargetDTO.setDynamicServerGroups(dynamicServerGroups);
        }
        if (CollectionUtils.isNotEmpty(taskExecuteObjectsInfoVO.getNodeList())) {
            List<DynamicServerTopoNodeDTO> topoNodes = new ArrayList<>();
            taskExecuteObjectsInfoVO.getNodeList().forEach(
                topoNode -> topoNodes.add(new DynamicServerTopoNodeDTO(topoNode.getInstanceId(),
                    topoNode.getObjectId())));
            executeTargetDTO.setTopoNodes(topoNodes);
        }

        // 处理容器
        if (CollectionUtils.isNotEmpty(taskExecuteObjectsInfoVO.getContainerList())) {
            List<Container> containerList = new ArrayList<>();
            taskExecuteObjectsInfoVO.getContainerList().forEach(container -> {
                Container targetContainer = new Container();
                if (container.getId() != null) {
                    targetContainer.setId(container.getId());
                }
                containerList.add(targetContainer);
            });
            executeTargetDTO.setStaticContainerList(containerList);
        }

        return executeTargetDTO;
    }

    /**
     * 获取所有执行对象列表(兼容当前版本+历史版本数据）
     */
    @JsonIgnore
    public List<ExecuteObject> getExecuteObjectsCompatibly() {
        if (executeObjects != null) {
            return executeObjects;
        } else if (ipList != null) {
            return ipList.stream().map(ExecuteObject::buildCompatibleExecuteObject).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * 计算所有执行对象数量(兼容当前版本+历史版本数据）
     */
    @JsonIgnore
    public int getExecuteObjectsCountCompatibly() {
        if (executeObjects != null) {
            return executeObjects.size();
        } else if (ipList != null) {
            return ipList.size();
        } else {
            return 0;
        }
    }

    /**
     * 获取所有主机执行对象列表(兼容当前版本+历史版本数据）
     */
    @JsonIgnore
    public List<HostDTO> getHostsCompatibly() {
        if (executeObjects != null) {
            return executeObjects.stream().filter(ExecuteObject::isHostExecuteObject)
                .map(ExecuteObject::getHost).collect(Collectors.toList());
        } else if (ipList != null) {
            return ipList;
        } else {
            return Collections.emptyList();
        }
    }


    /**
     * 合并所有的执行对象
     *
     * @param isSupportExecuteObjectFeature 是否支持执行对象特性
     */
    public void buildMergedExecuteObjects(boolean isSupportExecuteObjectFeature) {
        if (isSupportExecuteObjectFeature) {
            // 支持执行对象，写入 executeObjects 字段
            List<ExecuteObject> executeObjects = new ArrayList<>();
            List<HostDTO> hosts = extractHosts();
            if (CollectionUtils.isNotEmpty(hosts)) {
                executeObjects.addAll(hosts.stream().map(ExecuteObject::new)
                    .collect(Collectors.toList()));
            }
            if (CollectionUtils.isNotEmpty(staticContainerList)) {
                executeObjects.addAll(staticContainerList.stream().map(ExecuteObject::new)
                    .collect(Collectors.toList()));
            }
            if (CollectionUtils.isNotEmpty(containerFilters)) {
                containerFilters.forEach(containerFilter -> {
                    if (CollectionUtils.isNotEmpty(containerFilter.getContainers())) {
                        executeObjects.addAll(containerFilter.getContainers().stream().map(ExecuteObject::new)
                            .collect(Collectors.toList()));
                    }
                });
            }
            this.executeObjects = executeObjects;
        } else {
            // 兼容方式，写入 ipList 字段
            this.ipList = extractHosts();
        }
    }

    public static ExecuteTargetDTO buildFrom(OpenApiExecuteTargetDTO executeTarget) {
        if (executeTarget == null) {
            return null;
        }
        ExecuteTargetDTO executeTargetDTO = new ExecuteTargetDTO();

        // 主机拓扑节点
        if (CollectionUtils.isNotEmpty(executeTarget.getHostTopoNodes())) {
            List<DynamicServerTopoNodeDTO> topoNodes = new ArrayList<>();
            executeTarget.getHostTopoNodes().forEach(
                topoNode -> topoNodes.add(new DynamicServerTopoNodeDTO(topoNode.getId(),
                    topoNode.getNodeType())));
            executeTargetDTO.setTopoNodes(topoNodes);
        }

        // 主机动态分组
        if (CollectionUtils.isNotEmpty(executeTarget.getHostDynamicGroups())) {
            List<DynamicServerGroupDTO> dynamicServerGroups = new ArrayList<>();
            executeTarget.getHostDynamicGroups().forEach(
                group -> dynamicServerGroups.add(new DynamicServerGroupDTO(group.getId())));
            executeTargetDTO.setDynamicServerGroups(dynamicServerGroups);
        }

        // 静态主机列表
        if (CollectionUtils.isNotEmpty(executeTarget.getHosts())) {
            executeTargetDTO.setStaticIpList(
                executeTarget.getHosts().stream()
                    .map(host -> new HostDTO(host.getHostId(), host.getBkCloudId(), host.getIp()))
                    .collect(Collectors.toList()));
        }

        // 容器过滤器
        if (CollectionUtils.isNotEmpty(executeTarget.getKubeContainerFilters())) {
            executeTargetDTO.setContainerFilters(convertToKubeContainerFilter(executeTarget));
        }

        return executeTargetDTO;
    }

    private static List<KubeContainerFilter> convertToKubeContainerFilter(OpenApiExecuteTargetDTO executeTarget) {
        List<KubeContainerFilter> kubeContainerFilters = new ArrayList<>();

        executeTarget.getKubeContainerFilters().forEach(originContainerFilter -> {
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

    /**
     * 执行目标中是否包含容器执行对象
     */
    public boolean hasContainerExecuteObject() {
        return CollectionUtils.isNotEmpty(staticContainerList) || CollectionUtils.isNotEmpty(containerFilters);
    }

    /**
     * 执行目标中是否包含主机执行对象
     */
    public boolean hasHostExecuteObject() {
        return CollectionUtils.isNotEmpty(staticIpList) || CollectionUtils.isNotEmpty(dynamicServerGroups)
            || CollectionUtils.isNotEmpty(topoNodes);
    }

    public ServiceExecuteTargetDTO toServiceExecuteTargetDTO() {
        ServiceExecuteTargetDTO serviceExecuteTargetDTO = new ServiceExecuteTargetDTO();
        serviceExecuteTargetDTO.setVariable(variable);
        if (executeObjects != null) {
            serviceExecuteTargetDTO.setExecuteObjects(
                executeObjects.stream()
                    .map(ExecuteObject::toServiceExecuteObject)
                    .collect(Collectors.toList())
            );
        }
        return serviceExecuteTargetDTO;
    }
}
