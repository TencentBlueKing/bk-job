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

package com.tencent.bk.job.manage.dao.template.impl;

import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.util.TagUtils;
import com.tencent.bk.job.manage.common.util.DbRecordMapper;
import com.tencent.bk.job.manage.dao.template.TaskTemplateDAO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.query.TaskTemplateQuery;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record13;
import org.jooq.Result;
import org.jooq.SelectJoinStep;
import org.jooq.TableField;
import org.jooq.UpdateSetMoreStep;
import org.jooq.generated.tables.TaskTemplate;
import org.jooq.generated.tables.records.TaskTemplateRecord;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.falseCondition;

/**
 * @since 27/9/2019 15:22
 */
@Slf4j
@Repository
public class TaskTemplateDAOImpl implements TaskTemplateDAO {
    private static final TaskTemplate TABLE = TaskTemplate.TASK_TEMPLATE;
    private DSLContext context;

    @Autowired
    public TaskTemplateDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext context) {
        this.context = context;
    }

    private static OrderField<?> buildBasicOrderField(TableField<TaskTemplateRecord, ?> field, Integer order) {
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
    public PageData<TaskTemplateInfoDTO> listPageTaskTemplates(TaskTemplateQuery query) {
        List<Condition> conditions = buildConditionList(query);
        long templateCount = getPageTaskTemplateCount(conditions);

        BaseSearchCondition baseSearchCondition = query.getBaseSearchCondition();
        List<OrderField<?>> orderFields = buildOrderField(baseSearchCondition);


        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);

        SelectJoinStep<Record13<ULong, ULong, String, String, String, UByte, ULong, String, ULong, ULong, ULong,
                    String, UByte>> selectJoinStep =
            context.select(TABLE.ID, TABLE.APP_ID, TABLE.NAME, TABLE.DESCRIPTION, TABLE.CREATOR, TABLE.STATUS,
                TABLE.CREATE_TIME, TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME, TABLE.FIRST_STEP_ID,
                TABLE.LAST_STEP_ID, TABLE.VERSION, TABLE.SCRIPT_STATUS).from(TABLE);

        Result<Record13<ULong, ULong, String, String, String, UByte, ULong, String, ULong, ULong, ULong, String,
            UByte>> records;

        if (baseSearchCondition.isGetAll()) {
            start = 0;
            length = (int) templateCount;
            records = selectJoinStep.where(conditions).orderBy(orderFields).fetch();
        } else {
            records = selectJoinStep.where(conditions).orderBy(orderFields).limit(start, length).fetch();
        }

        List<TaskTemplateInfoDTO> templateInfoList = new ArrayList<>();
        if (records.size() >= 1) {
            records.forEach(record -> templateInfoList.add(DbRecordMapper.convertRecordToTemplateInfo(record)));
        }

        PageData<TaskTemplateInfoDTO> templateInfoPageData = new PageData<>();
        templateInfoPageData.setTotal(templateCount);
        templateInfoPageData.setStart(start);
        templateInfoPageData.setPageSize(length);
        templateInfoPageData.setData(templateInfoList);

        return templateInfoPageData;
    }

    @Override
    public List<TaskTemplateInfoDTO> listTaskTemplates(TaskTemplateQuery query) {
        List<Condition> conditions = buildConditionList(query);
        List<OrderField<?>> orderFields = buildOrderField(query.getBaseSearchCondition());
        Result<Record13<ULong, ULong, String, String, String, UByte, ULong, String, ULong, ULong, ULong, String,
            UByte>> result =
            context
                .select(TABLE.ID, TABLE.APP_ID, TABLE.NAME, TABLE.DESCRIPTION, TABLE.CREATOR, TABLE.STATUS,
                    TABLE.CREATE_TIME, TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME,
                    TABLE.FIRST_STEP_ID, TABLE.LAST_STEP_ID, TABLE.VERSION, TABLE.SCRIPT_STATUS)
                .from(TABLE).where(conditions).orderBy(orderFields).fetch();
        List<TaskTemplateInfoDTO> templateInfoList = new ArrayList<>();
        if (result.size() >= 1) {
            result.map(record -> templateInfoList.add(DbRecordMapper.convertRecordToTemplateInfo(record)));
        }
        return templateInfoList;
    }

    @Override
    public TaskTemplateInfoDTO getTaskTemplateById(Long appId, Long templateId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.equal(ULong.valueOf(templateId)));
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(appId)));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        Record13<ULong, ULong, String, String, String, UByte, ULong, String, ULong, ULong, ULong, String,
            UByte> record =
            context
                .select(TABLE.ID, TABLE.APP_ID, TABLE.NAME, TABLE.DESCRIPTION, TABLE.CREATOR, TABLE.STATUS,
                    TABLE.CREATE_TIME, TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME,
                    TABLE.FIRST_STEP_ID, TABLE.LAST_STEP_ID, TABLE.VERSION, TABLE.SCRIPT_STATUS)
                .from(TABLE).where(conditions).fetchOne();
        if (record != null) {
            TaskTemplateInfoDTO taskTemplate = DbRecordMapper.convertRecordToTemplateInfo(record);
            return taskTemplate;
        } else {
            return null;
        }
    }

    @Override
    public TaskTemplateInfoDTO getTaskTemplateById(Long templateId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.equal(ULong.valueOf(templateId)));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        return getOneTaskTemplate(conditions);
    }

    @Override
    public TaskTemplateInfoDTO getDeletedTaskTemplateById(Long templateId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.equal(ULong.valueOf(templateId)));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(1L)));
        return getOneTaskTemplate(conditions);
    }

    private TaskTemplateInfoDTO getOneTaskTemplate(List<Condition> conditions) {
        Record13<ULong, ULong, String, String, String, UByte, ULong, String, ULong, ULong, ULong, String,
            UByte> record = context.select(TABLE.ID, TABLE.APP_ID, TABLE.NAME, TABLE.DESCRIPTION, TABLE.CREATOR,
            TABLE.STATUS,
            TABLE.CREATE_TIME, TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME, TABLE.FIRST_STEP_ID,
            TABLE.LAST_STEP_ID, TABLE.VERSION, TABLE.SCRIPT_STATUS)
            .from(TABLE).where(conditions).fetchOne();
        if (record != null) {
            return DbRecordMapper.convertRecordToTemplateInfo(record);
        } else {
            return null;
        }
    }

    private long getPageTaskTemplateCount(List<Condition> conditions) {
        return context.selectCount().from(TABLE).where(conditions).fetchOne(0, Long.class);
    }

    private List<Condition> buildConditionList(TaskTemplateQuery query) {
        List<Condition> conditions = new ArrayList<>();

        conditions.add(TABLE.IS_DELETED.eq(UByte.valueOf(0)));

        if (query.getAppId() != null) {
            conditions.add(TABLE.APP_ID.eq(ULong.valueOf(query.getAppId())));
        }

        if (CollectionUtils.isNotEmpty(query.getExcludeTemplateIds())) {
            if (query.getId() != null && query.getId() > 0) {
                if (query.getExcludeTemplateIds().contains(query.getId())) {
                    conditions.add(falseCondition());
                } else {
                    conditions.add(TABLE.ID.eq(ULong.valueOf(query.getId())));
                }
            } else if (CollectionUtils.isNotEmpty(query.getIds())) {
                List<Long> includeIds = new ArrayList<>(query.getIds());
                includeIds.removeAll(query.getExcludeTemplateIds());
                if (includeIds.size() == 1) {
                    conditions.add(TABLE.ID.eq(ULong.valueOf(includeIds.get(0))));
                } else {
                    conditions.add(TABLE.ID.in(includeIds));
                }
            } else {
                conditions.add(TABLE.ID.notIn(query.getExcludeTemplateIds()));
            }
        } else {
            if (query.getId() != null && query.getId() > 0) {
                conditions.add(TABLE.ID.eq(ULong.valueOf(query.getId())));
            } else if (CollectionUtils.isNotEmpty(query.getIds())) {
                if (query.getIds().size() == 1) {
                    conditions.add(TABLE.ID.eq(ULong.valueOf(query.getIds().get(0))));
                } else {
                    conditions.add(TABLE.ID.in(query.getIds()));
                }
            }
        }

        if (StringUtils.isNotBlank(query.getName())) {
            conditions.add(TABLE.NAME.like("%" + query.getName() + "%"));
        }

        if (query.getScriptStatus() != null) {
            if (query.getScriptStatus() != 0) {
                conditions.add(TABLE.SCRIPT_STATUS.greaterThan(UByte.valueOf(0)));
            } else {
                conditions.add(TABLE.SCRIPT_STATUS.eq(UByte.valueOf(0)));
            }
        }
        if (query.getStatus() != null) {
            conditions.add(TABLE.STATUS.eq(UByte.valueOf(query.getStatus().getStatus())));
        }
        if (query.getBaseSearchCondition() != null) {
            BaseSearchCondition baseSearchCondition = query.getBaseSearchCondition();
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
    public Long insertTaskTemplate(TaskTemplateInfoDTO templateInfo) {
        TaskTemplateRecord record = context.insertInto(TABLE)
            .columns(TABLE.APP_ID, TABLE.NAME, TABLE.DESCRIPTION, TABLE.CREATOR, TABLE.STATUS, TABLE.CREATE_TIME,
                TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME, TABLE.FIRST_STEP_ID, TABLE.LAST_STEP_ID,
                TABLE.VERSION, TABLE.SCRIPT_STATUS)
            .values(ULong.valueOf(templateInfo.getAppId()), templateInfo.getName(), templateInfo.getDescription(),
                templateInfo.getCreator(), UByte.valueOf(templateInfo.getStatus().getStatus()),
                ULong.valueOf(templateInfo.getCreateTime()), templateInfo.getLastModifyUser(),
                ULong.valueOf(templateInfo.getLastModifyTime()),
                ULong.valueOf(templateInfo.getFirstStepId()), ULong.valueOf(templateInfo.getLastStepId()),
                UUID.randomUUID().toString(), UByte.valueOf(0))
            .returning(TABLE.ID).fetchOne();
        if (record != null) {
            return record.getId().longValue();
        } else {
            return null;
        }
    }

    @Override
    public boolean updateTaskTemplateById(TaskTemplateInfoDTO templateInfo, boolean bumpVersion) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.equal(ULong.valueOf(templateInfo.getId())));
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(templateInfo.getAppId())));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        UpdateSetMoreStep<TaskTemplateRecord> updateStep =
            context.update(TABLE).set(TABLE.LAST_MODIFY_USER, templateInfo.getLastModifyUser());
        updateStep = updateStep.set(TABLE.LAST_MODIFY_TIME, ULong.valueOf(templateInfo.getLastModifyTime()));
        if (bumpVersion) {
            updateStep = updateStep.set(TABLE.VERSION, UUID.randomUUID().toString());
        }
        if (templateInfo.getName() != null) {
            updateStep = updateStep.set(TABLE.NAME, templateInfo.getName());
        }
        if (templateInfo.getDescription() != null) {
            updateStep = updateStep.set(TABLE.DESCRIPTION, templateInfo.getDescription());
        }
        if (templateInfo.getStatus() != null) {
            updateStep = updateStep.set(TABLE.STATUS, UByte.valueOf(templateInfo.getStatus().getStatus()));
        }
        if (templateInfo.getFirstStepId() != null) {
            updateStep = updateStep.set(TABLE.FIRST_STEP_ID, ULong.valueOf(templateInfo.getFirstStepId()));
        }
        if (templateInfo.getLastStepId() != null) {
            updateStep = updateStep.set(TABLE.LAST_STEP_ID, ULong.valueOf(templateInfo.getLastStepId()));
        }
        if (templateInfo.getScriptStatus() != null) {
            updateStep = updateStep.set(TABLE.SCRIPT_STATUS, UByte.valueOf(templateInfo.getScriptStatus()));
        }
        return 1 == updateStep.where(conditions).limit(1).execute();
    }

    @Override
    public boolean deleteTaskTemplateById(Long appId, Long templateId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.equal(ULong.valueOf(templateId)));
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(appId)));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        return 1 == context.update(TABLE).set(TABLE.IS_DELETED, UByte.valueOf(1)).where(conditions).limit(1).execute();
    }

    @Override
    public Long getAllTemplateCount(Long appId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(appId)));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        return getPageTaskTemplateCount(conditions);
    }

    @Override
    public Long getNeedUpdateTemplateCount(Long appId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(appId)));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        conditions.add(TABLE.SCRIPT_STATUS.greaterThan(UByte.valueOf(0)));
        return getPageTaskTemplateCount(conditions);
    }

    @Override
    public boolean checkTemplateName(Long appId, Long templateId, String name) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(appId)));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        if (templateId > 0) {
            conditions.add(TABLE.ID.notEqual(ULong.valueOf(templateId)));
        }
        conditions.add(TABLE.NAME.equal(name));
        return context.selectCount().from(TABLE).where(conditions).fetchOne(0, Integer.class) == 0;
    }

    @Override
    public String getTemplateVersionById(Long appId, Long templateId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.equal(DbRecordMapper.getJooqLongValue(appId)));
        conditions.add(TABLE.ID.equal(DbRecordMapper.getJooqLongValue(templateId)));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        Record1<String> versionRecord = context.select(TABLE.VERSION).from(TABLE).where(conditions).limit(1).fetchOne();
        if (versionRecord != null) {
            return versionRecord.get(TABLE.VERSION);
        }
        return null;
    }

    @Override
    public TaskTemplateInfoDTO getTaskTemplateByName(Long appId, String name) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.NAME.equal(name));
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(appId)));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        Record13<ULong, ULong, String, String, String, UByte, ULong, String, ULong, ULong, ULong, String,
            UByte> record =
            context
                .select(TABLE.ID, TABLE.APP_ID, TABLE.NAME, TABLE.DESCRIPTION, TABLE.CREATOR, TABLE.STATUS,
                    TABLE.CREATE_TIME, TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME,
                    TABLE.FIRST_STEP_ID, TABLE.LAST_STEP_ID, TABLE.VERSION, TABLE.SCRIPT_STATUS)
                .from(TABLE).where(conditions).fetchOne();
        if (record != null) {
            TaskTemplateInfoDTO taskTemplate = DbRecordMapper.convertRecordToTemplateInfo(record);
            return taskTemplate;
        } else {
            return null;
        }
    }

    @Override
    public TaskTemplateInfoDTO getTemplateById(long templateId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.equal(ULong.valueOf(templateId)));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        Record13<ULong, ULong, String, String, String, UByte, ULong, String, ULong, ULong, ULong, String,
            UByte> record = context.select(TABLE.ID, TABLE.APP_ID, TABLE.NAME, TABLE.DESCRIPTION, TABLE.CREATOR,
            TABLE.STATUS,
            TABLE.CREATE_TIME, TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME,
            TABLE.FIRST_STEP_ID, TABLE.LAST_STEP_ID, TABLE.VERSION, TABLE.SCRIPT_STATUS).from(TABLE).where(conditions).fetchOne();
        if (record != null) {
            return DbRecordMapper.convertRecordToTemplateInfo(record);
        }
        return null;
    }

    @Override
    public String getTemplateName(long templateId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.equal(ULong.valueOf(templateId)));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        Record1<String> record = context.select(TABLE.NAME).from(TABLE).where(conditions).fetchOne();
        if (record != null) {
            return record.get(TABLE.NAME);
        }
        return null;
    }

    @Override
    public boolean checkTemplateId(Long templateId) {
        return Objects.requireNonNull(context.selectCount().from(TABLE)
            .where(TABLE.ID.equal(ULong.valueOf(templateId))).fetchOne()).get(0, Integer.class) == 0;
    }

    @Override
    public boolean insertTaskTemplateWithId(TaskTemplateInfoDTO templateInfo) {
        return context.insertInto(TABLE)
            .columns(TABLE.ID, TABLE.APP_ID, TABLE.NAME, TABLE.DESCRIPTION, TABLE.CREATOR, TABLE.STATUS,
                TABLE.CREATE_TIME,
                TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME, TABLE.FIRST_STEP_ID, TABLE.LAST_STEP_ID,
                TABLE.VERSION, TABLE.SCRIPT_STATUS)
            .values(ULong.valueOf(templateInfo.getId()), ULong.valueOf(templateInfo.getAppId()),
                templateInfo.getName(), templateInfo.getDescription(), templateInfo.getCreator(),
                UByte.valueOf(templateInfo.getStatus().getStatus()), ULong.valueOf(templateInfo.getCreateTime()),
                templateInfo.getLastModifyUser(), ULong.valueOf(templateInfo.getLastModifyTime()),
                ULong.valueOf(templateInfo.getFirstStepId()), ULong.valueOf(templateInfo.getLastStepId()),
                UUID.randomUUID().toString(), UByte.valueOf(0))
            .execute() == 1;
    }


    private Integer countScriptByConditions(Collection<Condition> conditions) {
        return Objects.requireNonNull(context.selectCount().from(TABLE)
            .where(conditions)
            .fetchOne()).value1();
    }

    @Override
    public List<Long> listAllTemplateId() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.IS_DELETED.eq(UByte.valueOf(0)));
        List<ULong> templateIds = context.select(TABLE.ID).from(TABLE).where(conditions).fetch(TABLE.ID);
        return templateIds.stream().map(ULong::longValue).collect(Collectors.toList());
    }

    @Override
    public List<Long> listAllAppTemplateId(Long appId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.eq(ULong.valueOf(appId)));
        conditions.add(TABLE.IS_DELETED.eq(UByte.valueOf(0)));
        List<ULong> templateIds = context.select(TABLE.ID).from(TABLE).where(conditions).fetch(TABLE.ID);
        return templateIds.stream().map(ULong::longValue).collect(Collectors.toList());
    }

    @Override
    public Integer countTemplates(Long appId) {
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
    public boolean updateTaskTemplateVersion(long appId, long templateId, String version) {
        return context.update(TABLE).set(TABLE.VERSION, version)
            .where(TABLE.ID.eq(ULong.valueOf(templateId)))
            .and(TABLE.APP_ID.eq(ULong.valueOf(appId)))
            .and(TABLE.IS_DELETED.equal(UByte.valueOf(0))).execute() > 0;
    }

    @Override
    public boolean isExistAnyAppTemplate(Long appId) {
        List<Condition> conditions = new ArrayList<>(2);
        conditions.add(TABLE.APP_ID.eq(ULong.valueOf(appId)));
        conditions.add(TABLE.IS_DELETED.eq(UByte.valueOf(0)));
        return context.fetchExists(TABLE, conditions);
    }

    @Override
    public void updateTemplateStatus(ULong templateId, int scriptStatus) {
        context.update(TABLE).set(TABLE.SCRIPT_STATUS, UByte.valueOf(scriptStatus))
            .where(TABLE.ID.equal(templateId)).execute();
    }

    @Override
    public Map<Long, List<Long>> listAllTemplateTagsCompatible() {
        Result<? extends Record> result =
            context.select(TABLE.ID, TABLE.TAGS).from(TABLE).where(TABLE.IS_DELETED.eq(UByte.valueOf(0))).fetch();
        Map<Long, List<Long>> templateTagsMap = new HashMap<>();
        result.map(record -> {
            long templateId = record.get(TABLE.ID).longValue();
            List<Long> tagIds = TagUtils.decodeDbTag(record.get(TABLE.TAGS));
            templateTagsMap.put(templateId, tagIds);
            return null;
        });
        return templateTagsMap;
    }
}
