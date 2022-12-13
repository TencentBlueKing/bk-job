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

package com.tencent.bk.job.execute.service;

import com.tencent.bk.job.common.cc.model.CcInstanceDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.model.DynamicServerGroupDTO;
import com.tencent.bk.job.execute.model.DynamicServerTopoNodeDTO;
import com.tencent.bk.job.manage.model.inner.ServiceHostDTO;
import com.tencent.bk.job.manage.model.inner.ServiceListAppHostResultDTO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 服务器服务
 */
public interface HostService {

    /**
     * 批量获取主机信息
     *
     * @param hostIps 主机Ip列表
     * @return 主机信息
     */
    Map<HostDTO, ServiceHostDTO> batchGetHosts(List<HostDTO> hostIps);

    /**
     * 获取主机信息
     *
     * @param host 主机
     * @return 主机信息
     */
    ServiceHostDTO getHost(HostDTO host);

    /**
     * 通过云区域ID与IPv6地址获取主机信息
     *
     * @param cloudAreaId 云区域ID
     * @param ipv6        IPv6地址
     * @return 主机信息
     */
    ServiceHostDTO getHostByCloudIpv6(long cloudAreaId, String ipv6);

    /**
     * 查询主机在白名单中允许的操作
     * tmp: 发布完成后可以仅仅支持使用hostId查询白名单
     *
     * @param appId 业务ID
     * @param host  主机
     * @return 允许的操作
     */
    List<String> getHostAllowedAction(long appId, HostDTO host);

    /**
     * 获取业务下的主机列表
     *
     * @param appId Job业务ID
     * @param hosts 主机列表
     * @param refreshAgentId 是否重新刷新主机agentId
     * @return 主机信息
     */
    ServiceListAppHostResultDTO batchGetAppHosts(Long appId, Collection<HostDTO> hosts, boolean refreshAgentId);

    /**
     * 获取动态分组主机
     *
     * @param appId   业务ID
     * @param groupId 动态分组ID
     * @return 主机列表
     */
    List<HostDTO> getHostsByDynamicGroupId(long appId, String groupId);

    /**
     * 批量获取动态分组主机，并根据动态分组ID对主机进行分组
     *
     * @param appId    业务ID
     * @param groupIds 动态分组ID列表
     * @return Map, key: 动态分组 value: 动态分组下的主机列表
     */
    Map<DynamicServerGroupDTO, List<HostDTO>> batchGetAndGroupHostsByDynamicGroup(
        long appId,
        Collection<DynamicServerGroupDTO> groupIds);


    /**
     * 根据topo节点获取主机
     *
     * @param appId       Job业务ID
     * @param ccInstances topo节点列表
     * @return 主机列表
     */
    List<HostDTO> getHostsByTopoNodes(long appId, List<CcInstanceDTO> ccInstances);

    /**
     * 根据topo节点获取主机并分组
     *
     * @param appId     Job业务ID
     * @param topoNodes topo节点列表
     * @return Map, key: topo 节点, value: 主机列表
     */
    Map<DynamicServerTopoNodeDTO, List<HostDTO>> getAndGroupHostsByTopoNodes(
        long appId,
        Collection<DynamicServerTopoNodeDTO> topoNodes
    );


    /**
     * 获取主机云区域名称
     *
     * @param cloudAreaId 云区域ID
     * @return 云区域名称
     */
    String getCloudAreaName(long cloudAreaId);
}
