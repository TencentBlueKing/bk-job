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

package com.tencent.bk.job.manage.model.db;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.annotation.PersistenceObject;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import lombok.Getter;
import lombok.Setter;

/**
 * Redis 缓存主机DO
 */
@PersistenceObject
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CacheHostDO {
    /**
     * 云区域ID
     */
    private Long cloudAreaId;

    /**
     * ipv4
     */
    private String ip;

    /**
     * ipv6
     */
    private String ipv6;

    /**
     * 主机ID
     */
    private Long hostId;

    /**
     * 主机Agent ID
     */
    private String agentId;

    /**
     * 业务ID
     */
    private Long appId;

    /**
     * CMDB业务ID
     */
    private Long bizId;

    /**
     * 主机描述
     */
    private String hostDesc;

    /**
     * 主机Agent状态
     */
    private Integer gseAgentStatus;

    /**
     * 主机Agent是否存活
     */
    @CompatibleImplementation(name = "ipv6", explain = "兼容实现，保证发布过程中无损变更，下个版本删除", version = "3.8.0")
    private Boolean gseAgentAlive;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 操作系统类型
     */
    private String osType;

    public ApplicationHostDTO toApplicationHostDTO() {
        ApplicationHostDTO host = new ApplicationHostDTO();
        host.setBizId(this.bizId);
        host.setAppId(this.appId);
        host.setCloudAreaId(this.cloudAreaId);
        host.setIp(this.ip);
        host.setIpv6(ipv6);
        host.setHostId(this.hostId);
        host.setAgentId(this.agentId);
        host.setHostName(this.hostDesc);
        host.setOsName(this.os);
        host.setOsType(this.osType);
        if (gseAgentStatus != null) {
            host.setGseAgentStatus(this.gseAgentStatus);
        } else {
            host.setGseAgentAlive(this.gseAgentAlive);
        }
        return host;
    }

    public static CacheHostDO fromApplicationHostDTO(ApplicationHostDTO host) {
        CacheHostDO cacheHost = new CacheHostDO();
        cacheHost.setBizId(host.getBizId());
        cacheHost.setAppId(host.getAppId());
        cacheHost.setCloudAreaId(host.getCloudAreaId());
        cacheHost.setIp(host.getIp());
        cacheHost.setIpv6(host.getIpv6());
        cacheHost.setHostId(host.getHostId());
        cacheHost.setAgentId(host.getAgentId());
        cacheHost.setHostDesc(host.getHostName());
        cacheHost.setOs(host.getOsName());
        cacheHost.setOsType(host.getOsType());
        cacheHost.setGseAgentStatus(host.getGseAgentStatus());
        cacheHost.setGseAgentAlive(host.getGseAgentAlive());
        return cacheHost;
    }
}
