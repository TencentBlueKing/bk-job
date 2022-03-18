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

package com.tencent.bk.job.manage.common;

import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.cc.model.TopologyNodeInfoDTO;
import com.tencent.bk.job.common.cc.sdk.CmdbClientFactory;
import com.tencent.bk.job.common.cc.service.CloudAreaService;
import com.tencent.bk.job.common.constant.CcNodeTypeEnum;
import com.tencent.bk.job.common.gse.service.QueryAgentStatusClient;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.DynamicGroupInfoDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.model.vo.CloudAreaInfoVO;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.model.web.vo.CcTopologyNodeVO;
import com.tencent.bk.job.manage.model.web.vo.DynamicGroupInfoVO;
import com.tencent.bk.job.manage.model.web.vo.NodeInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 拓扑信息缓存、更新工具类
 *
 * @since 18/12/2019 11:23
 */
@Slf4j
@Component
public class TopologyHelper {

    private static final BlockingQueue<InstanceTopologyDTO> TOPOLOGY_INFO_QUEUE = new ArrayBlockingQueue<>(10);

    private static final Map<Long, Map<String, Map<Long, String>>> BIZ_NODE_TYPE_NAME_MAP = new ConcurrentHashMap<>();

    private final QueryAgentStatusClient queryAgentStatusClient;
    private final ApplicationDAO applicationDAO;
    private final CloudAreaService cloudAreaService;

    @Autowired
    public TopologyHelper(ApplicationDAO applicationDAO, QueryAgentStatusClient queryAgentStatusClient,
                          CloudAreaService cloudAreaService) {
        this.applicationDAO = applicationDAO;
        this.queryAgentStatusClient = queryAgentStatusClient;
        this.cloudAreaService = cloudAreaService;
    }

    /**
     * 递归遍历查找节点路径
     *
     * @param treeNode
     * @param targetNode
     * @return
     */
    public static List<InstanceTopologyDTO> findTopoPath(
        InstanceTopologyDTO treeNode,
        InstanceTopologyDTO targetNode
    ) {
        if (treeNode == null || targetNode == null) return null;
        if (treeNode.equals(targetNode)) return Collections.singletonList(treeNode);
        List<InstanceTopologyDTO> childList = treeNode.getChild();
        if (childList == null || childList.isEmpty()) return null;
        for (InstanceTopologyDTO childNode : childList) {
            List<InstanceTopologyDTO> subPath = findTopoPath(childNode, targetNode);
            if (subPath != null) {
                List<InstanceTopologyDTO> pathNodeList = new ArrayList<>();
                pathNodeList.add(treeNode);
                pathNodeList.addAll(subPath);
                return pathNodeList;
            }
        }
        return null;
    }

    /**
     * 递归遍历查找多个节点路径
     *
     * @param treeNode
     * @param targetNodes
     * @return
     */
    public static List<List<InstanceTopologyDTO>> findTopoPaths(InstanceTopologyDTO treeNode,
                                                                List<InstanceTopologyDTO> targetNodes) {
        if (treeNode == null || targetNodes == null) return null;
        if (targetNodes.isEmpty()) return Collections.emptyList();
        Map<InstanceTopologyDTO, List<InstanceTopologyDTO>> pathMap = new HashMap<>();
        Set<InstanceTopologyDTO> targetNodeSet = new HashSet<>(targetNodes);
        if (targetNodeSet.contains(treeNode)) {
            List<InstanceTopologyDTO> path = new ArrayList<>();
            path.add(treeNode);
            pathMap.put(treeNode, path);
        }
        if (pathMap.size() == targetNodeSet.size()) {
            return genResult(pathMap, targetNodes);
        }
        List<InstanceTopologyDTO> childList = treeNode.getChild();
        if (childList == null || childList.isEmpty()) return genResult(pathMap, targetNodes);
        for (InstanceTopologyDTO childNode : childList) {
            List<InstanceTopologyDTO> notFoundTargetNodes = new ArrayList<>(targetNodes);
            notFoundTargetNodes.removeAll(pathMap.keySet());
            List<List<InstanceTopologyDTO>> subPathList = findTopoPaths(childNode, notFoundTargetNodes);
            for (List<InstanceTopologyDTO> subPath : subPathList) {
                if (subPath != null) {
                    InstanceTopologyDTO targetNode = subPath.get(subPath.size() - 1);
                    subPath.add(0, treeNode);
                    pathMap.put(targetNode, subPath);
                    if (pathMap.size() == targetNodeSet.size()) {
                        return genResult(pathMap, targetNodes);
                    }
                }
            }
        }
        return genResult(pathMap, targetNodes);
    }

