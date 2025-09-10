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
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.manage.dao.CurrentTenantHostDAO;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.model.dto.HostTopoDTO;
import com.tencent.bk.job.manage.model.query.HostQuery;
import com.tencent.bk.job.manage.service.host.CurrentTenantBizHostService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 业务主机服务
 */
@Slf4j
@Service
public class CurrentTenantBizHostServiceImpl implements CurrentTenantBizHostService {

    private final CurrentTenantHostDAO currentTenantHostDAO;
    private final HostTopoDAO hostTopoDAO;

    @Autowired
    public CurrentTenantBizHostServiceImpl(CurrentTenantHostDAO currentTenantHostDAO,
                                           HostTopoDAO hostTopoDAO) {
        this.currentTenantHostDAO = currentTenantHostDAO;
        this.hostTopoDAO = hostTopoDAO;
    }

    @Override
    public List<ApplicationHostDTO> getHostsByHostIds(Collection<Long> hostIds) {
        return currentTenantHostDAO.listHostInfoByHostIds(hostIds);
    }

    @Override
    public List<ApplicationHostDTO> getHostsByIps(Collection<String> ips) {
        return currentTenantHostDAO.listHostInfoByIps(ips);
    }

    @Override
    public List<ApplicationHostDTO> getHostsByCloudIps(Collection<String> cloudIps) {
        return currentTenantHostDAO.listHostInfoByCloudIps(cloudIps);
    }

    @Override
    public List<ApplicationHostDTO> getHostsByIpv6s(Collection<String> ipv6s) {
        return currentTenantHostDAO.listHostInfoByIpv6s(ipv6s);
    }

    @Override
    public List<ApplicationHostDTO> getHostsByHostNames(Collection<String> hostNames) {
        return currentTenantHostDAO.listHostInfoByHostNames(hostNames);
    }

    @Override
    public List<ApplicationHostDTO> getHostsByBizAndHostIds(Collection<Long> bizIds, Collection<Long> hostIds) {
        if (CollectionUtils.isEmpty(bizIds) || CollectionUtils.isEmpty(hostIds)) {
            return Collections.emptyList();
        }
        List<Long> hostIdsInBiz = hostTopoDAO.listHostIdByBizAndHostIds(bizIds, hostIds);
        if (CollectionUtils.isNotEmpty(hostIds) && hostIdsInBiz.size() != hostIds.size()) {
            Set<Long> hostIdsSet = new HashSet<>(hostIds);
            hostIdsSet.removeAll(hostIdsInBiz);
            log.warn(
                "hostIds [{}] not in bizIds [{}]",
                StringUtil.concatCollection(hostIdsSet),
                StringUtil.concatCollection(bizIds)
            );
        }
        return currentTenantHostDAO.listHostInfoByHostIds(hostIdsInBiz);
    }

    @Override
    public List<Long> filterHostIds(Collection<Long> hostIds) {
        if (CollectionUtils.isEmpty(hostIds)) {
            return Collections.emptyList();
        }
        return currentTenantHostDAO.listHostIdsByHostIds(hostIds);
    }

    @Override
    public List<Long> filterHostIdsByBiz(Collection<Long> bizIds, Collection<Long> hostIds) {
        if (CollectionUtils.isEmpty(bizIds) || CollectionUtils.isEmpty(hostIds)) {
            return Collections.emptyList();
        }
        return hostTopoDAO.listHostIdByBizAndHostIds(bizIds, hostIds);
    }

    @Override
    public List<ApplicationHostDTO> getHostsByBizAndIps(Collection<Long> bizIds, Collection<String> ips) {
        return currentTenantHostDAO.listHostInfoByBizAndIps(bizIds, ips);
    }

    @Override
    public List<ApplicationHostDTO> getHostsByBizAndCloudIps(Collection<Long> bizIds,
                                                             Collection<String> cloudIps) {
        return currentTenantHostDAO.listHostInfoByBizAndCloudIps(bizIds, cloudIps);
    }

    @Override
    public List<ApplicationHostDTO> getHostsByBizAndIpv6s(Collection<Long> bizIds, Collection<String> ipv6s) {
        return currentTenantHostDAO.listHostInfoByBizAndIpv6s(bizIds, ipv6s);
    }

    @Override
    public List<ApplicationHostDTO> getHostsByBizAndHostNames(Collection<Long> bizIds,
                                                              Collection<String> hostNames) {
        return currentTenantHostDAO.listHostInfoByBizAndHostNames(bizIds, hostNames);
    }

    @Override
    public PageData<Long> pageListHostId(HostQuery hostQuery) {
        List<Long> hostIdList;
        Long count;
        StopWatch watch = new StopWatch("pageListHostId");
        List<String> searchContents = hostQuery.getSearchContents();
        if (searchContents != null) {
            watch.start("getHostIdListBySearchContents");
            hostIdList = currentTenantHostDAO.getHostIdListBySearchContents(
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
            count = currentTenantHostDAO.countHostInfoBySearchContents(
                hostQuery.getBizIds(),
                hostQuery.getModuleIds(),
                hostQuery.getCloudAreaIds(),
                searchContents,
                hostQuery.getAgentAlive()
            );
            watch.stop();
        } else {
            watch.start("getHostIdListByMultiKeys");
            hostIdList = currentTenantHostDAO.getHostIdListByMultiKeys(
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
            count = currentTenantHostDAO.countHostInfoByMultiKeys(
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

    @Override
    public PageData<ApplicationHostDTO> pageListHost(HostQuery hostQuery) {
        List<ApplicationHostDTO> hostList;
        Long count;
        List<String> searchContents = hostQuery.getSearchContents();
        if (searchContents != null) {
            hostList = currentTenantHostDAO.listHostInfoBySearchContents(
                hostQuery.getBizIds(),
                hostQuery.getModuleIds(),
                hostQuery.getCloudAreaIds(),
                searchContents,
                hostQuery.getAgentAlive(),
                hostQuery.getStart(),
                hostQuery.getLimit()
            );
            count = currentTenantHostDAO.countHostInfoBySearchContents(
                hostQuery.getBizIds(),
                hostQuery.getModuleIds(),
                hostQuery.getCloudAreaIds(),
                searchContents,
                hostQuery.getAgentAlive()
            );
        } else {
            hostList = currentTenantHostDAO.listHostInfoByMultiKeys(
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
            count = currentTenantHostDAO.countHostInfoByMultiKeys(
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

    @Override
    public List<ApplicationHostDTO> getHostsByModuleIds(Collection<Long> moduleIds) {
        List<HostTopoDTO> hostTopoDTOList = hostTopoDAO.listHostTopoByModuleIds(moduleIds);
        List<Long> hostIdList =
            hostTopoDTOList.stream().map(HostTopoDTO::getHostId).collect(Collectors.toList());
        return currentTenantHostDAO.listHostInfoByHostIds(hostIdList);
    }
}
