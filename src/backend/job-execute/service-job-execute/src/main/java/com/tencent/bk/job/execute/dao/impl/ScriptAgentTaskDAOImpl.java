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

import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.execute.dao.ScriptAgentTaskDAO;
import com.tencent.bk.job.execute.engine.consts.AgentTaskStatus;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.AgentTaskResultGroupBaseDTO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.SelectLimitPercentStep;
import org.jooq.SelectSeekStep1;
import org.jooq.TableField;
import org.jooq.generated.tables.GseScriptAgentTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.tencent.bk.job.common.constant.Order.DESCENDING;
import static org.jooq.impl.DSL.count;

@Repository
public class ScriptAgentTaskDAOImpl implements ScriptAgentTaskDAO {

    private static final GseScriptAgentTask T_GSE_SCRIPT_AGENT_TASK = GseScriptAgentTask.GSE_SCRIPT_AGENT_TASK;
    private static final TableField<?, ?>[] ALL_FIELDS = {
        T_GSE_SCRIPT_AGENT_TASK.STEP_INSTANCE_ID,
        T_GSE_SCRIPT_AGENT_TASK.EXECUTE_COUNT,
        T_GSE_SCRIPT_AGENT_TASK.BATCH,
        T_GSE_SCRIPT_AGENT_TASK.HOST_ID,
        T_GSE_SCRIPT_AGENT_TASK.AGENT_ID,
        T_GSE_SCRIPT_AGENT_TASK.GSE_TASK_ID,
        T_GSE_SCRIPT_AGENT_TASK.STATUS,
        T_GSE_SCRIPT_AGENT_TASK.START_TIME,
        T_GSE_SCRIPT_AGENT_TASK.END_TIME,
        T_GSE_SCRIPT_AGENT_TASK.TOTAL_TIME,
        T_GSE_SCRIPT_AGENT_TASK.ERROR_CODE,
        T_GSE_SCRIPT_AGENT_TASK.EXIT_CODE,
        T_GSE_SCRIPT_AGENT_TASK.TAG,
        T_GSE_SCRIPT_AGENT_TASK.LOG_OFFSET
    };

    private final DSLContext CTX;

    @Autowired
    public ScriptAgentTaskDAOImpl(@Qualifier("job-execute-dsl-context") DSLContext CTX) {
        this.CTX = CTX;
    }

    @Override
    public void batchSaveAgentTasks(Collection<AgentTaskDTO> agentTasks) {
        String sql = "insert into gse_script_agent_task (step_instance_id, execute_count, batch, host_id, agent_id, "
            + "gse_task_id, status, start_time, end_time, total_time, error_code, exit_code, tag, log_offset)"
            + " values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        Object[][] params = new Object[agentTasks.size()][14];
        int batchCount = 0;
        for (AgentTaskDTO agentTask : agentTasks) {
            Object[] param = new Object[14];
            param[0] = agentTask.getStepInstanceId();
            param[1] = agentTask.getExecuteCount();
            param[2] = agentTask.getBatch();
            param[3] = agentTask.getHostId();
            param[4] = agentTask.getAgentId();
            param[5] = agentTask.getGseTaskId();
            param[6] = agentTask.getStatus();
            param[7] = agentTask.getStartTime();
            param[8] = agentTask.getEndTime();
            param[9] = agentTask.getTotalTime();
            param[10] = agentTask.getErrorCode();
            param[11] = agentTask.getExitCode();
            param[12] = StringUtils.truncate(agentTask.getTag(), JobConstants.RESULT_GROUP_TAG_MAX_LENGTH);
            param[13] = agentTask.getScriptLogOffset();
            params[batchCount++] = param;
        }
        CTX.batch(sql, params).execute();
    }

