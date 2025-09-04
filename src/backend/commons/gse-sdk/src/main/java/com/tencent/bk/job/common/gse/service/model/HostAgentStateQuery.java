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

package com.tencent.bk.job.common.gse.service.model;

import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.model.dto.HostSimpleDTO;
import lombok.Data;


@SuppressWarnings("DuplicatedCode")
@Data
public class HostAgentStateQuery {
    /**
     * 主机ID
     */
    private Long hostId;
    /**
     * cmdb业务ID
     */
    private Long bizId;
    /**
     * 云区域IP
     */
    private String cloudIp;
    /**
     * AgentId
     */
    private String agentId;

    public static HostAgentStateQuery from(HostDTO hostDTO) {
        if (hostDTO == null) {
            return null;
        }
        HostAgentStateQuery query = new HostAgentStateQuery();
        query.setHostId(hostDTO.getHostId());
        query.setCloudIp(hostDTO.toCloudIp());
        query.setAgentId(hostDTO.getAgentId());
        return query;
    }

    public static HostAgentStateQuery from(HostSimpleDTO hostSimpleDTO) {
        if (hostSimpleDTO == null) {
            return null;
        }
        HostAgentStateQuery query = new HostAgentStateQuery();
        query.setHostId(hostSimpleDTO.getHostId());
        query.setBizId(hostSimpleDTO.getBizId());
        query.setCloudIp(hostSimpleDTO.getCloudIp());
        query.setAgentId(hostSimpleDTO.getAgentId());
        return query;
    }

    public static HostAgentStateQuery from(ApplicationHostDTO applicationHostDTO) {
        if (applicationHostDTO == null) {
            return null;
        }
        HostAgentStateQuery query = new HostAgentStateQuery();
        query.setHostId(applicationHostDTO.getHostId());
        query.setBizId(applicationHostDTO.getBizId());
        query.setCloudIp(applicationHostDTO.getCloudIp());
        query.setAgentId(applicationHostDTO.getAgentId());
        return query;
    }
}
