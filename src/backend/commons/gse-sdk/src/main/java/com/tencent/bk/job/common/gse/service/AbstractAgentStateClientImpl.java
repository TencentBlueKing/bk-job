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

import com.tencent.bk.job.common.gse.IGseClient;
import com.tencent.bk.job.common.gse.config.AgentStateQueryConfig;
import com.tencent.bk.job.common.gse.v2.model.req.ListAgentStateReq;
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;
import com.tencent.bk.job.common.util.ConcurrencyUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public abstract class AbstractAgentStateClientImpl implements AgentStateClient {

    private final AgentStateQueryConfig agentStateQueryConfig;
    private final IGseClient gseClient;
    private final ThreadPoolExecutor threadPoolExecutor;

    public AbstractAgentStateClientImpl(AgentStateQueryConfig agentStateQueryConfig,
                                        IGseClient gseClient,
                                        ThreadPoolExecutor threadPoolExecutor) {
        this.agentStateQueryConfig = agentStateQueryConfig;
        this.gseClient = gseClient;
        this.threadPoolExecutor = threadPoolExecutor;
    }

    protected AgentState getAgentState(String agentId) {
        if (StringUtils.isBlank(agentId)) {
            return null;
        }
        ListAgentStateReq req = new ListAgentStateReq();
        req.setAgentIdList(Collections.singletonList(agentId));
        List<AgentState> agentStateList = gseClient.listAgentState(req);
        if (CollectionUtils.isEmpty(agentStateList)) {
            FormattingTuple msg = MessageFormatter.format(
                "cannot find agent state by agentId:{}",
                agentId
            );
            log.warn(msg.getMessage());
            return null;
        } else if (agentStateList.size() > 1) {
            FormattingTuple msg = MessageFormatter.format(
                "multi({}) agent states by agentId:{}, use the first one",
                agentStateList.size(),
                agentId
            );
            log.warn(msg.getMessage());
            return agentStateList.get(0);
        }
        return agentStateList.get(0);
    }

    public Map<String, AgentState> batchGetAgentStateConcurrent(List<String> agentIdList) {
        StopWatch watch = new StopWatch("batchGetAgentStateConcurrent");

        watch.start("splitToBatch");
        Map<String, AgentState> resultMap = new HashMap<>();
        if (CollectionUtils.isEmpty(agentIdList)) {
            return resultMap;
        }

        // 分批
        int batchSize = agentStateQueryConfig.getGseQueryBatchSize();
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
        watch.stop();

        // 并发查询
        watch.start("ConcurrencyUtil.getResultWithThreads");
        Collection<Map<String, AgentState>> maps = ConcurrencyUtil.getResultWithThreads(
            ipSubListList,
            threadPoolExecutor,
            ipList1 -> Collections.singletonList(batchGetAgentStatusWithoutLimit(ipList1))
        );
        watch.stop();

        watch.start("collectResult");
        maps.forEach(resultMap::putAll);
        watch.stop();

        long duration = watch.getTotalTimeMillis();
        FormattingTuple msg = MessageFormatter.format(
            "Get status of {} ips, time consuming: {}ms",
            resultMap.size(),
            duration
        );
        if (duration > 1000L) {
            log.warn(msg.getMessage() + ", statistics: " + watch.prettyPrint());
        } else {
            log.debug(msg.getMessage());
        }
        return resultMap;
    }

    private Map<String, AgentState> batchGetAgentStatusWithoutLimit(List<String> agentIdList) {
        StopWatch watch = new StopWatch("batchGetAgentStatusWithoutLimit");

        watch.start("listAgentState");
        Map<String, AgentState> resultMap = new HashMap<>();
        ListAgentStateReq req = new ListAgentStateReq();
        req.setAgentIdList(agentIdList);
        List<AgentState> agentStateList = gseClient.listAgentState(req);
        watch.stop();

        watch.start("collectResult");
        for (AgentState agentState : agentStateList) {
            resultMap.put(agentState.getAgentId(), agentState);
        }
        watch.stop();

        if (watch.getTotalTimeMillis() > 1000) {
            log.warn("batchGetAgentStatusWithoutLimit slow, statistics: " + watch.prettyPrint());
        }
        return resultMap;
    }
}
