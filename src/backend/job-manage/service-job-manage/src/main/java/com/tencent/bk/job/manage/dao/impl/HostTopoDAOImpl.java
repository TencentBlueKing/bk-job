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

import com.tencent.bk.job.manage.common.util.JooqDataTypeUtil;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.model.dto.HostTopoDTO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.BatchBindStep;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.DeleteConditionStep;
import org.jooq.Query;
import org.jooq.Result;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.HostTopo;
import org.jooq.generated.tables.records.HostTopoRecord;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
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
    public HostTopoDAOImpl(DSLContext dslContext) {
        this.defaultContext = dslContext;
    }

    @Override
    public void insertHostTopo(HostTopoDTO hostTopoDTO) {
        val query = defaultContext.insertInto(defaultTable,
            defaultTable.HOST_ID,
            defaultTable.APP_ID,
            defaultTable.SET_ID,
            defaultTable.MODULE_ID
        ).values(
            ULong.valueOf(hostTopoDTO.getHostId()),
            ULong.valueOf(hostTopoDTO.getBizId()),
            hostTopoDTO.getSetId(),
            hostTopoDTO.getModuleId()
        ).onDuplicateKeyIgnore();
        query.execute();
    }

    @Override
    public int batchInsertHostTopo(List<HostTopoDTO> hostTopoDTOList) {
        int batchSize = 1000;
        int size = hostTopoDTOList.size();
        int start = 0;
        int end;
        int affectedNum = 0;
        do {
            end = start + batchSize;
            end = Math.min(end, size);
            List<HostTopoDTO> subList = hostTopoDTOList.subList(start, end);
            if (subList.isEmpty()) {
                // 避免插入空数据
                break;
            }
            val insertQuery = defaultContext.insertInto(defaultTable,
                defaultTable.HOST_ID,
                defaultTable.APP_ID,
                defaultTable.SET_ID,
                defaultTable.MODULE_ID
            ).values(
                (ULong) null,
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
                    hostTopoDTO.getModuleId()
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
    public void deleteHostTopoByHostId(Long appId, Long hostId) {
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(defaultTable.APP_ID.eq(ULong.valueOf(appId)));
        }
        if (hostId != null) {
            conditions.add(defaultTable.HOST_ID.eq(ULong.valueOf(hostId)));
        }
        defaultContext.deleteFrom(defaultTable).where(
            conditions
        ).execute();
    }

    @Override
    public void deleteHostTopo(Long hostId, Long appId, Long setId, Long moduleId) {
        defaultContext.deleteFrom(defaultTable)
            .where(defaultTable.HOST_ID.eq(ULong.valueOf(hostId)))
            .and(defaultTable.APP_ID.eq(ULong.valueOf(appId)))
            .and(defaultTable.SET_ID.eq(setId))
            .and(defaultTable.MODULE_ID.eq(moduleId))
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

    @Override
    public int batchDeleteHostTopo(List<Long> hostIdList) {
        return batchDeleteHostTopo(null, hostIdList);
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
    public int countHostTopo(Long bizId, Long hostId) {
        List<Condition> conditions = new ArrayList<>();
        if (hostId != null) {
            conditions.add(defaultTable.HOST_ID.eq(ULong.valueOf(hostId)));
        }
        if (bizId != null) {
            conditions.add(defaultTable.APP_ID.eq(ULong.valueOf(bizId)));
        }
        return countHostTopoByConditions(conditions);
    }

    @Override
    public List<HostTopoDTO> listHostTopoByHostId(Long hostId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.HOST_ID.eq(ULong.valueOf(hostId)));
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

    private List<Long> listHostIdByConditions(Collection<Condition> conditions) {
        val query = defaultContext.select(
            defaultTable.HOST_ID
        ).from(defaultTable).where(conditions);
        return query.fetch().map(record -> record.get(defaultTable.HOST_ID, Long.class));
    }

    @Override
    public List<Long> listHostIdByBizIds(Collection<Long> bizIds) {
        List<Condition> conditions = new ArrayList<>();
        if (bizIds != null) {
            conditions.add(defaultTable.APP_ID.in(bizIds.stream().map(ULong::valueOf).collect(Collectors.toList())));
        }
        return listHostIdByConditions(conditions);
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

    private HostTopoDTO convertRecordToDto(HostTopoRecord record) {
        return new HostTopoDTO(
            record.getHostId().longValue(),
            record.getAppId().longValue(),
            record.getSetId(),
            record.getModuleId()
        );
    }
}
