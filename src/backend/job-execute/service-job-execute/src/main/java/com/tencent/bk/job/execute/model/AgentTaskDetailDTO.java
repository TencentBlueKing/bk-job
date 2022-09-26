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

package com.tencent.bk.job.execute.model;

import com.tencent.bk.job.common.model.dto.HostDTO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

/**
 * GSE Agent 任务详情，包含主机的详细信息
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class AgentTaskDetailDTO extends AgentTaskDTO {
    /**
     * 服务器IP,不包含云区域
     */
    private String ip;
    /**
     * 云区域ID
     */
    private Long bkCloudId;
    /**
     * 云区域名称
     */
    private String bkCloudName;
    /**
     * 展示给用户的IP
     */
    private String displayIp;

    public AgentTaskDetailDTO(AgentTaskDTO agentTask) {
        super(agentTask);
        if (StringUtils.isNotEmpty(agentTask.getCloudIp())) {
            HostDTO host = HostDTO.fromCloudIp(agentTask.getAgentId());
            this.ip = host.getIp();
            this.bkCloudId = host.getBkCloudId();
        }
    }

    public String getDisplayIp() {
        if (StringUtils.isNotEmpty(displayIp)) {
            return displayIp;
        } else {
            return ip;
        }
    }

    public HostDTO getHost() {
        HostDTO host = new HostDTO();
        host.setHostId(getHostId());
        host.setIp(getIp());
        host.setBkCloudId(getBkCloudId());
        host.setBkCloudName(getBkCloudName());
        host.setDisplayIp(getDisplayIp());
        host.setAgentId(getAgentId());
        return host;
    }


}
