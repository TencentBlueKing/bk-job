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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.tencent.bk.job.common.annotation.PersistenceObject;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.model.vo.CloudAreaInfoVO;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.common.util.ip.IpUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 主机
 */
@Slf4j
@PersistenceObject
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ApplicationHostDTO {

    /**
     * 主机ID
     */
    private Long hostId;
    /**
     * Job业务ID
     */
    private Long appId;
    /**
     * cmdb业务ID
     */
    private Long bizId;
    /**
     * IPv4(主机的第一个IPv4地址,完整的IPv4地址使用displayIp字段)
     */
    private String ip;
    /**
     * IPv6
     */
    private String ipv6;
    /**
     * AgentId
     */
    private String agentId;

    /**
     * 完整的IPv4地址
     */
    private String displayIp;
    /**
     * 主机名称
     */
    private String hostName;
    /**
     * 主机Agent状态值
     */
    private Integer gseAgentStatus = null;
    /**
     * 主机Agent是否正常
     */
    private Boolean gseAgentAlive = false;
    /**
     * 云区域ID
     */
    private Long cloudAreaId;
    /**
     * 云区域名称
     */
    @JsonIgnore
    private String cloudAreaName;
    /**
     * 云区域+ip
     */
    private String cloudIp;

    /**
     * 操作系统
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
     * 所属云厂商ID
     */
    private String cloudVendorId;

    /**
     * 所属云厂商名称
     */
    private String cloudVendorName;

    /**
     * CMDB中的上次修改时间
     */
    private Long lastTime;

    private String tenantId;

    /**
     * 集群ID
     */
    private List<Long> setId = new ArrayList<>();

    /**
     * 模块id,一个主机可以属于多个模块
     */
    private List<Long> moduleId = new ArrayList<>();

    /**
     * cc的模块类型集合， 选填0,1,2，0所有模块 1普通模块，2DB模块， 支持多个模块
     **/
    private List<Long> moduleType = new ArrayList<>();

    /**
     * IP 列表，搜索用参数
     */
    private List<String> ipList = new ArrayList<>();

    public String getCloudVendorId() {
        if (cloudVendorId != null && cloudVendorId.length() > 64) {
            return cloudVendorId.substring(0, 64);
        }
        return cloudVendorId;
    }

    public void setGseAgentStatus(Integer gseAgentStatus) {
        this.gseAgentStatus = gseAgentStatus;
        this.gseAgentAlive = gseAgentStatus != null && gseAgentStatus == 2;
    }

    public void setGseAgentAlive(Boolean gseAgentAlive) {
        this.gseAgentAlive = gseAgentAlive;
        if (gseAgentAlive != null && gseAgentAlive) {
            // 取值参考AgentStateStatusEnum
            this.gseAgentStatus = 2;
        } else {
            this.gseAgentStatus = -2;
        }
    }

    private Integer getAgentAliveValue() {
        return gseAgentAlive == null ? 0 : (gseAgentAlive ? 1 : 0);
    }

    private static boolean isGseAgentAlive(HostInfoVO hostInfoVO) {
        if (hostInfoVO.getAlive() != null) {
            return hostInfoVO.getAlive() == 1;
        }
        return false;
    }

    public static ApplicationHostDTO fromVO(HostInfoVO hostInfoVO) {
        if (hostInfoVO == null) {
            return null;
        }
        ApplicationHostDTO hostInfoDTO = new ApplicationHostDTO();
        hostInfoDTO.setHostId(hostInfoVO.getHostId());
        hostInfoDTO.setIp(hostInfoVO.getIp());
        hostInfoDTO.setDisplayIp(hostInfoVO.getDisplayIp());
        hostInfoDTO.setHostName(hostInfoVO.getHostName());
        if (hostInfoVO.getAgentStatus() != null) {
            hostInfoDTO.setGseAgentStatus(hostInfoVO.getAgentStatus());
        } else {
            hostInfoDTO.setGseAgentAlive(isGseAgentAlive(hostInfoVO));
        }
        if (hostInfoVO.getCloudArea() != null) {
            hostInfoDTO.setCloudAreaId(hostInfoVO.getCloudArea().getId());
            hostInfoDTO.setCloudAreaName(hostInfoVO.getCloudArea().getName());
        }
        hostInfoDTO.setOsName(hostInfoVO.getOsName());
        hostInfoDTO.setOsTypeName(hostInfoVO.getOsTypeName());
        hostInfoDTO.setAgentId(hostInfoVO.getAgentId());
        hostInfoDTO.setCloudVendorName(hostInfoVO.getCloudVendorName());
        return hostInfoDTO;
    }

    public HostInfoVO toVO() {
        HostInfoVO hostInfoVO = new HostInfoVO();
        hostInfoVO.setHostId(hostId);
        hostInfoVO.setCloudArea(new CloudAreaInfoVO(cloudAreaId, cloudAreaName));
        hostInfoVO.setIp(ip);
        hostInfoVO.setIpv6(ipv6);
        hostInfoVO.setDisplayIp(displayIp);
        hostInfoVO.setHostName(hostName);
        hostInfoVO.setOsName(osName);
        hostInfoVO.setOsTypeName(osTypeName);
        hostInfoVO.setAgentStatus(gseAgentStatus);
        hostInfoVO.setAlive(getAgentAliveValue());
        hostInfoVO.setAgentId(agentId);
        hostInfoVO.setCloudVendorName(cloudVendorName);
        return hostInfoVO;
    }

    @JsonIgnore
    public String getFinalAgentId() {
        if (StringUtils.isNotBlank(agentId)) {
            return agentId;
        }
        return getCloudIp();
    }

    public static List<String> buildAgentIdList(List<ApplicationHostDTO> hosts) {
        List<String> agentIdList = new ArrayList<>();
        for (ApplicationHostDTO host : hosts) {
            agentIdList.add(host.getFinalAgentId());
        }
        return agentIdList;
    }

    @JsonIgnore
    public String getCloudIp() {
        if (StringUtils.isNotBlank(cloudIp)) {
            return cloudIp;
        } else {
            return cloudAreaId + ":" + ip;
        }
    }

    @JsonIgnore
    public String getModuleIdsStr() {
        if (moduleId != null) {
            return moduleId.stream().map(Object::toString).collect(Collectors.joining(","));
        }
        return null;
    }

    @JsonIgnore
    public String getSetIdsStr() {
        if (setId != null) {
            return setId.stream().map(Object::toString).collect(Collectors.joining(","));
        }
        return null;
    }

    @JsonIgnore
    public String getModuleTypeStr() {
        if (moduleType != null) {
            return moduleType.stream().map(Object::toString).collect(Collectors.joining(","));
        }
        return null;
    }

    @JsonIgnore
    public int getAgentStatusValue() {
        if (gseAgentAlive == null || !gseAgentAlive) {
            return JobConstants.GSE_AGENT_STATUS_VALUE_NOT_ALIVE;
        }
        return JobConstants.GSE_AGENT_STATUS_VALUE_ALIVE;
    }

    @JsonIgnore
    public String getHostIdOrCloudIp() {
        if (hostId != null && hostId > 0) {
            return String.valueOf(hostId);
        }
        return getCloudIp();
    }

    public HostDTO toHostDTO() {
        HostDTO host = new HostDTO();
        host.setHostId(hostId);
        host.setBkCloudId(cloudAreaId);
        host.setBkCloudName(cloudAreaName);
        host.setIp(ip);
        host.setIpv6(ipv6);
        host.setAgentId(agentId);
        host.setAlive(getAgentAliveValue());
        host.setOsName(osName);
        host.setOsType(osType);
        host.setOsTypeName(osTypeName);
        host.setHostname(hostName);
        return host;
    }

    public String preferFullIpv6() {
        try {
            if (StringUtils.isNotBlank(ipv6) && IpUtils.checkIpv6(ipv6)) {
                return IpUtils.getFullIpv6ByCompressedOne(ipv6);
            }
        } catch (Exception e) {
            String msg = MessageFormatter.format(
                "Fail to getFullIpv6ByCompressedOne by {}",
                ipv6
            ).getMessage();
            log.warn(msg, e);
        }
        return ipv6;
    }

    /**
     * 获取主机的ip，优先返回ipv4
     *
     * @return 主机ipv4/ipv6, ipv4 优先
     */
    @JsonIgnore
    public String getPrimaryIp() {
        return StringUtils.isNotEmpty(ip) ? ip : ipv6;
    }
}
