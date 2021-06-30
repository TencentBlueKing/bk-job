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

package com.tencent.bk.job.execute.statistics;

import com.tencent.bk.job.common.util.ObjectWrapper;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.conf.ParamType;
import org.jooq.exception.DataAccessException;
import org.jooq.generated.tables.Statistics;
import org.jooq.impl.DSL;
import org.jooq.types.ULong;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class StatisticsFlushThread extends Thread {

    private static final Statistics tableStatistics = Statistics.STATISTICS;
    private final DSLContext dslContext;
    private volatile LinkedBlockingQueue<Map<String, Map<StatisticsKey, AtomicInteger>>> flushQueue;

    public StatisticsFlushThread(DSLContext dslContext, LinkedBlockingQueue<Map<String,
        Map<StatisticsKey, AtomicInteger>>> flushQueue) {
        this.dslContext = dslContext;
        this.flushQueue = flushQueue;
    }

    private void flushIncrementMap(Map<String, Map<StatisticsKey, AtomicInteger>> incrementMap) {
        incrementMap.forEach((dateStr, metricsMap) -> {
            metricsMap.forEach((statisticsKey, value) -> {
                final Long appId = statisticsKey.getAppId();
                final String resource = statisticsKey.getResource();
                final String dimension = statisticsKey.getDimension();
                final String dimensionValue = statisticsKey.getDimensionValue();
                final String date = dateStr;
                final Integer incrementValue = value.get();
                final ObjectWrapper<Result<Record2<Long, String>>> recordsWrapper = new ObjectWrapper<>(null);
                AtomicInteger affectedRows = new AtomicInteger(0);
                do {
                    dslContext.transaction(configuration -> {
                        DSLContext context = DSL.using(configuration);
                        List<Condition> conditions = new ArrayList<>();
                        conditions.add(tableStatistics.APP_ID.eq(appId));
                        conditions.add(tableStatistics.RESOURCE.eq(resource));
                        conditions.add(tableStatistics.DIMENSION.eq(dimension));
                        conditions.add(tableStatistics.DIMENSION_VALUE.eq(dimensionValue));
                        conditions.add(tableStatistics.DATE.eq(date));
                        try {
                            Long oldValue = 0L;
                            Long id;
                            val selectQuery = context.select(tableStatistics.ID, tableStatistics.VALUE)
                                .from(tableStatistics)
                                .where(conditions)
                                .forUpdate();
                            log.debug("selectQuery=" + selectQuery.getSQL(ParamType.INLINED));
                            Result<Record2<Long, String>> records = selectQuery.fetch();
                            recordsWrapper.set(records);
                            if (records.isEmpty()) {
                                log.debug("records is empty");
                                // 记录不存在，先插入
                                val query = context.insertInto(tableStatistics,
                                    tableStatistics.ID,
                                    tableStatistics.APP_ID,
                                    tableStatistics.RESOURCE,
                                    tableStatistics.DIMENSION,
                                    tableStatistics.DIMENSION_VALUE,
                                    tableStatistics.DATE,
                                    tableStatistics.VALUE,
                                    tableStatistics.CREATE_TIME,
                                    tableStatistics.LAST_MODIFY_TIME
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
                                ).returning(tableStatistics.ID);
                                id = query.fetchOne().getId();
                            } else {
                                if (records.size() > 1) {
                                    log.warn("more than 1 records, statisticsKey:{}", statisticsKey);
                                }
                                id = records.get(0).get(tableStatistics.ID);
                                oldValue = Long.parseLong(records.get(0).get(tableStatistics.VALUE));
                            }
                            // 更新
                            log.debug("Update record {} from {} to {}", id, oldValue, (oldValue + incrementValue));
                            affectedRows.set(context.update(tableStatistics)
                                .set(tableStatistics.VALUE, "" + (oldValue + incrementValue))
                                .where(tableStatistics.ID.eq(id))
                                .and(tableStatistics.VALUE.eq("" + oldValue))
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
                        log.debug("records={},affectedRows={}", recordsWrapper.get(), affectedRows);
                    });
                } while (recordsWrapper.get() == null || affectedRows.get() == 0);
            });
        });
    }

    @Override
    public void run() {
        while (true) {
            try {
                Map<String, Map<StatisticsKey, AtomicInteger>> incrementMap = flushQueue.take();
                flushIncrementMap(incrementMap);
            } catch (Throwable t) {
                log.error("Fail to flush statistics into DB", t);
            }
        }
    }
}
