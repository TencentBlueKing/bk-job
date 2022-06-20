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

import com.tencent.bk.job.common.esb.model.job.EsbCmdbTopoNodeDTO;
import com.tencent.bk.job.common.esb.model.job.EsbIpDTO;
import com.tencent.bk.job.common.esb.model.job.v3.EsbDynamicGroupDTO;
import com.tencent.bk.job.common.esb.model.job.v3.EsbServerV3DTO;
import com.tencent.bk.job.common.model.dto.CmdbTopoNodeDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
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

@ApiModel("目标服务器，四个不可同时为空")
@Data
public class ServerDTO {
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

    @ApiModelProperty(value = "服务器hostId列表（静态）")
    private List<HostDTO> hosts;

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
        TaskHostNodeVO taskHostNode = new TaskHostNodeVO();
        // 聚合通过hostId与IP指定的主机信息
        List<HostInfoVO> hostInfoVOList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(server.getHosts())) {
            hostInfoVOList.addAll(server.getHosts().parallelStream().map(HostDTO::toVO).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(server.getIps())) {
            hostInfoVOList.addAll(server.getIps().parallelStream().map(HostDTO::toVO).collect(Collectors.toList()));
        }
        if (!hostInfoVOList.isEmpty()) {
            taskHostNode.setIpList(hostInfoVOList);
        }
        if (CollectionUtils.isNotEmpty(server.getDynamicGroupIds())) {
            taskHostNode.setDynamicGroupList(server.getDynamicGroupIds());
        }
        if (CollectionUtils.isNotEmpty(server.getTopoNodes())) {
            taskHostNode.setTopoNodeList(server.getTopoNodes().parallelStream()
                .map(CmdbTopoNodeDTO::toVO).collect(Collectors.toList()));
        }
        taskTarget.setHostNodeInfo(taskHostNode);
        return taskTarget;
    }

    public static ServerDTO fromTargetVO(TaskTargetVO taskTarget) {
        if (taskTarget == null) {
            return null;
        }
        ServerDTO server = new ServerDTO();
        server.setVariable(taskTarget.getVariable());
        if (taskTarget.getHostNodeInfo() != null) {
            TaskHostNodeVO hostNodeInfo = taskTarget.getHostNodeInfo();
            if (CollectionUtils.isNotEmpty(hostNodeInfo.getIpList())) {
                server.setHosts(hostNodeInfo.getIpList().parallelStream()
                    .map(HostDTO::fromVO).collect(Collectors.toList()));
            }
            if (CollectionUtils.isNotEmpty(hostNodeInfo.getDynamicGroupList())) {
                server.setDynamicGroupIds(hostNodeInfo.getDynamicGroupList());
            }
            if (CollectionUtils.isNotEmpty(hostNodeInfo.getTopoNodeList())) {
                server.setTopoNodes(hostNodeInfo.getTopoNodeList().parallelStream()
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
        if (CollectionUtils.isNotEmpty(server.getIps())) {
            List<HostDTO> staticIpList = new ArrayList<>();
            server.getIps().forEach(ip -> staticIpList.add(new HostDTO(ip.getBkCloudId(), ip.getIp())));
            serverDTO.setIps(staticIpList);
        }
        if (CollectionUtils.isNotEmpty(server.getHostIds())) {
            List<HostDTO> hostIdHostList = new ArrayList<>();
            server.getHostIds().forEach(hostId -> hostIdHostList.add(HostDTO.fromHostId(hostId)));
            serverDTO.setHosts(hostIdHostList);
        }
        return serverDTO;
    }

    public static EsbServerV3DTO toEsbServerV3(ServerDTO server) {
        if (server == null) {
            return null;
        }
        EsbServerV3DTO esbServer = new EsbServerV3DTO();
        esbServer.setVariable(server.getVariable());
        if (CollectionUtils.isNotEmpty(server.getHosts())) {
            esbServer.setHostIds(server.getHosts().parallelStream()
                .map(HostDTO::getHostId)
                .collect(Collectors.toList())
            );
        }
        if (CollectionUtils.isNotEmpty(server.getIps())) {
            esbServer.setIps(server.getIps().parallelStream().map(EsbIpDTO::fromHost).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(server.getDynamicGroupIds())) {
            esbServer.setDynamicGroups(server.getDynamicGroupIds().parallelStream().map(id -> {
                EsbDynamicGroupDTO esbDynamicGroup = new EsbDynamicGroupDTO();
                esbDynamicGroup.setId(id);
                return esbDynamicGroup;
            }).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(server.getTopoNodes())) {
            esbServer.setTopoNodes(server.getTopoNodes().parallelStream()
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
        if (CollectionUtils.isNotEmpty(server.getHosts())) {
            hosts.addAll(server.getHosts());
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
}
