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

package com.tencent.bk.job.manage.service.host.impl;

import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.manage.service.host.strategy.TenantListHostStrategy;
import com.tencent.bk.job.manage.service.host.strategy.TenantListHostStrategyService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 指定租户的HostService实现
 */
@SuppressWarnings("SameParameterValue")
@Slf4j
public class BaseHostService {

    protected final TenantListHostStrategyService tenantListHostStrategyService;

    public BaseHostService(TenantListHostStrategyService tenantListHostStrategyService) {
        this.tenantListHostStrategyService = tenantListHostStrategyService;
    }

    protected Pair<List<Long>, List<String>> separateByHostIdOrCloudIp(Collection<HostDTO> hosts) {
        List<Long> hostIds = new ArrayList<>();
        List<String> cloudIps = new ArrayList<>();
        for (HostDTO host : hosts) {
            if (host.getHostId() != null) {
                hostIds.add(host.getHostId());
            } else {
                cloudIps.add(host.toCloudIp());
            }
        }
        return Pair.of(hostIds, cloudIps);
    }

    protected Pair<List<HostDTO>, List<ApplicationHostDTO>> listHostsFromCacheOrCmdb(String tenantId,
                                                                                     Collection<HostDTO> hosts) {
        List<ApplicationHostDTO> appHosts = new ArrayList<>();
        List<HostDTO> notExistHosts = new ArrayList<>();
        Pair<List<Long>, List<String>> pair = separateByHostIdOrCloudIp(hosts);
        List<Long> hostIds = pair.getLeft();
        List<String> cloudIps = pair.getRight();
        if (CollectionUtils.isNotEmpty(hostIds)) {
            TenantListHostStrategy<Long> strategy = tenantListHostStrategyService.buildByIdsFromCacheOrDbOrCmdbStrategy();
            Pair<List<Long>, List<ApplicationHostDTO>> result = strategy.listHosts(tenantId, hostIds);
            appHosts.addAll(result.getRight());
            if (CollectionUtils.isNotEmpty(result.getLeft())) {
                result.getLeft().forEach(notExistHostId -> notExistHosts.add(HostDTO.fromHostId(notExistHostId)));
            }
        }
        if (CollectionUtils.isNotEmpty(cloudIps)) {
            TenantListHostStrategy<String> strategy = tenantListHostStrategyService.buildByIpsFromCacheOrDbOrCmdbStrategy();
            Pair<List<String>, List<ApplicationHostDTO>> result = strategy.listHosts(tenantId, cloudIps);
            appHosts.addAll(result.getRight());
            if (CollectionUtils.isNotEmpty(result.getLeft())) {
                result.getLeft().forEach(notExistCloudIp -> notExistHosts.add(HostDTO.fromCloudIp(notExistCloudIp)));
            }
        }
        return Pair.of(notExistHosts, appHosts);
    }

}
