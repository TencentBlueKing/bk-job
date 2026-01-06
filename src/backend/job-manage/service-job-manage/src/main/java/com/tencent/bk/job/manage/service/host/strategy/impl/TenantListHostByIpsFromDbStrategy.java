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
import com.tencent.bk.job.manage.dao.TenantHostDAO;
import com.tencent.bk.job.manage.manager.host.HostCache;
import com.tencent.bk.job.manage.service.host.strategy.TenantListHostStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.function.Function;

/**
 * 根据CloudIPs从DB查询主机
 */
@Slf4j
public class TenantListHostByIpsFromDbStrategy extends AbstractCacheableListHostStrategy<String>
    implements TenantListHostStrategy<String> {

    private final TenantHostDAO tenantHostDAO;

    public TenantListHostByIpsFromDbStrategy(TenantHostDAO tenantHostDAO, HostCache hostCache) {
        super(hostCache);
        this.tenantHostDAO = tenantHostDAO;
    }

    @Override
    public Pair<List<String>, List<ApplicationHostDTO>> listHosts(String tenantId, List<String> cloudIps) {
        Function<Void, Pair<List<String>, List<ApplicationHostDTO>>> loadHostsFunc = voidObj ->
            Pair.of(cloudIps, tenantHostDAO.listHostsByCloudIps(tenantId, cloudIps));
        Function<ApplicationHostDTO, String> extractKeyFunc = ApplicationHostDTO::getCloudIp;
        return listHostsAndRefreshCache("loadHostsFromDb", loadHostsFunc, extractKeyFunc);
    }

}
