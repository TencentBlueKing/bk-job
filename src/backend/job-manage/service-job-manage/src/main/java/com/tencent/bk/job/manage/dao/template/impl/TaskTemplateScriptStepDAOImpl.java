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

import com.tencent.bk.job.manage.common.consts.task.TaskScriptSourceEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskTypeEnum;
import com.tencent.bk.job.manage.common.util.DbRecordMapper;
import com.tencent.bk.job.manage.common.util.JooqDataTypeUtil;
import com.tencent.bk.job.manage.dao.TaskScriptStepDAO;
import com.tencent.bk.job.manage.model.dto.task.TaskScriptStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record15;
import org.jooq.Record3;
import org.jooq.Result;
import org.jooq.generated.tables.TaskTemplate;
import org.jooq.generated.tables.TaskTemplateStep;
import org.jooq.generated.tables.TaskTemplateStepScript;
import org.jooq.generated.tables.records.TaskTemplateStepScriptRecord;
import org.jooq.impl.DSL;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @since 3/10/2019 21:53
 */
@Slf4j
@Repository("TaskTemplateScriptStepDAOImpl")
public class TaskTemplateScriptStepDAOImpl implements TaskScriptStepDAO {

    private static final TaskTemplateStepScript TABLE = TaskTemplateStepScript.TASK_TEMPLATE_STEP_SCRIPT;
    private static final TaskTemplate tableTaskTemplate = TaskTemplate.TASK_TEMPLATE;
    private static final TaskTemplateStep tableTTStep = TaskTemplateStep.TASK_TEMPLATE_STEP;
    private static final TaskTemplateStepScript tableTTStepScript = TaskTemplateStepScript.TASK_TEMPLATE_STEP_SCRIPT;

    private DSLContext context;

