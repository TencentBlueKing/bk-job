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

import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.BasicHostDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.model.dto.HostSimpleDTO;
import com.tencent.bk.job.manage.model.inner.ServiceListAppHostResultDTO;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 主机、topo相关服务
 */
public interface HostService {

    List<ApplicationHostDTO> getHostsByAppId(Long appId);

    /**
     * 批量新增主机
     *
     * @param insertList 主机信息
     * @return 成功插入的主机数量
     */
    int batchInsertHosts(List<ApplicationHostDTO> insertList);

    /**
     * 批量更新主机（只更新时间戳比当前数据旧的）
     *
     * @param hostInfoList 主机信息
     * @return 成功更新的主机数量
     */
    int batchUpdateHostsBeforeLastTime(List<ApplicationHostDTO> hostInfoList);

    /**
     * 创建或更新主机（仅更新时间戳在当前数据之前的数据）
     *
     * @param hostInfoDTO 主机信息
     * @return Pair<是否创建 ， 受影响主机数量>
     */
    Pair<Boolean, Integer> createOrUpdateHostBeforeLastTime(ApplicationHostDTO hostInfoDTO);

    int updateHostAttrsByHostId(ApplicationHostDTO hostInfoDTO);

    /**
     * 更新主机属性（仅更新时间戳在当前数据之前的数据）
     *
     * @param hostInfoDTO 主机信息
     * @return 成功更新的主机数量
     */
    int updateHostAttrsBeforeLastTime(ApplicationHostDTO hostInfoDTO);

    /**
     * 删除主机（仅删除时间戳等于或在当前数据之前的数据）
     *
     * @param hostInfoDTO 主机信息
     */
    int deleteHostBeforeOrEqualLastTime(ApplicationHostDTO hostInfoDTO);

    void updateDbHostToCache(Long hostId);

    long countHostsByOsType(String osType);

    Map<String, Integer> groupHostByOsType();

    List<List<InstanceTopologyDTO>> queryBizNodePaths(String username,
                                                      Long bizId,
                                                      List<InstanceTopologyDTO> nodeList);

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
     * @param hosts 主机
     * @return 主机
     */
    List<ApplicationHostDTO> listHosts(Collection<HostDTO> hosts);

    /**
     * 根据云区域ID与IPv6地址查询主机。如果在同步的主机中不存在，那么从cmdb查询
     * ipv6字段精确匹配目标主机多个Ipv6地址中的其中一个
     *
     * @param cloudAreaId 云区域ID
     * @param ipv6        IPv6地址
     * @return 主机
     */
    List<ApplicationHostDTO> listHostsByCloudIpv6(Long cloudAreaId, String ipv6);

    /**
     * 根据主机批量获取主机。如果在同步的主机中不存在，那么从cmdb查询
     *
     * @param hostIds 主机ID列表
     * @return 主机 Map<hostId, host>
     */
    Map<Long, ApplicationHostDTO> listHostsByHostIds(Collection<Long> hostIds);

    /**
     * 从cmdb实时查询主机
     *
     * @param hostIds 主机ID列表
     * @return 主机
     */
    List<ApplicationHostDTO> listHostsFromCmdbByHostIds(List<Long> hostIds);

    /**
     * 根据ip获取主机
     *
     * @param cloudIp 云区域+IP
     * @return 主机
     */
    ApplicationHostDTO getHostByIp(String cloudIp);

    /**
     * 根据主机批量获取主机。如果在同步的主机中不存在，那么从cmdb查询
     *
     * @param cloudIps 主机云区域+ip列表
     * @return 主机 Map<hostId, host>
     */
    Map<String, ApplicationHostDTO> listHostsByIps(Collection<String> cloudIps);

    /**
     * 更新主机状态
     *
     * @param simpleHostList 主机列表
     * @return 更新成功的条数
     */
    int updateHostsStatus(List<HostSimpleDTO> simpleHostList);

    /**
     * 查询所有主机基础信息
     *
     * @return 主机基础信息
     */
    List<BasicHostDTO> listAllBasicHost();

    /**
     * 根据主机基础信息批量删除主机
     *
     * @return 成功删除的主机数量
     */
    int deleteByBasicHost(List<BasicHostDTO> basicHostList);
}
