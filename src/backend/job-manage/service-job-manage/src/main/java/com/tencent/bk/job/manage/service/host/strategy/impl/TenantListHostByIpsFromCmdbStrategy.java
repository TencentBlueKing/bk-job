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
import com.tencent.bk.job.manage.manager.host.HostCache;
import com.tencent.bk.job.manage.service.host.strategy.TenantListHostStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 根据CloudIPs从CMDB查询主机
 */
@Slf4j
public class TenantListHostByIpsFromCmdbStrategy implements TenantListHostStrategy<String> {

    private final IBizCmdbClient bizCmdbClient;
    private final HostCache hostCache;

    public TenantListHostByIpsFromCmdbStrategy(IBizCmdbClient bizCmdbClient, HostCache hostCache) {
        this.bizCmdbClient = bizCmdbClient;
        this.hostCache = hostCache;
    }

    @Override
    public Pair<List<String>, List<ApplicationHostDTO>> listHosts(String tenantId, List<String> cloudIps) {
        StopWatch watch = new StopWatch();
        List<String> notExistCloudIps = new ArrayList<>(cloudIps);

        watch.start("listHostsFromCmdb");
        List<ApplicationHostDTO> cmdbExistHosts = bizCmdbClient.listHostsByCloudIps(tenantId, cloudIps);
        watch.stop();
        if (CollectionUtils.isNotEmpty(cmdbExistHosts)) {
            watch.start("addHostsToCache");
            List<String> cmdbExistHostIds = cmdbExistHosts.stream()
                .map(ApplicationHostDTO::getCloudIp)
                .collect(Collectors.toList());
            notExistCloudIps.removeAll(cmdbExistHostIds);
            hostCache.batchAddOrUpdateHosts(cmdbExistHosts);
            log.info("sync new hosts from cmdb, hosts:{}", cmdbExistHosts);
            watch.stop();
        }

        if (watch.getTotalTimeMillis() > 3000) {
            log.warn(
                "ListHostsFromCmdb and update cache slow, ipSize: {}, cost: {}",
                cloudIps.size(),
                watch.prettyPrint()
            );
        }
        return Pair.of(notExistCloudIps, cmdbExistHosts);
    }
}
