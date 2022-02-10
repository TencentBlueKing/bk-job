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

import com.tencent.bk.job.common.constant.Bool;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.execute.dao.AgentTaskDAO;
import com.tencent.bk.job.execute.engine.consts.IpStatus;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.ResultGroupBaseDTO;
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
import org.jooq.generated.tables.GseTaskIpLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tencent.bk.job.common.constant.Order.DESCENDING;
import static org.jooq.impl.DSL.count;

@Repository
public class AgentTaskDAOImpl implements AgentTaskDAO {

    private static final GseTaskIpLog T_GSE_TASK_IP_LOG = GseTaskIpLog.GSE_TASK_IP_LOG;
    private static final TableField<?, ?>[] ALL_FIELDS = {
        T_GSE_TASK_IP_LOG.STEP_INSTANCE_ID,
        T_GSE_TASK_IP_LOG.EXECUTE_COUNT,
        T_GSE_TASK_IP_LOG.BATCH,
        T_GSE_TASK_IP_LOG.IP,
        T_GSE_TASK_IP_LOG.GSE_TASK_ID,
        T_GSE_TASK_IP_LOG.STATUS,
        T_GSE_TASK_IP_LOG.START_TIME,
        T_GSE_TASK_IP_LOG.END_TIME,
        T_GSE_TASK_IP_LOG.TOTAL_TIME,
        T_GSE_TASK_IP_LOG.ERROR_CODE,
        T_GSE_TASK_IP_LOG.EXIT_CODE,
        T_GSE_TASK_IP_LOG.TAG,
        T_GSE_TASK_IP_LOG.LOG_OFFSET,
        T_GSE_TASK_IP_LOG.DISPLAY_IP,
        T_GSE_TASK_IP_LOG.IS_TARGET,
        T_GSE_TASK_IP_LOG.IS_SOURCE};

    private final DSLContext CTX;

    @Autowired
    public AgentTaskDAOImpl(@Qualifier("job-execute-dsl-context") DSLContext CTX) {
        this.CTX = CTX;
    }

    @Override
    public void batchSaveAgentTasks(List<AgentTaskDTO> agentTasks) {
        String sql = "REPLACE INTO gse_task_ip_log (step_instance_id, execute_count, batch, ip, gse_task_id, status, " +
            "start_time, end_time, total_time, error_code, exit_code, tag, log_offset, display_ip, is_target," +
            "is_source)" +
            " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        Object[][] params = new Object[agentTasks.size()][16];
        int batchCount = 0;
        for (AgentTaskDTO agentTask : agentTasks) {
            Object[] param = new Object[16];
            param[0] = agentTask.getStepInstanceId();
            param[1] = agentTask.getExecuteCount();
            param[2] = agentTask.getBatch();
            param[3] = agentTask.getCloudIp();
            param[4] = agentTask.getGseTaskId();
            param[5] = agentTask.getStatus();
            param[6] = agentTask.getStartTime();
            param[7] = agentTask.getEndTime();
            param[8] = agentTask.getTotalTime();
            param[9] = agentTask.getErrorCode();
            param[10] = agentTask.getExitCode();
            param[11] = StringUtils.truncate(agentTask.getTag(), JobConstants.RESULT_GROUP_TAG_MAX_LENGTH);
            param[12] = agentTask.getScriptLogOffset();
            param[13] = agentTask.getDisplayIp();
            param[14] = agentTask.isTargetServer() ? Bool.TRUE.getValue() : Bool.FALSE.getValue();
            param[15] = agentTask.isSourceServer() ? Bool.TRUE.getValue() : Bool.FALSE.getValue();
            params[batchCount++] = param;
        }
        CTX.batch(sql, params).execute();
    }

    @Override
    public void batchUpdateAgentTasks(long stepInstanceId, int executeCount, Collection<String> cloudAreaAndIps,
                                      Long startTime, Long endTime, IpStatus ipStatus) {
        String sql = "update gse_task_ip_log set start_time = ?, end_time = ?, status = ? where step_instance_id = ? " +
            "and execute_count = ? and ip = ?";
        Object[][] params = new Object[cloudAreaAndIps.size()][6];
        int batchCount = 0;
        for (String ip : cloudAreaAndIps) {
            Object[] param = new Object[6];
            param[0] = startTime;
            param[1] = endTime;
            param[2] = ipStatus.getValue();
            param[3] = stepInstanceId;
            param[4] = executeCount;
            param[5] = ip;
            params[batchCount++] = param;
        }
        CTX.batch(sql, params).execute();
    }

