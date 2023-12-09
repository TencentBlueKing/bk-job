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
import com.tencent.bk.job.common.gse.service.model.HostAgentStateQuery;
import com.tencent.bk.job.common.gse.util.AgentStateUtil;
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
public abstract class SingleChannelAgentStateClientImpl extends AbstractAgentStateClientImpl {

    public SingleChannelAgentStateClientImpl(AgentStateQueryConfig agentStateQueryConfig,
                                             IGseClient gseClient,
                                             ThreadPoolExecutor threadPoolExecutor) {
        super(agentStateQueryConfig, gseClient, threadPoolExecutor);
    }

    @Override
    public AgentState getAgentState(HostAgentStateQuery hostAgentStateQuery) {
        String finalAgentId = getEffectiveAgentId(hostAgentStateQuery);
        return getAgentState(finalAgentId);
    }

    @Override
    public Map<String, AgentState> batchGetAgentState(List<HostAgentStateQuery> hostAgentStateQueryList) {
        List<String> queryAgentIds = hostAgentStateQueryList.stream()
            .map(this::getEffectiveAgentId)
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toList());

        return batchGetAgentStateConcurrent(queryAgentIds);
    }

    @Override
    public Map<String, Boolean> batchGetAgentAliveStatus(List<HostAgentStateQuery> hostAgentStateQueryList) {
        Map<String, AgentState> agentStateMap = batchGetAgentState(hostAgentStateQueryList);
        return AgentStateUtil.batchGetAgentAliveStatus(agentStateMap);
    }
}
