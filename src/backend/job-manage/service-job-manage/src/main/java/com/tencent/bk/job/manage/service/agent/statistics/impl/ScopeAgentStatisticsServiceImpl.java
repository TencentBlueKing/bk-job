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

package com.tencent.bk.job.manage.service.agent.statistics.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.gse.constants.AgentAliveStatusEnum;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.HostStatusNumStatisticsDTO;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.CurrentTenantHostDAO;
import com.tencent.bk.job.manage.dao.NoTenantHostDAO;
import com.tencent.bk.job.manage.model.web.request.chooser.host.BizTopoNode;
import com.tencent.bk.job.manage.model.web.vo.common.AgentStatistics;
import com.tencent.bk.job.manage.service.agent.statistics.ScopeAgentStatisticsService;
import com.tencent.bk.job.manage.service.host.ScopeDynamicGroupHostService;
import com.tencent.bk.job.manage.service.host.ScopeHostService;
import com.tencent.bk.job.manage.service.host.ScopeTopoHostService;
import com.tencent.bk.job.manage.service.impl.agent.AgentStatusService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class ScopeAgentStatisticsServiceImpl implements ScopeAgentStatisticsService {

    private final ApplicationDAO applicationDAO;
    private final CurrentTenantHostDAO currentTenantHostDAO;
    private final NoTenantHostDAO noTenantHostDAO;
    private final ScopeHostService scopeHostService;
    private final ScopeTopoHostService scopeTopoHostService;
    private final ScopeDynamicGroupHostService scopeDynamicGroupHostService;
    private final AgentStatusService agentStatusService;

    @Autowired
    public ScopeAgentStatisticsServiceImpl(ApplicationDAO applicationDAO,
                                           CurrentTenantHostDAO currentTenantHostDAO,
                                           NoTenantHostDAO noTenantHostDAO,
                                           ScopeHostService scopeHostService,
                                           ScopeTopoHostService scopeTopoHostService,
                                           ScopeDynamicGroupHostService scopeDynamicGroupHostService,
                                           AgentStatusService agentStatusService) {
        this.applicationDAO = applicationDAO;
        this.currentTenantHostDAO = currentTenantHostDAO;
        this.noTenantHostDAO = noTenantHostDAO;
        this.scopeHostService = scopeHostService;
        this.scopeTopoHostService = scopeTopoHostService;
        this.scopeDynamicGroupHostService = scopeDynamicGroupHostService;
        this.agentStatusService = agentStatusService;
    }

    @Override
    public AgentStatistics getAgentStatistics(String tenantId,
                                              AppResourceScope appResourceScope,
                                              List<Long> hostIdList,
                                              List<BizTopoNode> nodeList,
                                              List<String> dynamicGroupIdList) {
        List<ApplicationHostDTO> allHostList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(hostIdList)) {
            List<ApplicationHostDTO> hostsById = scopeHostService.getScopeHostsByIds(
                appResourceScope,
                hostIdList
            );
            log.debug("hostsById={}", hostsById);
            allHostList.addAll(hostsById);
        }
        if (CollectionUtils.isNotEmpty(nodeList)) {
            List<ApplicationHostDTO> hostsByNode = new ArrayList<>(
                scopeTopoHostService.listHostByNodes(appResourceScope, nodeList)
            );
            log.debug("hostsByNode={}", hostsByNode);
            allHostList.addAll(hostsByNode);
        }
        if (CollectionUtils.isNotEmpty(dynamicGroupIdList)) {
            List<ApplicationHostDTO> hostsByDynamicGroup = new ArrayList<>();
            for (String id : dynamicGroupIdList) {
                hostsByDynamicGroup.addAll(
                    scopeDynamicGroupHostService.listHostByDynamicGroup(
                        tenantId,
                        appResourceScope,
                        id
                    )
                );
            }
            log.debug("hostsByDynamicGroup={}", hostsByDynamicGroup);
            allHostList.addAll(hostsByDynamicGroup);
        }
        return agentStatusService.calcAgentStatistics(allHostList);
    }

    @Override
    public AgentStatistics getAgentStatistics(AppResourceScope appResourceScope) {
        // 查出业务
        ApplicationDTO appInfo = applicationDAO.getAppById(appResourceScope.getAppId());
        int aliveCount = 0;
        int notAliveCount = 0;
        List<Long> bizIds;
        List<HostStatusNumStatisticsDTO> statisticsDTOS;
        if (appInfo.isBiz()) {
            // 普通业务
            bizIds = Collections.singletonList(Long.valueOf(appResourceScope.getId()));
            statisticsDTOS = currentTenantHostDAO.countHostStatusNumByBizIds(bizIds);
        } else if (appInfo.isAllBizSet()) {
            // 全业务
            statisticsDTOS = currentTenantHostDAO.countHostStatusNumByBizIds(null);
        } else if (appInfo.isBizSet()) {
            // 业务集
            bizIds = appInfo.getSubBizIds();
            statisticsDTOS = currentTenantHostDAO.countHostStatusNumByBizIds(bizIds);
        } else if (appInfo.isAllTenantSet()) {
            // 全租户
            statisticsDTOS = noTenantHostDAO.countHostStatusNumByBizIds(null);
        } else {
            throw new InternalException("Ilegal appInfo:" + appInfo, ErrorCode.INTERNAL_ERROR);
        }
        for (HostStatusNumStatisticsDTO statisticsDTO : statisticsDTOS) {
            if (statisticsDTO.getGseAgentAlive() == AgentAliveStatusEnum.ALIVE.getStatusValue()) {
                aliveCount += statisticsDTO.getHostNum();
            } else {
                notAliveCount += statisticsDTO.getHostNum();
            }
        }
        return new AgentStatistics(aliveCount, notAliveCount);
    }
}
