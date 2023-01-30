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

package com.tencent.bk.job.logsvr.model.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.model.dto.HostDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 脚本日志
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@ApiModel("脚本日志")
public class ServiceScriptLogDTO {
    /**
     * 主机ID
     */
    @ApiModelProperty("主机ID")
    private Long hostId;

    /**
     * 主机ipv4,格式: 云区域ID:IPv4
     */
    @ApiModelProperty("主机ipv4,格式: 云区域ID:IPv4")
    @JsonProperty("ip")
    private String cloudIp;

    /**
     * 主机ipv6,格式: 云区域ID:IPv6
     */
    @ApiModelProperty("主机ipv6,格式: 云区域ID:IPv6")
    @JsonProperty("ipv6")
    private String cloudIpv6;

    /**
     * 日志偏移 - 字节
     */
    @ApiModelProperty("日志偏移 - 字节")
    private Integer offset;

    /**
     * 日志内容
     */
    @ApiModelProperty("日志内容")
    private String content;

    /**
     * Constructor
     *
     * @param host    主机
     * @param offset  日志偏移量(byte)
     * @param content 日志内容
     */
    public ServiceScriptLogDTO(HostDTO host,
                               Integer offset,
                               String content) {
        this.hostId = host.getHostId();
        this.cloudIp = host.toCloudIp();
        this.cloudIpv6 = host.toCloudIpv6();
        this.offset = offset;
        this.content = content;
    }

    /**
     * Constructor
     *
     * @param hostId  主机hostId
     * @param cloudIp      主机ipv4,格式: 云区域ID:IPv4
     * @param cloudIpv6    主机ipv6,格式: 云区域ID:IPv6
     * @param content 日志内容
     */
    public ServiceScriptLogDTO(Long hostId,
                               String cloudIp,
                               String cloudIpv6,
                               String content) {
        this.hostId = hostId;
        this.cloudIp = cloudIp;
        this.cloudIpv6 = cloudIpv6;
        this.content = content;
    }
}
