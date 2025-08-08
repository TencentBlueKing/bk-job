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

package com.tencent.bk.job.analysis.dao.impl;

import com.tencent.bk.job.analysis.api.dto.StatisticsDTO;
import com.tencent.bk.job.analysis.dao.CurrentTenantStatisticsDAO;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.Wrapper;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Repository
@Slf4j
public class CurrentTenantStatisticsDAOImpl extends BaseStatisticsDAO implements CurrentTenantStatisticsDAO {

    @Autowired
    public CurrentTenantStatisticsDAOImpl(@Qualifier("job-analysis-dsl-context") DSLContext dslContext) {
        super(dslContext);
    }

    @Override
    protected List<Condition> getBasicConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.TENANT_ID.eq(JobContextUtil.getTenantId()));
        return conditions;
    }

    @Override
    public Long upsertStatistics(DSLContext dslContext, StatisticsDTO statisticsDTO) {
        Wrapper<Long> idWrapper = new Wrapper<>(-1L);
        dslContext.transaction(configuration -> {
            DSLContext context = DSL.using(configuration);
            StatisticsDTO oldStatisticsDTO = getStatistics(
                context,
                statisticsDTO.getAppId(),
                statisticsDTO.getResource(),
                statisticsDTO.getDimension(),
                statisticsDTO.getDimensionValue(),
                statisticsDTO.getDate()
            );
            if (oldStatisticsDTO == null) {
                idWrapper.setValue(insertStatistics(context, statisticsDTO));
            } else {
                oldStatisticsDTO.setValue(statisticsDTO.getValue());
                updateStatisticsById(context, oldStatisticsDTO);
                idWrapper.setValue(oldStatisticsDTO.getId());
            }
        });
        return idWrapper.getValue();
    }

    private Long insertStatistics(DSLContext dslContext,
                                  StatisticsDTO statisticsDTO) {
        if (statisticsDTO == null) {
            return -1L;
        }
        if (statisticsDTO.getCreateTime() == null) {
            statisticsDTO.setCreateTime(System.currentTimeMillis());
        }
        val query = dslContext.insertInto(
            defaultTable,
            defaultTable.ID,
            defaultTable.TENANT_ID,
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
            statisticsDTO.getTenantId(),
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
            val record = query.fetchOne();
            assert record != null;
            return record.getId();
        } catch (Exception e) {
            log.error("errorSQL={}", sql);
            throw e;
        }
    }

    private Collection<Condition> genConditions(Long appId,
                                                String resource,
                                                String dimension,
                                                String dimensionValue,
                                                String date) {
        List<Condition> conditions = getBasicConditions();
        conditions.add(defaultTable.APP_ID.eq(appId));
        conditions.add(defaultTable.RESOURCE.eq(resource));
        conditions.add(defaultTable.DIMENSION.eq(dimension));
        conditions.add(defaultTable.DIMENSION_VALUE.eq(dimensionValue));
        conditions.add(defaultTable.DATE.eq(date));
        return conditions;
    }

    private void updateStatisticsById(DSLContext dslContext, StatisticsDTO statisticsDTO) {
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
            query.execute();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    @Override
    public StatisticsDTO getStatistics(Long appId,
                                       String resource,
                                       String dimension,
                                       String dimensionValue,
                                       String date) {
        List<Condition> conditions = buildConditions(appId, resource, dimension, dimensionValue, date);
        val record = defaultDSLContext
            .select(ALL_FIELDS)
            .from(defaultTable)
            .where(conditions)
            .fetchOne();
        if (record == null) {
            return null;
        } else {
            return convert(record);
        }
    }

    private List<Condition> buildConditions(Long appId, String resource, String dimension) {
        List<Condition> conditions = getBasicConditions();
        if (appId != null) {
            conditions.add(defaultTable.APP_ID.eq(appId));
        }
        if (StringUtils.isNotBlank(resource)) {
            conditions.add(defaultTable.RESOURCE.eq(resource));
        }
        if (StringUtils.isNotBlank(dimension)) {
            conditions.add(defaultTable.DIMENSION.eq(dimension));
        }
        return conditions;
    }

    private List<Condition> buildConditions(Long appId, String resource, String dimension, String dimensionValue) {
        List<Condition> conditions = buildConditions(appId, resource, dimension);
        if (StringUtils.isNotBlank(dimensionValue)) {
            conditions.add(defaultTable.DIMENSION_VALUE.eq(dimensionValue));
        }
        return conditions;
    }

    private List<Condition> buildConditions(Long appId,
                                            String resource,
                                            String dimension,
                                            String dimensionValue,
                                            String date) {
        List<Condition> conditions = buildConditions(appId, resource, dimension, dimensionValue);
        if (StringUtils.isNotBlank(date)) {
            conditions.add(defaultTable.DATE.eq(date));
        }
        return conditions;
    }

    @Override
    public List<StatisticsDTO> getStatisticsList(List<Long> inAppIdList,
                                                 String resource,
                                                 String dimension,
                                                 String date) {
        List<Condition> conditions = getBasicConditions();
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
    public List<StatisticsDTO> getStatisticsList(List<Long> inAppIdList,
                                                 List<Long> notInAppIdList,
                                                 String resource,
                                                 String dimension,
                                                 String dimensionValue,
                                                 String date) {
        return listStatisticsWithConditions(
            defaultDSLContext,
            genConditions(
                inAppIdList,
                notInAppIdList,
                resource,
                dimension,
                dimensionValue,
                date
            )
        );
    }

    @Override
    public Long getTotalValueOfStatisticsList(List<Long> inAppIdList,
                                              List<Long> notInAppIdList,
                                              String resource,
                                              String dimension,
                                              String dimensionValue,
                                              String date) {
        return getTotalValueOfStatisticsWithConditions(
            defaultDSLContext,
            genConditions(
                inAppIdList,
                notInAppIdList,
                resource,
                dimension,
                dimensionValue,
                date
            )
        );
    }

    @Override
    public List<StatisticsDTO> getStatisticsListBetweenDate(List<Long> inAppIdList,
                                                            List<Long> notInAppIdList,
                                                            String resource,
                                                            String dimension,
                                                            String startDate,
                                                            String endDate) {
        List<Condition> conditions = genConditions(inAppIdList, notInAppIdList, resource, dimension);
        if (StringUtils.isNotBlank(startDate)) {
            conditions.add(defaultTable.DATE.greaterOrEqual(startDate));
        }
        if (StringUtils.isNotBlank(endDate)) {
            conditions.add(defaultTable.DATE.lessOrEqual(endDate));
        }
        return listStatisticsWithConditions(defaultDSLContext, conditions);
    }

    @Override
    public List<StatisticsDTO> getStatisticsListBetweenDate(List<Long> inAppIdList,
                                                            List<Long> notInAppIdList,
                                                            String resource,
                                                            String dimension,
                                                            String dimensionValue,
                                                            String startDate,
                                                            String endDate) {
        List<Condition> conditions = genConditions(inAppIdList, notInAppIdList, resource, dimension, dimensionValue);
        if (StringUtils.isNotBlank(startDate)) {
            conditions.add(defaultTable.DATE.greaterOrEqual(startDate));
        }
        if (StringUtils.isNotBlank(endDate)) {
            conditions.add(defaultTable.DATE.lessOrEqual(endDate));
        }
        return listStatisticsWithConditions(defaultDSLContext, conditions);
    }

    @Override
    public List<StatisticsDTO> getStatisticsListBetweenDate(Long appId,
                                                            String resource,
                                                            String dimension,
                                                            String dimensionValue,
                                                            String startDate,
                                                            String endDate) {
        List<Condition> conditions = buildConditions(appId, resource, dimension, dimensionValue);
        if (StringUtils.isNotBlank(startDate)) {
            conditions.add(defaultTable.DATE.greaterOrEqual(startDate));
        }
        if (StringUtils.isNotBlank(endDate)) {
            conditions.add(defaultTable.DATE.lessOrEqual(endDate));
        }
        return listStatisticsWithConditions(defaultDSLContext, conditions);
    }

    public StatisticsDTO getStatistics(DSLContext dslContext,
                                       Long appId,
                                       String resource,
                                       String dimension,
                                       String dimensionValue,
                                       String date) {
        Collection<Condition> conditions = genConditions(appId, resource, dimension, dimensionValue, date);
        val query = dslContext
            .select(ALL_FIELDS)
            .from(defaultTable)
            .where(conditions);
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


}
