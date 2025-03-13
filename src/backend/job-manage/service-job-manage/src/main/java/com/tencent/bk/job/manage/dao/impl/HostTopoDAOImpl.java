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

import com.tencent.bk.job.common.mysql.util.JooqDataTypeUtil;
import com.tencent.bk.job.common.util.CollectionUtil;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.model.dto.HostTopoDTO;
import com.tencent.bk.job.manage.model.tables.HostTopo;
import com.tencent.bk.job.manage.model.tables.records.HostTopoRecord;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.BatchBindStep;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.DeleteConditionStep;
import org.jooq.Query;
import org.jooq.Result;
import org.jooq.UpdateConditionStep;
import org.jooq.conf.ParamType;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class HostTopoDAOImpl implements HostTopoDAO {

    private static final HostTopo defaultTable = HostTopo.HOST_TOPO;
    private final DSLContext defaultContext;

    @Autowired
    public HostTopoDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext dslContext) {
        this.defaultContext = dslContext;
    }

    @Override
    public int insertHostTopo(HostTopoDTO hostTopoDTO) {
        val query = defaultContext.insertInto(defaultTable,
            defaultTable.HOST_ID,
            defaultTable.APP_ID,
            defaultTable.SET_ID,
            defaultTable.MODULE_ID,
            defaultTable.LAST_TIME
        ).values(
            ULong.valueOf(hostTopoDTO.getHostId()),
            ULong.valueOf(hostTopoDTO.getBizId()),
            hostTopoDTO.getSetId(),
            hostTopoDTO.getModuleId(),
            hostTopoDTO.getLastTime()
        ).onDuplicateKeyIgnore();
        return query.execute();
    }

    @Override
    public int batchInsertHostTopo(List<HostTopoDTO> hostTopoDTOList) {
        if (CollectionUtils.isEmpty(hostTopoDTOList)) {
            return 0;
        }
        int batchSize = 1000;
        int size = hostTopoDTOList.size();
        int start = 0;
        int end;
        int affectedNum = 0;
        do {
            end = start + batchSize;
            end = Math.min(end, size);
            List<HostTopoDTO> subList = hostTopoDTOList.subList(start, end);
            val insertQuery = defaultContext.insertInto(defaultTable,
                defaultTable.HOST_ID,
                defaultTable.APP_ID,
                defaultTable.SET_ID,
                defaultTable.MODULE_ID,
                defaultTable.LAST_TIME
            ).values(
                (ULong) null,
                null,
                null,
                null,
                null
            ).onDuplicateKeyIgnore();
            BatchBindStep batchQuery = defaultContext.batch(insertQuery);
            for (HostTopoDTO hostTopoDTO : subList) {
                batchQuery = batchQuery.bind(
                    ULong.valueOf(hostTopoDTO.getHostId()),
                    hostTopoDTO.getBizId(),
                    hostTopoDTO.getSetId(),
                    hostTopoDTO.getModuleId(),
                    hostTopoDTO.getLastTime()
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

    @Override
    public void deleteHostTopoByHostId(Long bizId, Long hostId) {
        List<Condition> conditions = new ArrayList<>();
        if (bizId != null) {
            conditions.add(defaultTable.APP_ID.eq(ULong.valueOf(bizId)));
        }
        if (hostId != null) {
            conditions.add(defaultTable.HOST_ID.eq(ULong.valueOf(hostId)));
        }
        defaultContext.deleteFrom(defaultTable).where(
            conditions
        ).execute();
    }

    @Override
    public int deleteHostTopoBeforeOrEqualLastTime(Long hostId, Long bizId, Long setId, Long moduleId, Long lastTime) {
        return defaultContext.deleteFrom(defaultTable)
            .where(defaultTable.HOST_ID.eq(ULong.valueOf(hostId)))
            .and(defaultTable.APP_ID.eq(ULong.valueOf(bizId)))
            .and(defaultTable.SET_ID.eq(setId))
            .and(defaultTable.MODULE_ID.eq(moduleId))
            .and(defaultTable.LAST_TIME.lessOrEqual(lastTime))
            .execute();
    }

    @Override
    public int batchDeleteHostTopo(Long bizId, List<Long> hostIdList) {
        int batchSize = 1000;
        int maxQueryNum = 100;
        int size = hostIdList.size();
        int start = 0;
        int end;
        List<Query> queryList = new ArrayList<>();
        int affectedNum = 0;
        do {
            end = Math.min(start + batchSize, size);
            List<Long> subList = hostIdList.subList(start, end);
            DeleteConditionStep<HostTopoRecord> step = defaultContext.deleteFrom(defaultTable)
                .where(defaultTable.HOST_ID.in(subList.stream().map(ULong::valueOf).collect(Collectors.toList())));
            if (bizId != null) {
                step = step.and(defaultTable.APP_ID.eq(JooqDataTypeUtil.buildULong(bizId)));
            }
            queryList.add(step);
            // SQL语句达到最大语句数量即执行
            if (queryList.size() >= maxQueryNum) {
                int[] results = defaultContext.batch(queryList).execute();
                queryList.clear();
                for (int result : results) {
                    affectedNum += result;
                }
            }
            start += batchSize;
        } while (end < size);
        if (!queryList.isEmpty()) {
            int[] results = defaultContext.batch(queryList).execute();
            for (int result : results) {
                affectedNum += result;
            }
        }
        return affectedNum;
    }

    private List<Condition> buildHostTopoMainFieldCondition(HostTopoDTO hostTopo) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.HOST_ID.eq(JooqDataTypeUtil.buildULong(hostTopo.getHostId())));
        conditions.add(defaultTable.APP_ID.eq(JooqDataTypeUtil.buildULong(hostTopo.getBizId())));
        conditions.add(defaultTable.SET_ID.eq(hostTopo.getSetId()));
        conditions.add(defaultTable.MODULE_ID.eq(hostTopo.getModuleId()));
        return conditions;
    }

    @Override
    public int batchUpdateBeforeLastTime(List<HostTopoDTO> hostTopoList) {
        if (CollectionUtils.isEmpty(hostTopoList)) {
            return 0;
        }
        int batchSize = 1000;
        List<Query> queryList = new ArrayList<>();
        int affectedNum = 0;
        List<List<HostTopoDTO>> subListList = CollectionUtil.partitionList(hostTopoList, batchSize);
        for (List<HostTopoDTO> subList : subListList) {
            for (HostTopoDTO hostTopo : subList) {
                List<Condition> conditions = buildHostTopoMainFieldCondition(hostTopo);
                conditions.add(defaultTable.LAST_TIME.lessThan(hostTopo.getLastTime()));
                UpdateConditionStep<HostTopoRecord> step = defaultContext.update(defaultTable)
                    .set(defaultTable.LAST_TIME, hostTopo.getLastTime())
                    .where(conditions);
                queryList.add(step);
            }
            int[] results = defaultContext.batch(queryList).execute();
            queryList.clear();
            for (int result : results) {
                affectedNum += result;
            }
        }
        return affectedNum;
    }

    @Override
    public int updateBeforeLastTime(HostTopoDTO hostTopo) {
        List<Condition> conditions = buildHostTopoMainFieldCondition(hostTopo);
        conditions.add(defaultTable.LAST_TIME.lessThan(hostTopo.getLastTime()));
        return defaultContext.update(defaultTable)
            .set(defaultTable.LAST_TIME, hostTopo.getLastTime())
            .where(conditions)
            .execute();
    }

    @Override
    public int batchDeleteHostTopo(List<Long> hostIdList) {
        return batchDeleteHostTopo(null, hostIdList);
    }

    @Override
    public int batchDeleteWithLastTime(List<HostTopoDTO> hostTopoList) {
        if (CollectionUtils.isEmpty(hostTopoList)) {
            return 0;
        }
        int batchSize = 1000;
        List<Query> queryList = new ArrayList<>();
        int affectedNum = 0;
        List<List<HostTopoDTO>> subListList = CollectionUtil.partitionList(hostTopoList, batchSize);
        for (List<HostTopoDTO> subList : subListList) {
            for (HostTopoDTO hostTopo : subList) {
                List<Condition> conditions = buildHostTopoMainFieldCondition(hostTopo);
                conditions.add(defaultTable.LAST_TIME.eq(hostTopo.getLastTime()));
                DeleteConditionStep<HostTopoRecord> step = defaultContext.deleteFrom(defaultTable)
                    .where(conditions);
                queryList.add(step);
            }
            int[] results = defaultContext.batch(queryList).execute();
            queryList.clear();
            for (int result : results) {
                affectedNum += result;
            }
        }
        return affectedNum;
    }

    private List<HostTopoDTO> listHostTopoByConditions(Collection<Condition> conditions) {
        return listHostTopoByConditions(conditions, null, null);
    }

    private List<HostTopoDTO> listHostTopoByConditions(Collection<Condition> conditions, Long start, Long limit) {
        val query = defaultContext.selectFrom(defaultTable)
            .where(conditions);
        val sql = query.getSQL(ParamType.INLINED);
        Result<HostTopoRecord> records;
        try {
            if (start == null || start < 0) {
                start = 0L;
            }
            if (limit != null && limit > 0) {
                records = query.limit(start, limit).fetch();
            } else {
                records = query.fetch();
            }
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
        if (records.isEmpty()) {
            return Collections.emptyList();
        } else {
            return records.map(this::convertRecordToDto);
        }
    }

    @SuppressWarnings("all")
    private int countHostTopoByConditions(Collection<Condition> conditions) {
        return defaultContext.selectCount()
            .from(defaultTable)
            .where(conditions)
            .fetchOne(0, Integer.class);
    }

    @Override
    public List<HostTopoDTO> listHostTopoByHostId(Long hostId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.HOST_ID.eq(ULong.valueOf(hostId)));
        return listHostTopoByConditions(conditions);
    }

    @Override
    public List<HostTopoDTO> listHostTopoByHostIds(Collection<Long> hostIds) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.HOST_ID.in(
            hostIds.stream()
                .map(JooqDataTypeUtil::buildULong)
                .collect(Collectors.toList())
        ));
        return listHostTopoByConditions(conditions);
    }

    @Override
    public List<HostTopoDTO> listHostTopoByModuleIds(Collection<Long> moduleIds) {
        return listHostTopoByModuleIds(moduleIds, null, null);
    }

    @Override
    public List<HostTopoDTO> listHostTopoByModuleIds(Collection<Long> moduleIds, Long start,
                                                     Long limit) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.MODULE_ID.in(moduleIds));
        return listHostTopoByConditions(conditions, start, limit);
    }

    @Override
    public List<HostTopoDTO> listHostTopoByExcludeHostIds(Collection<Long> excludeHostIds) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.HOST_ID.notIn(excludeHostIds));
        return listHostTopoByConditions(conditions, null, null);
    }

    private List<Long> listHostIdByConditions(Collection<Condition> conditions) {
        val query = defaultContext.select(
            defaultTable.HOST_ID
        ).from(defaultTable).where(conditions);
        return query.fetch().map(record -> record.get(defaultTable.HOST_ID, Long.class));
    }

    @Override
    public List<Long> listHostIdByBizAndHostIds(Collection<Long> bizIds, Collection<Long> hostIds) {
        List<Condition> conditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(bizIds)) {
            conditions.add(defaultTable.APP_ID.in(bizIds.stream()
                .filter(Objects::nonNull)
                .map(ULong::valueOf)
                .collect(Collectors.toList()))
            );
        }
        if (CollectionUtils.isNotEmpty(hostIds)) {
            conditions.add(defaultTable.HOST_ID.in(hostIds.stream()
                .filter(Objects::nonNull)
                .map(ULong::valueOf)
                .collect(Collectors.toList()))
            );
        }
        return listHostIdByConditions(conditions);
    }

    @Override
    public List<Long> listModuleIdByHostId(Long hostId) {
        val query = defaultContext.select(
            defaultTable.MODULE_ID
        ).from(defaultTable)
            .where(defaultTable.HOST_ID.eq(JooqDataTypeUtil.buildULong(hostId)));
        return query.fetch().map(record -> record.get(defaultTable.MODULE_ID, Long.class));
    }

    @Override
    public List<Pair<Long, Long>> listHostIdAndModuleIdByBizId(Long bizId) {
        List<Condition> conditions = new ArrayList<>();
        if (bizId != null) {
            conditions.add(defaultTable.APP_ID.eq(JooqDataTypeUtil.buildULong(bizId)));
        }
        val query = defaultContext.select(
            defaultTable.HOST_ID,
            defaultTable.MODULE_ID
        ).from(defaultTable)
            .where(conditions);
        return query.fetch().map(record -> Pair.of(
            record.get(defaultTable.HOST_ID, Long.class),
            record.get(defaultTable.MODULE_ID, Long.class)
        ));
    }

    private HostTopoDTO convertRecordToDto(HostTopoRecord record) {
        return new HostTopoDTO(
            record.getHostId().longValue(),
            record.getAppId().longValue(),
            record.getSetId(),
            record.getModuleId(),
            record.getLastTime()
        );
    }
}
