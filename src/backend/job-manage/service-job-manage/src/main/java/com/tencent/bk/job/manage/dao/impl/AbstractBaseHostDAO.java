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
import com.tencent.bk.job.common.model.dto.BasicHostDTO;
import com.tencent.bk.job.common.model.dto.HostSimpleDTO;
import com.tencent.bk.job.common.model.dto.HostStatusNumStatisticsDTO;
import com.tencent.bk.job.common.mysql.util.JooqDataTypeUtil;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.manage.common.TopologyHelper;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
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
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.SelectLimitStep;
import org.jooq.SelectSeekStep1;
import org.jooq.TableField;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.jooq.types.ULong;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 基础主机DAO，封装部分公共逻辑
 */
@Slf4j
abstract public class AbstractBaseHostDAO {
    protected static final Host TABLE = Host.HOST;
    protected static final TableField<?, ?>[] ALL_FIELDS = {
        TABLE.HOST_ID,
        TABLE.APP_ID,
        TABLE.IP,
        TABLE.IP_V6,
        TABLE.AGENT_ID,
        TABLE.IP_DESC,
        TABLE.SET_IDS,
        TABLE.MODULE_IDS,
        TABLE.CLOUD_ID,
        TABLE.DISPLAY_IP,
        TABLE.OS,
        TABLE.OS_TYPE,
        TABLE.MODULE_TYPE,
        TABLE.IS_AGENT_ALIVE,
        TABLE.CLOUD_IP,
        TABLE.CLOUD_VENDOR_ID,
        TABLE.TENANT_ID
    };

    protected static final TableField<?, ?>[] SIMPLE_FIELDS = {
        TABLE.HOST_ID,
        TABLE.APP_ID,
        TABLE.IS_AGENT_ALIVE,
        TABLE.IP,
        TABLE.CLOUD_ID,
        TABLE.AGENT_ID,
        TABLE.APP_ID,
        TABLE.IP_V6,
        TABLE.IP_DESC,
        TABLE.OS,
        TABLE.OS_TYPE
    };

    protected static final TableField<?, ?>[] BASIC_FIELDS = {
        TABLE.HOST_ID,
        TABLE.LAST_TIME
    };

    protected final DSLContext context;
    protected final ApplicationDAO applicationDAO;
    protected final HostTopoDAO hostTopoDAO;
    protected final TopologyHelper topologyHelper;

    public AbstractBaseHostDAO(DSLContext context,
                               ApplicationDAO applicationDAO,
                               HostTopoDAO hostTopoDAO,
                               TopologyHelper topologyHelper) {
        this.context = context;
        this.applicationDAO = applicationDAO;
        this.topologyHelper = topologyHelper;
        this.hostTopoDAO = hostTopoDAO;
    }

    protected List<Condition> buildConditions(Collection<Long> bizIds,
                                              Collection<Long> moduleIds,
                                              Integer agentAlive) {
        Host tHost = Host.HOST;
        HostTopo tHostTopo = HostTopo.HOST_TOPO;
        List<Condition> conditions = getBasicConditions();
        if (bizIds != null) {
            conditions.add(tHostTopo.APP_ID.in(bizIds));
        }
        if (agentAlive != null) {
            conditions.add(tHost.IS_AGENT_ALIVE.eq(JooqDataTypeUtil.buildUByte(agentAlive)));
        }
        if (moduleIds != null) {
            conditions.add(tHostTopo.MODULE_ID.in(moduleIds));
        }
        return conditions;
    }

    protected List<Condition> buildBizIdCondition(long bizId) {
        List<Condition> conditions = getBasicConditions();
        conditions.add(TABLE.APP_ID.eq(JooqDataTypeUtil.buildULong(bizId)));
        return conditions;
    }

