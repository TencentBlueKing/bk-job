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
import com.tencent.bk.job.common.model.dto.ApplicationHostInfoDTO;
import com.tencent.bk.job.common.model.dto.ApplicationInfoDTO;
import com.tencent.bk.job.common.model.dto.DynamicGroupInfoDTO;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.manage.model.web.request.AgentStatisticsReq;
import com.tencent.bk.job.manage.model.web.request.ipchooser.AppTopologyTreeNode;
import com.tencent.bk.job.manage.model.web.request.ipchooser.ListHostByBizTopologyNodesReq;
import com.tencent.bk.job.manage.model.web.vo.CcTopologyNodeVO;
import com.tencent.bk.job.manage.model.web.vo.NodeInfoVO;
import com.tencent.bk.job.manage.model.web.vo.index.AgentStatistics;

import java.util.List;

/**
 * @since 7/11/2019 19:10
 */
public interface ApplicationService {

    void setWhiteIPService(WhiteIPService whiteIPService);

    /**
     * 根据当前业务Id查询包含该业务的业务集与全业务
     *
     * @param appId
     * @return
     */
    List<Long> getFullAppIds(Long appId);

    /**
     * 根据给定条件查询主机信息
     *
     * @param applicationHostInfoCondition 业务查询条件
     * @param baseSearchCondition          通用查询分页条件
     * @return 带分页信息的主机信息列表
     */
    PageData<ApplicationHostInfoDTO> listAppHost(ApplicationHostInfoDTO applicationHostInfoCondition,
                                                 BaseSearchCondition baseSearchCondition);

    /**
     * 查询指定业务的拓扑树
     *
     * @param username 用户名
     * @param appId    业务 ID
     * @return 拓扑结构树
     */
    CcTopologyNodeVO listAppTopologyTree(String username, Long appId);

    /**
     * 查询带主机信息的业务的拓扑树
     *
     * @param username 用户名
     * @param appId    业务 ID
     * @return 带主机信息的拓扑结构树
     */
    CcTopologyNodeVO listAppTopologyHostTree(String username, Long appId);

    /**
     * 查询带主机数量信息的业务拓扑树
     *
     * @param username 用户名
     * @param appId    业务 ID
     * @return 带主机数量信息的拓扑结构树
     */
    CcTopologyNodeVO listAppTopologyHostCountTree(String username, Long appId);

    PageData<HostInfoVO> listHostByBizTopologyNodes(String username, Long appId, ListHostByBizTopologyNodesReq req);

    PageData<String> listIPByBizTopologyNodes(String username, Long appId, ListHostByBizTopologyNodesReq req);

    List<AppTopologyTreeNode> getAppTopologyTreeNodeDetail(String username, Long appId,
                                                           List<AppTopologyTreeNode> treeNodeList);

    List<List<InstanceTopologyDTO>> queryNodePaths(String username, Long appId, List<InstanceTopologyDTO> nodeList);

    List<NodeInfoVO> getHostsByNode(String username, Long appId, List<AppTopologyTreeNode> treeNodeList);

    /**
     * 获取业务下动态分组列表
     *
     * @param appId    业务 ID
     * @param username 用户名
     * @return 动态分组信息列表
     */
    List<DynamicGroupInfoDTO> getDynamicGroupList(String username, Long appId);

    /**
     * 根据动态分组 ID 列表批量获取带主机信息的动态分组信息列表
     *
     * @param username           用户名
     * @param appId              业务 ID
     * @param dynamicGroupIdList 动态分组 ID 列表
     * @return 带主机信息的动态分组信息列表
     */
    List<DynamicGroupInfoDTO> getDynamicGroupHostList(String username, Long appId, List<String> dynamicGroupIdList);

    /**
     * 根据 IP 列表查询主机信息
     *
     * @param username    用户名
     * @param appId       业务 ID
     * @param checkIpList 待查询的 IP 列表
     * @return 主机信息列表
     */
    List<HostInfoVO> getHostsByIp(String username, Long appId, List<String> checkIpList);

    List<ApplicationInfoDTO> listAllAppsFromLocalDB();

    /**
     * 判断用户是否有业务权限
     * <p>
     * 接入权限中心后不应使用本接口
     *
     * @param appId    业务 ID
     * @param username 用户名
     * @return 是否有业务权限
     */
    @Deprecated
    boolean checkAppPermission(long appId, String username);

    /**
     * 根据业务 ID 拉取业务信息
     *
     * @param appId 业务 ID
     * @return 业务信息
     */
    ApplicationInfoDTO getAppInfoById(Long appId);

    List<HostInfoVO> listHostByBizTopologyNodes(String username, Long appId, List<AppTopologyTreeNode> appTopoNodeList);

    AgentStatistics getAgentStatistics(String username, Long appId, AgentStatisticsReq agentStatisticsReq);

    void fillAgentStatus(List<ApplicationHostInfoDTO> hosts);

    Long createApp(ApplicationInfoDTO applicationInfoDTO);

    Integer countApps(String username);

    Boolean existsHost(Long appId, String ip);
}
