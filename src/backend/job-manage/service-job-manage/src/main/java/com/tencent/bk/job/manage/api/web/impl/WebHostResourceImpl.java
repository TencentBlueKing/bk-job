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

package com.tencent.bk.job.manage.api.web.impl;

import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.cc.sdk.BkNetClient;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.vo.CloudAreaInfoVO;
import com.tencent.bk.job.common.model.vo.DynamicGroupIdWithMeta;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.common.model.vo.TargetNodeVO;
import com.tencent.bk.job.common.util.PageUtil;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.manage.api.common.constants.whiteip.ActionScopeEnum;
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
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.host.BizTopoHostService;
import com.tencent.bk.job.manage.service.host.HostDetailService;
import com.tencent.bk.job.manage.service.host.HostService;
import com.tencent.bk.job.manage.service.host.ScopeHostService;
import com.tencent.bk.job.manage.service.host.ScopeTopoHostService;
import com.tencent.bk.job.manage.service.host.WhiteIpAwareScopeHostService;
import com.tencent.bk.job.manage.service.host.impl.BizDynamicGroupHostService;
import com.tencent.bk.job.manage.service.impl.BizDynamicGroupService;
import com.tencent.bk.job.manage.service.impl.ScopeDynamicGroupService;
import com.tencent.bk.job.manage.service.impl.agent.AgentStatusService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class WebHostResourceImpl implements WebHostResource {

    private final ApplicationService applicationService;
    private final HostService hostService;
    private final ScopeTopoHostService scopeTopoHostService;
    private final ScopeHostService scopeHostService;
    private final WhiteIpAwareScopeHostService whiteIpAwareScopeHostService;
    private final AgentStatusService agentStatusService;
    private final HostDetailService hostDetailService;
    private final BizTopoHostService bizTopoHostService;
    private final BizDynamicGroupService bizDynamicGroupService;
    private final BizDynamicGroupHostService bizDynamicGroupHostService;
    private final ScopeDynamicGroupService scopeDynamicGroupHostService;

    @Autowired
    public WebHostResourceImpl(ApplicationService applicationService,
                               HostService hostService,
                               ScopeTopoHostService scopeTopoHostService,
                               ScopeHostService scopeHostService,
                               WhiteIpAwareScopeHostService whiteIpAwareScopeHostService,
                               AgentStatusService agentStatusService,
                               HostDetailService hostDetailService,
                               BizTopoHostService bizTopoHostService,
                               BizDynamicGroupService bizDynamicGroupService,
                               BizDynamicGroupHostService bizDynamicGroupHostService,
                               ScopeDynamicGroupService scopeDynamicGroupHostService) {
        this.applicationService = applicationService;
        this.hostService = hostService;
        this.scopeTopoHostService = scopeTopoHostService;
        this.scopeHostService = scopeHostService;
        this.whiteIpAwareScopeHostService = whiteIpAwareScopeHostService;
        this.agentStatusService = agentStatusService;
        this.hostDetailService = hostDetailService;
        this.bizTopoHostService = bizTopoHostService;
        this.bizDynamicGroupService = bizDynamicGroupService;
        this.bizDynamicGroupHostService = bizDynamicGroupHostService;
        this.scopeDynamicGroupHostService = scopeDynamicGroupHostService;
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
            req.getIpKeyList(),
            req.getIpv6KeyList(),
            req.getHostNameKeyList(),
            req.getOsNameKeyList(),
            pagePair.getLeft(),
            pagePair.getRight()
        );
        hostDetailService.fillDetailForApplicationHosts(pageHostList.getData());
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
            req.getIpKeyList(),
            req.getIpv6KeyList(),
            req.getHostNameKeyList(),
            req.getOsNameKeyList(),
            pagePair.getLeft(),
            pagePair.getRight()
        );
        PageData<HostIdWithMeta> finalPageData = PageUtil.transferPageData(
            pageData,
            hostId -> new HostIdWithMeta(hostId, null)
        );
        return Response.buildSuccessResp(finalPageData);
    }

    // 标准接口2
    @Override
    public Response<List<List<CcTopologyNodeVO>>> queryNodePaths(String username,
                                                                 AppResourceScope appResourceScope,
                                                                 String scopeType,
                                                                 String scopeId,
                                                                 QueryNodesPathReq req) {
        return queryNodePaths(username, appResourceScope, req.getNodeList());
    }

    public Response<List<List<CcTopologyNodeVO>>> queryNodePaths(String username,
                                                                 AppResourceScope appResourceScope,
                                                                 List<TargetNodeVO> targetNodeVOList) {
        ApplicationDTO appDTO = applicationService.getAppByScope(appResourceScope);
        if (appDTO.isBizSet()) {
            return Response.buildSuccessResp(Collections.emptyList());
        }
        List<List<InstanceTopologyDTO>> pathList = hostService.queryBizNodePaths(
            username,
            appDTO.getBizIdIfBizApp(),
            targetNodeVOList.stream().map(it -> {
                InstanceTopologyDTO instanceTopologyDTO = new InstanceTopologyDTO();
                instanceTopologyDTO.setObjectId(it.getObjectId());
                instanceTopologyDTO.setInstanceId(it.getInstanceId());
                return instanceTopologyDTO;
            }).collect(Collectors.toList()));
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

    // 标准接口5
    @Override
    public Response<List<NodeHostStatisticsVO>> getHostAgentStatisticsByNodes(String username,
                                                                              AppResourceScope appResourceScope,
                                                                              String scopeType,
                                                                              String scopeId,
                                                                              GetHostAgentStatisticsByNodesReq req) {
        if (ResourceScopeTypeEnum.BIZ_SET.getValue().equals(scopeType)) {
            return Response.buildCommonFailResp(ErrorCode.NOT_SUPPORT_FEATURE_FOR_BIZ_SET);
        }
        List<TargetNodeVO> nodeList = req.getNodeList();
        if (CollectionUtils.isEmpty(nodeList)) {
            return Response.buildSuccessResp(Collections.emptyList());
        }
        List<NodeHostStatisticsVO> resultList = new ArrayList<>();
        // TODO:性能优化
        for (TargetNodeVO node : nodeList) {
            List<ApplicationHostDTO> hostList = bizTopoHostService.listHostByNode(
                Long.parseLong(scopeId),
                BizTopoNode.fromTargetNodeVO(node)
            );
            AgentStatistics agentStatistics = agentStatusService.calcAgentStatistics(hostList);
            resultList.add(new NodeHostStatisticsVO(node, agentStatistics));
        }
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
        List<DynamicGroupDTO> dynamicGroupList = scopeDynamicGroupHostService.listOrderedDynamicGroup(
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
        if (ResourceScopeTypeEnum.BIZ_SET.getValue().equals(scopeType)) {
            return Response.buildCommonFailResp(ErrorCode.NOT_SUPPORT_FEATURE_FOR_BIZ_SET);
        }
        List<DynamicGroupIdWithMeta> idWithMetaList = req.getDynamicGroupList();
        if (CollectionUtils.isEmpty(idWithMetaList)) {
            return Response.buildSuccessResp(Collections.emptyList());
        }
        List<String> idList = idWithMetaList.stream().map(DynamicGroupIdWithMeta::getId).collect(Collectors.toList());
        List<DynamicGroupDTO> dynamicGroupList =
            bizDynamicGroupService.listDynamicGroup(Long.parseLong(scopeId), idList);
        List<DynamicGroupHostStatisticsVO> resultList = new ArrayList<>();
        for (DynamicGroupDTO dynamicGroupDTO : dynamicGroupList) {
            DynamicGroupHostStatisticsVO statisticsVO = new DynamicGroupHostStatisticsVO();
            statisticsVO.setDynamicGroup(dynamicGroupDTO.toBasicVO());
            List<ApplicationHostDTO> hostList = bizDynamicGroupHostService.listHostByDynamicGroup(
                appResourceScope,
                dynamicGroupDTO.getId()
            );
            AgentStatistics agentStatistics = agentStatusService.calcAgentStatistics(hostList);
            statisticsVO.setAgentStatistics(agentStatistics);
            resultList.add(statisticsVO);
        }
        return Response.buildSuccessResp(resultList);
    }

    // 标准接口8
    @Override
    public Response<PageData<HostInfoVO>> pageListHostsByDynamicGroup(String username,
                                                                      AppResourceScope appResourceScope,
                                                                      String scopeType,
                                                                      String scopeId,
                                                                      PageListHostsByDynamicGroupReq req) {
        PageData<ApplicationHostDTO> pageData = bizDynamicGroupHostService.pageHostByDynamicGroups(
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
        List<ApplicationHostDTO> hostDTOList = findHosts(appResourceScope, req);
        // 填充实时agent状态
        agentStatusService.fillRealTimeAgentStatus(hostDTOList);
        List<HostInfoVO> hostList = hostDTOList.stream()
            .map(ApplicationHostDTO::toVO)
            .collect(Collectors.toList());
        // 填充云区域名称
        hostList.forEach(hostInfoVO -> {
            CloudAreaInfoVO cloudAreaInfo = hostInfoVO.getCloudArea();
            if (cloudAreaInfo != null
                && cloudAreaInfo.getId() != null
                && StringUtils.isBlank(cloudAreaInfo.getName())) {
                cloudAreaInfo.setName(BkNetClient.getCloudAreaNameFromCache(cloudAreaInfo.getId()));
            }
        });
        return Response.buildSuccessResp(hostList);
    }

    private List<ApplicationHostDTO> findHosts(AppResourceScope appResourceScope, HostCheckReq req) {
        List<ApplicationHostDTO> hostDTOList = new ArrayList<>();
        // 根据主机ID解析主机
        findHostsByHostIds(appResourceScope, req.getActionScope(), req.getHostIdList(), hostDTOList);
        // 根据Ipv4解析主机
        findHostsByIpv4s(appResourceScope, req.getActionScope(), req.getIpList(), hostDTOList);
        // 根据Ipv6解析主机
        findHostsByIpv6s(appResourceScope, req.getActionScope(), req.getIpv6List(), hostDTOList);
        // 根据关键字（主机名称）解析主机
        findHostsByKeys(appResourceScope, req.getActionScope(), req.getKeyList(), hostDTOList);
        // 去重
        Set<Long> hostIdSet = new HashSet<>();
        Iterator<ApplicationHostDTO> iterator = hostDTOList.iterator();
        while (iterator.hasNext()) {
            ApplicationHostDTO hostDTO = iterator.next();
            if (hostIdSet.contains(hostDTO.getHostId())) {
                iterator.remove();
            } else {
                hostIdSet.add(hostDTO.getHostId());
            }
        }
        return hostDTOList;
    }

    private void findHostsByHostIds(AppResourceScope appResourceScope,
                                    ActionScopeEnum actionScope,
                                    List<Long> hostIdList,
                                    List<ApplicationHostDTO> hostDTOList) {
        if (CollectionUtils.isNotEmpty(hostIdList)) {
            // 根据hostId查资源范围及白名单内的主机详情
            hostDTOList.addAll(whiteIpAwareScopeHostService.getScopeHostsIncludingWhiteIPByHostId(
                appResourceScope,
                actionScope,
                hostIdList
            ));
        }
    }

    private void findHostsByIpv4s(AppResourceScope appResourceScope,
                                  ActionScopeEnum actionScope,
                                  List<String> ipOrCloudIpList,
                                  List<ApplicationHostDTO> hostDTOList) {
        if (CollectionUtils.isEmpty(ipOrCloudIpList)) {
            return;
        }
        Pair<Set<String>, Set<String>> pair = IpUtils.parseCleanIpv4AndCloudIpv4s(ipOrCloudIpList);
        Set<String> ipSet = pair.getLeft();
        Set<String> cloudIpSet = pair.getRight();
        // 根据ip地址查资源范围及白名单内的主机详情
        if (CollectionUtils.isNotEmpty(ipSet)) {
            hostDTOList.addAll(whiteIpAwareScopeHostService.getScopeHostsIncludingWhiteIPByIp(
                appResourceScope,
                actionScope,
                ipSet
            ));
        }
        if (CollectionUtils.isNotEmpty(cloudIpSet)) {
            hostDTOList.addAll(whiteIpAwareScopeHostService.getScopeHostsIncludingWhiteIPByCloudIp(
                appResourceScope,
                actionScope,
                cloudIpSet
            ));
        }
    }

    private void findHostsByIpv6s(AppResourceScope appResourceScope,
                                  ActionScopeEnum actionScope,
                                  List<String> ipv6OrCloudIpv6List,
                                  List<ApplicationHostDTO> hostDTOList) {
        if (CollectionUtils.isEmpty(ipv6OrCloudIpv6List)) {
            return;
        }
        Pair<Set<String>, Set<Pair<Long, String>>> pair = IpUtils.parseFullIpv6AndCloudIpv6s(ipv6OrCloudIpv6List);
        Set<String> ipv6Set = pair.getLeft();
        Set<Pair<Long, String>> cloudIpv6Set = pair.getRight();
        Set<String> allIpv6Set = new HashSet<>(ipv6Set);
        Map<String, Long> ipv6CloudIdMap = new HashMap<>();
        for (Pair<Long, String> cloudIpv6 : cloudIpv6Set) {
            allIpv6Set.add(cloudIpv6.getRight());
            ipv6CloudIdMap.put(cloudIpv6.getRight(), cloudIpv6.getLeft());
        }
        List<ApplicationHostDTO> hostList = whiteIpAwareScopeHostService.getScopeHostsIncludingWhiteIPByIpv6(
            appResourceScope,
            actionScope,
            allIpv6Set
        );
        for (ApplicationHostDTO host : hostList) {
            String ipv6 = host.getIpv6();
            Long cloudId = host.getCloudAreaId();
            // 未指定云区域ID的数据匹配所有ipv6符合的数据
            if (ipv6Set.contains(ipv6) ||
                // 指定了云区域ID的数据需要精确匹配
                (ipv6CloudIdMap.containsKey(ipv6) && cloudId.equals(ipv6CloudIdMap.get(ipv6)))) {
                // 根据ipv6地址查资源范围及白名单内的主机详情
                hostDTOList.add(host);
            }
        }
    }

    private void findHostsByKeys(AppResourceScope appResourceScope,
                                 ActionScopeEnum actionScope,
                                 List<String> keyList,
                                 List<ApplicationHostDTO> hostDTOList) {
        if (CollectionUtils.isEmpty(keyList)) {
            return;
        }
        // 根据关键字（主机名称）查资源范围及白名单内的主机详情
        hostDTOList.addAll(whiteIpAwareScopeHostService.getScopeHostsIncludingWhiteIPByKey(
            appResourceScope,
            actionScope,
            keyList
        ));
    }

    // 标准接口10
    @Override
    public Response<List<HostInfoVO>> getHostDetails(String username,
                                                     AppResourceScope appResourceScope,
                                                     String scopeType,
                                                     String scopeId,
                                                     HostDetailReq req) {
        Collection<Long> hostIds = req.getHostList().stream()
            .filter(hostIdWithMeta -> hostIdWithMeta.getHostId() != null)
            .map(HostIdWithMeta::getHostId)
            .collect(Collectors.toList());
        List<ApplicationHostDTO> hostList = hostDetailService.listHostDetails(appResourceScope, hostIds);
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

    @Override
    public Response<AgentStatistics> agentStatistics(String username,
                                                     AppResourceScope appResourceScope,
                                                     String scopeType,
                                                     String scopeId,
                                                     AgentStatisticsReq agentStatisticsReq) {
        List<ApplicationHostDTO> allHostList = new ArrayList<>();
        List<HostIdWithMeta> hostList = agentStatisticsReq.getHostList();
        if (CollectionUtils.isNotEmpty(hostList)) {
            List<ApplicationHostDTO> hostsById = scopeHostService.getScopeHostsByIds(
                appResourceScope,
                hostList.stream().map(HostIdWithMeta::getHostId).collect(Collectors.toList())
            );
            log.debug("hostsById={}", hostsById);
            allHostList.addAll(hostsById);
        }
        List<BizTopoNode> nodeList = agentStatisticsReq.getNodeList();
        if (CollectionUtils.isNotEmpty(nodeList)) {
            long bizId = Long.parseLong(scopeId);
            List<ApplicationHostDTO> hostsByNode = new ArrayList<>();
            for (BizTopoNode node : nodeList) {
                hostsByNode.addAll(bizTopoHostService.listHostByNode(bizId, node));
            }
            log.debug("hostsByNode={}", hostsByNode);
            allHostList.addAll(hostsByNode);
        }
        List<String> dynamicGroupIdList = agentStatisticsReq.getDynamicGroupIds();
        if (CollectionUtils.isNotEmpty(dynamicGroupIdList)) {
            List<ApplicationHostDTO> hostsByDynamicGroup = new ArrayList<>();
            for (String id : dynamicGroupIdList) {
                hostsByDynamicGroup.addAll(bizDynamicGroupHostService.listHostByDynamicGroup(appResourceScope, id));
            }
            log.debug("hostsByDynamicGroup={}", hostsByDynamicGroup);
            allHostList.addAll(hostsByDynamicGroup);
        }
        AgentStatistics agentStatistics = agentStatusService.calcAgentStatistics(allHostList);
        return Response.buildSuccessResp(agentStatistics);
    }

    @Override
    public Response<List<DynamicGroupBasicVO>> listAllDynamicGroups(String username,
                                                                    AppResourceScope appResourceScope,
                                                                    String scopeType,
                                                                    String scopeId) {
        return listDynamicGroups(username, appResourceScope, scopeType, scopeId, null);
    }
}
