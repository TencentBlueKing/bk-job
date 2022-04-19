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

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.esb.model.EsbCallbackDTO;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.common.util.JooqDataTypeUtil;
import com.tencent.bk.job.execute.dao.TaskInstanceDAO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceQuery;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.SelectSeekStep1;
import org.jooq.SortField;
import org.jooq.UpdateSetMoreStep;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.GseTaskIpLog;
import org.jooq.generated.tables.StepInstance;
import org.jooq.generated.tables.TaskInstance;
import org.jooq.generated.tables.records.TaskInstanceRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 作业执行实例DAO
 */
@Slf4j
@Repository
public class TaskInstanceDAOImpl implements TaskInstanceDAO {
    private static final TaskInstance TABLE = TaskInstance.TASK_INSTANCE;
    private DSLContext ctx;

    @Autowired
    public TaskInstanceDAOImpl(@Qualifier("job-execute-dsl-context") DSLContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Long addTaskInstance(TaskInstanceDTO taskInstance) {
        Record record = ctx.insertInto(TABLE, TABLE.TASK_ID, TABLE.CRON_TASK_ID, TABLE.TASK_TEMPLATE_ID,
            TABLE.IS_DEBUG_TASK, TABLE.APP_ID, TABLE.NAME, TABLE.OPERATOR, TABLE.STARTUP_MODE, TABLE.CURRENT_STEP_ID,
            TABLE.STATUS, TABLE.START_TIME,
            TABLE.END_TIME, TABLE.TOTAL_TIME, TABLE.CREATE_TIME, TABLE.CALLBACK_URL, TABLE.CALLBACK, TABLE.TYPE, TABLE.APP_CODE)
            .values(taskInstance.getTaskId(),
                taskInstance.getCronTaskId(),
                taskInstance.getTaskTemplateId(),
                taskInstance.isDebugTask() ? (byte) 1 : (byte) 0,
                taskInstance.getAppId(),
                taskInstance.getName(),
                taskInstance.getOperator(),
                JooqDataTypeUtil.getByteFromInteger(taskInstance.getStartupMode()),
                taskInstance.getCurrentStepId(),
                JooqDataTypeUtil.getByteFromInteger(taskInstance.getStatus()),
                taskInstance.getStartTime(),
                taskInstance.getEndTime(),
                taskInstance.getTotalTime(),
                taskInstance.getCreateTime(),
                taskInstance.getCallbackUrl(),
                taskInstance.getCallback() == null ? null : JsonUtils.toJson(taskInstance.getCallback()),
                JooqDataTypeUtil.getByteFromInteger(taskInstance.getType()),
                taskInstance.getAppCode())
            .returning(TABLE.ID).fetchOne();
        return record.getValue(TABLE.ID);
    }

    @Override
    public TaskInstanceDTO getTaskInstance(long taskInstanceId) {
        Record record = ctx.select(TABLE.ID, TABLE.TASK_ID, TABLE.CRON_TASK_ID, TABLE.TASK_TEMPLATE_ID,
            TABLE.IS_DEBUG_TASK, TABLE.APP_ID, TABLE.NAME, TABLE.OPERATOR, TABLE.STARTUP_MODE, TABLE.CURRENT_STEP_ID,
            TABLE.STATUS,
            TABLE.START_TIME, TABLE.END_TIME, TABLE.TOTAL_TIME, TABLE.CREATE_TIME, TABLE.CALLBACK_URL, TABLE.CALLBACK, TABLE.TYPE,
            TABLE.APP_CODE).from(TABLE)
            .where(TABLE.ID.eq(taskInstanceId)).fetchOne();
        return extractInfo(record);
    }

    private TaskInstanceDTO extractInfo(Record record) {
        if (record == null) {
            return null;
        }
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setId(record.get(TaskInstance.TASK_INSTANCE.ID));
        taskInstance.setTaskId(record.get(TaskInstance.TASK_INSTANCE.TASK_ID));
        taskInstance.setCronTaskId(record.get(TaskInstance.TASK_INSTANCE.CRON_TASK_ID));
        taskInstance.setTaskTemplateId(record.get(TaskInstance.TASK_INSTANCE.TASK_TEMPLATE_ID));
        taskInstance.setDebugTask(record.get(TaskInstance.TASK_INSTANCE.IS_DEBUG_TASK) == 1);
        taskInstance.setAppId(record.get(TaskInstance.TASK_INSTANCE.APP_ID));
        taskInstance.setName(record.get(TaskInstance.TASK_INSTANCE.NAME));
        taskInstance.setType(JooqDataTypeUtil.getIntegerFromByte(record.get(TaskInstance.TASK_INSTANCE.TYPE)));
        taskInstance.setOperator(record.get(TaskInstance.TASK_INSTANCE.OPERATOR));
        taskInstance.setStartupMode(JooqDataTypeUtil.getIntegerFromByte(record.get(TaskInstance.TASK_INSTANCE.STARTUP_MODE)));
        taskInstance.setCurrentStepId(record.get(TaskInstance.TASK_INSTANCE.CURRENT_STEP_ID));
        taskInstance.setStatus(JooqDataTypeUtil.getIntegerFromByte(record.get(TaskInstance.TASK_INSTANCE.STATUS)));
        taskInstance.setStartTime(record.get(TaskInstance.TASK_INSTANCE.START_TIME));
        taskInstance.setEndTime(record.get(TaskInstance.TASK_INSTANCE.END_TIME));
        taskInstance.setTotalTime(record.get(TaskInstance.TASK_INSTANCE.TOTAL_TIME));
        taskInstance.setCreateTime(record.get(TaskInstance.TASK_INSTANCE.CREATE_TIME));
        taskInstance.setCallbackUrl(record.get(TaskInstance.TASK_INSTANCE.CALLBACK_URL));
        taskInstance.setCallback(record.get(TaskInstance.TASK_INSTANCE.CALLBACK) == null ? null : JsonUtils.fromJson(record.get(TaskInstance.TASK_INSTANCE.CALLBACK), new TypeReference<EsbCallbackDTO>() {
        }));
        taskInstance.setAppCode(record.get(TaskInstance.TASK_INSTANCE.APP_CODE));
        return taskInstance;
    }

    @Override
    public List<TaskInstanceDTO> getTaskInstanceByTaskId(long taskId) {
        Result result = ctx.select(TABLE.ID, TABLE.TASK_ID, TABLE.CRON_TASK_ID, TABLE.TASK_TEMPLATE_ID,
            TABLE.IS_DEBUG_TASK, TABLE.APP_ID, TABLE.NAME, TABLE.OPERATOR, TABLE.STARTUP_MODE, TABLE.CURRENT_STEP_ID,
            TABLE.STATUS,
            TABLE.START_TIME, TABLE.END_TIME, TABLE.TOTAL_TIME, TABLE.CREATE_TIME, TABLE.CALLBACK_URL, TABLE.CALLBACK, TABLE.TYPE,
            TABLE.APP_CODE).from(TABLE)
            .where(TABLE.TASK_ID.eq(taskId)).fetch();
        List<TaskInstanceDTO> taskInstances = new ArrayList<>();
        result.into(record -> {
            TaskInstanceDTO taskInstance = extractInfo(record);
            if (taskInstance != null) {
                taskInstances.add(taskInstance);
            }
        });
        return taskInstances;
    }

    @Override
    public void updateTaskStatus(long taskInstanceId, int status) {
        ctx.update(TABLE).set(TABLE.STATUS, Byte.valueOf(String.valueOf(status)))
            .where(TABLE.ID.eq(taskInstanceId))
            .execute();
    }

    @Override
    public void updateTaskStartTime(long taskInstanceId, Long startTime) {
        ctx.update(TABLE).set(TABLE.START_TIME, startTime)
            .where(TABLE.ID.eq(taskInstanceId))
            .execute();
    }

    @Override
    public void updateTaskEndTime(long taskInstanceId, Long endTime) {
        ctx.update(TABLE).set(TABLE.END_TIME, endTime)
            .where(TABLE.ID.eq(taskInstanceId))
            .execute();
    }

    @Override
    public List<Long> getTaskStepInstanceIdList(long taskInstanceId) {
        Result result = ctx.select(StepInstance.STEP_INSTANCE.ID).from(StepInstance.STEP_INSTANCE)
            .where(StepInstance.STEP_INSTANCE.TASK_INSTANCE_ID.eq(taskInstanceId))
            .orderBy(StepInstance.STEP_INSTANCE.ID.asc())
            .fetch();
        List<Long> stepInstanceIdList = new ArrayList<>();
        result.into(record -> {
            Long stepInstanceId = record.getValue(StepInstance.STEP_INSTANCE.ID);
            if (stepInstanceId != null) {
                stepInstanceIdList.add(stepInstanceId);
            }
        });
        return stepInstanceIdList;
    }

    @Override
    public void updateTaskCurrentStepId(Long taskInstanceId, Long stepInstanceId) {
        ctx.update(TABLE).set(TABLE.CURRENT_STEP_ID, stepInstanceId)
            .where(TABLE.ID.eq(taskInstanceId))
            .execute();
    }

    @Override
    public void resetTaskStatus(Long taskInstanceId) {
        ctx.update(TABLE).setNull(TABLE.START_TIME).setNull(TABLE.END_TIME).setNull(TABLE.TOTAL_TIME)
            .setNull(TABLE.CURRENT_STEP_ID)
            .set(TABLE.STATUS, JooqDataTypeUtil.getByteFromInteger(RunStatusEnum.BLANK.getValue()))
            .where(TABLE.ID.eq(taskInstanceId))
            .execute();
    }

    @Override
    public void cleanTaskEndTime(Long taskInstanceId) {
        ctx.update(TABLE).setNull(TABLE.END_TIME).setNull(TABLE.TOTAL_TIME)
            .where(TABLE.ID.eq(taskInstanceId))
            .execute();
    }

    @Override
    public void updateTaskTotalTime(Long taskInstanceId, Long totalTime) {
        ctx.update(TABLE).set(TABLE.TOTAL_TIME, totalTime)
            .where(TABLE.ID.eq(taskInstanceId))
            .execute();
    }

    @Override
    public PageData<TaskInstanceDTO> listPageTaskInstance(TaskInstanceQuery taskQuery,
                                                          BaseSearchCondition baseSearchCondition) {
        if (StringUtils.isNotEmpty(taskQuery.getIp())) {
            return listPageTaskInstanceByIp(taskQuery, baseSearchCondition);
        } else {
            return listPageTaskInstanceByBasicInfo(taskQuery, baseSearchCondition);
        }
    }

    private PageData<TaskInstanceDTO> listPageTaskInstanceByBasicInfo(TaskInstanceQuery taskQuery,
                                                                      BaseSearchCondition baseSearchCondition) {
        int count = getPageTaskInstanceCount(taskQuery);

        Collection<SortField<?>> orderFields = new ArrayList<>();
        orderFields.add(TABLE.CREATE_TIME.desc());
        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);
        Result<?> result = ctx.select(TABLE.ID, TABLE.TASK_ID, TABLE.CRON_TASK_ID, TABLE.TASK_TEMPLATE_ID,
            TABLE.IS_DEBUG_TASK, TABLE.APP_ID, TABLE.NAME, TABLE.OPERATOR, TABLE.STARTUP_MODE, TABLE.CURRENT_STEP_ID,
            TABLE.STATUS,
            TABLE.START_TIME, TABLE.END_TIME, TABLE.TOTAL_TIME, TABLE.CREATE_TIME, TABLE.CALLBACK_URL, TABLE.CALLBACK, TABLE.TYPE,
            TABLE.APP_CODE)
            .from(TaskInstanceDAOImpl.TABLE)
            .where(buildSearchCondition(taskQuery))
            .orderBy(orderFields)
            .limit(start, length)
            .fetch();
        return buildTaskInstancePageData(start, length, count, result);
    }

