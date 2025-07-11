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

package com.tencent.bk.job.common.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 主机
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class HostSimpleDTO {

    /**
     * 主机ID
     */
    private Long hostId;
    /**
     * cmdb业务ID
     */
    private Long bizId;
    /**
     * 主机Agent状态，取值参考AgentStatusEnum
     */
    private Integer agentAliveStatus;
    /**
     * 云区域+ip
     */
    private String cloudIp;

    /**
     * AgentId
     */
    private String agentId;

    /**
     * Job业务ID
     */
    private Long appId;

    /**
     * IPv6
     */
    private String ipv6;

    /**
     * 主机名称
     */
    private String hostName;

    /**
     * 操作系统
     */
    private String osName;

    /**
     * 操作系统类型
     */
    private String osType;

    /**
     * IPv4(主机的第一个IPv4地址,完整的IPv4地址使用displayIp字段)
     */
    private String ip;

    /**
     * 云区域ID
     */
    private Long cloudAreaId;

    @JsonIgnore
    public String getFinalAgentId() {
        if (StringUtils.isNotBlank(agentId)) {
            return agentId;
        }
        return cloudIp;
    }

    @JsonIgnore
    public String getHostIdOrCloudIp() {
        if (hostId != null && hostId > 0) {
            return String.valueOf(hostId);
        }
        return cloudIp;
    }

    public static List<String> buildAgentIdList(List<HostSimpleDTO> hosts) {
        List<String> agentIdList = new ArrayList<>();
        for (HostSimpleDTO host : hosts) {
            if (StringUtils.isBlank(host.getIp()) && StringUtils.isBlank(host.getAgentId())) {
                log.warn("buildAgentIdList, ip and agentId is blank, {}", host);
                continue;
            }
            agentIdList.add(host.getFinalAgentId());
        }
        return agentIdList;
    }

    public ApplicationHostDTO convertToHostDTO() {
        ApplicationHostDTO hostDTO = new ApplicationHostDTO();
        hostDTO.setIp(this.getIp());
        hostDTO.setCloudAreaId(this.getCloudAreaId());
        hostDTO.setCloudIp(this.getCloudIp());
        hostDTO.setGseAgentAlive(this.getAgentAliveStatus() == 1);
        hostDTO.setBizId(this.getBizId());
        hostDTO.setHostId(this.getHostId());
        hostDTO.setAgentId(this.getAgentId());
        hostDTO.setAppId(this.getAppId());
        hostDTO.setIpv6(this.getIpv6());
        hostDTO.setHostName(this.getHostName());
        hostDTO.setOsName(this.getOsName());
        hostDTO.setOsType(this.getOsType());
        return hostDTO;
    }
}
