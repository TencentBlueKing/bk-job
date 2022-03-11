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

package com.tencent.bk.job.manage.dao.plan.impl;

import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.util.json.JsonMapper;
import com.tencent.bk.job.manage.common.consts.task.TaskPlanTypeEnum;
import com.tencent.bk.job.manage.common.util.DbRecordMapper;
import com.tencent.bk.job.manage.dao.plan.TaskPlanDAO;
import com.tencent.bk.job.manage.model.dto.TaskPlanQueryDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.*;
import org.jooq.generated.tables.TaskPlan;
import org.jooq.generated.tables.records.TaskPlanRecord;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 15/11/2019 15:44
 */
@Slf4j
@Repository
public class TaskPlanDAOImpl implements TaskPlanDAO {
    private static final TaskPlan TABLE = TaskPlan.TASK_PLAN;
    private DSLContext context;

    @Autowired
    public TaskPlanDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext context) {
        this.context = context;
    }

    private static OrderField<?> buildBasicOrderField(TableField<TaskPlanRecord, ?> field, Integer order) {
        switch (order) {
            case 0:
                return field.desc();
            case 1:
            default:
                return field.asc();
        }
    }

    public static List<OrderField<?>> buildOrderField(BaseSearchCondition baseSearchCondition) {
        List<OrderField<?>> orderFields = new ArrayList<>();
        if (baseSearchCondition != null) {
            if (StringUtils.isBlank(baseSearchCondition.getOrderField())) {
                orderFields.add(TABLE.LAST_MODIFY_TIME.desc());
            } else {
                if (TABLE.NAME.getName().equals(baseSearchCondition.getOrderField())) {
                    orderFields.add(buildBasicOrderField(TABLE.NAME, baseSearchCondition.getOrder()));
                } else if (TABLE.CREATOR.getName().equals(baseSearchCondition.getOrderField())) {
                    orderFields.add(buildBasicOrderField(TABLE.CREATOR, baseSearchCondition.getOrder()));
                } else if (TABLE.LAST_MODIFY_TIME.getName().equals(baseSearchCondition.getOrderField())) {
                    orderFields.add(buildBasicOrderField(TABLE.LAST_MODIFY_TIME, baseSearchCondition.getOrder()));
                } else if (TABLE.LAST_MODIFY_USER.getName().equals(baseSearchCondition.getOrderField())) {
                    orderFields.add(buildBasicOrderField(TABLE.LAST_MODIFY_USER, baseSearchCondition.getOrder()));
                } else {
                    orderFields.add(buildBasicOrderField(TABLE.LAST_MODIFY_TIME, baseSearchCondition.getOrder()));
                }
            }
        }
        return orderFields;
    }

    @Override
    public List<Long> listTaskPlanIds(Long templateId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.TEMPLATE_ID.eq(ULong.valueOf(templateId)));
        conditions.add(TABLE.IS_DELETED.eq(UByte.valueOf(0)));
        conditions.add(TABLE.TYPE.eq(UByte.valueOf(TaskPlanTypeEnum.NORMAL.getType())));
        Result<Record1<ULong>> result =
            context
                .select(TABLE.ID)
                .from(TABLE).where(conditions).fetch();
        List<Long> taskPlanIdList = new ArrayList<>();
        if (result.size() >= 1) {
            result.map(record -> taskPlanIdList.add(record.get(TABLE.ID).longValue()));
        }
        return taskPlanIdList;
    }

    @Override
    public List<TaskPlanInfoDTO> listTaskPlans(Long appId, Long templateId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.eq(ULong.valueOf(appId)));
        conditions.add(TABLE.TEMPLATE_ID.eq(ULong.valueOf(templateId)));
        conditions.add(TABLE.IS_DELETED.eq(UByte.valueOf(0)));
        conditions.add(TABLE.TYPE.eq(UByte.valueOf(TaskPlanTypeEnum.NORMAL.getType())));
        Result<Record13<ULong, ULong, ULong, UByte, String, String, ULong, String, ULong, ULong, ULong, String,
            UByte>> result =
            context
                .select(TABLE.ID, TABLE.APP_ID, TABLE.TEMPLATE_ID, TABLE.TYPE, TABLE.NAME, TABLE.CREATOR,
                    TABLE.CREATE_TIME, TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME, TABLE.FIRST_STEP_ID,
                    TABLE.LAST_STEP_ID, TABLE.VERSION, TABLE.IS_LATEST_VERSION)
                .from(TABLE).where(conditions).orderBy(TABLE.CREATE_TIME.desc()).fetch();
        List<TaskPlanInfoDTO> taskPlanInfoList = new ArrayList<>();
        if (result != null && result.size() >= 1) {
            result.map(record -> taskPlanInfoList.add(DbRecordMapper.convertRecordToPlanInfo(record)));
        }
        return taskPlanInfoList;
    }

    @Override
    public PageData<TaskPlanInfoDTO> listPageTaskPlans(TaskPlanQueryDTO taskPlanQuery,
                                                       BaseSearchCondition baseSearchCondition,
                                                       List<Long> excludePlanIdList) {
        List<Condition> conditions = buildTaskPlanQueryCondition(taskPlanQuery, baseSearchCondition);
        List<OrderField<?>> orderFields = buildOrderField(baseSearchCondition);

        if (CollectionUtils.isNotEmpty(excludePlanIdList)) {
            conditions.add(
                TABLE.ID.notIn(excludePlanIdList.parallelStream().map(ULong::valueOf).collect(Collectors.toList())));
        }

        long count = getPageTaskPlanCount(conditions);

        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);

        SelectJoinStep<Record13<ULong, ULong, ULong, UByte, String, String, ULong, String, ULong, ULong, ULong, String,
            UByte>> selectJoinStep =
            context.select(TABLE.ID, TABLE.APP_ID, TABLE.TEMPLATE_ID, TABLE.TYPE, TABLE.NAME, TABLE.CREATOR,
                TABLE.CREATE_TIME, TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME, TABLE.FIRST_STEP_ID,
                TABLE.LAST_STEP_ID, TABLE.VERSION, TABLE.IS_LATEST_VERSION).from(TABLE);

        Result<Record13<ULong, ULong, ULong, UByte, String, String, ULong, String, ULong, ULong, ULong, String,
            UByte>> result = null;
        if (baseSearchCondition.isGetAll()) {
            start = 0;
            length = (int) count;
            result = selectJoinStep.where(conditions).orderBy(orderFields).fetch();
        } else {
            result = selectJoinStep.where(conditions).orderBy(orderFields).limit(start, length).fetch();
        }

        List<TaskPlanInfoDTO> taskPlanInfoList = new ArrayList<>();
        if (result != null && result.size() >= 1) {
            result.map(record -> taskPlanInfoList.add(DbRecordMapper.convertRecordToPlanInfo(record)));
        }

        PageData<TaskPlanInfoDTO> pageData = new PageData<>();
        pageData.setTotal(count);
        pageData.setData(taskPlanInfoList);
        pageData.setPageSize(length);
        pageData.setStart(start);
        return pageData;
    }

    /**
     * 查询符合条件的执行方案数量
     */
    private long getPageTaskPlanCount(List<Condition> conditions) {
        return context.selectCount().from(TABLE).where(conditions).fetchOne(0, Long.class);
    }

    private List<Condition> buildTaskPlanQueryCondition(TaskPlanQueryDTO taskPlanQuery,
                                                        BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.IS_DELETED.eq(UByte.valueOf(0)));
        conditions.add(TABLE.TYPE.eq(UByte.valueOf(TaskPlanTypeEnum.NORMAL.getType())));
        if (taskPlanQuery != null) {
            if (taskPlanQuery.getAppId() != null) {
                conditions.add(TABLE.APP_ID.eq(ULong.valueOf(taskPlanQuery.getAppId())));
            }
            if (taskPlanQuery.getPlanId() != null) {
                conditions.add(TABLE.ID.eq(ULong.valueOf(taskPlanQuery.getPlanId())));
                return conditions;
            }
            if (taskPlanQuery.getTemplateId() != null) {
                if (taskPlanQuery.getTemplateIdList() == null) {
                    taskPlanQuery.setTemplateIdList(new ArrayList<>());
                }
                taskPlanQuery.getTemplateIdList().add(taskPlanQuery.getTemplateId());
            }
            if (CollectionUtils.isNotEmpty(taskPlanQuery.getTemplateIdList())) {
                conditions.add(TABLE.TEMPLATE_ID.in(taskPlanQuery.getTemplateIdList().parallelStream().map(ULong::valueOf).collect(Collectors.toList())));
            }
            if (taskPlanQuery.getName() != null) {
                conditions.add(TABLE.NAME.like("%" + taskPlanQuery.getName() + "%"));
            }
        }
        if (baseSearchCondition != null) {
            if (StringUtils.isNotBlank(baseSearchCondition.getCreator())) {
                conditions.add(TABLE.CREATOR.eq(baseSearchCondition.getCreator()));
            }
            if (StringUtils.isNotBlank(baseSearchCondition.getLastModifyUser())) {
                conditions.add(TABLE.LAST_MODIFY_USER.eq(baseSearchCondition.getLastModifyUser()));
            }
        }
        return conditions;
    }

    @Override
    public TaskPlanInfoDTO getTaskPlanById(Long planId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.equal(ULong.valueOf(planId)));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        Record13<ULong, ULong, ULong, UByte, String, String, ULong, String, ULong, ULong, ULong, String,
            UByte> record = context
            .select(TABLE.ID, TABLE.APP_ID, TABLE.TEMPLATE_ID, TABLE.TYPE, TABLE.NAME, TABLE.CREATOR,
                TABLE.CREATE_TIME, TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME, TABLE.FIRST_STEP_ID,
                TABLE.LAST_STEP_ID, TABLE.VERSION, TABLE.IS_LATEST_VERSION)
            .from(TABLE).where(conditions).fetchOne();
        if (record != null) {
            TaskPlanInfoDTO taskPlan = DbRecordMapper.convertRecordToPlanInfo(record);
            return taskPlan;
        } else {
            return null;
        }
    }

    @Override
    public TaskPlanInfoDTO getTaskPlanById(Long appId, Long templateId, Long planId, TaskPlanTypeEnum planType) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.equal(ULong.valueOf(planId)));
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(appId)));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        if (planType != null) {
            conditions.add(TABLE.TYPE.equal(UByte.valueOf(planType.getType())));
        }
        if (templateId > 0) {
            conditions.add(TABLE.TEMPLATE_ID.eq(ULong.valueOf(templateId)));
        }
        Record13<ULong, ULong, ULong, UByte, String, String, ULong, String, ULong, ULong, ULong, String,
            UByte> record = context
            .select(TABLE.ID, TABLE.APP_ID, TABLE.TEMPLATE_ID, TABLE.TYPE, TABLE.NAME, TABLE.CREATOR,
                TABLE.CREATE_TIME, TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME, TABLE.FIRST_STEP_ID,
                TABLE.LAST_STEP_ID, TABLE.VERSION, TABLE.IS_LATEST_VERSION)
            .from(TABLE).where(conditions).fetchOne();
        if (record != null) {
            TaskPlanInfoDTO taskPlan = DbRecordMapper.convertRecordToPlanInfo(record);
            return taskPlan;
        } else {
            return null;
        }
    }

    @Override
    public Long insertTaskPlan(TaskPlanInfoDTO planInfo) {
        UByte planType;
        if (planInfo.getDebug()) {
            planType = UByte.valueOf(TaskPlanTypeEnum.DEBUG.getType());
        } else {
            planType = UByte.valueOf(TaskPlanTypeEnum.NORMAL.getType());
        }
        TaskPlanRecord taskPlanRecord = context.insertInto(TABLE)
            .columns(TABLE.APP_ID, TABLE.TEMPLATE_ID, TABLE.NAME, TABLE.CREATOR, TABLE.CREATE_TIME,
                TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME, TABLE.FIRST_STEP_ID, TABLE.LAST_STEP_ID, TABLE.TYPE,
                TABLE.VERSION)
            .values(ULong.valueOf(planInfo.getAppId()), ULong.valueOf(planInfo.getTemplateId()), planInfo.getName(),
                planInfo.getCreator(), ULong.valueOf(planInfo.getCreateTime()), planInfo.getLastModifyUser(),
                ULong.valueOf(planInfo.getLastModifyTime()), ULong.valueOf(planInfo.getFirstStepId()),
                ULong.valueOf(planInfo.getLastStepId()), planType, planInfo.getVersion())
            .returning(TABLE.ID).fetchOne();
        if (taskPlanRecord != null) {
            return taskPlanRecord.get(TABLE.ID).longValue();
        } else {
            return null;
        }
    }

    @Override
    public boolean updateTaskPlanById(TaskPlanInfoDTO planInfo) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.eq(ULong.valueOf(planInfo.getId())));
        conditions.add(TABLE.APP_ID.eq(ULong.valueOf(planInfo.getAppId())));
        conditions.add(TABLE.TEMPLATE_ID.eq(ULong.valueOf(planInfo.getTemplateId())));
        UpdateSetMoreStep<TaskPlanRecord> updateStep = context.update(TABLE)
            .set(TABLE.LAST_MODIFY_USER, planInfo.getLastModifyUser())
            .set(TABLE.LAST_MODIFY_TIME, ULong.valueOf(planInfo.getLastModifyTime()));
        if (StringUtils.isNotBlank(planInfo.getName())) {
            updateStep = updateStep.set(TABLE.NAME, planInfo.getName());
        }
        if (StringUtils.isNotBlank(planInfo.getVersion())) {
            updateStep = updateStep.set(TABLE.VERSION, planInfo.getVersion());
        }
        return 1 == updateStep.where(conditions).limit(1).execute();
    }

    @Override
    public boolean deleteTaskPlanById(Long appId, Long templateId, Long planId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.eq(ULong.valueOf(appId)));
        conditions.add(TABLE.ID.eq(ULong.valueOf(planId)));
        conditions.add(TABLE.TEMPLATE_ID.eq(ULong.valueOf(templateId)));
        conditions.add(TABLE.TYPE.eq(UByte.valueOf(TaskPlanTypeEnum.NORMAL.getType())));
        return 1 == context.update(TABLE).set(TABLE.IS_DELETED, UByte.valueOf(1)).where(conditions).limit(1).execute();
    }

    @Override
    public TaskPlanInfoDTO getDebugTaskPlan(Long appId, Long templateId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.eq(ULong.valueOf(appId)));
        conditions.add(TABLE.TEMPLATE_ID.eq(ULong.valueOf(templateId)));
        conditions.add(TABLE.TYPE.eq(UByte.valueOf(TaskPlanTypeEnum.DEBUG.getType())));
        conditions.add(TABLE.IS_DELETED.eq(UByte.valueOf(0)));
        Result<Record13<ULong, ULong, ULong, UByte, String, String, ULong, String, ULong, ULong, ULong, String,
            UByte>> result =
            context
                .select(TABLE.ID, TABLE.APP_ID, TABLE.TEMPLATE_ID, TABLE.TYPE, TABLE.NAME, TABLE.CREATOR,
                    TABLE.CREATE_TIME, TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME, TABLE.FIRST_STEP_ID,
                    TABLE.LAST_STEP_ID, TABLE.VERSION, TABLE.IS_LATEST_VERSION)
                .from(TABLE).where(conditions).fetch();
        if (result != null && result.size() > 0) {
            if (result.size() > 1) {
                log.warn("More than one debug plan for one template!|{}|{}|{}", appId, templateId,
                    JsonMapper.nonEmptyMapper().toJson(result));
            }
            return DbRecordMapper.convertRecordToPlanInfo(result.get(0));
        } else {
            return null;
        }
    }

    @Override
    public List<TaskPlanInfoDTO> listTaskPlanByIds(Long appId, List<Long> planIdList, TaskPlanQueryDTO taskPlanQuery,
                                                   BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = buildTaskPlanQueryCondition(taskPlanQuery, baseSearchCondition);
        if (taskPlanQuery == null) {
            conditions.add(TABLE.APP_ID.eq(ULong.valueOf(appId)));
        }
        conditions.add(TABLE.ID.in(planIdList.parallelStream().map(ULong::valueOf).collect(Collectors.toList())));
        Result<Record13<ULong, ULong, ULong, UByte, String, String, ULong, String, ULong, ULong, ULong, String,
            UByte>> result =
            context.select(TABLE.ID, TABLE.APP_ID, TABLE.TEMPLATE_ID, TABLE.TYPE, TABLE.NAME, TABLE.CREATOR,
                TABLE.CREATE_TIME, TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME, TABLE.FIRST_STEP_ID,
                TABLE.LAST_STEP_ID, TABLE.VERSION, TABLE.IS_LATEST_VERSION).from(TABLE)
                .where(conditions).fetch();

        List<TaskPlanInfoDTO> taskPlanInfoList = new ArrayList<>();
        if (result != null && result.size() > 0) {
            result.map(record ->
                taskPlanInfoList.add(DbRecordMapper.convertRecordToPlanInfo(record)));
        }
        return taskPlanInfoList;

    }

    @Override
    public boolean checkPlanName(Long appId, Long templateId, Long planId, String name) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.eq(ULong.valueOf(appId)));
        conditions.add(TABLE.TEMPLATE_ID.eq(ULong.valueOf(templateId)));
        conditions.add(TABLE.IS_DELETED.eq(UByte.valueOf(0)));
        if (planId > 0) {
            conditions.add(TABLE.ID.notEqual(ULong.valueOf(planId)));
        }
        conditions.add(TABLE.NAME.eq(name));
        return context.selectCount().from(TABLE).where(conditions).fetchOne(0, Integer.class) == 0;

    }

    @Override
    public boolean insertTaskPlanWithId(TaskPlanInfoDTO planInfo) {
        UByte planType = UByte.valueOf(TaskPlanTypeEnum.NORMAL.getType());
        return context.insertInto(TABLE)
            .columns(TABLE.ID, TABLE.APP_ID, TABLE.TEMPLATE_ID, TABLE.NAME, TABLE.CREATOR, TABLE.CREATE_TIME,
                TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME, TABLE.FIRST_STEP_ID, TABLE.LAST_STEP_ID, TABLE.TYPE,
                TABLE.VERSION)
            .values(ULong.valueOf(planInfo.getId()), ULong.valueOf(planInfo.getAppId()),
                ULong.valueOf(planInfo.getTemplateId()), planInfo.getName(), planInfo.getCreator(),
                ULong.valueOf(planInfo.getCreateTime()), planInfo.getLastModifyUser(),
                ULong.valueOf(planInfo.getLastModifyTime()), ULong.valueOf(planInfo.getFirstStepId()),
                ULong.valueOf(planInfo.getLastStepId()), planType, planInfo.getVersion())
            .execute() == 1;
    }

    @Override
    public String getPlanName(long planId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        conditions.add(TABLE.ID.equal(ULong.valueOf(planId)));
        Record1<String> record = context.select(TABLE.NAME).from(TABLE).where(conditions).fetchOne();
        if (record != null) {
            return record.get(TABLE.NAME);
        }
        return null;
    }

    @Override
    public boolean isDebugPlan(Long appId, Long templateId, Long planId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(appId)));
        conditions.add(TABLE.ID.equal(ULong.valueOf(planId)));
        conditions.add(TABLE.TEMPLATE_ID.equal(ULong.valueOf(templateId)));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        conditions.add(TABLE.TYPE.equal(UByte.valueOf(TaskPlanTypeEnum.DEBUG.getType())));
        return context.selectCount().from(TABLE).where(conditions).fetchOne(0, int.class) == 1;
    }

    @Override
    public boolean checkPlanId(Long planId) {
        return context.selectCount().from(TABLE).where(TABLE.ID.equal(ULong.valueOf(planId))).fetchOne().get(0,
            Integer.class) == 0;
    }

    @Override
    public TaskPlanInfoDTO getTaskPlanByName(Long appId, Long templateId, String name) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(appId)));
        conditions.add(TABLE.TEMPLATE_ID.equal(ULong.valueOf(templateId)));
        conditions.add(TABLE.NAME.equal(name));
        conditions.add(TABLE.IS_DELETED.eq(UByte.valueOf(0)));

        Record13<ULong, ULong, ULong, UByte, String, String, ULong, String, ULong, ULong, ULong, String,
            UByte> record = context
            .select(TABLE.ID, TABLE.APP_ID, TABLE.TEMPLATE_ID, TABLE.TYPE, TABLE.NAME, TABLE.CREATOR,
                TABLE.CREATE_TIME, TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME, TABLE.FIRST_STEP_ID,
                TABLE.LAST_STEP_ID, TABLE.VERSION, TABLE.IS_LATEST_VERSION)
            .from(TABLE).where(conditions).limit(1).fetchOne();
        if (record != null) {
            return DbRecordMapper.convertRecordToPlanInfo(record);
        }
        return null;
    }

    @Override
    public boolean deleteTaskPlanByTemplate(Long appId, Long templateId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(appId)));
        conditions.add(TABLE.TEMPLATE_ID.equal(ULong.valueOf(templateId)));
        return 1 <= context.update(TABLE).set(TABLE.IS_DELETED, UByte.valueOf(1)).where(conditions).execute();
    }

    @Override
    public boolean isExistAnyAppPlan(Long appId) {
        List<Condition> conditions = new ArrayList<>(2);
        conditions.add(TABLE.APP_ID.eq(ULong.valueOf(appId)));
        conditions.add(TABLE.IS_DELETED.eq(UByte.valueOf(0)));
        return context.fetchExists(TABLE, conditions);
    }

    @Override
    public Integer countTaskPlans(Long appId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.IS_DELETED.eq(UByte.valueOf(0)));
        if (appId != null) {
            conditions.add(TABLE.APP_ID.eq(ULong.valueOf(appId)));
        }
        return context.selectCount().from(TABLE)
            .where(conditions)
            .fetchOne().value1();
    }

    @Override
    public List<Long> listAllPlanId() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        List<ULong> uLongPlanId = context.select(TABLE.ID).from(TABLE).where(conditions).fetch(TABLE.ID);
        if (CollectionUtils.isNotEmpty(uLongPlanId)) {
            return uLongPlanId.parallelStream().map(ULong::longValue).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }
}
