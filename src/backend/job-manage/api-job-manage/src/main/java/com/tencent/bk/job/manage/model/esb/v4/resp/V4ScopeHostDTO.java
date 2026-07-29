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

package com.tencent.bk.job.manage.model.esb.v4.resp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * OpenAPI V4 资源范围主机信息。
 * <p>
 * 出于安全考虑不对外暴露 agentId。
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class V4ScopeHostDTO {

    /**
     * 主机 ID。
     */
    @JsonProperty("bk_host_id")
    private Long hostId;

    /**
     * 主机 IPv4。
     */
    @JsonProperty("ip")
    private String ip;

    /**
     * 主机 IPv6。
     */
    @JsonProperty("ipv6")
    private String ipv6;

    /**
     * 管控区域 ID。
     */
    @JsonProperty("bk_cloud_id")
    private Long cloudAreaId;

    /**
     * 管控区域名称。
     */
    @JsonProperty("bk_cloud_name")
    private String cloudAreaName;

    /**
     * 主机名称。
     */
    @JsonProperty("host_name")
    private String hostName;

    /**
     * 操作系统名称。
     */
    @JsonProperty("os_name")
    private String osName;

    /**
     * 操作系统类型。
     */
    @JsonProperty("os_type")
    private String osType;

    /**
     * Agent 状态：0-异常，1-正常。
     */
    @JsonProperty("alive")
    private Integer alive;
}
