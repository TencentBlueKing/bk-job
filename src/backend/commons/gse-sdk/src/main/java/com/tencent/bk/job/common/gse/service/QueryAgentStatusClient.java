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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

public interface QueryAgentStatusClient {

    /**
     * 批量获取agent状态
     *
     * @param ips
     * @return Map:key为cloudId:ip，value为AgentStatus，AgentStatus.ip不含云区域Id
     */
    Map<String, AgentStatus> batchGetAgentStatus(List<String> ips);

    /**
     * 获取agent状态
     *
     * @param ip
     * @return
     */
    AgentStatus getAgentStatus(String ip);

    /**
     * 获取agent绑定的ip
     *
     * @param multiIp
     * @param cloudAreaId
     * @return
     */
    String getHostIpByAgentStatus(String multiIp, long cloudAreaId);


    /**
     * 获取agent绑定的ip及Agent状态
     *
     * @param multiIp
     * @param cloudAreaId
     * @return
     */
    Pair<String, Boolean> getHostIpWithAgentStatus(String multiIp, long cloudAreaId);

    /**
     * Agent状态返回结果
     */
    class AgentStatus {
        // 不含云区域Id
        @JsonProperty("ip")
        public String ip;
        @JsonProperty("plat_id")
        public int cloudAreaId;
        @JsonProperty("status")
        public int status;
    }
}
