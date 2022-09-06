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

package com.tencent.bk.job.manage.service.host.impl;

import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.model.dto.HostTopoDTO;
import com.tencent.bk.job.manage.service.host.BizHostService;
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
public class BizHostServiceImpl implements BizHostService {

    private final ApplicationHostDAO applicationHostDAO;
    private final HostTopoDAO hostTopoDAO;

    @Autowired
    public BizHostServiceImpl(ApplicationHostDAO applicationHostDAO,
                              HostTopoDAO hostTopoDAO) {
        this.applicationHostDAO = applicationHostDAO;
        this.hostTopoDAO = hostTopoDAO;
    }

    @Override
    public List<ApplicationHostDTO> getHostsByHostIds(Collection<Long> hostIds) {
        return applicationHostDAO.listHostInfoByHostIds(hostIds);
    }

    @Override
    public List<ApplicationHostDTO> getHostsByIps(Collection<String> ips) {
        return applicationHostDAO.listHostInfoByIps(ips);
    }

    @Override
    public List<ApplicationHostDTO> getHostsByIpv6s(Collection<String> ipv6s) {
        return applicationHostDAO.listHostInfoByIpv6s(ipv6s);
    }

    @Override
    public List<ApplicationHostDTO> getHostsByHostNames(Collection<String> hostNames) {
        return applicationHostDAO.listHostInfoByHostNames(hostNames);
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
        return applicationHostDAO.listHostInfoByHostIds(hostIdsInBiz);
    }

    @Override
    public List<ApplicationHostDTO> getHostsByBizAndIps(Collection<Long> bizIds, Collection<String> ips) {
        return applicationHostDAO.listHostInfoByBizAndIps(bizIds, ips);
    }

    @Override
    public List<ApplicationHostDTO> getHostsByBizAndIpv6s(Collection<Long> bizIds, Collection<String> ipv6s) {
        return applicationHostDAO.listHostInfoByBizAndIpv6s(bizIds, ipv6s);
    }

    @Override
    public List<ApplicationHostDTO> getHostsByBizAndHostNames(Collection<Long> bizIds,
                                                              Collection<String> hostNames) {
        return applicationHostDAO.listHostInfoByBizAndHostNames(bizIds, hostNames);
    }

    @Override
    public PageData<Long> pageListHostId(Collection<Long> bizIds,
                                         Collection<Long> moduleIds,
                                         Collection<Long> cloudAreaIds,
                                         List<String> searchContents,
                                         Integer agentAlive,
                                         Long start,
                                         Long limit) {
        StopWatch watch = new StopWatch("pageListHostId");
        watch.start("listHostInfoBySearchContents");
        List<Long> hostIdList = applicationHostDAO.getHostIdListBySearchContents(
            bizIds,
            moduleIds,
            cloudAreaIds,
            searchContents,
            agentAlive,
            start,
            limit
        );
        watch.stop();
        watch.start("countHostInfoBySearchContents");
        Long count = applicationHostDAO.countHostInfoBySearchContents(
            bizIds,
            moduleIds,
            cloudAreaIds,
            searchContents,
            agentAlive
        );
        watch.stop();
        return new PageData<>(start.intValue(), limit.intValue(), count, hostIdList);
    }

    @Override
    public PageData<ApplicationHostDTO> pageListHost(Collection<Long> bizIds,
                                                     Collection<Long> moduleIds,
                                                     Collection<Long> cloudAreaIds,
                                                     List<String> searchContents,
                                                     Integer agentAlive,
                                                     List<String> ipKeyList,
                                                     List<String> ipv6KeyList,
                                                     List<String> hostNameKeyList,
                                                     List<String> osNameKeyList,
                                                     Long start,
                                                     Long limit) {
        List<ApplicationHostDTO> hostList;
        Long count;
        if (searchContents != null) {
            hostList = applicationHostDAO.listHostInfoBySearchContents(
                bizIds,
                moduleIds,
                cloudAreaIds,
                searchContents,
                agentAlive,
                start,
                limit
            );
            count = applicationHostDAO.countHostInfoBySearchContents(
                bizIds,
                moduleIds,
                cloudAreaIds,
                searchContents,
                agentAlive
            );
        } else {
            hostList = applicationHostDAO.listHostInfoByMultiKeys(
                bizIds,
                moduleIds,
                cloudAreaIds,
                ipKeyList,
                ipv6KeyList,
                hostNameKeyList,
                osNameKeyList,
                agentAlive,
                start,
                limit
            );
            count = applicationHostDAO.countHostInfoByMultiKeys(
                bizIds,
                moduleIds,
                cloudAreaIds,
                ipKeyList,
                ipv6KeyList,
                hostNameKeyList,
                osNameKeyList,
                agentAlive
            );
        }
        return new PageData<>(start.intValue(), limit.intValue(), count, hostList);
    }

    @Override
    public List<ApplicationHostDTO> getHostsByModuleIds(Collection<Long> moduleIds) {
        List<HostTopoDTO> hostTopoDTOList = hostTopoDAO.listHostTopoByModuleIds(moduleIds);
        List<Long> hostIdList =
            hostTopoDTOList.parallelStream().map(HostTopoDTO::getHostId).collect(Collectors.toList());
        return applicationHostDAO.listHostInfoByHostIds(hostIdList);
    }
}
