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

package com.tencent.bk.job.common.gse.service;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.gse.GseClient;
import com.tencent.bk.job.common.gse.config.AgentStateQueryConfig;
import com.tencent.bk.job.common.gse.constants.AgentStatusEnum;
import com.tencent.bk.job.common.gse.v2.model.req.ListAgentStateReq;
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;
import com.tencent.bk.job.common.util.ConcurrencyUtil;
import com.tencent.bk.job.common.util.LogUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AgentStateClientImpl implements AgentStateClient {

    private final AgentStateQueryConfig agentStateQueryConfig;
    private final GseClient gseClient;

    @Autowired
    public AgentStateClientImpl(AgentStateQueryConfig agentStateQueryConfig, GseClient gseClient) {
        this.agentStateQueryConfig = agentStateQueryConfig;
        this.gseClient = gseClient;
    }

    @Override
    public AgentState getAgentState(String agentId) {
        ListAgentStateReq req = new ListAgentStateReq();
        req.setAgentIdList(Collections.singletonList(agentId));
        List<AgentState> agentStateList = gseClient.listAgentState(req);
        if (CollectionUtils.isEmpty(agentStateList)) {
            FormattingTuple msg = MessageFormatter.format(
                "cannot find agent state by agentId:{}",
                agentId
            );
            throw new InternalException(ErrorCode.GSE_API_DATA_ERROR, new String[]{msg.getMessage()});
        } else if (agentStateList.size() > 1) {
            FormattingTuple msg = MessageFormatter.format(
                "multi({}) agent states by agentId:{}",
                agentStateList.size(),
                agentId
            );
            throw new InternalException(ErrorCode.GSE_API_DATA_ERROR, new String[]{msg.getMessage()});
        }
        return agentStateList.get(0);
    }

    @Override
    public boolean getAgentAliveStatus(String agentId) {
        AgentState agentState = getAgentState(agentId);
        return AgentStatusEnum.fromAgentState(agentState) == AgentStatusEnum.ALIVE;
    }

    @Override
    public String chooseOneAgentIdPreferAlive(List<String> agentIdList) {
        if (CollectionUtils.isEmpty(agentIdList)) {
            throw new InternalException("agentIdList cannot be empty", ErrorCode.INTERNAL_ERROR);
        }
        try {
            Map<String, Boolean> map = batchGetAgentAliveStatus(agentIdList);
            for (Map.Entry<String, Boolean> entry : map.entrySet()) {
                String agentId = entry.getKey();
                Boolean alive = entry.getValue();
                if (alive) {
                    return agentId;
                }
            }
        } catch (Exception e) {
            FormattingTuple msg = MessageFormatter.format(
                "Fail to get agentState by agentIdList:{}",
                LogUtil.buildListLog(agentIdList, 20)
            );
            log.warn(msg.getMessage(), e);
        }
        return agentIdList.get(0);
    }

    @Override
    public Map<String, AgentState> batchGetAgentState(List<String> agentIdList) {
        long startTime = System.currentTimeMillis();
        Map<String, AgentState> resultMap = new HashMap<>();
        // 分批
        int batchSize = agentStateQueryConfig.getGseQueryBatchSize();
        int threadNum = agentStateQueryConfig.getGseQueryThreadsNum();
        int start = 0;
        int end;
        int size = agentIdList.size();
        List<List<String>> ipSubListList = new ArrayList<>();
        while (start < size) {
            end = start + batchSize;
            end = Math.min(end, size);
            List<String> ipSubList = agentIdList.subList(start, end);
            ipSubListList.add(ipSubList);
            start += batchSize;
        }
        // 并发查询
        Collection<Map<String, AgentState>> maps = ConcurrencyUtil.getResultWithThreads(ipSubListList, threadNum,
            ipList1 -> Collections.singletonList(batchGetAgentStatusWithoutLimit(ipList1)));
        maps.forEach(resultMap::putAll);
        long duration = System.currentTimeMillis() - startTime;
        FormattingTuple msg = MessageFormatter.format(
            "Get status of {} ips, time consuming: {}ms",
            resultMap.size(),
            duration
        );
        if (duration > 1000L) {
            log.warn(msg.getMessage());
        } else {
            log.debug(msg.getMessage());
        }
        return resultMap;
    }

    @Override
    public Map<String, Boolean> batchGetAgentAliveStatus(List<String> agentIdList) {
        Map<String, AgentState> agentStateMap = batchGetAgentState(agentIdList);
        Map<String, Boolean> agentAliveStatusMap = new HashMap<>();
        for (Map.Entry<String, AgentState> entry : agentStateMap.entrySet()) {
            String agentId = entry.getKey();
            AgentState agentState = entry.getValue();
            agentAliveStatusMap.put(agentId, AgentStatusEnum.fromAgentState(agentState) == AgentStatusEnum.ALIVE);
        }
        return agentAliveStatusMap;
    }

    public Map<String, AgentState> batchGetAgentStatusWithoutLimit(List<String> agentIdList) {
        Map<String, AgentState> resultMap = new HashMap<>();
        ListAgentStateReq req = new ListAgentStateReq();
        req.setAgentIdList(agentIdList);
        List<AgentState> agentStateList = gseClient.listAgentState(req);
        for (AgentState agentState : agentStateList) {
            resultMap.put(agentState.getAgentId(), agentState);
        }
        return resultMap;
    }
}
