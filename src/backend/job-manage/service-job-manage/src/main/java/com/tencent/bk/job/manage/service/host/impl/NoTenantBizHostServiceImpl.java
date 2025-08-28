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

import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.manage.dao.NoTenantHostDAO;
import com.tencent.bk.job.manage.model.query.HostQuery;
import com.tencent.bk.job.manage.service.host.NoTenantBizHostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.List;

@Slf4j
@Service
public class NoTenantBizHostServiceImpl implements NoTenantBizHostService {

    private final NoTenantHostDAO noTenantHostDAO;

    @Autowired
    public NoTenantBizHostServiceImpl(NoTenantHostDAO noTenantHostDAO) {
        this.noTenantHostDAO = noTenantHostDAO;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public PageData<Long> pageListHostId(HostQuery hostQuery) {
        List<Long> hostIdList;
        Long count;
        StopWatch watch = new StopWatch("pageListHostId");
        List<String> searchContents = hostQuery.getSearchContents();
        if (searchContents != null) {
            watch.start("getHostIdListBySearchContents");
            hostIdList = noTenantHostDAO.getHostIdListBySearchContents(
                hostQuery.getBizIds(),
                hostQuery.getModuleIds(),
                hostQuery.getCloudAreaIds(),
                searchContents,
                hostQuery.getAgentAlive(),
                hostQuery.getStart(),
                hostQuery.getLimit()
            );
            watch.stop();
            watch.start("countHostInfoBySearchContents");
            count = noTenantHostDAO.countHostInfoBySearchContents(
                hostQuery.getBizIds(),
                hostQuery.getModuleIds(),
                hostQuery.getCloudAreaIds(),
                searchContents,
                hostQuery.getAgentAlive()
            );
            watch.stop();
        } else {
            watch.start("getHostIdListByMultiKeys");
            hostIdList = noTenantHostDAO.getHostIdListByMultiKeys(
                hostQuery.getBizIds(),
                hostQuery.getModuleIds(),
                hostQuery.getCloudAreaIds(),
                hostQuery.getIpKeyList(),
                hostQuery.getIpv6KeyList(),
                hostQuery.getHostNameKeyList(),
                hostQuery.getOsNameKeyList(),
                hostQuery.getAgentAlive(),
                hostQuery.getStart(),
                hostQuery.getLimit()
            );
            watch.stop();
            watch.start("countHostInfoByMultiKeys");
            count = noTenantHostDAO.countHostInfoByMultiKeys(
                hostQuery.getBizIds(),
                hostQuery.getModuleIds(),
                hostQuery.getCloudAreaIds(),
                hostQuery.getIpKeyList(),
                hostQuery.getIpv6KeyList(),
                hostQuery.getHostNameKeyList(),
                hostQuery.getOsNameKeyList(),
                hostQuery.getAgentAlive()
            );
            watch.stop();
        }
        if (watch.getTotalTimeMillis() > 3000) {
            log.warn("pageListHostId slow:" + watch.prettyPrint());
        }
        return new PageData<>(hostQuery.getStart().intValue(), hostQuery.getLimit().intValue(), count, hostIdList);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public PageData<ApplicationHostDTO> pageListHost(HostQuery hostQuery) {
        List<ApplicationHostDTO> hostList;
        Long count;
        List<String> searchContents = hostQuery.getSearchContents();
        if (searchContents != null) {
            hostList = noTenantHostDAO.listHostInfoBySearchContents(
                hostQuery.getBizIds(),
                hostQuery.getModuleIds(),
                hostQuery.getCloudAreaIds(),
                searchContents,
                hostQuery.getAgentAlive(),
                hostQuery.getStart(),
                hostQuery.getLimit()
            );
            count = noTenantHostDAO.countHostInfoBySearchContents(
                hostQuery.getBizIds(),
                hostQuery.getModuleIds(),
                hostQuery.getCloudAreaIds(),
                searchContents,
                hostQuery.getAgentAlive()
            );
        } else {
            hostList = noTenantHostDAO.listHostInfoByMultiKeys(
                hostQuery.getBizIds(),
                hostQuery.getModuleIds(),
                hostQuery.getCloudAreaIds(),
                hostQuery.getIpKeyList(),
                hostQuery.getIpv6KeyList(),
                hostQuery.getHostNameKeyList(),
                hostQuery.getOsNameKeyList(),
                hostQuery.getAgentAlive(),
                hostQuery.getStart(),
                hostQuery.getLimit()
            );
            count = noTenantHostDAO.countHostInfoByMultiKeys(
                hostQuery.getBizIds(),
                hostQuery.getModuleIds(),
                hostQuery.getCloudAreaIds(),
                hostQuery.getIpKeyList(),
                hostQuery.getIpv6KeyList(),
                hostQuery.getHostNameKeyList(),
                hostQuery.getOsNameKeyList(),
                hostQuery.getAgentAlive()
            );
        }
        return new PageData<>(hostQuery.getStart().intValue(), hostQuery.getLimit().intValue(), count, hostList);
    }

}
