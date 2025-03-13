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

import com.tencent.bk.job.backup.archive.dao.resultset.JobInstanceRecordResultSetFactory;
import com.tencent.bk.job.backup.archive.dao.resultset.RecordResultSet;
import com.tencent.bk.job.common.mysql.dynamic.ds.DSLContextProvider;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.Table;

import java.util.Collection;
import java.util.List;

import static org.jooq.impl.DSL.min;

/**
 * 作业实例 - 主表 - 热数据查询 DAO 基础抽象实现
 *
 * @param <T>
 */
public abstract class AbstractJobInstanceMainHotRecordDAO<T extends Record> extends AbstractJobInstanceHotRecordDAO<T> {

    public AbstractJobInstanceMainHotRecordDAO(DSLContextProvider dslContextProvider, Table<T> table) {
        super(dslContextProvider, table);
    }

    /**
     * 获取表中作业实例的最早创建时间
     */
    public Long getMinJobInstanceCreateTime() {
        Record1<Long> record =
            dsl().select(min(getJobInstanceCreateTimeField()))
                .from(getTable())
                .fetchOne();
        if (record != null) {
            Long minJobCreateTime = (Long) record.get(0);
            if (minJobCreateTime != null) {
                return minJobCreateTime;
            }
        }
        return Long.MAX_VALUE;
    }

    /**
     * 是否为空表
     */
    public boolean isTableEmpty() {
        return !dsl().fetchExists(getTable());
    }

    /**
     * 按时间范围+作业实例 ID，顺序读取表记录
     *
     * @param fromTimestamp     开始时间(include)
     * @param endTimestamp      开始时间(exclude)
     * @param fromJobInstanceId 起始作业实例 ID
     * @param limit             读取最大行数
     * @return 记录
     */
    public List<T> readSortedJobInstanceFromHotDB(Long fromTimestamp,
                                                  Long endTimestamp,
                                                  Long fromJobInstanceId,
                                                  int limit) {
        SelectConditionStep<Record> selectConditionStep =
            dsl().select()
                .from(getTable())
                .where(getJobInstanceCreateTimeField().greaterOrEqual(fromTimestamp))
                .and(getJobInstanceCreateTimeField().lessThan(endTimestamp));
        if (fromJobInstanceId != null) {
            selectConditionStep.and(getJobInstanceIdField().greaterThan(fromJobInstanceId));
        }
        Result<Record> result = selectConditionStep.orderBy(
                getJobInstanceIdField().asc()
            )
            .limit(limit)
            .fetch();
        return result.into(getTable());
    }


    @Override
    public RecordResultSet<T> executeQuery(Collection<Long> jobInstanceIds,
                                           long readRowLimit) {
        return JobInstanceRecordResultSetFactory.createOneQueryResultSet(
            this,
            jobInstanceIds,
            readRowLimit
        );
    }
}