    @Autowired
    public TaskTemplateScriptStepDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext context) {
        this.context = context;
    }

    @Override
    public List<TaskScriptStepDTO> listScriptStepByParentId(long parentId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.TEMPLATE_ID.eq(ULong.valueOf(parentId)));
        Result<
            Record15<ULong, ULong, ULong, UByte, String, ULong, String, UByte, String, ULong, ULong, String, UByte,
                UByte, UByte>> result =
            context
                .select(TABLE.ID, TABLE.TEMPLATE_ID, TABLE.STEP_ID, TABLE.SCRIPT_TYPE, TABLE.SCRIPT_ID,
                    TABLE.SCRIPT_VERSION_ID, TABLE.CONTENT, TABLE.LANGUAGE, TABLE.SCRIPT_PARAM,
                    TABLE.SCRIPT_TIMEOUT, TABLE.EXECUTE_ACCOUNT, TABLE.DESTINATION_HOST_LIST,
                    TABLE.IS_SECURE_PARAM, TABLE.STATUS, TABLE.IGNORE_ERROR)
                .from(TABLE).where(conditions).fetch();

        List<TaskScriptStepDTO> taskScriptStepList = new ArrayList<>();

        if (result != null && result.size() >= 1) {
            result.map(record -> taskScriptStepList
                .add(DbRecordMapper.convertRecordToTaskScriptStep(record, TaskTypeEnum.TEMPLATE)));
        }
        return taskScriptStepList;
    }

    @Override
    public Map<Long, TaskScriptStepDTO> listScriptStepByIds(List<Long> stepIdList) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.STEP_ID.in(stepIdList.stream().map(ULong::valueOf).collect(Collectors.toList())));
        Result<
            Record15<ULong, ULong, ULong, UByte, String, ULong, String, UByte, String, ULong, ULong, String, UByte,
                UByte, UByte>> result =
            context
                .select(TABLE.ID, TABLE.TEMPLATE_ID, TABLE.STEP_ID, TABLE.SCRIPT_TYPE, TABLE.SCRIPT_ID,
                    TABLE.SCRIPT_VERSION_ID, TABLE.CONTENT, TABLE.LANGUAGE, TABLE.SCRIPT_PARAM,
                    TABLE.SCRIPT_TIMEOUT, TABLE.EXECUTE_ACCOUNT, TABLE.DESTINATION_HOST_LIST,
                    TABLE.IS_SECURE_PARAM, TABLE.STATUS, TABLE.IGNORE_ERROR)
                .from(TABLE).where(conditions).fetch();

        Map<Long, TaskScriptStepDTO> taskScriptStepMap = new HashMap<>(stepIdList.size());

        if (result != null && result.size() >= 1) {
            result.map(record -> taskScriptStepMap.put(record.get(TABLE.STEP_ID).longValue(),
                DbRecordMapper.convertRecordToTaskScriptStep(record, TaskTypeEnum.TEMPLATE)));
        }
        return taskScriptStepMap;
    }

    @Override
    public TaskScriptStepDTO getScriptStepById(long stepId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.STEP_ID.eq(ULong.valueOf(stepId)));
        Record15<ULong, ULong, ULong, UByte, String, ULong, String, UByte, String, ULong, ULong, String, UByte, UByte,
            UByte> record = context.select(TABLE.ID, TABLE.TEMPLATE_ID, TABLE.STEP_ID, TABLE.SCRIPT_TYPE,
            TABLE.SCRIPT_ID, TABLE.SCRIPT_VERSION_ID, TABLE.CONTENT, TABLE.LANGUAGE, TABLE.SCRIPT_PARAM,
            TABLE.SCRIPT_TIMEOUT, TABLE.EXECUTE_ACCOUNT, TABLE.DESTINATION_HOST_LIST, TABLE.IS_SECURE_PARAM,
            TABLE.STATUS, TABLE.IGNORE_ERROR).from(TABLE).where(conditions).fetchOne();
        if (record != null) {
            return DbRecordMapper.convertRecordToTaskScriptStep(record, TaskTypeEnum.TEMPLATE);
        } else {
            return null;
        }
    }

    @Override
    public long insertScriptStep(TaskScriptStepDTO scriptStep) {
        UByte isSecureParam = UByte.valueOf(0);
        UByte ignoreError = UByte.valueOf(0);
        if (scriptStep.getSecureParam() != null && scriptStep.getSecureParam()) {
            isSecureParam = UByte.valueOf(1);
        }
        if (scriptStep.getIgnoreError() != null && scriptStep.getIgnoreError()) {
            ignoreError = UByte.valueOf(1);
        }
        TaskTemplateStepScriptRecord record = context.insertInto(TABLE)
            .columns(TABLE.TEMPLATE_ID, TABLE.STEP_ID, TABLE.SCRIPT_TYPE, TABLE.SCRIPT_ID, TABLE.SCRIPT_VERSION_ID,
                TABLE.CONTENT, TABLE.LANGUAGE, TABLE.SCRIPT_PARAM, TABLE.SCRIPT_TIMEOUT, TABLE.EXECUTE_ACCOUNT,
                TABLE.DESTINATION_HOST_LIST, TABLE.IS_SECURE_PARAM, TABLE.STATUS, TABLE.IGNORE_ERROR)
            .values(ULong.valueOf(scriptStep.getTemplateId()), ULong.valueOf(scriptStep.getStepId()),
                UByte.valueOf(scriptStep.getScriptSource().getType()), scriptStep.getScriptId(),
                scriptStep.getScriptVersionId() == null ? null : ULong.valueOf(scriptStep.getScriptVersionId()),
                scriptStep.getContent(), UByte.valueOf(scriptStep.getLanguage().getValue()),
                scriptStep.getScriptParam(), ULong.valueOf(scriptStep.getTimeout()),
                ULong.valueOf(scriptStep.getAccount()),
                scriptStep.getExecuteTarget() == null ? null : scriptStep.getExecuteTarget().toString(), isSecureParam,
                UByte.valueOf(scriptStep.getStatus()), ignoreError)
            .returning(TABLE.ID).fetchOne();
        return record.getId().longValue();
    }

    @Override
    public boolean updateScriptStepById(TaskScriptStepDTO scriptStep) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.STEP_ID.eq(ULong.valueOf(scriptStep.getStepId())));
        UByte isSecureParam = UByte.valueOf(0);
        UByte ignoreError = UByte.valueOf(0);
        if (scriptStep.getSecureParam()) {
            isSecureParam = UByte.valueOf(1);
        }
        if (scriptStep.getIgnoreError() != null && scriptStep.getIgnoreError()) {
            ignoreError = UByte.valueOf(1);
        }
        return 1 == context.update(TABLE).set(TABLE.SCRIPT_TYPE, UByte.valueOf(scriptStep.getScriptSource().getType()))
            .set(TABLE.SCRIPT_ID, scriptStep.getScriptId())
            .set(TABLE.SCRIPT_VERSION_ID,
                scriptStep.getScriptVersionId() == null ? null : ULong.valueOf(scriptStep.getScriptVersionId()))
            .set(TABLE.CONTENT, scriptStep.getContent())
            .set(TABLE.LANGUAGE, UByte.valueOf(scriptStep.getLanguage().getValue()))
            .set(TABLE.SCRIPT_PARAM, scriptStep.getScriptParam())
            .set(TABLE.SCRIPT_TIMEOUT, ULong.valueOf(scriptStep.getTimeout()))
            .set(TABLE.EXECUTE_ACCOUNT, ULong.valueOf(scriptStep.getAccount()))
            .set(TABLE.DESTINATION_HOST_LIST,
                scriptStep.getExecuteTarget() == null ? null : scriptStep.getExecuteTarget().toString())
            .set(TABLE.IS_SECURE_PARAM, isSecureParam).set(TABLE.STATUS, UByte.valueOf(scriptStep.getStatus()))
            .set(TABLE.IGNORE_ERROR, ignoreError).where(conditions).limit(1).execute();
    }

    @Override
    public boolean deleteScriptStepById(long stepId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.STEP_ID.eq(ULong.valueOf(stepId)));
        return 1 == context.deleteFrom(TABLE).where(conditions).limit(1).execute();
    }

    @Override
    public List<TaskScriptStepDTO> batchListScriptStepIdByParentIds(List<Long> templateIdList) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(
            TABLE.TEMPLATE_ID.in(templateIdList.parallelStream().map(ULong::valueOf).collect(Collectors.toList())));
        conditions.add(TABLE.SCRIPT_TYPE.in(Arrays.asList(UByte.valueOf(TaskScriptSourceEnum.CITING.getType()),
            UByte.valueOf(TaskScriptSourceEnum.PUBLIC.getType()))));
        conditions.add(TABLE.SCRIPT_ID.isNotNull());
        conditions.add(TABLE.SCRIPT_VERSION_ID.isNotNull());
        Result<Record3<ULong, String, ULong>> result = context
            .select(TABLE.TEMPLATE_ID, TABLE.SCRIPT_ID, TABLE.SCRIPT_VERSION_ID).from(TABLE).where(conditions).fetch();
        List<TaskScriptStepDTO> taskScriptStepList = new ArrayList<>();
        if (result != null && result.size() > 0) {
            result.map(record -> {
                TaskScriptStepDTO taskScriptStep = new TaskScriptStepDTO();
                taskScriptStep.setTemplateId(record.get(TABLE.TEMPLATE_ID).longValue());
                taskScriptStep.setScriptId(record.get(TABLE.SCRIPT_ID));
                taskScriptStep.setScriptVersionId(record.get(TABLE.SCRIPT_VERSION_ID).longValue());
                return taskScriptStepList.add(taskScriptStep);
            });
        }
        return taskScriptStepList;
    }

    @Override
    public boolean updateScriptStepRefScriptVersionId(Long templateId, Long stepId, Long scriptVersionId) {
        int updateRows = context.update(TABLE).set(TABLE.SCRIPT_VERSION_ID, ULong.valueOf(scriptVersionId))
            .where(TABLE.TEMPLATE_ID.eq(ULong.valueOf(templateId)))
            .and(TABLE.STEP_ID.eq(ULong.valueOf(stepId)))
            .execute();
        return updateRows > 0;
    }

    @Override
    public int countScriptSteps(Long appId, TaskScriptSourceEnum scriptSource) {
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(tableTaskTemplate.APP_ID.eq(ULong.valueOf(appId)));
        }
        if (scriptSource != null) {
            conditions.add(tableTTStepScript.SCRIPT_TYPE.eq(UByte.valueOf(scriptSource.getType())));
        }
        return context.selectCount().from(tableTTStepScript)
            .join(tableTTStep).on(tableTTStep.ID.eq(tableTTStepScript.STEP_ID))
            .join(tableTaskTemplate).on(tableTTStep.TEMPLATE_ID.eq(tableTaskTemplate.ID))
            .where(conditions).fetchOne().value1();
    }

    @Override
    public int countScriptCitedByStepsByScriptIds(Long appId, List<String> scriptIdList) {
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(tableTaskTemplate.APP_ID.eq(ULong.valueOf(appId)));
        }
        if (scriptIdList != null) {
            conditions.add(tableTTStepScript.SCRIPT_ID.in(scriptIdList));
        }
        return context.select(DSL.countDistinct(tableTTStepScript.SCRIPT_ID)).from(tableTTStepScript)
            .join(tableTTStep).on(tableTTStep.ID.eq(tableTTStepScript.STEP_ID))
            .join(tableTaskTemplate).on(tableTTStep.TEMPLATE_ID.eq(tableTaskTemplate.ID))
            .where(conditions).fetchOne().value1();
    }

    @Override
    public int countScriptStepsByScriptIds(Long appId, List<String> scriptIdList) {
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(tableTaskTemplate.APP_ID.eq(ULong.valueOf(appId)));
        }
        if (scriptIdList != null) {
            conditions.add(tableTTStepScript.SCRIPT_ID.in(scriptIdList));
        }
        return context.selectCount().from(tableTTStepScript)
            .join(tableTTStep).on(tableTTStep.ID.eq(tableTTStepScript.STEP_ID))
            .join(tableTaskTemplate).on(tableTTStep.TEMPLATE_ID.eq(tableTaskTemplate.ID))
            .where(conditions).fetchOne().value1();
    }

    @Override
    public Map<Long, TaskTargetDTO> listStepTargets() {
        Result<?> result = context.select(TABLE.ID, TABLE.DESTINATION_HOST_LIST).fetch();
        Map<Long, TaskTargetDTO> stepTargets = new HashMap<>();
        if (result.isNotEmpty()) {
            result.forEach(record -> {
                Long recordId = record.get(TABLE.ID).longValue();
                TaskTargetDTO target = TaskTargetDTO.fromJsonString(record.get(TABLE.DESTINATION_HOST_LIST));
                stepTargets.put(recordId, target);
            });
        }
        return stepTargets;
    }

    @Override
    public boolean updateStepTargets(Long recordId, String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }
        int result = context.update(TABLE)
            .set(TABLE.DESTINATION_HOST_LIST, value)
            .where(TABLE.ID.eq(JooqDataTypeUtil.buildULong(recordId)))
            .execute();
        return result == 1;
    }

}
