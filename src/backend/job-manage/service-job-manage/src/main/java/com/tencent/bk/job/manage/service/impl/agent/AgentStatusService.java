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

package com.tencent.bk.job.manage.service.impl.agent;

import com.tencent.bk.job.common.gse.constants.AgentAliveStatusEnum;
import com.tencent.bk.job.common.gse.service.AgentStateClient;
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.HostSimpleDTO;
import com.tencent.bk.job.common.util.LogUtil;
import com.tencent.bk.job.manage.model.web.vo.common.AgentStatistics;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Agent状态查询服务
 */
@Slf4j
@Service
public class AgentStatusService {

    private final AgentStateClient agentStateClient;

    @Autowired
    public AgentStatusService(AgentStateClient agentStateClient) {
        this.agentStateClient = agentStateClient;
    }

    /**
     * 为主机填充实时Agent状态
     *
     * @param hosts 主机列表
     */
    public void fillRealTimeAgentStatus(List<ApplicationHostDTO> hosts) {
        // 查出节点下主机与Agent状态
        List<String> agentIdList = ApplicationHostDTO.buildAgentIdList(hosts);
        // 批量设置agent状态
        Map<String, AgentState> agentStateMap;
        try {
            agentStateMap = agentStateClient.batchGetAgentState(agentIdList);
        } catch (Exception e) {
            FormattingTuple msg = MessageFormatter.format(
                "Fail to get agentState by agentIdList:{}",
                LogUtil.buildListLog(agentIdList, 20)
            );
            log.warn(msg.getMessage(), e);
            return;
        }

        if (CollectionUtils.isEmpty(hosts)) {
            return;
        }
        for (ApplicationHostDTO hostInfoDTO : hosts) {
            if (hostInfoDTO == null) {
                continue;
            }
            String agentId = hostInfoDTO.getFinalAgentId();
            AgentState agentState = agentStateMap.get(agentId);
            if (agentState == null) {
                hostInfoDTO.setGseAgentStatus(null);
            } else {
                hostInfoDTO.setGseAgentStatus(agentState.getStatusCode());
            }
        }
    }

    /**
     * 本地主机状态与GSE主机状态做个比较，找到状态不同的主机并返回
     *
     * @param hosts 主机列表
     */
    public List<HostSimpleDTO> findStatusChangedHosts(List<HostSimpleDTO> hosts) {
        List<HostSimpleDTO> statusChangedHosts = new ArrayList<>();
        if (hosts.isEmpty()) return statusChangedHosts;

        List<String> agentIdList = HostSimpleDTO.buildAgentIdList(hosts);
        Map<String, AgentState> agentStateMap;
        try {
            agentStateMap = agentStateClient.batchGetAgentState(agentIdList);
        } catch (Exception e) {
            FormattingTuple msg = MessageFormatter.format(
                "Fail to get agentState by agentIdList:{}",
                LogUtil.buildListLog(agentIdList, 20)
            );
            log.warn(msg.getMessage(), e);
            return statusChangedHosts;
        }

        for (HostSimpleDTO host : hosts) {
            String agentId = host.getFinalAgentId();
            AgentState agentState = agentStateMap.get(agentId);
            AgentAliveStatusEnum agentAliveStatus = AgentAliveStatusEnum.fromAgentState(agentState);
            int agentAliveStatusValue = agentAliveStatus.getStatusValue();
            if (host.getAgentAliveStatus() != agentAliveStatusValue) {
                if (log.isDebugEnabled()) {
                    log.debug("host {} status changed: {}->{}, agentId={}, agentState={}",
                        host.getHostId(),
                        host.getAgentAliveStatus(),
                        agentAliveStatusValue,
                        agentId,
                        agentState
                    );
                }
                host.setAgentAliveStatus(agentAliveStatusValue);
                statusChangedHosts.add(host);
            }
        }
        return statusChangedHosts;
    }

    /**
     * 为主机填充实时Agent状态并给出正常/异常统计数据
     *
     * @param hosts 主机列表
     * @return 主机统计数据
     */
    public AgentStatistics calcAgentStatistics(List<ApplicationHostDTO> hosts) {
        fillRealTimeAgentStatus(hosts);
        int normalNum = 0;
        int abnormalNum = 0;
        for (ApplicationHostDTO it : hosts) {
            Boolean alive = it.getGseAgentAlive();
            if (alive != null && alive) {
                normalNum++;
            } else {
                abnormalNum++;
            }
        }
        return new AgentStatistics(normalNum, abnormalNum);
    }
}
