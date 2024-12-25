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

package com.tencent.bk.job.execute.dao.impl;

import com.tencent.bk.job.analysis.api.dto.StatisticsDTO;
import com.tencent.bk.job.common.mysql.dynamic.ds.DbOperationEnum;
import com.tencent.bk.job.common.mysql.dynamic.ds.MySQLOperation;
import com.tencent.bk.job.execute.dao.StatisticsDAO;
import com.tencent.bk.job.execute.dao.common.DSLContextProviderFactory;
import com.tencent.bk.job.execute.model.tables.Statistics;
import com.tencent.bk.job.execute.model.tables.records.StatisticsRecord;
import com.tencent.bk.job.execute.statistics.StatisticsKey;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.conf.ParamType;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Repository("jobExecuteStatisticsDAOImpl")
@Slf4j
public class StatisticsDAOImpl extends BaseDAO implements StatisticsDAO {

    private static final Statistics defaultTable = Statistics.STATISTICS;

    @Autowired
    public StatisticsDAOImpl(DSLContextProviderFactory dslContextProviderFactory) {
        super(dslContextProviderFactory, defaultTable.getName());
    }

    @Override
    @MySQLOperation(table = "statistics", op = DbOperationEnum.WRITE)
    public int deleteStatisticsByDate(String date) {
        int totalAffectedRows = 0;
        int affectedRows;
        do {
            affectedRows = dsl().deleteFrom(defaultTable).where(
                defaultTable.DATE.lessThan(date)
            ).limit(10000).execute();
            totalAffectedRows += affectedRows;
        } while (affectedRows == 10000);
        return totalAffectedRows;
    }

