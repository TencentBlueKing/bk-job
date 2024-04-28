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

package com.tencent.bk.job.crontab.model.inner;

import com.tencent.bk.job.common.annotation.PersistenceObject;
import com.tencent.bk.job.common.esb.model.job.EsbIpDTO;
import com.tencent.bk.job.common.esb.model.job.v3.EsbServerV3DTO;
import com.tencent.bk.job.common.model.dto.CmdbTopoNodeDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.model.openapi.v3.EsbCmdbTopoNodeDTO;
import com.tencent.bk.job.common.model.openapi.v3.EsbDynamicGroupDTO;
import com.tencent.bk.job.common.model.vo.DynamicGroupIdWithMeta;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.common.model.vo.TargetNodeVO;
import com.tencent.bk.job.common.model.vo.TaskExecuteObjectsInfoVO;
import com.tencent.bk.job.common.model.vo.TaskHostNodeVO;
import com.tencent.bk.job.common.model.vo.TaskTargetVO;
import com.tencent.bk.job.execute.model.inner.ServiceTargetServers;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@PersistenceObject
@ApiModel("目标服务器，四个不可同时为空")
@Data
public class ServerDTO implements Cloneable {
    /**
     * 全局变量名
     * <p>
     * 表示引用全局变量定义的主机列表，忽略其他字段
     */
    @ApiModelProperty("如果目标服务器是通过全局变量-主机列表定义的，variable 表示变量 name")
    private String variable;

    /**
     * 静态服务器 IP 列表
     */
    @ApiModelProperty(value = "服务器ip列表（静态）")
    private List<HostDTO> ips;

    /**
     * 动态分组 ID 列表
     */
    @ApiModelProperty(value = "动态分组ID列表")
    private List<String> dynamicGroupIds;

    /**
     * 拓扑节点列表
     */
    @ApiModelProperty(value = "分布式拓扑节点列表")
    private List<CmdbTopoNodeDTO> topoNodes;

