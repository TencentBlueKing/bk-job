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

package com.tencent.bk.job.manage.model.inner.request;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.ip.IpUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * 查询业务主机
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Slf4j
public class ServiceBatchGetAppHostsReq {
    /**
     * 查询的主机列表
     */
    private List<HostDTO> hosts;
    /**
     * 是否需要刷新主机的bk_agent_id
     */
    private boolean refreshAgentId;


    public ServiceBatchGetAppHostsReq(List<HostDTO> hosts, boolean refreshAgentId) {
        this.hosts = hosts;
        this.refreshAgentId = refreshAgentId;
    }

    public void validate() throws InvalidParamException {
        if (CollectionUtils.isEmpty(hosts)) {
            log.error("Empty param: hosts");
            throw new InvalidParamException(
                ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new Object[]{
                    "hosts",
                    "hosts must not be empty"
                });
        }
        hosts.forEach(host -> {
            if (host.getHostId() == null && !IpUtils.checkCloudIp(host.toCloudIp())) {
                log.error("Invalid host: {}", host);
                throw new InvalidParamException(
                    ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                    new Object[]{
                        "hosts",
                        "Invalid host"
                    });
            }
        });
    }
}
