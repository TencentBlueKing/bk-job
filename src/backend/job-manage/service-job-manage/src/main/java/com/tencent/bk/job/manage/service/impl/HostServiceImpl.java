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

package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.job.common.cc.model.CcCloudAreaInfoDTO;
import com.tencent.bk.job.common.cc.model.CcDynamicGroupDTO;
import com.tencent.bk.job.common.cc.model.CcGroupHostPropDTO;
import com.tencent.bk.job.common.cc.model.CcInstanceDTO;
import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.cc.sdk.CmdbClientFactory;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.cc.service.CloudAreaService;
import com.tencent.bk.job.common.cc.util.TopologyUtil;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.gse.service.QueryAgentStatusClient;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.DynamicGroupWithHost;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.model.vo.CloudAreaInfoVO;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.common.util.ConcurrencyUtil;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.PageUtil;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.common.TopologyHelper;
import com.tencent.bk.job.manage.common.consts.whiteip.ActionScopeEnum;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.manager.host.HostCache;
import com.tencent.bk.job.manage.model.db.CacheHostDO;
import com.tencent.bk.job.manage.model.dto.HostTopoDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.CloudIPDTO;
import com.tencent.bk.job.manage.model.web.request.AgentStatisticsReq;
import com.tencent.bk.job.manage.model.web.request.ipchooser.AppTopologyTreeNode;
import com.tencent.bk.job.manage.model.web.request.ipchooser.ListHostByBizTopologyNodesReq;
import com.tencent.bk.job.manage.model.web.vo.CcTopologyNodeVO;
import com.tencent.bk.job.manage.model.web.vo.NodeInfoVO;
import com.tencent.bk.job.manage.model.web.vo.index.AgentStatistics;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.HostService;
import com.tencent.bk.job.manage.service.WhiteIPService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HostServiceImpl implements HostService {
    private final DSLContext dslContext;
    private final ApplicationHostDAO applicationHostDAO;
    private final ApplicationService applicationService;
    private final HostTopoDAO hostTopoDAO;
    private final TopologyHelper topologyHelper;
    private final CloudAreaService cloudAreaService;
    private final QueryAgentStatusClient queryAgentStatusClient;
    private final WhiteIPService whiteIPService;
    private final HostCache hostCache;
    private final MessageI18nService i18nService;

    @Autowired
    public HostServiceImpl(DSLContext dslContext,
                           ApplicationHostDAO applicationHostDAO,
                           ApplicationService applicationService,
                           HostTopoDAO hostTopoDAO,
                           TopologyHelper topologyHelper,
                           CloudAreaService cloudAreaService,
                           QueryAgentStatusClient queryAgentStatusClient,
                           WhiteIPService whiteIPService,
                           HostCache hostCache,
                           MessageI18nService i18nService) {
        this.dslContext = dslContext;
        this.applicationHostDAO = applicationHostDAO;
        this.applicationService = applicationService;
        this.hostTopoDAO = hostTopoDAO;
        this.topologyHelper = topologyHelper;
        this.cloudAreaService = cloudAreaService;
        this.queryAgentStatusClient = queryAgentStatusClient;
        this.whiteIPService = whiteIPService;
        this.hostCache = hostCache;
        this.i18nService = i18nService;
    }

    @Override
    public boolean existHost(long bizId, String ip) {
        return applicationHostDAO.existsHost(dslContext, bizId, ip);
    }

    @Override
    public List<ApplicationHostDTO> getHostsByAppId(Long appId) {
        ApplicationDTO applicationDTO = applicationService.getAppByAppId(appId);
        ResourceScope scope = applicationDTO.getScope();
        if (scope.getType() == ResourceScopeTypeEnum.BIZ) {
            return applicationHostDAO.listHostInfoByBizId(Long.parseLong(scope.getId()));
        } else {
            return applicationHostDAO.listHostInfoByBizIds(applicationDTO.getSubBizIds(), null, null);
        }
    }

    private boolean insertOrUpdateOneAppHost(Long bizId, ApplicationHostDTO infoDTO) {
        try {
            applicationHostDAO.insertOrUpdateHost(dslContext, infoDTO);
            hostCache.addOrUpdateHost(infoDTO);
        } catch (Throwable t) {
            log.error(String.format("insertHost fail:bizId=%d,hostInfo=%s", bizId, infoDTO), t);
            return false;
        }
        return true;
    }

    @Override
    public List<Long> insertHostsToBiz(Long bizId, List<ApplicationHostDTO> insertList) {
        StopWatch watch = new StopWatch();
        // 插入主机
        watch.start("insertAppHostInfo");
        List<Long> insertFailHostIds = new ArrayList<>();
        boolean batchInserted = false;
        try {
            //尝试批量插入
            if (!insertList.isEmpty()) {
                applicationHostDAO.batchInsertAppHostInfo(dslContext, insertList);
                insertList.forEach(hostCache::addOrUpdateHost);
            }
            batchInserted = true;
        } catch (Throwable throwable) {
            if (throwable instanceof DataAccessException) {
                String errorMessage = throwable.getMessage();
                if (errorMessage.contains("Duplicate entry") && errorMessage.contains("PRIMARY")) {
                    log.info("Fail to batchInsertAppHostInfo, try to insert one by one");
                } else {
                    log.warn("Fail to batchInsertAppHostInfo, try to insert one by one.", throwable);
                }
            } else {
                log.warn("Fail to batchInsertAppHostInfo, try to insert one by one..", throwable);
            }
            //批量插入失败，尝试逐条插入
            for (ApplicationHostDTO infoDTO : insertList) {
                if (!insertOrUpdateOneAppHost(bizId, infoDTO)) {
                    insertFailHostIds.add(infoDTO.getHostId());
                }
            }
        }
        watch.stop();
        if (!batchInserted) {
            watch.start("log insertAppHostInfo");
            if (!insertFailHostIds.isEmpty()) {
                log.warn(String.format("appId=%s,insertFailHostIds.size=%d,insertFailHostIds=%s",
                    bizId, insertFailHostIds.size(), String.join(",",
                        insertFailHostIds.stream().map(Object::toString).collect(Collectors.toSet()))));
            }
            watch.stop();
        }
        log.debug("Performance:insertHostsToApp:appId={},{}", bizId, watch.prettyPrint());
        return insertFailHostIds;
    }

    @Override
    public List<Long> updateHostsInBiz(Long bizId, List<ApplicationHostDTO> hostInfoList) {
        StopWatch watch = new StopWatch();
        watch.start("updateAppHostInfo");
        // 更新主机
        long updateCount = 0L;
        List<Long> updateHostIds = new ArrayList<>();
        long errorCount = 0L;
        List<Long> errorHostIds = new ArrayList<>();
        long notChangeCount = 0L;
        boolean batchUpdated = false;
        try {
            // 尝试批量更新
            if (!hostInfoList.isEmpty()) {
                applicationHostDAO.batchUpdateBizHostInfoByHostId(dslContext, hostInfoList);
                hostInfoList.forEach(hostCache::addOrUpdateHost);
            }
            batchUpdated = true;
        } catch (Throwable throwable) {
            if (throwable instanceof DataAccessException) {
                String errorMessage = throwable.getMessage();
                if (errorMessage.contains("Duplicate entry") && errorMessage.contains("PRIMARY")) {
                    log.info("Fail to batchUpdateAppHostInfoByHostId, try to update one by one");
                } else {
                    log.warn("Fail to batchUpdateAppHostInfoByHostId, try to update one by one.", throwable);
                }
            } else {
                log.warn("Fail to batchUpdateAppHostInfoByHostId, try to update one by one..", throwable);
            }
            // 批量更新失败，尝试逐条更新
            for (ApplicationHostDTO hostInfoDTO : hostInfoList) {
                try {
                    if (!applicationHostDAO.existAppHostInfoByHostId(dslContext, hostInfoDTO)) {
                        applicationHostDAO.updateBizHostInfoByHostId(dslContext, hostInfoDTO.getBizId(), hostInfoDTO);
                        hostCache.addOrUpdateHost(hostInfoDTO);
                        updateCount += 1;
                        updateHostIds.add(hostInfoDTO.getHostId());
                    } else {
                        notChangeCount += 1;
                    }
                } catch (Throwable t) {
                    log.error(String.format("updateHost fail:appId=%d,hostInfo=%s", bizId, hostInfoDTO), t);
                    errorCount += 1;
                    errorHostIds.add(hostInfoDTO.getHostId());
                }
            }
        }
        watch.stop();
        if (!batchUpdated) {
            watch.start("log updateAppHostInfo");
            log.info("Update host of appId={},errorCount={}," +
                    "updateCount={},notChangeCount={},errorHostIds={},updateHostIds={}",
                bizId, errorCount, updateCount, notChangeCount, errorHostIds, updateHostIds);
            watch.stop();
        }
        log.debug("Performance:updateHostsInApp:appId={},{}", bizId, watch.prettyPrint());
        return errorHostIds;
    }

    @Override
    public List<Long> removeHostsFromBiz(Long bizId, List<ApplicationHostDTO> hostList) {
        List<Long> hostIdList = hostList.stream().map(ApplicationHostDTO::getHostId).collect(Collectors.toList());
        StopWatch watch = new StopWatch();
        watch.start("deleteHostTopoOfBiz");
        List<Long> deleteFailHostIds = new ArrayList<>();
        // 删除业务与主机的关系
        hostTopoDAO.batchDeleteHostTopo(dslContext, bizId, hostIdList);
        watch.stop();
        watch.start("syncHostTopo");
        // 同步主机关系到host表
        hostIdList.forEach(hostId -> applicationHostDAO.syncHostTopo(dslContext, hostId));
        watch.stop();
        log.debug("Performance:removeHostsFromBiz:bizId={},{}", bizId, watch.prettyPrint());
        return deleteFailHostIds;
    }

    @Override
    public long countHostsByOsType(String osType) {
        return applicationHostDAO.countHostsByOsType(osType);
    }

    public static List<String> buildIpList(List<ApplicationHostDTO> hosts) {
        List<String> ipList = new ArrayList<>();
        for (ApplicationHostDTO host : hosts) {
            ipList.add(host.getCloudAreaId() + ":" + host.getIp());
        }
        return ipList;
    }

    public static void fillAgentStatus(Map<String, QueryAgentStatusClient.AgentStatus> agentStatusMap,
                                       List<HostInfoVO> ipListStatus) {
        if (CollectionUtils.isNotEmpty(ipListStatus)) {
            for (HostInfoVO hostInfo : ipListStatus) {
                if (hostInfo != null) {
                    String ip = hostInfo.getCloudAreaInfo().getId() + ":" + hostInfo.getIp();
                    QueryAgentStatusClient.AgentStatus agentStatus = agentStatusMap.get(ip);
                    if (agentStatus != null && agentStatus.status == 1) {
                        hostInfo.setAlive(1);
                    } else {
                        hostInfo.setAlive(0);
                    }
                }
            }
        }
    }

    @Override
    public PageData<ApplicationHostDTO> listAppHost(ApplicationHostDTO applicationHostInfoCondition,
                                                    BaseSearchCondition baseSearchCondition) {
        return applicationHostDAO.listHostInfoByPage(applicationHostInfoCondition, baseSearchCondition);
    }

    @Override
    public CcTopologyNodeVO listAppTopologyTree(String username, AppResourceScope appResourceScope) {
        ApplicationDTO appInfo = applicationService.getAppByAppId(appResourceScope.getAppId());
        if (appInfo == null) {
            throw new InvalidParamException(ErrorCode.WRONG_APP_ID);
        }
        CcTopologyNodeVO ccTopologyNodeVO = new CcTopologyNodeVO();
        ccTopologyNodeVO.setObjectId("biz");
        ccTopologyNodeVO.setObjectName(i18nService.getI18n("cmdb.object.name.biz"));
        ccTopologyNodeVO.setInstanceId(Long.valueOf(appResourceScope.getId()));
        ccTopologyNodeVO.setInstanceName(appInfo.getName());
        if (appInfo.isAllBizSet()) {
            // 全业务
            ccTopologyNodeVO.setCount((int) applicationHostDAO.countAllHosts());
            return ccTopologyNodeVO;
        } else if (appInfo.isBizSet()) {
            // 业务集
            ccTopologyNodeVO.setCount((int) applicationHostDAO.countHostsByBizIds(dslContext,
                topologyHelper.getBizSetSubBizIds(appInfo)));
            return ccTopologyNodeVO;
        }
        InstanceTopologyDTO instanceTopology = topologyHelper.getTopologyTreeByApplication(appInfo);
        return TopologyHelper.convertToCcTopologyTree(instanceTopology);
    }

    /**
     * 入参只需objectId与instanceId即可，其余字段可为空
     *
     * @param username         用户名
     * @param appResourceScope 资源范围
     * @param treeNodeList     拓扑节点列表
     * @return 拓扑节点详情
     */
    @Override
    public List<AppTopologyTreeNode> getAppTopologyTreeNodeDetail(String username,
                                                                  AppResourceScope appResourceScope,
                                                                  List<AppTopologyTreeNode> treeNodeList) {
        log.info(
            "Input(username={},appResourceScope={},treeNodeList={})",
            username, appResourceScope, JsonUtils.toJson(treeNodeList)
        );
        if (treeNodeList == null || treeNodeList.isEmpty()) {
            return Collections.emptyList();
        }
        // 查出业务
        ApplicationDTO appInfo = applicationService.getAppByAppId(appResourceScope.getAppId());
        // 查业务拓扑树
        IBizCmdbClient bizCmdbClient = CmdbClientFactory.getCmdbClient(JobContextUtil.getUserLang());
        InstanceTopologyDTO appTopologyTree = bizCmdbClient.getBizInstTopology(
            Long.parseLong(appResourceScope.getId())
        );
        List<AppTopologyTreeNode> nodeList = ConcurrencyUtil.getResultWithThreads(treeNodeList, 5, treeNode -> {
            CcInstanceDTO ccInstanceDTO = new CcInstanceDTO(treeNode.getObjectId(), treeNode.getInstanceId());
            // 查拓扑节点完整信息
            InstanceTopologyDTO completeNode = TopologyUtil.findNodeFromTopo(appTopologyTree, ccInstanceDTO);
            if (completeNode == null) {
                return Collections.emptyList();
            }
            AppTopologyTreeNode node = new AppTopologyTreeNode();
            node.setObjectId(completeNode.getObjectId());
            node.setInstanceId(completeNode.getInstanceId());
            node.setObjectName(completeNode.getObjectName());
            node.setInstanceName(completeNode.getInstanceName());
            return Collections.singletonList(node);
        });
        // 排序
        Map<String, AppTopologyTreeNode> map = new HashMap<>();
        for (AppTopologyTreeNode appTopologyTreeNode : nodeList) {
            map.put(appTopologyTreeNode.getObjectId() + "_" + appTopologyTreeNode.getInstanceId(), appTopologyTreeNode);
        }
        List<AppTopologyTreeNode> orderedList = new ArrayList<>();
        for (AppTopologyTreeNode appTopologyTreeNode : treeNodeList) {
            String key = appTopologyTreeNode.getObjectId() + "_" + appTopologyTreeNode.getInstanceId();
            if (map.containsKey(key)) {
                orderedList.add(map.get(key));
            }
        }
        return orderedList;
    }

    @Override
    public List<List<InstanceTopologyDTO>> queryBizNodePaths(String username,
                                                             Long bizId,
                                                             List<InstanceTopologyDTO> nodeList) {
        // 查业务拓扑树
        IBizCmdbClient bizCmdbClient = CmdbClientFactory.getCmdbClient(JobContextUtil.getUserLang());
        InstanceTopologyDTO appTopologyTree = bizCmdbClient.getBizInstTopology(bizId);
        // 搜索路径
        return TopologyHelper.findTopoPaths(appTopologyTree, nodeList);
    }

    /**
     * 入参只需objectId与instanceId即可，其余字段可为空
     *
     * @param username     用户名
     * @param bizId        业务ID
     * @param treeNodeList 拓扑节点列表
     * @return 节点详情
     */
    @Override
    public List<NodeInfoVO> getBizHostsByNode(String username,
                                              Long bizId,
                                              List<AppTopologyTreeNode> treeNodeList) {
        log.info(
            "Input(username={},bizId={},treeNodeList={})",
            username, bizId, JsonUtils.toJson(treeNodeList)
        );
        List<NodeInfoVO> nodeHostInfoList = new ArrayList<>();
        if (treeNodeList == null || treeNodeList.isEmpty()) {
            return nodeHostInfoList;
        }
        // 查业务拓扑树
        IBizCmdbClient bizCmdbClient = CmdbClientFactory.getCmdbClient(JobContextUtil.getUserLang());
        InstanceTopologyDTO appTopologyTree = bizCmdbClient.getBizInstTopology(bizId);
        final List<String> allIpWithCloudIdList = Collections.synchronizedList(new ArrayList<>());
        nodeHostInfoList = ConcurrencyUtil.getResultWithThreads(treeNodeList, 5, treeNode -> {
            CcInstanceDTO ccInstanceDTO = new CcInstanceDTO(treeNode.getObjectId(), treeNode.getInstanceId());
            NodeInfoVO nodeInfoVO = new NodeInfoVO();
            // 查拓扑节点完整信息
            InstanceTopologyDTO completeNode = TopologyUtil.findNodeFromTopo(appTopologyTree, ccInstanceDTO);
            // 节点在拓扑树中不存在，可能未同步或被删除
            if (completeNode == null) {
                return Collections.emptyList();
            }
            // 构造条件查主机
            List<CcInstanceDTO> conditions = new ArrayList<>();
            conditions.add(ccInstanceDTO);
            List<ApplicationHostDTO> hosts = bizCmdbClient.getHosts(bizId, conditions);
            List<HostInfoVO> hostInfoVOList = hosts.stream().map(it -> {
                HostInfoVO hostInfoVO = new HostInfoVO();
                hostInfoVO.setHostId(it.getHostId());
                hostInfoVO.setOs(it.getOs());
                hostInfoVO.setIp(it.getIp());
                hostInfoVO.setIpDesc(it.getIpDesc());
                hostInfoVO.setDisplayIp(it.getDisplayIp());
                hostInfoVO.setCloudAreaInfo(new CloudAreaInfoVO(it.getCloudAreaId(),
                    CloudAreaService.getCloudAreaNameFromCache(it.getCloudAreaId())));
                return hostInfoVO;
            }).collect(Collectors.toList());
            // 收集涉及的IP列表
            List<String> ipWithCloudIdList = buildIpList(hosts);
            allIpWithCloudIdList.addAll(ipWithCloudIdList);
            // 构造最终数据
            nodeInfoVO.setNodeType(completeNode.getObjectId());
            nodeInfoVO.setId(completeNode.getInstanceId());
            nodeInfoVO.setName(completeNode.getInstanceName());
            nodeInfoVO.setIpList(ipWithCloudIdList);
            nodeInfoVO.setIpListStatus(hostInfoVOList);
            return Collections.singletonList(nodeInfoVO);
        });
        // 查出节点下主机与Agent状态
        Map<String, QueryAgentStatusClient.AgentStatus> agentStatusMap =
            queryAgentStatusClient.batchGetAgentStatus(allIpWithCloudIdList);
        // 批量设置agent状态
        for (NodeInfoVO nodeInfoVO : nodeHostInfoList) {
            fillAgentStatus(agentStatusMap, nodeInfoVO.getIpListStatus());
        }
        return nodeHostInfoList;
    }

    @Override
    public List<DynamicGroupWithHost> getAppDynamicGroupList(String username,
                                                             AppResourceScope appResourceScope) {
        ApplicationDTO applicationInfo = applicationService.getAppByAppId(appResourceScope.getAppId());

        Map<String, DynamicGroupWithHost> ccGroupInfoMap = new HashMap<>();
        Map<Long, List<String>> appId2GroupIdMap = new HashMap<>();
        if (ResourceScopeTypeEnum.BIZ_SET == appResourceScope.getType()) {
            for (long subAppId : applicationInfo.getSubBizIds()) {
                getCustomGroupListByBizId(subAppId, ccGroupInfoMap, appId2GroupIdMap);
            }
        } else {
            getCustomGroupListByBizId(
                Long.parseLong(appResourceScope.getId()),
                ccGroupInfoMap,
                appId2GroupIdMap
            );
        }

        fillAppInfo(ccGroupInfoMap);

        return new ArrayList<>(ccGroupInfoMap.values());
    }

    @Override
    public List<DynamicGroupWithHost> getBizDynamicGroupHostList(String username,
                                                                 Long bizId,
                                                                 List<String> dynamicGroupIdList) {
        Set<String> dynamicGroupIdSet = new HashSet<>(dynamicGroupIdList);
        IBizCmdbClient cmdbClient = CmdbClientFactory.getCmdbClient(JobContextUtil.getUserLang());
        List<CcDynamicGroupDTO> dynamicGroupList = cmdbClient.getDynamicGroupList(bizId);
        List<DynamicGroupWithHost> dynamicGroupWithHostList = dynamicGroupList.stream()
            .filter(dynamicGroup -> dynamicGroupIdSet.contains(dynamicGroup.getId()))
            .map(CcDynamicGroupDTO::toDynamicGroupWithHost)
            .collect(Collectors.toList());

        // 查询业务数据
        ResourceScope resourceScope = new ResourceScope(ResourceScopeTypeEnum.BIZ, bizId.toString());
        ApplicationDTO appInfo = applicationService.getAppByScope(resourceScope);

        dynamicGroupWithHostList.forEach(dynamicGroupWithHost -> {
            // 填充业务信息
            fillAppInfo(appInfo, dynamicGroupWithHost);
            // 填充主机IP信息
            List<CcGroupHostPropDTO> ccGroupHostProps =
                cmdbClient.getDynamicGroupIp(bizId, dynamicGroupWithHost.getId());
            List<String> cloudIpList = new ArrayList<>();
            for (CcGroupHostPropDTO groupHost : ccGroupHostProps) {
                if (CollectionUtils.isNotEmpty(groupHost.getCloudIdList())) {
                    cloudIpList.add(groupHost.getCloudIdList().get(0).getInstanceId() + ":" + groupHost.getIp());
                } else {
                    log.warn("Wrong host info! No cloud area!|{}", groupHost);
                }
            }
            dynamicGroupWithHost.setIpList(cloudIpList);
            // 填充主机详情
            List<IpDTO> hostIps = buildIpDTOList(cloudIpList);
            List<ApplicationHostDTO> hostDetailList = listHosts(hostIps);
            if (hostDetailList.size() < cloudIpList.size()) {
                Set<String> existCloudIps = new HashSet<>(buildIpList(hostDetailList));
                Set<String> cloudIpSet = new HashSet<>(cloudIpList);
                cloudIpSet.removeAll(existCloudIps);
                if (!cloudIpSet.isEmpty()) {
                    log.warn("Cannot find hostDetail for {} ips: {}", cloudIpSet.size(), cloudIpSet);
                }
            }
            // 填充Agent状态信息
            fillAgentStatus(hostDetailList);
            dynamicGroupWithHost.setIpListStatus(hostDetailList);
        });
        return dynamicGroupWithHostList;
    }

    private void fillAppInfo(ApplicationDTO appInfo, DynamicGroupWithHost dynamicGroupWithHost) {
        if (appInfo != null) {
            dynamicGroupWithHost.setBizName(appInfo.getName());
            dynamicGroupWithHost.setOwner(appInfo.getBkSupplierAccount());
            dynamicGroupWithHost.setOwnerName(appInfo.getBkSupplierAccount());
        } else {
            log.warn("appInfo is null");
        }
    }

    private List<IpDTO> buildIpDTOList(List<String> cloudIpList) {
        if (CollectionUtils.isEmpty(cloudIpList)) {
            return Collections.emptyList();
        }
        return cloudIpList.stream().map(IpDTO::fromCloudAreaIdAndIpStr).collect(Collectors.toList());
    }

    @Override
    public CcTopologyNodeVO listAppTopologyHostTree(String username, AppResourceScope appResourceScope) {
        StopWatch watch = new StopWatch("listAppTopologyHostTree");
        ApplicationDTO appInfo = applicationService.getAppByAppId(appResourceScope.getAppId());
        watch.start("listAppTopologyTree");
        CcTopologyNodeVO topologyTree = this.listAppTopologyTree(username, appResourceScope);
        watch.stop();
        if (appResourceScope.getType() == ResourceScopeTypeEnum.BIZ) {
            watch.start("fillHostInfo");
            fillHostInfo(username, Long.valueOf(appResourceScope.getId()), topologyTree, true);
            watch.stop();
        } else {
            topologyTree.setIpListStatus(
                listHostByAppTopologyNodes(
                    username, appResourceScope.getAppId(), Collections.singletonList(
                        new AppTopologyTreeNode(
                            "biz",
                            "biz",
                            Long.parseLong(appResourceScope.getId()),
                            appInfo.getName(),
                            null
                        )
                    )
                )
            );
        }
        log.debug(watch.toString());
        return topologyTree;
    }

    /**
     * 建立moduleId与拓扑树module子节点映射Map
     * 顺便初始化IpListStatus
     */
    private void constructMap(Map<Long, CcTopologyNodeVO> map, CcTopologyNodeVO topologyTree) {
        if (topologyTree == null) return;
        if (topologyTree.getObjectId().equals("module")) {
            topologyTree.setIpListStatus(new ArrayList<>());
            map.put(topologyTree.getInstanceId(), topologyTree);
        } else {
            List<CcTopologyNodeVO> childs = topologyTree.getChild();
            if (childs != null && childs.size() > 0) {
                childs.forEach(child -> constructMap(map, child));
            }
        }
    }

    // 统计拓扑树各节点主机数量
    public void countHosts(CcTopologyNodeVO topologyTree) {
        List<CcTopologyNodeVO> childs = topologyTree.getChild();
        topologyTree.setHostIdSet(new HashSet<>());
        if (childs != null && !childs.isEmpty()) {
            childs.forEach(child -> {
                countHosts(child);
                topologyTree.getHostIdSet().addAll(child.getHostIdSet());
            });
            topologyTree.setCount(topologyTree.getHostIdSet().size());
        } else {
            List<HostInfoVO> hosts = topologyTree.getIpListStatus();
            if (hosts != null) {
                topologyTree.getHostIdSet().addAll(
                    hosts.parallelStream().map(HostInfoVO::getHostId).collect(Collectors.toSet()));
                topologyTree.setCount(topologyTree.getHostIdSet().size());
            } else {
                topologyTree.setCount(0);
            }
        }
    }

    public void clearHosts(CcTopologyNodeVO topologyTree) {
        List<CcTopologyNodeVO> childs = topologyTree.getChild();
        topologyTree.setHostIdSet(null);
        if (childs != null && !childs.isEmpty()) {
            childs.forEach(this::clearHosts);
            topologyTree.setIpList(null);
            topologyTree.setIpListStatus(null);
        } else {
            topologyTree.setIpList(null);
            topologyTree.setIpListStatus(null);
        }
    }

    public void fillHostInfo(String username, Long bizId, CcTopologyNodeVO topologyTree, boolean updateAgentStatus) {
        if (topologyTree == null) {
            return;
        }
        StopWatch watch = new StopWatch("fillHostInfo");
        watch.start("constructMap:" + topologyTree.getInstanceId());
        Map<Long, CcTopologyNodeVO> map = new HashMap<>();
        constructMap(map, topologyTree);
        log.info("{} modules mapped", map.keySet().size());
        if (map.keySet().size() < 200) {
            log.info("module ids:{}", map.keySet());
        } else {
            log.info("more than 200 module ids, do not print");
        }
        watch.stop();
        watch.start("getHosts of Module:" + topologyTree.getInstanceId());
        // 从DB拿主机
        List<ApplicationHostDTO> dbHosts = applicationHostDAO.listHostInfoByBizId(bizId);
        log.info("find {} hosts from DB", dbHosts.size());
        watch.stop();
        List<HostInfoVO> hostInfoVOList = dbHosts.stream().map(it -> {
            HostInfoVO hostInfoVO = new HostInfoVO();
            hostInfoVO.setHostId(it.getHostId());
            hostInfoVO.setOs(it.getOs());
            hostInfoVO.setIp(it.getIp());
            hostInfoVO.setIpDesc(it.getIpDesc());
            hostInfoVO.setDisplayIp(it.getDisplayIp());
            hostInfoVO.setCloudAreaInfo(new CloudAreaInfoVO(it.getCloudAreaId(),
                CloudAreaService.getCloudAreaNameFromCache(it.getCloudAreaId())));
            return hostInfoVO;
        }).collect(Collectors.toList());
        //批量设置agent状态
        if (updateAgentStatus) {
            watch.start("batchGetAgentStatus");
            Map<String, QueryAgentStatusClient.AgentStatus> agentStatusMap =
                queryAgentStatusClient.batchGetAgentStatus(buildIpList(dbHosts));
            fillAgentStatus(agentStatusMap, hostInfoVOList);
            watch.stop();
        }
        //将主机挂载到topo树
        watch.start("setToTopoTree");
        for (int i = 0; i < hostInfoVOList.size(); i++) {
            ApplicationHostDTO host = dbHosts.get(i);
            HostInfoVO hostInfoVO = hostInfoVOList.get(i);
            host.getModuleId().forEach(moduleId -> {
                CcTopologyNodeVO moduleNode = map.get(moduleId);
                if (moduleNode == null) {
                    log.warn("cannot find moduleNode in topoTree, cache may expire, ignore this moduleNode");
                } else {
                    moduleNode.getIpListStatus().add(hostInfoVO);
                }
            });
        }
        watch.stop();
        watch.start("countHosts");
        countHosts(topologyTree);
        watch.stop();
        log.debug(watch.toString());
    }

    public void fillAgentStatus(List<ApplicationHostDTO> hosts) {
        // 查出节点下主机与Agent状态
        List<String> ipWithCloudIdList = buildIpList(hosts);
        // 批量设置agent状态
        Map<String, QueryAgentStatusClient.AgentStatus> agentStatusMap =
            queryAgentStatusClient.batchGetAgentStatus(ipWithCloudIdList);
        if (CollectionUtils.isNotEmpty(hosts)) {
            for (ApplicationHostDTO hostInfoDTO : hosts) {
                if (hostInfoDTO != null) {
                    String ip = hostInfoDTO.getCloudAreaId() + ":" + hostInfoDTO.getIp();
                    QueryAgentStatusClient.AgentStatus agentStatus = agentStatusMap.get(ip);
                    hostInfoDTO.setGseAgentAlive(agentStatus != null && agentStatus.status == 1);
                }
            }
        }
    }

    @Override
    public Boolean existsHost(Long bizId, String ip) {
        return applicationHostDAO.existsHost(dslContext, bizId, ip);
    }

    @Override
    public CcTopologyNodeVO listAppTopologyHostCountTree(String username,
                                                         AppResourceScope appResourceScope) {
        StopWatch watch = new StopWatch("listAppTopologyHostCountTree");
        watch.start("listAppTopologyHostCountTree");
        CcTopologyNodeVO topologyTree = this.listAppTopologyTree(username, appResourceScope);
        watch.stop();
        if (appResourceScope.getType() == ResourceScopeTypeEnum.BIZ) {
            watch.start("fillHostInfo");
            fillHostInfo(username, Long.valueOf(appResourceScope.getId()), topologyTree, false);
            watch.stop();
            watch.start("clearHosts");
            clearHosts(topologyTree);
            watch.stop();
        }
        log.debug(watch.toString());
        return topologyTree;
    }

    private boolean matchSearchContent(String searchContent, String targetStr) {
        if (searchContent == null) {
            return true;
        }
        if (targetStr == null) {
            return false;
        }
        String[] searchContents = searchContent.split("[;,；，\\n\\s|\\u00A0]+");
        for (String content : searchContents) {
            if (StringUtils.isNotBlank(content) && targetStr.toLowerCase().contains(content.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public PageData<String> listIPByBizTopologyNodes(String username,
                                                     AppResourceScope appResourceScope,
                                                     ListHostByBizTopologyNodesReq req) {
        PageData<HostInfoVO> hostInfoVOResult = listHostByAppTopologyNodes(username, appResourceScope, req);
        List<String> data =
            hostInfoVOResult.getData().parallelStream()
                .map(it -> it.getCloudAreaInfo().getId().toString() + ":" + it.getIp())
                .collect(Collectors.toList());
        return new PageData<>(
            hostInfoVOResult.getStart(),
            hostInfoVOResult.getPageSize(),
            hostInfoVOResult.getTotal(),
            data
        );
    }

    public List<HostInfoVO> getHostInfoVOsByHostInfoDTOs(List<ApplicationHostDTO> hosts) {
        return hosts.stream().map(it -> {
            String ip = it.getIp();
            HostInfoVO hostInfoVO = new HostInfoVO();
            hostInfoVO.setHostId(it.getHostId());
            hostInfoVO.setOs(it.getOs());
            hostInfoVO.setIp(ip);
            hostInfoVO.setIpDesc(it.getIpDesc());
            hostInfoVO.setDisplayIp(it.getDisplayIp());
            hostInfoVO.setCloudAreaInfo(new CloudAreaInfoVO(it.getCloudAreaId(),
                CloudAreaService.getCloudAreaNameFromCache(it.getCloudAreaId())));
            if (it.getGseAgentAlive()) {
                hostInfoVO.setAlive(1);
            } else {
                hostInfoVO.setAlive(0);
            }
            return hostInfoVO;
        }).collect(Collectors.toList());
    }

    // DB分页
    @Override
    public PageData<HostInfoVO> listHostByAppTopologyNodes(String username,
                                                           AppResourceScope appResourceScope,
                                                           ListHostByBizTopologyNodesReq req) {
        StopWatch watch = new StopWatch("listHostByBizTopologyNodes");
        watch.start("genConditions");
        String searchContent = req.getSearchContent();
        Integer agentStatus = req.getAgentStatus();
        // 查出业务
        ApplicationDTO appInfo = applicationService.getAppByAppId(appResourceScope.getAppId());
        List<Long> moduleIds = null;
        List<Long> bizIds = null;
        if (appInfo.getScope().getType() == ResourceScopeTypeEnum.BIZ) {
            // 普通业务需要以moduleIds作为查询条件
            moduleIds = getBizModuleIdsByTopoNodes(
                username, Long.valueOf(appInfo.getScope().getId()), req.getAppTopoNodeList()
            );
        } else if (!appInfo.isAllBizSet() && appInfo.isBizSet()) {
            // 业务集：仅根据业务查主机
            // 查出对应的所有普通业务
            bizIds = topologyHelper.getBizSetSubBizIds(appInfo);
        }

        //获取所有云区域，找出名称符合条件的所有CloudAreaId
        List<Long> cloudAreaIds = null;
        if (StringUtils.isNotBlank(searchContent)) {
            cloudAreaIds = new ArrayList<>();
            List<CcCloudAreaInfoDTO> allCloudAreaInfos = cloudAreaService.getCloudAreaList();
            for (CcCloudAreaInfoDTO it : allCloudAreaInfos) {
                if (matchSearchContent(searchContent, it.getName())) {
                    cloudAreaIds.add(it.getId());
                }
            }
            log.debug("filter by cloudAreaIds={}", cloudAreaIds);
        }
        List<String> searchContents = null;
        if (req.getSearchContent() != null) {
            searchContents = Arrays.asList(searchContent.split("[;,；，\\n\\s|\\u00A0]+"));
        }
        watch.stop();
        //分页
        Pair<Long, Long> pagePair = PageUtil.normalizePageParam(req.getStart(), req.getPageSize());
        watch.start("listHostInfoBySearchContents");
        List<ApplicationHostDTO> hosts = applicationHostDAO.listHostInfoBySearchContents(bizIds, moduleIds,
            cloudAreaIds, searchContents, agentStatus, pagePair.getLeft(), pagePair.getRight());
        watch.stop();
        watch.start("countHostInfoBySearchContents");
        Long count = applicationHostDAO.countHostInfoBySearchContents(bizIds, moduleIds, cloudAreaIds, searchContents
            , agentStatus);
        watch.stop();
        watch.start("getHostInfoVOsByHostInfoDTOs");
        List<HostInfoVO> finalHostInfoVOList = getHostInfoVOsByHostInfoDTOs(hosts);
        watch.stop();
        if (watch.getTotalTimeMillis() > 5000) {
            log.info("listHostByBizTopologyNodes is slow:{}", watch);
        }
        return new PageData<>(pagePair.getLeft().intValue(), pagePair.getRight().intValue(), count,
            finalHostInfoVOList);
    }

    private Pair<Set<CloudIPDTO>, Set<String>> parseInputCloudIPList(List<String> checkIpList) {
        Set<CloudIPDTO> inputCloudIPSet = new HashSet<>();
        Set<String> inputIPWithoutCloudIdSet = new HashSet<>();
        Pattern pattern = Pattern.compile("[:：]");
        for (String ip : checkIpList) {
            if (StringUtils.isBlank(ip)) {
                continue;
            }
            if (ip.contains(":") || ip.contains("：")) {
                //有云区域Id
                String[] arr = pattern.split(ip);
                inputCloudIPSet.add(new CloudIPDTO(Long.parseLong(arr[0].trim()), arr[1].trim()));
            } else {
                inputIPWithoutCloudIdSet.add(ip.trim());
            }
        }
        return Pair.of(inputCloudIPSet, inputIPWithoutCloudIdSet);
    }

    private List<String> buildCloudIPList(List<CloudIPDTO> cloudIPDTOList) {
        if (CollectionUtils.isEmpty(cloudIPDTOList)) {
            return Collections.emptyList();
        }
        return cloudIPDTOList.parallelStream().map(CloudIPDTO::getCloudIP).collect(Collectors.toList());
    }

    /**
     * 根据主机所属CMDB业务ID判断主机是否归属于某个Job业务
     *
     * @param appDTO    Job业务信息
     * @param hostBizId 主机CMDB业务ID
     * @return 是否归属于Job业务
     */
    private boolean hostBelongToApp(ApplicationDTO appDTO, Long hostBizId) {
        if (appDTO.isAllBizSet()) {
            return true;
        } else if (appDTO.isBizSet()) {
            return appDTO.getSubBizIds().contains(hostBizId);
        } else {
            return hostBizId.equals(appDTO.getBizIdIfBizApp());
        }
    }

    /**
     * 分离存在于业务下与不存在于业务下的主机
     *
     * @param appId            Job业务ID
     * @param mixedCloudIPList 未分离的所有IP列表
     * @param inAppIPList      归属于Job业务的IP列表
     * @param notInAppIPList   不属于Job业务的IP列表
     */
    private void separateNotInAppIP(Long appId,
                                    List<CloudIPDTO> mixedCloudIPList,
                                    List<CloudIPDTO> inAppIPList,
                                    List<CloudIPDTO> notInAppIPList) {
        ApplicationDTO appDTO = applicationService.getAppByAppId(appId);
        if (appDTO.isAllBizSet()) {
            // 全业务
            inAppIPList.addAll(mixedCloudIPList);
            return;
        }
        List<String> cloudIPList = buildCloudIPList(mixedCloudIPList);
        List<Long> subBizIds;
        if (appDTO.isBizSet()) {
            // 业务集
            subBizIds = appDTO.getSubBizIds();
            log.info("check hosts of appId:{}, subBizIds:{}", appId, StringUtil.concatCollection(subBizIds));
        } else {
            // 普通业务
            subBizIds = Collections.singletonList(appDTO.getBizIdIfBizApp());
        }
        // 首先根据本地DB缓存的主机数据进行分离
        List<ApplicationHostDTO> hostDTOList = applicationHostDAO.listHostInfoByBizAndCloudIPs(
            subBizIds,
            cloudIPList
        );
        Set<String> inAppCloudIPSet = hostDTOList.parallelStream()
            .map(ApplicationHostDTO::getCloudIp)
            .collect(Collectors.toSet());
        List<CloudIPDTO> notInAppIPListByLocal = new ArrayList<>();
        mixedCloudIPList.forEach(cloudIPDTO -> {
            if (inAppCloudIPSet.contains(cloudIPDTO.getCloudIP())) {
                inAppIPList.add(cloudIPDTO);
            } else {
                notInAppIPListByLocal.add(cloudIPDTO);
            }
        });
        // 对于本地不在目标业务下的主机再到CMDB查询
        IBizCmdbClient bizCmdbClient = CmdbClientFactory.getCmdbClient();
        List<IpDTO> ipDTOList = notInAppIPListByLocal.parallelStream()
            .map(CloudIPDTO::toIpDTO)
            .collect(Collectors.toList());
        List<ApplicationHostDTO> cmdbExistHosts = bizCmdbClient.listHostsByIps(ipDTOList);
        Map<String, ApplicationHostDTO> cmdbHostsMap = new HashMap<>();
        Set<String> cmdbExistCloudIPSet = new HashSet<>();
        cmdbExistHosts.forEach(host -> {
            cmdbExistCloudIPSet.add(host.getCloudIp());
            cmdbHostsMap.put(host.getCloudIp(), host);
        });
        List<CloudIPDTO> notInCmdbIpList = new ArrayList<>();
        for (CloudIPDTO cloudIPDTO : notInAppIPListByLocal) {
            if (!cmdbExistCloudIPSet.contains(cloudIPDTO.getCloudIP())) {
                // 主机在CMDB中不存在
                notInCmdbIpList.add(cloudIPDTO);
                continue;
            }
            ApplicationHostDTO host = cmdbHostsMap.get(cloudIPDTO.getCloudIP());
            if (hostBelongToApp(appDTO, host.getBizId())) {
                inAppIPList.add(cloudIPDTO);
            } else {
                notInAppIPList.add(cloudIPDTO);
            }
        }
        if (!notInCmdbIpList.isEmpty()) {
            log.warn("ips not in cmdb:{}", StringUtil.concatCollection(notInCmdbIpList));
        }
        notInAppIPList.addAll(notInCmdbIpList);
    }

    @Override
    public List<HostInfoVO> getHostsByIp(
        String username,
        Long appId,
        ActionScopeEnum actionScope,
        List<String> checkIpList
    ) {
        log.info("Input=({},{},{})", username, appId, checkIpList);
        if (checkIpList == null || checkIpList.isEmpty()) {
            return Collections.emptyList();
        }
        Pair<Set<CloudIPDTO>, Set<String>> pair = parseInputCloudIPList(checkIpList);
        Set<CloudIPDTO> inputCloudIPSet = pair.getLeft();
        Set<String> inputIPWithoutCloudIdSet = pair.getRight();
        log.debug("inputCloudIPSet={},inputIPWithoutCloudIdSet={}", inputIPWithoutCloudIdSet, inputIPWithoutCloudIdSet);
        // 0.通过对输入的无云区域ID的IP进行云区域补全得到的完整IP，云区域ID来源：白名单、DB、默认值
        Set<CloudIPDTO> makeupCloudIPSet = new HashSet<>();
        // 1.查出对当前业务生效的白名单IP
        List<CloudIPDTO> appWhiteIPList = whiteIPService.listWhiteIP(appId, actionScope);
        log.debug("appWhiteIPList={}", appWhiteIPList);
        Set<String> appWhiteIPSet = new HashSet<>();
        appWhiteIPList.forEach(appWhiteIP -> {
            appWhiteIPSet.add(appWhiteIP.getCloudIP());
            // 若输入的纯IP与某个白名单IP匹配，则记录下其云区域ID用于后续查找
            if (inputIPWithoutCloudIdSet.contains(appWhiteIP.getIp().trim())) {
                makeupCloudIPSet.add(appWhiteIP);
            }
        });
        // 2.根据纯IP从DB查出所有可能的含云区域ID的完整IP
        List<ApplicationHostDTO> hostByPureIpList = applicationHostDAO.listHostInfo(null,
            inputIPWithoutCloudIdSet);
        Set<CloudIPDTO> hostByPureIpInDB = hostByPureIpList.parallelStream()
            .map(host -> new CloudIPDTO(host.getCloudAreaId(), host.getIp())).collect(Collectors.toSet());
        makeupCloudIPSet.addAll(hostByPureIpInDB);
        inputIPWithoutCloudIdSet.removeAll(
            hostByPureIpInDB.parallelStream().map(CloudIPDTO::getIp).collect(Collectors.toSet())
        );
        // 3.DB中找不到的纯IP视为使用默认云区域ID
        inputIPWithoutCloudIdSet.forEach(pureIp ->
            makeupCloudIPSet.add(new CloudIPDTO(JobConstants.DEFAULT_CLOUD_AREA_ID, pureIp))
        );

        log.debug("makeupCloudIPSet={}", makeupCloudIPSet);
        inputCloudIPSet.addAll(makeupCloudIPSet);
        List<CloudIPDTO> inputCloudIPList = new ArrayList<>(inputCloudIPSet);

        // 4.找出输入IP中的白名单IP
        List<CloudIPDTO> inputWhiteIPList = new ArrayList<>();
        List<CloudIPDTO> inputNotWhiteIPList = new ArrayList<>();
        inputCloudIPList.forEach(inputCloudIP -> {
            if (appWhiteIPSet.contains(inputCloudIP.getCloudIP())) {
                inputWhiteIPList.add(inputCloudIP);
            } else {
                inputNotWhiteIPList.add(inputCloudIP);
            }
        });
        // 5.非白名单IP校验是否在业务下
        List<CloudIPDTO> inAppIPList = new ArrayList<>();
        List<CloudIPDTO> notInAppIPList = new ArrayList<>();
        separateNotInAppIP(appId, inputNotWhiteIPList, inAppIPList, notInAppIPList);
        // 6.不在业务下的IP打印出来
        if (!notInAppIPList.isEmpty()) {
            log.warn(
                "ips not in app {}:{}",
                appId,
                StringUtil.concatCollection(notInAppIPList)
            );
        }
        // 7.查询主机详情
        List<CloudIPDTO> validIPList = new ArrayList<>(inputWhiteIPList);
        validIPList.addAll(inAppIPList);
        // 根据IP从本地查主机
        List<ApplicationHostDTO> hostDTOList = applicationHostDAO.listHostInfoByBizAndCloudIPs(null,
            validIPList.stream().map(CloudIPDTO::getCloudIP).collect(Collectors.toList()));
        Set<String> localHostCloudIPSet = hostDTOList.parallelStream()
            .map(ApplicationHostDTO::getCloudIp)
            .collect(Collectors.toSet());
        validIPList.removeIf(cloudIPDTO -> localHostCloudIPSet.contains(cloudIPDTO.getCloudIP()));
        // 查不到的再去CMDB查
        if (!validIPList.isEmpty()) {

            IBizCmdbClient cmdbClient = CmdbClientFactory.getCmdbClient();
            List<ApplicationHostDTO> cmdbHosts = cmdbClient.listHostsByIps(
                validIPList.parallelStream()
                    .map(CloudIPDTO::toIpDTO)
                    .collect(Collectors.toList())
            );
            hostDTOList.addAll(cmdbHosts);
            Set<String> cmdbHostIpSet = cmdbHosts.parallelStream()
                .map(ApplicationHostDTO::getCloudIp).collect(Collectors.toSet());
            validIPList.removeIf(cloudIPDTO -> cmdbHostIpSet.contains(cloudIPDTO.getCloudIP()));
            if (!validIPList.isEmpty()) {
                log.warn("Cannot find hostinfo of ips:{},ignore", validIPList);
            }
        }

        // 8.查询Agent状态
        fillAgentStatus(hostDTOList);
        // 9.类型转换，返回
        List<HostInfoVO> hostInfoList = new ArrayList<>();
        hostDTOList.forEach(hostInfo -> hostInfoList.add(TopologyHelper.convertToHostInfoVO(hostInfo)));
        return hostInfoList;
    }

    private void getCustomGroupListByBizId(Long bizId,
                                           Map<String, DynamicGroupWithHost> ccGroupInfoList,
                                           Map<Long, List<String>> bizId2GroupIdMap) {
        List<String> groupIdList = new ArrayList<>();
        List<CcDynamicGroupDTO> ccGroupList = CmdbClientFactory.getCmdbClient(JobContextUtil.getUserLang())
            .getDynamicGroupList(bizId);
        ccGroupList.forEach(ccDynamicGroup -> {
            ccGroupInfoList.put(ccDynamicGroup.getId(), ccDynamicGroup.toDynamicGroupWithHost());
            groupIdList.add(ccDynamicGroup.getId());
        });
        bizId2GroupIdMap.put(bizId, groupIdList);
    }

    private void fillAppInfo(Map<String, DynamicGroupWithHost> ccGroupInfoMap) {
        // 分组中的获取app信息
        Set<Long> bizIdSet = new HashSet<>();
        for (DynamicGroupWithHost groupInfo : ccGroupInfoMap.values()) {
            bizIdSet.add(groupInfo.getBizId());
        }
        List<ApplicationDTO> appInfoList = applicationService.listBizAppsByBizIds(bizIdSet);

        Map<String, ApplicationDTO> id2AppInfoMap = new HashMap<>(appInfoList.size());
        for (ApplicationDTO appInfo : appInfoList) {
            id2AppInfoMap.put(appInfo.getScope().getId(), appInfo);
        }

        for (DynamicGroupWithHost groupInfo : ccGroupInfoMap.values()) {
            ApplicationDTO appInfo = id2AppInfoMap.get(groupInfo.getBizId().toString());
            if (appInfo == null) {
                groupInfo.setBizName("");
                groupInfo.setOwner("");
                groupInfo.setOwnerName("");
            } else {
                groupInfo.setBizName(appInfo.getName());
                groupInfo.setOwner(appInfo.getBkSupplierAccount());
                groupInfo.setOwnerName(appInfo.getBkSupplierAccount());
            }
        }
    }

    @Override
    public List<HostInfoVO> listHostByAppTopologyNodes(String username, Long appId,
                                                       List<AppTopologyTreeNode> appTopoNodeList) {
        return listHostByAppTopologyNodes(username, appId, appTopoNodeList, null, null);
    }

    private List<Long> getBizModuleIdsByTopoNodes(String username,
                                                  Long bizId,
                                                  List<AppTopologyTreeNode> appTopoNodeList) {
        if (appTopoNodeList == null || appTopoNodeList.isEmpty()) {
            return Collections.emptyList();
        }
        IBizCmdbClient bizCmdbClient = CmdbClientFactory.getCmdbClient(JobContextUtil.getUserLang());
        // 查出业务
        ApplicationDTO appInfo = applicationService.getAppByScope(
            new ResourceScope(ResourceScopeTypeEnum.BIZ, bizId.toString())
        );
        List<Long> moduleIds = new ArrayList<>();
        // 普通业务可能根据各级自定义节点查主机，必须先根据拓扑树转为moduleId再查
        // 查业务拓扑树
        InstanceTopologyDTO appTopologyTree = bizCmdbClient.getBizInstTopology(bizId);
        if (appTopologyTree == null) {
            throw new InternalException("Fail to getBizTopo of bizId " + bizId + " from CMDB",
                ErrorCode.INTERNAL_ERROR);
        }
        for (AppTopologyTreeNode treeNode : appTopoNodeList) {
            CcInstanceDTO ccInstanceDTO = new CcInstanceDTO(treeNode.getObjectId(), treeNode.getInstanceId());
            // 查拓扑节点完整信息
            InstanceTopologyDTO completeNode = TopologyUtil.findNodeFromTopo(appTopologyTree, ccInstanceDTO);
            if (completeNode == null) {
                log.warn("Cannot find node in topo, node:{}, topo:", JsonUtils.toJson(ccInstanceDTO));
                TopologyUtil.printTopo(appTopologyTree);
                throw new InternalException("Fail to find node {objectId:" + ccInstanceDTO.getObjectType() + "," +
                    "instanceId:" + ccInstanceDTO.getInstanceId() + ") from app topo", ErrorCode.INTERNAL_ERROR);
            }
            moduleIds.addAll(TopologyUtil.findModuleIdsFromTopo(completeNode));
        }
        return moduleIds;
    }

    private List<HostInfoVO> listHostByAppTopologyNodes(String username,
                                                        Long appId,
                                                        List<AppTopologyTreeNode> appTopoNodeList,
                                                        Long start,
                                                        Long limit) {
        if (appTopoNodeList == null || appTopoNodeList.isEmpty()) {
            return Collections.emptyList();
        }
        IBizCmdbClient bizCmdbClient = CmdbClientFactory.getCmdbClient(JobContextUtil.getUserLang());
        // 查出业务
        ApplicationDTO appInfo = applicationService.getAppByAppId(appId);
        Set<Long> moduleIds = new HashSet<>();
        List<ApplicationHostDTO> hosts = new ArrayList<>();
        if (appInfo.isBiz()) {
            // 普通业务可能根据各级自定义节点查主机，必须先根据拓扑树转为moduleId再查
            // 查业务拓扑树
            InstanceTopologyDTO appTopologyTree = bizCmdbClient.getBizInstTopology(
                Long.parseLong(appInfo.getScope().getId())
            );
            for (AppTopologyTreeNode treeNode : appTopoNodeList) {
                CcInstanceDTO ccInstanceDTO = new CcInstanceDTO(treeNode.getObjectId(), treeNode.getInstanceId());
                // 查拓扑节点完整信息
                InstanceTopologyDTO completeNode = TopologyUtil.findNodeFromTopo(appTopologyTree, ccInstanceDTO);
                if (completeNode == null) {
                    log.warn("Cannot find completeNode {} from topologyTree",
                        ccInstanceDTO.getObjectType() + ":" + ccInstanceDTO.getInstanceId());
                    continue;
                }
                moduleIds.addAll(TopologyUtil.findModuleIdsFromTopo(completeNode));
            }
            // 查所有hostIds
            List<HostTopoDTO> hostTopoDTOList = hostTopoDAO.listHostTopoByModuleIds(dslContext, moduleIds, start,
                limit);
            List<Long> hostIdList =
                hostTopoDTOList.parallelStream().map(HostTopoDTO::getHostId).collect(Collectors.toList());
            hosts = applicationHostDAO.listHostInfoByHostIds(hostIdList);
        } else if (appInfo.isAllBizSet()) {
            // 全业务
            hosts = applicationHostDAO.listAllHostInfo(start, limit);
        } else if (appInfo.isBizSet()) {
            // 业务集：仅根据业务查主机
            // 查出对应的所有普通业务
            List<Long> allBizIds = topologyHelper.getBizSetSubBizIds(appInfo);
            hosts = applicationHostDAO.listHostInfoByBizIds(allBizIds, start, limit);
        }

        // 查出节点下主机与Agent状态
        List<String> ipWithCloudIdList = buildIpList(hosts);
        // 批量设置agent状态
        Map<String, QueryAgentStatusClient.AgentStatus> agentStatusMap =
            queryAgentStatusClient.batchGetAgentStatus(ipWithCloudIdList);
        List<HostInfoVO> hostInfoVOList = getHostInfoVOsByHostInfoDTOs(hosts);
        fillAgentStatus(agentStatusMap, hostInfoVOList);
        // 排序
        //异常排序
        List<HostInfoVO> orderedHostInfoVOList =
            hostInfoVOList.stream().filter(it -> it.getAlive() == 0).collect(Collectors.toList());
        orderedHostInfoVOList.addAll(hostInfoVOList.stream().filter(it -> it.getAlive() == 1)
            .collect(Collectors.toList()));
        hostInfoVOList = orderedHostInfoVOList;
        //重复靠前
        Map<String, List<HostInfoVO>> repeatMap = new HashMap<>();
        for (HostInfoVO it : hostInfoVOList) {
            String ip = it.getIp();
            if (repeatMap.containsKey(it.getIp())) {
                List<HostInfoVO> hostInfoVOs = repeatMap.get(ip);
                hostInfoVOs.add(it);
                repeatMap.put(ip, hostInfoVOs);
            } else {
                List<HostInfoVO> hostInfoVOs = new ArrayList<>();
                hostInfoVOs.add(it);
                repeatMap.put(ip, hostInfoVOs);
            }
        }
        hostInfoVOList.sort((o1, o2) -> repeatMap.get(o2.getIp()).size() - repeatMap.get(o1.getIp()).size());
        Set<Long> addedHostIds = new HashSet<>();
        List<HostInfoVO> finalHostInfoVOList = new ArrayList<>();
        for (HostInfoVO it : hostInfoVOList) {
            String ip = it.getIp();
            List<HostInfoVO> hostInfoVOs = repeatMap.get(ip);
            hostInfoVOs.forEach(hostInfoVO -> {
                if (!addedHostIds.contains(hostInfoVO.getHostId())) {
                    finalHostInfoVOList.add(hostInfoVO);
                    addedHostIds.add(hostInfoVO.getHostId());
                }
            });
        }
        return finalHostInfoVOList;
    }

    @Override
    public AgentStatistics getAgentStatistics(String username, Long appId, AgentStatisticsReq agentStatisticsReq) {
        log.info("Input=({},{},{})", username, appId, JsonUtils.toJson(agentStatisticsReq));
        ApplicationDTO applicationDTO = applicationService.getAppByAppId(appId);
        List<HostInfoVO> hostsByIp = getHostsByIp(username, appId, null, agentStatisticsReq.getIpList());
        log.debug("hostsByIp={}", hostsByIp);
        Set<HostInfoVO> allHostsSet = new HashSet<>(hostsByIp);
        List<HostInfoVO> hostsByNodes = listHostByAppTopologyNodes(username, appId,
            agentStatisticsReq.getAppTopoNodeList());
        log.debug("hostsByNodes={}", hostsByNodes);
        allHostsSet.addAll(hostsByNodes);
        // 只有普通业务才查动态分组
        if (applicationDTO.isBiz()) {
            List<ApplicationHostDTO> hostDTOsByDynamicGroupIds = new ArrayList<>();
            List<DynamicGroupWithHost> dynamicGroupList = getBizDynamicGroupHostList(
                username,
                applicationDTO.getBizIdIfBizApp(),
                agentStatisticsReq.getDynamicGroupIds()
            );
            dynamicGroupList.forEach(dynamicGroupInfoDTO -> {
                List<ApplicationHostDTO> applicationHostDTOList = dynamicGroupInfoDTO.getIpListStatus();
                if (applicationHostDTOList != null && !applicationHostDTOList.isEmpty()) {
                    hostDTOsByDynamicGroupIds.addAll(applicationHostDTOList);
                }
            });
            fillAgentStatus(hostDTOsByDynamicGroupIds);
            List<HostInfoVO> hostsByDynamicGroupIds = getHostInfoVOsByHostInfoDTOs(hostDTOsByDynamicGroupIds);
            log.debug("hostsByDynamicGroupIds={}", hostsByDynamicGroupIds);
            allHostsSet.addAll(hostsByDynamicGroupIds);
        }
        log.debug("allHostsSet.size={},allHostsSet={}", allHostsSet.size(), allHostsSet);
        int normalCount = 0;
        int abnormalCount = 0;
        for (HostInfoVO it : allHostsSet) {
            if (it.getAlive() == 1) {
                normalCount += 1;
            } else {
                abnormalCount += 1;
            }
        }
        return new AgentStatistics(normalCount, abnormalCount);
    }

    @Override
    public List<IpDTO> checkAppHosts(Long appId,
                                     List<IpDTO> hostIps) {
        ApplicationDTO application = applicationService.getAppByAppId(appId);

        if (application.isAllBizSet()) {
            // cmdb全业务包含了所有业务下的主机，不需要再判断
            return Collections.emptyList();
        }

        List<Long> includeBizIds = buildIncludeBizIdList(application);
        if (CollectionUtils.isEmpty(includeBizIds)) {
            log.warn("App do not contains any biz, appId:{}", appId);
            return hostIps;
        }

        List<BasicAppHost> hostsInOtherApp = new ArrayList<>();
        List<IpDTO> notExistHosts = new ArrayList<>();

        // 从缓存中查询主机信息，并判断主机是否在业务下
        checkCachedHosts(hostIps, includeBizIds, hostsInOtherApp, notExistHosts);

        // 如果主机不在缓存中，那么从已同步到Job的主机中查询
        if (CollectionUtils.isNotEmpty(notExistHosts)) {
            log.info("Hosts not in cached, check hosts by synced hosts. notCacheHosts: {}", notExistHosts);
            checkSyncHosts(includeBizIds, hostsInOtherApp, notExistHosts);
        }

        // 如果主机不在已同步到Job的主机中, 那么从cmdb直接查询
        if (CollectionUtils.isNotEmpty(notExistHosts)) {
            log.info("Hosts not synced, check hosts by cmdb. notSyncedHosts: {}", notExistHosts);
            checkHostsFromCmdb(includeBizIds, hostsInOtherApp, notExistHosts);
        }

        List<IpDTO> invalidHosts = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(notExistHosts) || CollectionUtils.isNotEmpty(hostsInOtherApp)) {
            invalidHosts.addAll(notExistHosts);
            invalidHosts.addAll(hostsInOtherApp.stream()
                .map(host -> new IpDTO(host.getCloudAreaId(), host.getIp())).collect(Collectors.toList()));
            log.info("Contains invalid hosts, appId: {}, notExistHosts: {}, hostsInOtherApp: {}",
                appId, notExistHosts, hostsInOtherApp);
        }
        return invalidHosts;
    }

    private List<Long> buildIncludeBizIdList(ApplicationDTO application) {
        List<Long> bizIdList = new ArrayList<>();
        if (application.isBiz()) {
            bizIdList.add(application.getBizIdIfBizApp());
        } else if (application.isBizSet()) {
            if (application.getSubBizIds() != null) {
                bizIdList.addAll(application.getSubBizIds());
            }
        }
        return bizIdList;
    }

    private void checkCachedHosts(List<IpDTO> hostIps,
                                  List<Long> includeBizIds,
                                  List<BasicAppHost> hostsInOtherApp,
                                  List<IpDTO> notExistHosts) {
        List<CacheHostDO> cacheHosts = hostCache.batchGetHosts(hostIps);
        for (int i = 0; i < hostIps.size(); i++) {
            IpDTO hostIp = hostIps.get(i);
            CacheHostDO cacheHost = cacheHosts.get(i);
            if (cacheHost != null) {
                if (!includeBizIds.contains(cacheHost.getBizId())) {
                    hostsInOtherApp.add(new BasicAppHost(cacheHost.getBizId(),
                        cacheHost.getCloudAreaId(), cacheHost.getIp()));
                }
            } else {
                notExistHosts.add(hostIp);
            }
        }
    }

    private void checkSyncHosts(List<Long> includeBizIds,
                                List<BasicAppHost> hostsInOtherApp,
                                List<IpDTO> notExistHosts) {
        List<ApplicationHostDTO> appHosts = applicationHostDAO.listHosts(notExistHosts);
        if (CollectionUtils.isNotEmpty(appHosts)) {
            for (ApplicationHostDTO appHost : appHosts) {
                if (appHost.getBizId() == null || appHost.getBizId() <= 0) {
                    log.info("Host: {} missing bizId, skip!", appHost.getCloudIp());
                    // DB中缓存的主机可能没有业务信息(依赖的主机事件还没有处理),那么暂时跳过该主机
                    continue;
                }
                IpDTO hostIp = new IpDTO(appHost.getCloudAreaId(), appHost.getIp());
                if (appHost.getBizId() == JobConstants.PUBLIC_APP_ID) {
                    continue;
                }
                notExistHosts.remove(hostIp);
                hostCache.addOrUpdateHost(appHost);
                if (!includeBizIds.contains(appHost.getBizId())) {
                    hostsInOtherApp.add(new BasicAppHost(appHost.getBizId(),
                        appHost.getCloudAreaId(), appHost.getIp()));
                }
            }
        }
    }

    private void checkHostsFromCmdb(List<Long> includeBizIds,
                                    List<BasicAppHost> hostsInOtherApp,
                                    List<IpDTO> notExistHosts) {

        IBizCmdbClient bizCmdbClient = CmdbClientFactory.getCmdbClient();
        try {
            List<ApplicationHostDTO> cmdbExistHosts = bizCmdbClient.listHostsByIps(notExistHosts);
            if (CollectionUtils.isNotEmpty(cmdbExistHosts)) {
                List<IpDTO> cmdbExistHostIps = cmdbExistHosts.stream()
                    .map(host -> new IpDTO(host.getCloudAreaId(), host.getIp()))
                    .collect(Collectors.toList());
                notExistHosts.removeAll(cmdbExistHostIps);
                log.info("sync new hosts from cmdb, hosts:{}", cmdbExistHosts);

                hostCache.addOrUpdateHosts(cmdbExistHosts);

                cmdbExistHosts.forEach(syncHost -> {
                    if (!includeBizIds.contains(syncHost.getBizId())) {
                        hostsInOtherApp.add(new BasicAppHost(syncHost.getBizId(),
                            syncHost.getCloudAreaId(), syncHost.getIp()));
                    }
                });
            }
        } catch (Exception e) {
            log.warn("Handle hosts that may not be synchronized from cmdb fail!", e);
        }
    }

    public List<ApplicationHostDTO> listHosts(Collection<IpDTO> hostIps) {
        // 从Job已同步的主机中查询
        List<ApplicationHostDTO> syncedHosts = applicationHostDAO.listHosts(hostIps);
        Map<String, ApplicationHostDTO> hostsMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(syncedHosts)) {
            syncedHosts.forEach(syncedHost -> hostsMap.put(syncedHost.getCloudIp(), syncedHost));
        }

        List<ApplicationHostDTO> resultHosts = new ArrayList<>();
        List<IpDTO> notSyncedHostIps = new ArrayList<>();
        hostIps.forEach(hostIp -> {
            ApplicationHostDTO host = hostsMap.get(hostIp.convertToStrIp());
            if (host == null || host.getBizId() == null || host.getBizId() <= 0) {
                notSyncedHostIps.add(hostIp);
            } else {
                resultHosts.add(host);
            }
        });
        // 如果从已同步的主机中没有查询到的主机或者主机信息不完整，那么从cmdb实时查询
        if (CollectionUtils.isNotEmpty(notSyncedHostIps)) {
            IBizCmdbClient bizCmdbClient = CmdbClientFactory.getCmdbClient();
            List<ApplicationHostDTO> hostsFromCmdb = bizCmdbClient.listHostsByIps(notSyncedHostIps);
            if (CollectionUtils.isNotEmpty(hostsFromCmdb)) {
                resultHosts.addAll(hostsFromCmdb);
            }
        }

        return resultHosts;
    }

    @Data
    private static class BasicAppHost {
        private Long bizId;
        private Long cloudAreaId;
        private String ip;

        private BasicAppHost(Long bizId, Long cloudAreaId, String ip) {
            this.bizId = bizId;
            this.cloudAreaId = cloudAreaId;
            this.ip = ip;
        }
    }
}
