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

package com.tencent.bk.job.common.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.annotation.PersistenceObject;
import com.tencent.bk.job.common.model.vo.CloudAreaInfoVO;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.common.util.ip.IpUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 主机通用表示-内部服务使用
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
@PersistenceObject
public class HostDTO implements Cloneable {
    /**
     * 主机ID
     */
    @JsonProperty("hostId")
    private Long hostId;

    /**
     * 主机 Agent ID
     */
    @JsonProperty("agentId")
    private String agentId;

    /**
     * 云区域ID
     */
    @JsonProperty("cloudAreaId")
    private Long bkCloudId;

    /**
     * 云区域名称
     */
    @JsonProperty("bkCloudName")
    private String bkCloudName;

    /**
     * 主机IP - ipv4
     */
    @JsonProperty("ip")
    private String ip;

    /**
     * 主机显示IP
     */
    @JsonProperty("displayIp")
    private String displayIp;

    /**
     * 主机IP - ipv6
     */
    @JsonProperty("ipv6")
    private String ipv6;

    /**
     * agent状态，0-异常，1-正常
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer alive;

    public HostDTO(Long bkCloudId, String ip) {
        this.bkCloudId = bkCloudId;
        this.ip = ip;
    }

    public static HostDTO fromHostId(Long hostId) {
        HostDTO hostDTO = new HostDTO();
        hostDTO.setHostId(hostId);
        return hostDTO;
    }

    public static HostDTO fromHostIdAndAgentId(Long hostId, String agentId) {
        HostDTO hostDTO = new HostDTO();
        hostDTO.setHostId(hostId);
        hostDTO.setAgentId(agentId);
        return hostDTO;
    }

    public static HostDTO fromHostIdAndCloudIp(Long hostId, String cloudIp) {
        HostDTO hostDTO = new HostDTO();
        hostDTO.setHostId(hostId);
        if (StringUtils.isNotEmpty(cloudIp)) {
            String[] ipProps = cloudIp.split(IpUtils.COLON);
            hostDTO.setBkCloudId(Long.valueOf(ipProps[0]));
            hostDTO.setIp(ipProps[1]);
        }

        return hostDTO;
    }

    public static HostDTO fromCloudIp(String cloudIp) {
        if (!IpUtils.checkCloudIp(cloudIp)) {
            throw new IllegalArgumentException("Invalid cloudIp : " + cloudIp);
        }
        String[] ipProps = cloudIp.split(IpUtils.COLON);
        return new HostDTO(Long.valueOf(ipProps[0]), ipProps[1]);
    }

    public static HostDTO fromHostIdOrCloudIp(Long hostId, String cloudIp) {
        HostDTO host = new HostDTO();
        host.setHostId(hostId);
        if (StringUtils.isNotEmpty(cloudIp)) {
            String[] ipProps = cloudIp.split(IpUtils.COLON);
            host.setBkCloudId(Long.valueOf(ipProps[0]));
            host.setIp(ipProps[1]);
        }
        return host;
    }


    public static HostDTO fromHostIdOrCloudIp(Long hostId, Long bkCloudId, String ip) {
        HostDTO host = new HostDTO();
        host.setHostId(hostId);
        host.setBkCloudId(bkCloudId);
        host.setIp(ip);
        return host;
    }

    public String toCloudIp() {
        return bkCloudId + ":" + ip;
    }

    public String getDisplayIp() {
        if (StringUtils.isNotEmpty(displayIp)) {
            return displayIp;
        } else {
            return ip;
        }
    }

    public static HostInfoVO toVO(HostDTO hostDTO) {
        if (hostDTO == null) {
            return null;
        }
        HostInfoVO hostInfoVO = new HostInfoVO();
        hostInfoVO.setHostId(hostDTO.getHostId());
        hostInfoVO.setIp(hostDTO.getIp());
        hostInfoVO.setIpv6(hostDTO.getIpv6());
        hostInfoVO.setAlive(hostDTO.getAlive());
        CloudAreaInfoVO cloudAreaInfo = new CloudAreaInfoVO();
        cloudAreaInfo.setId(hostDTO.getBkCloudId());
        cloudAreaInfo.setName(hostDTO.getBkCloudName());
        hostInfoVO.setCloudAreaInfo(cloudAreaInfo);
        return hostInfoVO;
    }

    public static HostDTO fromVO(HostInfoVO hostInfoVO) {
        if (hostInfoVO == null) {
            return null;
        }
        HostDTO hostDTO = new HostDTO();
        hostDTO.setHostId(hostInfoVO.getHostId());
        hostDTO.setIp(hostInfoVO.getIp());
        hostDTO.setIpv6(hostInfoVO.getIpv6());
        hostDTO.setBkCloudId(hostInfoVO.getCloudAreaInfo().getId());
        hostDTO.setBkCloudName(hostInfoVO.getCloudAreaInfo().getName());
        hostDTO.setAlive(hostInfoVO.getAlive());
        return hostDTO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HostDTO hostDTO = (HostDTO) o;
        if (hostId != null) {
            return hostId.equals(hostDTO.getHostId());
        } else {
            // 兼容没有hostId的场景
            return bkCloudId.equals(hostDTO.bkCloudId) &&
                ip.equals(hostDTO.ip);
        }
    }

    @Override
    public int hashCode() {
        if (hostId != null) {
            return Objects.hashCode(hostId);
        } else {
            // 兼容没有hostId的场景
            return Objects.hash(bkCloudId, ip);
        }
    }

    @SuppressWarnings("all")
    public HostDTO clone() {
        HostDTO clone = new HostDTO();
        clone.setHostId(hostId);
        clone.setAgentId(agentId);
        clone.setBkCloudId(bkCloudId);
        clone.setBkCloudName(bkCloudName);
        clone.setIp(ip);
        clone.setIpv6(ipv6);
        clone.setAlive(alive);
        return clone;
    }

    /**
     * 获取最终的agentId，若agentId不存在，则使用cloudIp作为agentId
     *
     * @return 最终的agentId
     */
    public String getFinalAgentId() {
        if (StringUtils.isNotBlank(agentId)) {
            return agentId;
        } else {
            return toCloudIp();
        }
    }
}
