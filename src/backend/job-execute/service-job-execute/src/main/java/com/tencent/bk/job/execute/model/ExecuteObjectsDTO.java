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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tencent.bk.job.common.annotation.PersistenceObject;
import com.tencent.bk.job.common.esb.model.job.EsbCmdbTopoNodeDTO;
import com.tencent.bk.job.common.esb.model.job.EsbIpDTO;
import com.tencent.bk.job.common.esb.model.job.v3.EsbDynamicGroupDTO;
import com.tencent.bk.job.common.esb.model.job.v3.EsbServerV3DTO;
import com.tencent.bk.job.common.gse.util.AgentUtils;
import com.tencent.bk.job.common.model.dto.Container;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.common.model.vo.TaskHostNodeVO;
import com.tencent.bk.job.common.model.vo.TaskTargetVO;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 执行对象集合 DTO
 */
@Data
@PersistenceObject
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ExecuteObjectsDTO implements Cloneable {
    /**
     * 如果目标服务器是通过全局变量-主机列表定义的，variable 表示变量 name
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
     * 执行对象列表(所有主机+容器）
     */
    private List<ExecuteObject> executeObjects;

    public static ExecuteObjectsDTO emptyInstance() {
        ExecuteObjectsDTO executeObjectsDTO = new ExecuteObjectsDTO();
        executeObjectsDTO.setIpList(Collections.emptyList());
        executeObjectsDTO.setDynamicServerGroups(Collections.emptyList());
        executeObjectsDTO.setStaticIpList(Collections.emptyList());
        executeObjectsDTO.setTopoNodes(Collections.emptyList());
        return executeObjectsDTO;
    }

    public ExecuteObjectsDTO clone() {
        ExecuteObjectsDTO cloneExecuteObjectsDTO = new ExecuteObjectsDTO();
        cloneExecuteObjectsDTO.setVariable(variable);
        if (CollectionUtils.isNotEmpty(staticIpList)) {
            List<HostDTO> cloneStaticIpList = new ArrayList<>(staticIpList.size());
            staticIpList.forEach(staticIp -> cloneStaticIpList.add(staticIp.clone()));
            cloneExecuteObjectsDTO.setStaticIpList(cloneStaticIpList);
        }
        if (CollectionUtils.isNotEmpty(dynamicServerGroups)) {
            List<DynamicServerGroupDTO> cloneServerGroups = new ArrayList<>(dynamicServerGroups.size());
            dynamicServerGroups.forEach(serverGroup -> cloneServerGroups.add(serverGroup.clone()));
            cloneExecuteObjectsDTO.setDynamicServerGroups(cloneServerGroups);
        }
        if (CollectionUtils.isNotEmpty(topoNodes)) {
            cloneExecuteObjectsDTO.setTopoNodes(topoNodes);
        }
        if (CollectionUtils.isNotEmpty(staticContainerList)) {
            List<Container> cloneContainerList = new ArrayList<>(staticContainerList.size());
            staticContainerList.forEach(container -> cloneContainerList.add(container.clone()));
            cloneExecuteObjectsDTO.setStaticContainerList(cloneContainerList);
        }
        if (CollectionUtils.isNotEmpty(ipList)) {
            List<HostDTO> cloneIpList = new ArrayList<>(ipList.size());
            ipList.forEach(ip -> cloneIpList.add(ip.clone()));
            cloneExecuteObjectsDTO.setIpList(cloneIpList);
        }
        if (CollectionUtils.isNotEmpty(executeObjects)) {
            List<ExecuteObject> cloneExecuteObjectList = new ArrayList<>(executeObjects.size());
            executeObjects.forEach(executeObject -> cloneExecuteObjectList.add(executeObject.clone()));
            cloneExecuteObjectsDTO.setExecuteObjects(cloneExecuteObjectList);
        }
        return cloneExecuteObjectsDTO;
    }

    public ExecuteObjectsDTO merge(ExecuteObjectsDTO executeObjects) {
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
    public boolean isEmpty() {
        return CollectionUtils.isEmpty(this.staticIpList)
            && CollectionUtils.isEmpty(this.topoNodes)
            && CollectionUtils.isEmpty(this.dynamicServerGroups);
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
        TaskTargetVO targetServer = new TaskTargetVO();
        targetServer.setVariable(variable);
        TaskHostNodeVO taskHostNodeVO = new TaskHostNodeVO();
        if (CollectionUtils.isNotEmpty(ipList)) {
            List<HostInfoVO> hostVOs = new ArrayList<>();
            ipList.forEach(host -> {
                HostInfoVO hostInfoVO = host.toHostInfoVO();
                hostInfoVO.setAgentId(AgentUtils.displayAsRealAgentId(host.getAgentId()));
                hostVOs.add(hostInfoVO);
            });
            taskHostNodeVO.setHostList(hostVOs);
            targetServer.setHostNodeInfo(taskHostNodeVO);
        }
        return targetServer;
    }

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
}
