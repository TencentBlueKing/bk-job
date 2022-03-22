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
import com.tencent.bk.job.common.cc.model.CcGroupDTO;
import com.tencent.bk.job.common.cc.model.CcGroupHostPropDTO;
import com.tencent.bk.job.common.cc.model.CcInstanceDTO;
import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.cc.sdk.CmdbClientFactory;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.cc.service.CloudAreaService;
import com.tencent.bk.job.common.cc.util.TopologyUtil;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.gse.service.QueryAgentStatusClient;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.DynamicGroupInfoDTO;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.model.vo.CloudAreaInfoVO;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.common.util.ConcurrencyUtil;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.PageUtil;
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

    @Autowired
    public HostServiceImpl(DSLContext dslContext,
                           ApplicationHostDAO applicationHostDAO,
                           ApplicationService applicationService,
                           HostTopoDAO hostTopoDAO,
                           TopologyHelper topologyHelper,
                           CloudAreaService cloudAreaService,
                           QueryAgentStatusClient queryAgentStatusClient,
                           WhiteIPService whiteIPService,
                           HostCache hostCache) {
        this.dslContext = dslContext;
        this.applicationHostDAO = applicationHostDAO;
        this.applicationService = applicationService;
        this.hostTopoDAO = hostTopoDAO;
        this.topologyHelper = topologyHelper;
        this.cloudAreaService = cloudAreaService;
        this.queryAgentStatusClient = queryAgentStatusClient;
        this.whiteIPService = whiteIPService;
        this.hostCache = hostCache;
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
            return applicationHostDAO.listHostInfoByBizIds(applicationDTO.getSubAppIds(), null, null);
        }
    }

    private boolean insertOrUpdateOneAppHost(Long bizId, ApplicationHostDTO infoDTO) {
        try {
            applicationHostDAO.insertAppHostInfo(dslContext, infoDTO);
        } catch (DataAccessException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("Duplicate entry") && errorMessage.contains("PRIMARY")) {
                log.warn(String.format(
                    "insertHost fail, try to update:Duplicate entry:bizId=%d," +
                        "insert hostInfo=%s, old " +
                        "hostInfo=%s", bizId, infoDTO,
                    applicationHostDAO.getHostById(infoDTO.getHostId())), e);
                try {
                    // 插入失败了就应当更新，以后来的数据为准
                    applicationHostDAO.updateBizHostInfoByHostId(dslContext, bizId, infoDTO);
                } catch (Throwable t) {
                    log.error(String.format("update after insert fail:bizId=%d,hostInfo=%s", bizId, infoDTO), t);
                    return false;
                }
            } else {
                log.error(String.format("insertHost fail:bizId=%d,hostInfo=%s", bizId, infoDTO), e);
                return false;
            }
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
    public List<Long> deleteHostsFromBiz(Long bizId, List<ApplicationHostDTO> deleteList) {
        StopWatch watch = new StopWatch();
        // 删除主机
        watch.start("deleteAppHostInfo");
        List<Long> deleteFailHostIds = new ArrayList<>();
        boolean batchDeleted = false;
        try {
            // 尝试批量删除
            if (!deleteList.isEmpty()) {
                applicationHostDAO.batchDeleteBizHostInfoById(dslContext, bizId,
                    deleteList.stream().map(ApplicationHostDTO::getHostId).collect(Collectors.toList()));
            }
            batchDeleted = true;
        } catch (Throwable throwable) {
            log.warn("Fail to batchDeleteAppHostInfoById, try to delete one by one", throwable);
            // 批量删除失败，尝试逐条删除
            for (ApplicationHostDTO ApplicationHostDTO : deleteList) {
                try {
                    applicationHostDAO.deleteBizHostInfoById(dslContext, bizId, ApplicationHostDTO.getHostId());
                } catch (Throwable t) {
                    log.error("deleteHost fail:appId={},hostInfo={}", bizId,
                        ApplicationHostDTO, t);
                    deleteFailHostIds.add(ApplicationHostDTO.getHostId());
                }
            }
        }
        watch.stop();
        if (!batchDeleted) {
            watch.start("log deleteAppHostInfo");
            if (!deleteFailHostIds.isEmpty()) {
                log.warn(String.format("appId=%s,deleteFailHostIds.size=%d,deleteFailHostIds=%s",
                    bizId, deleteFailHostIds.size(), String.join(",",
                        deleteFailHostIds.stream().map(Object::toString).collect(Collectors.toSet()))));
            }
            watch.stop();
        }
        log.debug("Performance:deleteHostsFromApp:appId={},{}", bizId, watch.prettyPrint());
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
        if (appResourceScope.getType() == ResourceScopeTypeEnum.BIZ_SET) {
            // 业务集
            CcTopologyNodeVO ccTopologyNodeVO = new CcTopologyNodeVO();
            ccTopologyNodeVO.setObjectId("biz");
            ccTopologyNodeVO.setObjectId("业务");
            ccTopologyNodeVO.setInstanceId(Long.valueOf(appResourceScope.getId()));
            ccTopologyNodeVO.setInstanceName(appInfo.getName());
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
    public List<DynamicGroupInfoDTO> getAppDynamicGroupList(String username,
                                                            AppResourceScope appResourceScope) {
        ApplicationDTO applicationInfo = applicationService.getAppByAppId(appResourceScope.getAppId());

        Map<String, DynamicGroupInfoDTO> ccGroupInfoMap = new HashMap<>();
        Map<Long, List<String>> appId2GroupIdMap = new HashMap<>();
        if (ResourceScopeTypeEnum.BIZ_SET == appResourceScope.getType()) {
            for (long subAppId : applicationInfo.getSubAppIds()) {
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
    public List<DynamicGroupInfoDTO> getBizDynamicGroupHostList(String username, Long bizId,
                                                                List<String> dynamicGroupIdList) {
        ApplicationDTO applicationInfo = applicationService.getAppByScope(
            new ResourceScope(ResourceScopeTypeEnum.BIZ, bizId.toString())
        );
        String maintainer = applicationInfo.getMaintainers().split("[,;]")[0];

        Map<String, DynamicGroupInfoDTO> ccGroupInfoMap = new HashMap<>();
        Map<Long, List<String>> bizId2GroupIdMap = new HashMap<>();
        getCustomGroupListByBizId(bizId, ccGroupInfoMap, bizId2GroupIdMap);

        for (Map.Entry<Long, List<String>> entry : bizId2GroupIdMap.entrySet()) {
            long groupBizId = entry.getKey();
            for (String customerGroupId : entry.getValue()) {
                if (!dynamicGroupIdList.contains(customerGroupId)) {
                    ccGroupInfoMap.remove(customerGroupId);
                    continue;
                }
                List<CcGroupHostPropDTO> ccGroupHostProps =
                    CmdbClientFactory.getCmdbClient(JobContextUtil.getUserLang())
                        .getCustomGroupIp(groupBizId, customerGroupId);
                List<String> ipList = new ArrayList<>();
                for (CcGroupHostPropDTO groupHost : ccGroupHostProps) {
                    if (CollectionUtils.isNotEmpty(groupHost.getCloudIdList())) {
                        ipList.add(groupHost.getCloudIdList().get(0).getInstanceId() + ":" + groupHost.getIp());
                    } else {
                        log.warn("Wrong host info! No cloud area!|{}", groupHost);
                        continue;
                    }
                }

                ccGroupInfoMap.get(customerGroupId).setIpList(ipList);
            }
        }

        fillAppInfo(ccGroupInfoMap);

        for (DynamicGroupInfoDTO group : ccGroupInfoMap.values()) {
            List<ApplicationHostDTO> applicationHostDTOList = topologyHelper.getIpStatusListByIps(bizId,
                group.getIpList());
            applicationHostDTOList.forEach(ApplicationHostDTO -> {
                ApplicationHostDTO appHostInfo = applicationHostDAO.getLatestHost(dslContext, bizId,
                    ApplicationHostDTO.getCloudAreaId(), ApplicationHostDTO.getIp());
                if (appHostInfo != null) {
                    // 填充主机名称与操作系统
                    ApplicationHostDTO.setHostId(appHostInfo.getHostId());
                    ApplicationHostDTO.setIpDesc(appHostInfo.getIpDesc());
                    ApplicationHostDTO.setOs(appHostInfo.getOs());
                }
            });
            group.setIpListStatus(applicationHostDTOList);
        }
        return new ArrayList<>(ccGroupInfoMap.values());
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
        List<ApplicationHostDTO> hosts = dbHosts;
        watch.stop();
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
        //批量设置agent状态
        if (updateAgentStatus) {
            watch.start("batchGetAgentStatus");
            Map<String, QueryAgentStatusClient.AgentStatus> agentStatusMap =
                queryAgentStatusClient.batchGetAgentStatus(buildIpList(hosts));
            fillAgentStatus(agentStatusMap, hostInfoVOList);
            watch.stop();
        }
        //将主机挂载到topo树
        watch.start("setToTopoTree");
        for (int i = 0; i < hostInfoVOList.size(); i++) {
            ApplicationHostDTO host = hosts.get(i);
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
                    if (agentStatus != null && agentStatus.status == 1) {
                        hostInfoDTO.setGseAgentAlive(true);
                    } else {
                        hostInfoDTO.setGseAgentAlive(false);
                    }
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
        } else if (appInfo.getScope().getType() == ResourceScopeTypeEnum.BIZ_SET) {
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

    private List<ApplicationHostDTO> filterBySpecifiedCloudId(
        List<CloudIPDTO> cloudIPDTOList,
        List<ApplicationHostDTO> applicationHostDTOList
    ) {
        //生成指定的云区域Map
        Map<String, Set<Long>> map = new HashMap<>();
        cloudIPDTOList.forEach(cloudIPDTO -> {
            String ip = cloudIPDTO.getIp();
            if (cloudIPDTO.getCloudAreaId() != null) {
                if (map.keySet().contains(ip)) {
                    Set<Long> set = map.get(ip);
                    set.add(cloudIPDTO.getCloudAreaId());
                    map.put(ip, set);
                } else {
                    Set<Long> set = new HashSet<>();
                    set.add(cloudIPDTO.getCloudAreaId());
                    map.put(ip, set);
                }
            } else {
                //未指定云区域的IP不作为过滤条件
            }
        });
        return applicationHostDTOList.stream().filter(ApplicationHostDTO -> {
            Set<String> keySet = map.keySet();
            String ip = ApplicationHostDTO.getIp();
            Long cloudId = ApplicationHostDTO.getCloudAreaId();
            if (keySet.contains(ip)) {
                return map.get(ip).contains(cloudId);
            }
            return true;
        }).collect(Collectors.toList());
    }

    public List<ApplicationHostDTO> getHostInfoById(String username, Long appId, List<String> ipList) {
        List<ApplicationHostDTO> hostInfoList = new ArrayList<>();
        if (CollectionUtils.isEmpty(ipList)) {
            return hostInfoList;
        }
        ApplicationDTO appInfo = applicationService.getAppByAppId(appId);
        if (appInfo == null) {
            return hostInfoList;
        }
        ResourceScopeTypeEnum scopeType = appInfo.getScope().getType();
        if (appInfo.getScope().getType() == ResourceScopeTypeEnum.BIZ) {
            hostInfoList.addAll(applicationHostDAO.listHostInfoByIps(
                Long.valueOf(appInfo.getScope().getId()), ipList));
        } else if (scopeType == ResourceScopeTypeEnum.BIZ_SET) {
            List<Long> subBizIds = topologyHelper.getBizSetSubBizIds(appInfo);
            // 直接使用本地缓存数据
            log.debug("subBizIdsSize={}, get host from local db", subBizIds.size());
            hostInfoList.addAll(applicationHostDAO.listHostInfo(subBizIds, ipList));
        } else {
            log.warn("Not supported scopeType:{}", scopeType);
        }
        return hostInfoList;
    }

    private List<CloudIPDTO> parseInputCloudIPList(List<String> checkIpList) {
        List<CloudIPDTO> inputCloudIPList = new ArrayList<>();
        checkIpList = checkIpList.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());
        for (int i = 0; i < checkIpList.size(); i++) {
            String ip = checkIpList.get(i);
            if (!StringUtils.isBlank(ip)) {
                if (ip.contains(":") || ip.contains("：")) {
                    //有云区域Id
                    try {
                        Pattern pattern = Pattern.compile("[:：]");
                        String[] arr = pattern.split(ip);
                        inputCloudIPList.add(new CloudIPDTO(Long.parseLong(arr[0].trim()), arr[1].trim()));
                    } catch (Exception e) {
                        log.warn("Invalid Ip:" + ip, e);
                        throw new InternalException("every ip in checkIpList must contain cloudAreaId",
                            ErrorCode.INTERNAL_ERROR);
                    }
                } else {
                    inputCloudIPList.add(new CloudIPDTO(null, ip.trim()));
                }
            }
        }
        return inputCloudIPList;
    }

    private void separateWhiteIP(
        List<CloudIPDTO> inputCloudIPList,
        List<CloudIPDTO> inputWhiteIPList,
        List<CloudIPDTO> inputNotWhiteIPList,
        Map<String, List<CloudIPDTO>> whiteIPMap
    ) {
        Set<String> whiteIPSet = whiteIPMap.keySet();
        inputCloudIPList.forEach(cloudIPDTO -> {
            String ip = cloudIPDTO.getIp();
            Long cloudId = cloudIPDTO.getCloudAreaId();
            if (whiteIPSet.contains(ip)) {
                Set<Long> whiteCloudIdSet =
                    whiteIPMap.get(ip).stream().map(CloudIPDTO::getCloudAreaId).collect(Collectors.toSet());
                if (cloudId == null) {
                    //输入IP时未指定云区域，白名单中的所有云区域都可以用
                    for (Long cloudAreaId : whiteCloudIdSet) {
                        inputWhiteIPList.add(new CloudIPDTO(cloudAreaId, ip));
                    }
                } else {
                    //输入IP时指定了云区域
                    if (whiteCloudIdSet.contains(cloudId)) {
                        inputWhiteIPList.add(cloudIPDTO);
                    } else {
                        inputNotWhiteIPList.add(cloudIPDTO);
                    }
                }
            } else {
                inputNotWhiteIPList.add(cloudIPDTO);
            }
        });
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
        List<CloudIPDTO> inputCloudIPList = parseInputCloudIPList(checkIpList);
        //1.先查IP白名单中是否存在该IP
        List<CloudIPDTO> appWhiteIPList = whiteIPService.listWhiteIP(appId, actionScope);
        //生成IP为索引的Map
        Map<String, List<CloudIPDTO>> whiteIPMap = new HashMap<>();
        appWhiteIPList.forEach(cloudIPDTO -> {
            String ip = cloudIPDTO.getIp();
            if (whiteIPMap.containsKey(ip)) {
                List<CloudIPDTO> list = whiteIPMap.get(ip);
                list.add(cloudIPDTO);
                whiteIPMap.put(ip, list);
            } else {
                List<CloudIPDTO> list = new ArrayList<>();
                list.add(cloudIPDTO);
                whiteIPMap.put(ip, list);
            }
        });

        //找出输入IP中的白名单IP
        List<CloudIPDTO> inputWhiteIPList = new ArrayList<>();
        List<CloudIPDTO> inputNotWhiteIPList = new ArrayList<>();
        separateWhiteIP(inputCloudIPList, inputWhiteIPList, inputNotWhiteIPList, whiteIPMap);
        //使用不带业务信息接口查询白名单IP对应的主机详情
        //仅根据IP查询，查出后再根据指定云区域过滤
        //根据IP查主机（缺少业务信息）本地
        List<ApplicationHostDTO> applicationHostDTOList = applicationHostDAO.listHostInfoByIps(null,
            inputWhiteIPList.stream().map(CloudIPDTO::getIp).collect(Collectors.toList()));

        //根据指定的云区域过滤
        applicationHostDTOList = filterBySpecifiedCloudId(inputWhiteIPList, applicationHostDTOList);

        //2.在当前业务下查IP对应的主机详情
        List<ApplicationHostDTO> hostInfoById = getHostInfoById(username, appId,
            inputNotWhiteIPList.stream().map(CloudIPDTO::getIp).collect(Collectors.toList()));
        //根据指定的云区域过滤
        hostInfoById = filterBySpecifiedCloudId(inputNotWhiteIPList, hostInfoById);
        hostInfoById.addAll(applicationHostDTOList);
        //3.查主机状态
        List<ApplicationHostDTO> hostIpList =
            topologyHelper.getIpStatusListByIps(appId, hostInfoById.parallelStream()
                .map(hostInfo -> hostInfo.getCloudAreaId() + ":" + hostInfo.getIp()).collect(Collectors.toList()));
        for (int i = 0; i < hostInfoById.size(); i++) {
            hostIpList.get(i).setHostId(hostInfoById.get(i).getHostId());
            hostIpList.get(i).setDisplayIp(hostInfoById.get(i).getDisplayIp());
            hostIpList.get(i).setOs(hostInfoById.get(i).getOs());
            hostIpList.get(i).setIpDesc(hostInfoById.get(i).getIpDesc());
        }
        List<HostInfoVO> hostInfoList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(hostIpList)) {
            hostIpList.forEach(hostInfo -> hostInfoList.add(TopologyHelper.convertToHostInfoVO(hostInfo)));
        }
        return hostInfoList;
    }

    private void getCustomGroupListByBizId(Long bizId,
                                           Map<String, DynamicGroupInfoDTO> ccGroupInfoList,
                                           Map<Long, List<String>> bizId2GroupIdMap) {
        List<String> groupIdList = new ArrayList<>();
        List<CcGroupDTO> ccGroupList = CmdbClientFactory.getCmdbClient(JobContextUtil.getUserLang())
            .getCustomGroupList(bizId);
        ccGroupList.forEach(ccGroupDTO -> {
            ccGroupInfoList.put(ccGroupDTO.getId(), ccGroupDTO.toDynamicGroupInfo());
            groupIdList.add(ccGroupDTO.getId());
        });
        bizId2GroupIdMap.put(bizId, groupIdList);
    }

    private void fillAppInfo(Map<String, DynamicGroupInfoDTO> ccGroupInfoMap) {
        // 分组中的获取app信息
        Set<Long> bizIdSet = new HashSet<>();
        for (DynamicGroupInfoDTO groupInfo : ccGroupInfoMap.values()) {
            bizIdSet.add(groupInfo.getBizId());
        }
        List<ApplicationDTO> appInfoList = applicationService.listBizAppsByBizIds(bizIdSet);

        Map<String, ApplicationDTO> id2AppInfoMap = new HashMap<>(appInfoList.size());
        for (ApplicationDTO appInfo : appInfoList) {
            id2AppInfoMap.put(appInfo.getScope().getId(), appInfo);
        }

        for (DynamicGroupInfoDTO groupInfo : ccGroupInfoMap.values()) {
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
        if (appInfo.getScope().getType() == ResourceScopeTypeEnum.BIZ) {
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
        } else if (appInfo.getScope().getType() == ResourceScopeTypeEnum.BIZ_SET) {
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
        if (applicationDTO.getScope().getType() == ResourceScopeTypeEnum.BIZ) {
            List<ApplicationHostDTO> hostDTOsByDynamicGroupIds = new ArrayList<>();
            List<DynamicGroupInfoDTO> dynamicGroupList =
                getBizDynamicGroupHostList(username, appId, agentStatisticsReq.getDynamicGroupIds());
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
        List<Long> includeAppIds = buildIncludeAppIdList(application);
        if (CollectionUtils.isEmpty(includeAppIds)) {
            log.warn("App is not exist or appSet contains no sub app, appId:{}", application.getId());
            return hostIps;
        }

        List<IpDTO> hostsNotInApp = new ArrayList<>();
        List<CacheHostDO> cacheHosts = hostCache.batchGetHosts(hostIps);
        List<IpDTO> notExistHosts = new ArrayList<>();

        for (int i = 0; i < hostIps.size(); i++) {
            IpDTO hostIp = hostIps.get(i);
            CacheHostDO cacheHost = cacheHosts.get(i);
            if (cacheHost != null) {
                if (!includeAppIds.contains(cacheHost.getAppId())) {
                    hostsNotInApp.add(new IpDTO(cacheHost.getCloudAreaId(), cacheHost.getIp()));
                }
            } else {
                notExistHosts.add(hostIp);
            }
        }

        if (CollectionUtils.isNotEmpty(notExistHosts)) {
            List<ApplicationHostDTO> appHosts = applicationHostDAO.listHosts(notExistHosts);
            if (CollectionUtils.isNotEmpty(appHosts)) {
                for (ApplicationHostDTO appHost : appHosts) {
                    IpDTO hostIp = new IpDTO(appHost.getCloudAreaId(), appHost.getIp());
                    notExistHosts.remove(hostIp);
                    hostCache.addOrUpdateHost(appHost);
                    if (!includeAppIds.contains(appHost.getBizId())) {
                        hostsNotInApp.add(hostIp);
                    }
                }
            }
        }

        if (CollectionUtils.isNotEmpty(notExistHosts)) {
            hostsNotInApp.addAll(notExistHosts);
        }
        return hostsNotInApp;
    }

    private List<Long> buildIncludeAppIdList(ApplicationDTO application) {
        List<Long> appIdList = new ArrayList<>();
        boolean isBiz = application.getScope().getType() == ResourceScopeTypeEnum.BIZ;
        if (isBiz) {
            appIdList.add(application.getId());
        } else {
            if (application.getSubAppIds() != null) {
                appIdList.addAll(application.getSubAppIds());
            }
        }
        return appIdList;
    }

    public List<ApplicationHostDTO> listHosts(Collection<IpDTO> hostIps) {
        return applicationHostDAO.listHosts(hostIps);
    }
}
