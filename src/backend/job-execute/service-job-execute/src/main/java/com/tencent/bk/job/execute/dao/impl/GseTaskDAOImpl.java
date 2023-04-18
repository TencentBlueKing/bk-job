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
import com.tencent.bk.job.execute.model.GseTaskSimpleDTO;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.TableField;
import org.jooq.generated.tables.GseTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class GseTaskDAOImpl implements GseTaskDAO {
    private final DSLContext dslContext;

    private static final GseTask TABLE = GseTask.GSE_TASK;
    private static final TableField<?, ?>[] ALL_FIELDS = {TABLE.ID, TABLE.STEP_INSTANCE_ID, TABLE.EXECUTE_COUNT,
        TABLE.BATCH, TABLE.START_TIME, TABLE.END_TIME, TABLE.TOTAL_TIME, TABLE.STATUS, TABLE.GSE_TASK_ID};

    private static final TableField<?, ?>[] SIMPLE_FIELDS = {TABLE.STEP_INSTANCE_ID, TABLE.EXECUTE_COUNT,
        TABLE.BATCH, TABLE.GSE_TASK_ID};

    @Autowired
    public GseTaskDAOImpl(@Qualifier("job-execute-dsl-context") DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    private GseTaskDTO extractInfo(Record record) {
        if (record == null) {
            return null;
        }
        GseTaskDTO gseTaskDTO = new GseTaskDTO();

        gseTaskDTO.setId(record.get(TABLE.ID));
        gseTaskDTO.setStepInstanceId(record.get(TABLE.STEP_INSTANCE_ID));
        gseTaskDTO.setExecuteCount(record.get(TABLE.EXECUTE_COUNT).intValue());
        gseTaskDTO.setBatch(record.get(TABLE.BATCH));
        gseTaskDTO.setStartTime(record.get(TABLE.START_TIME));
        gseTaskDTO.setEndTime(record.get(TABLE.END_TIME));
        gseTaskDTO.setTotalTime(record.get(TABLE.TOTAL_TIME));
        gseTaskDTO.setStatus(record.get(TABLE.STATUS).intValue());
        gseTaskDTO.setGseTaskId(record.get(TABLE.GSE_TASK_ID));
        return gseTaskDTO;
    }

    private GseTaskSimpleDTO extractSimpleInfo(Record record) {
        if (record == null) {
            return null;
        }
        GseTaskSimpleDTO gseTaskSimpleDTO = new GseTaskSimpleDTO();
        gseTaskSimpleDTO.setStepInstanceId(record.get(TABLE.STEP_INSTANCE_ID));
        gseTaskSimpleDTO.setExecuteCount(record.get(TABLE.EXECUTE_COUNT).intValue());
        gseTaskSimpleDTO.setBatch(record.get(TABLE.BATCH).intValue());
        gseTaskSimpleDTO.setGseTaskId(record.get(TABLE.GSE_TASK_ID));
        return gseTaskSimpleDTO;
    }

    @Override
    public long saveGseTask(GseTaskDTO gseTask) {
        Record record = dslContext.insertInto(
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
                gseTask.getExecuteCount().shortValue(),
                (short) gseTask.getBatch(),
                gseTask.getStartTime(),
                gseTask.getEndTime(),
                gseTask.getTotalTime(),
                JooqDataTypeUtil.toByte(gseTask.getStatus()),
                gseTask.getGseTaskId())
            .returning(TABLE.ID)
            .fetchOne();

        return record == null ? 0 : record.get(TABLE.ID);
    }

    @Override
    public boolean updateGseTask(GseTaskDTO gseTask) {
        int affectRows = dslContext.update(TABLE)
            .set(TABLE.START_TIME, gseTask.getStartTime())
            .set(TABLE.END_TIME, gseTask.getEndTime())
            .set(TABLE.TOTAL_TIME, gseTask.getTotalTime())
            .set(TABLE.STATUS, gseTask.getStatus().byteValue())
            .set(TABLE.GSE_TASK_ID, gseTask.getGseTaskId())
            .where(TABLE.ID.eq(gseTask.getId()))
            .execute();
        return affectRows > 0;
    }

    @Override
    public GseTaskDTO getGseTask(long stepInstanceId, int executeCount, Integer batch) {
        Record record = dslContext.select(ALL_FIELDS).from(TABLE)
            .where(TABLE.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(TABLE.EXECUTE_COUNT.eq((short) executeCount))
            .and(TABLE.BATCH.eq(batch == null ? 0 : batch.shortValue()))
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

    @Override
    public GseTaskSimpleDTO getGseTaskSimpleInfo(String gseTaskId) {
        Result<Record> records = dslContext.select(SIMPLE_FIELDS).from(TABLE)
            .where(TABLE.GSE_TASK_ID.eq(gseTaskId))
            .limit(1)
            .fetch();
        if (records.isEmpty()) {
            return null;
        }
        return extractSimpleInfo(records.get(0));
    }

    @Override
    public List<GseTaskSimpleDTO> ListGseTaskSimpleInfo(Long stepInstanceId, Integer executeCount, Integer batch) {
        List<Condition> conditions = new ArrayList<>();
        if (stepInstanceId != null) {
            conditions.add(TABLE.STEP_INSTANCE_ID.eq(stepInstanceId));
        }
        if (executeCount != null) {
            conditions.add(TABLE.EXECUTE_COUNT.eq(executeCount.shortValue()));
        }
        if (batch != null) {
            conditions.add(TABLE.BATCH.eq(batch.shortValue()));
        }
        Result<Record> records = dslContext.select(SIMPLE_FIELDS).from(TABLE)
            .where(conditions)
            .fetch();
        List<GseTaskSimpleDTO> results = new ArrayList<>();
        if (records.isNotEmpty()) {
            records.into(record -> {
                results.add(extractSimpleInfo(record));
            });
        }
        return results;
    }
}
