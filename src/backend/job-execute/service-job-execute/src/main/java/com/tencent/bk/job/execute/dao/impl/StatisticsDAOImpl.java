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

import com.tencent.bk.job.common.statistics.model.dto.StatisticsDTO;
import com.tencent.bk.job.common.util.Wrapper;
import com.tencent.bk.job.execute.dao.StatisticsDAO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.apache.commons.lang3.StringUtils;
import org.jooq.*;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.Statistics;
import org.jooq.generated.tables.records.StatisticsRecord;
import org.jooq.impl.DSL;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @Description
 * @Date 2020/3/6
 * @Version 1.0
 */
@Repository
@Slf4j
public class StatisticsDAOImpl implements StatisticsDAO {

    private static final Statistics defaultTable = Statistics.STATISTICS;
    private final DSLContext defaultDSLContext;

    @Autowired
    public StatisticsDAOImpl(DSLContext dslContext) {
        this.defaultDSLContext = dslContext;
    }

    @Override
    public Long insertStatistics(DSLContext dslContext, StatisticsDTO statisticsDTO) {
        if (statisticsDTO == null) {
            return -1L;
        }
        if (statisticsDTO.getCreateTime() == null) {
            statisticsDTO.setCreateTime(System.currentTimeMillis());
        }
        val query = dslContext.insertInto(defaultTable,
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
            statisticsDTO.getAppId(),
            statisticsDTO.getResource(),
            statisticsDTO.getDimension(),
            statisticsDTO.getDimensionValue(),
            statisticsDTO.getDate(),
            statisticsDTO.getValue(),
            ULong.valueOf(statisticsDTO.getCreateTime()),
            ULong.valueOf(System.currentTimeMillis())
        ).returning(defaultTable.ID);
        val sql = query.getSQL(ParamType.INLINED);
        try {
            return query.fetchOne().getId();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    private Collection<Condition> genConditions(Long appId, String resource, String dimension, String dimensionValue,
                                                String date) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.APP_ID.eq(appId));
        conditions.add(defaultTable.RESOURCE.eq(resource));
        conditions.add(defaultTable.DIMENSION.eq(dimension));
        conditions.add(defaultTable.DIMENSION_VALUE.eq(dimensionValue));
        conditions.add(defaultTable.DATE.eq(date));
        return conditions;
    }

    public Boolean exist(DSLContext dslContext, Long appId, String resource, String dimension, String dimensionValue,
                         String date) {
        Collection<Condition> conditions = genConditions(appId, resource, dimension, dimensionValue, date);
        return dslContext.fetchExists(defaultTable, conditions);
    }

    @Override
    public Long upsertStatistics(DSLContext dslContext, StatisticsDTO statisticsDTO) {
        Wrapper<Long> idWrapper = new Wrapper<>(-1L);
        dslContext.transaction(new TransactionalRunnable() {
            @Override
            public void run(Configuration configuration) throws Throwable {
                DSLContext context = DSL.using(configuration);
                StatisticsDTO oldStatisticsDTO = getStatistics(context, statisticsDTO.getAppId(),
                    statisticsDTO.getResource(), statisticsDTO.getDimension(), statisticsDTO.getDimensionValue(),
                    statisticsDTO.getDate());
                if (oldStatisticsDTO == null) {
                    idWrapper.setValue(insertStatistics(context, statisticsDTO));
                } else {
                    oldStatisticsDTO.setValue(statisticsDTO.getValue());
                    updateStatisticsById(context, oldStatisticsDTO);
                    idWrapper.setValue(oldStatisticsDTO.getId());
                }
            }
        });
        return idWrapper.getValue();
    }