    @Override
    public void batchUpdateAgentTasks(Collection<AgentTaskDTO> agentTasks) {
        if (CollectionUtils.isEmpty(agentTasks)) {
            return;
        }
        String sql = "update gse_script_agent_task set gse_task_id = ?, status = ?, start_time = ?, end_time = ?"
            + ", total_time = ?, error_code = ?, exit_code = ?, tag = ?, log_offset = ?"
            + " where step_instance_id = ? and execute_count = ? and batch = ? and host_id = ?";
        Object[][] params = new Object[agentTasks.size()][13];
        int batchCount = 0;
        for (AgentTaskDTO agentTask : agentTasks) {
            Object[] param = new Object[13];
            param[0] = agentTask.getGseTaskId();
            param[1] = agentTask.getStatus();
            param[2] = agentTask.getStartTime();
            param[3] = agentTask.getEndTime();
            param[4] = agentTask.getTotalTime();
            param[5] = agentTask.getErrorCode();
            param[6] = agentTask.getExitCode();
            param[7] = StringUtils.truncate(agentTask.getTag(), JobConstants.RESULT_GROUP_TAG_MAX_LENGTH);
            param[8] = agentTask.getScriptLogOffset();
            param[9] = agentTask.getStepInstanceId();
            param[10] = agentTask.getExecuteCount();
            param[11] = agentTask.getBatch();
            param[12] = agentTask.getHostId();
            params[batchCount++] = param;
        }
        CTX.batch(sql, params).execute();
    }

