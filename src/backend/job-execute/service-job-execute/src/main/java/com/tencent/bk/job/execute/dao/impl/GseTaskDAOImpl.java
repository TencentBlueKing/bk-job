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
import com.tencent.bk.job.execute.dao.GseTaskDAO;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.TableField;
import org.jooq.generated.tables.GseTaskLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class GseTaskDAOImpl implements GseTaskDAO {
    private final DSLContext dslContext;

    private static final GseTaskLog TABLE = GseTaskLog.GSE_TASK_LOG;
    private static final TableField<?, ?>[] ALL_FIELDS = {TABLE.ID, TABLE.STEP_INSTANCE_ID, TABLE.EXECUTE_COUNT,
        TABLE.BATCH, TABLE.START_TIME, TABLE.END_TIME, TABLE.TOTAL_TIME, TABLE.STATUS, TABLE.GSE_TASK_ID};

    @Autowired
    public GseTaskDAOImpl(@Qualifier("job-execute-dsl-context") DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    private GseTaskDTO extractInfo(Record record) {
        if (record == null) {
            return null;
        }
        GseTaskDTO gseTaskDTO = new GseTaskDTO();

        gseTaskDTO.setStepInstanceId(record.get(TABLE.STEP_INSTANCE_ID));
        gseTaskDTO.setExecuteCount(record.get(TABLE.EXECUTE_COUNT));
        gseTaskDTO.setBatch(record.get(TABLE.BATCH));
        gseTaskDTO.setStartTime(record.get(TABLE.START_TIME));
        gseTaskDTO.setEndTime(record.get(TABLE.END_TIME));
        gseTaskDTO.setTotalTime(record.get(TABLE.TOTAL_TIME));
        gseTaskDTO.setStatus(record.get(TABLE.STATUS).intValue());
        gseTaskDTO.setGseTaskId(record.get(TABLE.GSE_TASK_ID));
        return gseTaskDTO;
    }

    @Override
    public long saveGseTask(GseTaskDTO gseTask) {
        Result<?> result = dslContext.insertInto(
            TABLE,
            TABLE.STEP_INSTANCE_ID,
            TABLE.EXECUTE_COUNT,
            TABLE.BATCH,
            TABLE.START_TIME,
            TABLE.END_TIME,
            TABLE.TOTAL_TIME,
            TABLE.STATUS,
            TABLE.GSE_TASK_ID)
            .values(
                gseTask.getStepInstanceId(),
                gseTask.getExecuteCount(),
                (short) gseTask.getBatch(),
                gseTask.getStartTime(),
                gseTask.getEndTime(),
                gseTask.getTotalTime(),
                JooqDataTypeUtil.toByte(gseTask.getStatus()),
                gseTask.getGseTaskId())
            .onDuplicateKeyUpdate()
            .set(TABLE.START_TIME, gseTask.getStartTime())
            .set(TABLE.END_TIME, gseTask.getEndTime())
            .set(TABLE.TOTAL_TIME, gseTask.getTotalTime())
            .set(TABLE.STATUS, JooqDataTypeUtil.toByte(gseTask.getStatus()))
            .set(TABLE.GSE_TASK_ID, gseTask.getGseTaskId())
            .returning(TABLE.ID)
            .fetch();
        long id = 0L;
        /*
         * With ON DUPLICATE KEY UPDATE, the affected-rows value per row is 1 if the row is inserted as a new row, 2
         * if an existing row is updated, and 0 if an existing row is set to its current values.
         */
        if (result.size() > 0) {
            id = result.stream().map(record -> record.get(TABLE.ID)).findFirst().get().intValue();
        }
        return id;
    }

    @Override
    public GseTaskDTO getGseTask(long stepInstanceId, int executeCount, int batch) {
        Record record = dslContext.select(ALL_FIELDS).from(TABLE)
            .where(TABLE.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(TABLE.EXECUTE_COUNT.eq(executeCount))
            .and(TABLE.BATCH.eq((short) batch))
            .fetchOne();
        return extractInfo(record);
    }

    @Override
    public GseTaskDTO getGseTask(long gseTaskId) {
        Record record = dslContext.select(ALL_FIELDS).from(TABLE)
            .where(TABLE.ID.eq(gseTaskId))
            .fetchOne();
        return extractInfo(record);
    }
}
