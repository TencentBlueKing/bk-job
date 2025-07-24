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

import java.util.List;
import java.util.Map;

/**
 * 封装Agent状态查询的常用操作：判断真实使用的AgentId、单个主机Agent状态查询、批量状态查询
 */
public interface AgentStateClient {

    /**
     * 获取用于查询Agent状态的AgentId，来源于cloudIp或agentId字段（会按需查询并填充hostAgentStateQueryList中各元素的相关字段）
     *
     * @param hostAgentStateQuery Agent状态查询条件
     * @return 用于查询Agent状态的AgentId
     */
    String getEffectiveAgentId(HostAgentStateQuery hostAgentStateQuery);

    /**
     * 根据agentId获取agent状态（会按需查询并填充hostAgentStateQueryList中各元素的相关字段）
     *
     * @param hostAgentStateQuery Agent状态查询条件
     * @return Agent状态对象
     */
    AgentState getAgentState(HostAgentStateQuery hostAgentStateQuery);

    /**
     * 根据agentId批量获取agent状态（会按需查询并填充hostAgentStateQueryList中各元素的相关字段）
     *
     * @param hostAgentStateQueryList Agent状态查询条件列表
     * @return agentId与Agent状态的Map
     */
    Map<String, AgentState> batchGetAgentState(List<HostAgentStateQuery> hostAgentStateQueryList);

    /**
     * 根据agentId批量获取agent存活状态（会按需查询并填充hostAgentStateQueryList中各元素的相关字段）
     *
     * @param hostAgentStateQueryList Agent状态查询条件列表
     * @return agentId与Agent存活状态的Map
     */
    Map<String, Boolean> batchGetAgentAliveStatus(List<HostAgentStateQuery> hostAgentStateQueryList);
}
