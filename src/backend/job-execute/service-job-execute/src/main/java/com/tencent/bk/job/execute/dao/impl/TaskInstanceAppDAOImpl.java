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

import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.mysql.dynamic.ds.DbOperationEnum;
import com.tencent.bk.job.common.mysql.dynamic.ds.MySQLOperation;
import com.tencent.bk.job.common.mysql.jooq.JooqDataTypeUtil;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.dao.TaskInstanceAppDAO;
import com.tencent.bk.job.execute.dao.common.DSLContextProviderFactory;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceQuery;
import com.tencent.bk.job.execute.model.tables.TaskInstanceApp;
import com.tencent.bk.job.execute.model.tables.TaskInstanceHost;
import com.tencent.bk.job.execute.model.tables.records.TaskInstanceAppRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.SelectSeekStep1;
import org.jooq.SortField;
import org.jooq.TableField;
import org.jooq.UpdateSetMoreStep;
import org.jooq.conf.ParamType;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Repository
@Slf4j
public class TaskInstanceAppDAOImpl extends BaseDAO implements TaskInstanceAppDAO {
    private static final TaskInstanceApp TASK_INSTANCE_APP = TaskInstanceApp.TASK_INSTANCE_APP;
    private static final TaskInstanceHost TASK_INSTANCE_HOST = TaskInstanceHost.TASK_INSTANCE_HOST;

    private static final TableField<?, ?>[] ALL_FIELDS = {
        TASK_INSTANCE_APP.ID,
        TASK_INSTANCE_APP.TASK_ID,
        TASK_INSTANCE_APP.CRON_TASK_ID,
        TASK_INSTANCE_APP.TASK_TEMPLATE_ID,
        TASK_INSTANCE_APP.IS_DEBUG_TASK,
        TASK_INSTANCE_APP.APP_ID,
        TASK_INSTANCE_APP.NAME,
        TASK_INSTANCE_APP.OPERATOR,
        TASK_INSTANCE_APP.STARTUP_MODE,
        TASK_INSTANCE_APP.CURRENT_STEP_ID,
        TASK_INSTANCE_APP.STATUS,
        TASK_INSTANCE_APP.START_TIME,
        TASK_INSTANCE_APP.END_TIME,
        TASK_INSTANCE_APP.TOTAL_TIME,
        TASK_INSTANCE_APP.CREATE_TIME,
        TASK_INSTANCE_APP.CALLBACK_URL,
        TASK_INSTANCE_APP.TYPE,
        TASK_INSTANCE_APP.APP_CODE
    };

    public TaskInstanceAppDAOImpl(DSLContextProviderFactory dslContextProviderFactory) {
        super(dslContextProviderFactory, TASK_INSTANCE_APP.getName());
    }

    @Override
    @MySQLOperation(table = "task_instance_app", op = DbOperationEnum.WRITE)
    public Long addTaskInstance(TaskInstanceDTO taskInstance) {
        dsl().insertInto(
                TASK_INSTANCE_APP,
                TASK_INSTANCE_APP.ID,
                TASK_INSTANCE_APP.TASK_ID,
                TASK_INSTANCE_APP.CRON_TASK_ID,
                TASK_INSTANCE_APP.TASK_TEMPLATE_ID,
                TASK_INSTANCE_APP.IS_DEBUG_TASK,
                TASK_INSTANCE_APP.APP_ID,
                TASK_INSTANCE_APP.NAME,
                TASK_INSTANCE_APP.OPERATOR,
                TASK_INSTANCE_APP.STARTUP_MODE,
                TASK_INSTANCE_APP.CURRENT_STEP_ID,
                TASK_INSTANCE_APP.STATUS,
                TASK_INSTANCE_APP.START_TIME,
                TASK_INSTANCE_APP.END_TIME,
                TASK_INSTANCE_APP.TOTAL_TIME,
                TASK_INSTANCE_APP.CREATE_TIME,
                TASK_INSTANCE_APP.CALLBACK_URL,
                TASK_INSTANCE_APP.TYPE,
                TASK_INSTANCE_APP.APP_CODE)
            .values(
                taskInstance.getId(),
                taskInstance.getPlanId(),
                taskInstance.getCronTaskId(),
                taskInstance.getTaskTemplateId(),
                taskInstance.isDebugTask() ? (byte) 1 : (byte) 0,
                taskInstance.getAppId(),
                taskInstance.getName(),
                taskInstance.getOperator(),
                JooqDataTypeUtil.toByte(taskInstance.getStartupMode()),
                taskInstance.getCurrentStepInstanceId(),
                taskInstance.getStatus().getValue().byteValue(),
                taskInstance.getStartTime(),
                taskInstance.getEndTime(),
                taskInstance.getTotalTime(),
                taskInstance.getCreateTime(),
                taskInstance.getCallbackUrl(),
                JooqDataTypeUtil.toByte(taskInstance.getType()),
                taskInstance.getAppCode())
            .execute();
        return taskInstance.getId();
    }

