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

package com.tencent.bk.job.manage.service.agent.statistics.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.NotImplementedException;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.vo.DynamicGroupIdWithMeta;
import com.tencent.bk.job.manage.model.dto.DynamicGroupDTO;
import com.tencent.bk.job.manage.model.web.vo.common.AgentStatistics;
import com.tencent.bk.job.manage.model.web.vo.ipchooser.DynamicGroupHostStatisticsVO;
import com.tencent.bk.job.manage.service.agent.statistics.ScopeDynamicGroupAgentStatisticsService;
import com.tencent.bk.job.manage.service.host.ScopeDynamicGroupHostService;
import com.tencent.bk.job.manage.service.host.impl.ScopeDynamicGroupHostServiceImpl;
import com.tencent.bk.job.manage.service.impl.BizDynamicGroupService;
import com.tencent.bk.job.manage.service.impl.agent.AgentStatusService;
import com.tencent.bk.job.manage.util.ScopeFeatureUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScopeDynamicGroupAgentStatisticsServiceImpl implements ScopeDynamicGroupAgentStatisticsService {

    private final BizDynamicGroupService bizDynamicGroupService;
    private final ScopeDynamicGroupHostService scopeDynamicGroupHostService;
    private final AgentStatusService agentStatusService;

    @Autowired
    public ScopeDynamicGroupAgentStatisticsServiceImpl(BizDynamicGroupService bizDynamicGroupService,
                                                       ScopeDynamicGroupHostServiceImpl scopeDynamicGroupHostService,
                                                       AgentStatusService agentStatusService) {
        this.bizDynamicGroupService = bizDynamicGroupService;
        this.scopeDynamicGroupHostService = scopeDynamicGroupHostService;
        this.agentStatusService = agentStatusService;
    }

    @Override
    public List<DynamicGroupHostStatisticsVO> getAgentStatisticsByDynamicGroups(
        AppResourceScope appResourceScope,
        List<DynamicGroupIdWithMeta> idWithMetaList
    ) {
        ScopeFeatureUtil.assertOnlyBizSupported(appResourceScope);
        if (CollectionUtils.isEmpty(idWithMetaList)) {
            return Collections.emptyList();
        }
        List<String> idList = idWithMetaList.stream().map(DynamicGroupIdWithMeta::getId).collect(Collectors.toList());
        Long bizId = Long.parseLong(appResourceScope.getId());
        List<DynamicGroupDTO> dynamicGroupList = bizDynamicGroupService.listDynamicGroup(bizId, idList);
        List<DynamicGroupHostStatisticsVO> resultList = new ArrayList<>();
        for (DynamicGroupDTO dynamicGroupDTO : dynamicGroupList) {
            DynamicGroupHostStatisticsVO statisticsVO = new DynamicGroupHostStatisticsVO();
            statisticsVO.setDynamicGroup(dynamicGroupDTO.toBasicVO());
            List<ApplicationHostDTO> hostList = scopeDynamicGroupHostService.listHostByDynamicGroup(
                appResourceScope,
                dynamicGroupDTO.getId()
            );
            AgentStatistics agentStatistics = agentStatusService.calcAgentStatistics(hostList);
            statisticsVO.setAgentStatistics(agentStatistics);
            resultList.add(statisticsVO);
        }
        return resultList;
    }
}
