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

package com.tencent.bk.job.manage.api.web.impl;

import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.vo.DynamicGroupIdWithMeta;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.common.model.vo.TargetNodeVO;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.PageUtil;
import com.tencent.bk.job.manage.api.web.WebHostResource;
import com.tencent.bk.job.manage.model.dto.DynamicGroupDTO;
import com.tencent.bk.job.manage.model.web.request.AgentStatisticsReq;
import com.tencent.bk.job.manage.model.web.request.HostCheckReq;
import com.tencent.bk.job.manage.model.web.request.chooser.ListTopologyTreesReq;
import com.tencent.bk.job.manage.model.web.request.chooser.host.BizTopoNode;
import com.tencent.bk.job.manage.model.web.request.chooser.host.GetHostAgentStatisticsByDynamicGroupsReq;
import com.tencent.bk.job.manage.model.web.request.chooser.host.GetHostAgentStatisticsByNodesReq;
import com.tencent.bk.job.manage.model.web.request.chooser.host.HostDetailReq;
import com.tencent.bk.job.manage.model.web.request.chooser.host.HostIdWithMeta;
import com.tencent.bk.job.manage.model.web.request.chooser.host.ListDynamicGroupsReq;
import com.tencent.bk.job.manage.model.web.request.chooser.host.ListHostByBizTopologyNodesReq;
import com.tencent.bk.job.manage.model.web.request.chooser.host.PageListHostsByDynamicGroupReq;
import com.tencent.bk.job.manage.model.web.request.chooser.host.QueryNodesPathReq;
import com.tencent.bk.job.manage.model.web.vo.CcTopologyNodeVO;
import com.tencent.bk.job.manage.model.web.vo.DynamicGroupBasicVO;
import com.tencent.bk.job.manage.model.web.vo.common.AgentStatistics;
import com.tencent.bk.job.manage.model.web.vo.ipchooser.DynamicGroupHostStatisticsVO;
import com.tencent.bk.job.manage.model.web.vo.ipchooser.NodeHostStatisticsVO;
import com.tencent.bk.job.manage.service.agent.statistics.ScopeAgentStatisticsService;
import com.tencent.bk.job.manage.service.agent.statistics.ScopeDynamicGroupAgentStatisticsService;
import com.tencent.bk.job.manage.service.agent.statistics.ScopeNodeAgentStatisticsService;
import com.tencent.bk.job.manage.service.host.HostDetailService;
import com.tencent.bk.job.manage.service.host.ScopeDynamicGroupHostService;
import com.tencent.bk.job.manage.service.host.ScopeHostService;
import com.tencent.bk.job.manage.service.host.ScopeTopoHostService;
import com.tencent.bk.job.manage.service.host.WhiteIpAwareScopeHostService;
import com.tencent.bk.job.manage.service.impl.ScopeDynamicGroupService;
import com.tencent.bk.job.manage.service.impl.agent.AgentStatusService;
import com.tencent.bk.job.manage.service.topo.ScopeTopoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class WebHostResourceImpl implements WebHostResource {

    private final ScopeTopoService scopeTopoService;
    private final ScopeTopoHostService scopeTopoHostService;
    private final ScopeHostService scopeHostService;
    private final WhiteIpAwareScopeHostService whiteIpAwareScopeHostService;
    private final ScopeNodeAgentStatisticsService scopeNodeAgentStatisticsService;
    private final ScopeDynamicGroupAgentStatisticsService scopeDGAgentStatsService;
    private final ScopeAgentStatisticsService scopeAgentStatisticsService;
    private final AgentStatusService agentStatusService;
    private final HostDetailService hostDetailService;
    private final ScopeDynamicGroupHostService scopeDynamicGroupHostService;
    private final ScopeDynamicGroupService scopeDynamicGroupService;

    @Autowired
    public WebHostResourceImpl(ScopeTopoService scopeTopoService,
                               ScopeTopoHostService scopeTopoHostService,
                               ScopeHostService scopeHostService,
                               WhiteIpAwareScopeHostService whiteIpAwareScopeHostService,
                               ScopeNodeAgentStatisticsService scopeNodeAgentStatisticsService,
                               ScopeDynamicGroupAgentStatisticsService scopeDGAgentStatsService,
                               ScopeAgentStatisticsService scopeAgentStatisticsService,
                               AgentStatusService agentStatusService,
                               HostDetailService hostDetailService,
                               ScopeDynamicGroupHostService bizDynamicGroupHostService,
                               ScopeDynamicGroupService scopeDynamicGroupService) {
        this.scopeTopoService = scopeTopoService;
        this.scopeTopoHostService = scopeTopoHostService;
        this.scopeHostService = scopeHostService;
        this.whiteIpAwareScopeHostService = whiteIpAwareScopeHostService;
        this.scopeNodeAgentStatisticsService = scopeNodeAgentStatisticsService;
        this.scopeDGAgentStatsService = scopeDGAgentStatsService;
        this.scopeAgentStatisticsService = scopeAgentStatisticsService;
        this.agentStatusService = agentStatusService;
        this.hostDetailService = hostDetailService;
        this.scopeDynamicGroupHostService = bizDynamicGroupHostService;
        this.scopeDynamicGroupService = scopeDynamicGroupService;
    }

    @Override
    public Response<AgentStatistics> agentStatistics(String username,
                                                     AppResourceScope appResourceScope,
                                                     String scopeType,
                                                     String scopeId,
                                                     AgentStatisticsReq agentStatisticsReq) {
        List<Long> hostIdList = agentStatisticsReq.getHostList()
            .stream().map(HostIdWithMeta::getHostId).collect(Collectors.toList());
        List<BizTopoNode> nodeList = agentStatisticsReq.getNodeList();
        List<String> dynamicGroupIdList = agentStatisticsReq.getDynamicGroupIds();
        String tenantId = JobContextUtil.getTenantId();
        AgentStatistics agentStatistics = scopeAgentStatisticsService.getAgentStatistics(
            tenantId,
            appResourceScope,
            hostIdList,
            nodeList,
            dynamicGroupIdList
        );
        return Response.buildSuccessResp(agentStatistics);
    }

    @Override
    public Response<List<DynamicGroupBasicVO>> listAllDynamicGroups(String username,
                                                                    AppResourceScope appResourceScope,
                                                                    String scopeType,
                                                                    String scopeId) {
        return listDynamicGroups(username, appResourceScope, scopeType, scopeId, null);
    }

    // 标准接口1
    @Override
    public Response<List<CcTopologyNodeVO>> listTopologyHostCountTrees(String username,
                                                                       AppResourceScope appResourceScope,
                                                                       String scopeType,
                                                                       String scopeId,
                                                                       ListTopologyTreesReq req) {
        return Response.buildSuccessResp(
            Collections.singletonList(
                scopeTopoHostService.listAppTopologyHostCountTree(username, appResourceScope)
            )
        );
    }

    // 标准接口2
    @Override
    public Response<List<List<CcTopologyNodeVO>>> queryNodePaths(String username,
                                                                 AppResourceScope appResourceScope,
                                                                 String scopeType,
                                                                 String scopeId,
                                                                 QueryNodesPathReq req) {
        return queryNodePaths(appResourceScope, req.getNodeList());
    }

    public Response<List<List<CcTopologyNodeVO>>> queryNodePaths(AppResourceScope appResourceScope,
                                                                 List<TargetNodeVO> targetNodeVOList) {
        List<List<InstanceTopologyDTO>> pathList = scopeTopoService.queryNodePaths(appResourceScope, targetNodeVOList);
        List<List<CcTopologyNodeVO>> resultList = new ArrayList<>();
        for (List<InstanceTopologyDTO> instanceTopologyDTOS : pathList) {
            if (instanceTopologyDTOS == null) {
                continue;
            }
            resultList.add(instanceTopologyDTOS.stream().map(it -> {
                CcTopologyNodeVO ccTopologyNodeVO = new CcTopologyNodeVO();
                ccTopologyNodeVO.setObjectId(it.getObjectId());
                ccTopologyNodeVO.setObjectName(it.getObjectName());
                ccTopologyNodeVO.setInstanceId(it.getInstanceId());
                ccTopologyNodeVO.setInstanceName(it.getInstanceName());
                return ccTopologyNodeVO;
            }).collect(Collectors.toList()));
        }
        return Response.buildSuccessResp(resultList);
    }

    // 标准接口3
    @Override
    public Response<PageData<HostInfoVO>> listHostByBizTopologyNodes(String username,
                                                                     AppResourceScope appResourceScope,
                                                                     String scopeType,
                                                                     String scopeId,
                                                                     ListHostByBizTopologyNodesReq req) {
        Pair<Long, Long> pagePair = PageUtil.normalizePageParam(req.getStart(), req.getPageSize());
        PageData<ApplicationHostDTO> pageHostList = scopeHostService.searchHost(
            appResourceScope,
            req.getNodeList(),
            req.getAlive(),
            req.getSearchContent(),
            req.getCleanIpKeyList(),
            req.getCleanIpv6KeyList(),
            req.getCleanHostNameKeyList(),
            req.getCleanOsNameKeyList(),
            pagePair.getLeft(),
            pagePair.getRight()
        );
        String tenantId = JobContextUtil.getTenantId();
        hostDetailService.fillDetailForApplicationHosts(tenantId, pageHostList.getData());
        return Response.buildSuccessResp(PageUtil.transferPageData(
            pageHostList,
            ApplicationHostDTO::toVO
        ));
    }

    // 标准接口4
    @Override
    public Response<PageData<HostIdWithMeta>> listHostIdByBizTopologyNodes(String username,
                                                                           AppResourceScope appResourceScope,
                                                                           String scopeType,
                                                                           String scopeId,
                                                                           ListHostByBizTopologyNodesReq req) {
        // 参数标准化
        Pair<Long, Long> pagePair = PageUtil.normalizePageParam(req.getStart(), req.getPageSize());
        PageData<Long> pageData = scopeHostService.listHostIdByBizTopologyNodes(
            appResourceScope,
            req.getNodeList(),
            req.getSearchContent(),
            req.getAlive(),
            req.getCleanIpKeyList(),
            req.getCleanIpv6KeyList(),
            req.getCleanHostNameKeyList(),
            req.getCleanOsNameKeyList(),
            pagePair.getLeft(),
            pagePair.getRight()
        );
        PageData<HostIdWithMeta> finalPageData = PageUtil.transferPageData(
            pageData,
            hostId -> new HostIdWithMeta(hostId, null)
        );
        return Response.buildSuccessResp(finalPageData);
    }

    // 标准接口5
    @Override
    public Response<List<NodeHostStatisticsVO>> getHostAgentStatisticsByNodes(String username,
                                                                              AppResourceScope appResourceScope,
                                                                              String scopeType,
                                                                              String scopeId,
                                                                              GetHostAgentStatisticsByNodesReq req) {
        List<NodeHostStatisticsVO> resultList = scopeNodeAgentStatisticsService.getAgentStatisticsByNodes(
            appResourceScope,
            req.getNodeList()
        );
        return Response.buildSuccessResp(resultList);
    }

    // 标准接口6
    @Override
    public Response<List<DynamicGroupBasicVO>> listDynamicGroups(String username,
                                                                 AppResourceScope appResourceScope,
                                                                 String scopeType,
                                                                 String scopeId,
                                                                 ListDynamicGroupsReq req) {
        List<DynamicGroupIdWithMeta> idWithMetaList = req == null ? null : req.getDynamicGroupList();
        Set<String> ids = idWithMetaList == null ? null :
            idWithMetaList.stream().map(DynamicGroupIdWithMeta::getId).collect(Collectors.toSet());
        List<DynamicGroupDTO> dynamicGroupList = scopeDynamicGroupService.listOrderedDynamicGroup(
            appResourceScope,
            ids
        );
        List<DynamicGroupBasicVO> dynamicGroupInfoList = dynamicGroupList.stream()
            .map(DynamicGroupDTO::toBasicVO)
            .collect(Collectors.toList());
        return Response.buildSuccessResp(dynamicGroupInfoList);
    }

    // 标准接口7
    @Override
    public Response<List<DynamicGroupHostStatisticsVO>> getHostAgentStatisticsByDynamicGroups(
        String username,
        AppResourceScope appResourceScope,
        String scopeType,
        String scopeId,
        GetHostAgentStatisticsByDynamicGroupsReq req
    ) {
        String tenantId = JobContextUtil.getTenantId();
        List<DynamicGroupHostStatisticsVO> resultList = scopeDGAgentStatsService.getAgentStatisticsByDynamicGroups(
            tenantId,
            appResourceScope,
            req.getDynamicGroupList()
        );
        return Response.buildSuccessResp(resultList);
    }

    // 标准接口8
    @Override
    public Response<PageData<HostInfoVO>> pageListHostsByDynamicGroup(String username,
                                                                      AppResourceScope appResourceScope,
                                                                      String scopeType,
                                                                      String scopeId,
                                                                      PageListHostsByDynamicGroupReq req) {
        String tenantId = JobContextUtil.getTenantId();
        PageData<ApplicationHostDTO> pageData = scopeDynamicGroupHostService.pageHostByDynamicGroups(
            tenantId,
            appResourceScope,
            req.getId(),
            req.getStart().intValue(),
            req.getPageSize().intValue()
        );
        // 填充Agent状态数据
        if (CollectionUtils.isNotEmpty(pageData.getData())) {
            agentStatusService.fillRealTimeAgentStatus(pageData.getData());
        }
        return Response.buildSuccessResp(PageUtil.transferPageData(pageData, ApplicationHostDTO::toVO));
    }

    // 标准接口9
    @Override
    public Response<List<HostInfoVO>> checkHosts(String username,
                                                 AppResourceScope appResourceScope,
                                                 String scopeType,
                                                 String scopeId,
                                                 HostCheckReq req) {
        // 查出所有主机
        List<ApplicationHostDTO> hostDTOList = whiteIpAwareScopeHostService.findHosts(appResourceScope, req);
        // 填充云区域名称等信息
        String tenantId = JobContextUtil.getTenantId();
        hostDetailService.fillDetailForApplicationHosts(tenantId, hostDTOList);
        // 填充实时agent状态
        agentStatusService.fillRealTimeAgentStatus(hostDTOList);
        List<HostInfoVO> hostList = hostDTOList.stream()
            .map(ApplicationHostDTO::toVO)
            .collect(Collectors.toList());
        return Response.buildSuccessResp(hostList);
    }

    // 标准接口10
    @Override
    public Response<List<HostInfoVO>> getHostDetails(String username,
                                                     AppResourceScope appResourceScope,
                                                     String scopeType,
                                                     String scopeId,
                                                     HostDetailReq req) {
        Collection<Long> hostIds = req.getHostList().stream()
            .map(HostIdWithMeta::getHostId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        String tenantId = JobContextUtil.getTenantId();
        List<ApplicationHostDTO> hostList = hostDetailService.listHostDetails(tenantId, appResourceScope, hostIds);
        // 排序：Agent异常机器在前，Agent正常机器在后
        List<HostInfoVO> hostInfoVOList = hostList.stream()
            .filter(hostDTO -> !hostDTO.getGseAgentAlive())
            .map(ApplicationHostDTO::toVO)
            .collect(Collectors.toList());
        hostInfoVOList.addAll(hostList.stream()
            .filter(ApplicationHostDTO::getGseAgentAlive)
            .map(ApplicationHostDTO::toVO)
            .collect(Collectors.toList()));
        return Response.buildSuccessResp(hostInfoVOList);
    }

}
