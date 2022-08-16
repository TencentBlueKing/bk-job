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

package com.tencent.bk.job.common.esb.model.job;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.ip.IpUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Setter
@Getter
@NoArgsConstructor
public class EsbIpDTO {

    @JsonProperty("bk_host_id")
    private Long hostId;

    @JsonProperty("bk_cloud_id")
    @NotNull(message = "{validation.constraints.InvalidBkCloudId.message}")
    @Min(value = 0L, message = "{validation.constraints.InvalidBkCloudId.message}")
    private Long bkCloudId;

    @JsonProperty("ip")
    @Pattern(regexp = "\\b((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)" +
        "\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)" +
        "\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\b",
        message = "{validation.constraints.InvalidIp.message}")
    private String ip;

    public EsbIpDTO(Long hostId, Long bkCloudId, String ip) {
        this.hostId = hostId;
        this.bkCloudId = bkCloudId;
        this.ip = ip;
    }

    public static EsbIpDTO fromApplicationHostInfo(ApplicationHostDTO applicationHostInfo) {
        if (applicationHostInfo == null) {
            return null;
        }
        EsbIpDTO esbIp = new EsbIpDTO();
        esbIp.setBkCloudId(applicationHostInfo.getCloudAreaId());
        esbIp.setIp(applicationHostInfo.getIp());
        esbIp.setHostId(applicationHostInfo.getHostId());
        return esbIp;
    }

    public static EsbIpDTO fromHost(HostDTO host) {
        if (host == null) {
            return null;
        }
        EsbIpDTO esbIp = new EsbIpDTO();
        esbIp.setBkCloudId(host.getBkCloudId());
        esbIp.setIp(host.getIp());
        esbIp.setHostId(host.getHostId());
        return esbIp;
    }

    public static EsbIpDTO fromCloudIp(String cloudIp) {
        if (!IpUtils.checkCloudIp(cloudIp)) {
            return null;
        }
        String[] ipProps = cloudIp.split(IpUtils.COLON);
        EsbIpDTO host = new EsbIpDTO();
        host.setBkCloudId(Long.valueOf(ipProps[0]));
        host.setIp(ipProps[1]);
        return host;
    }
}