    private TaskInstanceDTO extractInfo(Record record) {
        if (record == null) {
            return null;
        }
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setId(record.get(TASK_INSTANCE_APP.ID));
        taskInstance.setPlanId(record.get(TASK_INSTANCE_APP.TASK_ID));
        taskInstance.setCronTaskId(record.get(TASK_INSTANCE_APP.CRON_TASK_ID));
        taskInstance.setTaskTemplateId(record.get(TASK_INSTANCE_APP.TASK_TEMPLATE_ID));
        taskInstance.setDebugTask(record.get(TASK_INSTANCE_APP.IS_DEBUG_TASK) == 1);
        taskInstance.setAppId(record.get(TASK_INSTANCE_APP.APP_ID));
        taskInstance.setName(record.get(TASK_INSTANCE_APP.NAME));
        taskInstance.setType(JooqDataTypeUtil.toInteger(record.get(TASK_INSTANCE_APP.TYPE)));
        taskInstance.setOperator(record.get(TASK_INSTANCE_APP.OPERATOR));
        taskInstance.setStartupMode(JooqDataTypeUtil.toInteger(record.get(TASK_INSTANCE_APP.STARTUP_MODE)));
        taskInstance.setCurrentStepInstanceId(record.get(TASK_INSTANCE_APP.CURRENT_STEP_ID));
        taskInstance.setStatus(RunStatusEnum.valueOf(record.get(TASK_INSTANCE_APP.STATUS)));
        taskInstance.setStartTime(record.get(TASK_INSTANCE_APP.START_TIME));
        taskInstance.setEndTime(record.get(TASK_INSTANCE_APP.END_TIME));
        taskInstance.setTotalTime(record.get(TASK_INSTANCE_APP.TOTAL_TIME));
        taskInstance.setCreateTime(record.get(TASK_INSTANCE_APP.CREATE_TIME));
        taskInstance.setCallbackUrl(record.get(TASK_INSTANCE_APP.CALLBACK_URL));
        taskInstance.setAppCode(record.get(TASK_INSTANCE_APP.APP_CODE));
        return taskInstance;
    }

    @Override
    @MySQLOperation(table = "task_instance_app", op = DbOperationEnum.WRITE)
    public void updateTaskStatus(long appId, long taskInstanceId, int status) {
        dsl().update(TASK_INSTANCE_APP)
            .set(TASK_INSTANCE_APP.STATUS, Byte.valueOf(String.valueOf(status)))
            .where(TASK_INSTANCE_APP.ID.eq(taskInstanceId))
            .and(TASK_INSTANCE_APP.APP_ID.eq(appId))
            .execute();
    }