    @Override
    public int updateStatisticsById(DSLContext dslContext, StatisticsDTO statisticsDTO) {
        val query = dslContext.update(defaultTable)
            .set(defaultTable.APP_ID, statisticsDTO.getAppId())
            .set(defaultTable.RESOURCE, statisticsDTO.getResource())
            .set(defaultTable.DIMENSION, statisticsDTO.getDimension())
            .set(defaultTable.DIMENSION_VALUE, statisticsDTO.getDimensionValue())
            .set(defaultTable.DATE, statisticsDTO.getDate())
            .set(defaultTable.VALUE, statisticsDTO.getValue())
            .set(defaultTable.CREATE_TIME, ULong.valueOf(statisticsDTO.getCreateTime()))
            .set(defaultTable.LAST_MODIFY_TIME, ULong.valueOf(System.currentTimeMillis()))
            .where(defaultTable.ID.eq(statisticsDTO.getId()));
        val sql = query.getSQL(ParamType.INLINED);
        try {
            return query.execute();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    @Override
    public int deleteStatisticsById(DSLContext dslContext, Long id) {
        return dslContext.deleteFrom(defaultTable).where(
            defaultTable.ID.eq(id)
        ).execute();
    }

    @Override
    public int deleteStatisticsByDate(String date) {
        int totalAffectedRows = 0;
        int affectedRows;
        do {
            affectedRows = defaultDSLContext.deleteFrom(defaultTable).where(
                defaultTable.DATE.lessThan(date)
            ).limit(10000).execute();
            totalAffectedRows += affectedRows;
        } while (affectedRows == 10000);
        return totalAffectedRows;
    }

    @Override
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
            affectedRows = defaultDSLContext.deleteFrom(defaultTable)
                .where(conditions)
                .limit(10000).execute();
            totalAffectedRows += affectedRows;
        } while (affectedRows == 10000);
        return totalAffectedRows;
    }

    @Override
    public StatisticsDTO getStatisticsById(Long id) {
        val record = defaultDSLContext.selectFrom(defaultTable).where(
            defaultTable.ID.eq(id)
        ).fetchOne();
        if (record == null) {
            return null;
        } else {
            return convert(record);
        }
    }

    @Override
    public StatisticsDTO getStatistics(Long appId, String resource, String dimension, String dimensionValue,
                                       String date) {
        val record = defaultDSLContext.selectFrom(defaultTable)
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
    public List<StatisticsDTO> getStatisticsListByAppId(Long appId, String resource, String dimension,
                                                        String dimensionValue, String sinceDate) {
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(defaultTable.APP_ID.eq(appId));
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
        if (StringUtils.isNotBlank(sinceDate)) {
            conditions.add(defaultTable.DATE.lessOrEqual(sinceDate));
        }
        return listStatisticsWithConditions(defaultDSLContext, conditions);
    }

    @Override
    public List<StatisticsDTO> getStatisticsList(Long appId, String resource, String dimension, String date) {
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(defaultTable.APP_ID.eq(appId));
        }
        if (StringUtils.isNotBlank(resource)) {
            conditions.add(defaultTable.RESOURCE.eq(resource));
        }
        if (StringUtils.isNotBlank(dimension)) {
            conditions.add(defaultTable.DIMENSION.eq(dimension));
        }
        if (StringUtils.isNotBlank(date)) {
            conditions.add(defaultTable.DATE.eq(date));
        }
        return listStatisticsWithConditions(defaultDSLContext, conditions);
    }

    @Override
    public List<StatisticsDTO> getStatisticsList(List<Long> inAppIdList, String resource, String dimension,
                                                 String date) {
        List<Condition> conditions = new ArrayList<>();
        if (inAppIdList != null) {
            conditions.add(defaultTable.APP_ID.in(inAppIdList));
        }
        if (StringUtils.isNotBlank(resource)) {
            conditions.add(defaultTable.RESOURCE.eq(resource));
        }
        if (StringUtils.isNotBlank(dimension)) {
            conditions.add(defaultTable.DIMENSION.eq(dimension));
        }
        if (StringUtils.isNotBlank(date)) {
            conditions.add(defaultTable.DATE.eq(date));
        }
        return listStatisticsWithConditions(defaultDSLContext, conditions);
    }

    @Override
    public List<StatisticsDTO> getStatisticsList(List<Long> inAppIdList, List<Long> notInAppIdList, String resource,
                                                 String dimension, String dimensionValue, String date) {
        return listStatisticsWithConditions(defaultDSLContext, genConditions(inAppIdList, notInAppIdList, resource,
            dimension, dimensionValue, date));
    }

