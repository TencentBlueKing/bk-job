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
import com.tencent.bk.job.common.constant.Bool;
import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.execute.dao.GseTaskIpLogDAO;
import com.tencent.bk.job.execute.engine.consts.AgentTaskStatusEnum;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.AgentTaskResultGroupBaseDTO;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.SelectLimitPercentStep;
import org.jooq.SelectSeekStep1;
import org.jooq.generated.tables.GseTaskIpLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.tencent.bk.job.common.constant.Order.DESCENDING;
import static org.jooq.impl.DSL.count;

@CompatibleImplementation(name = "rolling_execute", explain = "兼容老版本数据，过1-2个大版本之后删除", version = "3.7.x")
@Repository
public class GseTaskIpLogDAOImpl implements GseTaskIpLogDAO {

    private final DSLContext CTX;

    @Autowired
    public GseTaskIpLogDAOImpl(@Qualifier("job-execute-dsl-context") DSLContext CTX) {
        this.CTX = CTX;
    }

    @Override
    public int getSuccessAgentTaskCount(long stepInstanceId, int executeCount) {
        GseTaskIpLog t = GseTaskIpLog.GSE_TASK_IP_LOG;

        return CTX.selectCount().from(t)
            .where(t.STATUS.in(AgentTaskStatusEnum.LAST_SUCCESS.getValue(),
                AgentTaskStatusEnum.SUCCESS.getValue()))
            .and(t.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(t.EXECUTE_COUNT.eq(executeCount))
            .and(t.IS_TARGET.eq(Bool.TRUE.getValue()))
            .fetchOne(0, Integer.class);
    }

    @Override
    public List<AgentTaskResultGroupBaseDTO> listResultGroups(long stepInstanceId, int executeCount) {
        GseTaskIpLog t = GseTaskIpLog.GSE_TASK_IP_LOG;
        Result result = CTX.select(t.STATUS, t.TAG, count().as("ip_count")).from(t)
            .where(t.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(t.EXECUTE_COUNT.eq(executeCount))
            .and(t.IS_TARGET.eq(Bool.TRUE.getValue()))
            .groupBy(t.STATUS, t.TAG)
            .orderBy(t.STATUS.asc())
            .fetch();

        List<AgentTaskResultGroupBaseDTO> resultGroups = new ArrayList<>();
        result.into(record -> {
            AgentTaskResultGroupBaseDTO resultGroup = new AgentTaskResultGroupBaseDTO();
            resultGroup.setStatus(record.get(t.STATUS));
            resultGroup.setTag(record.get(t.TAG));
            resultGroup.setTotalAgentTasks((int) record.get("ip_count"));
            resultGroups.add(resultGroup);
        });
        return resultGroups;
    }

    @Override
    public List<AgentTaskDTO> listAgentTaskByResultGroup(Long stepInstanceId,
                                                         Integer executeCount,
                                                         Integer status,
                                                         String tag) {
        GseTaskIpLog t = GseTaskIpLog.GSE_TASK_IP_LOG;
        Result result = CTX.select(t.STEP_INSTANCE_ID, t.EXECUTE_COUNT, t.IP, t.STATUS, t.START_TIME, t.END_TIME,
            t.TOTAL_TIME, t.ERROR_CODE, t.EXIT_CODE, t.TAG, t.LOG_OFFSET, t.DISPLAY_IP, t.IS_TARGET, t.IS_SOURCE)
            .from(t)
            .where(t.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(t.EXECUTE_COUNT.eq(executeCount))
            .and(t.STATUS.eq(status))
            .and(t.TAG.eq(tag == null ? "" : tag))
            .and(t.IS_TARGET.eq(Bool.TRUE.getValue()))
            .fetch();

        List<AgentTaskDTO> agentTasks = new ArrayList<>();
        if (result.size() > 0) {
            result.into(record -> agentTasks.add(extract(record)));
        }
        return agentTasks;
    }

    @Override
    public List<AgentTaskDTO> listAgentTaskByResultGroup(Long stepInstanceId,
                                                         Integer executeCount,
                                                         Integer status,
                                                         String tag,
                                                         Integer limit,
                                                         String orderField,
                                                         Order order) {
        GseTaskIpLog t = GseTaskIpLog.GSE_TASK_IP_LOG;

        List<Condition> conditions = new ArrayList<>();
        conditions.add(t.STEP_INSTANCE_ID.eq(stepInstanceId));
        conditions.add(t.EXECUTE_COUNT.eq(executeCount));
        conditions.add(t.STATUS.eq(status));
        conditions.add(t.TAG.eq(tag == null ? "" : tag));
        conditions.add(t.IS_TARGET.eq(Bool.TRUE.getValue()));

        SelectConditionStep select = CTX.select(t.STEP_INSTANCE_ID, t.EXECUTE_COUNT, t.IP, t.STATUS, t.START_TIME,
            t.END_TIME,
            t.TOTAL_TIME, t.ERROR_CODE, t.EXIT_CODE, t.TAG, t.LOG_OFFSET, t.DISPLAY_IP, t.IS_TARGET, t.IS_SOURCE)
            .from(t)
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

    private OrderField buildOrderField(String field, Order order) {
        OrderField orderField = null;
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
    public List<AgentTaskDTO> listAgentTasks(Long stepInstanceId, Integer executeCount) {
        GseTaskIpLog t = GseTaskIpLog.GSE_TASK_IP_LOG;
        Result<? extends Record> result = CTX.select(t.STEP_INSTANCE_ID, t.EXECUTE_COUNT, t.IP, t.STATUS,
            t.START_TIME, t.END_TIME,
            t.TOTAL_TIME, t.ERROR_CODE, t.EXIT_CODE, t.TAG, t.LOG_OFFSET, t.DISPLAY_IP, t.IS_TARGET, t.IS_SOURCE)
            .from(t)
            .where(t.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(t.EXECUTE_COUNT.eq(executeCount))
            .fetch();
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
        GseTaskIpLog t = GseTaskIpLog.GSE_TASK_IP_LOG;
        AgentTaskDTO agentTask = new AgentTaskDTO();
        agentTask.setStepInstanceId(record.get(t.STEP_INSTANCE_ID));
        agentTask.setExecuteCount(record.get(t.EXECUTE_COUNT));
        agentTask.setCloudIp(record.get(t.IP));
        agentTask.setAgentId(record.get(t.IP));
        agentTask.setStatus(AgentTaskStatusEnum.valueOf(record.get(t.STATUS)));
        agentTask.setStartTime(record.get(t.START_TIME));
        agentTask.setEndTime(record.get(t.END_TIME));
        agentTask.setTotalTime(record.get(t.TOTAL_TIME));
        agentTask.setErrorCode(record.get(t.ERROR_CODE));
        agentTask.setExitCode(record.get(t.EXIT_CODE, Integer.class));
        agentTask.setTag(record.get(t.TAG));
        agentTask.setScriptLogOffset(record.get(t.LOG_OFFSET));
        boolean isUploadMode = record.get(t.IS_SOURCE) == Bool.TRUE.getValue();
        agentTask.setFileTaskMode(isUploadMode ? FileTaskModeEnum.UPLOAD : FileTaskModeEnum.DOWNLOAD);
        return agentTask;
    }

    @Override
    public AgentTaskDTO getAgentTaskByIp(Long stepInstanceId, Integer executeCount, String ip) {
        GseTaskIpLog t = GseTaskIpLog.GSE_TASK_IP_LOG;
        Record record = CTX.select(t.STEP_INSTANCE_ID, t.EXECUTE_COUNT, t.IP, t.STATUS, t.START_TIME, t.END_TIME,
            t.TOTAL_TIME, t.ERROR_CODE, t.EXIT_CODE, t.TAG, t.LOG_OFFSET, t.DISPLAY_IP, t.IS_TARGET, t.IS_SOURCE)
            .from(t)
            .where(t.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(t.EXECUTE_COUNT.eq(executeCount))
            .and(t.IP.eq(ip))
            .fetchOne();
        return extract(record);
    }

    @Override
    public int getActualSuccessExecuteCount(long stepInstanceId, String cloudIp) {
        GseTaskIpLog t = GseTaskIpLog.GSE_TASK_IP_LOG;
        Record record = CTX.select(t.EXECUTE_COUNT).from(t).where(t.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(t.IP.eq(cloudIp))
            .and(t.STATUS.eq(AgentTaskStatusEnum.SUCCESS.getValue()))
            .orderBy(t.EXECUTE_COUNT.desc())
            .limit(1)
            .fetchOne();
        if (record != null && record.size() > 0) {
            return record.getValue(t.EXECUTE_COUNT);
        } else {
            return 0;
        }
    }
}
