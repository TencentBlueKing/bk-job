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

import com.tencent.bk.job.execute.common.util.JooqDataTypeUtil;
import com.tencent.bk.job.execute.dao.StepInstanceRollingTaskDAO;
import com.tencent.bk.job.execute.model.StepInstanceRollingTaskDTO;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.generated.tables.StepInstanceRollingTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class StepInstanceRollingTaskDAOImpl implements StepInstanceRollingTaskDAO {

    private static final StepInstanceRollingTask TABLE = StepInstanceRollingTask.STEP_INSTANCE_ROLLING_TASK;
    private final DSLContext CTX;

    @Autowired
    public StepInstanceRollingTaskDAOImpl(@Qualifier("job-execute-dsl-context") DSLContext CTX) {
        this.CTX = CTX;
    }

    @Override
    public StepInstanceRollingTaskDTO queryRollingTask(long stepInstanceId, int executeCount, int batch) {
        Record record = CTX.select(
            TABLE.ID,
            TABLE.STEP_INSTANCE_ID,
            TABLE.EXECUTE_COUNT,
            TABLE.BATCH,
            TABLE.STATUS,
            TABLE.START_TIME,
            TABLE.END_TIME,
            TABLE.TOTAL_TIME)
            .from(TABLE)
            .where(TABLE.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(TABLE.EXECUTE_COUNT.eq(JooqDataTypeUtil.toByte(executeCount)))
            .and(TABLE.BATCH.eq(JooqDataTypeUtil.toShort(batch)))
            .fetchOne();
        return extract(record);
    }

    private StepInstanceRollingTaskDTO extract(Record record) {
        if (record == null) {
            return null;
        }
        StepInstanceRollingTaskDTO stepInstanceRollingTask = new StepInstanceRollingTaskDTO();
        stepInstanceRollingTask.setId(record.get(TABLE.ID));
        stepInstanceRollingTask.setStepInstanceId(record.get(TABLE.STEP_INSTANCE_ID));
        stepInstanceRollingTask.setExecuteCount(record.get(TABLE.EXECUTE_COUNT).intValue());
        stepInstanceRollingTask.setBatch(record.get(TABLE.BATCH).intValue());
        stepInstanceRollingTask.setStatus(record.get(TABLE.STATUS).intValue());
        stepInstanceRollingTask.setStartTime(record.get(TABLE.START_TIME));
        stepInstanceRollingTask.setEndTime(record.get(TABLE.END_TIME));
        stepInstanceRollingTask.setTotalTime(record.get(TABLE.TOTAL_TIME));
        return stepInstanceRollingTask;
    }

    @Override
    public long saveRollingTask(StepInstanceRollingTaskDTO rollingTask) {
        Record record = CTX.insertInto(
            TABLE,
            TABLE.STEP_INSTANCE_ID,
            TABLE.EXECUTE_COUNT,
            TABLE.BATCH,
            TABLE.STATUS,
            TABLE.START_TIME,
            TABLE.END_TIME,
            TABLE.TOTAL_TIME)
            .values(
                rollingTask.getStepInstanceId(),
                JooqDataTypeUtil.toByte(rollingTask.getExecuteCount()),
                JooqDataTypeUtil.toShort(rollingTask.getBatch()),
                JooqDataTypeUtil.toByte(rollingTask.getStatus()),
                rollingTask.getStartTime(),
                rollingTask.getEndTime(),
                rollingTask.getTotalTime())
            .returning(TABLE.ID)
            .fetchOne();
        assert record != null;
        return record.get(TABLE.ID);
    }
}
