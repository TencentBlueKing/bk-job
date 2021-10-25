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

package com.tencent.bk.job.crontab.dao.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.UserRoleInfoDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.crontab.dao.CronJobDAO;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.model.dto.CronJobVariableDTO;
import com.tencent.bk.job.crontab.util.DbRecordMapper;
import com.tencent.bk.job.crontab.util.DbUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.jooq.Record1;
import org.jooq.Record21;
import org.jooq.Record5;
import org.jooq.Result;
import org.jooq.TableField;
import org.jooq.UpdateSetMoreStep;
import org.jooq.generated.tables.CronJob;
import org.jooq.generated.tables.records.CronJobRecord;
import org.jooq.types.UByte;
import org.jooq.types.UInteger;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 23/12/2019 17:32
 */
@Slf4j
@Repository
public class CronJobDAOImpl implements CronJobDAO {
    private static final CronJob TABLE = CronJob.CRON_JOB;

    private final DSLContext context;

    @Autowired
    public CronJobDAOImpl(@Qualifier("job-crontab-dsl-context") DSLContext context) {
        this.context = context;
    }

    private static OrderField<?> buildOrderField(TableField<CronJobRecord, ?> field, Integer order) {
        switch (order) {
            case 0:
                return field.desc();
            case 1:
            default:
                return field.asc();
        }
    }

    @Override
    public PageData<CronJobInfoDTO> listPageCronJobsByCondition(CronJobInfoDTO cronJobCondition,
                                                                BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = buildConditionList(cronJobCondition, baseSearchCondition);
        long templateCount = getPageCronJobCount(conditions);
        List<OrderField<?>> orderFields = new ArrayList<>();
        if (StringUtils.isBlank(baseSearchCondition.getOrderField())) {
            orderFields.add(TABLE.IS_ENABLE.desc());
            orderFields.add(TABLE.LAST_MODIFY_TIME.desc());
        } else {
            if (TABLE.NAME.getName().equals(baseSearchCondition.getOrderField())) {
                orderFields.add(buildOrderField(TABLE.NAME, baseSearchCondition.getOrder()));
            } else if (TABLE.CREATOR.getName().equals(baseSearchCondition.getOrderField())) {
                orderFields.add(buildOrderField(TABLE.CREATOR, baseSearchCondition.getOrder()));
            } else if (TABLE.LAST_MODIFY_TIME.getName().equals(baseSearchCondition.getOrderField())) {
                orderFields.add(buildOrderField(TABLE.LAST_MODIFY_TIME, baseSearchCondition.getOrder()));
            } else if (TABLE.LAST_MODIFY_USER.getName().equals(baseSearchCondition.getOrderField())) {
                orderFields.add(buildOrderField(TABLE.LAST_MODIFY_USER, baseSearchCondition.getOrder()));
            } else if (TABLE.IS_ENABLE.getName().equals(baseSearchCondition.getOrderField())) {
                orderFields.add(buildOrderField(TABLE.IS_ENABLE, baseSearchCondition.getOrder()));
            } else {
                orderFields.add(buildOrderField(TABLE.LAST_MODIFY_TIME, baseSearchCondition.getOrder()));
            }
        }
        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);

        Result<Record21<ULong, ULong, String, String, ULong, ULong, String, ULong, String, ULong, String, UByte, UByte,
            UByte, ULong, String, ULong, ULong, ULong, String, String>> records =
            context
                .select(TABLE.ID, TABLE.APP_ID, TABLE.NAME, TABLE.CREATOR, TABLE.TASK_TEMPLATE_ID,
                    TABLE.TASK_PLAN_ID, TABLE.SCRIPT_ID, TABLE.SCRIPT_VERSION_ID, TABLE.CRON_EXPRESSION,
                    TABLE.EXECUTE_TIME, TABLE.VARIABLE_VALUE, TABLE.LAST_EXECUTE_STATUS, TABLE.IS_ENABLE,
                    TABLE.IS_DELETED, TABLE.CREATE_TIME, TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME,
                    TABLE.END_TIME, TABLE.NOTIFY_OFFSET, TABLE.NOTIFY_USER, TABLE.NOTIFY_CHANNEL)
                .from(TABLE).where(conditions).orderBy(orderFields).limit(start, length).fetch();