    @Override
    public int getSuccessIpCount(long stepInstanceId, int executeCount) {
        return CTX.selectCount().from(T_GSE_TASK_IP_LOG)
            .where(T_GSE_TASK_IP_LOG.STATUS.in(IpStatus.LAST_SUCCESS.getValue(), IpStatus.SUCCESS.getValue()))
            .and(T_GSE_TASK_IP_LOG.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T_GSE_TASK_IP_LOG.EXECUTE_COUNT.eq(executeCount))
            .and(T_GSE_TASK_IP_LOG.IS_TARGET.eq((byte) Bool.TRUE.getValue()))
            .fetchOne(0, Integer.class);
    }

    @Override
    public Map<IpStatus, Integer> countStepAgentTaskGroupByStatus(long stepInstanceId, int executeCount) {
        Result<?> result = CTX.select(T_GSE_TASK_IP_LOG.STATUS, count().as("ip_count")).from(T_GSE_TASK_IP_LOG)
            .where(T_GSE_TASK_IP_LOG.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T_GSE_TASK_IP_LOG.EXECUTE_COUNT.eq(executeCount))
            .and(T_GSE_TASK_IP_LOG.IS_TARGET.eq((byte) Bool.TRUE.getValue()))
            .groupBy(T_GSE_TASK_IP_LOG.STATUS)
            .fetch();

        Map<IpStatus, Integer> agentTaskCountMap = new HashMap<>();
        if (result.size() != 0) {
            result.forEach(record -> {
                IpStatus status = IpStatus.valueOf(record.get(T_GSE_TASK_IP_LOG.STATUS));
                Integer count = (int) record.get("ip_count");
                agentTaskCountMap.put(status, count);
            });
        }
        return agentTaskCountMap;
    }

    @Override
    public List<AgentTaskDTO> listSuccessAgentTasks(long stepInstanceId, int executeCount) {
        Result<?> result = CTX.select(ALL_FIELDS)
            .from(T_GSE_TASK_IP_LOG)
            .where(T_GSE_TASK_IP_LOG.STATUS.in(IpStatus.LAST_SUCCESS.getValue(), IpStatus.SUCCESS.getValue()))
            .and(T_GSE_TASK_IP_LOG.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T_GSE_TASK_IP_LOG.EXECUTE_COUNT.eq(executeCount))
            .and(T_GSE_TASK_IP_LOG.IS_TARGET.eq(Bool.TRUE.getValue()))
            .fetch();
        List<AgentTaskDTO> successGseIpList = new ArrayList<>();
        result.forEach(record -> successGseIpList.add(extract(record)));
        return successGseIpList;
    }

    @Override
    public List<ResultGroupBaseDTO> listResultGroups(long stepInstanceId, int executeCount) {
        Result<?> result = CTX.select(T_GSE_TASK_IP_LOG.STATUS, T_GSE_TASK_IP_LOG.TAG, count().as("ip_count"))
            .from(T_GSE_TASK_IP_LOG)
            .where(T_GSE_TASK_IP_LOG.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T_GSE_TASK_IP_LOG.EXECUTE_COUNT.eq(executeCount))
            .and(T_GSE_TASK_IP_LOG.IS_TARGET.eq(Bool.TRUE.getValue()))
            .groupBy(T_GSE_TASK_IP_LOG.STATUS, T_GSE_TASK_IP_LOG.TAG)
            .orderBy(T_GSE_TASK_IP_LOG.STATUS.asc())
            .fetch();

        List<ResultGroupBaseDTO> resultGroups = new ArrayList<>();
        result.forEach(record -> {
            ResultGroupBaseDTO resultGroup = new ResultGroupBaseDTO();
            resultGroup.setResultType(record.get(T_GSE_TASK_IP_LOG.STATUS));
            resultGroup.setTag(record.get(T_GSE_TASK_IP_LOG.TAG));
            resultGroup.setAgentTaskCount((int) record.get("ip_count"));
            resultGroups.add(resultGroup);
        });
        return resultGroups;
    }

