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
import com.tencent.bk.job.execute.model.tables.StepInstanceScript;
import com.tencent.bk.job.execute.model.tables.records.StepInstanceScriptRecord;
import org.jooq.Condition;
import org.jooq.OrderField;
import org.jooq.TableField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * step_instance_script DAO
 */
public class StepInstanceScriptRecordDAO extends AbstractJobInstanceHotRecordDAO<StepInstanceScriptRecord> {

    private static final StepInstanceScript TABLE = StepInstanceScript.STEP_INSTANCE_SCRIPT;

    private static final List<OrderField<?>> ORDER_FIELDS = new ArrayList<>();

    static {
        ORDER_FIELDS.add(StepInstanceScript.STEP_INSTANCE_SCRIPT.STEP_INSTANCE_ID.asc());
    }


    public StepInstanceScriptRecordDAO(DSLContextProvider dslContextProvider) {
        super(dslContextProvider, TABLE);
    }


    @Override
    public TableField<StepInstanceScriptRecord, Long> getJobInstanceIdField() {
        return TABLE.TASK_INSTANCE_ID;
    }

    protected Collection<? extends OrderField<?>> getListRecordsOrderFields() {
        return ORDER_FIELDS;
    }

    @Override
    public RecordResultSet<StepInstanceScriptRecord> executeQuery(Collection<Long> jobInstanceIds,
                                                                  long readRowLimit) {
        return JobInstanceRecordResultSetFactory.createMultiQueryResultSet(
            this,
            jobInstanceIds,
            readRowLimit,
            lastRecord -> {
                List<Condition> conditions = new ArrayList<>();
                conditions.add(TABLE.STEP_INSTANCE_ID.gt(lastRecord.getStepInstanceId()));
                return conditions;
            }
        );
    }
}
