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


import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.manage.dao.NoTenantHostDAO;
import com.tencent.bk.job.manage.manager.host.HostCache;
import com.tencent.bk.job.manage.service.host.strategy.NoTenantListHostStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class NoTenantListHostByHostIdsFromDbStrategy implements NoTenantListHostStrategy<Long> {
    private final NoTenantHostDAO noTenantHostDAO;
    private final HostCache hostCache;

    public NoTenantListHostByHostIdsFromDbStrategy(NoTenantHostDAO noTenantHostDAO, HostCache hostCache) {
        this.noTenantHostDAO = noTenantHostDAO;
        this.hostCache = hostCache;
    }

    @Override
    public Pair<List<Long>, List<ApplicationHostDTO>> listHosts(List<Long> hostIds) {
        StopWatch watch = new StopWatch();
        List<ApplicationHostDTO> appHosts = new ArrayList<>();
        List<Long> notExistHostIds = new ArrayList<>(hostIds);

        watch.start("listHostsFromDb");
        List<ApplicationHostDTO> hostsInDb = noTenantHostDAO.listHostInfoByHostIds(hostIds);
        watch.stop();

        if (CollectionUtils.isNotEmpty(hostsInDb)) {
            watch.start("addHostsToCache");
            for (ApplicationHostDTO appHost : hostsInDb) {
                if (appHost.getBizId() == null || appHost.getBizId() <= 0) {
                    log.info("Host: {}|{} missing bizId, skip!", appHost.getHostId(), appHost.getCloudIp());
                    // DB中缓存的主机可能没有业务信息(依赖的主机事件还没有处理),那么暂时跳过该主机
                    continue;
                }
                notExistHostIds.remove(appHost.getHostId());
                appHosts.add(appHost);
            }
            hostCache.batchAddOrUpdateHosts(appHosts);
            watch.stop();
        }

        if (watch.getTotalTimeMillis() > 3000) {
            log.warn(
                "ListHostsFromDb and update cache slow, hostSize: {}, cost: {}",
                hostIds.size(),
                watch.prettyPrint()
            );
        }
        return Pair.of(notExistHostIds, appHosts);
    }
}
