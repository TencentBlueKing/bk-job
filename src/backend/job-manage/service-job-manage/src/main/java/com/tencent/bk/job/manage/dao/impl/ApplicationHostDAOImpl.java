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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.BasicHostDTO;
import com.tencent.bk.job.common.model.dto.HostSimpleDTO;
import com.tencent.bk.job.common.model.dto.HostStatusNumStatisticsDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.common.mysql.util.JooqDataTypeUtil;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.TagUtils;
import com.tencent.bk.job.manage.common.TopologyHelper;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.model.dto.HostTopoDTO;
import com.tencent.bk.job.manage.model.tables.Host;
import com.tencent.bk.job.manage.model.tables.HostTopo;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.BatchBindStep;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.SelectSeekStep2;
import org.jooq.TableField;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.count;

/**
 * 主机DAO
 */
@Slf4j
@Repository
public class ApplicationHostDAOImpl implements ApplicationHostDAO {
    private static final Host TABLE = Host.HOST;
    private static final TableField<?, ?>[] ALL_FIELDS = {
        TABLE.HOST_ID,
        TABLE.APP_ID,
        TABLE.IP,
        TABLE.IP_V6,
        TABLE.AGENT_ID,
        TABLE.IP_DESC,
        TABLE.SET_IDS,
        TABLE.MODULE_IDS,
        TABLE.CLOUD_AREA_ID,
        TABLE.DISPLAY_IP,
        TABLE.OS,
        TABLE.OS_TYPE,
        TABLE.MODULE_TYPE,
        TABLE.IS_AGENT_ALIVE,
        TABLE.CLOUD_IP,
        TABLE.CLOUD_VENDOR_ID
    };

    private static final TableField<?, ?>[] SIMPLE_FIELDS = {
        TABLE.HOST_ID,
        TABLE.APP_ID,
        TABLE.IS_AGENT_ALIVE,
        TABLE.IP,
        TABLE.CLOUD_AREA_ID,
        TABLE.AGENT_ID,
        TABLE.APP_ID,
        TABLE.IP_V6,
        TABLE.IP_DESC,
        TABLE.OS,
        TABLE.OS_TYPE
    };

    private static final TableField<?, ?>[] BASIC_FIELDS = {
        TABLE.HOST_ID,
        TABLE.LAST_TIME
    };

    private final DSLContext context;
    private final ApplicationDAO applicationDAO;
    private final HostTopoDAO hostTopoDAO;
    private final TopologyHelper topologyHelper;

