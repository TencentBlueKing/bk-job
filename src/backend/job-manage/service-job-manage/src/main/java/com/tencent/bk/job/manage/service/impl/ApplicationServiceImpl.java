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
import com.tencent.bk.job.common.cc.sdk.CcClient;
import com.tencent.bk.job.common.cc.sdk.CcClientFactory;
import com.tencent.bk.job.common.cc.service.CloudAreaService;
import com.tencent.bk.job.common.cc.util.TopologyUtil;
import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.gse.service.QueryAgentStatusClient;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.ApplicationHostInfoDTO;
import com.tencent.bk.job.common.model.dto.ApplicationInfoDTO;
import com.tencent.bk.job.common.model.dto.DynamicGroupInfoDTO;
import com.tencent.bk.job.common.model.vo.CloudAreaInfoVO;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.common.util.ConcurrencyUtil;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.PageUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.common.TopologyHelper;
import com.tencent.bk.job.manage.common.consts.whiteip.ActionScopeEnum;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.dao.ApplicationInfoDAO;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.model.dto.HostTopoDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.CloudIPDTO;
import com.tencent.bk.job.manage.model.web.request.AgentStatisticsReq;
import com.tencent.bk.job.manage.model.web.request.ipchooser.AppTopologyTreeNode;
import com.tencent.bk.job.manage.model.web.request.ipchooser.ListHostByBizTopologyNodesReq;
import com.tencent.bk.job.manage.model.web.vo.CcTopologyNodeVO;
import com.tencent.bk.job.manage.model.web.vo.NodeInfoVO;
import com.tencent.bk.job.manage.model.web.vo.index.AgentStatistics;
import com.tencent.bk.job.manage.service.AccountService;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.WhiteIPService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @since 7/11/2019 19:12
 */
@Slf4j
@Service
public class ApplicationServiceImpl implements ApplicationService {

    private final DSLContext dslContext;
    private final ApplicationHostDAO applicationHostDAO;
    private final ApplicationInfoDAO applicationInfoDAO;
    private final HostTopoDAO hostTopoDAO;
    private final TopologyHelper topologyHelper;
    private final AccountService accountService;
    private final CloudAreaService cloudAreaService;
    private final QueryAgentStatusClient queryAgentStatusClient;
    private WhiteIPService whiteIPService;

    @Autowired
    public ApplicationServiceImpl(DSLContext dslContext, ApplicationHostDAO applicationHostDAO,
                                  ApplicationInfoDAO applicationInfoDAO,
                                  HostTopoDAO hostTopoDAO, TopologyHelper topologyHelper,
                                  QueryAgentStatusClient queryAgentStatusClient, CloudAreaService cloudAreaService,
                                  AccountService accountService) {
        this.dslContext = dslContext;
        this.applicationHostDAO = applicationHostDAO;
        this.applicationInfoDAO = applicationInfoDAO;
        this.hostTopoDAO = hostTopoDAO;
        this.topologyHelper = topologyHelper;
        this.queryAgentStatusClient = queryAgentStatusClient;
        this.cloudAreaService = cloudAreaService;
        this.accountService = accountService;
    }

