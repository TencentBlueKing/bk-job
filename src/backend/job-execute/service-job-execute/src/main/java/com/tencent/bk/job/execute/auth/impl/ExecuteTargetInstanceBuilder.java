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
import com.tencent.bk.job.common.constant.CcNodeTypeEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.NotImplementedException;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.execute.model.DynamicServerTopoNodeDTO;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.service.TopoService;
import com.tencent.bk.sdk.iam.constants.SystemId;
import com.tencent.bk.sdk.iam.dto.InstanceDTO;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 把执行目标（静态主机 / 拓扑节点 / 动态分组 / 容器）构造为带拓扑路径的主机实例。
 *
 * <p>从原 {@code ExecuteAuthServiceImpl} 抽出，供 v3 执行器鉴权与路由层组装无权限详情共用，
 * 避免同一套路径构造逻辑出现在两处。</p>
 *
 * <p>产出的是 <b>v3 形态</b>的 {@link InstanceDTO}，其 {@code system} 恒为 {@code bk_cmdb}——v3 模型里
 * 主机是 CMDB 的资源、bk-job 只是跨系统引用，SDK 会按它决定「本系统资源不传 resource」以及是否填充主机
 * 拓扑路径。v4 取消了跨系统引用、资源身份里没有 system 这一维，{@code V4ExecuteAuthExecutor} 只取
 * {@link InstanceDTO#getPath()} 转成 v4 路径，不读该字段。</p>
 */
@Slf4j
public class ExecuteTargetInstanceBuilder {

    private final TopoService topoService;

    public ExecuteTargetInstanceBuilder(TopoService topoService) {
        this.topoService = topoService;
    }

    /**
     * 按执行目标类型分别构造主机实例。同一次执行可能混合多种目标类型，全部合并返回。
     */
    public List<InstanceDTO> buildHostInstances(AppResourceScope appResourceScope,
                                                ExecuteTargetDTO executeObjects) {
        List<InstanceDTO> hostInstanceList = new ArrayList<>();
        // 静态IP
        if (!CollectionUtils.isEmpty(executeObjects.getStaticIpList())) {
            switch (appResourceScope.getType()) {
                case BIZ:
                    hostInstanceList.addAll(buildBizStaticHostInstances(appResourceScope, executeObjects));
                    break;
                case BIZ_SET:
                case TENANT_SET:
                    InstanceDTO hostInstance = new InstanceDTO();
                    hostInstance.setType(ResourceTypeEnum.HOST.getId());
                    hostInstance.setSystem(SystemId.CMDB);
                    hostInstance.setPath(ExecuteIamPathBuilder.buildAppScopePath(appResourceScope));
                    hostInstanceList.add(hostInstance);
                    break;
                default:
                    throw new NotImplementedException(
                        "Unsupported appScopeType:" + appResourceScope.getType().getValue(),
                        ErrorCode.NOT_SUPPORT_FEATURE);
            }
        }
        // 动态topo节点
        List<DynamicServerTopoNodeDTO> topoNodes = executeObjects.getTopoNodes();
        if (!CollectionUtils.isEmpty(topoNodes)) {
            hostInstanceList.addAll(buildAppTopoNodeHostInstances(appResourceScope, topoNodes));
        }
        // 动态分组
        if (!CollectionUtils.isEmpty(executeObjects.getDynamicServerGroups())) {
            executeObjects.getDynamicServerGroups().forEach(serverGroup -> {
                InstanceDTO serverGroupInstance = new InstanceDTO();
                serverGroupInstance.setType(ResourceTypeEnum.HOST.getId());
                serverGroupInstance.setSystem(SystemId.CMDB);

                serverGroupInstance.setPath(
                    ExecuteIamPathBuilder.buildAppScopeResourcePath(appResourceScope,
                        ResourceTypeEnum.DYNAMIC_GROUP, serverGroup.getGroupId())
                );
                hostInstanceList.add(serverGroupInstance);
            });
        }
        // 容器：静态容器与动态条件过滤器（containerFilters）均按业务鉴权，构造一次业务维度实例即可
        if (!CollectionUtils.isEmpty(executeObjects.getStaticContainerList())
            || !CollectionUtils.isEmpty(executeObjects.getContainerFilters())) {
            InstanceDTO hostInstance = new InstanceDTO();
            hostInstance.setType(ResourceTypeEnum.HOST.getId());
            hostInstance.setSystem(SystemId.CMDB);
            hostInstance.setPath(ExecuteIamPathBuilder.buildAppScopePath(appResourceScope));
            hostInstanceList.add(hostInstance);
        }
        return hostInstanceList;
    }

    public List<InstanceDTO> buildAppTopoNodeHostInstances(AppResourceScope appResourceScope,
                                                           List<DynamicServerTopoNodeDTO> topoNodes) {
        long bizId = Long.parseLong(appResourceScope.getId());
        List<InstanceTopologyDTO> topoNodeTopologyList = topoService.batchGetTopoNodeHierarchy(bizId, topoNodes);
        Map<String, InstanceTopologyDTO> topoNodeTopologyMap = new HashMap<>();
        for (InstanceTopologyDTO instanceTopologyDTO : topoNodeTopologyList) {
            topoNodeTopologyMap.put(instanceTopologyDTO.getUniqueKey(), instanceTopologyDTO);
        }
        List<InstanceDTO> topoNodeInstanceList = new ArrayList<>(topoNodes.size());
        for (DynamicServerTopoNodeDTO topoNode : topoNodes) {
            String topoNodeKey = topoNode.getUniqueKey();
            InstanceTopologyDTO nodeTopology = topoNodeTopologyMap.get(topoNodeKey);
            if (nodeTopology == null) {
                log.info("Cannot find topoPath for node {}, ignore", topoNodeKey);
                continue;
            }
            InstanceDTO topoNodeInstance = new InstanceDTO();
            topoNodeInstance.setName(String.valueOf(topoNode.getTopoNodeId()));
            topoNodeInstance.setType(ResourceTypeEnum.HOST.getId());
            // v3 必需：主机属 bk_cmdb，SDK 按此决定是否传 resource 与填充拓扑路径；v4 不管系统，不读该字段
            // 这里上层既有v3也有v4，为了兼容，这里还是传 CMDB
            topoNodeInstance.setSystem(SystemId.CMDB);
            topoNodeInstance.setPath(buildIamPathForTopoNode(nodeTopology));
            topoNodeInstanceList.add(topoNodeInstance);
        }
        return topoNodeInstanceList;
    }

    private List<InstanceDTO> buildBizStaticHostInstances(AppResourceScope appResourceScope,
                                                          ExecuteTargetDTO executeTarget) {
        List<InstanceDTO> hostInstanceList = new ArrayList<>();
        executeTarget.getStaticIpList().forEach(host -> {
            InstanceDTO hostInstance = new InstanceDTO();
            String hostIdStr = host.getHostId().toString();
            hostInstance.setId(hostIdStr);
            hostInstance.setType(ResourceTypeEnum.HOST.getId());
            hostInstance.setSystem(SystemId.CMDB);
            hostInstance.setName(host.getIp());
            hostInstance.setPath(
                ExecuteIamPathBuilder.buildAppScopeResourcePath(
                    appResourceScope, ResourceTypeEnum.HOST, hostIdStr));
            hostInstanceList.add(hostInstance);
        });
        return hostInstanceList;
    }

    private PathInfoDTO buildIamPathForTopoNode(InstanceTopologyDTO nodeTopology) {
        List<InstanceTopologyDTO> parents = nodeTopology.getParents();
        if (parents == null) {
            parents = Collections.emptyList();
        }
        List<InstanceTopologyDTO> pathNodeList = new ArrayList<>(parents);
        pathNodeList.add(nodeTopology);
        // 权限路径不支持自定义节点，过滤掉
        pathNodeList = pathNodeList.stream().filter(parent ->
            CcNodeTypeEnum.BIZ.getType().equals(parent.getObjectId())
                || CcNodeTypeEnum.SET.getType().equals(parent.getObjectId())
                || CcNodeTypeEnum.MODULE.getType().equals(parent.getObjectId())
        ).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(pathNodeList)) {
            return null;
        }
        return buildPathInfoDTO(pathNodeList);
    }

    private PathInfoDTO buildPathInfoDTO(List<InstanceTopologyDTO> pathNodeList) {
        PathInfoDTO rootPathInfo = null;
        PathInfoDTO endPathInfo = null;
        for (InstanceTopologyDTO pathNode : pathNodeList) {
            if (rootPathInfo == null) {
                rootPathInfo = new PathInfoDTO();
                rootPathInfo.setType(pathNode.getObjectId());
                rootPathInfo.setId(pathNode.getInstanceId().toString());
                endPathInfo = rootPathInfo;
            } else {
                PathInfoDTO childPathInfo = new PathInfoDTO();
                childPathInfo.setType(pathNode.getObjectId());
                childPathInfo.setId(pathNode.getInstanceId().toString());
                endPathInfo.setChild(childPathInfo);
                endPathInfo = childPathInfo;
            }
        }
        return rootPathInfo;
    }
}