    public static TaskTargetVO toTargetVO(ServerDTO server) {
        if (server == null) {
            return null;
        }
        TaskTargetVO taskTarget = new TaskTargetVO();
        taskTarget.setVariable(server.getVariable());
        TaskExecuteObjectsInfoVO taskExecuteObjectsInfoVO = new TaskExecuteObjectsInfoVO();
        TaskHostNodeVO taskHostNode = new TaskHostNodeVO();
        taskTarget.setExecuteObjectsInfo(taskExecuteObjectsInfoVO);
        taskTarget.setHostNodeInfo(taskHostNode);

        // 聚合通过hostId与IP指定的主机信息
        List<HostInfoVO> hostInfoVOList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(server.getIps())) {
            hostInfoVOList.addAll(server.getIps().stream().map(HostDTO::toHostInfoVO).collect(Collectors.toList()));
        }
        if (!hostInfoVOList.isEmpty()) {
            taskExecuteObjectsInfoVO.setHostList(hostInfoVOList);
            taskHostNode.setHostList(hostInfoVOList);
        }
        if (CollectionUtils.isNotEmpty(server.getDynamicGroupIds())) {
            taskExecuteObjectsInfoVO.setDynamicGroupList(
                server.getDynamicGroupIds().stream().map(DynamicGroupIdWithMeta::new).collect(Collectors.toList()));
            taskHostNode.setDynamicGroupIdList(server.getDynamicGroupIds());
        }
        if (CollectionUtils.isNotEmpty(server.getTopoNodes())) {
            List<TargetNodeVO> nodeList = server.getTopoNodes().stream()
                .map(CmdbTopoNodeDTO::toVO).collect(Collectors.toList());
            taskExecuteObjectsInfoVO.setNodeList(nodeList);
            taskHostNode.setNodeList(nodeList);
        }
        return taskTarget;
    }

    public static ServerDTO fromTargetVO(TaskTargetVO taskTarget) {
        if (taskTarget == null) {
            return null;
        }
        ServerDTO server = new ServerDTO();
        server.setVariable(taskTarget.getVariable());
        if (taskTarget.getExecuteObjectsInfoCompatibly() != null) {
            TaskExecuteObjectsInfoVO taskExecuteObjectsInfoVO = taskTarget.getExecuteObjectsInfoCompatibly();
            if (CollectionUtils.isNotEmpty(taskExecuteObjectsInfoVO.getHostList())) {
                server.setIps(taskExecuteObjectsInfoVO.getHostList().stream()
                    .map(HostDTO::fromHostInfoVO).collect(Collectors.toList()));
            }
            if (CollectionUtils.isNotEmpty(taskExecuteObjectsInfoVO.getDynamicGroupList())) {
                server.setDynamicGroupIds(taskExecuteObjectsInfoVO.getDynamicGroupList().stream()
                    .map(DynamicGroupIdWithMeta::getId).collect(Collectors.toList()));
            }
            if (CollectionUtils.isNotEmpty(taskExecuteObjectsInfoVO.getNodeList())) {
                server.setTopoNodes(taskExecuteObjectsInfoVO.getNodeList().stream()
                    .map(CmdbTopoNodeDTO::fromVO).collect(Collectors.toList()));
            }
        }
        return server;
    }

    public static ServerDTO fromEsbServerV3(EsbServerV3DTO server) {
        if (server == null) {
            return null;
        }
        ServerDTO serverDTO = new ServerDTO();
        if (CollectionUtils.isNotEmpty(server.getTopoNodes())) {
            List<CmdbTopoNodeDTO> topoNodes = new ArrayList<>();
            server.getTopoNodes().forEach(topoNode -> topoNodes.add(new CmdbTopoNodeDTO(topoNode.getId(),
                topoNode.getNodeType())));
            serverDTO.setTopoNodes(topoNodes);
        }
        if (CollectionUtils.isNotEmpty(server.getDynamicGroups())) {
            List<String> dynamicGroupIds = new ArrayList<>();
            server.getDynamicGroups().forEach(group -> dynamicGroupIds.add(group.getId()));
            serverDTO.setDynamicGroupIds(dynamicGroupIds);
        }
        List<HostDTO> hosts = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(server.getIps())) {
            List<HostDTO> staticIpList = new ArrayList<>();
            server.getIps().forEach(ip -> staticIpList.add(new HostDTO(ip.getBkCloudId(), ip.getIp())));
            hosts.addAll(staticIpList);
        }
        if (CollectionUtils.isNotEmpty(server.getHostIds())) {
            List<HostDTO> hostIdHostList = new ArrayList<>();
            server.getHostIds().forEach(hostId -> hostIdHostList.add(HostDTO.fromHostId(hostId)));
            hosts.addAll(hostIdHostList);
        }
        serverDTO.setIps(hosts);
        return serverDTO;
    }

    public static EsbServerV3DTO toEsbServerV3(ServerDTO server) {
        if (server == null) {
            return null;
        }
        EsbServerV3DTO esbServer = new EsbServerV3DTO();
        esbServer.setVariable(server.getVariable());
        if (CollectionUtils.isNotEmpty(server.getIps())) {
            esbServer.setIps(server.getIps().stream().map(EsbIpDTO::fromHost).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(server.getDynamicGroupIds())) {
            esbServer.setDynamicGroups(server.getDynamicGroupIds().stream().map(id -> {
                EsbDynamicGroupDTO esbDynamicGroup = new EsbDynamicGroupDTO();
                esbDynamicGroup.setId(id);
                return esbDynamicGroup;
            }).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(server.getTopoNodes())) {
            esbServer.setTopoNodes(server.getTopoNodes().stream()
                .map(EsbCmdbTopoNodeDTO::fromCmdbTopoNode).collect(Collectors.toList()));
        }
        return esbServer;
    }

    public static ServiceTargetServers toServiceServer(ServerDTO server) {
        if (server == null) {
            return null;
        }
        ServiceTargetServers serviceServer = new ServiceTargetServers();
        serviceServer.setVariable(server.getVariable());
        List<HostDTO> hosts = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(server.getIps())) {
            hosts.addAll(server.getIps());
        }
        serviceServer.setIps(hosts);
        serviceServer.setDynamicGroupIds(server.getDynamicGroupIds());
        if (CollectionUtils.isNotEmpty(server.getTopoNodes())) {
            serviceServer.setTopoNodes(server.getTopoNodes());
        }
        return serviceServer;
    }

    public void standardizeDynamicGroupId() {
        // 移除动态分组ID中多余的appId(历史问题)
        if (CollectionUtils.isNotEmpty(this.dynamicGroupIds)) {
            List<String> standardDynamicGroupIdList = new ArrayList<>();
            this.dynamicGroupIds.forEach(dynamicGroupId -> {
                if (StringUtils.isNotEmpty(dynamicGroupId)) {
                    // appId:groupId
                    String[] appIdAndGroupId = dynamicGroupId.split(":");
                    if (appIdAndGroupId.length == 2) {
                        standardDynamicGroupIdList.add(appIdAndGroupId[1]);
                    } else {
                        standardDynamicGroupIdList.add(dynamicGroupId);
                    }
                }
            });
            this.dynamicGroupIds = standardDynamicGroupIdList;
        }
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ServerDTO clone() {
        ServerDTO serverDTO = new ServerDTO();
        serverDTO.setVariable(variable);
        if (null != ips) {
            List<HostDTO> cloneIps = new ArrayList<>(ips.size());
            for (HostDTO ip : ips) {
                if (ip != null) {
                    cloneIps.add(ip.clone());
                } else {
                    cloneIps.add(null);
                }
            }
            serverDTO.setIps(cloneIps);
        }
        if (null != dynamicGroupIds) {
            List<String> cloneDynamicGroupIds = new ArrayList<>(dynamicGroupIds);
            serverDTO.setDynamicGroupIds(cloneDynamicGroupIds);
        }
        if (null != topoNodes) {
            List<CmdbTopoNodeDTO> cloneTopoNodes = new ArrayList<>(topoNodes.size());
            for (CmdbTopoNodeDTO topoNode : topoNodes) {
                if (topoNode != null) {
                    cloneTopoNodes.add(topoNode.clone());
                } else {
                    cloneTopoNodes.add(null);
                }
            }
            serverDTO.setTopoNodes(cloneTopoNodes);
        }
        return serverDTO;
    }
}
