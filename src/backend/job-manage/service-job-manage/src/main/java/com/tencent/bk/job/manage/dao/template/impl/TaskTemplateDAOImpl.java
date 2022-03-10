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
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.service.TagService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.*;
import org.jooq.generated.tables.TaskTemplate;
import org.jooq.generated.tables.records.TaskTemplateRecord;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @since 27/9/2019 15:22
 */
@Slf4j
@Repository
public class TaskTemplateDAOImpl implements TaskTemplateDAO {
    private static final TaskTemplate TABLE = TaskTemplate.TASK_TEMPLATE;
    private DSLContext context;
    private TagService tagService;

    @Autowired
    public TaskTemplateDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext context, TagService tagService) {
        this.context = context;
        this.tagService = tagService;
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
                } else if (TABLE.TAGS.getName().equals(baseSearchCondition.getOrderField())) {
                    orderFields.add(buildBasicOrderField(TABLE.TAGS, baseSearchCondition.getOrder()));
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
    public PageData<TaskTemplateInfoDTO> listPageTaskTemplates(TaskTemplateInfoDTO templateCondition,
                                                               BaseSearchCondition baseSearchCondition,
                                                               List<Long> excludeTemplateIdList) {
        List<Condition> conditions = buildConditionList(templateCondition, baseSearchCondition);
        long templateCount = getPageTaskTemplateCount(conditions);
        List<OrderField<?>> orderFields = buildOrderField(baseSearchCondition);

        if (CollectionUtils.isNotEmpty(excludeTemplateIdList)) {
            conditions.add(TABLE.ID
                .notIn(excludeTemplateIdList.parallelStream().map(ULong::valueOf).collect(Collectors.toList())));
        }

        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);

        SelectJoinStep<Record14<ULong, ULong, String, String, String, UByte, ULong, String, ULong, String, ULong, ULong,
            String, UByte>> selectJoinStep =
            context.select(TABLE.ID, TABLE.APP_ID, TABLE.NAME, TABLE.DESCRIPTION, TABLE.CREATOR, TABLE.STATUS,
                TABLE.CREATE_TIME, TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME, TABLE.TAGS, TABLE.FIRST_STEP_ID,
                TABLE.LAST_STEP_ID, TABLE.VERSION, TABLE.SCRIPT_STATUS).from(TABLE);

        Result<Record14<ULong, ULong, String, String, String, UByte, ULong, String, ULong, String, ULong, ULong, String,
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
        setTagName(templateInfoList);
        PageData<TaskTemplateInfoDTO> templateInfoPageData = new PageData<>();
        templateInfoPageData.setTotal(templateCount);
        templateInfoPageData.setStart(start);
        templateInfoPageData.setPageSize(length);
        templateInfoPageData.setData(templateInfoList);

        return templateInfoPageData;
    }

    @Override
    public List<TaskTemplateInfoDTO> listTaskTemplateByIds(Long appId, List<Long> templateIdList,
                                                           TaskTemplateInfoDTO templateCondition,
                                                           BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = buildConditionList(templateCondition, baseSearchCondition);
        conditions.add(TABLE.ID.in(templateIdList.parallelStream().map(ULong::valueOf).collect(Collectors.toList())));
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(appId)));
        List<OrderField<?>> orderFields = buildOrderField(baseSearchCondition);
        Result<Record14<ULong, ULong, String, String, String, UByte, ULong, String, ULong, String, ULong, ULong, String,
            UByte>> result =
            context
                .select(TABLE.ID, TABLE.APP_ID, TABLE.NAME, TABLE.DESCRIPTION, TABLE.CREATOR, TABLE.STATUS,
                    TABLE.CREATE_TIME, TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME, TABLE.TAGS,
                    TABLE.FIRST_STEP_ID, TABLE.LAST_STEP_ID, TABLE.VERSION, TABLE.SCRIPT_STATUS)
                .from(TABLE).where(conditions).orderBy(orderFields).fetch();
        List<TaskTemplateInfoDTO> templateInfoList = new ArrayList<>();
        if (result != null && result.size() >= 1) {
            result.map(record -> templateInfoList.add(DbRecordMapper.convertRecordToTemplateInfo(record)));
            setTagName(templateInfoList);
        }
        return templateInfoList;
    }

    @Override
    public TaskTemplateInfoDTO getTaskTemplateById(Long appId, Long templateId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.equal(ULong.valueOf(templateId)));
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(appId)));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        Record14<ULong, ULong, String, String, String, UByte, ULong, String, ULong, String, ULong, ULong, String,
            UByte> record =
            context
                .select(TABLE.ID, TABLE.APP_ID, TABLE.NAME, TABLE.DESCRIPTION, TABLE.CREATOR, TABLE.STATUS,
                    TABLE.CREATE_TIME, TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME, TABLE.TAGS,
                    TABLE.FIRST_STEP_ID, TABLE.LAST_STEP_ID, TABLE.VERSION, TABLE.SCRIPT_STATUS)
                .from(TABLE).where(conditions).fetchOne();
        if (record != null) {
            TaskTemplateInfoDTO taskTemplate = DbRecordMapper.convertRecordToTemplateInfo(record);
            setTagName(Collections.singletonList(taskTemplate));
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
        Record14<ULong, ULong, String, String, String, UByte, ULong, String, ULong, String, ULong, ULong, String,
            UByte> record = context.select(TABLE.ID, TABLE.APP_ID, TABLE.NAME, TABLE.DESCRIPTION, TABLE.CREATOR,
            TABLE.STATUS,
            TABLE.CREATE_TIME, TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME, TABLE.TAGS, TABLE.FIRST_STEP_ID,
            TABLE.LAST_STEP_ID, TABLE.VERSION, TABLE.SCRIPT_STATUS)
            .from(TABLE).where(conditions).fetchOne();
        if (record != null && record.size() > 0) {
            return DbRecordMapper.convertRecordToTemplateInfo(record);
        } else {
            return null;
        }
    }

    /**
     * 查询符合条件的模版数量
     *
     * @return
     */
    private long getPageTaskTemplateCount(List<Condition> conditions) {
        return context.selectCount().from(TABLE).where(conditions).fetchOne(0, Long.class);
    }

    private List<Condition> buildConditionList(TaskTemplateInfoDTO templateCondition,
                                               BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = new ArrayList<>();

        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));

        if (templateCondition != null) {
            conditions.add(TABLE.APP_ID.equal(ULong.valueOf(templateCondition.getAppId())));
            if (templateCondition.getId() != null && templateCondition.getId() > 0) {
                conditions.add(TABLE.ID.equal(ULong.valueOf(templateCondition.getId())));
                return conditions;
            }
            if (StringUtils.isNotBlank(templateCondition.getName())) {
                conditions.add(TABLE.NAME.like("%" + templateCondition.getName() + "%"));
            }
            if (StringUtils.isNotBlank(templateCondition.getCreator())) {
                conditions.add(TABLE.CREATOR.equal(templateCondition.getCreator()));
            }
            if (CollectionUtils.isNotEmpty(templateCondition.getTags())) {
                if (templateCondition.getTags().size() == 1 && templateCondition.getTags().get(0).getId() == 0) {
                    conditions.add(TABLE.TAGS.isNull());
                } else {
                    for (TagDTO tagInfo : templateCondition.getTags()) {
                        conditions.add(TABLE.TAGS.like("%" + TagUtils.buildDbTag(tagInfo.getId()) + "%"));
                    }
                }
            }
            if (templateCondition.getScriptStatus() != null) {
                if (templateCondition.getScriptStatus() != 0) {
                    conditions.add(TABLE.SCRIPT_STATUS.greaterThan(UByte.valueOf(0)));
                } else {
                    conditions.add(TABLE.SCRIPT_STATUS.equal(UByte.valueOf(0)));
                }
            }
            if (templateCondition.getStatus() != null) {
                conditions.add(TABLE.STATUS.equal(UByte.valueOf(templateCondition.getStatus().getStatus())));
            }
        }
        if (baseSearchCondition != null) {
            if (StringUtils.isNotBlank(baseSearchCondition.getCreator())) {
                conditions.add(TABLE.CREATOR.equal(baseSearchCondition.getCreator()));
            }
            if (StringUtils.isNotBlank(baseSearchCondition.getLastModifyUser())) {
                conditions.add(TABLE.LAST_MODIFY_USER.equal(baseSearchCondition.getLastModifyUser()));
            }
        }
        return conditions;
    }

    @Override
    public Long insertTaskTemplate(TaskTemplateInfoDTO templateInfo) {
        TaskTemplateRecord record = context.insertInto(TABLE)
            .columns(TABLE.APP_ID, TABLE.NAME, TABLE.DESCRIPTION, TABLE.CREATOR, TABLE.STATUS, TABLE.CREATE_TIME,
                TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME, TABLE.TAGS, TABLE.FIRST_STEP_ID, TABLE.LAST_STEP_ID,
                TABLE.VERSION, TABLE.SCRIPT_STATUS)
            .values(ULong.valueOf(templateInfo.getAppId()), templateInfo.getName(), templateInfo.getDescription(),
                templateInfo.getCreator(), UByte.valueOf(templateInfo.getStatus().getStatus()),
                ULong.valueOf(templateInfo.getCreateTime()), templateInfo.getLastModifyUser(),
                ULong.valueOf(templateInfo.getLastModifyTime()),
                TagUtils
                    .buildDbTagList(templateInfo.getTags().stream().map(TagDTO::getId).collect(Collectors.toList())),
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
        if (templateInfo.getTags() != null) {
            updateStep = updateStep.set(TABLE.TAGS, TagUtils
                .buildDbTagList(templateInfo.getTags().stream().map(TagDTO::getId).collect(Collectors.toList())));
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
    public Map<Long, Long> getTemplateTagCount(Long appId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(appId)));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        Result<Record1<String>> result = context.select(TABLE.TAGS).from(TABLE).where(conditions).fetch();
        if (result == null) {
            return Collections.emptyMap();
        }
        Map<Long, Long> tagCount = new HashMap<>(result.size());
        for (Record1<String> record : result) {
            List<Long> tags = TagUtils.decodeDbTag(record.get(TABLE.TAGS));
            tags.forEach(tag -> {
                if (tagCount.containsKey(tag)) {
                    tagCount.put(tag, tagCount.get(tag) + 1);
                } else {
                    tagCount.put(tag, 1L);
                }
            });
        }

        return tagCount;
    }

    @Override
    public Long getAllTemplateCount(Long appId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(appId)));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        return getPageTaskTemplateCount(conditions);
    }

    @Override
    public Long getUnclassifiedTemplateCount(Long appId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(appId)));
        conditions.add(TABLE.IS_DELETED.equal(UByte.valueOf(0)));
        conditions.add(TABLE.TAGS.isNull());
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
        Record14<ULong, ULong, String, String, String, UByte, ULong, String, ULong, String, ULong, ULong, String,
            UByte> record =
            context
                .select(TABLE.ID, TABLE.APP_ID, TABLE.NAME, TABLE.DESCRIPTION, TABLE.CREATOR, TABLE.STATUS,
                    TABLE.CREATE_TIME, TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME, TABLE.TAGS,
                    TABLE.FIRST_STEP_ID, TABLE.LAST_STEP_ID, TABLE.VERSION, TABLE.SCRIPT_STATUS)
                .from(TABLE).where(conditions).fetchOne();
        if (record != null) {
            TaskTemplateInfoDTO taskTemplate = DbRecordMapper.convertRecordToTemplateInfo(record);
            setTagName(Collections.singletonList(taskTemplate));
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
        Record14<ULong, ULong, String, String, String, UByte, ULong, String, ULong, String, ULong, ULong, String,
            UByte> record = context.select(TABLE.ID, TABLE.APP_ID, TABLE.NAME, TABLE.DESCRIPTION, TABLE.CREATOR,
            TABLE.STATUS,
            TABLE.CREATE_TIME, TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME, TABLE.TAGS,
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
        return context.selectCount().from(TABLE).where(TABLE.ID.equal(ULong.valueOf(templateId))).fetchOne().get(0,
            Integer.class) == 0;
    }

    @Override
    public boolean insertTaskTemplateWithId(TaskTemplateInfoDTO templateInfo) {
        return context.insertInto(TABLE)
            .columns(TABLE.ID, TABLE.APP_ID, TABLE.NAME, TABLE.DESCRIPTION, TABLE.CREATOR, TABLE.STATUS,
                TABLE.CREATE_TIME,
                TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME, TABLE.TAGS, TABLE.FIRST_STEP_ID, TABLE.LAST_STEP_ID,
                TABLE.VERSION, TABLE.SCRIPT_STATUS)
            .values(ULong.valueOf(templateInfo.getId()), ULong.valueOf(templateInfo.getAppId()),
                templateInfo.getName(), templateInfo.getDescription(), templateInfo.getCreator(),
                UByte.valueOf(templateInfo.getStatus().getStatus()), ULong.valueOf(templateInfo.getCreateTime()),
                templateInfo.getLastModifyUser(), ULong.valueOf(templateInfo.getLastModifyTime()),
                TagUtils
                    .buildDbTagList(templateInfo.getTags().stream().map(TagDTO::getId).collect(Collectors.toList())),
                ULong.valueOf(templateInfo.getFirstStepId()), ULong.valueOf(templateInfo.getLastStepId()),
                UUID.randomUUID().toString(), UByte.valueOf(0))
            .execute() == 1;
    }


    private Integer countScriptByConditions(Collection<Condition> conditions) {
        return context.selectCount().from(TABLE)
            .where(conditions)
            .fetchOne().value1();
    }

    @Override
    public Integer countByTag(Long appId, Long tagId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.IS_DELETED.eq(UByte.valueOf(0)));
        if (appId != null) {
            conditions.add(TABLE.APP_ID.eq(ULong.valueOf(appId)));
        }
        if (tagId != null) {
            conditions.add(TABLE.TAGS.like("%<" + tagId + ">%"));
        }
        return countScriptByConditions(conditions);
    }

    @Override
    public List<Long> listAllTemplateId() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.IS_DELETED.eq(UByte.valueOf(0)));
        List<ULong> uLongTemplateId = context.select(TABLE.ID).from(TABLE).where(conditions).fetch(TABLE.ID);
        if (CollectionUtils.isNotEmpty(uLongTemplateId)) {
            return uLongTemplateId.parallelStream().map(ULong::longValue).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
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

    private void setTagName(List<TaskTemplateInfoDTO> templateInfoList) {
        // 设置标签名称.从DAO查询的结果仅包含tagId
        if (templateInfoList != null && !templateInfoList.isEmpty()) {
            Long appId = templateInfoList.get(0).getAppId();
            List<TagDTO> tags = tagService.listTagsByAppId(appId);
            Map<Long, String> tagIdNameMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(tags)) {
                tags.forEach(tag -> tagIdNameMap.put(tag.getId(), tag.getName()));
            }
            for (TaskTemplateInfoDTO templateInfo : templateInfoList) {
                List<TagDTO> scriptTags = templateInfo.getTags();
                if (CollectionUtils.isNotEmpty(scriptTags)) {
                    scriptTags.forEach(tag -> tag.setName(tagIdNameMap.get(tag.getId())));
                }
            }
        }
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
}