    @Override
    @MySQLOperation(table = "task_instance_app", op = DbOperationEnum.WRITE)
    public void updateTaskCurrentStepId(long appId, Long taskInstanceId, Long stepInstanceId) {
        dsl().update(TASK_INSTANCE_APP)
            .set(TASK_INSTANCE_APP.CURRENT_STEP_ID, stepInstanceId)
            .where(TASK_INSTANCE_APP.ID.eq(taskInstanceId))
            .and(TASK_INSTANCE_APP.APP_ID.eq(appId))
            .execute();
    }

    @Override
    @MySQLOperation(table = "task_instance_app", op = DbOperationEnum.WRITE)
    public void resetTaskStatus(long appId, Long taskInstanceId) {
        dsl().update(TASK_INSTANCE_APP)
            .setNull(TASK_INSTANCE_APP.START_TIME)
            .setNull(TASK_INSTANCE_APP.END_TIME)
            .setNull(TASK_INSTANCE_APP.TOTAL_TIME)
            .setNull(TASK_INSTANCE_APP.CURRENT_STEP_ID)
            .set(TASK_INSTANCE_APP.STATUS, JooqDataTypeUtil.toByte(RunStatusEnum.BLANK.getValue()))
            .where(TASK_INSTANCE_APP.ID.eq(taskInstanceId))
            .and(TASK_INSTANCE_APP.APP_ID.eq(appId))
            .execute();
    }

    @Override
    @MySQLOperation(table = "task_instance_app", op = DbOperationEnum.READ)
    public PageData<TaskInstanceDTO> listPageTaskInstance(TaskInstanceQuery taskQuery,
                                                          BaseSearchCondition baseSearchCondition) {
        if (StringUtils.isNotEmpty(taskQuery.getIp()) || StringUtils.isNotEmpty(taskQuery.getIpv6())) {
            return listPageTaskInstanceByIp(taskQuery, baseSearchCondition);
        } else {
            return listPageTaskInstanceByBasicInfo(taskQuery, baseSearchCondition);
        }
    }

    @MySQLOperation(table = "task_instance_app", op = DbOperationEnum.READ)
    private PageData<TaskInstanceDTO> listPageTaskInstanceByBasicInfo(TaskInstanceQuery taskQuery,
                                                                      BaseSearchCondition baseSearchCondition) {
        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);

        int count = 0;
        if (baseSearchCondition.isCountPageTotal()) {
            count = getPageTaskInstanceCount(taskQuery);
            if (count == 0) {
                return PageData.emptyPageData(start, length);
            }
        }