    @Override
    @MySQLOperation(table = "statistics", op = DbOperationEnum.WRITE)
    public int deleteOneDayStatistics(Long appId, String date) {
        int totalAffectedRows = 0;
        int affectedRows;
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(defaultTable.APP_ID.eq(appId));
        }
        if (StringUtils.isNotBlank(date)) {
            conditions.add(defaultTable.DATE.eq(date));
        }
        do {
            affectedRows = dsl().deleteFrom(defaultTable)
                .where(conditions)
                .limit(10000).execute();
            totalAffectedRows += affectedRows;
        } while (affectedRows == 10000);
        return totalAffectedRows;
    }


    @Override
    @MySQLOperation(table = "statistics", op = DbOperationEnum.READ)
    public StatisticsDTO getStatistics(Long appId, String resource, String dimension, String dimensionValue,
                                       String date) {
        val record = dsl().selectFrom(defaultTable)
            .where(defaultTable.APP_ID.eq(appId))
            .and(defaultTable.RESOURCE.eq(resource))
            .and(defaultTable.DIMENSION.eq(dimension))
            .and(defaultTable.DIMENSION_VALUE.eq(dimensionValue))
            .and(defaultTable.DATE.eq(date))
            .fetchOne();
        if (record == null) {
            return null;
        } else {
            return convert(record);
        }
    }

    @Override
    @MySQLOperation(table = "statistics", op = DbOperationEnum.READ)
    public List<StatisticsDTO> getStatisticsList(List<Long> inAppIdList, List<Long> notInAppIdList, String resource,
                                                 String dimension, String dimensionValue, String date) {
        return listStatisticsWithConditions(dsl(), genConditions(inAppIdList, notInAppIdList, resource,
            dimension, dimensionValue, date));
    }

    public Collection<Condition> genConditions(List<Long> inAppIdList, List<Long> notInAppIdList, String resource,
                                               String dimension, String dimensionValue, String date) {
        List<Condition> conditions = new ArrayList<>();
        if (inAppIdList != null) {
            conditions.add(defaultTable.APP_ID.in(inAppIdList));
        }
        if (notInAppIdList != null) {
            conditions.add(defaultTable.APP_ID.notIn(notInAppIdList));
        }
        if (StringUtils.isNotBlank(resource)) {
            conditions.add(defaultTable.RESOURCE.eq(resource));
        }
        if (StringUtils.isNotBlank(dimension)) {
            conditions.add(defaultTable.DIMENSION.eq(dimension));
        }
        if (StringUtils.isNotBlank(dimensionValue)) {
            conditions.add(defaultTable.DIMENSION_VALUE.eq(dimensionValue));
        }
        if (StringUtils.isNotBlank(date)) {
            conditions.add(defaultTable.DATE.eq(date));
        }
        return conditions;
    }

    private List<StatisticsDTO> listStatisticsWithConditions(DSLContext dslContext, Collection<Condition> conditions) {
        var query = dslContext.selectFrom(defaultTable).where(
            conditions
        );
        Result<StatisticsRecord> records;
        val sql = query.getSQL(ParamType.INLINED);
        try {
            records = query.fetch();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        } else {
            return records.map(this::convert);
        }
    }

    private StatisticsDTO convert(StatisticsRecord record) {
        return new StatisticsDTO(
            record.getId(),
            record.getAppId(),
            record.getResource(),
            record.getDimension(),
            record.getDimensionValue(),
            record.getDate(),
            record.getValue(),
            record.getCreateTime().longValue(),
            record.getLastModifyTime().longValue()
        );
    }

    @MySQLOperation(table = "statistics", op = DbOperationEnum.WRITE)
    public int increaseStatisticValue(String date, StatisticsKey statisticsKey, Integer incrementValue) {
        Long appId = statisticsKey.getAppId();
        String resource = statisticsKey.getResource();
        String dimension = statisticsKey.getDimension();
        String dimensionValue = statisticsKey.getDimensionValue();
        AtomicInteger affectedRows = new AtomicInteger(0);
        dsl().transaction(configuration -> {
            DSLContext context = DSL.using(configuration);
            List<Condition> conditions = new ArrayList<>();
            conditions.add(defaultTable.APP_ID.eq(appId));
            conditions.add(defaultTable.RESOURCE.eq(resource));
            conditions.add(defaultTable.DIMENSION.eq(dimension));
            conditions.add(defaultTable.DIMENSION_VALUE.eq(dimensionValue));
            conditions.add(defaultTable.DATE.eq(date));
            try {
                Long oldValue = 0L;
                Long id;
                val selectQuery = context.select(defaultTable.ID, defaultTable.VALUE)
                    .from(defaultTable)
                    .where(conditions)
                    .forUpdate();
                log.debug("selectQuery=" + selectQuery.getSQL(ParamType.INLINED));
                Result<Record2<Long, String>> records = selectQuery.fetch();
                if (records.isEmpty()) {
                    log.debug("records is empty");
                    // 记录不存在，先插入
                    val query = context.insertInto(defaultTable,
                        defaultTable.ID,
                        defaultTable.APP_ID,
                        defaultTable.RESOURCE,
                        defaultTable.DIMENSION,
                        defaultTable.DIMENSION_VALUE,
                        defaultTable.DATE,
                        defaultTable.VALUE,
                        defaultTable.CREATE_TIME,
                        defaultTable.LAST_MODIFY_TIME
                    ).values(
                        null,
                        appId,
                        resource,
                        dimension,
                        dimensionValue,
                        date,
                        "0",
                        ULong.valueOf(System.currentTimeMillis()),
                        ULong.valueOf(System.currentTimeMillis())
                    ).returning(defaultTable.ID);
                    id = query.fetchOne().getId();
                } else {
                    if (records.size() > 1) {
                        log.warn("more than 1 records, statisticsKey:{}", statisticsKey);
                    }
                    id = records.get(0).get(defaultTable.ID);
                    oldValue = Long.parseLong(records.get(0).get(defaultTable.VALUE));
                }
                // 更新
                log.debug("Update record {} from {} to {}", id, oldValue, (oldValue + incrementValue));
                affectedRows.set(context.update(defaultTable)
                    .set(defaultTable.VALUE, "" + (oldValue + incrementValue))
                    .where(defaultTable.ID.eq(id))
                    .and(defaultTable.VALUE.eq("" + oldValue))
                    .execute());
                if (affectedRows.get() == 0) {
                    log.debug("Record {} updated by other thread just now, retry", statisticsKey);
                }
            } catch (DataAccessException dataAccessException) {
                if (dataAccessException.getMessage().contains("Duplicate entry")) {
                    //多个实例并发插入导致的主键冲突，忽略，改为更新
                } else {
                    log.warn("dataAccessException when update/create", dataAccessException);
                }
            } catch (Throwable t) {
                log.info("May fail to lock", t);
            }
            log.debug("affectedRows={}", affectedRows.get());
        });
        return affectedRows.get();
    }

}
