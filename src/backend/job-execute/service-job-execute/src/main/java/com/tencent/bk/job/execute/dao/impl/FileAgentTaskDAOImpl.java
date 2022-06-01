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

import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.execute.dao.FileAgentTaskDAO;
import com.tencent.bk.job.execute.engine.consts.IpStatus;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.AgentTaskResultGroupBaseDTO;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.SelectLimitPercentStep;
import org.jooq.SelectSeekStep1;
import org.jooq.TableField;
import org.jooq.generated.tables.GseFileAgentTask;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tencent.bk.job.common.constant.Order.DESCENDING;
import static org.jooq.impl.DSL.count;

@Repository
public class FileAgentTaskDAOImpl implements FileAgentTaskDAO {

    private static final GseFileAgentTask T_GSE_FILE_AGENT_TASK = GseFileAgentTask.GSE_FILE_AGENT_TASK;
    private static final TableField<?, ?>[] ALL_FIELDS = {
        T_GSE_FILE_AGENT_TASK.STEP_INSTANCE_ID,
        T_GSE_FILE_AGENT_TASK.EXECUTE_COUNT,
        T_GSE_FILE_AGENT_TASK.BATCH,
        T_GSE_FILE_AGENT_TASK.MODE,
        T_GSE_FILE_AGENT_TASK.IP,
        T_GSE_FILE_AGENT_TASK.GSE_TASK_ID,
        T_GSE_FILE_AGENT_TASK.STATUS,
        T_GSE_FILE_AGENT_TASK.START_TIME,
        T_GSE_FILE_AGENT_TASK.END_TIME,
        T_GSE_FILE_AGENT_TASK.TOTAL_TIME,
        T_GSE_FILE_AGENT_TASK.ERROR_CODE,
        T_GSE_FILE_AGENT_TASK.DISPLAY_IP
    };

    private final DSLContext CTX;

    @Autowired
    public FileAgentTaskDAOImpl(@Qualifier("job-execute-dsl-context") DSLContext CTX) {
        this.CTX = CTX;
    }

    @Override
    public void batchSaveAgentTasks(List<AgentTaskDTO> agentTasks) {
        String sql = "insert into gse_file_agent_task (step_instance_id, execute_count, batch, mode, ip, gse_task_id"
            + ",status, start_time, end_time, total_time, error_code, display_ip)" +
            " values (?,?,?,?,?,?,?,?,?,?,?,?)";
        Object[][] params = new Object[agentTasks.size()][12];
        int batchCount = 0;
        for (AgentTaskDTO agentTask : agentTasks) {
            Object[] param = new Object[12];
            param[0] = agentTask.getStepInstanceId();
            param[1] = agentTask.getExecuteCount();
            param[2] = agentTask.getBatch();
            param[3] = agentTask.getFileTaskMode().getValue();
            param[4] = agentTask.getCloudIp();
            param[5] = agentTask.getGseTaskId();
            param[6] = agentTask.getStatus();
            param[7] = agentTask.getStartTime();
            param[8] = agentTask.getEndTime();
            param[9] = agentTask.getTotalTime();
            param[10] = agentTask.getErrorCode();
            param[11] = agentTask.getDisplayIp();
            params[batchCount++] = param;
        }
        CTX.transaction(configuration -> DSL.using(configuration).batch(sql, params).execute());
    }

    @Override
    public void batchUpdateAgentTasks(List<AgentTaskDTO> agentTasks) {
        if (CollectionUtils.isEmpty(agentTasks)) {
            return;
        }
        String sql = "update gse_file_agent_task set gse_task_id = ?, status = ?, start_time = ?, end_time = ?"
            + ", total_time = ?, error_code = ?"
            + " where step_instance_id = ? and execute_count = ? and batch = ? and mode = ? and ip = ?";
        Object[][] params = new Object[agentTasks.size()][11];
        int batchCount = 0;
        for (AgentTaskDTO agentTask : agentTasks) {
            Object[] param = new Object[11];
            param[0] = agentTask.getGseTaskId();
            param[1] = agentTask.getStatus();
            param[2] = agentTask.getStartTime();
            param[3] = agentTask.getEndTime();
            param[4] = agentTask.getTotalTime();
            param[5] = agentTask.getErrorCode();
            param[6] = agentTask.getStepInstanceId();
            param[7] = agentTask.getExecuteCount();
            param[8] = agentTask.getBatch();
            param[9] = agentTask.getFileTaskMode().getValue();
            param[10] = agentTask.getCloudIp();
            params[batchCount++] = param;
        }
        CTX.batch(sql, params).execute();
    }