    public static List<String> buildIpList(List<ApplicationHostInfoDTO> hosts) {
        List<String> ipList = new ArrayList<>();
        for (ApplicationHostInfoDTO host : hosts) {
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
    public void setWhiteIPService(WhiteIPService whiteIPService) {
        this.whiteIPService = whiteIPService;
    }

    @Override
    public List<Long> getFullAppIds(Long appId) {
        //1.查找包含当前业务的业务集、全业务
        List<Long> fullAppIds = new ArrayList<>();
        fullAppIds.add(appId);
        List<ApplicationInfoDTO> appSets = applicationInfoDAO.listAppInfoByType(AppTypeEnum.APP_SET);
        if (appSets != null && !appSets.isEmpty()) {
            appSets.forEach(appSet -> {
                List<Long> subAppIds = topologyHelper.getAppSetSubAppIds(appSet);
                if (subAppIds.contains(appId)) {
                    fullAppIds.add(appSet.getId());
                }
            });
        }
        List<ApplicationInfoDTO> allAppRecords = applicationInfoDAO.listAppInfoByType(AppTypeEnum.ALL_APP);
        if (allAppRecords != null && !allAppRecords.isEmpty()) {
            allAppRecords.forEach(record -> {
                fullAppIds.add(record.getId());
            });
        }
        return fullAppIds;
    }

    @Override
    public PageData<ApplicationHostInfoDTO> listAppHost(ApplicationHostInfoDTO applicationHostInfoCondition,
                                                        BaseSearchCondition baseSearchCondition) {
        return applicationHostDAO.listHostInfoByPage(applicationHostInfoCondition, baseSearchCondition);
    }

    @Override
    public CcTopologyNodeVO listAppTopologyTree(String username, Long appId) {
        ApplicationInfoDTO appInfo = applicationInfoDAO.getCacheAppInfoById(appId);
        if (appInfo == null) {
            throw new InvalidParamException(ErrorCode.WRONG_APP_ID);
        }
        if (appInfo.getAppType() == AppTypeEnum.ALL_APP) {
            // 全业务
            CcTopologyNodeVO ccTopologyNodeVO = new CcTopologyNodeVO();
            ccTopologyNodeVO.setObjectId("biz");
            ccTopologyNodeVO.setObjectId("业务");
            ccTopologyNodeVO.setInstanceId(appId);
            ccTopologyNodeVO.setInstanceName(appInfo.getName());
            ccTopologyNodeVO.setCount((int) applicationHostDAO.countAllHosts());
            return ccTopologyNodeVO;
        } else if (appInfo.getAppType() == AppTypeEnum.APP_SET) {
            // 业务集
            CcTopologyNodeVO ccTopologyNodeVO = new CcTopologyNodeVO();
            ccTopologyNodeVO.setObjectId("biz");
            ccTopologyNodeVO.setObjectId("业务");
            ccTopologyNodeVO.setInstanceId(appId);
            ccTopologyNodeVO.setInstanceName(appInfo.getName());
            ccTopologyNodeVO.setCount((int) applicationHostDAO.countHostsByAppIds(dslContext,
                topologyHelper.getAppSetSubAppIds(appInfo)));
            return ccTopologyNodeVO;
        }
        InstanceTopologyDTO instanceTopology = topologyHelper.getTopologyTreeByApplication(username, appInfo);
        CcTopologyNodeVO ccTopologyNode = TopologyHelper.convertToCcTopologyTree(instanceTopology);
        return ccTopologyNode;
    }

    /**
     * 入参只需objectId与instanceId即可，其余字段可为空
     *
     * @param username
     * @param appId
     * @param treeNodeList
     * @return
     */
    @Override
    public List<AppTopologyTreeNode> getAppTopologyTreeNodeDetail(String username, Long appId,
                                                                  List<AppTopologyTreeNode> treeNodeList) {
        log.info("Input(username={},appId={},treeNodeList={})", username, appId, JsonUtils.toJson(treeNodeList));
        if (treeNodeList == null || treeNodeList.isEmpty()) {
            return Collections.emptyList();
        }
        // 查出业务
        ApplicationInfoDTO appInfo = applicationInfoDAO.getCacheAppInfoById(appId);
        // 查业务拓扑树
        CcClient ccClient = CcClientFactory.getCcClient(JobContextUtil.getUserLang());
        InstanceTopologyDTO appTopologyTree = ccClient.getBizInstTopology(appId, appInfo.getBkSupplierAccount(),
            username);
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
    public List<List<InstanceTopologyDTO>> queryNodePaths(String username, Long appId,
                                                          List<InstanceTopologyDTO> nodeList) {
        // 查出业务
        ApplicationInfoDTO appInfo = applicationInfoDAO.getCacheAppInfoById(appId);
        // 查业务拓扑树
        CcClient ccClient = CcClientFactory.getCcClient(JobContextUtil.getUserLang());
        InstanceTopologyDTO appTopologyTree = ccClient.getBizInstTopology(appId, appInfo.getBkSupplierAccount(),
            username);
        // 搜索路径
        return TopologyHelper.findTopoPaths(appTopologyTree, nodeList);
    }

    /**
     * 入参只需objectId与instanceId即可，其余字段可为空
     *
     * @param username
     * @param appId
     * @param treeNodeList
     * @return
     */
    @Override
    public List<NodeInfoVO> getHostsByNode(String username, Long appId, List<AppTopologyTreeNode> treeNodeList) {
        log.info("Input(username={},appId={},treeNodeList={})", username, appId, JsonUtils.toJson(treeNodeList));
        List<NodeInfoVO> nodeHostInfoList = new ArrayList<>();
        if (treeNodeList == null || treeNodeList.isEmpty()) {
            return nodeHostInfoList;
        }
        // 查出业务
        ApplicationInfoDTO appInfo = applicationInfoDAO.getCacheAppInfoById(appId);
        // 查业务拓扑树
        CcClient ccClient = CcClientFactory.getCcClient(JobContextUtil.getUserLang());
        InstanceTopologyDTO appTopologyTree = ccClient.getBizInstTopology(appId, appInfo.getBkSupplierAccount(),
            username);
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
            List<ApplicationHostInfoDTO> hosts = ccClient.getHosts(appId, conditions);
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
    public List<DynamicGroupInfoDTO> getDynamicGroupList(String username, Long appId) {
        ApplicationInfoDTO applicationInfo = applicationInfoDAO.getCacheAppInfoById(appId);

        Map<String, DynamicGroupInfoDTO> ccGroupInfoMap = new HashMap<>();
        Map<Long, List<String>> appId2GroupIdMap = new HashMap<>();
        if (AppTypeEnum.ALL_APP == applicationInfo.getAppType()) {
            return new ArrayList<>(ccGroupInfoMap.values());
        } else if (AppTypeEnum.APP_SET == applicationInfo.getAppType()) {
            for (long subAppId : applicationInfo.getSubAppIds()) {
                getCustomGroupListByAppId(subAppId, applicationInfo, username, ccGroupInfoMap, appId2GroupIdMap);
            }
        } else {
            getCustomGroupListByAppId(appId, applicationInfo, username, ccGroupInfoMap, appId2GroupIdMap);
        }

        fillAppInfo(ccGroupInfoMap);

        return new ArrayList<>(ccGroupInfoMap.values());
    }

    @Override
    public List<DynamicGroupInfoDTO> getDynamicGroupHostList(String username, Long appId,
                                                             List<String> dynamicGroupIdList) {
        ApplicationInfoDTO applicationInfo = applicationInfoDAO.getCacheAppInfoById(appId);
        String maintainer = applicationInfo.getMaintainers().split("[,;]")[0];

        Map<String, DynamicGroupInfoDTO> ccGroupInfoMap = new HashMap<>();
        Map<Long, List<String>> appId2GroupIdMap = new HashMap<>();
        if (AppTypeEnum.ALL_APP == applicationInfo.getAppType()) {
            return new ArrayList<>(ccGroupInfoMap.values());
        } else if (AppTypeEnum.APP_SET == applicationInfo.getAppType()) {
            for (long subAppId : applicationInfo.getSubAppIds()) {
                getCustomGroupListByAppId(subAppId, applicationInfo, username, ccGroupInfoMap, appId2GroupIdMap);
            }
        } else {
            getCustomGroupListByAppId(appId, applicationInfo, username, ccGroupInfoMap, appId2GroupIdMap);
        }

        for (Map.Entry<Long, List<String>> entry : appId2GroupIdMap.entrySet()) {
            long groupAppId = entry.getKey();
            for (String customerGroupId : entry.getValue()) {
                if (!dynamicGroupIdList.contains(customerGroupId)) {
                    ccGroupInfoMap.remove(customerGroupId);
                    continue;
                }
                List<CcGroupHostPropDTO> ccGroupHostProps = CcClientFactory.getCcClient(JobContextUtil.getUserLang())
                    .getCustomGroupIp(groupAppId, applicationInfo.getBkSupplierAccount(), maintainer, customerGroupId);
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
            List<ApplicationHostInfoDTO> applicationHostInfoDTOList = topologyHelper.getIpStatusListByIps(appId,
                group.getIpList());
            applicationHostInfoDTOList.forEach(applicationHostInfoDTO -> {
                ApplicationHostInfoDTO appHostInfo = applicationHostDAO.getLatestHost(dslContext, appId,
                    applicationHostInfoDTO.getCloudAreaId(), applicationHostInfoDTO.getIp());
                if (appHostInfo != null) {
                    // 填充主机名称与操作系统
                    applicationHostInfoDTO.setHostId(appHostInfo.getHostId());
                    applicationHostInfoDTO.setIpDesc(appHostInfo.getIpDesc());
                    applicationHostInfoDTO.setOs(appHostInfo.getOs());
                }
            });
            group.setIpListStatus(applicationHostInfoDTOList);
        }
        return new ArrayList<>(ccGroupInfoMap.values());
    }

    @Override
    public CcTopologyNodeVO listAppTopologyHostTree(String username, Long appId) {
        StopWatch watch = new StopWatch("listAppTopologyHostTree");
        ApplicationInfoDTO appInfo = getAppInfoById(appId);
        watch.start("listAppTopologyTree");
        CcTopologyNodeVO topologyTree = this.listAppTopologyTree(username, appId);
        watch.stop();
        if (appInfo.getAppType() == AppTypeEnum.NORMAL) {
            watch.start("fillHostInfo");
            fillHostInfo(username, appId, topologyTree, true);
            watch.stop();
        } else {
            topologyTree.setIpListStatus(listHostByBizTopologyNodes(username, appId, Collections.singletonList(
                new AppTopologyTreeNode(
                    "biz",
                    "biz",
                    appId,
                    appInfo.getName(),
                    null
                )
            )));
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

    public void fillHostInfo(String username, Long appId, CcTopologyNodeVO topologyTree, boolean updateAgentStatus) {
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
        List<ApplicationHostInfoDTO> dbHosts = applicationHostDAO.listHostInfoByAppId(appId);
        log.info("find {} hosts from DB", dbHosts.size());
        List<ApplicationHostInfoDTO> hosts = dbHosts;
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
            ApplicationHostInfoDTO host = hosts.get(i);
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

    public void fillAgentStatus(List<ApplicationHostInfoDTO> hosts) {
        // 查出节点下主机与Agent状态
        List<String> ipWithCloudIdList = buildIpList(hosts);
        // 批量设置agent状态
        Map<String, QueryAgentStatusClient.AgentStatus> agentStatusMap =
            queryAgentStatusClient.batchGetAgentStatus(ipWithCloudIdList);
        if (CollectionUtils.isNotEmpty(hosts)) {
            for (ApplicationHostInfoDTO hostInfoDTO : hosts) {
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
    public Long createApp(ApplicationInfoDTO applicationInfoDTO) {
        Long appId = applicationInfoDAO.insertAppInfo(dslContext, applicationInfoDTO);
        try {
            // 创建默认账号
            accountService.createDefaultAccounts(appId);
        } catch (Exception e) {
            log.warn("Fail to create default accounts for appId={}", appId);
        }
        return appId;
    }

    @Override
    public Integer countApps(String username) {
        return applicationInfoDAO.countApps();
    }

    @Override
    public Boolean existsHost(Long appId, String ip) {
        return applicationHostDAO.existsHost(dslContext, appId, ip);
    }

    @Override
    public CcTopologyNodeVO listAppTopologyHostCountTree(String username, Long appId) {
        StopWatch watch = new StopWatch("listAppTopologyHostCountTree");
        ApplicationInfoDTO appInfo = getAppInfoById(appId);
        watch.start("listAppTopologyHostCountTree");
        CcTopologyNodeVO topologyTree = this.listAppTopologyTree(username, appId);
        watch.stop();
        if (appInfo.getAppType() == AppTypeEnum.NORMAL) {
            watch.start("fillHostInfo");
            fillHostInfo(username, appId, topologyTree, false);
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
    public PageData<String> listIPByBizTopologyNodes(String username, Long appId, ListHostByBizTopologyNodesReq req) {
        PageData<HostInfoVO> hostInfoVOResult = listHostByBizTopologyNodes(username, appId, req);
        List<String> data =
            hostInfoVOResult.getData().parallelStream()
                .map(it -> it.getCloudAreaInfo().getId().toString() + ":" + it.getIp())
                .collect(Collectors.toList());
        PageData<String> result = new PageData<>(hostInfoVOResult.getStart(), hostInfoVOResult.getPageSize(),
            hostInfoVOResult.getTotal(), data);
        return result;
    }

    public List<HostInfoVO> getHostInfoVOsByHostInfoDTOs(List<ApplicationHostInfoDTO> hosts) {
        List<HostInfoVO> hostInfoVOList = hosts.stream().map(it -> {
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
        return hostInfoVOList;
    }

    // DB分页
    @Override
    public PageData<HostInfoVO> listHostByBizTopologyNodes(String username, Long appId,
                                                           ListHostByBizTopologyNodesReq req) {
        StopWatch watch = new StopWatch("listHostByBizTopologyNodes");
        watch.start("genConditions");
        String searchContent = req.getSearchContent();
        Integer agentStatus = req.getAgentStatus();
        // 查出业务
        ApplicationInfoDTO appInfo = applicationInfoDAO.getCacheAppInfoById(appId);
        List<Long> moduleIds = null;
        List<Long> appIds = null;
        if (appInfo.getAppType() == AppTypeEnum.NORMAL) {
            // 普通业务需要以moduleIds作为查询条件
            moduleIds = getModuleIdsByTopoNodes(username, appId, req.getAppTopoNodeList());
        } else if (appInfo.getAppType() == AppTypeEnum.APP_SET) {
            // 业务集：仅根据业务查主机
            // 查出对应的所有普通业务
            appIds = topologyHelper.getAppSetSubAppIds(appInfo);
        } else if (appInfo.getAppType() == AppTypeEnum.ALL_APP) {
            // 全业务：仅根据具体的条件查主机
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
        List<ApplicationHostInfoDTO> hosts = applicationHostDAO.listHostInfoBySearchContents(appIds, moduleIds,
            cloudAreaIds, searchContents, agentStatus, pagePair.getLeft(), pagePair.getRight());
        watch.stop();
        watch.start("countHostInfoBySearchContents");
        Long count = applicationHostDAO.countHostInfoBySearchContents(appIds, moduleIds, cloudAreaIds, searchContents
            , agentStatus);
        watch.stop();
        watch.start("getHostInfoVOsByHostInfoDTOs");
        List<HostInfoVO> finalHostInfoVOList = getHostInfoVOsByHostInfoDTOs(hosts);
        watch.stop();
        if (watch.getTotalTimeMillis() > 5000) {
            log.info("listHostByBizTopologyNodes is slow:{}", watch.toString());
        }
        return new PageData<>(pagePair.getLeft().intValue(), pagePair.getRight().intValue(), count,
            finalHostInfoVOList);
    }

    private List<ApplicationHostInfoDTO> filterBySpecifiedCloudId(
        List<CloudIPDTO> cloudIPDTOList,
        List<ApplicationHostInfoDTO> applicationHostInfoDTOList
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
        return applicationHostInfoDTOList.stream().filter(applicationHostInfoDTO -> {
            Set<String> keySet = map.keySet();
            String ip = applicationHostInfoDTO.getIp();
            Long cloudId = applicationHostInfoDTO.getCloudAreaId();
            if (keySet.contains(ip)) {
                return map.get(ip).contains(cloudId);
            }
            return true;
        }).collect(Collectors.toList());
    }

    public List<ApplicationHostInfoDTO> getHostInfoById(String username, Long appId, List<String> ipList) {
        List<ApplicationHostInfoDTO> hostInfoList = new ArrayList<>();
        if (CollectionUtils.isEmpty(ipList)) {
            return hostInfoList;
        }
        ApplicationInfoDTO appInfo = applicationInfoDAO.getCacheAppInfoById(appId);
        if (appInfo == null) {
            return hostInfoList;
        }
        AppTypeEnum appType = appInfo.getAppType();
        if (appType == AppTypeEnum.NORMAL) {
            hostInfoList.addAll(applicationHostDAO.listHostInfoByIps(appId, ipList));
        } else if (appType == AppTypeEnum.APP_SET) {
            List<Long> subAppIds = topologyHelper.getAppSetSubAppIds(appInfo);
            // 直接使用本地缓存数据
            log.debug("subAppIdsSize={}, get host from local db", subAppIds.size());
            hostInfoList.addAll(applicationHostDAO.listHostInfo(subAppIds, ipList));
        } else if (appType == AppTypeEnum.ALL_APP) {
            hostInfoList.addAll(applicationHostDAO.listHostInfoByIps(null, ipList));
        } else {
            log.warn("Not supported AppType:{}", appType.name());
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
                        throw new InternalException(ErrorCode.INTERNAL_ERROR, "every ip in checkIpList must contain " +
                            "cloudAreaId");
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
        separateWhiteIP(inputCloudIPList,inputWhiteIPList,inputNotWhiteIPList,whiteIPMap);
        //使用不带业务信息接口查询白名单IP对应的主机详情
        //仅根据IP查询，查出后再根据指定云区域过滤
        //根据IP查主机（缺少业务信息）本地
        List<ApplicationHostInfoDTO> applicationHostInfoDTOList = applicationHostDAO.listHostInfoByIps(null,
            inputWhiteIPList.stream().map(CloudIPDTO::getIp).collect(Collectors.toList()));

        //根据指定的云区域过滤
        applicationHostInfoDTOList = filterBySpecifiedCloudId(inputWhiteIPList, applicationHostInfoDTOList);

        //2.在当前业务下查IP对应的主机详情
        List<ApplicationHostInfoDTO> hostInfoById = getHostInfoById(username, appId,
            inputNotWhiteIPList.stream().map(CloudIPDTO::getIp).collect(Collectors.toList()));
        //根据指定的云区域过滤
        hostInfoById = filterBySpecifiedCloudId(inputNotWhiteIPList, hostInfoById);
        hostInfoById.addAll(applicationHostInfoDTOList);
        //3.查主机状态
        List<ApplicationHostInfoDTO> hostIpList =
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

    private void getCustomGroupListByAppId(Long appId, ApplicationInfoDTO applicationInfo, String userName,
                                           Map<String, DynamicGroupInfoDTO> ccGroupInfoList,
                                           Map<Long, List<String>> appId2GroupIdMap) {
        List<String> groupIdList = new ArrayList<>();
        List<CcGroupDTO> ccGroupList = CcClientFactory.getCcClient(JobContextUtil.getUserLang())
            .getCustomGroupList(appId, applicationInfo.getBkSupplierAccount(), userName);
        ccGroupList.forEach(ccGroupDTO -> {
            ccGroupInfoList.put(ccGroupDTO.getId(), ccGroupDTO.toDynamicGroupInfo());
            groupIdList.add(ccGroupDTO.getId());
        });
        appId2GroupIdMap.put(appId, groupIdList);
    }

    private void fillAppInfo(Map<String, DynamicGroupInfoDTO> ccGroupInfoMap) {
        // 分组中的获取app信息
        Set<Long> appIdSet = new HashSet<>();
        for (DynamicGroupInfoDTO groupInfo : ccGroupInfoMap.values()) {
            appIdSet.add(groupInfo.getAppId());
        }
        List<ApplicationInfoDTO> appInfoList = applicationInfoDAO.getAppInfoByIds(new ArrayList<>(appIdSet));

        Map<Long, ApplicationInfoDTO> id2AppInfoMap = new HashMap<>(appInfoList.size());
        for (ApplicationInfoDTO appInfo : appInfoList) {
            id2AppInfoMap.put(appInfo.getId(), appInfo);
        }

        for (DynamicGroupInfoDTO groupInfo : ccGroupInfoMap.values()) {
            ApplicationInfoDTO appInfo = id2AppInfoMap.get(groupInfo.getAppId());
            if (appInfo == null) {
                groupInfo.setAppName("");
                groupInfo.setOwner("");
                groupInfo.setOwnerName("");
            } else {
                groupInfo.setAppName(appInfo.getName());
                groupInfo.setOwner(appInfo.getBkSupplierAccount());
                groupInfo.setOwnerName(appInfo.getBkSupplierAccount());
            }
        }
    }

    @Override
    public List<ApplicationInfoDTO> listAllAppsFromLocalDB() {
        return applicationInfoDAO.listAppInfo();
    }

    @Override
    public boolean checkAppPermission(long appId, String username) {
        if (StringUtils.isBlank(username)) {
            return false;
        }
        username = username.trim();
        ApplicationInfoDTO appInfo = applicationInfoDAO.getCacheAppInfoById(appId);
        if (log.isDebugEnabled()) {
            log.debug("Get app info for {}|{}", appId, appInfo);
        }
        if (appInfo == null) {
            log.warn("Check app permission, app[{}] is not exist!", appId);
            return false;
        }
        String maintainers = appInfo.getMaintainers();
        if (log.isDebugEnabled()) {
            log.debug("Checking app {} user {}|{}", appId, username, maintainers);
        }
        if (StringUtils.isNotBlank(maintainers)) {
            for (String maintainer : maintainers.split("[,;]")) {
                if (StringUtils.isBlank(maintainer)) {
                    continue;
                }
                if (log.isDebugEnabled()) {
                    log.debug("Checking...|{}|{}|{}", appId, username, maintainer);
                }
                if (username.equals(maintainer.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public ApplicationInfoDTO getAppInfoById(Long appId) {
        if (appId == null || appId < 0) {
            return null;
        }
        return applicationInfoDAO.getCacheAppInfoById(appId);
    }

    @Override
    public List<HostInfoVO> listHostByBizTopologyNodes(String username, Long appId,
                                                       List<AppTopologyTreeNode> appTopoNodeList) {
        return listHostByBizTopologyNodes(username, appId, appTopoNodeList, null, null);
    }

    private List<Long> getModuleIdsByTopoNodes(String username, Long appId, List<AppTopologyTreeNode> appTopoNodeList) {
        if (appTopoNodeList == null || appTopoNodeList.isEmpty()) {
            return Collections.emptyList();
        }
        CcClient ccClient = CcClientFactory.getCcClient(JobContextUtil.getUserLang());
        // 查出业务
        ApplicationInfoDTO appInfo = applicationInfoDAO.getCacheAppInfoById(appId);
        List<Long> moduleIds = new ArrayList<>();
        if (appInfo.getAppType() == AppTypeEnum.NORMAL) {
            // 普通业务可能根据各级自定义节点查主机，必须先根据拓扑树转为moduleId再查
            // 查业务拓扑树
            InstanceTopologyDTO appTopologyTree = ccClient.getBizInstTopology(appId, appInfo.getBkSupplierAccount(),
                username);
            if (appTopologyTree == null) {
                throw new InternalException(ErrorCode.INTERNAL_ERROR,
                    "Fail to getBizTopo of appId " + appId + " from CMDB");
            }
            for (AppTopologyTreeNode treeNode : appTopoNodeList) {
                CcInstanceDTO ccInstanceDTO = new CcInstanceDTO(treeNode.getObjectId(), treeNode.getInstanceId());
                // 查拓扑节点完整信息
                InstanceTopologyDTO completeNode = TopologyUtil.findNodeFromTopo(appTopologyTree, ccInstanceDTO);
                if (completeNode == null) {
                    log.warn("Cannot find node in topo, node:{}, topo:", JsonUtils.toJson(ccInstanceDTO));
                    TopologyUtil.printTopo(appTopologyTree);
                    throw new InternalException(ErrorCode.INTERNAL_ERROR,
                        "Fail to find node {objectId:" + ccInstanceDTO.getObjectType() + "," +
                            "instanceId:" + ccInstanceDTO.getInstanceId() + ") from app topo");
                }
                moduleIds.addAll(TopologyUtil.findModuleIdsFromTopo(completeNode));
            }
            return moduleIds;
        } else {
            return null;
        }
    }

    private List<HostInfoVO> listHostByBizTopologyNodes(String username, Long appId,
                                                        List<AppTopologyTreeNode> appTopoNodeList, Long start,
                                                        Long limit) {
        if (appTopoNodeList == null || appTopoNodeList.isEmpty()) {
            return Collections.emptyList();
        }
        CcClient ccClient = CcClientFactory.getCcClient(JobContextUtil.getUserLang());
        // 查出业务
        ApplicationInfoDTO appInfo = applicationInfoDAO.getCacheAppInfoById(appId);
        Set<Long> moduleIds = new HashSet<>();
        List<ApplicationHostInfoDTO> hosts = new ArrayList<>();
        if (appInfo.getAppType() == AppTypeEnum.NORMAL) {
            // 普通业务可能根据各级自定义节点查主机，必须先根据拓扑树转为moduleId再查
            // 查业务拓扑树
            InstanceTopologyDTO appTopologyTree = ccClient.getBizInstTopology(appId, appInfo.getBkSupplierAccount(),
                username);
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
        } else if (appInfo.getAppType() == AppTypeEnum.ALL_APP) {
            // 全业务：仅根据业务查主机
            hosts = applicationHostDAO.listAllHostInfo(start, limit);
        } else if (appInfo.getAppType() == AppTypeEnum.APP_SET) {
            // 业务集：仅根据业务查主机
            // 查出对应的所有普通业务
            List<Long> allNormalAppIds = topologyHelper.getAppSetSubAppIds(appInfo);
            hosts = applicationHostDAO.listHostInfoByNormalAppIds(allNormalAppIds, start, limit);
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
        ApplicationInfoDTO applicationInfoDTO = getAppInfoById(appId);
        List<HostInfoVO> hostsByIp = getHostsByIp(username, appId, null, agentStatisticsReq.getIpList());
        log.debug("hostsByIp={}", hostsByIp);
        Set<HostInfoVO> allHostsSet = new HashSet<>();
        allHostsSet.addAll(hostsByIp);
        List<HostInfoVO> hostsByNodes = listHostByBizTopologyNodes(username, appId,
            agentStatisticsReq.getAppTopoNodeList());
        log.debug("hostsByNodes={}", hostsByNodes);
        allHostsSet.addAll(hostsByNodes);
        // 只有普通业务才查动态分组
        if (applicationInfoDTO.getAppType() == AppTypeEnum.NORMAL) {
            List<ApplicationHostInfoDTO> hostDTOsByDynamicGroupIds = new ArrayList<>();
            List<DynamicGroupInfoDTO> dynamicGroupList =
                getDynamicGroupHostList(username, appId, agentStatisticsReq.getDynamicGroupIds());
            dynamicGroupList.forEach(dynamicGroupInfoDTO -> {
                List<ApplicationHostInfoDTO> applicationHostInfoDTOList = dynamicGroupInfoDTO.getIpListStatus();
                if (applicationHostInfoDTOList != null && !applicationHostInfoDTOList.isEmpty()) {
                    hostDTOsByDynamicGroupIds.addAll(applicationHostInfoDTOList);
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
}