    @Override
    public List<AgentTaskDTO> listAgentTaskByResultType(Long stepInstanceId, Integer executeCount, Integer resultType,
                                                        String tag) {
        Result<?> result = CTX.select(ALL_FIELDS)
            .from(T_GSE_TASK_IP_LOG)
            .where(T_GSE_TASK_IP_LOG.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T_GSE_TASK_IP_LOG.EXECUTE_COUNT.eq(executeCount))
            .and(T_GSE_TASK_IP_LOG.STATUS.eq(resultType))
            .and(T_GSE_TASK_IP_LOG.TAG.eq(tag == null ? "" : tag))
            .and(T_GSE_TASK_IP_LOG.IS_TARGET.eq(Bool.TRUE.getValue()))
            .fetch();

        List<AgentTaskDTO> agentTasks = new ArrayList<>();
        if (result.size() > 0) {
            result.forEach(record -> agentTasks.add(extract(record)));
        }
        return agentTasks;
    }

    @Override
    public List<AgentTaskDTO> listAgentTaskByResultType(Long stepInstanceId, Integer executeCount, Integer resultType,
                                                        String tag, Integer limit, String orderField, Order order) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(T_GSE_TASK_IP_LOG.STEP_INSTANCE_ID.eq(stepInstanceId));
        conditions.add(T_GSE_TASK_IP_LOG.EXECUTE_COUNT.eq(executeCount));
        conditions.add(T_GSE_TASK_IP_LOG.STATUS.eq(resultType));
        conditions.add(T_GSE_TASK_IP_LOG.TAG.eq(tag == null ? "" : tag));
        conditions.add(T_GSE_TASK_IP_LOG.IS_TARGET.eq(Bool.TRUE.getValue()));

        SelectConditionStep select = CTX.select(ALL_FIELDS)
            .from(T_GSE_TASK_IP_LOG)
            .where(conditions);

        SelectSeekStep1 selectSeekStep = null;
        OrderField orderFieldEntity = buildOrderField(orderField, order);
        if (orderFieldEntity != null) {
            selectSeekStep = select.orderBy(orderFieldEntity);
        }

        SelectLimitPercentStep selectLimitPercentStep = null;
        if (limit != null && limit > 0) {
            if (selectSeekStep != null) {
                selectLimitPercentStep = selectSeekStep.limit(limit);
            } else {
                selectLimitPercentStep = select.limit(limit);
            }
        }

        List<AgentTaskDTO> agentTasks = new ArrayList<>();
        Result result;
        if (selectLimitPercentStep != null) {
            result = selectLimitPercentStep.fetch();
        } else if (selectSeekStep != null) {
            result = selectSeekStep.fetch();
        } else {
            result = select.fetch();
        }

