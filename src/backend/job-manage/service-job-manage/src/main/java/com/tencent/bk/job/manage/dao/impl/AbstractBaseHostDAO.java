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
import com.tencent.bk.job.common.model.dto.BasicHostDTO;
import com.tencent.bk.job.common.model.dto.HostSimpleDTO;
import com.tencent.bk.job.common.mysql.util.JooqDataTypeUtil;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.manage.common.TopologyHelper;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.model.tables.Host;
import com.tencent.bk.job.manage.model.tables.HostTopo;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.SelectSeekStep2;
import org.jooq.TableField;
import org.jooq.conf.ParamType;
import org.jooq.types.UByte;
import org.jooq.types.ULong;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        TABLE.CLOUD_AREA_ID,
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
        TABLE.CLOUD_AREA_ID,
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
        Result<Record> records = fetchRecordsWithLimit(query, start, limit);
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
        Result<Record> records = fetchRecordsWithLimit(query, start, limit);
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

    protected Result<Record> fetchRecordsWithLimit(SelectSeekStep2<Record, UByte, ULong> query,
                                                   Long start,
                                                   Long limit) {
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
        applicationHostDTO.setCloudAreaId(record.get(TABLE.CLOUD_AREA_ID).longValue());
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
        hostSimpleDTO.setCloudAreaId(record.get(TABLE.CLOUD_AREA_ID).longValue());
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

    protected abstract List<Condition> getBasicConditions();
}