    private static List<List<InstanceTopologyDTO>> genResult(
        Map<InstanceTopologyDTO, List<InstanceTopologyDTO>> pathMap,
        List<InstanceTopologyDTO> targetNodes
    ) {
        List<List<InstanceTopologyDTO>> resultList = new ArrayList<>();
        for (InstanceTopologyDTO targetNode : targetNodes) {
            List<InstanceTopologyDTO> pathList = pathMap.get(targetNode);
            resultList.add(pathList);
        }
        return resultList;
    }

    /**
     * 将 CMDB 返回数据转换成作业平台拓扑结构
     *
     * @param bizInstTopo CMDB 返回数据
     * @return 作业平台拓扑结构
     */
    public static CcTopologyNodeVO convertToCcTopologyTree(InstanceTopologyDTO bizInstTopo) {
        CcTopologyNodeVO node = new CcTopologyNodeVO();
        node.setObjectId(bizInstTopo.getObjectId());
        node.setObjectName(bizInstTopo.getObjectName());
        node.setInstanceId(bizInstTopo.getInstanceId());
        node.setInstanceName(bizInstTopo.getInstanceName());
        // 模块不需要展开,其他的默认展开
        if (CcNodeTypeEnum.MODULE.getType().equals(bizInstTopo.getObjectId())) {
            node.setExpanded(false);
        }
        List<CcTopologyNodeVO> children = new ArrayList<>();
        if (bizInstTopo.getChild() != null && !bizInstTopo.getChild().isEmpty()) {
            for (InstanceTopologyDTO childBizInstTopo : bizInstTopo.getChild()) {
                children.add(convertToCcTopologyTree(childBizInstTopo));
            }
        }
        node.setChild(children);
        return node;
    }

    /**
     * 将作业平台内部主机信息转换为展示用主机信息
     *
     * @param hostInfo 作业平台内主机信息
     * @return 展示用主机信息
     */
    public static HostInfoVO convertToHostInfoVO(ApplicationHostDTO hostInfo) {
        if (hostInfo == null) {
            return null;
        }
        HostInfoVO hostInfoVO = new HostInfoVO();
        hostInfoVO.setHostId(hostInfo.getHostId());
        hostInfoVO.setIp(hostInfo.getIp());
        hostInfoVO.setDisplayIp(hostInfo.getDisplayIp());
        hostInfoVO.setIpDesc(hostInfo.getIpDesc());
        if (hostInfo.getGseAgentAlive() != null) {
            hostInfoVO.setAlive(hostInfo.getGseAgentAlive() ? 1 : 0);
        } else {
            hostInfoVO.setAlive(0);
        }
        hostInfoVO.setCloudAreaInfo(new CloudAreaInfoVO(hostInfo.getCloudAreaId(),
            CloudAreaService.getCloudAreaNameFromCache(hostInfo.getCloudAreaId())));
        hostInfoVO.setOs(hostInfo.getOs());
        return hostInfoVO;
    }

    /**
     * 将作业平台内部拓扑信息转换为展示用拓扑信息
     *
     * @param nodeInfo 作业平台内拓扑结构
     * @return 展示用拓扑结构
     */
    public static NodeInfoVO convertToNodeInfoVO(TopologyNodeInfoDTO nodeInfo) {
        NodeInfoVO nodeInfoVO = new NodeInfoVO();
        nodeInfoVO.setId(nodeInfo.getId());
        nodeInfoVO.setName(nodeInfo.getName());
        nodeInfoVO.setNodeType(nodeInfo.getType().getType());
        nodeInfoVO.setIpList(nodeInfo.getIpList());
        if (CollectionUtils.isNotEmpty(nodeInfo.getIpListStatus())) {
            nodeInfoVO.setIpListStatus(
                nodeInfo.getIpListStatus().parallelStream()
                    .map(TopologyHelper::convertToHostInfoVO).collect(Collectors.toList()));
        }
        return nodeInfoVO;
    }

