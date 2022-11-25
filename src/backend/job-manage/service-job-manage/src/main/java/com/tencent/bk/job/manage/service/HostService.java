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

package com.tencent.bk.job.manage.service;

import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.DynamicGroupWithHost;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.manage.common.consts.whiteip.ActionScopeEnum;
import com.tencent.bk.job.manage.model.web.request.AgentStatisticsReq;
import com.tencent.bk.job.manage.model.web.request.ipchooser.AppTopologyTreeNode;
import com.tencent.bk.job.manage.model.web.request.ipchooser.ListHostByBizTopologyNodesReq;
import com.tencent.bk.job.manage.model.web.vo.CcTopologyNodeVO;
import com.tencent.bk.job.manage.model.web.vo.NodeInfoVO;
import com.tencent.bk.job.manage.model.web.vo.index.AgentStatistics;

import java.util.Collection;
import java.util.List;

/**
 * 主机、topo相关服务
 */
public interface HostService {
    boolean existHost(long bizId, String ip);

    List<ApplicationHostDTO> getHostsByAppId(Long appId);

    /**
     * 新增业务下的主机
     *
     * @param bizId      业务ID
     * @param insertList 主机信息
     * @return 新增失败的主机ID
     */
    List<Long> insertHostsToBiz(Long bizId, List<ApplicationHostDTO> insertList);

    /**
     * 更新业务下的主机
     *
     * @param bizId        业务ID
     * @param hostInfoList 主机信息
     * @return 更新失败的主机ID
     */
    List<Long> updateHostsInBiz(Long bizId, List<ApplicationHostDTO> hostInfoList);

    /**
     * 将主机从业务下移除但不删除
     *
     * @param bizId    业务ID
     * @param hostList 主机信息
     * @return 移除失败的主机ID
     */
    List<Long> removeHostsFromBiz(Long bizId, List<ApplicationHostDTO> hostList);

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

    List<AppTopologyTreeNode> getAppTopologyTreeNodeDetail(String username,
                                                           AppResourceScope appResourceScope,
                                                           List<AppTopologyTreeNode> treeNodeList);

    List<List<InstanceTopologyDTO>> queryBizNodePaths(String username,
                                                      Long bizId,
                                                      List<InstanceTopologyDTO> nodeList);

    List<NodeInfoVO> getBizHostsByNode(String username,
                                       Long bizId,
                                       List<AppTopologyTreeNode> treeNodeList);

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
                                                List<AppTopologyTreeNode> appTopoNodeList);

    AgentStatistics getAgentStatistics(String username,
                                       Long appId,
                                       AgentStatisticsReq agentStatisticsReq);

    void fillAgentStatus(List<ApplicationHostDTO> hosts);


    Boolean existsHost(Long bizId, String ip);

    /**
     * 检查主机是否在业务下
     *
     * @param appId   Job业务ID
     * @param hostIps 被检查的主机
     * @return 非法的主机
     */
    List<IpDTO> checkAppHosts(Long appId, List<IpDTO> hostIps);

    /**
     * 根据主机IP批量获取主机。如果在同步的主机中不存在，那么从cmdb查询
     *
     * @param hostIps 主机IP
     * @return 主机
     */
    List<ApplicationHostDTO> listHosts(Collection<IpDTO> hostIps);
}