    protected List<ApplicationHostDTO> batchQueryHostInfo(
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

    protected List<ApplicationHostDTO> listHostInfoByIpsIndeed(Collection<String> ips) {
        List<Condition> conditions = getBasicConditions();
        conditions.add(TABLE.IP.in(ips));
        return queryHostsByCondition(conditions);
    }

    protected List<ApplicationHostDTO> listHostInfoByCloudIpsIndeed(Collection<String> cloudIps) {
        List<Condition> conditions = getBasicConditions();
        conditions.add(TABLE.CLOUD_IP.in(cloudIps));
        return queryHostsByCondition(conditions);
    }

    protected List<ApplicationHostDTO> listHostInfoByHostIds(Collection<Long> hostIds) {
        List<Condition> conditions = getBasicConditions();
        conditions.add(TABLE.HOST_ID.in(hostIds.stream().map(ULong::valueOf).collect(Collectors.toList())));
        return listHostInfoByConditions(conditions);
    }

    protected List<ApplicationHostDTO> listHostInfoByIpv6s(Collection<String> ipv6s) {
        List<Condition> conditions = getBasicConditions();
        conditions.add(TABLE.IP_V6.in(ipv6s));
        return listHostInfoByConditions(conditions);
    }

    protected List<ApplicationHostDTO> listHostInfoByHostNames(Collection<String> hostNames) {
        List<Condition> conditions = getBasicConditions();
        conditions.add(TABLE.IP_DESC.in(hostNames));
        return listHostInfoByConditions(conditions);
    }

    protected Long countHostInfoBySearchContents(Collection<Long> bizIds,
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

    protected List<ApplicationHostDTO> listHostInfoBySearchContents(Collection<Long> bizIds,
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

    protected List<Long> getHostIdListBySearchContents(Collection<Long> bizIds,
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

    protected List<ApplicationHostDTO> listHostInfoByMultiKeys(Collection<Long> bizIds,
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

    protected List<Long> getHostIdListByMultiKeys(Collection<Long> bizIds,
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

    protected Long countHostInfoByMultiKeys(Collection<Long> bizIds,
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
                condition = condition.or(tHost.CLOUD_ID.in(cloudAreaIds));
            } else {
                condition = tHost.CLOUD_ID.in(cloudAreaIds);
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
        } else if (keys != null && keys.isEmpty()) {
            // keys为空数组，表示不匹配任何数据
            Condition condition = field.in(Collections.emptyList());
            conditions.add(condition);
        }
        // keys为null，表示没有任何条件，匹配所有数据
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
            conditions.add(tHost.CLOUD_ID.in(cloudAreaIds));
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

    protected List<Long> getHostIdListByConditions(Collection<Condition> conditions,
                                                   Long start,
                                                   Long limit) {
        Host tHost = Host.HOST;
        HostTopo tHostTopo = HostTopo.HOST_TOPO;
        val query =
            context
                .selectDistinct(tHost.HOST_ID)
                .select(tHost.IS_AGENT_ALIVE)
                .from(tHost)
                .join(tHostTopo)
                .on(tHost.HOST_ID.eq(tHostTopo.HOST_ID))
                .where(conditions)
                .orderBy(TABLE.IS_AGENT_ALIVE.desc(), TABLE.HOST_ID.asc());
        log.debug("SQL={}", query.getSQL(ParamType.INLINED));
        Result<? extends Record> records = fetchRecordsWithLimit(query, start, limit);
        List<Long> hostIdList = new ArrayList<>();
        if (!records.isEmpty()) {
            hostIdList = records.stream()
                .map(record -> record.get(0, Long.class))
                .collect(Collectors.toList());
        }
        Set<Long> hostIdSet = new HashSet<>();
        List<Long> uniqueHostIdList = new ArrayList<>();
        hostIdList.forEach(hostId -> {
            if (!hostIdSet.contains(hostId)) {
                uniqueHostIdList.add(hostId);
                hostIdSet.add(hostId);
            }
        });
        return uniqueHostIdList;
    }

    protected List<Long> getHostIdListFromHostByConditions(Collection<Condition> conditions,
                                                           Long start,
                                                           Long limit) {
        Host tHost = Host.HOST;
        final SelectSeekStep1<Record1<ULong>, ULong> query = context
            .selectDistinct(tHost.HOST_ID)
            .from(tHost)
            .where(conditions)
            .orderBy(TABLE.HOST_ID.asc());
        log.debug("SQL={}", query.getSQL(ParamType.INLINED));
        Result<? extends Record> records = fetchRecordsWithLimit(query, start, limit);
        return records.stream()
            .map(record -> record.get(0, Long.class))
            .collect(Collectors.toList());
    }

    protected List<ApplicationHostDTO> queryHostsByCondition(List<Condition> conditions) {
        Result<Record> result =
            context.select(ALL_FIELDS)
                .from(TABLE)
                .where(conditions)
                .fetch();

        List<ApplicationHostDTO> hostInfoList = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(result)) {
            result.map(record -> hostInfoList.add(extractData(record)));
        }
        return hostInfoList;
    }

    protected List<BasicHostDTO> listBasicHostInfoByConditions(Collection<Condition> conditions) {
        if (conditions == null) {
            conditions = Collections.emptyList();
        }
        val query = context.select(
                TABLE.HOST_ID,
                TABLE.LAST_TIME
            ).from(TABLE)
            .where(conditions);
        Result<Record2<ULong, Long>> records = query.fetch();
        List<BasicHostDTO> basicHostInfoList = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(records)) {
            records.forEach(record -> {
                BasicHostDTO basicHost = new BasicHostDTO(
                    JooqDataTypeUtil.getLongFromULong(record.get(TABLE.HOST_ID)),
                    record.get(TABLE.LAST_TIME)
                );
                basicHostInfoList.add(basicHost);
            });
        }
        return basicHostInfoList;
    }

    protected List<Long> listHostIdByConditions(Collection<Condition> conditions) {
        if (conditions == null) {
            conditions = Collections.emptyList();
        }
        val query = context.select(TABLE.HOST_ID)
            .from(TABLE)
            .where(conditions);
        if (log.isDebugEnabled()) {
            log.debug("SQL={}", query.getSQL(ParamType.INLINED));
        }
        Result<Record1<ULong>> records = query.fetch();
        return records.stream().map(record -> record.get(0, Long.class)).collect(Collectors.toList());
    }

    protected List<ApplicationHostDTO> listHostInfoByConditions(Collection<Condition> conditions) {
        return listHostInfoByConditions(conditions, null, null);
    }

    protected List<ApplicationHostDTO> listHostInfoByConditions(Collection<Condition> conditions,
                                                                Long start,
                                                                Long limit) {
        if (conditions == null) {
            conditions = Collections.emptyList();
        }
        val query = context.select(ALL_FIELDS)
            .from(TABLE)
            .where(conditions)
            .orderBy(TABLE.IS_AGENT_ALIVE.desc(), TABLE.HOST_ID.asc());
        Result<? extends Record> records = fetchRecordsWithLimit(query, start, limit);
        List<ApplicationHostDTO> hostInfoList = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(records)) {
            records.map(record -> hostInfoList.add(extractData(record)));
        }
        return hostInfoList;
    }

    /**
     * 查询符合条件的主机数量
     */
    @SuppressWarnings("all")
    protected long countHostByConditions(List<Condition> conditions) {
        if (conditions == null) {
            conditions = getBasicConditions();
        }
        return context.selectCount().from(TABLE).where(conditions).fetchOne(0, Long.class);
    }

    protected Result<? extends Record> fetchRecordsWithLimit(SelectLimitStep<?> query,
                                                             Long start,
                                                             Long limit) {
        Result<? extends Record> records;
        if (start == null || start < 0) {
            start = 0L;
        }
        if (limit != null && limit > 0) {
            records = query.limit(start, limit).fetch();
        } else {
            records = query.fetch();
        }
        return records;
    }

    protected ApplicationHostDTO extractData(Record record) {
        if (record == null) {
            return null;
        }

        ApplicationHostDTO applicationHostDTO = new ApplicationHostDTO();
        applicationHostDTO.setBizId(record.get(TABLE.APP_ID).longValue());
        applicationHostDTO.setIp(record.get(TABLE.IP));
        applicationHostDTO.setIpv6(record.get(TABLE.IP_V6));
        applicationHostDTO.setAgentId(record.get(TABLE.AGENT_ID));
        applicationHostDTO.setHostName(record.get(TABLE.IP_DESC));
        applicationHostDTO.setGseAgentAlive(record.get(TABLE.IS_AGENT_ALIVE).intValue() == 1);
        List<Long> setIdList = new ArrayList<>();
        String setIdsStr = record.get(TABLE.SET_IDS);
        if (setIdsStr != null) {
            List<Long> list = new ArrayList<>();
            for (String id : setIdsStr.split(",")) {
                if (!id.trim().isEmpty()) {
                    Long parseLong = Long.parseLong(id);
                    list.add(parseLong);
                }
            }
            setIdList = list;
        }
        applicationHostDTO.setSetId(setIdList);
        applicationHostDTO.setModuleId(StringUtil.strToList(record.get(TABLE.MODULE_IDS), Long.class, ","));
        applicationHostDTO.setCloudAreaId(record.get(TABLE.CLOUD_ID));
        applicationHostDTO.setDisplayIp(record.get(TABLE.DISPLAY_IP));
        applicationHostDTO.setOsName(record.get(TABLE.OS));
        applicationHostDTO.setOsType(record.get(TABLE.OS_TYPE));
        applicationHostDTO.setModuleType(StringUtil.strToList(record.get(TABLE.MODULE_TYPE), Long.class, ","));
        applicationHostDTO.setHostId(record.get(TABLE.HOST_ID).longValue());
        applicationHostDTO.setCloudIp(record.get(TABLE.CLOUD_IP));
        applicationHostDTO.setCloudVendorId(record.get(TABLE.CLOUD_VENDOR_ID));
        applicationHostDTO.setTenantId(record.get(TABLE.TENANT_ID));
        return applicationHostDTO;
    }

    protected HostSimpleDTO extractSimpleData(Record record) {
        if (record == null) {
            return null;
        }
        HostSimpleDTO hostSimpleDTO = new HostSimpleDTO();
        hostSimpleDTO.setBizId(record.get(TABLE.APP_ID).longValue());
        hostSimpleDTO.setAgentAliveStatus(record.get(TABLE.IS_AGENT_ALIVE).intValue());
        hostSimpleDTO.setHostId(record.get(TABLE.HOST_ID).longValue());
        hostSimpleDTO.setAgentId(record.get(TABLE.AGENT_ID));
        hostSimpleDTO.setIpv6(record.get(TABLE.IP_V6));
        hostSimpleDTO.setHostName(record.get(TABLE.IP_DESC));
        hostSimpleDTO.setOsName(record.get(TABLE.OS));
        hostSimpleDTO.setOsType(record.get(TABLE.OS_TYPE));
        hostSimpleDTO.setIp(record.get(TABLE.IP));
        hostSimpleDTO.setCloudAreaId(record.get(TABLE.CLOUD_ID));
        hostSimpleDTO.setCloudIp(hostSimpleDTO.getCloudAreaId() + ":" + hostSimpleDTO.getIp());

        return hostSimpleDTO;
    }

    protected BasicHostDTO extractBasicHost(Record record) {
        if (record == null) {
            return null;
        }
        BasicHostDTO basicHost = new BasicHostDTO();
        basicHost.setHostId(record.get(TABLE.HOST_ID).longValue());
        basicHost.setLastTime(record.get(TABLE.LAST_TIME));
        return basicHost;
    }

    protected List<HostStatusNumStatisticsDTO> countHostStatusNumByBizIds(List<Long> bizIds) {
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

    protected abstract List<Condition> getBasicConditions();
}
