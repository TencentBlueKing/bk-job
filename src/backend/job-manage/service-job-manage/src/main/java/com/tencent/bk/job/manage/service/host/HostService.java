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

package com.tencent.bk.job.manage.service.host;

import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.BasicHostDTO;
import com.tencent.bk.job.common.model.dto.DynamicGroupWithHost;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.model.dto.HostSimpleDTO;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.manage.common.consts.whiteip.ActionScopeEnum;
import com.tencent.bk.job.manage.model.inner.ServiceListAppHostResultDTO;
import com.tencent.bk.job.manage.model.web.request.ipchooser.BizTopoNode;
import com.tencent.bk.job.manage.model.web.request.ipchooser.ListHostByBizTopologyNodesReq;
import com.tencent.bk.job.manage.model.web.vo.CcTopologyNodeVO;
import com.tencent.bk.job.manage.model.web.vo.NodeInfoVO;

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
     * @return 新增主机数量
     */
    int createOrUpdateHostBeforeLastTime(ApplicationHostDTO hostInfoDTO);

    /**
     * 更新主机属性（仅更新时间戳在当前数据之前的数据）
     *
     * @param hostInfoDTO 主机信息
     * @return 成功更新的主机数量
     */
    int updateHostAttrsBeforeLastTime(ApplicationHostDTO hostInfoDTO);

    /**
     * 删除主机（仅删除时间戳在当前数据之前的数据）
     *
     * @param hostInfoDTO 主机信息
     */
    void deleteHostBeforeLastTime(ApplicationHostDTO hostInfoDTO);

    long countHostsByOsType(String osType);

    /**
     * 根据给定条件查询主机信息
     *
     * @param applicationHostInfoCondition 业务查询条件
     * @param baseSearchCondition          通用查询分页条件
     * @return 带分页信息的主机信息列表
     */
    PageData<ApplicationHostDTO> listAppHost(ApplicationHostDTO applicationHostInfoCondition,
                                             BaseSearchCondition baseSearchCondition);

    /**
     * 查询指定业务的拓扑树
     *
     * @param username         用户名
     * @param appResourceScope 资源范围
     * @return 拓扑结构树
     */
    CcTopologyNodeVO listAppTopologyTree(String username, AppResourceScope appResourceScope);

    /**
     * 查询带主机信息的业务的拓扑树
     *
     * @param username         用户名
     * @param appResourceScope 资源范围
     * @return 带主机信息的拓扑结构树
     */
    CcTopologyNodeVO listAppTopologyHostTree(String username, AppResourceScope appResourceScope);

    /**
     * 查询带主机数量信息的业务拓扑树
     *
     * @param username         用户名
     * @param appResourceScope 资源范围
     * @return 带主机数量信息的拓扑结构树
     */
    CcTopologyNodeVO listAppTopologyHostCountTree(String username, AppResourceScope appResourceScope);

    PageData<HostInfoVO> listHostByAppTopologyNodes(String username,
                                                    AppResourceScope appResourceScope,
                                                    ListHostByBizTopologyNodesReq req);

    PageData<String> listIPByBizTopologyNodes(String username,
                                              AppResourceScope appResourceScope,
                                              ListHostByBizTopologyNodesReq req);

    List<BizTopoNode> getAppTopologyTreeNodeDetail(String username,
                                                   AppResourceScope appResourceScope,
                                                   List<BizTopoNode> treeNodeList);

    List<List<InstanceTopologyDTO>> queryBizNodePaths(String username,
                                                      Long bizId,
                                                      List<InstanceTopologyDTO> nodeList);

    List<NodeInfoVO> getBizHostsByNode(String username,
                                       Long bizId,
                                       List<BizTopoNode> treeNodeList);

    /**
     * 获取业务下动态分组列表
     *
     * @param appResourceScope 资源范围
     * @param username         用户名
     * @return 动态分组信息列表
     */
    List<DynamicGroupWithHost> getAppDynamicGroupList(String username, AppResourceScope appResourceScope);

    /**
     * 根据动态分组 ID 列表批量获取带主机信息的动态分组信息列表
     *
     * @param username           用户名
     * @param bizId              业务 ID
     * @param dynamicGroupIdList 动态分组 ID 列表
     * @return 带主机信息的动态分组信息列表
     */
    List<DynamicGroupWithHost> getBizDynamicGroupHostList(String username,
                                                          Long bizId,
                                                          List<String> dynamicGroupIdList);

    /**
     * 根据 IP 列表查询主机信息
     *
     * @param username    用户名
     * @param appId       业务 ID
     * @param checkIpList 待查询的 IP 列表
     * @return 主机信息列表
     */
    List<HostInfoVO> getHostsByIp(String username,
                                  Long appId,
                                  ActionScopeEnum actionScope,
                                  List<String> checkIpList);

    List<HostInfoVO> listHostByAppTopologyNodes(String username,
                                                Long appId,
                                                List<BizTopoNode> appTopoNodeList);

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
