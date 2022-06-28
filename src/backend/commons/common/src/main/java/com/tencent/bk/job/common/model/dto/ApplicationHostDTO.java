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

import com.tencent.bk.job.common.constant.JobConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 主机
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
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
     * IP
     */
    private String ip;

    /**
     * 展示用的IP
     */
    private String displayIp;
    /**
     * 主机描述
     */
    private String ipDesc;
    /**
     * 主机Agent状态
     */
    private Boolean gseAgentAlive;
    /**
     * 云区域ID
     */
    private Long cloudAreaId;
    /**
     * 云区域+ip
     */
    private String cloudIp;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 操作系统类型
     */
    private String osType;

    /**
     * 集群ID
     */
    private List<Long> setId;

    /**
     * 模块id,一个主机可以属于多个模块
     */
    private List<Long> moduleId;

    /**
     * cc的模块类型集合， 选填0,1,2，0所有模块 1普通模块，2DB模块， 支持多个模块
     **/
    private List<Long> moduleType;

    /**
     * IP 列表，搜索用参数
     */
    private List<String> ipList;

    public String getCloudIp() {
        if (StringUtils.isNotBlank(cloudIp)) {
            return cloudIp;
        } else {
            return cloudAreaId + ":" + ip;
        }
    }

    public String getModuleIdsStr() {
        if (moduleId != null) {
            return moduleId.stream().map(Object::toString).collect(Collectors.joining(","));
        }
        return null;
    }

    public String getSetIdsStr() {
        if (setId != null) {
            return setId.stream().map(Object::toString).collect(Collectors.joining(","));
        }
        return null;
    }

    public String getModuleTypeStr() {
        if (moduleType != null) {
            return moduleType.stream().map(Object::toString).collect(Collectors.joining(","));
        }
        return null;
    }

    public int getAgentStatusValue() {
        if (gseAgentAlive == null || !gseAgentAlive) {
            return JobConstants.GSE_AGENT_STATUS_VALUE_NOT_ALIVE;
        }
        return JobConstants.GSE_AGENT_STATUS_VALUE_ALIVE;
    }

}
