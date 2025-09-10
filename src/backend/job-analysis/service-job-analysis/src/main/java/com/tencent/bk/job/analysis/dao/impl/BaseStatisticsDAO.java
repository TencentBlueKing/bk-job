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
import com.tencent.bk.job.analysis.model.tables.Statistics;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.TableField;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Slf4j
public abstract class BaseStatisticsDAO {

    protected static final Statistics defaultTable = Statistics.STATISTICS;
    protected final DSLContext defaultDSLContext;
    protected static final TableField<?, ?>[] ALL_FIELDS = {
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
    };

    public BaseStatisticsDAO(DSLContext dslContext) {
        this.defaultDSLContext = dslContext;
    }

    abstract protected List<Condition> getBasicConditions();

    protected List<Condition> genConditions(List<Long> inAppIdList,
                                            List<Long> notInAppIdList,
                                            String resource,
                                            String dimension) {
        List<Condition> conditions = getBasicConditions();
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
        return conditions;
    }

    protected List<Condition> genConditions(List<Long> inAppIdList,
                                            List<Long> notInAppIdList,
                                            String resource,
                                            String dimension,
                                            String dimensionValue) {
        List<Condition> conditions = genConditions(inAppIdList, notInAppIdList, resource, dimension);
        if (StringUtils.isNotBlank(dimensionValue)) {
            conditions.add(defaultTable.DIMENSION_VALUE.eq(dimensionValue));
        }
        return conditions;
    }

    protected List<Condition> genConditions(List<Long> inAppIdList,
                                            List<Long> notInAppIdList,
                                            String resource,
                                            String dimension,
                                            String dimensionValue,
                                            String date) {
        List<Condition> conditions = genConditions(inAppIdList, notInAppIdList, resource, dimension, dimensionValue);
        if (StringUtils.isNotBlank(date)) {
            conditions.add(defaultTable.DATE.eq(date));
        }
        return conditions;
    }

    protected boolean existsStatisticsByConditions(Collection<Condition> conditions) {
        return defaultDSLContext.fetchExists(defaultTable, conditions);
    }

    protected List<StatisticsDTO> listStatisticsWithConditions(DSLContext dslContext,
                                                               Collection<Condition> conditions) {
        var query = dslContext
            .select(ALL_FIELDS)
            .from(defaultTable)
            .where(conditions);
        Result<Record> records;
        val sql = query.getSQL(ParamType.INLINED);
        try {
            records = query.fetch();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
        if (records.isEmpty()) {
            return Collections.emptyList();
        } else {
            return records.map(this::convert);
        }
    }

    protected Long getTotalValueOfStatisticsWithConditions(DSLContext dslContext, Collection<Condition> conditions) {
        var query = dslContext
            .select(DSL.sum(defaultTable.VALUE.cast(Long.class)))
            .from(defaultTable)
            .where(conditions);
        Result<Record1<BigDecimal>> records;
        val sql = query.getSQL(ParamType.INLINED);
        try {
            records = query.fetch();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
        if (records.isEmpty()) {
            return 0L;
        } else {
            val record = records.get(0);
            if (record == null) return 0L;
            val value = record.value1();
            if (value == null) return 0L;
            return value.longValue();
        }
    }

    protected StatisticsDTO convert(Record record) {
        return new StatisticsDTO(
            record.get(defaultTable.ID),
            record.get(defaultTable.TENANT_ID),
            record.get(defaultTable.APP_ID),
            record.get(defaultTable.RESOURCE),
            record.get(defaultTable.DIMENSION),
            record.get(defaultTable.DIMENSION_VALUE),
            record.get(defaultTable.DATE),
            record.get(defaultTable.VALUE),
            record.get(defaultTable.CREATE_TIME).longValue(),
            record.get(defaultTable.LAST_MODIFY_TIME).longValue()
        );
    }

}
