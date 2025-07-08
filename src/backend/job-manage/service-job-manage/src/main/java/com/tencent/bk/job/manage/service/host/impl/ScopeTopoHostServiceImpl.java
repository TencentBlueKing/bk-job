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

package com.tencent.bk.job.manage.service.host.impl;

import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.cc.sdk.BkNetClient;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.manage.common.TopologyHelper;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.model.web.vo.CcTopologyNodeVO;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.host.ScopeTopoHostService;
import com.tencent.bk.job.manage.service.impl.agent.AgentStatusService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("SameParameterValue")
@Slf4j
@Service
public class ScopeTopoHostServiceImpl implements ScopeTopoHostService {

    private final ApplicationHostDAO applicationHostDAO;
    private final ApplicationService applicationService;
    private final HostTopoDAO hostTopoDAO;
    private final TopologyHelper topologyHelper;
    private final AgentStatusService agentStatusService;
    private final MessageI18nService i18nService;

    @Autowired
    public ScopeTopoHostServiceImpl(ApplicationHostDAO applicationHostDAO,
                                    ApplicationService applicationService,
                                    HostTopoDAO hostTopoDAO,
                                    TopologyHelper topologyHelper,
                                    AgentStatusService agentStatusService,
                                    MessageI18nService i18nService) {
        this.applicationHostDAO = applicationHostDAO;
        this.applicationService = applicationService;
        this.hostTopoDAO = hostTopoDAO;
        this.topologyHelper = topologyHelper;
        this.agentStatusService = agentStatusService;
        this.i18nService = i18nService;
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
            fillHostInfo(Long.valueOf(appResourceScope.getId()), topologyTree, false);
            watch.stop();
            watch.start("clearHosts");
            clearHosts(topologyTree);
            watch.stop();
        }
        log.debug(watch.toString());
        return topologyTree;
    }

    private CcTopologyNodeVO fillObjInfoForNode(ApplicationDTO appInfo, CcTopologyNodeVO node) {
        if (appInfo.isBiz()) {
            node.setObjectId("biz");
            node.setObjectName(i18nService.getI18n("cmdb.object.name.biz"));
        } else if (appInfo.isBizSet()) {
            node.setObjectId("biz_set");
            node.setObjectName(i18nService.getI18n("cmdb.object.name.biz_set"));
        }
        return node;
    }

    public CcTopologyNodeVO listAppTopologyTree(String username, AppResourceScope appResourceScope) {
        ApplicationDTO appInfo = applicationService.getAppByAppId(appResourceScope.getAppId());
        if (appInfo == null) {
            throw new InvalidParamException(ErrorCode.WRONG_APP_ID);
        }
        CcTopologyNodeVO ccTopologyNodeVO = fillObjInfoForNode(appInfo, new CcTopologyNodeVO());
        ccTopologyNodeVO.setInstanceId(Long.valueOf(appResourceScope.getId()));
        ccTopologyNodeVO.setInstanceName(appInfo.getName());
        if (appInfo.isAllBizSet()) {
            // 全业务
            ccTopologyNodeVO.setCount((int) applicationHostDAO.countAllHosts());
            return ccTopologyNodeVO;
        } else if (appInfo.isBizSet()) {
            // 业务集
            ccTopologyNodeVO.setCount(
                (int) applicationHostDAO.countHostsByBizIds(topologyHelper.getBizSetSubBizIds(appInfo))
            );
            return ccTopologyNodeVO;
        }
        InstanceTopologyDTO instanceTopology = topologyHelper.getTopologyTreeByApplication(appInfo);
        return TopologyHelper.convertToCcTopologyTree(instanceTopology);
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
                    hosts.stream().map(HostInfoVO::getHostId).collect(Collectors.toSet()));
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
        }
        topologyTree.setIpList(null);
        topologyTree.setIpListStatus(null);
    }

    public void fillHostInfo(Long bizId, CcTopologyNodeVO topologyTree, boolean updateAgentStatus) {
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
        //批量设置agent状态
        if (updateAgentStatus) {
            watch.start("fillRealTimeAgentStatus");
            agentStatusService.fillRealTimeAgentStatus(dbHosts);
            watch.stop();
        }
        //填充云区域名称
        fillCloudAreaName(dbHosts);
        //将主机挂载到topo树
        setHostsToTopoTree(bizId, dbHosts, topologyTree, map, watch);
    }

    private void setHostsToTopoTree(Long bizId,
                                    List<ApplicationHostDTO> dbHosts,
                                    CcTopologyNodeVO topologyTree,
                                    Map<Long, CcTopologyNodeVO> map,
                                    StopWatch watch) {
        List<HostInfoVO> hostInfoVOList = new ArrayList<>();
        for (ApplicationHostDTO dbHost : dbHosts) {
            hostInfoVOList.add(dbHost.toVO());
        }
        watch.start("setToTopoTree");
        List<Pair<Long, Long>> hostModuleIdPairList = hostTopoDAO.listHostIdAndModuleIdByBizId(bizId);
        Map<Long, List<Long>> hostIdModuleMap = new HashMap<>();
        hostModuleIdPairList.forEach(pair -> {
            List<Long> moduleIdList = hostIdModuleMap.computeIfAbsent(pair.getLeft(), aLong -> new ArrayList<>());
            moduleIdList.add(pair.getRight());
        });
        for (int i = 0; i < hostInfoVOList.size(); i++) {
            ApplicationHostDTO host = dbHosts.get(i);
            HostInfoVO hostInfoVO = hostInfoVOList.get(i);
            List<Long> moduleIdList = hostIdModuleMap.get(host.getHostId());
            if (CollectionUtils.isNotEmpty(moduleIdList)) {
                moduleIdList.forEach(moduleId -> {
                    CcTopologyNodeVO moduleNode = map.get(moduleId);
                    if (moduleNode == null) {
                        log.warn("cannot find moduleNode in topoTree, cache may expire, ignore this moduleNode");
                    } else {
                        moduleNode.getIpListStatus().add(hostInfoVO);
                    }
                });
            } else {
                log.info("No moduleId found for host:{}, ignore", host);
            }
        }
        watch.stop();
        watch.start("countHosts");
        countHosts(topologyTree);
        watch.stop();
        if (watch.getTotalTimeMillis() > 4000) {
            log.warn("PERF:SLOW:fillHostInfo: {}", watch.toString());
        } else {
            if (log.isDebugEnabled()) {
                log.debug("fillHostInfo: {}", watch.toString());
            }
        }
    }

    private void fillCloudAreaName(List<ApplicationHostDTO> hostDTOList) {
        hostDTOList.forEach(hostDTO ->
            hostDTO.setCloudAreaName(BkNetClient.getCloudAreaNameFromCache(hostDTO.getCloudAreaId()))
        );
    }
}
