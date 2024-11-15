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

import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.constant.CompatibleType;
import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.common.mysql.dynamic.ds.DbOperationEnum;
import com.tencent.bk.job.common.mysql.dynamic.ds.MySQLOperation;
import com.tencent.bk.job.execute.dao.FileAgentTaskDAO;
import com.tencent.bk.job.execute.dao.common.DSLContextProviderFactory;
import com.tencent.bk.job.execute.engine.consts.ExecuteObjectTaskStatusEnum;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.ResultGroupBaseDTO;
import com.tencent.bk.job.execute.model.tables.GseFileAgentTask;
import com.tencent.bk.job.execute.model.tables.records.GseFileAgentTaskRecord;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.tencent.bk.job.common.constant.Order.DESCENDING;
import static org.jooq.impl.DSL.count;

@Repository
@Deprecated
@CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA)
public class FileAgentTaskDAOImpl extends BaseDAO implements FileAgentTaskDAO {

    private static final GseFileAgentTask T_GSE_FILE_AGENT_TASK = GseFileAgentTask.GSE_FILE_AGENT_TASK;
    private static final TableField<?, ?>[] ALL_FIELDS = {
        T_GSE_FILE_AGENT_TASK.TASK_INSTANCE_ID,
        T_GSE_FILE_AGENT_TASK.STEP_INSTANCE_ID,
        T_GSE_FILE_AGENT_TASK.EXECUTE_COUNT,
        T_GSE_FILE_AGENT_TASK.ACTUAL_EXECUTE_COUNT,
        T_GSE_FILE_AGENT_TASK.BATCH,
        T_GSE_FILE_AGENT_TASK.MODE,
        T_GSE_FILE_AGENT_TASK.HOST_ID,
        T_GSE_FILE_AGENT_TASK.AGENT_ID,
        T_GSE_FILE_AGENT_TASK.GSE_TASK_ID,
        T_GSE_FILE_AGENT_TASK.STATUS,
        T_GSE_FILE_AGENT_TASK.START_TIME,
        T_GSE_FILE_AGENT_TASK.END_TIME,
        T_GSE_FILE_AGENT_TASK.TOTAL_TIME,
        T_GSE_FILE_AGENT_TASK.ERROR_CODE
    };

    @Autowired
    public FileAgentTaskDAOImpl(DSLContextProviderFactory dslContextProviderFactory) {
        super(dslContextProviderFactory, T_GSE_FILE_AGENT_TASK.getName());
    }

    @Override
    @MySQLOperation(table = "gse_file_agent_task", op = DbOperationEnum.WRITE)
    public void batchSaveAgentTasks(Collection<ExecuteObjectTask> agentTasks) {
        String sql = "insert into gse_file_agent_task (task_instance_id, step_instance_id, execute_count, "
            + "actual_execute_count, batch, mode, host_id, agent_id ,gse_task_id,status, start_time, "
            + "end_time, total_time, error_code) "
            + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        Object[][] params = new Object[agentTasks.size()][14];
        int batchCount = 0;
        for (ExecuteObjectTask agentTask : agentTasks) {
            Object[] param = new Object[14];
            param[0] = agentTask.getTaskInstanceId();
            param[1] = agentTask.getStepInstanceId();
            param[2] = agentTask.getExecuteCount();
            param[3] = agentTask.getActualExecuteCount();
            param[4] = agentTask.getBatch();
            param[5] = agentTask.getFileTaskMode().getValue();
            param[6] = agentTask.getHostId();
            param[7] = agentTask.getAgentId() == null ? "" : agentTask.getAgentId();
            param[8] = agentTask.getGseTaskId();
            param[9] = agentTask.getStatus().getValue();
            param[10] = agentTask.getStartTime();
            param[11] = agentTask.getEndTime();
            param[12] = agentTask.getTotalTime();
            param[13] = agentTask.getErrorCode();
            params[batchCount++] = param;
        }
        dsl().batch(sql, params).execute();
    }

