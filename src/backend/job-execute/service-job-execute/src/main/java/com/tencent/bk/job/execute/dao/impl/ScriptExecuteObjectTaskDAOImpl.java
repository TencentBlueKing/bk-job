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

import com.tencent.bk.job.common.constant.ExecuteObjectTypeEnum;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.common.mysql.dynamic.ds.DbOperationEnum;
import com.tencent.bk.job.common.mysql.dynamic.ds.MySQLOperation;
import com.tencent.bk.job.execute.dao.ScriptExecuteObjectTaskDAO;
import com.tencent.bk.job.execute.dao.common.DSLContextProviderFactory;
import com.tencent.bk.job.execute.engine.consts.ExecuteObjectTaskStatusEnum;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.ResultGroupBaseDTO;
import com.tencent.bk.job.execute.model.tables.GseScriptExecuteObjTask;
import com.tencent.bk.job.execute.model.tables.records.GseScriptExecuteObjTaskRecord;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.OrderField;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.SelectLimitPercentStep;
import org.jooq.SelectSeekStep1;
import org.jooq.TableField;
import org.jooq.UpdateConditionStep;
import org.jooq.UpdateSetMoreStep;
import org.jooq.UpdateSetStep;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.tencent.bk.job.common.constant.Order.DESCENDING;
import static org.jooq.impl.DSL.count;

@Repository
public class ScriptExecuteObjectTaskDAOImpl extends BaseDAO implements ScriptExecuteObjectTaskDAO {

    private static final GseScriptExecuteObjTask T = GseScriptExecuteObjTask.GSE_SCRIPT_EXECUTE_OBJ_TASK;

    private static final TableField<?, ?>[] ALL_FIELDS = {
        T.TASK_INSTANCE_ID,
        T.STEP_INSTANCE_ID,
        T.EXECUTE_COUNT,
        T.ACTUAL_EXECUTE_COUNT,
        T.BATCH,
        T.EXECUTE_OBJ_TYPE,
        T.EXECUTE_OBJ_ID,
        T.GSE_TASK_ID,
        T.STATUS,
        T.START_TIME,
        T.END_TIME,
        T.TOTAL_TIME,
        T.ERROR_CODE,
        T.EXIT_CODE,
        T.TAG,
        T.LOG_OFFSET
    };

    private static final String BATCH_INSERT_SQL =
        "insert into gse_script_execute_obj_task (id,task_instance_id,step_instance_id,execute_count,"
            + "actual_execute_count,batch,execute_obj_type,execute_obj_id,gse_task_id,status,start_time,end_time,"
            + "total_time,error_code,exit_code,tag,log_offset) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private static final String BATCH_UPDATE_SQL =
        "update gse_script_execute_obj_task set gse_task_id = ?, status = ?, start_time = ?, end_time = ?"
            + ", total_time = ?, error_code = ?, exit_code = ?, tag = ?, log_offset = ?"
            + " where task_instance_id = ? and step_instance_id = ? and execute_count = ? and batch = ?"
            + " and execute_obj_id = ?";

    public ScriptExecuteObjectTaskDAOImpl(DSLContextProviderFactory dslContextProviderFactory) {
        super(dslContextProviderFactory, T.getName());
    }

    @Override
    @MySQLOperation(table = "gse_script_execute_obj_task", op = DbOperationEnum.WRITE)
    public void batchSaveTasks(Collection<ExecuteObjectTask> tasks) {
        Object[][] params = new Object[tasks.size()][17];
        int batchCount = 0;
        for (ExecuteObjectTask task : tasks) {
            Object[] param = new Object[17];
            param[0] = task.getId();
            param[1] = task.getTaskInstanceId();
            param[2] = task.getStepInstanceId();
            param[3] = task.getExecuteCount();
            param[4] = task.getActualExecuteCount();
            param[5] = task.getBatch();
            param[6] = task.getExecuteObjectType().getValue();
            param[7] = task.getExecuteObjectId();
            param[8] = task.getGseTaskId();
            param[9] = task.getStatus().getValue();
            param[10] = task.getStartTime();
            param[11] = task.getEndTime();
            param[12] = task.getTotalTime();
            param[13] = task.getErrorCode();
            param[14] = task.getExitCode();
            param[15] = StringUtils.truncate(task.getTag(), JobConstants.RESULT_GROUP_TAG_MAX_LENGTH);
            param[16] = task.getScriptLogOffset();
            params[batchCount++] = param;
        }
        dsl().batch(BATCH_INSERT_SQL, params).execute();
    }

