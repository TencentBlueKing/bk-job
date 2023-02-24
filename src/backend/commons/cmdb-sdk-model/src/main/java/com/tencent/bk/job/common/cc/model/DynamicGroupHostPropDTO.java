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

package com.tencent.bk.job.common.cc.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.util.ip.IpUtils;
import lombok.Data;

import java.util.List;

/**
 * 接口实体类，CMDB动态分组中的主机属性
 */
@Data
public class DynamicGroupHostPropDTO {

    @JsonProperty("bk_host_id")
    private Long id;

    @JsonProperty("bk_host_name")
    private String name;

    // 可能为多个ip
    @JsonProperty("bk_host_innerip")
    private String innerIp;

    // 可能为多个ip
    @JsonProperty("bk_host_innerip_v6")
    private String ipv6;

    // 可能为多个ip
    @JsonProperty("bk_agent_id")
    private String agentId;

    @JsonProperty("bk_cloud_id")
    private List<CcCloudIdDTO> cloudIdList;

    @JsonIgnore
    public String getFirstIp() {
        return IpUtils.getFirstIpFromMultiIp(innerIp, ",");
    }
}
