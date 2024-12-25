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

package com.tencent.bk.job.backup.archive.dao.impl;

import com.tencent.bk.job.backup.archive.dao.JobInstanceHotRecordDAO;
import com.tencent.bk.job.common.mysql.dynamic.ds.DSLContextProvider;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.Table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 作业实例热数据查询 DAO 基础抽象实现
 *
 * @param <T> 表记录
 */
public abstract class AbstractJobInstanceHotRecordDAO<T extends Record> implements JobInstanceHotRecordDAO<T> {

    protected final DSLContextProvider dslContextProvider;

    protected final Table<T> table;

    public AbstractJobInstanceHotRecordDAO(DSLContextProvider dslContextProvider, Table<T> table) {
        this.dslContextProvider = dslContextProvider;
        this.table = table;
    }

    @Override
    public List<T> listRecords(Collection<Long> jobInstanceIds, long readRowLimit) {
        return query(table, buildBasicConditions(jobInstanceIds), readRowLimit);
    }

    public List<T> listRecordsByConditions(List<Condition> conditions, long readRowLimit) {
        return query(table, conditions, readRowLimit);
    }

    @Override
    public int deleteRecords(Collection<Long> jobInstanceIds, long maxLimitedDeleteRows) {
        return deleteWithLimit(table, buildBasicConditions(jobInstanceIds), maxLimitedDeleteRows);
    }

    public List<Condition> buildBasicConditions(Collection<Long> jobInstanceIds) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(getJobInstanceIdField().in(jobInstanceIds));
        return conditions;
    }

    private int deleteWithLimit(Table<? extends Record> table, List<Condition> conditions, long maxLimitedDeleteRows) {
        int totalDeleteRows = 0;
        while (true) {
            int deletedRows = dsl()
                .delete(table)
                .where(conditions)
                .limit(maxLimitedDeleteRows)
                .execute();
            totalDeleteRows += deletedRows;
            if (deletedRows < maxLimitedDeleteRows) {
                break;
            }
        }
        return totalDeleteRows;
    }

    protected List<T> query(Table<?> table,
                            List<Condition> conditions,
                            long readRowLimit) {
        SelectConditionStep<Record> selectConditionStep = dsl()
            .select()
            .from(table)
            .where(conditions);

        Result<Record> result;
        if (CollectionUtils.isNotEmpty(getListRecordsOrderFields())) {
            result = selectConditionStep.orderBy(getListRecordsOrderFields()).limit(0, readRowLimit).fetch();
        } else {
            result = selectConditionStep.limit(0, readRowLimit).fetch();
        }
        return result.into(getTable());

    }

    public Table<T> getTable() {
        return this.table;
    }

    protected abstract Collection<? extends OrderField<?>> getListRecordsOrderFields();

    protected DSLContext dsl() {
        return this.dslContextProvider.get(table.getName());
    }
}