    private PageData<TaskInstanceDTO> listPageTaskInstanceByIp(TaskInstanceQuery taskQuery,
                                                               BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = buildSearchCondition(taskQuery);
        conditions.add(GseTaskIpLog.GSE_TASK_IP_LOG.DISPLAY_IP.eq(taskQuery.getIp()));
        int count = ctx.selectCount().from(TaskInstance.TASK_INSTANCE)
            .leftJoin(StepInstance.STEP_INSTANCE).on(TaskInstance.TASK_INSTANCE.ID.eq(StepInstance.STEP_INSTANCE.TASK_INSTANCE_ID))
            .leftJoin(GseTaskIpLog.GSE_TASK_IP_LOG).on(GseTaskIpLog.GSE_TASK_IP_LOG.STEP_INSTANCE_ID.eq(StepInstance.STEP_INSTANCE.ID))
            .where(conditions)
            .fetchOne(0, Integer.class);
        Collection<SortField<?>> orderFields = new ArrayList<>();
        orderFields.add(TABLE.ID.desc());
        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);
        Result result = ctx.select(TABLE.ID, TABLE.TASK_ID, TABLE.CRON_TASK_ID, TABLE.TASK_TEMPLATE_ID,
            TABLE.IS_DEBUG_TASK, TABLE.APP_ID, TABLE.NAME, TABLE.OPERATOR, TABLE.STARTUP_MODE, TABLE.CURRENT_STEP_ID,
            TABLE.STATUS,
            TABLE.START_TIME, TABLE.END_TIME, TABLE.TOTAL_TIME, TABLE.CREATE_TIME, TABLE.CALLBACK_URL, TABLE.CALLBACK, TABLE.TYPE,
            TABLE.APP_CODE, GseTaskIpLog.GSE_TASK_IP_LOG.DISPLAY_IP)
            .from(TaskInstanceDAOImpl.TABLE)
            .leftJoin(StepInstance.STEP_INSTANCE).on(TaskInstance.TASK_INSTANCE.ID.eq(StepInstance.STEP_INSTANCE.TASK_INSTANCE_ID))
            .leftJoin(GseTaskIpLog.GSE_TASK_IP_LOG).on(GseTaskIpLog.GSE_TASK_IP_LOG.STEP_INSTANCE_ID.eq(StepInstance.STEP_INSTANCE.ID))
            .where(conditions)
            .groupBy(TaskInstance.TASK_INSTANCE.ID)
            .orderBy(orderFields)
            .limit(start, length)
            .fetch();
        return buildTaskInstancePageData(start, length, count, result);
    }

    private PageData<TaskInstanceDTO> buildTaskInstancePageData(int start, int length, int count, Result result) {
        List<TaskInstanceDTO> taskInstances = new ArrayList<>();
        if (result != null && result.size() > 0) {
            result.into(record -> taskInstances.add(extractInfo(record)));
        }
        PageData<TaskInstanceDTO> pageData = new PageData<>();
        pageData.setData(taskInstances);
        pageData.setStart(start);
        pageData.setPageSize(length);
        pageData.setTotal(Long.valueOf(String.valueOf(count)));
        return pageData;
    }

    @SuppressWarnings("all")
    private int getPageTaskInstanceCount(TaskInstanceQuery taskQuery) {
        List<Condition> conditions = buildSearchCondition(taskQuery);
        return ctx.selectCount().from(TABLE).where(conditions).fetchOne(0, Integer.class);
    }

    private List<Condition> buildSearchCondition(TaskInstanceQuery taskQuery) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.eq(taskQuery.getAppId()));
        if (taskQuery.getTaskInstanceId() != null && taskQuery.getTaskInstanceId() > 0) {
            conditions.add(TABLE.ID.eq(taskQuery.getTaskInstanceId()));
            return conditions;
        }
        if (StringUtils.isNotBlank(taskQuery.getOperator())) {
            conditions.add(TABLE.OPERATOR.eq(taskQuery.getOperator()));
        }
        if (StringUtils.isNotBlank(taskQuery.getTaskName())) {
            conditions.add(TABLE.NAME.like("%" + taskQuery.getTaskName() + "%"));
        }
        if (taskQuery.getStatus() != null) {
            conditions.add(TABLE.STATUS.eq(JooqDataTypeUtil.getByteFromInteger(taskQuery.getStatus().getValue())));
        }
        if (CollectionUtils.isNotEmpty(taskQuery.getStartupModes())) {
            if (taskQuery.getStartupModes().size() == 0) {
                conditions.add(TABLE.STARTUP_MODE.eq(JooqDataTypeUtil.getByteFromInteger(taskQuery.getStartupModes().get(0).getValue())));
            } else {
                conditions.add(TABLE.STARTUP_MODE.in(taskQuery.getStartupModeValues()));
            }
        }
        if (taskQuery.getTaskType() != null) {
            conditions.add(TABLE.TYPE.eq(JooqDataTypeUtil.getByteFromInteger(taskQuery.getTaskType().getValue())));
        }
        if (taskQuery.getStartTime() != null) {
            conditions.add(TABLE.CREATE_TIME.ge(taskQuery.getStartTime()));
        }
        if (taskQuery.getEndTime() != null) {
            conditions.add(TABLE.CREATE_TIME.le(taskQuery.getEndTime()));
        }
        if (taskQuery.getMinTotalTimeMills() != null) {
            conditions.add(TABLE.TOTAL_TIME.greaterThan(taskQuery.getMinTotalTimeMills()));
        }
        if (taskQuery.getMaxTotalTimeMills() != null) {
            conditions.add(TABLE.TOTAL_TIME.lessOrEqual(taskQuery.getMaxTotalTimeMills()));
        }
        if (taskQuery.getCronTaskId() != null && taskQuery.getCronTaskId() > 0) {
            conditions.add(TABLE.CRON_TASK_ID.eq(taskQuery.getCronTaskId()));
        }
        return conditions;
    }

    @Override
    public void addCallbackUrl(long taskInstanceId, String callBackUrl) {
        ctx.update(TABLE).set(TABLE.CALLBACK_URL, callBackUrl)
            .where(TABLE.ID.eq(taskInstanceId))
            .execute();
    }

    @Override
    public List<TaskInstanceDTO> listLatestCronTaskInstance(long appId, Long cronTaskId, Long latestTimeInSeconds,
                                                            RunStatusEnum status, Integer limit) {
        TaskInstance TABLE = TaskInstance.TASK_INSTANCE;
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.eq(appId));
        conditions.add(TABLE.CRON_TASK_ID.eq(cronTaskId));
        if (latestTimeInSeconds != null) {
            long fromTimeInMillSecond = Instant.now().minusMillis(1000 * latestTimeInSeconds).toEpochMilli();
            conditions.add(TABLE.CREATE_TIME.ge(fromTimeInMillSecond));
        }
        if (status != null) {
            conditions.add(TABLE.STATUS.eq(status.getValue().byteValue()));
        }

        SelectSeekStep1 select = ctx.select(TABLE.ID, TABLE.TASK_ID, TABLE.CRON_TASK_ID, TABLE.TASK_TEMPLATE_ID,
            TABLE.IS_DEBUG_TASK, TABLE.APP_ID, TABLE.NAME, TABLE.OPERATOR, TABLE.STARTUP_MODE, TABLE.CURRENT_STEP_ID,
            TABLE.STATUS, TABLE.START_TIME, TABLE.END_TIME, TABLE.TOTAL_TIME, TABLE.CREATE_TIME,
            TABLE.CALLBACK_URL, TABLE.CALLBACK, TABLE.TYPE, TABLE.APP_CODE)
            .from(TABLE)
            .where(conditions)
            .orderBy(TABLE.CREATE_TIME.desc());
        if(log.isDebugEnabled()) {
            log.debug("SQL=", select.getSQL(ParamType.INLINED));
        }
        Result result;
        if (limit != null && limit > 0) {
            result = select.limit(0, limit.intValue()).fetch();
        } else {
            result = select.fetch();
        }
        List<TaskInstanceDTO> taskInstances = new ArrayList<>();
        result.into(record -> {
            TaskInstanceDTO taskInstance = extractInfo(record);
            if (taskInstance != null) {
                taskInstances.add(taskInstance);
            }
        });
        return taskInstances;
    }

    @Override
    public void updateTaskExecutionInfo(long taskInstanceId, RunStatusEnum status, Long currentStepId,
                                        Long startTime, Long endTime, Long totalTime) {
        UpdateSetMoreStep<TaskInstanceRecord> updateSetMoreStep = null;
        if (status != null) {
            updateSetMoreStep = ctx.update(TABLE).set(TABLE.STATUS,
                JooqDataTypeUtil.getByteFromInteger(status.getValue()));
        }
        if (currentStepId != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = ctx.update(TABLE).set(TABLE.CURRENT_STEP_ID, currentStepId);
            } else {
                updateSetMoreStep.set(TABLE.CURRENT_STEP_ID, currentStepId);
            }
        }
        if (startTime != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = ctx.update(TABLE).set(TABLE.START_TIME, startTime);
            } else {
                updateSetMoreStep.set(TABLE.START_TIME, startTime);
            }
        }
        if (endTime != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = ctx.update(TABLE).set(TABLE.END_TIME, endTime);
            } else {
                updateSetMoreStep.set(TABLE.END_TIME, endTime);
            }
        }
        if (totalTime != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = ctx.update(TABLE).set(TABLE.TOTAL_TIME, totalTime);
            } else {
                updateSetMoreStep.set(TABLE.TOTAL_TIME, totalTime);
            }
        }
        if (updateSetMoreStep == null) {
            return;
        }
        updateSetMoreStep.where(TABLE.ID.eq(taskInstanceId)).execute();
    }

    @Override
    public void resetTaskExecuteInfoForResume(long taskInstanceId) {
        ctx.update(TABLE)
            .setNull(TABLE.END_TIME)
            .setNull(TABLE.TOTAL_TIME)
            .set(TABLE.STATUS, RunStatusEnum.RUNNING.getValue().byteValue())
            .where(TABLE.ID.eq(taskInstanceId))
            .execute();
    }

    public Integer countTaskInstanceByConditions(Collection<Condition> conditions) {
        return ctx.selectCount().from(TABLE)
            .where(conditions).fetchOne().value1();
    }

    @Override
    public Integer countTaskInstances(Long appId, Long minTotalTime, Long maxTotalTime,
                                      TaskStartupModeEnum taskStartupMode, TaskTypeEnum taskType,
                                      List<Byte> runStatusList, Long fromTime, Long toTime) {
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(TABLE.APP_ID.eq(appId));
        }
        if (taskStartupMode != null) {
            conditions.add(TABLE.STARTUP_MODE.eq((byte) (taskStartupMode.getValue())));
        }
        if (taskType != null) {
            conditions.add(TABLE.TYPE.eq(taskType.getValue().byteValue()));
        }
        if (runStatusList != null) {
            conditions.add(TABLE.STATUS.in(runStatusList));
        }
        if (minTotalTime != null) {
            conditions.add(TABLE.TOTAL_TIME.greaterOrEqual(minTotalTime * 1000));
        }
        if (maxTotalTime != null) {
            conditions.add(TABLE.TOTAL_TIME.lessOrEqual(maxTotalTime * 1000));
        }
        if (fromTime != null) {
            conditions.add(TABLE.CREATE_TIME.greaterOrEqual(fromTime));
        }
        if (toTime != null) {
            conditions.add(TABLE.CREATE_TIME.lessThan(toTime));
        }
        return countTaskInstanceByConditions(conditions);
    }

    @Override
    public List<Long> listTaskInstanceAppId(List<Long> inAppIdList, Long cronTaskId, Long minCreateTime) {
        List<Condition> conditions = new ArrayList<>();
        if (inAppIdList != null) {
            conditions.add(TABLE.APP_ID.in(inAppIdList));
        }
        if (cronTaskId != null) {
            conditions.add(TABLE.CRON_TASK_ID.eq(cronTaskId));
        }
        if (minCreateTime != null) {
            conditions.add(TABLE.CREATE_TIME.greaterOrEqual(minCreateTime));
        }
        Result result = ctx.selectDistinct(TABLE.APP_ID).from(TABLE)
            .where(conditions)
            .fetch();
        List<Long> appIdList = new ArrayList<>();
        result.into(record -> {
            Long appId = record.getValue(TABLE.APP_ID);
            if (appId != null) {
                appIdList.add(appId);
            }
        });
        return appIdList;
    }

    @Override
    public boolean hasExecuteHistory(Long appId, Long cronTaskId, Long fromTime, Long toTime) {
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(TABLE.APP_ID.eq(appId));
        }
        if (cronTaskId != null) {
            conditions.add(TABLE.CRON_TASK_ID.eq(cronTaskId));
        }
        if (fromTime != null) {
            conditions.add(TABLE.CREATE_TIME.greaterOrEqual(fromTime));
        }
        if (toTime != null) {
            conditions.add(TABLE.CREATE_TIME.lessOrEqual(toTime));
        }
        Result<Record1<Long>> result = ctx.select(TABLE.APP_ID).from(TABLE)
            .where(conditions)
            .limit(1)
            .fetch();
        return result.size() > 0;
    }

    @Override
    public List<Long> listTaskInstanceId(Long appId, Long fromTime, Long toTime, int offset, int limit) {
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(TABLE.APP_ID.eq(appId));
        }
        if (fromTime != null) {
            conditions.add(TABLE.CREATE_TIME.greaterOrEqual(fromTime));
        }
        if (toTime != null) {
            conditions.add(TABLE.CREATE_TIME.lessThan(toTime));
        }
        Result<Record1<Long>> result = ctx.select(TABLE.ID).from(TABLE)
            .where(conditions)
            .limit(offset, limit)
            .fetch();
        List<Long> taskInstanceIdList = new ArrayList<>();
        result.into(record -> {
            taskInstanceIdList.add(record.get(TABLE.ID));
        });
        return taskInstanceIdList;
    }
}
