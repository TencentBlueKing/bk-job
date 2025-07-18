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

package com.tencent.bk.job.common.gse.service;

import com.tencent.bk.job.common.gse.GseClient;
import com.tencent.bk.job.common.gse.config.AgentStateQueryConfig;
import com.tencent.bk.job.common.gse.service.model.HostAgentStateQuery;
import com.tencent.bk.job.common.gse.util.AgentStateUtil;
import com.tencent.bk.job.common.gse.util.AgentUtils;
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
public class PreferV2AgentStateClientImpl extends AbstractAgentStateClientImpl {

    public PreferV2AgentStateClientImpl(AgentStateQueryConfig agentStateQueryConfig,
                                        GseClient gseClient,
                                        ThreadPoolExecutor threadPoolExecutor) {
        super(agentStateQueryConfig, gseClient, threadPoolExecutor);
    }

    @Override
    public String getEffectiveAgentId(HostAgentStateQuery hostAgentStateQuery) {
        String agentId = hostAgentStateQuery.getAgentId();
        if (StringUtils.isNotBlank(agentId)) {
            return agentId;
        }
        return hostAgentStateQuery.getCloudIp();
    }

    @Override
    public AgentState getAgentState(HostAgentStateQuery hostAgentStateQuery) {
        String finalAgentId = getEffectiveAgentId(hostAgentStateQuery);
        if (StringUtils.isBlank(finalAgentId)) {
            return null;
        }
        return getAgentState(finalAgentId);
    }

    @Override
    public Map<String, AgentState> batchGetAgentState(List<HostAgentStateQuery> hostAgentStateQueryList) {
        // 对agentId按照对应的GSE Agent 版本进行分类
        List<String> queryAgentIds = hostAgentStateQueryList.stream()
            .map(this::getEffectiveAgentId)
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toList());
        Pair<List<String>, List<String>> classifiedAgentIdList = classifyGseAgentIds(queryAgentIds);

        Map<String, AgentState> results = batchGetAgentStateConcurrent(classifiedAgentIdList.getLeft());
        results.putAll(batchGetAgentStateConcurrent(classifiedAgentIdList.getRight()));
        return results;
    }

    private Pair<List<String>, List<String>> classifyGseAgentIds(List<String> agentIdList) {
        List<String> v1AgentIdList = new ArrayList<>();
        List<String> v2AgentIdList = new ArrayList<>();
        agentIdList.forEach(agentId -> {
            if (AgentUtils.isGseV1AgentId(agentId)) {
                v1AgentIdList.add(agentId);
            } else {
                v2AgentIdList.add(agentId);
            }
        });
        return Pair.of(v1AgentIdList, v2AgentIdList);
    }

    @Override
    public Map<String, Boolean> batchGetAgentAliveStatus(List<HostAgentStateQuery> hostAgentStateQueryList) {
        Map<String, AgentState> agentStateMap = batchGetAgentState(hostAgentStateQueryList);
        return AgentStateUtil.batchGetAgentAliveStatus(agentStateMap);
    }
}