    @Override
    @MySQLOperation(table = "gse_file_agent_task", op = DbOperationEnum.WRITE)
    public void batchUpdateAgentTasks(Collection<ExecuteObjectTask> agentTasks) {
        if (CollectionUtils.isEmpty(agentTasks)) {
            return;
        }
        String sql = "update gse_file_agent_task set gse_task_id = ?, status = ?, start_time = ?, end_time = ?"
            + ", total_time = ?, error_code = ?"
            + " where task_instance_id = ? and step_instance_id = ? and execute_count = ?"
            + " and batch = ? and mode = ? and host_id = ?";
        Object[][] params = new Object[agentTasks.size()][12];
        int batchCount = 0;
        for (ExecuteObjectTask agentTask : agentTasks) {
            Object[] param = new Object[12];
            param[0] = agentTask.getGseTaskId();
            param[1] = agentTask.getStatus().getValue();
            param[2] = agentTask.getStartTime();
            param[3] = agentTask.getEndTime();
            param[4] = agentTask.getTotalTime();
            param[5] = agentTask.getErrorCode();
            param[6] = agentTask.getTaskInstanceId();
            param[7] = agentTask.getStepInstanceId();
            param[8] = agentTask.getExecuteCount();
            param[9] = agentTask.getBatch();
            param[10] = agentTask.getFileTaskMode().getValue();
            param[11] = agentTask.getHostId();
            params[batchCount++] = param;
        }
        dsl().batch(sql, params).execute();
    }

    @Override
    @MySQLOperation(table = "gse_file_agent_task", op = DbOperationEnum.READ)
    public int getSuccessAgentTaskCount(Long taskInstanceId, long stepInstanceId, int executeCount) {
        Integer count = dsl().selectCount()
            .from(T_GSE_FILE_AGENT_TASK)
            .where(T_GSE_FILE_AGENT_TASK.STATUS.in(ExecuteObjectTaskStatusEnum.LAST_SUCCESS.getValue(),
                ExecuteObjectTaskStatusEnum.SUCCESS.getValue()))
            .and(T_GSE_FILE_AGENT_TASK.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T_GSE_FILE_AGENT_TASK.EXECUTE_COUNT.eq((short) executeCount))
            .and(T_GSE_FILE_AGENT_TASK.MODE.eq(FileTaskModeEnum.DOWNLOAD.getValue().byteValue()))
            .and(buildTaskInstanceIdQueryCondition(taskInstanceId))
            .fetchOne(0, Integer.class);
        return count == null ? 0 : count;
    }

    private Condition buildTaskInstanceIdQueryCondition(Long taskInstanceId) {
        return TaskInstanceIdDynamicCondition.build(
            taskInstanceId,
            T_GSE_FILE_AGENT_TASK.TASK_INSTANCE_ID::eq
        );
    }

    @Override
    @MySQLOperation(table = "gse_file_agent_task", op = DbOperationEnum.READ)
    public List<ResultGroupBaseDTO> listResultGroups(Long taskInstanceId,
                                                     long stepInstanceId,
                                                     int executeCount,
                                                     Integer batch) {
        SelectConditionStep<?> selectConditionStep =
            dsl().select(T_GSE_FILE_AGENT_TASK.STATUS, count().as("ip_count"))
                .from(T_GSE_FILE_AGENT_TASK)
                .where(T_GSE_FILE_AGENT_TASK.STEP_INSTANCE_ID.eq(stepInstanceId))
                .and(T_GSE_FILE_AGENT_TASK.EXECUTE_COUNT.eq((short) executeCount))
                .and(T_GSE_FILE_AGENT_TASK.MODE.eq(FileTaskModeEnum.DOWNLOAD.getValue().byteValue()))
                .and(buildTaskInstanceIdQueryCondition(taskInstanceId));
        if (batch != null && batch > 0) {
            selectConditionStep.and(T_GSE_FILE_AGENT_TASK.BATCH.eq(batch.shortValue()));
        }

        Result<?> result = selectConditionStep.groupBy(T_GSE_FILE_AGENT_TASK.STATUS)
            .orderBy(T_GSE_FILE_AGENT_TASK.STATUS.asc())
            .fetch();

        List<ResultGroupBaseDTO> resultGroups = new ArrayList<>();
        result.forEach(record -> {
            ResultGroupBaseDTO resultGroup = new ResultGroupBaseDTO();
            resultGroup.setStatus(record.get(T_GSE_FILE_AGENT_TASK.STATUS));
            resultGroup.setTag("");
            Object ipCount = record.get("ip_count");
            resultGroup.setTotal(ipCount == null ? 0 : (int) ipCount);
            resultGroups.add(resultGroup);
        });
        return resultGroups;
    }

