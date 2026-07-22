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

package com.tencent.bk.job.execute.dao.impl;

import com.tencent.bk.job.common.mysql.dynamic.ds.DbOperationEnum;
import com.tencent.bk.job.common.mysql.dynamic.ds.MySQLOperation;
import com.tencent.bk.job.common.mysql.jooq.JooqDataTypeUtil;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.dao.StepInstanceRollingTaskDAO;
import com.tencent.bk.job.execute.dao.common.DSLContextProviderFactory;
import com.tencent.bk.job.execute.engine.rolling.scatter.ScatterBatchFinishResult;
import com.tencent.bk.job.execute.model.StepInstanceRollingTaskDTO;
import com.tencent.bk.job.execute.model.tables.StepInstanceRollingTask;
import com.tencent.bk.job.execute.model.tables.records.StepInstanceRollingTaskRecord;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.TableField;
import org.jooq.UpdateSetMoreStep;
import org.jooq.impl.DSL;
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
        TABLE.TOTAL_TIME,
        TABLE.DISPATCH_TIME,
        TABLE.DISPATCHED
    };

    /**
     * 批次终态状态集合（用于并行错峰模式完成判定）
     */
    private static final Byte[] FINAL_STATUS_VALUES = new Byte[]{
        RunStatusEnum.SUCCESS.getValue().byteValue(),
        RunStatusEnum.FAIL.getValue().byteValue(),
        RunStatusEnum.IGNORE_ERROR.getValue().byteValue(),
        RunStatusEnum.STOP_SUCCESS.getValue().byteValue(),
        RunStatusEnum.ABNORMAL_STATE.getValue().byteValue(),
        RunStatusEnum.ABANDONED.getValue().byteValue(),
        RunStatusEnum.SKIPPED.getValue().byteValue()
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
        stepInstanceRollingTask.setDispatchTime(record.get(TABLE.DISPATCH_TIME));
        Byte dispatched = record.get(TABLE.DISPATCHED);
        stepInstanceRollingTask.setDispatched(dispatched != null && dispatched != 0);
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
                TABLE.TOTAL_TIME,
                TABLE.DISPATCH_TIME,
                TABLE.DISPATCHED)
            .values(
                rollingTask.getId(),
                rollingTask.getTaskInstanceId(),
                rollingTask.getStepInstanceId(),
                JooqDataTypeUtil.toShort(rollingTask.getExecuteCount()),
                JooqDataTypeUtil.toShort(rollingTask.getBatch()),
                JooqDataTypeUtil.toByte(rollingTask.getStatus().getValue()),
                rollingTask.getStartTime(),
                rollingTask.getEndTime(),
                rollingTask.getTotalTime(),
                rollingTask.getDispatchTime(),
                (byte) (Boolean.TRUE.equals(rollingTask.getDispatched()) ? 1 : 0))
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

    @Override
    @MySQLOperation(table = "step_instance_rolling_task", op = DbOperationEnum.WRITE)
    public void updateDispatchInfo(Long taskInstanceId,
                                   long stepInstanceId,
                                   int executeCount,
                                   int batch,
                                   Long dispatchTime,
                                   Boolean dispatched) {
        UpdateSetMoreStep<StepInstanceRollingTaskRecord> updateSetMoreStep = null;
        if (dispatchTime != null) {
            updateSetMoreStep = dsl().update(TABLE).set(TABLE.DISPATCH_TIME, dispatchTime);
        }
        if (dispatched != null) {
            byte dispatchedValue = (byte) (dispatched ? 1 : 0);
            if (updateSetMoreStep == null) {
                updateSetMoreStep = dsl().update(TABLE).set(TABLE.DISPATCHED, dispatchedValue);
            } else {
                updateSetMoreStep.set(TABLE.DISPATCHED, dispatchedValue);
            }
        }
        if (updateSetMoreStep == null) {
            log.error("Invalid update dispatch info param, do nothing!");
            return;
        }
        updateSetMoreStep.where(TABLE.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(TABLE.EXECUTE_COUNT.eq(JooqDataTypeUtil.toShort(executeCount)))
            .and(TABLE.BATCH.eq(JooqDataTypeUtil.toShort(batch)))
            .and(buildTaskInstanceIdQueryCondition(taskInstanceId))
            .execute();
    }

    @Override
    @MySQLOperation(table = "step_instance_rolling_task", op = DbOperationEnum.WRITE)
    public ScatterBatchFinishResult finishBatchAndCheckAllDone(Long taskInstanceId,
                                                               long stepInstanceId,
                                                               int executeCount,
                                                               int batch,
                                                               RunStatusEnum status,
                                                               Long startTime,
                                                               Long endTime,
                                                               Long totalTime,
                                                               int totalBatch) {
        // 同一事务内完成：锚点行 FOR UPDATE 串行 + 幂等闸门 + COUNT
        return dsl().transactionResult(configuration -> {
            DSLContext txDsl = DSL.using(configuration);
            // ① 抢 batch=1 锚点行锁，串行化本步骤的完成判定，顺序一致避免死锁
            Record1<Long> anchor = txDsl.select(TABLE.ID)
                .from(TABLE)
                .where(TABLE.STEP_INSTANCE_ID.eq(stepInstanceId))
                .and(buildTaskInstanceIdQueryCondition(taskInstanceId))
                .and(TABLE.EXECUTE_COUNT.eq(JooqDataTypeUtil.toShort(executeCount)))
                .and(TABLE.BATCH.eq((short) 1))
                .forUpdate()
                .fetchOne();
            if (anchor == null) {
                log.error("Anchor rolling task(batch=1) not found, stepInstanceId={}, executeCount={}",
                    stepInstanceId, executeCount);
            }
            // ② 幂等闸门：仅当当前批次未处于终态时才跃迁为终态
            int affected = txDsl.update(TABLE)
                .set(TABLE.STATUS, JooqDataTypeUtil.toByte(status.getValue()))
                .set(TABLE.START_TIME, startTime)
                .set(TABLE.END_TIME, endTime)
                .set(TABLE.TOTAL_TIME, totalTime)
                .where(TABLE.STEP_INSTANCE_ID.eq(stepInstanceId))
                .and(buildTaskInstanceIdQueryCondition(taskInstanceId))
                .and(TABLE.EXECUTE_COUNT.eq(JooqDataTypeUtil.toShort(executeCount)))
                .and(TABLE.BATCH.eq(JooqDataTypeUtil.toShort(batch)))
                .and(TABLE.STATUS.notIn(FINAL_STATUS_VALUES))
                .execute();
            if (affected == 0) {
                return ScatterBatchFinishResult.ALREADY_FINAL;
            }
            // ③ 统计终态批次数，持锚点锁下之前提交的终态更新均可见
            Integer finishedCount = txDsl.selectCount()
                .from(TABLE)
                .where(TABLE.STEP_INSTANCE_ID.eq(stepInstanceId))
                .and(buildTaskInstanceIdQueryCondition(taskInstanceId))
                .and(TABLE.EXECUTE_COUNT.eq(JooqDataTypeUtil.toShort(executeCount)))
                .and(TABLE.STATUS.in(FINAL_STATUS_VALUES))
                .fetchOne(0, Integer.class);
            int finished = finishedCount == null ? 0 : finishedCount;
            return finished >= totalBatch
                ? ScatterBatchFinishResult.LAST_BATCH
                : ScatterBatchFinishResult.NOT_LAST_BATCH;
        });
    }
}
