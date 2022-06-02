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

import com.tencent.bk.job.common.gse.service.QueryAgentStatusClient;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Agent状态查询服务
 */
@Slf4j
@Service
public class AgentStatusService {

    private final QueryAgentStatusClient queryAgentStatusClient;

    @Autowired
    public AgentStatusService(QueryAgentStatusClient queryAgentStatusClient) {
        this.queryAgentStatusClient = queryAgentStatusClient;
    }

    /**
     * 为主机填充实时Agent状态
     *
     * @param hosts 主机列表
     */
    public void fillRealTimeAgentStatus(List<ApplicationHostDTO> hosts) {
        // 查出节点下主机与Agent状态
        List<String> ipWithCloudIdList = ApplicationHostDTO.buildIpList(hosts);
        // 批量设置agent状态
        Map<String, QueryAgentStatusClient.AgentStatus> agentStatusMap =
            queryAgentStatusClient.batchGetAgentStatus(ipWithCloudIdList);
        if (CollectionUtils.isEmpty(hosts)) {
            return;
        }
        for (ApplicationHostDTO hostInfoDTO : hosts) {
            if (hostInfoDTO == null) {
                continue;
            }
            String ip = hostInfoDTO.getCloudAreaId() + ":" + hostInfoDTO.getIp();
            QueryAgentStatusClient.AgentStatus agentStatus = agentStatusMap.get(ip);
            hostInfoDTO.setGseAgentAlive(agentStatus != null && agentStatus.status == 1);
        }
    }
}