    @Override
    @MySQLOperation(table = "gse_file_agent_task", op = DbOperationEnum.READ)
    public List<ExecuteObjectTask> listAgentTaskByResultGroup(Long taskInstanceId,
                                                              Long stepInstanceId,
                                                              Integer executeCount,
                                                              Integer batch,
                                                              Integer status) {
        SelectConditionStep<?> selectConditionStep = dsl().select(ALL_FIELDS)
            .from(T_GSE_FILE_AGENT_TASK)
            .where(T_GSE_FILE_AGENT_TASK.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T_GSE_FILE_AGENT_TASK.EXECUTE_COUNT.eq(executeCount.shortValue()))
            .and(T_GSE_FILE_AGENT_TASK.STATUS.eq(status))
            .and(T_GSE_FILE_AGENT_TASK.MODE.eq(FileTaskModeEnum.DOWNLOAD.getValue().byteValue()))
            .and(buildTaskInstanceIdQueryCondition(taskInstanceId));
        if (batch != null && batch > 0) {
            selectConditionStep.and(T_GSE_FILE_AGENT_TASK.BATCH.eq(batch.shortValue()));
        }
        Result<?> result = selectConditionStep.fetch();

        List<ExecuteObjectTask> agentTasks = new ArrayList<>();
        if (result.size() > 0) {
            result.forEach(record -> agentTasks.add(extract(record)));
        }
        return agentTasks;
    }

    @Override
    @MySQLOperation(table = "gse_file_agent_task", op = DbOperationEnum.READ)
    public List<ExecuteObjectTask> listAgentTaskByResultGroup(Long taskInstanceId,
                                                              Long stepInstanceId,
                                                              Integer executeCount,
                                                              Integer batch,
                                                              Integer status,
                                                              Integer limit,
                                                              String orderField,
                                                              Order order) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(T_GSE_FILE_AGENT_TASK.STEP_INSTANCE_ID.eq(stepInstanceId));
        conditions.add(T_GSE_FILE_AGENT_TASK.EXECUTE_COUNT.eq(executeCount.shortValue()));
        conditions.add(T_GSE_FILE_AGENT_TASK.STATUS.eq(status));
        conditions.add(T_GSE_FILE_AGENT_TASK.MODE.eq(FileTaskModeEnum.DOWNLOAD.getValue().byteValue()));
        conditions.add(buildTaskInstanceIdQueryCondition(taskInstanceId));

        SelectConditionStep<Record> select = dsl().select(ALL_FIELDS)
            .from(T_GSE_FILE_AGENT_TASK)
            .where(conditions);

        if (batch != null && batch > 0) {
            select.and(T_GSE_FILE_AGENT_TASK.BATCH.eq(batch.shortValue()));
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

        List<ExecuteObjectTask> agentTasks = new ArrayList<>();
        Result<Record> result;
        if (selectLimitPercentStep != null) {
            result = selectLimitPercentStep.fetch();
        } else if (selectSeekStep != null) {
            result = selectSeekStep.fetch();
        } else {
            result = select.fetch();
        }

        if (result.size() > 0) {
            result.into(record -> agentTasks.add(extract(record)));
        }
        return agentTasks;
    }