        if (result.size() > 0) {
            result.into(record -> {
                agentTasks.add(extract(record));
            });
        }
        return agentTasks;
    }

    private OrderField<?> buildOrderField(String field, Order order) {
        OrderField<?> orderField = null;
        if (StringUtils.isNotBlank(field)) {
            if (field.equals(GseTaskIpLog.GSE_TASK_IP_LOG.TOTAL_TIME.getName())) {
                if (order == DESCENDING) {
                    orderField = GseTaskIpLog.GSE_TASK_IP_LOG.TOTAL_TIME.desc();
                } else {
                    orderField = GseTaskIpLog.GSE_TASK_IP_LOG.TOTAL_TIME.asc();
                }
            } else if (field.equals(GseTaskIpLog.GSE_TASK_IP_LOG.EXIT_CODE.getName())) {
                if (order == DESCENDING) {
                    orderField = GseTaskIpLog.GSE_TASK_IP_LOG.EXIT_CODE.desc();
                } else {
                    orderField = GseTaskIpLog.GSE_TASK_IP_LOG.EXIT_CODE.asc();
                }
            } else if (field.equals(GseTaskIpLog.GSE_TASK_IP_LOG.IP.getName())) {
                if (order == DESCENDING) {
                    orderField = GseTaskIpLog.GSE_TASK_IP_LOG.IP.desc();
                } else {
                    orderField = GseTaskIpLog.GSE_TASK_IP_LOG.IP.asc();
                }
            }
        }
        return orderField;
    }

    @Override
    public List<AgentTaskDTO> listAgentTasks(Long stepInstanceId,
                                             Integer executeCount,
                                             Integer batch,
                                             boolean onlyTargetIp) {
        SelectConditionStep<?> selectConditionStep = CTX.select(ALL_FIELDS)
            .from(T_GSE_TASK_IP_LOG)
            .where(T_GSE_TASK_IP_LOG.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T_GSE_TASK_IP_LOG.EXECUTE_COUNT.eq(executeCount));
        if (batch != null && batch > 0) {
            selectConditionStep.and(T_GSE_TASK_IP_LOG.BATCH.eq(batch.shortValue()));
        }
        if (onlyTargetIp) {
            selectConditionStep.and(T_GSE_TASK_IP_LOG.IS_TARGET.eq(Bool.TRUE.getValue()));
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
        agentTask.setStepInstanceId(record.get(T_GSE_TASK_IP_LOG.STEP_INSTANCE_ID));
        agentTask.setExecuteCount(record.get(T_GSE_TASK_IP_LOG.EXECUTE_COUNT));
        agentTask.setBatch(record.get(T_GSE_TASK_IP_LOG.BATCH));
        agentTask.setCloudIp(record.get(T_GSE_TASK_IP_LOG.IP));
        agentTask.setGseTaskId(record.get(T_GSE_TASK_IP_LOG.GSE_TASK_ID));
        agentTask.setStatus(record.get(T_GSE_TASK_IP_LOG.STATUS));
        agentTask.setStartTime(record.get(T_GSE_TASK_IP_LOG.START_TIME));
        agentTask.setEndTime(record.get(T_GSE_TASK_IP_LOG.END_TIME));
        agentTask.setTotalTime(record.get(T_GSE_TASK_IP_LOG.TOTAL_TIME));
        agentTask.setErrorCode(record.get(T_GSE_TASK_IP_LOG.ERROR_CODE));
        agentTask.setExitCode(record.get(T_GSE_TASK_IP_LOG.EXIT_CODE, Integer.class));
        agentTask.setTag(record.get(T_GSE_TASK_IP_LOG.TAG));
        agentTask.setScriptLogOffset(record.get(T_GSE_TASK_IP_LOG.LOG_OFFSET));
        agentTask.setDisplayIp(record.get(T_GSE_TASK_IP_LOG.DISPLAY_IP));
        String[] cloudAreaIdAndIpArray = record.get(T_GSE_TASK_IP_LOG.IP).split(":");
        agentTask.setCloudId(Long.valueOf(cloudAreaIdAndIpArray[0]));
        agentTask.setIp(cloudAreaIdAndIpArray[1]);
        agentTask.setTargetServer(record.get(T_GSE_TASK_IP_LOG.IS_TARGET) == Bool.TRUE.getValue());
        agentTask.setSourceServer(record.get(T_GSE_TASK_IP_LOG.IS_SOURCE) == Bool.TRUE.getValue());
        return agentTask;
    }

    @Override
    public List<AgentTaskDTO> listAgentTasksByGseTaskId(Long gseTaskId) {
        List<AgentTaskDTO> agentTaskList = new ArrayList<>();

        Result<?> result = CTX.select(ALL_FIELDS)
            .from(T_GSE_TASK_IP_LOG)
            .where(T_GSE_TASK_IP_LOG.GSE_TASK_ID.eq(gseTaskId))
            .fetch();
        if (result.size() > 0) {
            result.forEach(record -> agentTaskList.add(extract(record)));
        }
        return agentTaskList;
    }

    @Override
    public AgentTaskDTO getAgentTaskByIp(Long stepInstanceId, Integer executeCount, String ip) {
        Record record = CTX.select(ALL_FIELDS)
            .from(T_GSE_TASK_IP_LOG)
            .where(T_GSE_TASK_IP_LOG.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T_GSE_TASK_IP_LOG.EXECUTE_COUNT.eq(executeCount))
            .and(T_GSE_TASK_IP_LOG.IP.eq(ip))
            .fetchOne();
        return extract(record);
    }

    @Override
    public List<AgentTaskDTO> listAgentTasksByIps(Long stepInstanceId, Integer executeCount, String[] ipArray) {
        Result result = CTX.select(ALL_FIELDS)
            .from(T_GSE_TASK_IP_LOG)
            .where(T_GSE_TASK_IP_LOG.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T_GSE_TASK_IP_LOG.EXECUTE_COUNT.eq(executeCount))
            .and(T_GSE_TASK_IP_LOG.IP.in(ipArray))
            .and(T_GSE_TASK_IP_LOG.IS_TARGET.eq(Bool.TRUE.getValue()))
            .fetch();
        List<AgentTaskDTO> agentTaskList = new ArrayList<>();
        if (result.size() != 0) {
            result.into(record -> {
                agentTaskList.add(extract(record));
            });
        }
        return agentTaskList;
    }

    @Override
    public void deleteAllAgentTasks(long stepInstanceId, int executeCount) {
        CTX.deleteFrom(T_GSE_TASK_IP_LOG)
            .where(T_GSE_TASK_IP_LOG.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T_GSE_TASK_IP_LOG.EXECUTE_COUNT.eq((executeCount)))
            .execute();
    }

    @Override
    public int getSuccessRetryCount(long stepInstanceId, String cloudAreaAndIp) {
        Record record = CTX.select(T_GSE_TASK_IP_LOG.EXECUTE_COUNT)
            .from(T_GSE_TASK_IP_LOG)
            .where(T_GSE_TASK_IP_LOG.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T_GSE_TASK_IP_LOG.IP.eq(cloudAreaAndIp))
            .and(T_GSE_TASK_IP_LOG.STATUS.eq(IpStatus.SUCCESS.getValue()))
            .orderBy(T_GSE_TASK_IP_LOG.EXECUTE_COUNT.desc())
            .limit(1)
            .fetchOne();
        if (record != null && record.size() > 0) {
            return record.getValue(T_GSE_TASK_IP_LOG.EXECUTE_COUNT);
        } else {
            return 0;
        }
    }

    @Override
    public List<String> getTaskFileSourceIps(Long stepInstanceId, Integer executeCount) {
        Result result = CTX.select(T_GSE_TASK_IP_LOG.IP)
            .from(T_GSE_TASK_IP_LOG)
            .where(T_GSE_TASK_IP_LOG.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T_GSE_TASK_IP_LOG.EXECUTE_COUNT.eq(executeCount))
            .and(T_GSE_TASK_IP_LOG.IS_SOURCE.eq(Bool.TRUE.getValue()))
            .fetch();
        List<String> ips = new ArrayList<>();
        if (result != null && result.size() > 0) {
            result.into(record -> ips.add(record.getValue(T_GSE_TASK_IP_LOG.IP)));
        }
        return ips;
    }

    @Override
    public List<String> fuzzySearchTargetIpsByIp(Long stepInstanceId, Integer executeCount, String searchIp) {
        Result result = CTX.select(T_GSE_TASK_IP_LOG.IP)
            .from(T_GSE_TASK_IP_LOG)
            .where(T_GSE_TASK_IP_LOG.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(T_GSE_TASK_IP_LOG.EXECUTE_COUNT.eq(executeCount))
            .and(T_GSE_TASK_IP_LOG.DISPLAY_IP.like("%" + searchIp + "%"))
            .and(T_GSE_TASK_IP_LOG.IS_TARGET.eq(Bool.TRUE.getValue()))
            .fetch();
        if (result == null || result.size() == 0) {
            return Collections.emptyList();
        }
        List<String> cloudIps = new ArrayList<>();
        result.into(record -> cloudIps.add(record.getValue(T_GSE_TASK_IP_LOG.IP)));
        return cloudIps;
    }
}
