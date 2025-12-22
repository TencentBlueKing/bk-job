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

package com.tencent.bk.job.manage.service.host;

import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.manage.model.inner.ServiceListAppHostResultDTO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 主机、topo相关服务
 */
public interface TenantHostService {

    /**
     * 获取业务下的主机。如果在Job缓存的主机中不存在，那么从cmdb查询
     *
     * @param appId          Job业务ID
     * @param hosts          主机列表
     * @param refreshAgentId 是否刷新主机的bk_agent_id
     */
    ServiceListAppHostResultDTO listAppHostsPreferCache(Long appId, List<HostDTO> hosts, boolean refreshAgentId);

    /**
     * 批量获取主机。如果在Job缓存的主机中不存在，那么从cmdb查询
     *
     * @param tenantId 租户ID
     * @param hosts    主机
     * @return 主机
     */
    List<ApplicationHostDTO> listHosts(String tenantId, Collection<HostDTO> hosts);

    /**
     * 根据云区域ID与IPv6地址查询主机。如果在同步的主机中不存在，那么从cmdb查询
     * ipv6字段精确匹配目标主机多个Ipv6地址中的其中一个
     *
     * @param tenantId    租户ID
     * @param cloudAreaId 云区域ID
     * @param ipv6        IPv6地址
     * @return 主机
     */
    List<ApplicationHostDTO> listHostsByCloudIpv6(String tenantId, Long cloudAreaId, String ipv6);

    /**
     * 根据主机批量获取主机。如果在同步的主机中不存在，那么从cmdb查询
     *
     * @param tenantId 租户ID
     * @param cloudIps 主机云区域+ip列表
     * @return 主机 Map<hostId, host>
     */
    Map<String, ApplicationHostDTO> listHostsByIps(String tenantId, Collection<String> cloudIps);

}