        List<CronJobInfoDTO> cronJobInfoList = new ArrayList<>();
        if (records != null && records.size() >= 1) {
            records.map(record -> cronJobInfoList.add(convertToCronJobDTO(record)));
        }
        PageData<CronJobInfoDTO> cronJobInfoPageData = new PageData<>();
        cronJobInfoPageData.setTotal(templateCount);
        cronJobInfoPageData.setStart(start);
        cronJobInfoPageData.setPageSize(length);
        cronJobInfoPageData.setData(cronJobInfoList);

        return cronJobInfoPageData;
    }

    private List<Condition> buildConditionList(CronJobInfoDTO cronJobCondition,
                                               BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = new ArrayList<>();

        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(cronJobCondition.getAppId())));

        if (cronJobCondition.getId() != null && cronJobCondition.getId() > 0) {
            conditions.add(TABLE.ID.equal(ULong.valueOf(cronJobCondition.getId())));
            return conditions;
        }
        if (cronJobCondition.getTaskPlanId() != null && cronJobCondition.getTaskPlanId() > 0) {
            conditions.add(TABLE.TASK_PLAN_ID.equal(ULong.valueOf(cronJobCondition.getTaskPlanId())));
        }
        if (StringUtils.isNotBlank(cronJobCondition.getName())) {
            conditions.add(TABLE.NAME.like("%" + cronJobCondition.getName() + "%"));
        }
        if (cronJobCondition.getEnable() != null) {
            if (cronJobCondition.getEnable()) {
                conditions.add(TABLE.IS_ENABLE.equal(UByte.valueOf(1)));
            } else {
                conditions.add(TABLE.IS_ENABLE.equal(UByte.valueOf(0)));
            }
        }
        if (StringUtils.isNotBlank(baseSearchCondition.getCreator())) {
            conditions.add(TABLE.CREATOR.equal(baseSearchCondition.getCreator()));
        }
        if (StringUtils.isNotBlank(baseSearchCondition.getLastModifyUser())) {
            conditions.add(TABLE.LAST_MODIFY_USER.equal(baseSearchCondition.getLastModifyUser()));
        }
        return conditions;
    }

    private long getPageCronJobCount(List<Condition> conditions) {
        return context.selectCount().from(TABLE).where(conditions).fetchOne(0, Long.class);
    }

    @Override
    public CronJobInfoDTO getCronJobById(long cronJobId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.equal(ULong.valueOf(cronJobId)));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        return getCronJobByConditions(conditions);
    }

    @Override
    public List<CronJobInfoDTO> getCronJobByIds(List<Long> cronJobIdList) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.in(cronJobIdList.parallelStream().map(ULong::valueOf).collect(Collectors.toList())));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        return fetchData(conditions);
    }

    @Override
    public CronJobInfoDTO getCronJobById(long appId, long cronJobId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.equal(ULong.valueOf(cronJobId)));
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(appId)));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        return getCronJobByConditions(conditions);
    }

    @Override
    public CronJobInfoDTO getCronJobErrorById(long appId, long cronJobId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.equal(ULong.valueOf(cronJobId)));
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(appId)));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        return getCronJobErrorByConditions(conditions);
    }

    private CronJobInfoDTO getCronJobErrorByConditions(Collection<Condition> conditions) {
        Record5<ULong, ULong, UByte, ULong, UInteger> record = context
            .select(TABLE.ID, TABLE.APP_ID, TABLE.LAST_EXECUTE_STATUS, TABLE.LAST_EXECUTE_ERROR_CODE,
                TABLE.LAST_EXECUTE_ERROR_COUNT)
            .from(TABLE).where(conditions).fetchOne();
        return convertToCronJobErrorDTO(record);
    }

    private CronJobInfoDTO getCronJobByConditions(Collection<Condition> conditions) {
        Record21<ULong, ULong, String, String, ULong, ULong, String, ULong, String, ULong, String, UByte, UByte, UByte,
            ULong, String, ULong, ULong, ULong, String,
            String> record = context
            .select(TABLE.ID, TABLE.APP_ID, TABLE.NAME, TABLE.CREATOR, TABLE.TASK_TEMPLATE_ID, TABLE.TASK_PLAN_ID,
                TABLE.SCRIPT_ID, TABLE.SCRIPT_VERSION_ID, TABLE.CRON_EXPRESSION, TABLE.EXECUTE_TIME,
                TABLE.VARIABLE_VALUE, TABLE.LAST_EXECUTE_STATUS, TABLE.IS_ENABLE, TABLE.IS_DELETED,
                TABLE.CREATE_TIME, TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME, TABLE.END_TIME,
                TABLE.NOTIFY_OFFSET, TABLE.NOTIFY_USER, TABLE.NOTIFY_CHANNEL)
            .from(TABLE).where(conditions).fetchOne();
        return convertToCronJobDTO(record);
    }

    @Override
    public long insertCronJob(CronJobInfoDTO cronJob) {
        UByte lastExecuteStatus;
        if (cronJob.getLastExecuteStatus() != null) {
            lastExecuteStatus = UByte.valueOf(cronJob.getLastExecuteStatus());
        } else {
            lastExecuteStatus = UByte.valueOf(0);
        }
        standardizeDynamicGroupId(cronJob.getVariableValue());

        CronJobRecord cronJobRecord = context.insertInto(TABLE)
            .columns(TABLE.APP_ID, TABLE.NAME, TABLE.CREATOR, TABLE.TASK_TEMPLATE_ID, TABLE.TASK_PLAN_ID,
                TABLE.SCRIPT_ID, TABLE.SCRIPT_VERSION_ID, TABLE.CRON_EXPRESSION, TABLE.EXECUTE_TIME,
                TABLE.VARIABLE_VALUE, TABLE.LAST_EXECUTE_STATUS, TABLE.IS_ENABLE, TABLE.IS_DELETED, TABLE.CREATE_TIME,
                TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME, TABLE.END_TIME, TABLE.NOTIFY_OFFSET, TABLE.NOTIFY_USER,
                TABLE.NOTIFY_CHANNEL)
            .values(ULong.valueOf(cronJob.getAppId()), cronJob.getName(), cronJob.getCreator(),
                DbUtils.getJooqLongValue(cronJob.getTaskTemplateId()),
                DbUtils.getJooqLongValue(cronJob.getTaskPlanId()), cronJob.getScriptId(),
                DbUtils.getJooqLongValue(cronJob.getScriptVersionId()), cronJob.getCronExpression(),
                DbUtils.getJooqLongValue(cronJob.getExecuteTime()), JsonUtils.toJson(cronJob.getVariableValue()),
                lastExecuteStatus, DbUtils.getBooleanValue(cronJob.getEnable()),
                DbUtils.getBooleanValue(cronJob.getDelete()), ULong.valueOf(cronJob.getCreateTime()),
                cronJob.getLastModifyUser(), ULong.valueOf(cronJob.getLastModifyTime()),
                ULong.valueOf(cronJob.getEndTime()), ULong.valueOf(cronJob.getNotifyOffset()),
                cronJob.getNotifyUser() == null ? null : JsonUtils.toJson(cronJob.getNotifyUser()),
                cronJob.getNotifyChannel() == null ? null : JsonUtils.toJson(cronJob.getNotifyChannel()))
            .returning(TABLE.ID).fetchOne();
        if (cronJobRecord != null) {
            return cronJobRecord.getId().longValue();
        } else {
            return 0;
        }
    }

    @Override
    public boolean updateCronJobById(CronJobInfoDTO cronJob) {
        standardizeDynamicGroupId(cronJob.getVariableValue());
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.equal(ULong.valueOf(cronJob.getId())));
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(cronJob.getAppId())));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));

        UpdateSetMoreStep<CronJobRecord> updateStep =
            context.update(TABLE).set(TABLE.LAST_MODIFY_USER, cronJob.getLastModifyUser()).set(TABLE.LAST_MODIFY_TIME,
                ULong.valueOf(cronJob.getLastModifyTime()));
        if (StringUtils.isNotBlank(cronJob.getName())) {
            updateStep = updateStep.set(TABLE.NAME, cronJob.getName());
        }
        if (cronJob.getTaskTemplateId() != null && cronJob.getTaskTemplateId() > 0) {
            updateStep = updateStep.set(TABLE.TASK_TEMPLATE_ID, ULong.valueOf(cronJob.getTaskTemplateId()));
        }
        if (cronJob.getTaskPlanId() != null && cronJob.getTaskPlanId() > 0) {
            updateStep = updateStep.set(TABLE.TASK_PLAN_ID, ULong.valueOf(cronJob.getTaskPlanId()));
        }
        if (StringUtils.isNotBlank(cronJob.getScriptId())) {
            updateStep = updateStep.set(TABLE.SCRIPT_ID, cronJob.getScriptId());
        }
        if (cronJob.getScriptVersionId() != null && cronJob.getScriptVersionId() > 0) {
            updateStep = updateStep.set(TABLE.SCRIPT_VERSION_ID, ULong.valueOf(cronJob.getScriptVersionId()));
        }
        boolean hasExpression = false;
        if (StringUtils.isNotBlank(cronJob.getCronExpression())) {
            updateStep = updateStep.set(TABLE.CRON_EXPRESSION, cronJob.getCronExpression());
            hasExpression = true;
        }
        boolean hasExecuteTime = false;
        if (cronJob.getExecuteTime() != null) {
            updateStep = updateStep.set(TABLE.EXECUTE_TIME, ULong.valueOf(cronJob.getExecuteTime()));
            hasExecuteTime = true;
        }
        if (hasExpression) {
            updateStep.setNull(TABLE.EXECUTE_TIME);
        } else if (hasExecuteTime) {
            updateStep.setNull(TABLE.CRON_EXPRESSION);
        }
        if (CollectionUtils.isNotEmpty(cronJob.getVariableValue())) {
            updateStep = updateStep.set(TABLE.VARIABLE_VALUE, JsonUtils.toJson(cronJob.getVariableValue()));
        }
        if (cronJob.getLastExecuteStatus() != null) {
            updateStep = updateStep.set(TABLE.LAST_EXECUTE_STATUS, UByte.valueOf(cronJob.getLastExecuteStatus()));
        }
        if (cronJob.getEnable() != null) {
            updateStep = updateStep.set(TABLE.IS_ENABLE, DbUtils.getBooleanValue(cronJob.getEnable()));
        }
        if (cronJob.getDelete() != null) {
            updateStep = updateStep.set(TABLE.IS_DELETED, DbUtils.getBooleanValue(cronJob.getDelete()));
        }
        if (StringUtils.isNotBlank(cronJob.getLastModifyUser())) {
            updateStep = updateStep.set(TABLE.LAST_MODIFY_USER, cronJob.getLastModifyUser());
        }
        if (cronJob.getLastModifyTime() != null) {
            updateStep = updateStep.set(TABLE.LAST_MODIFY_TIME, ULong.valueOf(cronJob.getLastModifyTime()));
        }
        if (cronJob.getEndTime() != null) {
            updateStep = updateStep.set(TABLE.END_TIME, ULong.valueOf(cronJob.getEndTime()));
        }
        if (cronJob.getNotifyOffset() != null) {
            updateStep = updateStep.set(TABLE.NOTIFY_OFFSET, ULong.valueOf(cronJob.getNotifyOffset()));
        }
        if (cronJob.getNotifyUser() != null) {
            updateStep = updateStep.set(TABLE.NOTIFY_USER, JsonUtils.toJson(cronJob.getNotifyUser()));
        }
        if (cronJob.getNotifyChannel() != null) {
            updateStep = updateStep.set(TABLE.NOTIFY_CHANNEL, JsonUtils.toJson(cronJob.getNotifyChannel()));
        }

        return 1 == updateStep.where(conditions).limit(1).execute();
    }

    @Override
    public boolean updateCronJobErrorById(CronJobInfoDTO cronJobErrorInfo) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.equal(ULong.valueOf(cronJobErrorInfo.getId())));
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(cronJobErrorInfo.getAppId())));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));

        UpdateSetMoreStep<CronJobRecord> updateStep =
            context.update(TABLE).set(TABLE.LAST_EXECUTE_STATUS, UByte.valueOf(cronJobErrorInfo.getLastExecuteStatus()));
        if (cronJobErrorInfo.getLastExecuteErrorCode() != null) {
            updateStep = updateStep.set(TABLE.LAST_EXECUTE_ERROR_CODE, ULong.valueOf(cronJobErrorInfo.getLastExecuteErrorCode()));
        }
        if (cronJobErrorInfo.getLastExecuteErrorCount() != null) {
            updateStep = updateStep.set(TABLE.LAST_EXECUTE_ERROR_COUNT, UInteger.valueOf(cronJobErrorInfo.getLastExecuteErrorCount()));
        }
        return 1 == updateStep.where(conditions).limit(1).execute();
    }

    @Override
    public boolean deleteCronJobById(long appId, long cronJobId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.equal(ULong.valueOf(cronJobId)));
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(appId)));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        return context.update(TABLE).set(TABLE.IS_DELETED, UByte.valueOf(1)).where(conditions).limit(1).execute() == 1;
    }

    @Override
    public boolean checkCronJobName(long appId, long cronJobId, String name) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(appId)));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        if (cronJobId > 0) {
            conditions.add(TABLE.ID.notEqual(ULong.valueOf(cronJobId)));
        }
        conditions.add(TABLE.NAME.equal(name));
        return context.selectCount().from(TABLE).where(conditions).fetchOne(0, Integer.class) == 0;
    }

    @Override
    public List<CronJobInfoDTO> listCronJobByPlanId(long appId, long planId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.eq(DbRecordMapper.getJooqLongValue(appId)));
        conditions.add(TABLE.TASK_PLAN_ID.eq(DbRecordMapper.getJooqLongValue(planId)));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        return fetchData(conditions);
    }

    @Override
    public boolean insertCronJobWithId(CronJobInfoDTO cronJob) {
        standardizeDynamicGroupId(cronJob.getVariableValue());
        UByte lastExecuteStatus = UByte.valueOf(0);
        return context.insertInto(TABLE)
            .columns(TABLE.ID, TABLE.APP_ID, TABLE.NAME, TABLE.CREATOR, TABLE.TASK_TEMPLATE_ID, TABLE.TASK_PLAN_ID,
                TABLE.SCRIPT_ID, TABLE.SCRIPT_VERSION_ID, TABLE.CRON_EXPRESSION, TABLE.EXECUTE_TIME,
                TABLE.VARIABLE_VALUE, TABLE.LAST_EXECUTE_STATUS, TABLE.IS_ENABLE, TABLE.IS_DELETED, TABLE.CREATE_TIME,
                TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME, TABLE.END_TIME, TABLE.NOTIFY_OFFSET, TABLE.NOTIFY_USER,
                TABLE.NOTIFY_CHANNEL)
            .values(ULong.valueOf(cronJob.getId()), ULong.valueOf(cronJob.getAppId()), cronJob.getName(),
                cronJob.getCreator(), DbUtils.getJooqLongValue(cronJob.getTaskTemplateId()),
                DbUtils.getJooqLongValue(cronJob.getTaskPlanId()), cronJob.getScriptId(),
                DbUtils.getJooqLongValue(cronJob.getScriptVersionId()), cronJob.getCronExpression(),
                DbUtils.getJooqLongValue(cronJob.getExecuteTime()), JsonUtils.toJson(cronJob.getVariableValue()),
                lastExecuteStatus, DbUtils.getBooleanValue(cronJob.getEnable()),
                DbUtils.getBooleanValue(cronJob.getDelete()), ULong.valueOf(cronJob.getCreateTime()),
                cronJob.getLastModifyUser(), ULong.valueOf(cronJob.getLastModifyTime()),
                ULong.valueOf(cronJob.getEndTime()), ULong.valueOf(cronJob.getNotifyOffset()),
                cronJob.getNotifyUser() == null ? null : JsonUtils.toJson(cronJob.getNotifyUser()),
                cronJob.getNotifyChannel() == null ? null : JsonUtils.toJson(cronJob.getNotifyChannel()))
            .execute() == 1;
    }

    @Override
    public String getCronJobNameById(long id) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.equal(ULong.valueOf(id)));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        Record1<String> record = context.select(TABLE.NAME).from(TABLE).where(conditions).fetchOne();
        if (record != null) {
            return record.get(TABLE.NAME);
        } else {
            return null;
        }
    }

    @Override
    public List<CronJobInfoDTO> listCronJobByIds(long appId, List<Long> cronJobIdList) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.eq(DbRecordMapper.getJooqLongValue(appId)));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        conditions.add(TABLE.ID.in(cronJobIdList.parallelStream().map(ULong::valueOf).collect(Collectors.toList())));

        return fetchData(conditions);
    }

    @Override
    public boolean isExistAnyAppCronJob(Long appId) {
        List<Condition> conditions = new ArrayList<>(2);
        conditions.add(TABLE.APP_ID.eq(ULong.valueOf(appId)));
        conditions.add(TABLE.IS_DELETED.eq(UByte.valueOf(0)));
        return context.fetchExists(TABLE, conditions);
    }

    private List<CronJobInfoDTO> fetchData(Collection<Condition> conditions) {
        Result<Record21<ULong, ULong, String, String, ULong, ULong, String, ULong, String, ULong, String, UByte, UByte,
            UByte, ULong, String, ULong, ULong, ULong, String, String>> records =
            context
                .select(TABLE.ID, TABLE.APP_ID, TABLE.NAME, TABLE.CREATOR, TABLE.TASK_TEMPLATE_ID,
                    TABLE.TASK_PLAN_ID, TABLE.SCRIPT_ID, TABLE.SCRIPT_VERSION_ID, TABLE.CRON_EXPRESSION,
                    TABLE.EXECUTE_TIME, TABLE.VARIABLE_VALUE, TABLE.LAST_EXECUTE_STATUS, TABLE.IS_ENABLE,
                    TABLE.IS_DELETED, TABLE.CREATE_TIME, TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME,
                    TABLE.END_TIME, TABLE.NOTIFY_OFFSET, TABLE.NOTIFY_USER, TABLE.NOTIFY_CHANNEL)
                .from(TABLE).where(conditions).fetch();

        List<CronJobInfoDTO> cronJobInfoList = new ArrayList<>();
        if (records.size() >= 1) {
            records.map(record -> cronJobInfoList.add(convertToCronJobDTO(record)));
        }
        return cronJobInfoList;
    }

    @Override
    public Integer countCronJob(Long appId, Boolean active, Boolean cron) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.IS_DELETED.eq(UByte.valueOf(0)));
        if (appId != null) {
            conditions.add(TABLE.APP_ID.eq(ULong.valueOf(appId)));
        }
        if (active != null) {
            conditions.add(TABLE.IS_ENABLE.eq(UByte.valueOf(active ? 1 : 0)));
        }
        if (cron != null) {
            if (cron) {
                conditions.add(TABLE.CRON_EXPRESSION.isNotNull());
            } else {
                conditions.add(TABLE.EXECUTE_TIME.isNotNull().and(TABLE.CRON_EXPRESSION.isNull()));
            }
        }
        return context.selectCount().from(TABLE)
            .where(conditions).fetchOne().value1();
    }

    private CronJobInfoDTO convertToCronJobDTO(Record21<ULong, ULong, String, String, ULong, ULong, String, ULong,
        String, ULong, String, UByte, UByte, UByte, ULong, String, ULong, ULong, ULong, String, String> record) {
        if (record == null) {
            return null;
        }
        CronJobInfoDTO cronJobInfoDTO = new CronJobInfoDTO();
        cronJobInfoDTO.setId(record.get(TABLE.ID).longValue());
        cronJobInfoDTO.setAppId(record.get(TABLE.APP_ID).longValue());
        cronJobInfoDTO.setName(record.get(TABLE.NAME));
        cronJobInfoDTO.setCreator(record.get(TABLE.CREATOR));
        if (record.get(TABLE.TASK_TEMPLATE_ID) != null) {
            cronJobInfoDTO.setTaskTemplateId(record.get(TABLE.TASK_TEMPLATE_ID).longValue());
        }
        if (record.get(TABLE.TASK_PLAN_ID) != null) {
            cronJobInfoDTO.setTaskPlanId(record.get(TABLE.TASK_PLAN_ID).longValue());
        }
        cronJobInfoDTO.setScriptId(record.get(TABLE.SCRIPT_ID));
        if (record.get(TABLE.SCRIPT_VERSION_ID) != null) {
            cronJobInfoDTO.setScriptVersionId(record.get(TABLE.SCRIPT_VERSION_ID).longValue());
        }
        cronJobInfoDTO.setCronExpression(record.get(TABLE.CRON_EXPRESSION));
        cronJobInfoDTO.setExecuteTime(DbUtils.convertJooqLongValue(record.get(TABLE.EXECUTE_TIME)));
        cronJobInfoDTO.setVariableValue(
            JsonUtils.fromJson(record.get(TABLE.VARIABLE_VALUE), new TypeReference<List<CronJobVariableDTO>>() {
            }));
        standardizeDynamicGroupId(cronJobInfoDTO.getVariableValue());
        cronJobInfoDTO.setLastExecuteStatus(record.get(TABLE.LAST_EXECUTE_STATUS).intValue());
        cronJobInfoDTO.setEnable(record.get(TABLE.IS_ENABLE).intValue() == 1);
        cronJobInfoDTO.setDelete(record.get(TABLE.IS_DELETED).intValue() == 1);
        cronJobInfoDTO.setCreateTime(record.get(TABLE.CREATE_TIME).longValue());
        cronJobInfoDTO.setLastModifyUser(record.get(TABLE.LAST_MODIFY_USER));
        cronJobInfoDTO.setLastModifyTime(record.get(TABLE.LAST_MODIFY_TIME).longValue());
        cronJobInfoDTO.setEndTime(record.get(TABLE.END_TIME).longValue());
        cronJobInfoDTO.setNotifyOffset(record.get(TABLE.NOTIFY_OFFSET).longValue());
        if (record.get(TABLE.NOTIFY_USER) != null) {
            cronJobInfoDTO.setNotifyUser(JsonUtils.fromJson(record.get(TABLE.NOTIFY_USER), UserRoleInfoDTO.class));
        } else {
            cronJobInfoDTO.setNotifyUser(new UserRoleInfoDTO());
        }
        if (record.get(TABLE.NOTIFY_CHANNEL) != null) {
            cronJobInfoDTO.setNotifyChannel(
                JsonUtils.fromJson(record.get(TABLE.NOTIFY_CHANNEL), new TypeReference<List<String>>() {
                }));
        } else {
            cronJobInfoDTO.setNotifyChannel(Collections.emptyList());
        }
        return cronJobInfoDTO;
    }

    private CronJobInfoDTO convertToCronJobErrorDTO(Record5<ULong, ULong, UByte, ULong, UInteger> record) {
        if (record == null) {
            return null;
        }
        CronJobInfoDTO cronJobInfoDTO = new CronJobInfoDTO();
        cronJobInfoDTO.setId(record.get(TABLE.ID).longValue());
        cronJobInfoDTO.setAppId(record.get(TABLE.APP_ID).longValue());
        cronJobInfoDTO.setLastExecuteStatus(record.get(TABLE.LAST_EXECUTE_STATUS).intValue());
        cronJobInfoDTO.setLastExecuteErrorCode(DbRecordMapper.getLongValue(record.get(TABLE.LAST_EXECUTE_ERROR_CODE)));
        cronJobInfoDTO.setLastExecuteErrorCount(record.get(TABLE.LAST_EXECUTE_ERROR_COUNT).intValue());
        return cronJobInfoDTO;
    }

    private void standardizeDynamicGroupId(List<CronJobVariableDTO> variables) {
        if (CollectionUtils.isNotEmpty(variables)) {
            // 移除动态分组ID中多余的appId(历史问题)
            variables.stream().filter(variable -> variable.getServer() != null)
                .forEach(variable -> variable.getServer().standardizeDynamicGroupId());
        }
    }
}
