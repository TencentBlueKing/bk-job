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
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.dao.StepInstanceRollingTaskDAO;
import com.tencent.bk.job.execute.dao.common.DSLContextProviderFactory;
import com.tencent.bk.job.execute.model.StepInstanceRollingTaskDTO;
import com.tencent.bk.job.execute.model.tables.StepInstanceRollingTask;
import com.tencent.bk.job.execute.model.tables.records.StepInstanceRollingTaskRecord;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.TableField;
import org.jooq.UpdateSetMoreStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@Slf4j
public class StepInstanceRollingTaskDAOImpl extends BaseDAO implements StepInstanceRollingTaskDAO {

    private static final StepInstanceRollingTask TABLE = StepInstanceRollingTask.STEP_INSTANCE_ROLLING_TASK;
    private static final TableField<?, ?>[] ALL_FIELDS = {
        TABLE.ID,
        TABLE.TASK_INSTANCE_ID,
        TABLE.STEP_INSTANCE_ID,
        TABLE.EXECUTE_COUNT,
        TABLE.BATCH,
        TABLE.STATUS,
        TABLE.START_TIME,
        TABLE.END_TIME,
        TABLE.TOTAL_TIME
    };

    @Autowired
    public StepInstanceRollingTaskDAOImpl(DSLContextProviderFactory dslContextProviderFactory) {
        super(dslContextProviderFactory, TABLE.getName());
    }

