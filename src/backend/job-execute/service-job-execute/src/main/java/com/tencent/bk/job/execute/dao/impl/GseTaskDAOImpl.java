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

import com.tencent.bk.job.common.mysql.dynamic.ds.DbOperationEnum;
import com.tencent.bk.job.common.mysql.dynamic.ds.MySQLOperation;
import com.tencent.bk.job.common.mysql.jooq.JooqDataTypeUtil;
import com.tencent.bk.job.execute.dao.GseTaskDAO;
import com.tencent.bk.job.execute.dao.common.DSLContextProviderFactory;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.GseTaskSimpleDTO;
import com.tencent.bk.job.execute.model.tables.GseTask;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.TableField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class GseTaskDAOImpl extends BaseDAO implements GseTaskDAO {

    private static final GseTask TABLE = GseTask.GSE_TASK;
    private static final TableField<?, ?>[] ALL_FIELDS = {
        TABLE.ID,
        TABLE.STEP_INSTANCE_ID,
        TABLE.EXECUTE_COUNT,
        TABLE.BATCH,
        TABLE.START_TIME,
        TABLE.END_TIME,
        TABLE.TOTAL_TIME,
        TABLE.STATUS,
        TABLE.GSE_TASK_ID,
        TABLE.TASK_INSTANCE_ID
    };

    private static final TableField<?, ?>[] SIMPLE_FIELDS = {
        TABLE.STEP_INSTANCE_ID,
        TABLE.EXECUTE_COUNT,
        TABLE.BATCH,
        TABLE.GSE_TASK_ID,
        TABLE.TASK_INSTANCE_ID
    };

    @Autowired
    public GseTaskDAOImpl(DSLContextProviderFactory dslContextProviderFactory) {
        super(dslContextProviderFactory, TABLE.getName());
    }

    private GseTaskDTO extractInfo(Record record) {
        if (record == null) {
            return null;
        }
        GseTaskDTO gseTaskDTO = new GseTaskDTO();

        gseTaskDTO.setId(record.get(TABLE.ID));
        gseTaskDTO.setTaskInstanceId(record.get(TABLE.TASK_INSTANCE_ID));
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
        gseTaskSimpleDTO.setTaskInstanceId(record.get(TABLE.TASK_INSTANCE_ID));
        gseTaskSimpleDTO.setStepInstanceId(record.get(TABLE.STEP_INSTANCE_ID));
        gseTaskSimpleDTO.setExecuteCount(record.get(TABLE.EXECUTE_COUNT).intValue());
        gseTaskSimpleDTO.setBatch(record.get(TABLE.BATCH).intValue());
        gseTaskSimpleDTO.setGseTaskId(record.get(TABLE.GSE_TASK_ID));
        return gseTaskSimpleDTO;
    }

    @Override
    @MySQLOperation(table = "gse_task", op = DbOperationEnum.WRITE)
    public long saveGseTask(GseTaskDTO gseTask) {
        Record record = dsl().insertInto(
                TABLE,
                TABLE.ID,
                TABLE.STEP_INSTANCE_ID,
                TABLE.EXECUTE_COUNT,
                TABLE.BATCH,
                TABLE.START_TIME,
                TABLE.END_TIME,
                TABLE.TOTAL_TIME,
                TABLE.STATUS,
                TABLE.GSE_TASK_ID,
                TABLE.TASK_INSTANCE_ID)
            .values(
                gseTask.getId(),
                gseTask.getStepInstanceId(),
                gseTask.getExecuteCount().shortValue(),
                (short) gseTask.getBatch(),
                gseTask.getStartTime(),
                gseTask.getEndTime(),
                gseTask.getTotalTime(),
                JooqDataTypeUtil.toByte(gseTask.getStatus()),
                gseTask.getGseTaskId(),
                gseTask.getTaskInstanceId())
            .returning(TABLE.ID)
            .fetchOne();
        return gseTask.getId() != null ? gseTask.getId() : record.getValue(TABLE.ID);

    }

    @Override
    @MySQLOperation(table = "gse_task", op = DbOperationEnum.WRITE)
    public boolean updateGseTask(GseTaskDTO gseTask) {
        int affectRows = dsl().update(TABLE)
            .set(TABLE.START_TIME, gseTask.getStartTime())
            .set(TABLE.END_TIME, gseTask.getEndTime())
            .set(TABLE.TOTAL_TIME, gseTask.getTotalTime())
            .set(TABLE.STATUS, gseTask.getStatus().byteValue())
            .set(TABLE.GSE_TASK_ID, gseTask.getGseTaskId())
            .where(TABLE.ID.eq(gseTask.getId()))
            .and(TaskInstanceIdDynamicCondition.build(gseTask.getTaskInstanceId(), TABLE.TASK_INSTANCE_ID::eq))
            .execute();
        return affectRows > 0;
    }

    @Override
    @MySQLOperation(table = "gse_task", op = DbOperationEnum.READ)
    public GseTaskDTO getGseTask(Long taskInstanceId, long stepInstanceId, int executeCount, Integer batch) {
        SelectConditionStep<?> selectConditionStep =
            dsl().select(ALL_FIELDS).from(TABLE)
                .where(TABLE.STEP_INSTANCE_ID.eq(stepInstanceId))
                .and(TABLE.EXECUTE_COUNT.eq((short) executeCount))
                .and(TaskInstanceIdDynamicCondition.build(taskInstanceId, TABLE.TASK_INSTANCE_ID::eq));
        if (batch != null && batch > 0) {
            // 滚动执行批次，传入null或者0将忽略该参数
            selectConditionStep.and(TABLE.BATCH.eq(batch.shortValue()));
        }
        selectConditionStep.limit(1);
        Record record = selectConditionStep.fetchOne();
        return extractInfo(record);
    }

    @Override
    @MySQLOperation(table = "gse_task", op = DbOperationEnum.READ)
    public GseTaskDTO getGseTask(Long taskInstanceId, long gseTaskId) {
        Record record = dsl().select(ALL_FIELDS).from(TABLE)
            .where(TABLE.ID.eq(gseTaskId))
            .and(TaskInstanceIdDynamicCondition.build(taskInstanceId, TABLE.TASK_INSTANCE_ID::eq))
            .fetchOne();
        return extractInfo(record);
    }

    @Override
    @MySQLOperation(table = "gse_task", op = DbOperationEnum.READ)
    public GseTaskSimpleDTO getGseTaskSimpleInfo(String gseTaskId) {
        Result<Record> records = dsl().select(SIMPLE_FIELDS).from(TABLE)
            .where(TABLE.GSE_TASK_ID.eq(gseTaskId))
            .limit(1)
            .fetch();
        if (records.isEmpty()) {
            return null;
        }
        return extractSimpleInfo(records.get(0));
    }

    @Override
    @MySQLOperation(table = "gse_task", op = DbOperationEnum.READ)
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
        Result<Record> records = dsl().select(SIMPLE_FIELDS).from(TABLE)
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