    private OrderField<?> buildOrderField(String field, Order order) {
        OrderField<?> orderField = null;
        if (StringUtils.isNotBlank(field)) {
            if (field.equals(T_GSE_FILE_AGENT_TASK.TOTAL_TIME.getName())) {
                if (order == DESCENDING) {
                    orderField = T_GSE_FILE_AGENT_TASK.TOTAL_TIME.desc();
                } else {
                    orderField = T_GSE_FILE_AGENT_TASK.TOTAL_TIME.asc();
                }
            }
        }
        return orderField;
    }

    @Override
    @MySQLOperation(table = "gse_file_agent_task", op = DbOperationEnum.READ)
    public List<ExecuteObjectTask> listAgentTasks(Long taskInstanceId,
                                                  Long stepInstanceId,
                                                  Integer executeCount,
                                                  Integer batch,
                                                  FileTaskModeEnum fileTaskMode) {
        SelectConditionStep<?> selectConditionStep = dsl().select(ALL_FIELDS)
            .from(T_GSE_FILE_AGENT_TASK)
            .where(T_GSE_FILE_AGENT_TASK.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T_GSE_FILE_AGENT_TASK.EXECUTE_COUNT.eq(executeCount.shortValue()))
            .and(buildTaskInstanceIdQueryCondition(taskInstanceId));
        if (batch != null && batch > 0) {
            selectConditionStep.and(T_GSE_FILE_AGENT_TASK.BATCH.eq(batch.shortValue()));
        }
        if (fileTaskMode != null) {
            selectConditionStep.and(T_GSE_FILE_AGENT_TASK.MODE.eq(fileTaskMode.getValue().byteValue()));
        }
        Result<?> result = selectConditionStep.fetch();
        List<ExecuteObjectTask> agentTaskList = new ArrayList<>();
        if (result.size() != 0) {
            result.map(record -> {
                agentTaskList.add(extract(record));
                return null;
            });
        }
        return agentTaskList;
    }

    private ExecuteObjectTask extract(Record record) {
        if (record == null) {
            return null;
        }
        ExecuteObjectTask agentTask = new ExecuteObjectTask();
        agentTask.setTaskInstanceId(record.get(T_GSE_FILE_AGENT_TASK.TASK_INSTANCE_ID));
        agentTask.setStepInstanceId(record.get(T_GSE_FILE_AGENT_TASK.STEP_INSTANCE_ID));
        agentTask.setExecuteCount(record.get(T_GSE_FILE_AGENT_TASK.EXECUTE_COUNT));
        Short actualExecuteCount = record.get(T_GSE_FILE_AGENT_TASK.ACTUAL_EXECUTE_COUNT);
        agentTask.setActualExecuteCount(actualExecuteCount != null ? actualExecuteCount.intValue() : null);
        agentTask.setBatch(record.get(T_GSE_FILE_AGENT_TASK.BATCH));
        agentTask.setFileTaskMode(FileTaskModeEnum.getFileTaskMode(record.get(T_GSE_FILE_AGENT_TASK.MODE).intValue()));
        agentTask.setHostId(record.get(T_GSE_FILE_AGENT_TASK.HOST_ID));
        agentTask.setAgentId(record.get(T_GSE_FILE_AGENT_TASK.AGENT_ID));
        agentTask.setGseTaskId(record.get(T_GSE_FILE_AGENT_TASK.GSE_TASK_ID));
        agentTask.setStatus(ExecuteObjectTaskStatusEnum.valOf(record.get(T_GSE_FILE_AGENT_TASK.STATUS)));
        agentTask.setStartTime(record.get(T_GSE_FILE_AGENT_TASK.START_TIME));
        agentTask.setEndTime(record.get(T_GSE_FILE_AGENT_TASK.END_TIME));
        agentTask.setTotalTime(record.get(T_GSE_FILE_AGENT_TASK.TOTAL_TIME));
        agentTask.setErrorCode(record.get(T_GSE_FILE_AGENT_TASK.ERROR_CODE));
        return agentTask;
    }