    /**
     * 将作业平台内部动态分组信息转换为展示用动态分组信息
     *
     * @param dynamicGroupInfoDTO 作业平台内部动态分组信息
     * @return 展示用动态分组信息
     */
    public static DynamicGroupInfoVO convertToDynamicGroupInfoVO(DynamicGroupInfoDTO dynamicGroupInfoDTO) {
        if (dynamicGroupInfoDTO == null) {
            return null;
        }
        DynamicGroupInfoVO dynamicGroupInfoVO = new DynamicGroupInfoVO();
        dynamicGroupInfoVO.setAppId(dynamicGroupInfoDTO.getAppId());
        AppScopeMappingService appScopeMappingService =
            ApplicationContextRegister.getBean(AppScopeMappingService.class);
        ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(dynamicGroupInfoDTO.getAppId());
        dynamicGroupInfoVO.setScopeType(resourceScope.getType().getValue());
        dynamicGroupInfoVO.setScopeId(resourceScope.getId());
        dynamicGroupInfoVO.setAppName(dynamicGroupInfoDTO.getAppName());
        dynamicGroupInfoVO.setId(dynamicGroupInfoDTO.getId());
        dynamicGroupInfoVO.setOwner(dynamicGroupInfoDTO.getOwner());
        dynamicGroupInfoVO.setOwnerName(dynamicGroupInfoDTO.getOwnerName());
        dynamicGroupInfoVO.setName(dynamicGroupInfoDTO.getName());
        dynamicGroupInfoVO.setType(dynamicGroupInfoDTO.getType());
        if (dynamicGroupInfoDTO.getIpListStatus() != null) {
            dynamicGroupInfoVO.setIpListStatus(dynamicGroupInfoDTO.getIpListStatus().parallelStream()
                .map(TopologyHelper::convertToHostInfoVO).collect(Collectors.toList()));
        } else {
            dynamicGroupInfoVO.setIpListStatus(null);
        }
        return dynamicGroupInfoVO;
    }

    @PostConstruct
    private void initCache() {
        TopologyNameCacheThread topologyNameCacheThread = new TopologyNameCacheThread();
        topologyNameCacheThread.start();

    }

    public InstanceTopologyDTO getTopologyTreeByApplication(String username, ApplicationDTO applicationInfo) {
        InstanceTopologyDTO instanceTopology = CmdbClientFactory.getCcClient(JobContextUtil.getUserLang())
            .getBizInstTopology(applicationInfo.getId(), applicationInfo.getBkSupplierAccount(), username);
        if (instanceTopology == null) {
            return null;
        }
        if (StringUtils.isEmpty(instanceTopology.getInstanceName())) {
            instanceTopology.setInstanceName(applicationInfo.getName());
        }
        boolean addFlag = TOPOLOGY_INFO_QUEUE.offer(instanceTopology);
        if (!addFlag) {
            log.warn("Fail to add topologyTree into cache queue");
        }
        return instanceTopology;
    }

    /**
     * 根据拓扑节点 ID 和类型获取节点名称
     *
     * @param username 用户名
     * @param appId    业务 ID
     * @param nodeId   节点 ID
     * @param nodeType 节点类型
     * @return 节点名称
     */
    public String getTopologyNodeName(String username, Long appId, Long nodeId, String nodeType) {
        Map<String, Map<Long, String>> nodeTypeNameMap = BIZ_NODE_TYPE_NAME_MAP.get(appId);
        ApplicationDTO appInfo = applicationDAO.getCacheAppById(appId);
        if (appInfo == null) {
            return String.valueOf(nodeId);
        }
        if (nodeTypeNameMap == null || nodeTypeNameMap.get(nodeType) == null) {
            InstanceTopologyDTO topology = getTopologyTreeByApplication(username, appInfo);
            processTopologyNodeName(topology, null);
        }
        if (CcNodeTypeEnum.BIZ.getType().equals(nodeType)) {
            return appInfo.getName();
        }
        nodeTypeNameMap = BIZ_NODE_TYPE_NAME_MAP.get(appId);
        if (nodeTypeNameMap != null) {
            if (nodeTypeNameMap.get(nodeType) != null) {
                String name = nodeTypeNameMap.get(nodeType).get(nodeId);
                if (StringUtils.isBlank(name)) {
                    name = String.valueOf(nodeId);
                }
                return name;
            }
        }
        return String.valueOf(nodeId);
    }