    @Autowired
    public ApplicationHostDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext context,
                                  ApplicationDAO applicationDAO,
                                  HostTopoDAO hostTopoDAO,
                                  TopologyHelper topologyHelper) {
        this.context = context;
        this.applicationDAO = applicationDAO;
        this.topologyHelper = topologyHelper;
        this.hostTopoDAO = hostTopoDAO;
    }

    @Override
    public ApplicationHostDTO getHostById(Long hostId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.HOST_ID.eq(JooqDataTypeUtil.buildULong(hostId)));
        Record record = context.select(ALL_FIELDS).from(TABLE).where(conditions).fetchOne();
        return extractData(record);
    }

    @Override
    public List<Long> listHostId(long bizId, long minUpdateTimeMills, long maxUpdateTimeMills) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.eq(ULong.valueOf(bizId)));
        conditions.add(TABLE.LAST_MODIFY_TIME.greaterThan(JooqDataTypeUtil.buildULong(minUpdateTimeMills)));
        conditions.add(TABLE.LAST_MODIFY_TIME.lessThan(JooqDataTypeUtil.buildULong(maxUpdateTimeMills)));
        return listHostIdByConditions(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByIps(Collection<String> ips) {
        return listHostInfoByIps(null, ips);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByCloudIps(Collection<String> cloudIps) {
        return listHostInfoByCloudIps(null, cloudIps);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByIps(Long bizId, Collection<String> ips) {
        List<String> ipList = new ArrayList<>(ips);
        List<ApplicationHostDTO> hostInfoList = new ArrayList<>();
        // 分批，防止SQL超长
        int batchSize = 30000;
        int start = 0;
        int end = start + batchSize;
        int ipSize = ipList.size();
        end = Math.min(end, ipSize);
        do {
            List<String> ipSubList = ipList.subList(start, end);
            hostInfoList.addAll(listHostInfoByIpsIndeed(bizId, ipSubList));
            start += batchSize;
            end = start + batchSize;
            end = Math.min(end, ipSize);
        } while (start < ipSize);
        return hostInfoList;
    }

    public List<ApplicationHostDTO> listHostInfoByCloudIps(Long bizId, Collection<String> cloudIps) {
        List<String> cloudIpList = new ArrayList<>(cloudIps);
        List<ApplicationHostDTO> hostInfoList = new ArrayList<>();
        // 分批，防止SQL超长
        int batchSize = 30000;
        int start = 0;
        int end = start + batchSize;
        int ipSize = cloudIpList.size();
        end = Math.min(end, ipSize);
        do {
            List<String> ipSubList = cloudIpList.subList(start, end);
            hostInfoList.addAll(listHostInfoByCloudIpsIndeed(bizId, ipSubList));
            start += batchSize;
            end = start + batchSize;
            end = Math.min(end, ipSize);
        } while (start < ipSize);
        return hostInfoList;
    }

    private List<ApplicationHostDTO> listHostInfoByIpsIndeed(Long bizId, Collection<String> ips) {
        List<Condition> conditions = new ArrayList<>();
        if (bizId != null) {
            conditions.addAll(buildBizIdCondition(bizId));
        }
        conditions.add(TABLE.IP.in(ips));
        return queryHostsByCondition(conditions);
    }

    private List<ApplicationHostDTO> listHostInfoByCloudIpsIndeed(Long bizId, Collection<String> cloudIps) {
        List<Condition> conditions = new ArrayList<>();
        if (bizId != null) {
            conditions.addAll(buildBizIdCondition(bizId));
        }
        conditions.add(TABLE.CLOUD_IP.in(cloudIps));
        return queryHostsByCondition(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByBizId(long bizId) {
        List<Condition> conditions = buildBizIdCondition(bizId);
        return listHostInfoByConditions(conditions);
    }

    @Override
    public List<BasicHostDTO> listBasicHostInfo(Collection<Long> hostIds) {
        List<Condition> conditions = buildHostIdsCondition(hostIds);
        return listBasicHostInfoByConditions(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listAllHostInfo(Long start, Long limit) {
        return listHostInfoByConditions(Collections.emptyList(), start, limit);
    }

    private List<ApplicationHostDTO> listHostInfoByConditions(Collection<Condition> conditions) {
        return listHostInfoByConditions(conditions, null, null);
    }

    private List<BasicHostDTO> listBasicHostInfoByConditions(Collection<Condition> conditions) {
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

    private List<Long> listHostIdByConditions(Collection<Condition> conditions) {
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

    private List<ApplicationHostDTO> listHostInfoByConditions(Collection<Condition> conditions,
                                                              Long start,
                                                              Long limit) {
        if (conditions == null) {
            conditions = Collections.emptyList();
        }
        val query = context.select(ALL_FIELDS)
            .from(TABLE)
            .where(conditions)
            .orderBy(TABLE.IS_AGENT_ALIVE.desc(), TABLE.HOST_ID.asc());
        Result<Record> records = fetchRecordsWithLimit(query, start, limit);
        List<ApplicationHostDTO> hostInfoList = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(records)) {
            records.map(record -> hostInfoList.add(extractData(record)));
        }
        return hostInfoList;
    }

    private Result<Record> fetchRecordsWithLimit(SelectSeekStep2<Record, UByte, ULong> query, Long start, Long limit) {
        Result<Record> records;
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

    @Override
    public List<ApplicationHostDTO> listHostInfo(Collection<Long> bizIds, Collection<String> ips) {
        List<Condition> conditions = new ArrayList<>();
        if (bizIds != null) {
            conditions.add(TABLE.APP_ID.in(bizIds.stream().map(ULong::valueOf).collect(Collectors.toList())));
        }
        conditions.add(TABLE.IP.in(ips.stream().map(String::trim).collect(Collectors.toList())));
        return listHostInfoByConditions(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByBizAndCloudIPs(Collection<Long> bizIds, Collection<String> cloudIPs) {
        List<Condition> conditions = new ArrayList<>();
        if (bizIds != null) {
            conditions.add(TABLE.APP_ID.in(bizIds.stream().map(ULong::valueOf).collect(Collectors.toList())));
        }
        conditions.add(TABLE.CLOUD_IP.in(cloudIPs.stream().map(String::trim).collect(Collectors.toList())));
        return listHostInfoByConditions(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByBizAndIps(Collection<Long> bizIds, Collection<String> ips) {
        List<Condition> conditions = new ArrayList<>();
        if (bizIds != null) {
            conditions.add(TABLE.APP_ID.in(bizIds.stream().map(ULong::valueOf).collect(Collectors.toList())));
        }
        conditions.add(TABLE.IP.in(ips));
        return listHostInfoByConditions(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByBizAndCloudIps(Collection<Long> bizIds, Collection<String> cloudIps) {
        List<Condition> conditions = new ArrayList<>();
        if (bizIds != null) {
            conditions.add(TABLE.APP_ID.in(bizIds.stream().map(ULong::valueOf).collect(Collectors.toList())));
        }
        conditions.add(TABLE.CLOUD_IP.in(cloudIps));
        return listHostInfoByConditions(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByBizAndIpv6s(Collection<Long> bizIds, Collection<String> ipv6s) {
        List<Condition> conditions = new ArrayList<>();
        if (bizIds != null) {
            conditions.add(TABLE.APP_ID.in(bizIds.stream().map(ULong::valueOf).collect(Collectors.toList())));
        }
        conditions.add(TABLE.IP_V6.in(ipv6s));
        return listHostInfoByConditions(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByBizAndHostNames(Collection<Long> bizIds,
                                                                  Collection<String> hostNames) {
        List<Condition> conditions = new ArrayList<>();
        if (bizIds != null) {
            conditions.add(TABLE.APP_ID.in(bizIds.stream().map(ULong::valueOf).collect(Collectors.toList())));
        }
        conditions.add(TABLE.IP_DESC.in(hostNames));
        return listHostInfoByConditions(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByBizIds(Collection<Long> bizIds, Long start, Long limit) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.in(bizIds.stream().map(ULong::valueOf).collect(Collectors.toList())));
        return listHostInfoByConditions(conditions, start, limit);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByHostIds(Collection<Long> hostIds) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.HOST_ID.in(hostIds.stream().map(ULong::valueOf).collect(Collectors.toList())));
        return listHostInfoByConditions(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByIpv6s(Collection<String> ipv6s) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.IP_V6.in(ipv6s));
        return listHostInfoByConditions(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByCloudIpv6(Long cloudAreaId, String ipv6) {
        List<Condition> conditions = new ArrayList<>();
        if (cloudAreaId != null) {
            conditions.add(TABLE.CLOUD_AREA_ID.eq(JooqDataTypeUtil.buildULong(cloudAreaId)));
        }
        conditions.add(TABLE.IP_V6.like("%" + ipv6 + "%"));
        return listHostInfoByConditions(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByHostNames(Collection<String> hostNames) {
        List<Condition> conditions = new ArrayList<>();
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

    private List<Condition> buildConditions(Collection<Long> bizIds,
                                            Collection<Long> moduleIds,
                                            Integer agentAlive) {
        Host tHost = Host.HOST;
        HostTopo tHostTopo = HostTopo.HOST_TOPO;
        List<Condition> conditions = new ArrayList<>();
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

    private List<Long> getHostIdListByConditions(Collection<Condition> conditions,
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
        Result<Record> records = fetchRecordsWithLimit(query, start, limit);
        List<Long> hostIdList = new ArrayList<>();
        if (records.size() >= 1) {
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

    @Override
    public PageData<ApplicationHostDTO> listHostInfoByPage(ApplicationHostDTO applicationHostInfoCondition,
                                                           BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = buildBizIdCondition(applicationHostInfoCondition.getBizId());
        conditions.addAll(buildCondition(applicationHostInfoCondition));

        long hostCount = countHostByConditions(conditions);

        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);

        Result<Record> result = context.select(ALL_FIELDS)
            .from(TABLE)
            .where(conditions)
            .orderBy(TABLE.IS_AGENT_ALIVE.desc(), TABLE.HOST_ID.asc())
            .limit(start, length)
            .fetch();

        List<ApplicationHostDTO> hostInfoList = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(result)) {
            result.map(record -> hostInfoList.add(extractData(record)));
        }

        PageData<ApplicationHostDTO> hostInfoPageData = new PageData<>();
        hostInfoPageData.setTotal(hostCount);
        hostInfoPageData.setStart(start);
        hostInfoPageData.setPageSize(length);
        hostInfoPageData.setData(hostInfoList);

        return hostInfoPageData;
    }

    @Override
    public int insertHostWithoutTopo(ApplicationHostDTO applicationHostDTO) {
        return insertOrUpdateHost(context, applicationHostDTO);
    }

    private int insertOrUpdateHost(DSLContext defaultContext,
                                   ApplicationHostDTO applicationHostDTO) {
        int result;
        String finalSetIdsStr = applicationHostDTO.getSetIdsStr();
        String finalModuleIdsStr = applicationHostDTO.getModuleIdsStr();
        String finalModuleTypeStr = applicationHostDTO.getModuleTypeStr();
        ULong bizId = ULong.valueOf(applicationHostDTO.getBizId());
        String ip = applicationHostDTO.getIp();
        String ipv6 = applicationHostDTO.preferFullIpv6();
        String agentId = applicationHostDTO.getAgentId();
        String ipDesc = applicationHostDTO.getHostName();
        ULong cloudAreaId = ULong.valueOf(applicationHostDTO.getCloudAreaId());
        String displayIp = applicationHostDTO.getDisplayIp();
        String os = applicationHostDTO.getOsName();
        String osType = applicationHostDTO.getOsType();
        UByte gseAgentAlive = UByte.valueOf(applicationHostDTO.getGseAgentAlive() ? 1 : 0);
        String cloudIp = applicationHostDTO.getCloudIp();
        String cloudVendor = applicationHostDTO.getCloudVendorId();
        Long lastTime = applicationHostDTO.getLastTime();
        var query = defaultContext.insertInto(TABLE,
            TABLE.HOST_ID,
            TABLE.APP_ID,
            TABLE.IP,
            TABLE.IP_V6,
            TABLE.AGENT_ID,
            TABLE.IP_DESC,
            TABLE.SET_IDS,
            TABLE.MODULE_IDS,
            TABLE.CLOUD_AREA_ID,
            TABLE.DISPLAY_IP,
            TABLE.OS,
            TABLE.OS_TYPE,
            TABLE.MODULE_TYPE,
            TABLE.IS_AGENT_ALIVE,
            TABLE.CLOUD_IP,
            TABLE.LAST_MODIFY_TIME,
            TABLE.CLOUD_VENDOR_ID,
            TABLE.LAST_TIME
        ).values(
            JooqDataTypeUtil.buildULong(applicationHostDTO.getHostId()),
            bizId,
            ip,
            ipv6,
            agentId,
            ipDesc,
            finalSetIdsStr,
            finalModuleIdsStr,
            cloudAreaId,
            displayIp,
            os,
            osType,
            finalModuleTypeStr,
            gseAgentAlive,
            cloudIp,
            JooqDataTypeUtil.buildULong(System.currentTimeMillis()),
            cloudVendor,
            lastTime
        );
        try {
            result = query.onDuplicateKeyUpdate()
                .set(TABLE.APP_ID, bizId)
                .set(TABLE.IP, ip)
                .set(TABLE.IP_V6, ipv6)
                .set(TABLE.AGENT_ID, agentId)
                .set(TABLE.IP_DESC, ipDesc)
                .set(TABLE.SET_IDS, finalSetIdsStr)
                .set(TABLE.MODULE_IDS, finalModuleIdsStr)
                .set(TABLE.CLOUD_AREA_ID, cloudAreaId)
                .set(TABLE.DISPLAY_IP, displayIp)
                .set(TABLE.OS, os)
                .set(TABLE.OS_TYPE, osType)
                .set(TABLE.MODULE_TYPE, finalModuleTypeStr)
                .set(TABLE.IS_AGENT_ALIVE, gseAgentAlive)
                .set(TABLE.CLOUD_IP, cloudIp)
                .set(TABLE.CLOUD_VENDOR_ID, cloudVendor)
                .set(TABLE.LAST_TIME, lastTime)
                .execute();
        } catch (Throwable t) {
            log.info("SQL=" + query.getSQL(ParamType.INLINED));
            throw t;
        }
        return result;
    }

    @Override
    public int batchInsertHost(List<ApplicationHostDTO> applicationHostDTOList) {
        int batchSize = 1000;
        int size = applicationHostDTOList.size();
        int start = 0;
        int end;
        int affectedNum = 0;
        do {
            end = start + batchSize;
            end = Math.min(end, size);
            List<ApplicationHostDTO> subList = applicationHostDTOList.subList(start, end);
            val insertQuery = context.insertInto(TABLE,
                TABLE.HOST_ID,
                TABLE.APP_ID,
                TABLE.IP,
                TABLE.IP_V6,
                TABLE.AGENT_ID,
                TABLE.IP_DESC,
                TABLE.SET_IDS,
                TABLE.MODULE_IDS,
                TABLE.CLOUD_AREA_ID,
                TABLE.DISPLAY_IP,
                TABLE.OS,
                TABLE.OS_TYPE,
                TABLE.MODULE_TYPE,
                TABLE.IS_AGENT_ALIVE,
                TABLE.CLOUD_IP,
                TABLE.LAST_MODIFY_TIME,
                TABLE.CLOUD_VENDOR_ID,
                TABLE.LAST_TIME
            ).values(
                (ULong) null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            );
            BatchBindStep batchQuery = context.batch(insertQuery);
            for (ApplicationHostDTO applicationHostDTO : subList) {
                batchQuery = batchQuery.bind(
                    JooqDataTypeUtil.buildULong(applicationHostDTO.getHostId()),
                    JooqDataTypeUtil.buildULong(applicationHostDTO.getBizId()),
                    applicationHostDTO.getIp(),
                    applicationHostDTO.preferFullIpv6(),
                    applicationHostDTO.getAgentId(),
                    applicationHostDTO.getHostName(),
                    applicationHostDTO.getSetIdsStr(),
                    applicationHostDTO.getModuleIdsStr(),
                    JooqDataTypeUtil.buildULong(applicationHostDTO.getCloudAreaId()),
                    applicationHostDTO.getDisplayIp(),
                    applicationHostDTO.getOsName(),
                    applicationHostDTO.getOsType(),
                    applicationHostDTO.getModuleTypeStr(),
                    JooqDataTypeUtil.buildUByte(applicationHostDTO.getAgentStatusValue()),
                    applicationHostDTO.getCloudIp(),
                    JooqDataTypeUtil.buildULong(System.currentTimeMillis()),
                    applicationHostDTO.getCloudVendorId(),
                    applicationHostDTO.getLastTime()
                );
            }
            int[] results = batchQuery.execute();
            for (int result : results) {
                affectedNum += result;
            }
            start += batchSize;
        } while (end < size);
        return affectedNum;
    }

    @SuppressWarnings("all")
    @Override
    public boolean existAppHostInfoByHostId(Long hostId) {
        val query = context.selectCount().from(TABLE)
            .where(TABLE.HOST_ID.eq(JooqDataTypeUtil.buildULong(hostId)));
        try {
            return query.fetchOne(0, Long.class) >= 1;
        } catch (Throwable t) {
            log.info("SQL=" + query.getSQL(ParamType.INLINED));
            throw t;
        }
    }

    @Override
    public int updateHostAttrsByHostId(ApplicationHostDTO applicationHostDTO) {
        checkHostId(applicationHostDTO);
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.HOST_ID.eq(ULong.valueOf(applicationHostDTO.getHostId())));
        return updateHostAttrsByConditions(applicationHostDTO, conditions);
    }

    @Override
    public int updateHostAttrsBeforeLastTime(ApplicationHostDTO applicationHostDTO) {
        checkHostId(applicationHostDTO);
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.HOST_ID.eq(ULong.valueOf(applicationHostDTO.getHostId())));
        conditions.add(TABLE.LAST_TIME.lessThan(applicationHostDTO.getLastTime()));
        return updateHostAttrsByConditions(applicationHostDTO, conditions);
    }

    private void checkHostId(ApplicationHostDTO applicationHostDTO) {
        Long hostId = applicationHostDTO.getHostId();
        if (hostId == null || hostId <= 0) {
            FormattingTuple msg = MessageFormatter.format(
                "fail to update host, hostId is invalid:{}",
                applicationHostDTO
            );
            log.error(msg.getMessage());
            throw new InternalException(msg.getMessage(), ErrorCode.INTERNAL_ERROR);
        }
    }

    public int updateHostAttrsByConditions(ApplicationHostDTO applicationHostDTO, Collection<Condition> conditions) {
        val query = context.update(TABLE)
            .set(TABLE.CLOUD_AREA_ID, ULong.valueOf(applicationHostDTO.getCloudAreaId()))
            .set(TABLE.IP, applicationHostDTO.getIp())
            .set(TABLE.IP_V6, applicationHostDTO.preferFullIpv6())
            .set(TABLE.AGENT_ID, applicationHostDTO.getAgentId())
            .set(TABLE.DISPLAY_IP, applicationHostDTO.getDisplayIp())
            .set(TABLE.CLOUD_IP, applicationHostDTO.getCloudIp())
            .set(TABLE.IP_DESC, applicationHostDTO.getHostName())
            .set(TABLE.OS, applicationHostDTO.getOsName())
            .set(TABLE.OS_TYPE, applicationHostDTO.getOsType())
            .set(TABLE.IS_AGENT_ALIVE, UByte.valueOf(applicationHostDTO.getAgentStatusValue()))
            .set(TABLE.LAST_MODIFY_TIME, JooqDataTypeUtil.buildULong(System.currentTimeMillis()))
            .set(TABLE.CLOUD_VENDOR_ID, applicationHostDTO.getCloudVendorId())
            .set(TABLE.LAST_TIME, applicationHostDTO.getLastTime())
            .where(conditions);
        try {
            return query.execute();
        } catch (Throwable t) {
            log.info("SQL=" + query.getSQL(ParamType.INLINED));
            throw t;
        }
    }

    private Query buildQueryWithHostAndConditions(ApplicationHostDTO applicationHostDTO, List<Condition> conditions) {
        return context.update(TABLE)
            .set(TABLE.APP_ID, JooqDataTypeUtil.buildULong(applicationHostDTO.getBizId()))
            .set(TABLE.IP, applicationHostDTO.getIp())
            .set(TABLE.IP_V6, applicationHostDTO.preferFullIpv6())
            .set(TABLE.AGENT_ID, applicationHostDTO.getAgentId())
            .set(TABLE.CLOUD_IP, applicationHostDTO.getCloudIp())
            .set(TABLE.IP_DESC, applicationHostDTO.getHostName())
            .set(TABLE.SET_IDS, applicationHostDTO.getSetIdsStr())
            .set(TABLE.MODULE_IDS, applicationHostDTO.getModuleIdsStr())
            .set(TABLE.CLOUD_AREA_ID, JooqDataTypeUtil.buildULong(applicationHostDTO.getCloudAreaId()))
            .set(TABLE.DISPLAY_IP, applicationHostDTO.getDisplayIp())
            .set(TABLE.OS, applicationHostDTO.getOsName())
            .set(TABLE.OS_TYPE, applicationHostDTO.getOsType())
            .set(TABLE.MODULE_TYPE, applicationHostDTO.getModuleTypeStr())
            .set(TABLE.IS_AGENT_ALIVE, JooqDataTypeUtil.buildUByte(applicationHostDTO.getAgentStatusValue()))
            .set(TABLE.LAST_MODIFY_TIME, JooqDataTypeUtil.buildULong(System.currentTimeMillis()))
            .set(TABLE.CLOUD_VENDOR_ID, applicationHostDTO.getCloudVendorId())
            .set(TABLE.LAST_TIME, applicationHostDTO.getLastTime())
            .where(conditions);
    }

    @Override
    public int batchUpdateHostsBeforeLastTime(List<ApplicationHostDTO> hostList) {
        int batchSize = 1000;
        int size = hostList.size();
        int start = 0;
        int end;
        List<Query> queryList = new ArrayList<>();
        int affectedNum = 0;
        do {
            end = start + batchSize;
            end = Math.min(end, size);
            List<ApplicationHostDTO> subList = hostList.subList(start, end);
            for (ApplicationHostDTO host : subList) {
                List<Condition> conditions = new ArrayList<>();
                conditions.add(TABLE.HOST_ID.eq(ULong.valueOf(host.getHostId())));
                conditions.add(TABLE.LAST_TIME.lessThan(host.getLastTime()));
                queryList.add(buildQueryWithHostAndConditions(host, conditions));
            }
            int[] results = context.batch(queryList).execute();
            queryList.clear();
            for (int result : results) {
                affectedNum += result;
            }
            start += batchSize;
        } while (end < size);
        return affectedNum;
    }

    @JobTransactional(transactionManager = "jobManageTransactionManager")
    @Override
    public int deleteHostBeforeOrEqualLastTime(Long bizId, Long hostId, Long lastTime) {
        int affectedNum;
        List<Condition> conditions = new ArrayList<>();
        if (bizId != null) {
            conditions.add(TABLE.APP_ID.eq(JooqDataTypeUtil.buildULong(bizId)));
        }
        if (hostId != null) {
            conditions.add(TABLE.HOST_ID.eq(JooqDataTypeUtil.buildULong(hostId)));
        }
        if (lastTime != null) {
            conditions.add(TABLE.LAST_TIME.lessOrEqual(lastTime));
        }
        affectedNum = context.deleteFrom(TABLE)
            .where(conditions)
            .execute();
        hostTopoDAO.deleteHostTopoByHostId(bizId, hostId);
        return affectedNum;
    }

    @JobTransactional(transactionManager = "jobManageTransactionManager")
    @Override
    public int batchDeleteHostById(List<Long> hostIdList) {
        if (CollectionUtils.isEmpty(hostIdList)) {
            return 0;
        }
        List<Condition> conditions = new ArrayList<>();
        conditions.add(
            TABLE.HOST_ID.in(hostIdList.stream().map(ULong::valueOf).collect(Collectors.toList()))
        );
        int deletedRelationNum = hostTopoDAO.batchDeleteHostTopo(hostIdList);
        log.info("{} host relation deleted", deletedRelationNum);
        return context.deleteFrom(TABLE)
            .where(conditions)
            .execute();
    }

    @JobTransactional(transactionManager = "jobManageTransactionManager")
    @Override
    public int batchDeleteBizHostInfoById(Long bizId, List<Long> hostIdList) {
        int affectedNum = 0;
        int batchSize = 1000;
        int size = hostIdList.size();
        int start = 0;
        int end;
        List<Query> queryList = new ArrayList<>();
        do {
            end = start + batchSize;
            end = Math.min(end, size);
            List<Long> subList = hostIdList.subList(start, end);
            if (bizId != null) {
                queryList.add(context.deleteFrom(TABLE)
                    .where(TABLE.HOST_ID.in(subList.stream().map(ULong::valueOf).collect(Collectors.toList())))
                    .and(TABLE.APP_ID.eq(ULong.valueOf(bizId)))
                );
            } else {
                queryList.add(context.deleteFrom(TABLE)
                    .where(TABLE.HOST_ID.in(subList.stream().map(ULong::valueOf).collect(Collectors.toList())))
                );
            }
            // SQL语句达到批量即执行
            if (queryList.size() >= batchSize) {
                int[] results = context.batch(queryList).execute();
                queryList.clear();
                for (int result : results) {
                    affectedNum += result;
                }
            }
            hostTopoDAO.batchDeleteHostTopo(subList);
            start += batchSize;
        } while (end < size);
        if (!queryList.isEmpty()) {
            int[] results = context.batch(queryList).execute();
            for (int result : results) {
                affectedNum += result;
            }
        }
        hostTopoDAO.batchDeleteHostTopo(hostIdList);
        return affectedNum;
    }

    @Override
    public int deleteBizHostInfoByBizId(long bizId) {
        // 先查出所有的hostId
        List<Long> hostIds = getHostIdListBySearchContents(
            Collections.singleton(bizId),
            null,
            null,
            null,
            null,
            null,
            null
        );
        // 删除拓扑信息+主机信息
        return batchDeleteBizHostInfoById(bizId, hostIds);
    }

    @Override
    public int deleteByBasicHost(List<BasicHostDTO> basicHostList) {
        int affectedNum = 0;
        int batchSize = 1000;
        int size = basicHostList.size();
        int start = 0;
        int end;
        List<Query> queryList = new ArrayList<>();
        do {
            end = start + batchSize;
            end = Math.min(end, size);
            List<BasicHostDTO> subList = basicHostList.subList(start, end);
            for (BasicHostDTO basicHost : subList) {
                queryList.add(context.deleteFrom(TABLE)
                    .where(TABLE.HOST_ID.eq(JooqDataTypeUtil.buildULong(basicHost.getHostId())))
                    .and(TABLE.LAST_TIME.eq(basicHost.getLastTime()))
                );
            }
            int[] results = context.batch(queryList).execute();
            queryList.clear();
            for (int result : results) {
                affectedNum += result;
            }
            start += batchSize;
        } while (end < size);
        return affectedNum;
    }

    @Override
    public List<HostSimpleDTO> listAllHostSimpleInfo() {
        val query = context.select(SIMPLE_FIELDS)
            .from(TABLE);
        Result<Record> records = query.fetch();
        List<HostSimpleDTO> hostInfoList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(records)) {
            records.map(record -> hostInfoList.add(extractSimpleData(record)));
        }
        return hostInfoList;
    }

    @Override
    public List<BasicHostDTO> listAllBasicHost() {
        val query = context.select(BASIC_FIELDS)
            .from(TABLE);
        Result<Record> records = query.fetch();
        List<BasicHostDTO> basicHostList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(records)) {
            records.map(record -> basicHostList.add(extractBasicHost(record)));
        }
        return basicHostList;
    }

    @Override
    public int batchUpdateHostStatusByHostIds(int status, List<Long> hostIdList) {
        return context.update(TABLE)
            .set(TABLE.IS_AGENT_ALIVE, UByte.valueOf(status))
            .where(TABLE.HOST_ID.in(hostIdList))
            .execute();
    }

    @Override
    public long countHostsByBizIds(Collection<Long> bizIds) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.in(bizIds));
        return countHostByConditions(conditions);
    }

    @Override
    public long countAllHosts() {
        log.debug("countAllHosts");
        return countHostByConditions(null);
    }

    @Override
    public long countHostsByOsType(String osType) {
        List<Condition> conditions = new ArrayList<>();
        if (osType != null) {
            conditions.add(TABLE.OS_TYPE.eq(osType));
        }
        return countHostByConditions(conditions);
    }

    @Override
    public Map<String, Integer> groupHostByOsType() {
        Map<String, Integer> groupMap = new HashMap<>();
        context.select(
                TABLE.OS_TYPE,
                count()
            )
            .from(TABLE)
            .groupBy(TABLE.OS_TYPE)
            .fetch()
            .map(record -> {
                String osType = record.get(0, String.class);
                if (StringUtils.isNotBlank(osType)) {
                    groupMap.put(osType, record.get(1, Integer.class));
                } else {
                    groupMap.put("null", record.get(1, Integer.class));
                }
                return record;
            });
        return groupMap;
    }

    @Override
    public int syncHostTopo(Long hostId) {
        ApplicationHostDTO hostInfoDTO = getHostById(hostId);
        if (hostInfoDTO != null) {
            List<HostTopoDTO> hostTopoDTOList = hostTopoDAO.listHostTopoByHostId(hostId);
            List<Long> setIds =
                hostTopoDTOList.stream().map(HostTopoDTO::getSetId).collect(Collectors.toList());
            List<Long> moduleIds =
                hostTopoDTOList.stream().map(HostTopoDTO::getModuleId).collect(Collectors.toList());
            List<Long> moduleTypes = moduleIds.stream().map(it -> 1L).collect(Collectors.toList());
            if (!hostTopoDTOList.isEmpty()) {
                hostInfoDTO.setBizId(hostTopoDTOList.get(0).getBizId());
            } else {
                hostInfoDTO.setBizId(JobConstants.PUBLIC_APP_ID);
            }
            hostInfoDTO.setSetId(setIds);
            hostInfoDTO.setModuleId(moduleIds);
            hostInfoDTO.setModuleType(moduleTypes);
            return updateHostTopoAttrsByHostId(hostInfoDTO);
        }
        return -1;
    }

    private int updateHostTopoAttrsByHostId(ApplicationHostDTO host) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.HOST_ID.eq(ULong.valueOf(host.getHostId())));
        val query = context.update(TABLE)
            .set(TABLE.APP_ID, ULong.valueOf(host.getBizId()))
            .set(TABLE.SET_IDS, host.getSetIdsStr())
            .set(TABLE.MODULE_IDS, host.getModuleIdsStr())
            .set(TABLE.MODULE_TYPE, host.getModuleTypeStr())
            .where(conditions);
        try {
            return query.execute();
        } catch (Throwable t) {
            log.info("SQL=" + query.getSQL(ParamType.INLINED));
            throw t;
        }
    }

    /**
     * 查询符合条件的主机数量
     */
    @SuppressWarnings("all")
    private long countHostByConditions(List<Condition> conditions) {
        if (conditions == null) {
            conditions = Collections.emptyList();
        }
        return context.selectCount().from(TABLE).where(conditions).fetchOne(0, Long.class);
    }

    private List<Condition> buildHostIdsCondition(Collection<Long> hostIds) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.HOST_ID.in(hostIds));
        return conditions;
    }

    private List<Condition> buildBizIdCondition(long bizId) {
        ApplicationDTO appInfo = applicationDAO.getAppByScope(
            new ResourceScope(ResourceScopeTypeEnum.BIZ, "" + bizId)
        );
        List<Condition> conditions = new ArrayList<>();
        if (appInfo.isBiz()) {
            conditions.add(TABLE.APP_ID.eq(JooqDataTypeUtil.buildULong(bizId)));
        } else if (!appInfo.isAllBizSet() && appInfo.isBizSet()) {
            List<Long> subBizIds = topologyHelper.getBizSetSubBizIds(appInfo);
            conditions.add(TABLE.APP_ID.in(subBizIds.stream().map(ULong::valueOf).collect(Collectors.toList())));
        }
        return conditions;
    }

    private List<Condition> buildCondition(ApplicationHostDTO applicationHostInfoCondition) {
        List<Condition> conditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(applicationHostInfoCondition.getIpList())) {
            conditions.add(TABLE.IP.in(applicationHostInfoCondition.getIpList()));
        }

        if (applicationHostInfoCondition.getGseAgentAlive() != null) {
            conditions.add(TABLE.IS_AGENT_ALIVE.eq(JooqDataTypeUtil.buildUByte(applicationHostInfoCondition.getGseAgentAlive() ? 1
                : 0)));
        }

        if (CollectionUtils.isNotEmpty(applicationHostInfoCondition.getModuleType())) {
            if (applicationHostInfoCondition.getModuleType().size() != 1
                || applicationHostInfoCondition.getModuleType().get(0) != 0) {
                conditions.add(TABLE.MODULE_TYPE
                    .like("%" + TagUtils.buildDbTag(applicationHostInfoCondition.getModuleType().get(0)) + "%"));
            }
        }

        if (StringUtils.isNotBlank(applicationHostInfoCondition.getIp())) {
            String ipQueryString = "%" + applicationHostInfoCondition.getIp() + "%";
            conditions.add(TABLE.IP.like(ipQueryString).or(TABLE.DISPLAY_IP.like(ipQueryString)));
        }
        return conditions;
    }

    @Override
    public List<ApplicationHostDTO> listHostsByCloudIps(Collection<String> cloudIps) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.CLOUD_IP.in(cloudIps));
        return queryHostsByCondition(conditions);
    }

    @Override
    public List<HostStatusNumStatisticsDTO> countHostStatusNumByBizIds(List<Long> bizIds) {
        List<Condition> conditions = new ArrayList<>();
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
        if (records.size() > 0) {
            records.forEach(record -> {
                HostStatusNumStatisticsDTO statisticsDTO = new HostStatusNumStatisticsDTO();
                statisticsDTO.setHostNum(record.get(HostStatusNumStatisticsDTO.KEY_HOST_NUM, Integer.class));
                statisticsDTO.setGseAgentAlive(record.get(HostStatusNumStatisticsDTO.KEY_AGENT_ALIVE, Integer.class));
                countList.add(statisticsDTO);
            });
        }
        return countList;
    }

    private List<ApplicationHostDTO> queryHostsByCondition(List<Condition> conditions) {
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

    public static ApplicationHostDTO extractData(Record record) {
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
                if (!id.trim().equals("")) {
                    Long parseLong = Long.parseLong(id);
                    list.add(parseLong);
                }
            }
            setIdList = list;
        }
        applicationHostDTO.setSetId(setIdList);
        applicationHostDTO.setModuleId(StringUtil.strToList(record.get(TABLE.MODULE_IDS), Long.class, ","));
        applicationHostDTO.setCloudAreaId(record.get(TABLE.CLOUD_AREA_ID).longValue());
        applicationHostDTO.setDisplayIp(record.get(TABLE.DISPLAY_IP));
        applicationHostDTO.setOsName(record.get(TABLE.OS));
        applicationHostDTO.setOsType(record.get(TABLE.OS_TYPE));
        applicationHostDTO.setModuleType(StringUtil.strToList(record.get(TABLE.MODULE_TYPE), Long.class, ","));
        applicationHostDTO.setHostId(record.get(TABLE.HOST_ID).longValue());
        applicationHostDTO.setCloudIp(record.get(TABLE.CLOUD_IP));
        applicationHostDTO.setCloudVendorId(record.get(TABLE.CLOUD_VENDOR_ID));
        return applicationHostDTO;
    }

    public static HostSimpleDTO extractSimpleData(Record record) {
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
        hostSimpleDTO.setCloudAreaId(record.get(TABLE.CLOUD_AREA_ID).longValue());
        hostSimpleDTO.setCloudIp(hostSimpleDTO.getCloudAreaId() + ":" + hostSimpleDTO.getIp());

        return hostSimpleDTO;
    }

    public static BasicHostDTO extractBasicHost(Record record) {
        if (record == null) {
            return null;
        }
        BasicHostDTO basicHost = new BasicHostDTO();
        basicHost.setHostId(record.get(TABLE.HOST_ID).longValue());
        basicHost.setLastTime(record.get(TABLE.LAST_TIME));
        return basicHost;
    }
}