        Collection<SortField<?>> orderFields = new ArrayList<>();
        orderFields.add(TASK_INSTANCE_APP.CREATE_TIME.desc());
        Result<?> result = dsl().select(ALL_FIELDS)
            .from(TASK_INSTANCE_APP)
            .where(buildSearchCondition(taskQuery))
            .orderBy(orderFields)
            .limit(start, length)
            .fetch();
        return buildTaskInstancePageData(start, length, count, result);
    }

    private PageData<TaskInstanceDTO> listPageTaskInstanceByIp(TaskInstanceQuery taskQuery,
                                                               BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = buildSearchCondition(taskQuery);
        if (StringUtils.isNotEmpty(taskQuery.getIp())) {
            conditions.add(TASK_INSTANCE_HOST.IP.eq(taskQuery.getIp()));
        } else {
            conditions.add(TASK_INSTANCE_HOST.IPV6.eq(taskQuery.getIpv6()));
        }
        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);
        Integer count = 0;
        if (baseSearchCondition.isCountPageTotal()) {
            count = dsl().selectCount().from(TASK_INSTANCE_APP)
                .leftJoin(TASK_INSTANCE_HOST).on(TASK_INSTANCE_APP.ID.eq(TASK_INSTANCE_HOST.TASK_INSTANCE_ID))
                .where(conditions)
                .fetchOne(0, Integer.class);
            if (count == null || count == 0) {
                return PageData.emptyPageData(start, length);
            }
        }
        Collection<SortField<?>> orderFields = new ArrayList<>();
        orderFields.add(TASK_INSTANCE_APP.ID.desc());
        Result<? extends Record> result = dsl().select(ALL_FIELDS)
            .from(TASK_INSTANCE_APP)
            .leftJoin(TASK_INSTANCE_HOST).on(TASK_INSTANCE_APP.ID.eq(TASK_INSTANCE_HOST.TASK_INSTANCE_ID))
            .where(conditions)
            .groupBy(TASK_INSTANCE_APP.ID)
            .orderBy(orderFields)
            .limit(start, length)
            .fetch();
        return buildTaskInstancePageData(start, length, count, result);
    }

    private PageData<TaskInstanceDTO> buildTaskInstancePageData(int start,
                                                                int length,
                                                                int count,
                                                                Result<? extends Record> result) {
        List<TaskInstanceDTO> taskInstances = new ArrayList<>();
        if (result != null && !result.isEmpty()) {
            result.forEach(record -> taskInstances.add(extractInfo(record)));
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
        return dsl().selectCount().from(TASK_INSTANCE_APP).where(conditions).fetchOne(0, Integer.class);
    }

    private List<Condition> buildSearchCondition(TaskInstanceQuery taskQuery) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TASK_INSTANCE_APP.APP_ID.eq(taskQuery.getAppId()));
        if (taskQuery.getTaskInstanceId() != null && taskQuery.getTaskInstanceId() > 0) {
            conditions.add(TASK_INSTANCE_APP.ID.eq(taskQuery.getTaskInstanceId()));
            return conditions;
        }
        if (StringUtils.isNotBlank(taskQuery.getOperator())) {
            conditions.add(TASK_INSTANCE_APP.OPERATOR.eq(taskQuery.getOperator()));
        }
        if (StringUtils.isNotBlank(taskQuery.getTaskName())) {
            conditions.add(TASK_INSTANCE_APP.NAME.like("%" + taskQuery.getTaskName() + "%"));
        }
        if (taskQuery.getStatus() != null) {
            conditions.add(TASK_INSTANCE_APP.STATUS.eq(JooqDataTypeUtil.toByte(taskQuery.getStatus().getValue())));
        }
        if (CollectionUtils.isNotEmpty(taskQuery.getStartupModes())) {
            if (taskQuery.getStartupModes().size() == 1) {
                conditions.add(TASK_INSTANCE_APP.STARTUP_MODE.eq(
                    JooqDataTypeUtil.toByte(taskQuery.getStartupModes().get(0).getValue())));
            } else {
                conditions.add(TASK_INSTANCE_APP.STARTUP_MODE.in(taskQuery.getStartupModeValues()));
            }
        }
        if (taskQuery.getTaskType() != null) {
            conditions.add(TASK_INSTANCE_APP.TYPE.eq(JooqDataTypeUtil.toByte(taskQuery.getTaskType().getValue())));
        }
        if (taskQuery.getStartTime() != null) {
            conditions.add(TASK_INSTANCE_APP.CREATE_TIME.ge(taskQuery.getStartTime()));
        }
        if (taskQuery.getEndTime() != null) {
            conditions.add(TASK_INSTANCE_APP.CREATE_TIME.le(taskQuery.getEndTime()));
        }
        if (taskQuery.getMinTotalTimeMills() != null) {
            conditions.add(TASK_INSTANCE_APP.TOTAL_TIME.greaterThan(taskQuery.getMinTotalTimeMills()));
        }
        if (taskQuery.getMaxTotalTimeMills() != null) {
            conditions.add(TASK_INSTANCE_APP.TOTAL_TIME.lessOrEqual(taskQuery.getMaxTotalTimeMills()));
        }
        if (taskQuery.getCronTaskId() != null && taskQuery.getCronTaskId() > 0) {
            conditions.add(TASK_INSTANCE_APP.CRON_TASK_ID.eq(taskQuery.getCronTaskId()));
        }
        return conditions;
    }

    @Override
    @MySQLOperation(table = "task_instance_app", op = DbOperationEnum.READ)
    public List<TaskInstanceDTO> listLatestCronTaskInstance(long appId,
                                                            Long cronTaskId,
                                                            Long latestTimeInSeconds,
                                                            RunStatusEnum status,
                                                            Integer limit) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TASK_INSTANCE_APP.APP_ID.eq(appId));
        conditions.add(TASK_INSTANCE_APP.CRON_TASK_ID.eq(cronTaskId));
        if (latestTimeInSeconds != null) {
            long fromTimeInMillSecond = Instant.now().minusMillis(1000 * latestTimeInSeconds).toEpochMilli();
            conditions.add(TASK_INSTANCE_APP.CREATE_TIME.ge(fromTimeInMillSecond));
        }
        if (status != null) {
            conditions.add(TASK_INSTANCE_APP.STATUS.eq(status.getValue().byteValue()));
        }

        SelectSeekStep1<? extends Record, Long> select = dsl().select(ALL_FIELDS)
            .from(TASK_INSTANCE_APP)
            .where(conditions)
            .orderBy(TASK_INSTANCE_APP.CREATE_TIME.desc());
        if (log.isDebugEnabled()) {
            log.debug("SQL={}", select.getSQL(ParamType.INLINED));
        }
        Result<? extends Record> result;
        if (limit != null && limit > 0) {
            result = select.limit(0, limit.intValue()).fetch();
        } else {
            result = select.fetch();
        }
        List<TaskInstanceDTO> taskInstances = new ArrayList<>();
        result.forEach(record -> {
            TaskInstanceDTO taskInstance = extractInfo(record);
            if (taskInstance != null) {
                taskInstances.add(taskInstance);
            }
        });
        return taskInstances;
    }

    @Override
    @MySQLOperation(table = "task_instance_app", op = DbOperationEnum.WRITE)
    public void updateTaskExecutionInfo(long appId,
                                        long taskInstanceId,
                                        RunStatusEnum status,
                                        Long currentStepId,
                                        Long startTime,
                                        Long endTime,
                                        Long totalTime) {
        UpdateSetMoreStep<TaskInstanceAppRecord> updateSetMoreStep = null;
        if (status != null) {
            updateSetMoreStep = dsl().update(TASK_INSTANCE_APP).set(TASK_INSTANCE_APP.STATUS,
                JooqDataTypeUtil.toByte(status.getValue()));
        }
        if (currentStepId != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = dsl().update(TASK_INSTANCE_APP).set(TASK_INSTANCE_APP.CURRENT_STEP_ID,
                    currentStepId);
            } else {
                updateSetMoreStep.set(TASK_INSTANCE_APP.CURRENT_STEP_ID, currentStepId);
            }
        }
        if (startTime != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = dsl().update(TASK_INSTANCE_APP).set(TASK_INSTANCE_APP.START_TIME, startTime);
            } else {
                updateSetMoreStep.set(TASK_INSTANCE_APP.START_TIME, startTime);
            }
        }
        if (endTime != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = dsl().update(TASK_INSTANCE_APP).set(TASK_INSTANCE_APP.END_TIME, endTime);
            } else {
                updateSetMoreStep.set(TASK_INSTANCE_APP.END_TIME, endTime);
            }
        }
        if (totalTime != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = dsl().update(TASK_INSTANCE_APP).set(TASK_INSTANCE_APP.TOTAL_TIME, totalTime);
            } else {
                updateSetMoreStep.set(TASK_INSTANCE_APP.TOTAL_TIME, totalTime);
            }
        }
        if (updateSetMoreStep == null) {
            return;
        }
        updateSetMoreStep
            .where(TASK_INSTANCE_APP.ID.eq(taskInstanceId))
            .and(TASK_INSTANCE_APP.APP_ID.eq(appId))
            .execute();
    }

    @Override
    @MySQLOperation(table = "task_instance_app", op = DbOperationEnum.WRITE)
    public void resetTaskExecuteInfoForRetry(long appId, long taskInstanceId) {
        dsl().update(TASK_INSTANCE_APP)
            .setNull(TASK_INSTANCE_APP.END_TIME)
            .setNull(TASK_INSTANCE_APP.TOTAL_TIME)
            .set(TASK_INSTANCE_APP.STATUS, RunStatusEnum.RUNNING.getValue().byteValue())
            .where(TASK_INSTANCE_APP.ID.eq(taskInstanceId))
            .and(TASK_INSTANCE_APP.APP_ID.eq(appId))
            .execute();
    }

    @Override
    @MySQLOperation(table = "task_instance_app", op = DbOperationEnum.READ)
    public List<Long> listTaskInstanceAppId(List<Long> inAppIdList, Long cronTaskId, Long minCreateTime) {
        List<Condition> conditions = new ArrayList<>();
        if (inAppIdList != null) {
            conditions.add(TASK_INSTANCE_APP.APP_ID.in(inAppIdList));
        }
        if (cronTaskId != null) {
            conditions.add(TASK_INSTANCE_APP.CRON_TASK_ID.eq(cronTaskId));
        }
        if (minCreateTime != null) {
            conditions.add(TASK_INSTANCE_APP.CREATE_TIME.greaterOrEqual(minCreateTime));
        }
        Result<? extends Record> result = dsl().selectDistinct(TASK_INSTANCE_APP.APP_ID).from(TASK_INSTANCE_APP)
            .where(conditions)
            .fetch();
        List<Long> appIdList = new ArrayList<>();
        result.forEach(record -> {
            Long appId = record.getValue(TASK_INSTANCE_APP.APP_ID);
            if (appId != null) {
                appIdList.add(appId);
            }
        });
        return appIdList;
    }

    @Override
    @MySQLOperation(table = "task_instance_app", op = DbOperationEnum.READ)
    public boolean hasExecuteHistory(Long appId, Long cronTaskId, Long fromTime, Long toTime) {
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(TASK_INSTANCE_APP.APP_ID.eq(appId));
        }
        if (cronTaskId != null) {
            conditions.add(TASK_INSTANCE_APP.CRON_TASK_ID.eq(cronTaskId));
        }
        if (fromTime != null) {
            conditions.add(TASK_INSTANCE_APP.CREATE_TIME.greaterOrEqual(fromTime));
        }
        if (toTime != null) {
            conditions.add(TASK_INSTANCE_APP.CREATE_TIME.lessOrEqual(toTime));
        }
        Result<Record1<Long>> result = dsl().select(TASK_INSTANCE_APP.APP_ID).from(TASK_INSTANCE_APP)
            .where(conditions)
            .limit(1)
            .fetch();
        return !result.isEmpty();
    }

    @Override
    @MySQLOperation(table = "task_instance_app", op = DbOperationEnum.READ)
    public List<Long> listTaskInstanceId(Long appId, Long fromTime, Long toTime, int offset, int limit) {
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(TASK_INSTANCE_APP.APP_ID.eq(appId));
        }
        if (fromTime != null) {
            conditions.add(TASK_INSTANCE_APP.CREATE_TIME.greaterOrEqual(fromTime));
        }
        if (toTime != null) {
            conditions.add(TASK_INSTANCE_APP.CREATE_TIME.lessThan(toTime));
        }
        Result<Record1<Long>> result = dsl().select(TASK_INSTANCE_APP.ID).from(TASK_INSTANCE_APP)
            .where(conditions)
            .limit(offset, limit)
            .fetch();
        List<Long> taskInstanceIdList = new ArrayList<>();
        result.into(record -> {
            taskInstanceIdList.add(record.get(TASK_INSTANCE_APP.ID));
        });
        return taskInstanceIdList;
    }

}