    @Override
    public int getSuccessAgentTaskCount(long stepInstanceId, int executeCount) {
        Integer count = CTX.selectCount()
            .from(T_GSE_SCRIPT_AGENT_TASK)
            .where(T_GSE_SCRIPT_AGENT_TASK.STATUS.in(AgentTaskStatus.LAST_SUCCESS.getValue(), AgentTaskStatus.SUCCESS.getValue()))
            .and(T_GSE_SCRIPT_AGENT_TASK.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T_GSE_SCRIPT_AGENT_TASK.EXECUTE_COUNT.eq(executeCount))
            .fetchOne(0, Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    public List<AgentTaskResultGroupBaseDTO> listResultGroups(long stepInstanceId, int executeCount, Integer batch) {
        SelectConditionStep<?> selectConditionStep =
            CTX.select(T_GSE_SCRIPT_AGENT_TASK.STATUS, T_GSE_SCRIPT_AGENT_TASK.TAG, count().as("ip_count"))
                .from(T_GSE_SCRIPT_AGENT_TASK)
                .where(T_GSE_SCRIPT_AGENT_TASK.STEP_INSTANCE_ID.eq(stepInstanceId))
                .and(T_GSE_SCRIPT_AGENT_TASK.EXECUTE_COUNT.eq(executeCount));
        if (batch != null && batch > 0) {
            selectConditionStep.and(T_GSE_SCRIPT_AGENT_TASK.BATCH.eq(batch.shortValue()));
        }

        Result<?> result = selectConditionStep.groupBy(T_GSE_SCRIPT_AGENT_TASK.STATUS, T_GSE_SCRIPT_AGENT_TASK.TAG)
            .orderBy(T_GSE_SCRIPT_AGENT_TASK.STATUS.asc())
            .fetch();

        List<AgentTaskResultGroupBaseDTO> resultGroups = new ArrayList<>();
        result.forEach(record -> {
            AgentTaskResultGroupBaseDTO resultGroup = new AgentTaskResultGroupBaseDTO();
            resultGroup.setStatus(record.get(T_GSE_SCRIPT_AGENT_TASK.STATUS));
            resultGroup.setTag(record.get(T_GSE_SCRIPT_AGENT_TASK.TAG));
            Object ipCount = record.get("ip_count");
            resultGroup.setTotalAgentTasks(ipCount == null ? 0 : (int) ipCount);
            resultGroups.add(resultGroup);
        });
        return resultGroups;
    }

    @Override
    public List<AgentTaskDTO> listAgentTaskByResultGroup(Long stepInstanceId,
                                                         Integer executeCount,
                                                         Integer batch,
                                                         Integer status,
                                                         String tag) {
        SelectConditionStep<?> selectConditionStep = CTX.select(ALL_FIELDS)
            .from(T_GSE_SCRIPT_AGENT_TASK)
            .where(T_GSE_SCRIPT_AGENT_TASK.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T_GSE_SCRIPT_AGENT_TASK.EXECUTE_COUNT.eq(executeCount))
            .and(T_GSE_SCRIPT_AGENT_TASK.STATUS.eq(status))
            .and(T_GSE_SCRIPT_AGENT_TASK.TAG.eq(tag == null ? "" : tag));
        if (batch != null && batch > 0) {
            selectConditionStep.and(T_GSE_SCRIPT_AGENT_TASK.BATCH.eq(batch.shortValue()));
        }
        Result<?> result = selectConditionStep.fetch();

        List<AgentTaskDTO> agentTasks = new ArrayList<>();
        if (result.size() > 0) {
            result.forEach(record -> agentTasks.add(extract(record)));
        }
        return agentTasks;
    }

    @Override
    public List<AgentTaskDTO> listAgentTaskByResultGroup(Long stepInstanceId,
                                                         Integer executeCount,
                                                         Integer batch,
                                                         Integer status,
                                                         String tag,
                                                         Integer limit,
                                                         String orderField,
                                                         Order order) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(T_GSE_SCRIPT_AGENT_TASK.STEP_INSTANCE_ID.eq(stepInstanceId));
        conditions.add(T_GSE_SCRIPT_AGENT_TASK.EXECUTE_COUNT.eq(executeCount));
        conditions.add(T_GSE_SCRIPT_AGENT_TASK.STATUS.eq(status));
        conditions.add(T_GSE_SCRIPT_AGENT_TASK.TAG.eq(tag == null ? "" : tag));

        SelectConditionStep<Record> select = CTX.select(ALL_FIELDS)
            .from(T_GSE_SCRIPT_AGENT_TASK)
            .where(conditions);

        if (batch != null && batch > 0) {
            select.and(T_GSE_SCRIPT_AGENT_TASK.BATCH.eq(batch.shortValue()));
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

        List<AgentTaskDTO> agentTasks = new ArrayList<>();
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
            if (field.equals(T_GSE_SCRIPT_AGENT_TASK.TOTAL_TIME.getName())) {
                if (order == DESCENDING) {
                    orderField = T_GSE_SCRIPT_AGENT_TASK.TOTAL_TIME.desc();
                } else {
                    orderField = T_GSE_SCRIPT_AGENT_TASK.TOTAL_TIME.asc();
                }
            } else if (field.equals(T_GSE_SCRIPT_AGENT_TASK.EXIT_CODE.getName())) {
                if (order == DESCENDING) {
                    orderField = T_GSE_SCRIPT_AGENT_TASK.EXIT_CODE.desc();
                } else {
                    orderField = T_GSE_SCRIPT_AGENT_TASK.EXIT_CODE.asc();
                }
            }
        }
        return orderField;
    }

    @Override
    public List<AgentTaskDTO> listAgentTasks(Long stepInstanceId,
                                             Integer executeCount,
                                             Integer batch) {
        SelectConditionStep<?> selectConditionStep = CTX.select(ALL_FIELDS)
            .from(T_GSE_SCRIPT_AGENT_TASK)
            .where(T_GSE_SCRIPT_AGENT_TASK.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T_GSE_SCRIPT_AGENT_TASK.EXECUTE_COUNT.eq(executeCount));
        if (batch != null && batch > 0) {
            selectConditionStep.and(T_GSE_SCRIPT_AGENT_TASK.BATCH.eq(batch.shortValue()));
        }
        Result<?> result = selectConditionStep.fetch();
        List<AgentTaskDTO> agentTaskList = new ArrayList<>();
        if (result.size() != 0) {
            result.map(record -> {
                agentTaskList.add(extract(record));
                return null;
            });
        }
        return agentTaskList;
    }

    private AgentTaskDTO extract(Record record) {
        if (record == null) {
            return null;
        }
        AgentTaskDTO agentTask = new AgentTaskDTO();
        agentTask.setStepInstanceId(record.get(T_GSE_SCRIPT_AGENT_TASK.STEP_INSTANCE_ID));
        agentTask.setExecuteCount(record.get(T_GSE_SCRIPT_AGENT_TASK.EXECUTE_COUNT));
        agentTask.setBatch(record.get(T_GSE_SCRIPT_AGENT_TASK.BATCH));
        agentTask.setHostId(record.get(T_GSE_SCRIPT_AGENT_TASK.HOST_ID));
        agentTask.setAgentId(record.get(T_GSE_SCRIPT_AGENT_TASK.AGENT_ID));
        agentTask.setGseTaskId(record.get(T_GSE_SCRIPT_AGENT_TASK.GSE_TASK_ID));
        agentTask.setStatus(record.get(T_GSE_SCRIPT_AGENT_TASK.STATUS));
        agentTask.setStartTime(record.get(T_GSE_SCRIPT_AGENT_TASK.START_TIME));
        agentTask.setEndTime(record.get(T_GSE_SCRIPT_AGENT_TASK.END_TIME));
        agentTask.setTotalTime(record.get(T_GSE_SCRIPT_AGENT_TASK.TOTAL_TIME));
        agentTask.setErrorCode(record.get(T_GSE_SCRIPT_AGENT_TASK.ERROR_CODE));
        agentTask.setExitCode(record.get(T_GSE_SCRIPT_AGENT_TASK.EXIT_CODE, Integer.class));
        agentTask.setTag(record.get(T_GSE_SCRIPT_AGENT_TASK.TAG));
        agentTask.setScriptLogOffset(record.get(T_GSE_SCRIPT_AGENT_TASK.LOG_OFFSET));
        return agentTask;
    }

    @Override
    public List<AgentTaskDTO> listAgentTasksByGseTaskId(Long gseTaskId) {
        List<AgentTaskDTO> agentTaskList = new ArrayList<>();

        Result<?> result = CTX.select(ALL_FIELDS)
            .from(T_GSE_SCRIPT_AGENT_TASK)
            .where(T_GSE_SCRIPT_AGENT_TASK.GSE_TASK_ID.eq(gseTaskId))
            .fetch();
        if (result.size() > 0) {
            result.forEach(record -> agentTaskList.add(extract(record)));
        }
        return agentTaskList;
    }

    @Override
    public AgentTaskDTO getAgentTaskByHostId(Long stepInstanceId, Integer executeCount, Integer batch, long hostId) {
        Record record = CTX.select(ALL_FIELDS)
            .from(T_GSE_SCRIPT_AGENT_TASK)
            .where(T_GSE_SCRIPT_AGENT_TASK.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T_GSE_SCRIPT_AGENT_TASK.EXECUTE_COUNT.eq(executeCount))
            .and(T_GSE_SCRIPT_AGENT_TASK.BATCH.eq(batch == null ? 0 : batch.shortValue()))
            .and(T_GSE_SCRIPT_AGENT_TASK.HOST_ID.eq(hostId))
            .fetchOne();
        return extract(record);
    }

    @Override
    public int getActualSuccessExecuteCount(long stepInstanceId, Integer batch, long hostId) {
        Record record = CTX.select(T_GSE_SCRIPT_AGENT_TASK.EXECUTE_COUNT)
            .from(T_GSE_SCRIPT_AGENT_TASK)
            .where(T_GSE_SCRIPT_AGENT_TASK.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T_GSE_SCRIPT_AGENT_TASK.BATCH.eq(batch == null ? 0 : batch.shortValue()))
            .and(T_GSE_SCRIPT_AGENT_TASK.HOST_ID.eq(hostId))
            .and(T_GSE_SCRIPT_AGENT_TASK.STATUS.eq(AgentTaskStatus.SUCCESS.getValue()))
            .orderBy(T_GSE_SCRIPT_AGENT_TASK.EXECUTE_COUNT.desc())
            .limit(1)
            .fetchOne();
        if (record != null && record.size() > 0) {
            return record.getValue(T_GSE_SCRIPT_AGENT_TASK.EXECUTE_COUNT);
        } else {
            return 0;
        }
    }
}
