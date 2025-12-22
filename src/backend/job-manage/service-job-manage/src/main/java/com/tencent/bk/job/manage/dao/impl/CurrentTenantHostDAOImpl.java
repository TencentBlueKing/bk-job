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

package com.tencent.bk.job.manage.dao.impl;

import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.HostStatusNumStatisticsDTO;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.common.TopologyHelper;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.CurrentTenantHostDAO;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作限定在当前租户下的主机DAO
 */
@Slf4j
@Repository
public class CurrentTenantHostDAOImpl extends AbstractBaseHostDAO implements CurrentTenantHostDAO {

    @Autowired
    public CurrentTenantHostDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext context,
                                    ApplicationDAO applicationDAO,
                                    HostTopoDAO hostTopoDAO,
                                    TopologyHelper topologyHelper) {
        super(context, applicationDAO, hostTopoDAO, topologyHelper);
    }

    @Override
    protected List<Condition> getBasicConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.TENANT_ID.eq(JobContextUtil.getTenantId()));
        return conditions;
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByIps(Collection<String> ips) {
        return batchQueryHostInfo(ips, this::listHostInfoByIpsIndeed);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByCloudIps(Collection<String> cloudIps) {
        return batchQueryHostInfo(cloudIps, this::listHostInfoByCloudIpsIndeed);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByBizId(long bizId) {
        List<Condition> conditions = buildBizIdCondition(bizId);
        return listHostInfoByConditions(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByBizAndIps(Collection<Long> bizIds, Collection<String> ips) {
        List<Condition> conditions = getBasicConditions();
        if (bizIds != null) {
            conditions.add(TABLE.APP_ID.in(bizIds.stream().map(ULong::valueOf).collect(Collectors.toList())));
        }
        conditions.add(TABLE.IP.in(ips));
        return listHostInfoByConditions(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByBizAndCloudIps(Collection<Long> bizIds, Collection<String> cloudIps) {
        List<Condition> conditions = getBasicConditions();
        if (bizIds != null) {
            conditions.add(TABLE.APP_ID.in(bizIds.stream().map(ULong::valueOf).collect(Collectors.toList())));
        }
        conditions.add(TABLE.CLOUD_IP.in(cloudIps));
        return listHostInfoByConditions(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByBizAndIpv6s(Collection<Long> bizIds, Collection<String> ipv6s) {
        List<Condition> conditions = getBasicConditions();
        if (bizIds != null) {
            conditions.add(TABLE.APP_ID.in(bizIds.stream().map(ULong::valueOf).collect(Collectors.toList())));
        }
        conditions.add(TABLE.IP_V6.in(ipv6s));
        return listHostInfoByConditions(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByBizAndHostNames(Collection<Long> bizIds,
                                                                  Collection<String> hostNames) {
        List<Condition> conditions = getBasicConditions();
        if (bizIds != null) {
            conditions.add(TABLE.APP_ID.in(bizIds.stream().map(ULong::valueOf).collect(Collectors.toList())));
        }
        conditions.add(TABLE.IP_DESC.in(hostNames));
        return listHostInfoByConditions(conditions);
    }

    @Override
    public List<Long> listHostIdsByHostIds(Collection<Long> hostIds) {
        List<Condition> conditions = getBasicConditions();
        conditions.add(TABLE.HOST_ID.in(hostIds.stream().map(ULong::valueOf).collect(Collectors.toList())));
        return listHostIdByConditions(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByHostIds(Collection<Long> hostIds) {
        return super.listHostInfoByHostIds(hostIds);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByIpv6s(Collection<String> ipv6s) {
        return super.listHostInfoByIpv6s(ipv6s);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByHostNames(Collection<String> hostNames) {
        return super.listHostInfoByHostNames(hostNames);
    }

    @Override
    public Long countHostInfoBySearchContents(Collection<Long> bizIds,
                                              Collection<Long> moduleIds,
                                              Collection<Long> cloudAreaIds,
                                              List<String> searchContents,
                                              Integer agentStatus) {
        return super.countHostInfoBySearchContents(bizIds, moduleIds, cloudAreaIds, searchContents, agentStatus);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoBySearchContents(Collection<Long> bizIds,
                                                                 Collection<Long> moduleIds,
                                                                 Collection<Long> cloudAreaIds,
                                                                 List<String> searchContents,
                                                                 Integer agentStatus,
                                                                 Long start,
                                                                 Long limit) {
        return super.listHostInfoBySearchContents(
            bizIds,
            moduleIds,
            cloudAreaIds,
            searchContents,
            agentStatus,
            start,
            limit
        );
    }

    @Override
    public List<Long> getHostIdListBySearchContents(Collection<Long> bizIds,
                                                    Collection<Long> moduleIds,
                                                    Collection<Long> cloudAreaIds,
                                                    List<String> searchContents,
                                                    Integer agentAlive,
                                                    Long start,
                                                    Long limit) {
        return super.getHostIdListBySearchContents(
            bizIds,
            moduleIds,
            cloudAreaIds,
            searchContents,
            agentAlive,
            start,
            limit
        );
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByMultiKeys(Collection<Long> bizIds,
                                                            Collection<Long> moduleIds,
                                                            Collection<Long> cloudAreaIds,
                                                            Collection<String> ipKeys,
                                                            Collection<String> ipv6Keys,
                                                            Collection<String> hostNameKeys,
                                                            Collection<String> osNameKeys,
                                                            Integer agentAlive,
                                                            Long start,
                                                            Long limit) {
        return super.listHostInfoByMultiKeys(
            bizIds,
            moduleIds,
            cloudAreaIds,
            ipKeys,
            ipv6Keys,
            hostNameKeys,
            osNameKeys,
            agentAlive,
            start,
            limit
        );
    }

    @Override
    public List<Long> getHostIdListByMultiKeys(Collection<Long> bizIds,
                                               Collection<Long> moduleIds,
                                               Collection<Long> cloudAreaIds,
                                               Collection<String> ipKeys,
                                               Collection<String> ipv6Keys,
                                               Collection<String> hostNameKeys,
                                               Collection<String> osNameKeys,
                                               Integer agentAlive,
                                               Long start,
                                               Long limit) {
        return super.getHostIdListByMultiKeys(
            bizIds,
            moduleIds,
            cloudAreaIds,
            ipKeys,
            ipv6Keys,
            hostNameKeys,
            osNameKeys,
            agentAlive,
            start,
            limit
        );
    }

    @Override
    public Long countHostInfoByMultiKeys(Collection<Long> bizIds,
                                         Collection<Long> moduleIds,
                                         Collection<Long> cloudAreaIds,
                                         Collection<String> ipKeys,
                                         Collection<String> ipv6Keys,
                                         Collection<String> hostNameKeys,
                                         Collection<String> osNameKeys,
                                         Integer agentAlive) {
        return super.countHostInfoByMultiKeys(
            bizIds,
            moduleIds,
            cloudAreaIds,
            ipKeys,
            ipv6Keys,
            hostNameKeys,
            osNameKeys,
            agentAlive
        );
    }

    @Override
    public long countHostsByBizIds(Collection<Long> bizIds) {
        List<Condition> conditions = getBasicConditions();
        conditions.add(TABLE.APP_ID.in(bizIds));
        return countHostByConditions(conditions);
    }

    @Override
    public long countAllHosts() {
        log.debug("countAllHosts");
        return countHostByConditions(null);
    }

    @Override
    public List<HostStatusNumStatisticsDTO> countHostStatusNumByBizIds(List<Long> bizIds) {
        return super.countHostStatusNumByBizIds(bizIds);
    }

}
