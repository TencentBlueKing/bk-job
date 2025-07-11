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

import com.tencent.bk.job.common.gse.service.model.HostAgentStateQuery;
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;
import com.tencent.bk.job.common.util.feature.FeatureIdConstants;
import com.tencent.bk.job.common.util.feature.FeatureToggle;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 根据动态变化的特性配置自动选择应当使用的AgentStateClient
 */
@Slf4j
public class AutoChoosingAgentStateClientImpl implements AgentStateClient {

    private final AgentStateClient preferV2AgentStateClient;
    private final AgentStateClient useV2ByFeatureAgentStateClient;

    public AutoChoosingAgentStateClientImpl(AgentStateClient preferV2AgentStateClient,
                                            AgentStateClient useV2ByFeatureAgentStateClient) {
        this.preferV2AgentStateClient = preferV2AgentStateClient;
        this.useV2ByFeatureAgentStateClient = useV2ByFeatureAgentStateClient;
    }

    @Override
    public String getEffectiveAgentId(HostAgentStateQuery hostAgentStateQuery) {
        AgentStateClient agentStateClient = chooseAgentStateByFeatureConfig();
        return agentStateClient.getEffectiveAgentId(hostAgentStateQuery);
    }

    @Override
    public AgentState getAgentState(HostAgentStateQuery hostAgentStateQuery) {
        AgentStateClient agentStateClient = chooseAgentStateByFeatureConfig();
        return agentStateClient.getAgentState(hostAgentStateQuery);
    }

    @Override
    public Map<String, AgentState> batchGetAgentState(List<HostAgentStateQuery> hostAgentStateQueryList) {
        AgentStateClient agentStateClient = chooseAgentStateByFeatureConfig();
        return agentStateClient.batchGetAgentState(hostAgentStateQueryList);
    }

    @Override
    public Map<String, Boolean> batchGetAgentAliveStatus(List<HostAgentStateQuery> hostAgentStateQueryList) {
        AgentStateClient agentStateClient = chooseAgentStateByFeatureConfig();
        return agentStateClient.batchGetAgentAliveStatus(hostAgentStateQueryList);
    }

    private AgentStateClient chooseAgentStateByFeatureConfig() {
        AgentStateClient agentStateClient;
        if (FeatureToggle.isFeatureEnabled(FeatureIdConstants.FEATURE_AGENT_STATUS_GSE_V2)) {
            agentStateClient = useV2ByFeatureAgentStateClient;
        } else {
            agentStateClient = preferV2AgentStateClient;
        }
        return agentStateClient;
    }
}