    @Override
    @MySQLOperation(table = "gse_file_agent_task", op = DbOperationEnum.READ)
    public List<ExecuteObjectTask> listAgentTasksByGseTaskId(Long taskInstanceId, Long gseTaskId) {
        if (gseTaskId == null || gseTaskId <= 0) {
            return Collections.emptyList();
        }

        List<ExecuteObjectTask> agentTaskList = new ArrayList<>();

        Result<?> result = dsl().select(ALL_FIELDS)
            .from(T_GSE_FILE_AGENT_TASK)
            .where(T_GSE_FILE_AGENT_TASK.GSE_TASK_ID.eq(gseTaskId))
            .and(buildTaskInstanceIdQueryCondition(taskInstanceId))
            .fetch();
        if (result.size() > 0) {
            result.forEach(record -> agentTaskList.add(extract(record)));
        }
        return agentTaskList;
    }

    @Override
    @MySQLOperation(table = "gse_file_agent_task", op = DbOperationEnum.READ)
    public ExecuteObjectTask getAgentTaskByHostId(Long taskInstanceId,
                                                  Long stepInstanceId,
                                                  Integer executeCount,
                                                  Integer batch,
                                                  FileTaskModeEnum mode,
                                                  long hostId) {
        SelectConditionStep<?> selectConditionStep =
            dsl().select(ALL_FIELDS)
                .from(T_GSE_FILE_AGENT_TASK)
                .where(T_GSE_FILE_AGENT_TASK.STEP_INSTANCE_ID.eq(stepInstanceId))
                .and(T_GSE_FILE_AGENT_TASK.EXECUTE_COUNT.eq(executeCount.shortValue()))
                .and(T_GSE_FILE_AGENT_TASK.MODE.eq(mode.getValue().byteValue()))
                .and(T_GSE_FILE_AGENT_TASK.HOST_ID.eq(hostId))
                .and(buildTaskInstanceIdQueryCondition(taskInstanceId));
        if (batch != null && batch > 0) {
            // 滚动执行批次，传入null或者0将忽略该参数
            selectConditionStep.and(T_GSE_FILE_AGENT_TASK.BATCH.eq(batch.shortValue()));
        }
        selectConditionStep.limit(1);
        Record record = selectConditionStep.fetchOne();
        return extract(record);
    }

    @Override
    @MySQLOperation(table = "gse_file_agent_task", op = DbOperationEnum.WRITE)
    public void updateAgentTaskFields(Long taskInstanceId,
                                      long stepInstanceId,
                                      int executeCount,
                                      Integer batch,
                                      Integer actualExecuteCount,
                                      Long gseTaskId) {
        UpdateSetStep<GseFileAgentTaskRecord> updateSetStep = dsl().update(T_GSE_FILE_AGENT_TASK);
        boolean needUpdate = false;
        if (actualExecuteCount != null) {
            updateSetStep = updateSetStep.set(T_GSE_FILE_AGENT_TASK.ACTUAL_EXECUTE_COUNT,
                actualExecuteCount.shortValue());
            needUpdate = true;
        }
        if (gseTaskId != null) {
            updateSetStep = updateSetStep.set(T_GSE_FILE_AGENT_TASK.GSE_TASK_ID, gseTaskId);
            needUpdate = true;
        }

        if (!needUpdate) {
            return;
        }

        UpdateSetMoreStep<GseFileAgentTaskRecord> updateSetMoreStep =
            (UpdateSetMoreStep<GseFileAgentTaskRecord>) updateSetStep;

        UpdateConditionStep<GseFileAgentTaskRecord> updateConditionStep =
            updateSetMoreStep
                .where(T_GSE_FILE_AGENT_TASK.STEP_INSTANCE_ID.eq(stepInstanceId))
                .and(T_GSE_FILE_AGENT_TASK.EXECUTE_COUNT.eq((short) executeCount))
                .and(buildTaskInstanceIdQueryCondition(taskInstanceId));
        if (batch != null) {
            updateConditionStep.and(T_GSE_FILE_AGENT_TASK.BATCH.eq(batch.shortValue()));
        }
        updateConditionStep.execute();
    }
}
