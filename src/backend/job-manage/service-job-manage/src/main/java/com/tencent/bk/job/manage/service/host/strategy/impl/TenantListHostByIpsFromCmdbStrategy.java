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

package com.tencent.bk.job.manage.service.host.strategy.impl;


import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.manage.service.host.strategy.TenantListHostStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 根据CloudIPs从CMDB查询主机
 */
@Slf4j
public class TenantListHostByIpsFromCmdbStrategy implements TenantListHostStrategy<String> {

    private final IBizCmdbClient bizCmdbClient;

    public TenantListHostByIpsFromCmdbStrategy(IBizCmdbClient bizCmdbClient) {
        this.bizCmdbClient = bizCmdbClient;
    }

    @Override
    public Pair<List<String>, List<ApplicationHostDTO>> listHosts(String tenantId, List<String> cloudIps) {
        long start = System.currentTimeMillis();

        List<String> notExistCloudIps = new ArrayList<>(cloudIps);

        List<ApplicationHostDTO> cmdbExistHosts = bizCmdbClient.listHostsByCloudIps(tenantId, cloudIps);
        if (CollectionUtils.isNotEmpty(cmdbExistHosts)) {
            List<String> cmdbExistHostIds = cmdbExistHosts.stream()
                .map(ApplicationHostDTO::getCloudIp)
                .collect(Collectors.toList());
            notExistCloudIps.removeAll(cmdbExistHostIds);
            log.info("sync new hosts from cmdb, hosts:{}", cmdbExistHosts);
        }

        long cost = System.currentTimeMillis() - start;
        if (cost > 1000) {
            log.warn("ListHostsFromCmdb slow, ipSize: {}, cost: {}", cloudIps.size(), cost);
        }
        return Pair.of(notExistCloudIps, cmdbExistHosts);
    }
}
