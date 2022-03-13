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
import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.iam.dto.AppIdResult;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostInfoDTO;
import com.tencent.bk.job.common.model.dto.DynamicGroupInfoDTO;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.common.model.vo.TargetNodeVO;
import com.tencent.bk.job.common.util.CompareUtil;
import com.tencent.bk.job.common.util.PageUtil;
import com.tencent.bk.job.manage.api.web.WebAppResource;
import com.tencent.bk.job.manage.common.TopologyHelper;
import com.tencent.bk.job.manage.model.dto.ApplicationFavorDTO;
import com.tencent.bk.job.manage.model.web.request.AgentStatisticsReq;
import com.tencent.bk.job.manage.model.web.request.IpCheckReq;
import com.tencent.bk.job.manage.model.web.request.ipchooser.AppTopologyTreeNode;
import com.tencent.bk.job.manage.model.web.request.ipchooser.ListHostByBizTopologyNodesReq;
import com.tencent.bk.job.manage.model.web.vo.AppVO;
import com.tencent.bk.job.manage.model.web.vo.CcTopologyNodeVO;
import com.tencent.bk.job.manage.model.web.vo.DynamicGroupInfoVO;
import com.tencent.bk.job.manage.model.web.vo.NodeInfoVO;
import com.tencent.bk.job.manage.model.web.vo.PageDataWithAvailableIdList;
import com.tencent.bk.job.manage.model.web.vo.index.AgentStatistics;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.HostService;
import com.tencent.bk.job.manage.service.impl.ApplicationFavorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class WebAppResourceImpl implements WebAppResource {

    private final ApplicationService applicationService;
    private final ApplicationFavorService applicationFavorService;
    private final AppAuthService appAuthService;
    private final HostService hostService;

    @Autowired
    public WebAppResourceImpl(ApplicationService applicationService,
                              ApplicationFavorService applicationFavorService,
                              AppAuthService appAuthService,
                              HostService hostService) {
        this.applicationService = applicationService;
        this.applicationFavorService = applicationFavorService;
        this.hostService = hostService;
        this.appAuthService = appAuthService;
    }

    @Override
    public Response<PageDataWithAvailableIdList<AppVO, Long>> listAppWithFavor(String username,
                                                                               Integer start,
                                                                               Integer pageSize) {
        List<ApplicationDTO> appList = applicationService.listAllApps();
        List<ApplicationDTO> normalAppList =
            appList.parallelStream().filter(it -> it.getAppType() == AppTypeEnum.NORMAL).collect(Collectors.toList());
        appList.removeAll(normalAppList);
        // 业务集/全业务根据运维角色鉴权
        List<ApplicationDTO> specialAppList =
            appList.parallelStream().filter(it -> it.getMaintainers().contains(username)).collect(Collectors.toList());
        AppIdResult appIdResult = appAuthService.getAppIdList(username,
            normalAppList.parallelStream().map(ApplicationDTO::getId).collect(Collectors.toList()));
        List<AppVO> finalAppList = new ArrayList<>();
        // 可用的普通业务Id
        List<Long> authorizedAppIds = appIdResult.getAppId();
        // 所有可用的AppId(含业务集、全业务Id)
        List<Long> availableAppIds = new ArrayList<>(authorizedAppIds);
        if (appIdResult.getAny()) {
            for (ApplicationDTO normalApp : normalAppList) {
                AppVO appVO = new AppVO(normalApp.getId(), normalApp.getScope().getType().getValue(),
                    normalApp.getScope().getId(), normalApp.getName(), normalApp.getAppType().getValue(),
                    true, null, null);
                finalAppList.add(appVO);
                availableAppIds.add(normalApp.getId());
            }
        } else {
            // 普通业务根据权限中心结果鉴权
            for (ApplicationDTO normalApp : normalAppList) {
                AppVO appVO = new AppVO(normalApp.getId(), normalApp.getScope().getType().getValue(),
                    normalApp.getScope().getId(), normalApp.getName(), normalApp.getAppType().getValue(),
                    true, null, null);
                if (authorizedAppIds.contains(normalApp.getId())) {
                    appVO.setHasPermission(true);
                    finalAppList.add(appVO);
                } else {
                    appVO.setHasPermission(false);
                    finalAppList.add(appVO);
                }
            }
        }
        for (ApplicationDTO specialApp : specialAppList) {
            AppVO appVO = new AppVO(specialApp.getId(), specialApp.getScope().getType().getValue(),
                specialApp.getScope().getId(), specialApp.getName(), specialApp.getAppType().getValue(),
                true, null, null);
            finalAppList.add(appVO);
            availableAppIds.add(specialApp.getId());
        }
        // 收藏标识刷新
        List<ApplicationFavorDTO> applicationFavorDTOList = applicationFavorService.getAppFavorListByUsername(username);
        Map<Long, Long> appIdFavorTimeMap = new HashMap<>();
        for (ApplicationFavorDTO applicationFavorDTO : applicationFavorDTOList) {
            appIdFavorTimeMap.put(applicationFavorDTO.getAppId(), applicationFavorDTO.getFavorTime());
        }
        for (AppVO appVO : finalAppList) {
            if (appIdFavorTimeMap.containsKey(appVO.getId())) {
                appVO.setFavor(true);
                appVO.setFavorTime(appIdFavorTimeMap.get(appVO.getId()));
            } else {
                appVO.setFavor(false);
                appVO.setFavorTime(null);
            }
        }
        // 排序：有无权限、是否收藏、收藏时间倒序
        finalAppList.sort((o1, o2) -> {
            int result = o2.getHasPermission().compareTo(o1.getHasPermission());
            if (result != 0) {
                return result;
            } else {
                result = CompareUtil.safeCompareNullFront(o2.getFavor(), o1.getFavor());
            }
            if (result != 0) {
                return result;
            } else {
                return CompareUtil.safeCompareNullFront(o2.getFavorTime(), o1.getFavorTime());
            }
        });
        // 分页
        PageData<AppVO> pageData = PageUtil.pageInMem(finalAppList, start, pageSize);
        PageDataWithAvailableIdList<AppVO, Long> pageDataWithAvailableIdList =
            new PageDataWithAvailableIdList<>(pageData, availableAppIds);
        return Response.buildSuccessResp(pageDataWithAvailableIdList);
    }

    @Override
    public Response<Integer> favorApp(String username,
                                      AppResourceScope appResourceScope,
                                      String scopeType,
                                      String scopeId) {
        return Response.buildSuccessResp(applicationFavorService.favorApp(username, appResourceScope.getAppId()));
    }

    @Override
    public Response<Integer> cancelFavorApp(String username,
                                            AppResourceScope appResourceScope,
                                            String scopeType,
                                            String scopeId) {
        return Response.buildSuccessResp(applicationFavorService.cancelFavorApp(username, appResourceScope.getAppId()));
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
        ApplicationHostInfoDTO applicationHostInfoCondition = new ApplicationHostInfoDTO();
        applicationHostInfoCondition.setAppId(appResourceScope.getAppId());
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

        PageData<ApplicationHostInfoDTO> appHostInfoPageData =
            hostService.listAppHost(applicationHostInfoCondition, baseSearchCondition);
        PageData<HostInfoVO> finalHostInfoPageData = new PageData<>();
        finalHostInfoPageData.setTotal(appHostInfoPageData.getTotal());
        finalHostInfoPageData.setStart(appHostInfoPageData.getStart());
        finalHostInfoPageData.setPageSize(appHostInfoPageData.getPageSize());
        finalHostInfoPageData
            .setData(appHostInfoPageData.getData().stream()
                .map(TopologyHelper::convertToHostInfoVO).collect(Collectors.toList()));
        return Response.buildSuccessResp(finalHostInfoPageData);
    }

    @Override
    public Response<CcTopologyNodeVO> listAppTopologyTree(String username,
                                                          AppResourceScope appResourceScope,
                                                          String scopeType,
                                                          String scopeId) {
        return Response.buildSuccessResp(hostService.listAppTopologyTree(username, appResourceScope.getAppId()));
    }

    @Override
    public Response<CcTopologyNodeVO> listAppTopologyHostTree(String username,
                                                              AppResourceScope appResourceScope,
                                                              String scopeType,
                                                              String scopeId) {
        return Response.buildSuccessResp(hostService.listAppTopologyHostTree(username, appResourceScope.getAppId()));
    }

    @Override
    public Response<CcTopologyNodeVO> listAppTopologyHostCountTree(String username,
                                                                   AppResourceScope appResourceScope,
                                                                   String scopeType,
                                                                   String scopeId) {
        return Response.buildSuccessResp(hostService.listAppTopologyHostCountTree(username,
            appResourceScope.getAppId()));
    }

    @Override
    public Response<PageData<HostInfoVO>> listHostByBizTopologyNodes(String username,
                                                                     AppResourceScope appResourceScope,
                                                                     String scopeType,
                                                                     String scopeId,
                                                                     ListHostByBizTopologyNodesReq req) {
        return Response.buildSuccessResp(hostService.listHostByBizTopologyNodes(username, appResourceScope.getAppId()
            , req));
    }

    @Override
    public Response<PageData<String>> listIpByBizTopologyNodes(String username,
                                                               AppResourceScope appResourceScope,
                                                               String scopeType,
                                                               String scopeId,
                                                               ListHostByBizTopologyNodesReq req) {
        return Response.buildSuccessResp(hostService.listIPByBizTopologyNodes(username, appResourceScope.getAppId(),
            req));
    }

    @Override
    public Response<List<AppTopologyTreeNode>> getNodeDetail(String username,
                                                             AppResourceScope appResourceScope,
                                                             String scopeType,
                                                             String scopeId,
                                                             List<TargetNodeVO> targetNodeVOList) {
        List<AppTopologyTreeNode> treeNodeList = hostService.getAppTopologyTreeNodeDetail(username,
            appResourceScope.getAppId(),
            targetNodeVOList.stream().map(it -> new AppTopologyTreeNode(
                it.getType(),
                "",
                it.getId(),
                "",
                null
            )).collect(Collectors.toList()));
        return Response.buildSuccessResp(treeNodeList);
    }

    @Override
    public Response<List<List<CcTopologyNodeVO>>> queryNodePaths(String username,
                                                                 AppResourceScope appResourceScope,
                                                                 String scopeType,
                                                                 String scopeId,
                                                                 List<TargetNodeVO> targetNodeVOList) {
        List<List<InstanceTopologyDTO>> pathList = hostService.queryNodePaths(username, appResourceScope.getAppId(),
            targetNodeVOList.stream().map(it -> {
                InstanceTopologyDTO instanceTopologyDTO = new InstanceTopologyDTO();
                instanceTopologyDTO.setObjectId(it.getType());
                instanceTopologyDTO.setInstanceId(it.getId());
                return instanceTopologyDTO;
            }).collect(Collectors.toList()));
        List<List<CcTopologyNodeVO>> resultList = new ArrayList<>();
        for (List<InstanceTopologyDTO> instanceTopologyDTOS : pathList) {
            if (instanceTopologyDTOS == null) {
                resultList.add(null);
            } else {
                resultList.add(instanceTopologyDTOS.stream().map(it -> {
                    CcTopologyNodeVO ccTopologyNodeVO = new CcTopologyNodeVO();
                    ccTopologyNodeVO.setObjectId(it.getObjectId());
                    ccTopologyNodeVO.setObjectName(it.getObjectName());
                    ccTopologyNodeVO.setInstanceId(it.getInstanceId());
                    ccTopologyNodeVO.setInstanceName(it.getInstanceName());
                    return ccTopologyNodeVO;
                }).collect(Collectors.toList()));
            }
        }
        return Response.buildSuccessResp(resultList);
    }

    @Override
    public Response<List<NodeInfoVO>> listHostByNode(String username,
                                                     AppResourceScope appResourceScope,
                                                     String scopeType,
                                                     String scopeId,
                                                     List<TargetNodeVO> targetNodeVOList) {
        List<NodeInfoVO> moduleHostInfoList = hostService.getHostsByNode(username, appResourceScope.getAppId(),
            targetNodeVOList.stream().map(it -> new AppTopologyTreeNode(
                it.getType(),
                "",
                it.getId(),
                "",
                null
            )).collect(Collectors.toList()));
        return Response.buildSuccessResp(moduleHostInfoList);
    }

    @Override
    public Response<List<DynamicGroupInfoVO>> listAppDynamicGroup(String username,
                                                                  AppResourceScope appResourceScope,
                                                                  String scopeType,
                                                                  String scopeId) {
        ApplicationDTO applicationDTO = applicationService.getAppByAppId(appResourceScope.getAppId());
        // 业务集动态分组暂不支持
        if (applicationDTO.getAppType() != AppTypeEnum.NORMAL) {
            return Response.buildSuccessResp(new ArrayList<>());
        }
        List<DynamicGroupInfoDTO> dynamicGroupList = hostService.getDynamicGroupList(username,
            appResourceScope.getAppId());
        List<DynamicGroupInfoVO> dynamicGroupInfoList = dynamicGroupList.parallelStream()
            .map(TopologyHelper::convertToDynamicGroupInfoVO)
            .collect(Collectors.toList());
        return Response.buildSuccessResp(dynamicGroupInfoList);
    }

    @Override
    public Response<List<DynamicGroupInfoVO>> listAppDynamicGroupHost(String username,
                                                                      AppResourceScope appResourceScope,
                                                                      String scopeType,
                                                                      String scopeId, List<String> dynamicGroupIds) {
        List<DynamicGroupInfoDTO> dynamicGroupList =
            hostService.getDynamicGroupHostList(username, appResourceScope.getAppId(), dynamicGroupIds);
        List<DynamicGroupInfoVO> dynamicGroupInfoList = dynamicGroupList.parallelStream()
            .map(TopologyHelper::convertToDynamicGroupInfoVO)
            .collect(Collectors.toList());
        return Response.buildSuccessResp(dynamicGroupInfoList);
    }

    @Override
    public Response<List<DynamicGroupInfoVO>> listAppDynamicGroupWithoutHosts(String username,
                                                                              AppResourceScope appResourceScope,
                                                                              String scopeType,
                                                                              String scopeId,
                                                                              List<String> dynamicGroupIds) {
        List<DynamicGroupInfoDTO> dynamicGroupList = hostService.getDynamicGroupList(username,
            appResourceScope.getAppId());
        List<DynamicGroupInfoVO> dynamicGroupInfoList = dynamicGroupList.parallelStream()
            .filter(dynamicGroupInfoDTO -> dynamicGroupIds.contains(dynamicGroupInfoDTO.getId()))
            .map(TopologyHelper::convertToDynamicGroupInfoVO)
            .collect(Collectors.toList());
        return Response.buildSuccessResp(dynamicGroupInfoList);
    }

    @Override
    public Response<List<HostInfoVO>> listHostByIp(String username,
                                                   AppResourceScope appResourceScope,
                                                   String scopeType,
                                                   String scopeId,
                                                   IpCheckReq req) {
        return Response.buildSuccessResp(hostService.getHostsByIp(
            username,
            appResourceScope.getAppId(),
            req.getActionScope(),
            req.getIpList())
        );
    }

    @Override
    public Response<AgentStatistics> agentStatistics(String username,
                                                     AppResourceScope appResourceScope,
                                                     String scopeType,
                                                     String scopeId,
                                                     AgentStatisticsReq agentStatisticsReq) {
        return Response.buildSuccessResp(hostService.getAgentStatistics(username, appResourceScope.getAppId(),
            agentStatisticsReq));
    }
}