    @Override
    public int getSuccessAgentTaskCount(long stepInstanceId, int executeCount) {
        Integer count = CTX.selectCount()
            .from(T_GSE_FILE_AGENT_TASK)
            .where(T_GSE_FILE_AGENT_TASK.STATUS.in(IpStatus.LAST_SUCCESS.getValue(), IpStatus.SUCCESS.getValue()))
            .and(T_GSE_FILE_AGENT_TASK.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T_GSE_FILE_AGENT_TASK.EXECUTE_COUNT.eq(executeCount))
            .and(T_GSE_FILE_AGENT_TASK.MODE.eq(FileTaskModeEnum.DOWNLOAD.getValue().byteValue()))
            .fetchOne(0, Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    public Map<IpStatus, Integer> countStepAgentTaskGroupByStatus(long stepInstanceId, int executeCount) {
        Result<?> result =
            CTX.select(T_GSE_FILE_AGENT_TASK.STATUS, count().as("ip_count"))
                .from(T_GSE_FILE_AGENT_TASK)
                .where(T_GSE_FILE_AGENT_TASK.STEP_INSTANCE_ID.eq(stepInstanceId))
                .and(T_GSE_FILE_AGENT_TASK.EXECUTE_COUNT.eq(executeCount))
                .and(T_GSE_FILE_AGENT_TASK.MODE.eq(FileTaskModeEnum.DOWNLOAD.getValue().byteValue()))
                .groupBy(T_GSE_FILE_AGENT_TASK.STATUS)
                .fetch();

        Map<IpStatus, Integer> agentTaskCountMap = new HashMap<>();
        if (result.size() != 0) {
            result.forEach(record -> {
                IpStatus status = IpStatus.valueOf(record.get(T_GSE_FILE_AGENT_TASK.STATUS));
                Object ipCount = record.get("ip_count");
                agentTaskCountMap.put(status, ipCount == null ? 0 : (int) ipCount);
            });
        }
        return agentTaskCountMap;
    }

    @Override
    public List<AgentTaskResultGroupBaseDTO> listResultGroups(long stepInstanceId, int executeCount, Integer batch) {
        SelectConditionStep<?> selectConditionStep =
            CTX.select(T_GSE_FILE_AGENT_TASK.STATUS, count().as("ip_count"))
                .from(T_GSE_FILE_AGENT_TASK)
                .where(T_GSE_FILE_AGENT_TASK.STEP_INSTANCE_ID.eq(stepInstanceId))
                .and(T_GSE_FILE_AGENT_TASK.EXECUTE_COUNT.eq(executeCount))
                .and(T_GSE_FILE_AGENT_TASK.MODE.eq(FileTaskModeEnum.DOWNLOAD.getValue().byteValue()));
        if (batch != null && batch > 0) {
            selectConditionStep.and(T_GSE_FILE_AGENT_TASK.BATCH.eq(batch.shortValue()));
        }

        Result<?> result = selectConditionStep.groupBy(T_GSE_FILE_AGENT_TASK.STATUS)
            .orderBy(T_GSE_FILE_AGENT_TASK.STATUS.asc())
            .fetch();

        List<AgentTaskResultGroupBaseDTO> resultGroups = new ArrayList<>();
        result.forEach(record -> {
            AgentTaskResultGroupBaseDTO resultGroup = new AgentTaskResultGroupBaseDTO();
            resultGroup.setStatus(record.get(T_GSE_FILE_AGENT_TASK.STATUS));
            resultGroup.setTag("");
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
                                                         Integer status) {
        SelectConditionStep<?> selectConditionStep = CTX.select(ALL_FIELDS)
            .from(T_GSE_FILE_AGENT_TASK)
            .where(T_GSE_FILE_AGENT_TASK.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T_GSE_FILE_AGENT_TASK.EXECUTE_COUNT.eq(executeCount))
            .and(T_GSE_FILE_AGENT_TASK.STATUS.eq(status))
            .and(T_GSE_FILE_AGENT_TASK.MODE.eq(FileTaskModeEnum.DOWNLOAD.getValue().byteValue()));
        if (batch != null && batch > 0) {
            selectConditionStep.and(T_GSE_FILE_AGENT_TASK.BATCH.eq(batch.shortValue()));
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
                                                         Integer limit,
                                                         String orderField,
                                                         Order order) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(T_GSE_FILE_AGENT_TASK.STEP_INSTANCE_ID.eq(stepInstanceId));
        conditions.add(T_GSE_FILE_AGENT_TASK.EXECUTE_COUNT.eq(executeCount));
        conditions.add(T_GSE_FILE_AGENT_TASK.STATUS.eq(status));
        conditions.add(T_GSE_FILE_AGENT_TASK.MODE.eq(FileTaskModeEnum.DOWNLOAD.getValue().byteValue()));

        SelectConditionStep<Record> select = CTX.select(ALL_FIELDS)
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
            if (field.equals(T_GSE_FILE_AGENT_TASK.TOTAL_TIME.getName())) {
                if (order == DESCENDING) {
                    orderField = T_GSE_FILE_AGENT_TASK.TOTAL_TIME.desc();
                } else {
                    orderField = T_GSE_FILE_AGENT_TASK.TOTAL_TIME.asc();
                }
            } else if (field.equals(T_GSE_FILE_AGENT_TASK.IP.getName())) {
                if (order == DESCENDING) {
                    orderField = T_GSE_FILE_AGENT_TASK.IP.desc();
                } else {
                    orderField = T_GSE_FILE_AGENT_TASK.IP.asc();
                }
            }
        }
        return orderField;
    }

    @Override
    public List<AgentTaskDTO> listAgentTasks(Long stepInstanceId,
                                             Integer executeCount,
                                             Integer batch,
                                             FileTaskModeEnum fileTaskMode) {
        SelectConditionStep<?> selectConditionStep = CTX.select(ALL_FIELDS)
            .from(T_GSE_FILE_AGENT_TASK)
            .where(T_GSE_FILE_AGENT_TASK.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T_GSE_FILE_AGENT_TASK.EXECUTE_COUNT.eq(executeCount));
        if (batch != null && batch > 0) {
            selectConditionStep.and(T_GSE_FILE_AGENT_TASK.BATCH.eq(batch.shortValue()));
        }
        if (fileTaskMode != null) {
            selectConditionStep.and(T_GSE_FILE_AGENT_TASK.MODE.eq(fileTaskMode.getValue().byteValue()));
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
        agentTask.setStepInstanceId(record.get(T_GSE_FILE_AGENT_TASK.STEP_INSTANCE_ID));
        agentTask.setExecuteCount(record.get(T_GSE_FILE_AGENT_TASK.EXECUTE_COUNT));
        agentTask.setBatch(record.get(T_GSE_FILE_AGENT_TASK.BATCH));
        agentTask.setFileTaskMode(FileTaskModeEnum.getFileTaskMode(record.get(T_GSE_FILE_AGENT_TASK.MODE).intValue()));
        agentTask.setCloudIp(record.get(T_GSE_FILE_AGENT_TASK.IP));
        agentTask.setGseTaskId(record.get(T_GSE_FILE_AGENT_TASK.GSE_TASK_ID));
        agentTask.setStatus(record.get(T_GSE_FILE_AGENT_TASK.STATUS));
        agentTask.setStartTime(record.get(T_GSE_FILE_AGENT_TASK.START_TIME));
        agentTask.setEndTime(record.get(T_GSE_FILE_AGENT_TASK.END_TIME));
        agentTask.setTotalTime(record.get(T_GSE_FILE_AGENT_TASK.TOTAL_TIME));
        agentTask.setErrorCode(record.get(T_GSE_FILE_AGENT_TASK.ERROR_CODE));
        agentTask.setDisplayIp(record.get(T_GSE_FILE_AGENT_TASK.DISPLAY_IP));
        String[] cloudAreaIdAndIpArray = record.get(T_GSE_FILE_AGENT_TASK.IP).split(":");
        agentTask.setCloudId(Long.valueOf(cloudAreaIdAndIpArray[0]));
        agentTask.setIp(cloudAreaIdAndIpArray[1]);
        return agentTask;
    }

    @Override
    public List<AgentTaskDTO> listAgentTasksByGseTaskId(Long gseTaskId) {
        List<AgentTaskDTO> agentTaskList = new ArrayList<>();

        Result<?> result = CTX.select(ALL_FIELDS)
            .from(T_GSE_FILE_AGENT_TASK)
            .where(T_GSE_FILE_AGENT_TASK.GSE_TASK_ID.eq(gseTaskId))
            .fetch();
        if (result.size() > 0) {
            result.forEach(record -> agentTaskList.add(extract(record)));
        }
        return agentTaskList;
    }

    @Override
    public AgentTaskDTO getAgentTaskByIp(Long stepInstanceId, Integer executeCount, Integer batch,
                                         FileTaskModeEnum mode, String cloudIp) {
        Record record = CTX.select(ALL_FIELDS)
            .from(T_GSE_FILE_AGENT_TASK)
            .where(T_GSE_FILE_AGENT_TASK.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T_GSE_FILE_AGENT_TASK.EXECUTE_COUNT.eq(executeCount))
            .and(T_GSE_FILE_AGENT_TASK.BATCH.eq(batch.shortValue()))
            .and(T_GSE_FILE_AGENT_TASK.MODE.eq(mode.getValue().byteValue()))
            .and(T_GSE_FILE_AGENT_TASK.IP.eq(cloudIp))
            .fetchOne();
        return extract(record);
    }

    @Override
    public List<String> fuzzySearchTargetIpsByIp(Long stepInstanceId, Integer executeCount, String ipKeyword) {
        Result<Record1<String>> result =
            CTX.select(T_GSE_FILE_AGENT_TASK.IP)
                .from(T_GSE_FILE_AGENT_TASK)
                .where(T_GSE_FILE_AGENT_TASK.STEP_INSTANCE_ID.eq(stepInstanceId))
                .and(T_GSE_FILE_AGENT_TASK.EXECUTE_COUNT.eq(executeCount))
                .and(T_GSE_FILE_AGENT_TASK.DISPLAY_IP.like("%" + ipKeyword + "%"))
                .and(T_GSE_FILE_AGENT_TASK.MODE.eq(FileTaskModeEnum.DOWNLOAD.getValue().byteValue()))
                .fetch();
        if (result.size() == 0) {
            return Collections.emptyList();
        }
        List<String> cloudIps = new ArrayList<>();
        result.into(record -> cloudIps.add(record.getValue(T_GSE_FILE_AGENT_TASK.IP)));
        return cloudIps;
    }

    @Override
    public List<String> getTaskFileSourceIps(Long stepInstanceId, Integer executeCount) {
        Result<Record1<String>> result =
            CTX.selectDistinct(T_GSE_FILE_AGENT_TASK.IP)
                .from(T_GSE_FILE_AGENT_TASK)
                .where(T_GSE_FILE_AGENT_TASK.STEP_INSTANCE_ID.eq(stepInstanceId))
                .and(T_GSE_FILE_AGENT_TASK.EXECUTE_COUNT.eq(executeCount))
                .and(T_GSE_FILE_AGENT_TASK.MODE.eq(FileTaskModeEnum.UPLOAD.getValue().byteValue()))
                .fetch();
        if (result.size() == 0) {
            return Collections.emptyList();
        }
        List<String> cloudIps = new ArrayList<>();
        result.into(record -> cloudIps.add(record.getValue(T_GSE_FILE_AGENT_TASK.IP)));
        return cloudIps;
    }

    @Override
    public int getActualSuccessExecuteCount(long stepInstanceId, Integer batch, FileTaskModeEnum mode, String cloudIp) {
        Record record = CTX.select(T_GSE_FILE_AGENT_TASK.EXECUTE_COUNT)
            .from(T_GSE_FILE_AGENT_TASK)
            .where(T_GSE_FILE_AGENT_TASK.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T_GSE_FILE_AGENT_TASK.BATCH.eq(batch == null ? 0 : batch.shortValue()))
            .and(T_GSE_FILE_AGENT_TASK.IP.eq(cloudIp))
            .and(T_GSE_FILE_AGENT_TASK.STATUS.eq(IpStatus.SUCCESS.getValue()))
            .and(T_GSE_FILE_AGENT_TASK.MODE.eq(mode.getValue().byteValue()))
            .orderBy(T_GSE_FILE_AGENT_TASK.EXECUTE_COUNT.desc())
            .limit(1)
            .fetchOne();
        if (record != null && record.size() > 0) {
            return record.getValue(T_GSE_FILE_AGENT_TASK.EXECUTE_COUNT);
        } else {
            return 0;
        }
    }
}
