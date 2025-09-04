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

package com.tencent.bk.job.manage.model.inner;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
     * 云区域名称
     */
    private String cloudAreaName;

    /**
     * ipv4
     */
    private String ip;

    /**
     * ipv6
     */
    private String ipv6;

    /**
     * 业务ID
     */
    private Long appId;

    /**
     * CMDB 业务ID
     */
    private Long bizId;

    /**
     * agent存活状态，0-异常，1-正常
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer alive;

    /**
     * 操作系统名称
     */
    private String osName;

    /**
     * 操作系统类型
     */
    private String osType;

    /**
     * 操作系统类型名称
     */
    private String osTypeName;
    /**
     * 主机名称
     */
    private String hostname;

    /**
     * 所属云厂商ID
     */
    private String cloudVendorId;

    /**
     * 所属云厂商名称
     */
    private String cloudVendorName;

    @JsonIgnore
    public String getCloudIp() {
        return cloudAreaId + ":" + ip;
    }

    @JsonIgnore
    public String getHostIdOrCloudIp() {
        if (hostId != null && hostId > 0) {
            return String.valueOf(hostId);
        }
        return cloudAreaId + ":" + ip;
    }

    public static ServiceHostDTO fromApplicationHostDTO(ApplicationHostDTO host) {
        return ServiceHostDTO.builder()
            .bizId(host.getBizId())
            .appId(host.getAppId())
            .hostId(host.getHostId())
            .cloudAreaId(host.getCloudAreaId())
            .cloudAreaName(host.getCloudAreaName())
            .ip(host.getIp())
            .ipv6(host.getIpv6())
            .agentId(host.getAgentId())
            .osName(host.getOsName())
            .osType(host.getOsType())
            .osTypeName(host.getOsTypeName())
            .hostname(host.getHostName())
            .cloudVendorId(host.getCloudVendorId())
            .cloudVendorName(host.getCloudVendorName())
            .build();
    }

    public static HostDTO toHostDTO(ServiceHostDTO serviceHostDTO) {
        if (serviceHostDTO == null) {
            return null;
        }
        HostDTO hostDTO = new HostDTO();
        hostDTO.setHostId(serviceHostDTO.getHostId());
        hostDTO.setAgentId(serviceHostDTO.getAgentId());
        hostDTO.setIp(serviceHostDTO.getIp());
        hostDTO.setIpv6(serviceHostDTO.getIpv6());
        hostDTO.setBkCloudId(serviceHostDTO.getCloudAreaId());
        hostDTO.setBkCloudName(serviceHostDTO.getCloudAreaName());
        hostDTO.setHostname(serviceHostDTO.getHostname());
        hostDTO.setCloudVendorId(serviceHostDTO.getCloudVendorId());
        hostDTO.setCloudVendorName(serviceHostDTO.getCloudVendorName());
        hostDTO.setOsType(serviceHostDTO.getOsType());
        hostDTO.setOsTypeName(serviceHostDTO.getOsTypeName());
        hostDTO.setOsName(serviceHostDTO.getOsName());
        return hostDTO;
    }
}