    public List<Long> getAppSetSubAppIds(ApplicationDTO appInfo) {
        List<Long> subAppIds = appInfo.getSubAppIds();
        Long optDeptId = appInfo.getOperateDeptId();
        if (subAppIds == null || subAppIds.isEmpty() && optDeptId != null) {
            // 使用OperateDeptId
            subAppIds = applicationDAO.getNormalAppIdsByOptDeptId(optDeptId);
        } else {
            // subAppIds与OperateDeptId同时生效
            if (optDeptId != null) {
                subAppIds.addAll(applicationDAO.getNormalAppIdsByOptDeptId(optDeptId));
            }
        }
        // 去重
        subAppIds = new ArrayList<>(new HashSet<>(subAppIds));
        return subAppIds;
    }

    /**
     * 向主机信息中填充云区域名称
     * <p>
     * 不可删除！
     *
     * @param hostInfoList 主机信息列表
     */
    public void fillCloudAreaName(List<HostInfoVO> hostInfoList) {
        if (CollectionUtils.isEmpty(hostInfoList)) {
            return;
        }
        for (HostInfoVO hostInfoVO : hostInfoList) {
            if (hostInfoVO != null) {
                if (hostInfoVO.getCloudAreaInfo() != null) {
                    hostInfoVO.getCloudAreaInfo()
                        .setName(cloudAreaService.getCloudAreaName(hostInfoVO.getCloudAreaInfo().getId()));
                }
            }
        }
    }

    /**
     * 根据 IP 地址列表批量获取机器 Agent 状态
     *
     * @param appId  业务 ID
     * @param ipList IP 地址列表
     * @return 机器 Agent 状态信息列表
     */
    public List<ApplicationHostDTO> getIpStatusListByIps(long appId, List<String> ipList) {
        List<ApplicationHostDTO> ipInfoList = new ArrayList<>();
        if (CollectionUtils.isEmpty(ipList)) {
            return ipInfoList;
        }
        Map<String, QueryAgentStatusClient.AgentStatus> agentStatusMap =
            queryAgentStatusClient.batchGetAgentStatus(ipList);
        for (String ip : ipList) {
            ApplicationHostDTO ipInfo = new ApplicationHostDTO();
            ipInfo.setCloudAreaId(Long.valueOf(ip.split(":")[0]));
            ipInfo.setAppId(appId);
            ipInfo.setIp(ip.split(":")[1]);
            ipInfo.setGseAgentAlive(agentStatusMap.get(ip) != null && (agentStatusMap.get(ip).status == 1));
            ipInfoList.add(ipInfo);
        }
        return ipInfoList;
    }

    /**
     * 更新拓扑节点名称信息缓存
     *
     * @param topology        拓扑节点
     * @param nodeTypeNameMap 待更新的节点名称对应表
     */
    private void processTopologyNodeName(
        InstanceTopologyDTO topology,
        Map<String, Map<Long, String>> nodeTypeNameMap
    ) {
        if (CcNodeTypeEnum.BIZ.getType().equals(topology.getObjectId())) {
            Long appId = topology.getInstanceId();
            if (BIZ_NODE_TYPE_NAME_MAP.get(appId) == null) {
                BIZ_NODE_TYPE_NAME_MAP.put(appId, new ConcurrentHashMap<>(3));
            }
            // Root node, app, find all child
            if (CollectionUtils.isNotEmpty(topology.getChild())) {
                topology.getChild().forEach(child -> processTopologyNodeName(child, BIZ_NODE_TYPE_NAME_MAP.get(appId)));
            }
        } else {
            // Wrong topology tree
            if (nodeTypeNameMap == null) {
                throw new RuntimeException("Uninitialized node type name map!");
            }
            // Check and init node name map for current type
            Map<Long, String> nodeNameMap = nodeTypeNameMap.get(topology.getObjectId());
            if (nodeNameMap == null) {
                nodeNameMap = new ConcurrentHashMap<>(1);
                nodeTypeNameMap.put(topology.getObjectId(), nodeNameMap);
            }
            if (StringUtils.isNotBlank(topology.getInstanceName())) {
                nodeNameMap.put(topology.getInstanceId(), topology.getInstanceName());
            }
            if (CollectionUtils.isNotEmpty(topology.getChild())) {
                topology.getChild().forEach(child -> processTopologyNodeName(child, nodeTypeNameMap));
            }
        }
    }

    class TopologyNameCacheThread extends Thread {
        @Override
        public void run() {
            this.setName("Topology-Name-Update-Thread");
            while (true) {
                try {
                    InstanceTopologyDTO topology = TOPOLOGY_INFO_QUEUE.take();
                    processTopologyNodeName(topology, null);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.error("Error while process topology name!", e);
                }
            }
        }
    }
}
