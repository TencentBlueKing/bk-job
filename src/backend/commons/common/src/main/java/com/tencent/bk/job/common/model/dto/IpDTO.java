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
import com.tencent.bk.job.common.util.ip.IpUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

/**
 * 主机IP通用表示-内部服务使用
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class IpDTO implements Cloneable {
    @JsonProperty("cloudAreaId")
    private Long cloudAreaId;

    @JsonProperty("ip")
    private String ip;

    /**
     * agent状态，0-异常，1-正常
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer alive;

    public IpDTO(Long cloudAreaId, String ip) {
        this.cloudAreaId = cloudAreaId;
        this.ip = ip;
    }

    public static IpDTO fromCloudAreaIdAndIpStr(String cloudAreaIdAndIpStr) {
        if (!IpUtils.checkCloudAreaIdAndIpStr(cloudAreaIdAndIpStr)) {
            return null;
        }
        String[] ipProps = cloudAreaIdAndIpStr.split(IpUtils.COLON);
        return new IpDTO(Long.valueOf(ipProps[0]), ipProps[1]);
    }

    public static HostDTO toHost(IpDTO ip) {
        if (ip == null) {
            return null;
        }
        HostDTO host = new HostDTO();
        host.setCloudAreaId(ip.getCloudAreaId());
        host.setIp(ip.getIp());
        return host;
    }

    public String convertToStrIp() {
        return cloudAreaId + ":" + ip;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IpDTO ipDTO = (IpDTO) o;
        return cloudAreaId.equals(ipDTO.cloudAreaId) &&
            ip.equals(ipDTO.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cloudAreaId, ip);
    }

    public IpDTO clone() {
        IpDTO clone = new IpDTO(this.cloudAreaId, this.ip);
        clone.setAlive(alive);
        return clone;
    }
}
