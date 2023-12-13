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

package com.tencent.bk.job.common.cc.model.result;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.TimeUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Getter
@Setter
@ToString
public class HostProp {
    @JsonProperty("bk_host_id")
    private Long hostId;
    @JsonProperty("bk_host_innerip")
    private String ip;
    @JsonProperty("bk_host_innerip_v6")
    private String ipv6;
    @JsonProperty("bk_agent_id")
    private String agentId;
    @JsonProperty("bk_host_name")
    private String hostName;
    @JsonProperty("bk_os_name")
    private String osName;
    @JsonProperty("bk_os_type")
    private String osType;
    @JsonProperty("bk_cloud_id")
    private Long cloudAreaId = 0L;
    @JsonProperty("bk_cloud_vendor")
    private String cloudVendorId;
    @JsonProperty("last_time")
    private String lastTime;

    public ApplicationHostDTO toApplicationHostDTO() {
        ApplicationHostDTO applicationHostDTO = new ApplicationHostDTO();
        applicationHostDTO.setHostId(hostId);
        List<String> ipList = StringUtil.strToList(ip, String.class, ",");
        applicationHostDTO.setIpList(ipList);
        if (ipList != null && !ipList.isEmpty()) {
            applicationHostDTO.setIp(ipList.get(0));
        }
        applicationHostDTO.setDisplayIp(ip);
        applicationHostDTO.setIpv6(ipv6);
        applicationHostDTO.setAgentId(agentId);
        int hostNameMaxLength = 2000;
        int osNameMaxLength = 512;
        int osTypeNameMaxLength = 32;
        applicationHostDTO.setHostName(StringUtil.substring(hostName, hostNameMaxLength));
        applicationHostDTO.setOsName(StringUtil.substring(osName, osNameMaxLength));
        applicationHostDTO.setOsType(StringUtil.substring(osType, osTypeNameMaxLength));
        applicationHostDTO.setCloudAreaId(cloudAreaId);
        applicationHostDTO.setCloudVendorId(cloudVendorId);
        if (StringUtils.isNotBlank(lastTime)) {
            applicationHostDTO.setLastTime(TimeUtil.parseIsoZonedTimeToMillis(lastTime));
        }
        return applicationHostDTO;
    }
}
