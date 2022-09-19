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
import com.tencent.bk.job.common.cc.service.CloudAreaService;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.exception.NotImplementedException;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.DynamicGroupInfoDTO;
import com.tencent.bk.job.common.model.vo.CloudAreaInfoVO;
import com.tencent.bk.job.common.model.vo.DynamicGroupIdWithMeta;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.common.model.vo.TargetNodeVO;
import com.tencent.bk.job.common.util.PageUtil;
import com.tencent.bk.job.manage.api.web.WebHostResource;
import com.tencent.bk.job.manage.common.TopologyHelper;
import com.tencent.bk.job.manage.model.dto.DynamicGroupDTO;
import com.tencent.bk.job.manage.model.web.request.AgentStatisticsReq;
import com.tencent.bk.job.manage.model.web.request.HostCheckReq;
import com.tencent.bk.job.manage.model.web.request.ipchooser.BizTopoNode;
import com.tencent.bk.job.manage.model.web.request.ipchooser.GetHostAgentStatisticsByDynamicGroupsReq;
import com.tencent.bk.job.manage.model.web.request.ipchooser.GetHostAgentStatisticsByNodesReq;
import com.tencent.bk.job.manage.model.web.request.ipchooser.HostDetailReq;
import com.tencent.bk.job.manage.model.web.request.ipchooser.HostIdWithMeta;
import com.tencent.bk.job.manage.model.web.request.ipchooser.ListDynamicGroupsReq;
import com.tencent.bk.job.manage.model.web.request.ipchooser.ListHostByBizTopologyNodesReq;
import com.tencent.bk.job.manage.model.web.request.ipchooser.ListTopologyHostCountTreesReq;
import com.tencent.bk.job.manage.model.web.request.ipchooser.PageListHostsByDynamicGroupReq;
import com.tencent.bk.job.manage.model.web.request.ipchooser.QueryNodesPathReq;
import com.tencent.bk.job.manage.model.web.vo.CcTopologyNodeVO;
import com.tencent.bk.job.manage.model.web.vo.DynamicGroupBasicVO;
import com.tencent.bk.job.manage.model.web.vo.DynamicGroupInfoVO;
import com.tencent.bk.job.manage.model.web.vo.NodeInfoVO;
import com.tencent.bk.job.manage.model.web.vo.common.AgentStatistics;
import com.tencent.bk.job.manage.model.web.vo.ipchooser.DynamicGroupHostStatisticsVO;
import com.tencent.bk.job.manage.model.web.vo.ipchooser.NodeHostStatisticsVO;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.host.BizTopoHostService;
import com.tencent.bk.job.manage.service.host.HostDetailService;
import com.tencent.bk.job.manage.service.host.HostService;
import com.tencent.bk.job.manage.service.host.ScopeHostService;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class WebHostResourceImpl implements WebHostResource {

    private final ApplicationService applicationService;
    private final HostService hostService;
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
        this.scopeHostService = scopeHostService;
        this.whiteIpAwareScopeHostService = whiteIpAwareScopeHostService;
        this.agentStatusService = agentStatusService;
        this.hostDetailService = hostDetailService;
        this.bizTopoHostService = bizTopoHostService;
        this.bizDynamicGroupService = bizDynamicGroupService;
        this.bizDynamicGroupHostService = bizDynamicGroupHostService;
        this.scopeDynamicGroupHostService = scopeDynamicGroupHostService;
    }

    @Override
    public Response<PageData<HostInfoVO>> listAppHost(String username,
                                                      AppResourceScope appResourceScope,
                                                      String scopeType,
                                                      String scopeId,
                                                      Integer start,
                                                      Integer pageSize,
                                                      Long moduleType,
                                                      String ipCondition) {
        ApplicationHostDTO applicationHostInfoCondition = new ApplicationHostDTO();
        applicationHostInfoCondition.setBizId(appResourceScope.getAppId());
        applicationHostInfoCondition.setIp(ipCondition);
        if (moduleType != null) {
            applicationHostInfoCondition.getModuleType().add(moduleType);
        }

        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        if (start == null || start < 0) {
            start = 0;
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = 10;
        }
        baseSearchCondition.setStart(start);
        baseSearchCondition.setLength(pageSize);

        PageData<ApplicationHostDTO> appHostInfoPageData =
            hostService.listAppHost(applicationHostInfoCondition, baseSearchCondition);
        PageData<HostInfoVO> finalHostInfoPageData = new PageData<>();
        finalHostInfoPageData.setTotal(appHostInfoPageData.getTotal());
        finalHostInfoPageData.setStart(appHostInfoPageData.getStart());
        finalHostInfoPageData.setPageSize(appHostInfoPageData.getPageSize());
        finalHostInfoPageData
            .setData(appHostInfoPageData.getData().stream()
                .filter(Objects::nonNull)
                .map(ApplicationHostDTO::toVO).collect(Collectors.toList()));
        return Response.buildSuccessResp(finalHostInfoPageData);
    }

    @Override
    public Response<CcTopologyNodeVO> listAppTopologyTree(String username,
                                                          AppResourceScope appResourceScope,
                                                          String scopeType,
                                                          String scopeId) {
        return Response.buildSuccessResp(
            hostService.listAppTopologyTree(username, appResourceScope)
        );
    }

    @Override
    public Response<CcTopologyNodeVO> listAppTopologyHostTree(String username,
                                                              AppResourceScope appResourceScope,
                                                              String scopeType,
                                                              String scopeId) {
        return Response.buildSuccessResp(hostService.listAppTopologyHostTree(username, appResourceScope));
    }

    @Override
    public Response<CcTopologyNodeVO> listAppTopologyHostCountTree(String username,
                                                                   AppResourceScope appResourceScope,
                                                                   String scopeType,
                                                                   String scopeId) {
        return Response.buildSuccessResp(hostService.listAppTopologyHostCountTree(username,
            appResourceScope));
    }

    // 标准接口1
    @Override
    public Response<List<CcTopologyNodeVO>> listTopologyHostCountTrees(String username,
                                                                       AppResourceScope appResourceScope,
                                                                       String scopeType,
                                                                       String scopeId,
                                                                       ListTopologyHostCountTreesReq req) {
        return Response.buildSuccessResp(
            Collections.singletonList(
                hostService.listAppTopologyHostCountTree(username, appResourceScope)
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
        hostDetailService.fillDetailForHosts(pageHostList.getData());
        return Response.buildSuccessResp(PageUtil.transferPageData(
            pageHostList,
            ApplicationHostDTO::toVO
        ));
    }

    @Override
    public Response<PageData<String>> listIpByBizTopologyNodes(String username,
                                                               AppResourceScope appResourceScope,
                                                               String scopeType,
                                                               String scopeId,
                                                               ListHostByBizTopologyNodesReq req) {
        return Response.buildSuccessResp(
            hostService.listIPByBizTopologyNodes(
                username, appResourceScope, req
            )
        );
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
            pagePair.getLeft(),
            pagePair.getRight()
        );
        PageData<HostIdWithMeta> finalPageData = PageUtil.transferPageData(
            pageData,
            hostId -> new HostIdWithMeta(hostId, null)
        );
        return Response.buildSuccessResp(finalPageData);
    }

    @Override
    public Response<List<BizTopoNode>> getNodeDetail(String username,
                                                     AppResourceScope appResourceScope,
                                                     String scopeType,
                                                     String scopeId,
                                                     List<TargetNodeVO> targetNodeVOList) {
        List<BizTopoNode> treeNodeList = hostService.getAppTopologyTreeNodeDetail(username,
            appResourceScope,
            targetNodeVOList.stream().map(it -> new BizTopoNode(
                it.getObjectId(),
                "",
                it.getInstanceId(),
                "",
                null
            )).collect(Collectors.toList()));
        return Response.buildSuccessResp(treeNodeList);
    }

    @Override
    public Response<List<List<CcTopologyNodeVO>>> queryNodePath(String username,
                                                                AppResourceScope appResourceScope,
                                                                String scopeType,
                                                                String scopeId,
                                                                List<TargetNodeVO> targetNodeVOList) {
        return queryNodePaths(username, appResourceScope, targetNodeVOList);
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

    @Override
    public Response<List<NodeInfoVO>> listHostByNode(String username,
                                                     AppResourceScope appResourceScope,
                                                     String scopeType,
                                                     String scopeId,
                                                     List<TargetNodeVO> targetNodeVOList) {
        ApplicationDTO appDTO = applicationService.getAppByScope(appResourceScope);
        if (appDTO.isBizSet()) {
            String msg = "topo node of bizset not supported yet";
            throw new NotImplementedException(msg, ErrorCode.NOT_SUPPORT_FEATURE);
        }
        List<NodeInfoVO> moduleHostInfoList = hostService.getBizHostsByNode(
            username,
            appDTO.getBizIdIfBizApp(),
            targetNodeVOList.stream().map(it -> new BizTopoNode(
                it.getObjectId(),
                "",
                it.getInstanceId(),
                "",
                null
            )).collect(Collectors.toList())
        );
        return Response.buildSuccessResp(moduleHostInfoList);
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
        List<DynamicGroupBasicVO> dynamicGroupInfoList = dynamicGroupList.parallelStream()
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
                Long.parseLong(scopeId),
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
        PageData<ApplicationHostDTO> pageData = bizDynamicGroupHostService.pageHostByDynamicGroup(
            Long.parseLong(scopeId),
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

    @Override
    public Response<List<DynamicGroupInfoVO>> listAppDynamicGroupHost(String username,
                                                                      AppResourceScope appResourceScope,
                                                                      String scopeType,
                                                                      String scopeId, List<String> dynamicGroupIds) {
        // 目前只有业务支持动态分组
        if (appResourceScope.getType() == ResourceScopeTypeEnum.BIZ) {
            List<DynamicGroupInfoDTO> dynamicGroupList =
                hostService.getBizDynamicGroupHostList(
                    username, Long.parseLong(appResourceScope.getId()), dynamicGroupIds
                );
            List<DynamicGroupInfoVO> dynamicGroupInfoList = dynamicGroupList.parallelStream()
                .map(TopologyHelper::convertToDynamicGroupInfoVO)
                .collect(Collectors.toList());
            return Response.buildSuccessResp(dynamicGroupInfoList);
        }
        return Response.buildSuccessResp(Collections.emptyList());
    }

    @Override
    public Response<List<DynamicGroupInfoVO>> listAppDynamicGroupWithoutHosts(String username,
                                                                              AppResourceScope appResourceScope,
                                                                              String scopeType,
                                                                              String scopeId,
                                                                              List<String> dynamicGroupIds) {
        List<DynamicGroupInfoDTO> dynamicGroupList = hostService.getAppDynamicGroupList(
            username, appResourceScope
        );
        List<DynamicGroupInfoVO> dynamicGroupInfoList = dynamicGroupList.parallelStream()
            .filter(dynamicGroupInfoDTO -> dynamicGroupIds.contains(dynamicGroupInfoDTO.getId()))
            .map(TopologyHelper::convertToDynamicGroupInfoVO)
            .collect(Collectors.toList());
        return Response.buildSuccessResp(dynamicGroupInfoList);
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
        List<HostInfoVO> hostList = hostDTOList.parallelStream()
            .map(ApplicationHostDTO::toVO)
            .collect(Collectors.toList());
        // 填充云区域名称
        hostList.forEach(hostInfoVO -> {
            CloudAreaInfoVO cloudAreaInfo = hostInfoVO.getCloudArea();
            if (cloudAreaInfo != null
                && cloudAreaInfo.getId() != null
                && StringUtils.isBlank(cloudAreaInfo.getName())) {
                cloudAreaInfo.setName(CloudAreaService.getCloudAreaNameFromCache(cloudAreaInfo.getId()));
            }
        });
        return Response.buildSuccessResp(hostList);
    }

    private List<ApplicationHostDTO> findHosts(AppResourceScope appResourceScope, HostCheckReq req) {
        List<ApplicationHostDTO> hostDTOList = new ArrayList<>();
        List<Long> hostIdList = req.getHostIdList();
        if (CollectionUtils.isNotEmpty(hostIdList)) {
            // 根据hostId查资源范围及白名单内的主机详情
            hostDTOList.addAll(whiteIpAwareScopeHostService.getScopeHostsIncludingWhiteIPByHostId(
                appResourceScope,
                req.getActionScope(),
                hostIdList
            ));
        }
        List<String> ipList = req.getIpList();
        if (CollectionUtils.isNotEmpty(ipList)) {
            // 根据ip地址查资源范围及白名单内的主机详情
            hostDTOList.addAll(whiteIpAwareScopeHostService.getScopeHostsIncludingWhiteIPByIp(
                appResourceScope,
                req.getActionScope(),
                ipList
            ));
        }
        List<String> ipv6List = req.getIpv6List();
        if (CollectionUtils.isNotEmpty(ipv6List)) {
            // 根据ipv6地址查资源范围及白名单内的主机详情
            hostDTOList.addAll(whiteIpAwareScopeHostService.getScopeHostsIncludingWhiteIPByIpv6(
                appResourceScope,
                req.getActionScope(),
                ipv6List
            ));
        }
        List<String> keyList = req.getKeyList();
        if (CollectionUtils.isNotEmpty(keyList)) {
            // 根据关键字（主机名称）查资源范围及白名单内的主机详情
            hostDTOList.addAll(whiteIpAwareScopeHostService.getScopeHostsIncludingWhiteIPByKey(
                appResourceScope,
                req.getActionScope(),
                keyList
            ));
        }
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

    // 标准接口10
    @Override
    public Response<List<HostInfoVO>> getHostDetails(String username,
                                                     AppResourceScope appResourceScope,
                                                     String scopeType,
                                                     String scopeId,
                                                     HostDetailReq req) {
        Collection<Long> hostIds = req.getHostList().parallelStream()
            .filter(hostIdWithMeta -> hostIdWithMeta.getHostId() != null)
            .map(HostIdWithMeta::getHostId)
            .collect(Collectors.toList());
        List<ApplicationHostDTO> hostList = hostDetailService.listHostDetails(appResourceScope, hostIds);
        List<HostInfoVO> hostInfoVOList = hostList.parallelStream()
            .map(ApplicationHostDTO::toVO)
            .collect(Collectors.toList());
        return Response.buildSuccessResp(hostInfoVOList);
    }

    @Override
    public Response<AgentStatistics> agentStatistics(String username,
                                                     AppResourceScope appResourceScope,
                                                     String scopeType,
                                                     String scopeId,
                                                     AgentStatisticsReq agentStatisticsReq) {
        List<ApplicationHostDTO> allHostList = new ArrayList<>();
        List<String> ipList = agentStatisticsReq.getIpList();
        if (CollectionUtils.isNotEmpty(ipList)) {
            List<ApplicationHostDTO> hostsByIp = scopeHostService.getScopeHostsByIps(
                appResourceScope,
                ipList
            );
            log.debug("hostsByIp={}", hostsByIp);
            allHostList.addAll(hostsByIp);
        }
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
            long bizId = Long.parseLong(scopeId);
            List<ApplicationHostDTO> hostsByDynamicGroup = new ArrayList<>();
            for (String id : dynamicGroupIdList) {
                hostsByDynamicGroup.addAll(bizDynamicGroupHostService.listHostByDynamicGroup(bizId, id));
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
