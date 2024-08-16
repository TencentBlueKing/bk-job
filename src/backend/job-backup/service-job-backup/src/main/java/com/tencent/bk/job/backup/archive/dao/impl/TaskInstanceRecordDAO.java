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

import com.tencent.bk.job.execute.model.tables.TaskInstance;
import com.tencent.bk.job.execute.model.tables.records.TaskInstanceRecord;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.Table;
import org.jooq.TableField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jooq.impl.DSL.min;

/**
 * task_instance DAO
 */
public class TaskInstanceRecordDAO extends AbstractJobInstanceHotRecordDAO<TaskInstanceRecord> {

    private static final TaskInstance TABLE = TaskInstance.TASK_INSTANCE;

    private static final List<OrderField<?>> ORDER_FIELDS = new ArrayList<>();

    static {
        ORDER_FIELDS.add(TaskInstance.TASK_INSTANCE.CREATE_TIME.asc());
        ORDER_FIELDS.add(TaskInstance.TASK_INSTANCE.ID.asc());
    }

    public TaskInstanceRecordDAO(DSLContext context) {
        super(context);
    }

    @Override
    public Table<TaskInstanceRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<TaskInstanceRecord, Long> getJobInstanceIdField() {
        return TABLE.ID;
    }

    @Override
    protected Collection<? extends OrderField<?>> getListRecordsOrderFields() {
        return ORDER_FIELDS;
    }

    public Long getMinJobCreateTime() {
        Record1<Long> record =
            context.select(min(TABLE.CREATE_TIME))
                .from(TABLE)
                .fetchOne();
        if (record != null) {
            Long minJobCreateTime = (Long) record.get(0);
            if (minJobCreateTime != null) {
                return minJobCreateTime;
            }
        }
        return null;
    }

    public List<TaskInstanceRecord> readSortedJobInstanceFromHotDB(Long fromTimestamp,
                                                                   Long endTimestamp,
                                                                   Long fromJobInstanceId,
                                                                   int limit) {
        SelectConditionStep<Record> selectConditionStep =
            context.select()
                .from(TABLE)
                .where(TABLE.CREATE_TIME.greaterOrEqual(fromTimestamp))
                .and(TABLE.CREATE_TIME.lessThan(endTimestamp));
        if (fromJobInstanceId != null) {
            selectConditionStep.and(TABLE.ID.greaterThan(fromJobInstanceId));
        }
        Result<Record> result = selectConditionStep.orderBy(TABLE.CREATE_TIME.asc(), TABLE.ID.asc())
            .limit(limit)
            .fetch();
        return result.into(TABLE);
    }


}
