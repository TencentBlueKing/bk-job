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

package com.tencent.bk.job.manage.service.host.strategy.impl;


import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.manage.manager.host.HostCache;
import com.tencent.bk.job.manage.model.db.CacheHostDO;
import com.tencent.bk.job.manage.service.host.strategy.NoTenantListHostStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * 根据CloudIPs从缓存查询主机
 */
@Slf4j
public class NoTenantListHostByIpsFromCacheStrategy implements NoTenantListHostStrategy<String> {

    protected final HostCache hostCache;

    public NoTenantListHostByIpsFromCacheStrategy(HostCache hostCache) {
        this.hostCache = hostCache;
    }

    @Override
    public Pair<List<String>, List<ApplicationHostDTO>> listHosts(List<String> cloudIps) {
        long start = System.currentTimeMillis();
        List<ApplicationHostDTO> appHosts = new ArrayList<>();
        List<String> notExistCloudIps = new ArrayList<>();
        List<CacheHostDO> cacheHosts = hostCache.batchGetHostsByIps(cloudIps);
        for (int i = 0; i < cloudIps.size(); i++) {
            String cloudIp = cloudIps.get(i);
            CacheHostDO cacheHost = cacheHosts.get(i);
            if (cacheHost != null) {
                appHosts.add(cacheHost.toApplicationHostDTO());
            } else {
                notExistCloudIps.add(cloudIp);
            }
        }

        long cost = System.currentTimeMillis() - start;
        if (cost > 1000) {
            log.warn("ListHostsFromRedis slow, ipSize: {}, cost: {}", cloudIps.size(), cost);
        }
        return Pair.of(notExistCloudIps, appHosts);
    }
}
