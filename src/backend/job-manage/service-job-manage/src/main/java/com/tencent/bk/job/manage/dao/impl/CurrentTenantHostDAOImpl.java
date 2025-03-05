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

package com.tencent.bk.job.manage.dao.impl;

import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.HostStatusNumStatisticsDTO;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.common.TopologyHelper;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.CurrentTenantHostDAO;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.model.tables.Host;
import com.tencent.bk.job.manage.model.tables.HostTopo;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
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

    private List<ApplicationHostDTO> batchQueryHostInfo(
        Collection<String> items,
        Function<List<String>, List<ApplicationHostDTO>> queryFunction
    ) {
        List<String> itemList = new ArrayList<>(items);
        List<ApplicationHostDTO> hostInfoList = new ArrayList<>();
        // 分批，防止SQL超长
        int batchSize = 30000;
        int start = 0;
        int end = start + batchSize;
        int totalSize = itemList.size();
        end = Math.min(end, totalSize);
        do {
            List<String> subList = itemList.subList(start, end);
            hostInfoList.addAll(queryFunction.apply(subList));
            start += batchSize;
            end = start + batchSize;
            end = Math.min(end, totalSize);
        } while (start < totalSize);
        return hostInfoList;
    }

    private List<ApplicationHostDTO> listHostInfoByIpsIndeed(Collection<String> ips) {
        List<Condition> conditions = getBasicConditions();
        conditions.add(TABLE.IP.in(ips));
        return queryHostsByCondition(conditions);
    }

    private List<ApplicationHostDTO> listHostInfoByCloudIpsIndeed(Collection<String> cloudIps) {
        List<Condition> conditions = getBasicConditions();
        conditions.add(TABLE.CLOUD_IP.in(cloudIps));
        return queryHostsByCondition(conditions);
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
    public List<ApplicationHostDTO> listHostInfoByHostIds(Collection<Long> hostIds) {
        List<Condition> conditions = getBasicConditions();
        conditions.add(TABLE.HOST_ID.in(hostIds.stream().map(ULong::valueOf).collect(Collectors.toList())));
        return listHostInfoByConditions(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByIpv6s(Collection<String> ipv6s) {
        List<Condition> conditions = getBasicConditions();
        conditions.add(TABLE.IP_V6.in(ipv6s));
        return listHostInfoByConditions(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByHostNames(Collection<String> hostNames) {
        List<Condition> conditions = getBasicConditions();
        conditions.add(TABLE.IP_DESC.in(hostNames));
        return listHostInfoByConditions(conditions);
    }

    @Override
    public Long countHostInfoBySearchContents(Collection<Long> bizIds,
                                              Collection<Long> moduleIds,
                                              Collection<Long> cloudAreaIds,
                                              List<String> searchContents,
                                              Integer agentStatus) {
        List<Long> hostIdList = getHostIdListBySearchContents(
            bizIds,
            moduleIds,
            cloudAreaIds,
            searchContents,
            agentStatus,
            null,
            null
        );
        return (long) (hostIdList.size());
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoBySearchContents(Collection<Long> bizIds,
                                                                 Collection<Long> moduleIds,
                                                                 Collection<Long> cloudAreaIds,
                                                                 List<String> searchContents,
                                                                 Integer agentStatus,
                                                                 Long start,
                                                                 Long limit) {
        List<Long> hostIdList = getHostIdListBySearchContents(
            bizIds,
            moduleIds,
            cloudAreaIds,
            searchContents,
            agentStatus,
            start,
            limit
        );
        return listHostInfoByHostIds(hostIdList);
    }

    @Override
    public List<Long> getHostIdListBySearchContents(Collection<Long> bizIds,
                                                    Collection<Long> moduleIds,
                                                    Collection<Long> cloudAreaIds,
                                                    List<String> searchContents,
                                                    Integer agentAlive,
                                                    Long start,
                                                    Long limit) {
        List<Condition> conditions = buildSearchContentsConditions(
            bizIds,
            moduleIds,
            cloudAreaIds,
            searchContents,
            agentAlive
        );
        return getHostIdListByConditions(conditions, start, limit);
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
        List<Long> hostIdList = getHostIdListByMultiKeys(
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
        return listHostInfoByHostIds(hostIdList);
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
        List<Condition> conditions = buildMultiKeysConditions(
            bizIds,
            moduleIds,
            cloudAreaIds,
            ipKeys,
            ipv6Keys,
            hostNameKeys,
            osNameKeys,
            agentAlive
        );
        return getHostIdListByConditions(conditions, start, limit);
    }

    public Long countHostInfoByMultiKeys(Collection<Long> bizIds,
                                         Collection<Long> moduleIds,
                                         Collection<Long> cloudAreaIds,
                                         Collection<String> ipKeys,
                                         Collection<String> ipv6Keys,
                                         Collection<String> hostNameKeys,
                                         Collection<String> osNameKeys,
                                         Integer agentAlive) {
        List<Condition> conditions = buildMultiKeysConditions(
            bizIds,
            moduleIds,
            cloudAreaIds,
            ipKeys,
            ipv6Keys,
            hostNameKeys,
            osNameKeys,
            agentAlive
        );
        return countHostIdByConditions(conditions);
    }

    private List<Condition> buildSearchContentsConditions(Collection<Long> bizIds,
                                                          Collection<Long> moduleIds,
                                                          Collection<Long> cloudAreaIds,
                                                          List<String> searchContents,
                                                          Integer agentAlive) {
        Host tHost = Host.HOST;
        List<Condition> conditions = buildConditions(bizIds, moduleIds, agentAlive);
        Condition condition = null;
        if (searchContents != null && !searchContents.isEmpty()) {
            String firstContent = searchContents.get(0);
            condition = tHost.IP.like("%" + firstContent + "%");
            for (int i = 1; i < searchContents.size(); i++) {
                condition = condition.or(tHost.IP.like("%" + searchContents.get(i) + "%"));
            }
            condition = condition.or(tHost.IP_V6.like("%" + firstContent + "%"));
            for (int i = 1; i < searchContents.size(); i++) {
                condition = condition.or(tHost.IP_V6.like("%" + searchContents.get(i) + "%"));
            }
            condition = condition.or(tHost.IP_DESC.like("%" + firstContent + "%"));
            for (int i = 1; i < searchContents.size(); i++) {
                condition = condition.or(tHost.IP_DESC.like("%" + searchContents.get(i) + "%"));
            }
            condition = condition.or(tHost.OS.like("%" + firstContent + "%"));
            for (int i = 1; i < searchContents.size(); i++) {
                condition = condition.or(tHost.OS.like("%" + searchContents.get(i) + "%"));
            }
        }
        if (cloudAreaIds != null) {
            if (condition != null) {
                condition = condition.or(tHost.CLOUD_AREA_ID.in(cloudAreaIds));
            } else {
                condition = tHost.CLOUD_AREA_ID.in(cloudAreaIds);
            }
        }
        if (condition != null) {
            conditions.add(condition);
        }
        return conditions;
    }

    private <T> void addFieldMultiLikeCondition(List<Condition> conditions, Field<T> field, Collection<String> keys) {
        if (CollectionUtils.isNotEmpty(keys)) {
            List<String> keyList = new ArrayList<>(keys);
            String firstContent = keyList.get(0);
            Condition condition = field.like("%" + firstContent + "%");
            for (int i = 1; i < keyList.size(); i++) {
                condition = condition.or(field.like("%" + keyList.get(i) + "%"));
            }
            conditions.add(condition);
        }
    }

    private List<Condition> buildMultiKeysConditions(Collection<Long> bizIds,
                                                     Collection<Long> moduleIds,
                                                     Collection<Long> cloudAreaIds,
                                                     Collection<String> ipKeys,
                                                     Collection<String> ipv6Keys,
                                                     Collection<String> hostNameKeys,
                                                     Collection<String> osNameKeys,
                                                     Integer agentAlive) {
        Host tHost = Host.HOST;
        List<Condition> conditions = buildConditions(bizIds, moduleIds, agentAlive);
        if (cloudAreaIds != null) {
            conditions.add(tHost.CLOUD_AREA_ID.in(cloudAreaIds));
        }
        addFieldMultiLikeCondition(conditions, tHost.IP, ipKeys);
        addFieldMultiLikeCondition(conditions, tHost.IP_V6, ipv6Keys);
        addFieldMultiLikeCondition(conditions, tHost.IP_DESC, hostNameKeys);
        addFieldMultiLikeCondition(conditions, tHost.OS, osNameKeys);
        return conditions;
    }


    private Long countHostIdByConditions(Collection<Condition> conditions) {
        Host tHost = Host.HOST;
        HostTopo tHostTopo = HostTopo.HOST_TOPO;
        return context
            .select(DSL.countDistinct(tHost.HOST_ID))
            .from(tHost)
            .join(tHostTopo)
            .on(tHost.HOST_ID.eq(tHostTopo.HOST_ID))
            .where(conditions)
            .fetchOne(0, Long.class);
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
        List<Condition> conditions = getBasicConditions();
        if (bizIds != null) {
            conditions.add(HostTopo.HOST_TOPO.APP_ID.in(bizIds));
        }
        var query = context.select(
                TABLE.IS_AGENT_ALIVE.as(HostStatusNumStatisticsDTO.KEY_AGENT_ALIVE),
                DSL.countDistinct(TABLE.HOST_ID).as(HostStatusNumStatisticsDTO.KEY_HOST_NUM)
            ).from(TABLE)
            .leftJoin(HostTopo.HOST_TOPO).on(TABLE.HOST_ID.eq(HostTopo.HOST_TOPO.HOST_ID))
            .where(conditions)
            .groupBy(TABLE.IS_AGENT_ALIVE);
        val records = query.fetch();
        List<HostStatusNumStatisticsDTO> countList = new ArrayList<>();
        if (!records.isEmpty()) {
            records.forEach(record -> {
                HostStatusNumStatisticsDTO statisticsDTO = new HostStatusNumStatisticsDTO();
                statisticsDTO.setHostNum(record.get(HostStatusNumStatisticsDTO.KEY_HOST_NUM, Integer.class));
                statisticsDTO.setGseAgentAlive(record.get(HostStatusNumStatisticsDTO.KEY_AGENT_ALIVE, Integer.class));
                countList.add(statisticsDTO);
            });
        }
        return countList;
    }

}