    @Override
    public Long getTotalValueOfStatisticsList(List<Long> inAppIdList, List<Long> notInAppIdList, String resource,
                                              String dimension, String dimensionValue, String date) {
        return getTotalValueOfStatisticsWithConditions(defaultDSLContext, genConditions(inAppIdList, notInAppIdList,
            resource, dimension, dimensionValue, date));
    }

    @Override
    public Integer countStatistics(List<Long> inAppIdList, List<Long> notInAppIdList, String resource,
                                   String dimension, String dimensionValue, String date) {
        return countStatisticsByConditions(genConditions(inAppIdList, notInAppIdList, resource, dimension,
            dimensionValue, date));
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

    @Override
    public List<StatisticsDTO> getStatisticsListBetweenDate(List<Long> inAppIdList, List<Long> notInAppIdList,
                                                            String resource, String dimension, String startDate,
                                                            String endDate) {
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
        if (StringUtils.isNotBlank(startDate)) {
            conditions.add(defaultTable.DATE.greaterOrEqual(startDate));
        }
        if (StringUtils.isNotBlank(endDate)) {
            conditions.add(defaultTable.DATE.lessOrEqual(endDate));
        }
        return listStatisticsWithConditions(defaultDSLContext, conditions);
    }

    @Override
    public List<StatisticsDTO> getStatisticsListBetweenDate(List<Long> inAppIdList, List<Long> notInAppIdList,
                                                            String resource, String dimension, String dimensionValue,
                                                            String startDate, String endDate) {
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
        if (StringUtils.isNotBlank(startDate)) {
            conditions.add(defaultTable.DATE.greaterOrEqual(startDate));
        }
        if (StringUtils.isNotBlank(endDate)) {
            conditions.add(defaultTable.DATE.lessOrEqual(endDate));
        }
        return listStatisticsWithConditions(defaultDSLContext, conditions);
    }

    @Override
    public List<StatisticsDTO> getStatisticsListBetweenDate(Long appId, String resource, String dimension,
                                                            String dimensionValue, String startDate, String endDate) {
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(defaultTable.APP_ID.eq(appId));
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
        if (StringUtils.isNotBlank(startDate)) {
            conditions.add(defaultTable.DATE.greaterOrEqual(startDate));
        }
        if (StringUtils.isNotBlank(endDate)) {
            conditions.add(defaultTable.DATE.lessOrEqual(endDate));
        }
        return listStatisticsWithConditions(defaultDSLContext, conditions);
    }

    public StatisticsDTO getStatistics(DSLContext dslContext, Long appId, String resource, String dimension,
                                       String dimensionValue, String date) {
        Collection<Condition> conditions = genConditions(appId, resource, dimension, dimensionValue, date);
        val query = dslContext.selectFrom(defaultTable).where(conditions);
        val sql = query.getSQL(ParamType.INLINED);
        try {
            val record = query.fetchOne();
            if (record == null) {
                return null;
            } else {
                return convert(record);
            }
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    @Override
    public List<StatisticsDTO> listAllStatistics() {
        return listStatisticsWithConditions(defaultDSLContext, Collections.emptyList());
    }

    @Override
    public Integer countStatisticsByDate(String date) {
        Collection<Condition> conditions = new ArrayList<>();
        if (StringUtils.isNotBlank(date)) {
            conditions.add(defaultTable.DATE.eq(date));
        }
        var query = defaultDSLContext.selectCount().from(defaultTable).where(
            conditions
        );
        return query.fetchOne().get(0, Integer.class);
    }

    public Integer countStatisticsByConditions(Collection<Condition> conditions) {
        var query = defaultDSLContext.selectCount().from(defaultTable).where(
            conditions
        );
        return query.fetchOne().get(0, Integer.class);
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

    private Long getTotalValueOfStatisticsWithConditions(DSLContext dslContext, Collection<Condition> conditions) {
        var query = dslContext.select(DSL.sum(defaultTable.VALUE.cast(Long.class))).from(defaultTable).where(
            conditions
        );
        Result<Record1<BigDecimal>> records;
        val sql = query.getSQL(ParamType.INLINED);
        try {
            records = query.fetch();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
        if (records == null || records.isEmpty()) {
            return 0L;
        } else {
            return records.get(0).value1().longValue();
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

}
