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

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.service.HostService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 主机校验服务
 */
@Service
@Slf4j
public class HostValidationService {

    private final HostService hostService;

    public HostValidationService(HostService hostService) {
        this.hostService = hostService;
    }

    /**
     * 校验主机是否存在，校验失败返回不存在的主机
     */
    public ValidateResult validateHostIpsExist(Long appId,
                                               List<HostDTO> hostList) {
        if (CollectionUtils.isEmpty(hostList)) {
            return ValidateResult.pass();
        }

        List<HostDTO> notExistHosts = hostService.extractNotExistHosts(appId, hostList);
        if (CollectionUtils.isNotEmpty(notExistHosts)) {
            List<String> notExistCloudIps = notExistHosts.stream()
                .map(HostDTO::toCloudIp)
                .collect(Collectors.toList());
            String msg = String.format("CloudIps %s are not exist.", notExistCloudIps);
            log.warn(msg);
            return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"ip_list", msg});
        }

        return ValidateResult.pass();
    }

    /**
     * 校验主机id是否存在，校验失败返回不存在的主机id
     */
    public ValidateResult validateHostIdsExist(Long appId,
                                               List<Long> hostIdList) {
        if (CollectionUtils.isEmpty(hostIdList)) {
            return ValidateResult.pass();
        }

        List<HostDTO> notExistHosts = hostService.extractNotExistHosts(appId,
            hostIdList.stream()
                .map(HostDTO::fromHostId)
                .collect(Collectors.toList()));
        if (CollectionUtils.isNotEmpty(notExistHosts)) {
            List<Long> notExistHostIds = notExistHosts.stream()
                .map(HostDTO::getHostId)
                .collect(Collectors.toList());
            String msg = String.format("HostIds %s are not exist.", notExistHostIds);
            log.warn(msg);
            return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"host_id_list", msg});
        }

        return ValidateResult.pass();
    }
}
