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

import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.vo.TargetNodeVO;
import com.tencent.bk.job.manage.model.web.request.chooser.host.BizTopoNode;
import com.tencent.bk.job.manage.model.web.vo.common.AgentStatistics;
import com.tencent.bk.job.manage.model.web.vo.ipchooser.NodeHostStatisticsVO;
import com.tencent.bk.job.manage.service.agent.statistics.ScopeNodeAgentStatisticsService;
import com.tencent.bk.job.manage.service.host.BizTopoHostService;
import com.tencent.bk.job.manage.service.impl.agent.AgentStatusService;
import com.tencent.bk.job.manage.util.ScopeFeatureUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ScopeNodeAgentStatisticsServiceImpl implements ScopeNodeAgentStatisticsService {

    private final BizTopoHostService bizTopoHostService;
    private final AgentStatusService agentStatusService;

    @Autowired
    public ScopeNodeAgentStatisticsServiceImpl(BizTopoHostService bizTopoHostService,
                                               AgentStatusService agentStatusService) {
        this.bizTopoHostService = bizTopoHostService;
        this.agentStatusService = agentStatusService;
    }

    @Override
    public List<NodeHostStatisticsVO> getAgentStatisticsByNodes(AppResourceScope appResourceScope,
                                                                List<TargetNodeVO> nodeList) {
        ScopeFeatureUtil.assertOnlyBizSupported(appResourceScope);
        if (CollectionUtils.isEmpty(nodeList)) {
            return Collections.emptyList();
        }
        List<NodeHostStatisticsVO> resultList = new ArrayList<>();
        // TODO:性能优化
        for (TargetNodeVO node : nodeList) {
            List<ApplicationHostDTO> hostList = bizTopoHostService.listHostByNode(
                Long.parseLong(appResourceScope.getId()),
                BizTopoNode.fromTargetNodeVO(node)
            );
            AgentStatistics agentStatistics = agentStatusService.calcAgentStatistics(hostList);
            resultList.add(new NodeHostStatisticsVO(node, agentStatistics));
        }
        return resultList;
    }
}
