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

package com.tencent.bk.job.manage.model.inner;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

/**
 * 主机
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceHostDTO {
    /**
     * 主机ID
     */
    private Long hostId;

    /**
     * AgentID
     */
    private String agentId;

    /**
     * 云区域ID
     */
    private Long cloudAreaId;

    /**
     * ip
     */
    private String ip;

    /**
     * 业务ID
     */
    private Long appId;

    /**
     * CMDB 业务ID
     */
    private Long bizId;

    @JsonIgnore
    public String getCloudIp() {
        return cloudAreaId + ":" + ip;
    }

    @JsonIgnore
    public String getFinalAgentId() {
        if (StringUtils.isNotBlank(agentId)) {
            return agentId;
        }
        return getCloudIp();
    }

    public static ServiceHostDTO fromApplicationHostDTO(ApplicationHostDTO host) {
        return ServiceHostDTO.builder()
            .bizId(host.getBizId())
            .appId(host.getAppId())
            .hostId(host.getHostId())
            .cloudAreaId(host.getCloudAreaId())
            .ip(host.getIp())
            .agentId(host.getAgentId())
            .build();
    }
}