    @Override
    @MySQLOperation(table = "step_instance_rolling_task", op = DbOperationEnum.READ)
    public StepInstanceRollingTaskDTO queryRollingTask(Long taskInstanceId,
                                                       long stepInstanceId,
                                                       int executeCount,
                                                       int batch) {
        Record record = dsl().select(ALL_FIELDS)
            .from(TABLE)
            .where(TABLE.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(buildTaskInstanceIdQueryCondition(taskInstanceId))
            .and(TABLE.EXECUTE_COUNT.eq(JooqDataTypeUtil.toShort(executeCount)))
            .and(TABLE.BATCH.eq(JooqDataTypeUtil.toShort(batch)))
            .fetchOne();
        return extract(record);
    }

    private Condition buildTaskInstanceIdQueryCondition(Long taskInstanceId) {
        return TaskInstanceIdDynamicCondition.build(
            taskInstanceId,
            TABLE.TASK_INSTANCE_ID::eq
        );
    }

    private StepInstanceRollingTaskDTO extract(Record record) {
        if (record == null) {
            return null;
        }
        StepInstanceRollingTaskDTO stepInstanceRollingTask = new StepInstanceRollingTaskDTO();
        stepInstanceRollingTask.setId(record.get(TABLE.ID));
        stepInstanceRollingTask.setTaskInstanceId(record.get(TABLE.TASK_INSTANCE_ID));
        stepInstanceRollingTask.setStepInstanceId(record.get(TABLE.STEP_INSTANCE_ID));
        stepInstanceRollingTask.setExecuteCount(record.get(TABLE.EXECUTE_COUNT).intValue());
        stepInstanceRollingTask.setBatch(record.get(TABLE.BATCH).intValue());
        stepInstanceRollingTask.setStatus(RunStatusEnum.valueOf(record.get(TABLE.STATUS)));
        stepInstanceRollingTask.setStartTime(record.get(TABLE.START_TIME));
        stepInstanceRollingTask.setEndTime(record.get(TABLE.END_TIME));
        stepInstanceRollingTask.setTotalTime(record.get(TABLE.TOTAL_TIME));
        return stepInstanceRollingTask;
    }

    @Override
    @MySQLOperation(table = "step_instance_rolling_task", op = DbOperationEnum.READ)
    public List<StepInstanceRollingTaskDTO> listRollingTasks(Long taskInstanceId,
                                                             long stepInstanceId,
                                                             Integer executeCount,
                                                             Integer batch) {
        SelectConditionStep<?> selectConditionStep = dsl().select(ALL_FIELDS)
            .from(TABLE)
            .where(TABLE.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(buildTaskInstanceIdQueryCondition(taskInstanceId));
        if (executeCount != null) {
            selectConditionStep.and(TABLE.EXECUTE_COUNT.eq(executeCount.shortValue()));
        }
        if (batch != null && batch > 0) {
            selectConditionStep.and(TABLE.BATCH.eq(batch.shortValue()));
        }

        Result<? extends Record> result = selectConditionStep.orderBy(TABLE.BATCH.asc()).fetch();

        List<StepInstanceRollingTaskDTO> stepInstanceRollingTasks = new ArrayList<>();
        if (!result.isEmpty()) {
            stepInstanceRollingTasks = result.map(this::extract);
        }
        return stepInstanceRollingTasks;
    }

    @Override
    @MySQLOperation(table = "step_instance_rolling_task", op = DbOperationEnum.WRITE)
    public long saveRollingTask(StepInstanceRollingTaskDTO rollingTask) {
        Record record = dsl().insertInto(
                TABLE,
                TABLE.ID,
                TABLE.TASK_INSTANCE_ID,
                TABLE.STEP_INSTANCE_ID,
                TABLE.EXECUTE_COUNT,
                TABLE.BATCH,
                TABLE.STATUS,
                TABLE.START_TIME,
                TABLE.END_TIME,
                TABLE.TOTAL_TIME)
            .values(
                rollingTask.getId(),
                rollingTask.getTaskInstanceId(),
                rollingTask.getStepInstanceId(),
                JooqDataTypeUtil.toShort(rollingTask.getExecuteCount()),
                JooqDataTypeUtil.toShort(rollingTask.getBatch()),
                JooqDataTypeUtil.toByte(rollingTask.getStatus().getValue()),
                rollingTask.getStartTime(),
                rollingTask.getEndTime(),
                rollingTask.getTotalTime())
            .returning(TABLE.ID)
            .fetchOne();
        return rollingTask.getId() != null ? rollingTask.getId() : record.getValue(TABLE.ID);

    }

    @Override
    @MySQLOperation(table = "step_instance_rolling_task", op = DbOperationEnum.WRITE)
    public void updateRollingTask(Long taskInstanceId,
                                  long stepInstanceId,
                                  int executeCount,
                                  int batch,
                                  RunStatusEnum status,
                                  Long startTime,
                                  Long endTime,
                                  Long totalTime) {
        UpdateSetMoreStep<StepInstanceRollingTaskRecord> updateSetMoreStep = null;
        if (status != null) {
            updateSetMoreStep = dsl().update(TABLE).set(TABLE.STATUS, JooqDataTypeUtil.toByte(status.getValue()));
        }
        if (startTime != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = dsl().update(TABLE).set(TABLE.START_TIME, startTime);
            } else {
                updateSetMoreStep.set(TABLE.START_TIME, startTime);
            }
        }
        if (endTime != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = dsl().update(TABLE).set(TABLE.END_TIME, endTime);
            } else {
                updateSetMoreStep.set(TABLE.END_TIME, endTime);
            }
        }
        if (totalTime != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = dsl().update(TABLE).set(TABLE.TOTAL_TIME, totalTime);
            } else {
                updateSetMoreStep.set(TABLE.TOTAL_TIME, totalTime);
            }
        }
        if (updateSetMoreStep == null) {
            log.error("Invalid update rolling task param, do nothing!");
            return;
        }
        updateSetMoreStep.where(TABLE.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(TABLE.EXECUTE_COUNT.eq(JooqDataTypeUtil.toByte(executeCount).shortValue()))
            .and(TABLE.BATCH.eq(JooqDataTypeUtil.toShort(batch)))
            .and(buildTaskInstanceIdQueryCondition(taskInstanceId))
            .execute();

    }
}