    @Override
    @MySQLOperation(table = "gse_script_execute_obj_task", op = DbOperationEnum.WRITE)
    public void batchUpdateTasks(Collection<ExecuteObjectTask> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }
        Object[][] params = new Object[tasks.size()][14];
        int batchCount = 0;
        for (ExecuteObjectTask task : tasks) {
            Object[] param = new Object[14];
            param[0] = task.getGseTaskId();
            param[1] = task.getStatus().getValue();
            param[2] = task.getStartTime();
            param[3] = task.getEndTime();
            param[4] = task.getTotalTime();
            param[5] = task.getErrorCode();
            param[6] = task.getExitCode();
            param[7] = StringUtils.truncate(task.getTag(), JobConstants.RESULT_GROUP_TAG_MAX_LENGTH);
            param[8] = task.getScriptLogOffset();
            param[9] = task.getTaskInstanceId();
            param[10] = task.getStepInstanceId();
            param[11] = task.getExecuteCount();
            param[12] = task.getBatch();
            param[13] = task.getExecuteObjectId();
            params[batchCount++] = param;
        }
        dsl().batch(BATCH_UPDATE_SQL, params).execute();
    }

    @Override
    @MySQLOperation(table = "gse_script_execute_obj_task", op = DbOperationEnum.READ)
    public int getSuccessTaskCount(Long taskInstanceId, long stepInstanceId, int executeCount) {
        Integer count = dsl().selectCount()
            .from(T)
            .where(T.STATUS.in(ExecuteObjectTaskStatusEnum.LAST_SUCCESS.getValue(),
                ExecuteObjectTaskStatusEnum.SUCCESS.getValue()))
            .and(T.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T.EXECUTE_COUNT.eq((short) executeCount))
            .and(buildTaskInstanceIdQueryCondition(taskInstanceId))
            .fetchOne(0, Integer.class);
        return count == null ? 0 : count;
    }

    private Condition buildTaskInstanceIdQueryCondition(Long taskInstanceId) {
        return TaskInstanceIdDynamicCondition.build(
            taskInstanceId,
            T.TASK_INSTANCE_ID::eq
        );
    }

    @Override
    @MySQLOperation(table = "gse_script_execute_obj_task", op = DbOperationEnum.READ)
    public List<ResultGroupBaseDTO> listResultGroups(Long taskInstanceId,
                                                     long stepInstanceId,
                                                     int executeCount,
                                                     Integer batch) {
        SelectConditionStep<?> selectConditionStep =
            dsl().select(T.STATUS, T.TAG, count().as("task_count"))
                .from(T)
                .where(T.STEP_INSTANCE_ID.eq(stepInstanceId))
                .and(T.EXECUTE_COUNT.eq((short) executeCount))
                .and(buildTaskInstanceIdQueryCondition(taskInstanceId));
        if (batch != null && batch > 0) {
            selectConditionStep.and(T.BATCH.eq(batch.shortValue()));
        }

        Result<?> result = selectConditionStep.groupBy(T.STATUS, T.TAG)
            .orderBy(T.STATUS.asc())
            .fetch();

        List<ResultGroupBaseDTO> resultGroups = new ArrayList<>();
        result.forEach(record -> {
            ResultGroupBaseDTO resultGroup = new ResultGroupBaseDTO();
            resultGroup.setStatus(record.get(T.STATUS).intValue());
            resultGroup.setTag(record.get(T.TAG));
            Object taskCount = record.get("task_count");
            resultGroup.setTotal(taskCount == null ? 0 : (int) taskCount);
            resultGroups.add(resultGroup);
        });
        return resultGroups;
    }

    @Override
    @MySQLOperation(table = "gse_script_execute_obj_task", op = DbOperationEnum.READ)
    public List<ExecuteObjectTask> listTasksByResultGroup(Long taskInstanceId,
                                                          Long stepInstanceId,
                                                          Integer executeCount,
                                                          Integer batch,
                                                          Integer status,
                                                          String tag) {
        SelectConditionStep<?> selectConditionStep = dsl().select(ALL_FIELDS)
            .from(T)
            .where(T.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T.EXECUTE_COUNT.eq(executeCount.shortValue()))
            .and(T.STATUS.eq(status))
            .and(T.TAG.eq(tag == null ? "" : tag))
            .and(buildTaskInstanceIdQueryCondition(taskInstanceId));
        if (batch != null && batch > 0) {
            selectConditionStep.and(T.BATCH.eq(batch.shortValue()));
        }
        Result<?> result = selectConditionStep.fetch();

        List<ExecuteObjectTask> executeObjectTasks = new ArrayList<>();
        if (result.size() > 0) {
            result.forEach(record -> executeObjectTasks.add(extract(record)));
        }
        return executeObjectTasks;
    }

    private ExecuteObjectTask extract(Record record) {
        if (record == null) {
            return null;
        }
        ExecuteObjectTask executeObjectTask = new ExecuteObjectTask();
        executeObjectTask.setTaskInstanceId(record.get(T.TASK_INSTANCE_ID));
        executeObjectTask.setStepInstanceId(record.get(T.STEP_INSTANCE_ID));
        executeObjectTask.setExecuteCount(record.get(T.EXECUTE_COUNT));
        Short actualExecuteCount = record.get(T.ACTUAL_EXECUTE_COUNT);
        executeObjectTask.setActualExecuteCount(actualExecuteCount != null ? actualExecuteCount.intValue() : null);
        executeObjectTask.setBatch(record.get(T.BATCH));
        executeObjectTask.setExecuteObjectId(record.get(T.EXECUTE_OBJ_ID));
        executeObjectTask.setExecuteObjectType(ExecuteObjectTypeEnum.valOf(record.get(T.EXECUTE_OBJ_TYPE).intValue()));
        executeObjectTask.setGseTaskId(record.get(T.GSE_TASK_ID));
        executeObjectTask.setStatus(ExecuteObjectTaskStatusEnum.valOf(record.get(T.STATUS)));
        executeObjectTask.setStartTime(record.get(T.START_TIME));
        executeObjectTask.setEndTime(record.get(T.END_TIME));
        executeObjectTask.setTotalTime(record.get(T.TOTAL_TIME));
        executeObjectTask.setErrorCode(record.get(T.ERROR_CODE));
        executeObjectTask.setExitCode(record.get(T.EXIT_CODE, Integer.class));
        executeObjectTask.setTag(record.get(T.TAG));
        executeObjectTask.setScriptLogOffset(record.get(T.LOG_OFFSET));
        return executeObjectTask;
    }

    @Override
    @MySQLOperation(table = "gse_script_execute_obj_task", op = DbOperationEnum.READ)
    public List<ExecuteObjectTask> listTasksByResultGroup(Long taskInstanceId,
                                                          Long stepInstanceId,
                                                          Integer executeCount,
                                                          Integer batch,
                                                          Integer status,
                                                          String tag,
                                                          Integer limit,
                                                          String orderField,
                                                          Order order) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(T.STEP_INSTANCE_ID.eq(stepInstanceId));
        conditions.add(T.EXECUTE_COUNT.eq(executeCount.shortValue()));
        conditions.add(T.STATUS.eq(status));
        conditions.add(T.TAG.eq(tag == null ? "" : tag));
        conditions.add(buildTaskInstanceIdQueryCondition(taskInstanceId));

        SelectConditionStep<Record> select = dsl().select(ALL_FIELDS)
            .from(T)
            .where(conditions);

        if (batch != null && batch > 0) {
            select.and(T.BATCH.eq(batch.shortValue()));
        }

        SelectSeekStep1<Record, ?> selectSeekStep = null;
        OrderField<?> orderFieldEntity = buildOrderField(orderField, order);
        if (orderFieldEntity != null) {
            selectSeekStep = select.orderBy(orderFieldEntity);
        }

        SelectLimitPercentStep<Record> selectLimitPercentStep = null;
        if (limit != null && limit > 0) {
            if (selectSeekStep != null) {
                selectLimitPercentStep = selectSeekStep.limit(limit);
            } else {
                selectLimitPercentStep = select.limit(limit);
            }
        }

        List<ExecuteObjectTask> executeObjectTasks = new ArrayList<>();
        Result<Record> result;
        if (selectLimitPercentStep != null) {
            result = selectLimitPercentStep.fetch();
        } else if (selectSeekStep != null) {
            result = selectSeekStep.fetch();
        } else {
            result = select.fetch();
        }

        if (result.size() > 0) {
            result.into(record -> executeObjectTasks.add(extract(record)));
        }
        return executeObjectTasks;
    }

    private OrderField<?> buildOrderField(String field, Order order) {
        OrderField<?> orderField = null;
        if (StringUtils.isNotBlank(field)) {
            if (field.equals(T.TOTAL_TIME.getName())) {
                if (order == DESCENDING) {
                    orderField = T.TOTAL_TIME.desc();
                } else {
                    orderField = T.TOTAL_TIME.asc();
                }
            } else if (field.equals(T.EXIT_CODE.getName())) {
                if (order == DESCENDING) {
                    orderField = T.EXIT_CODE.desc();
                } else {
                    orderField = T.EXIT_CODE.asc();
                }
            }
        }
        return orderField;
    }

    @Override
    @MySQLOperation(table = "gse_script_execute_obj_task", op = DbOperationEnum.READ)
    public List<ExecuteObjectTask> listTasks(Long taskInstanceId,
                                             Long stepInstanceId,
                                             Integer executeCount,
                                             Integer batch) {
        SelectConditionStep<?> selectConditionStep = dsl().select(ALL_FIELDS)
            .from(T)
            .where(T.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T.EXECUTE_COUNT.eq(executeCount.shortValue()))
            .and(buildTaskInstanceIdQueryCondition(taskInstanceId));
        if (batch != null && batch > 0) {
            selectConditionStep.and(T.BATCH.eq(batch.shortValue()));
        }
        Result<?> result = selectConditionStep.fetch();
        List<ExecuteObjectTask> executeObjectList = new ArrayList<>();
        if (result.size() != 0) {
            result.map(record -> {
                executeObjectList.add(extract(record));
                return null;
            });
        }
        return executeObjectList;
    }

    @Override
    @MySQLOperation(table = "gse_script_execute_obj_task", op = DbOperationEnum.READ)
    public List<ExecuteObjectTask> listTasksByGseTaskId(Long taskInstanceId, Long gseTaskId) {
        if (gseTaskId == null || gseTaskId <= 0) {
            return Collections.emptyList();
        }

        List<ExecuteObjectTask> executeObjectList = new ArrayList<>();

        Result<?> result = dsl().select(ALL_FIELDS)
            .from(T)
            .where(T.GSE_TASK_ID.eq(gseTaskId))
            .and(buildTaskInstanceIdQueryCondition(taskInstanceId))
            .fetch();
        if (result.size() > 0) {
            result.forEach(record -> executeObjectList.add(extract(record)));
        }
        return executeObjectList;
    }

    @Override
    @MySQLOperation(table = "gse_script_execute_obj_task", op = DbOperationEnum.READ)
    public ExecuteObjectTask getTaskByExecuteObjectId(Long taskInstanceId,
                                                      Long stepInstanceId,
                                                      Integer executeCount,
                                                      Integer batch,
                                                      String executeObjectId) {
        SelectConditionStep<?> selectConditionStep =
            dsl().select(ALL_FIELDS)
                .from(T)
                .where(buildTaskInstanceIdQueryCondition(taskInstanceId))
                .and(T.STEP_INSTANCE_ID.eq(stepInstanceId))
                .and(T.EXECUTE_COUNT.eq(executeCount.shortValue()))
                .and(T.EXECUTE_OBJ_ID.eq(executeObjectId));
        if (batch != null && batch > 0) {
            // 滚动执行批次，传入null或者0将忽略该参数
            selectConditionStep.and(T.BATCH.eq(batch.shortValue()));
        }
        selectConditionStep.limit(1);
        Record record = selectConditionStep.fetchOne();
        return extract(record);
    }

    @Override
    @MySQLOperation(table = "gse_script_execute_obj_task", op = DbOperationEnum.READ)
    public List<ExecuteObjectTask> getTaskByExecuteObjectIds(Long taskInstanceId,
                                                             Long stepInstanceId,
                                                             Integer executeCount,
                                                             Integer batch,
                                                             Collection<String> executeObjectIds) {
        SelectConditionStep<?> selectConditionStep =
            dsl().select(ALL_FIELDS)
                .from(T)
                .where(buildTaskInstanceIdQueryCondition(taskInstanceId))
                .and(T.STEP_INSTANCE_ID.eq(stepInstanceId))
                .and(T.EXECUTE_COUNT.eq(executeCount.shortValue()))
                .and(T.EXECUTE_OBJ_ID.in(executeObjectIds));
        if (batch != null && batch > 0) {
            // 滚动执行批次，传入null或者0将忽略该参数
            selectConditionStep.and(T.BATCH.eq(batch.shortValue()));
        }
        Result<?> records = selectConditionStep.fetch();
        return records.map(this::extract);
    }

    @Override
    @MySQLOperation(table = "gse_script_execute_obj_task", op = DbOperationEnum.READ)
    public boolean isStepInstanceRecordExist(Long taskInstanceId, long stepInstanceId) {
        return dsl().fetchExists(
            T,
            T.STEP_INSTANCE_ID.eq(stepInstanceId),
            buildTaskInstanceIdQueryCondition(taskInstanceId));
    }

    @Override
    @MySQLOperation(table = "gse_script_execute_obj_task", op = DbOperationEnum.WRITE)
    public void updateTaskFields(Long taskInstanceId,
                                 long stepInstanceId,
                                 int executeCount,
                                 Integer batch,
                                 Integer actualExecuteCount,
                                 Long gseTaskId) {
        UpdateSetStep<GseScriptExecuteObjTaskRecord> updateSetStep = dsl().update(T);
        boolean needUpdate = false;
        if (actualExecuteCount != null) {
            updateSetStep = updateSetStep.set(T.ACTUAL_EXECUTE_COUNT,
                actualExecuteCount.shortValue());
            needUpdate = true;
        }
        if (gseTaskId != null) {
            updateSetStep = updateSetStep.set(T.GSE_TASK_ID, gseTaskId);
            needUpdate = true;
        }

        if (!needUpdate) {
            return;
        }

        UpdateSetMoreStep<GseScriptExecuteObjTaskRecord> updateSetMoreStep =
            (UpdateSetMoreStep<GseScriptExecuteObjTaskRecord>) updateSetStep;

        UpdateConditionStep<GseScriptExecuteObjTaskRecord> updateConditionStep =
            updateSetMoreStep
                .where(T.STEP_INSTANCE_ID.eq(stepInstanceId))
                .and(buildTaskInstanceIdQueryCondition(taskInstanceId))
                .and(T.EXECUTE_COUNT.eq((short) executeCount));
        if (batch != null) {
            updateConditionStep.and(T.BATCH.eq(batch.shortValue()));
        }
        updateConditionStep.execute();
    }
}
