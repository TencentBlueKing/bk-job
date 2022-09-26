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

import com.tencent.bk.job.common.annotation.PersistenceObject;
import com.tencent.bk.job.common.model.dto.HostDTO;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

@Data
@PersistenceObject
public class ServersDTO implements Cloneable {
    /**
     * 如果目标服务器是通过全局变量-主机列表定义的，variable 表示变量 name
     */
    private String variable;
    /**
     * 用户选择的服务器ip列表（静态）
     */
    private List<HostDTO> staticIpList;

    /**
     * 服务器动态分组列表
     */
    private List<DynamicServerGroupDTO> dynamicServerGroups;

    /**
     * topo节点
     */
    private List<DynamicServerTopoNodeDTO> topoNodes;

    /**
     * 服务器（动态分组、静态ip、动态topo节点）等对应的所有ip的集合
     */
    private List<HostDTO> ipList;

    /**
     * 非法服务器
     */
    private List<HostDTO> invalidIpList;

    /**
     * 非法动态分组
     */
    private List<DynamicServerGroupDTO> invalidDynamicServerGroups;

    /**
     * 非法topo节点
     */
    private List<DynamicServerTopoNodeDTO> invalidTopoNodes;

    public static ServersDTO emptyInstance() {
        ServersDTO serversDTO = new ServersDTO();
        serversDTO.setIpList(Collections.emptyList());
        serversDTO.setDynamicServerGroups(Collections.emptyList());
        serversDTO.setInvalidDynamicServerGroups(Collections.emptyList());
        serversDTO.setInvalidIpList(Collections.emptyList());
        serversDTO.setInvalidTopoNodes(Collections.emptyList());
        serversDTO.setStaticIpList(Collections.emptyList());
        serversDTO.setTopoNodes(Collections.emptyList());
        return serversDTO;
    }

    public String buildIpListStr() {
        if (ipList == null || ipList.isEmpty()) {
            return null;
        }
        StringJoiner sj = new StringJoiner(",");
        ipList.forEach(ipDTO -> sj.add(ipDTO.getBkCloudId() + ":" + ipDTO.getIp()));
        return sj.toString();
    }

    public ServersDTO clone() {
        ServersDTO cloneServersDTO = new ServersDTO();
        cloneServersDTO.setVariable(variable);
        if (staticIpList != null) {
            List<HostDTO> cloneStaticIpList = new ArrayList<>(staticIpList.size());
            staticIpList.forEach(staticIp -> cloneStaticIpList.add(staticIp.clone()));
            cloneServersDTO.setStaticIpList(cloneStaticIpList);
        }
        if (dynamicServerGroups != null) {
            List<DynamicServerGroupDTO> cloneServerGroups = new ArrayList<>(dynamicServerGroups.size());
            dynamicServerGroups.forEach(serverGroup -> cloneServerGroups.add(serverGroup.clone()));
            cloneServersDTO.setDynamicServerGroups(cloneServerGroups);
        }
        if (topoNodes != null) {
            cloneServersDTO.setTopoNodes(topoNodes);
        }
        if (ipList != null) {
            List<HostDTO> cloneIpList = new ArrayList<>(ipList.size());
            ipList.forEach(ip -> cloneIpList.add(ip.clone()));
            cloneServersDTO.setIpList(cloneIpList);
        }
        if (invalidIpList != null) {
            List<HostDTO> cloneIpList = new ArrayList<>(invalidIpList.size());
            invalidIpList.forEach(ip -> cloneIpList.add(ip.clone()));
            cloneServersDTO.setInvalidIpList(cloneIpList);
        }
        if (invalidDynamicServerGroups != null) {
            List<DynamicServerGroupDTO> cloneInvalidDynamicServerGroups =
                new ArrayList<>(invalidDynamicServerGroups.size());
            invalidDynamicServerGroups.forEach(group -> cloneInvalidDynamicServerGroups.add(group.clone()));
            cloneServersDTO.setInvalidDynamicServerGroups(cloneInvalidDynamicServerGroups);
        }
        if (invalidTopoNodes != null) {
            List<DynamicServerTopoNodeDTO> cloneInvalidDynamicServerGroups = new ArrayList<>(invalidTopoNodes.size());
            invalidTopoNodes.forEach(topo -> cloneInvalidDynamicServerGroups.add(topo.clone()));
            cloneServersDTO.setInvalidTopoNodes(cloneInvalidDynamicServerGroups);
        }
        return cloneServersDTO;
    }

    public ServersDTO merge(ServersDTO servers) {
        if (servers == null) {
            return this;
        }
        if (servers.getStaticIpList() != null) {
            if (this.staticIpList == null) {
                this.staticIpList = new ArrayList<>(servers.getStaticIpList());
            } else {
                servers.getStaticIpList().forEach(ipDTO -> {
                    if (!this.staticIpList.contains(ipDTO)) {
                        this.staticIpList.add(ipDTO);
                    }
                });
            }
        }
        if (servers.getTopoNodes() != null) {
            if (this.topoNodes == null) {
                this.topoNodes = new ArrayList<>(servers.getTopoNodes());
            } else {
                servers.getTopoNodes().forEach(topoNode -> {
                    if (!this.topoNodes.contains(topoNode)) {
                        this.topoNodes.add(topoNode);
                    }
                });
            }
        }
        if (servers.getDynamicServerGroups() != null) {
            if (this.dynamicServerGroups == null) {
                this.dynamicServerGroups = new ArrayList<>(servers.getDynamicServerGroups());
            } else {
                servers.getDynamicServerGroups().forEach(dynamicServerGroup -> {
                    if (!this.dynamicServerGroups.contains(dynamicServerGroup)) {
                        this.dynamicServerGroups.add(dynamicServerGroup);
                    }
                });
            }
        }
        return this;
    }

    public void addStaticIps(Collection<HostDTO> ips) {
        if (staticIpList == null) {
            staticIpList = new ArrayList<>();
        }
        staticIpList.addAll(ips);
    }

    /**
     * 服务器是否为空
     */
    public boolean isEmpty() {
        return CollectionUtils.isEmpty(this.staticIpList)
            && CollectionUtils.isEmpty(this.topoNodes)
            && CollectionUtils.isEmpty(this.dynamicServerGroups);
    }

    public void addInvalidDynamicServerGroup(DynamicServerGroupDTO serverGroup) {
        if (this.invalidDynamicServerGroups == null) {
            this.invalidDynamicServerGroups = new ArrayList<>();
        }
        this.invalidDynamicServerGroups.add(serverGroup);
    }

    public void addInvalidTopoNodeDTO(DynamicServerTopoNodeDTO topoNode) {
        if (this.invalidTopoNodes == null) {
            this.invalidTopoNodes = new ArrayList<>();
        }
        this.invalidTopoNodes.add(topoNode);
    }
}
