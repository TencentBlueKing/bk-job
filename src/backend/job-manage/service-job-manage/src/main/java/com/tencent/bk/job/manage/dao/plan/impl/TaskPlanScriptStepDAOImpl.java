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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.manage.common.consts.task.TaskScriptSourceEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskTypeEnum;
import com.tencent.bk.job.manage.common.util.DbRecordMapper;
import com.tencent.bk.job.manage.dao.TaskScriptStepDAO;
import com.tencent.bk.job.manage.model.dto.task.TaskScriptStepDTO;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record15;
import org.jooq.Result;
import org.jooq.generated.tables.TaskPlan;
import org.jooq.generated.tables.TaskPlanStep;
import org.jooq.generated.tables.TaskPlanStepScript;
import org.jooq.generated.tables.records.TaskPlanStepScriptRecord;
import org.jooq.impl.DSL;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @since 3/10/2019 21:53
 */
@Slf4j
@Repository("TaskPlanScriptStepDAOImpl")
public class TaskPlanScriptStepDAOImpl implements TaskScriptStepDAO {

    private static final TaskPlanStepScript TABLE = TaskPlanStepScript.TASK_PLAN_STEP_SCRIPT;
    private static final TaskPlan tableTaskPlan = TaskPlan.TASK_PLAN;
    private static final TaskPlanStep tableTTStep = TaskPlanStep.TASK_PLAN_STEP;
    private static final TaskPlanStepScript tableTTStepScript = TaskPlanStepScript.TASK_PLAN_STEP_SCRIPT;

    private DSLContext context;

    @Autowired
    public TaskPlanScriptStepDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext context) {
        this.context = context;
    }

    @Override
    public List<TaskScriptStepDTO> listScriptStepByParentId(long parentId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.PLAN_ID.eq(ULong.valueOf(parentId)));
        Result<
            Record15<ULong, ULong, ULong, UByte, String, ULong, String, UByte, String, ULong, ULong, String, UByte,
                UByte, UByte>> result =
            context
                .select(TABLE.ID, TABLE.PLAN_ID, TABLE.STEP_ID, TABLE.SCRIPT_TYPE, TABLE.SCRIPT_ID,
                    TABLE.SCRIPT_VERSION_ID, TABLE.CONTENT, TABLE.LANGUAGE, TABLE.SCRIPT_PARAM,
                    TABLE.SCRIPT_TIMEOUT, TABLE.EXECUTE_ACCOUNT, TABLE.DESTINATION_HOST_LIST,
                    TABLE.IS_SECURE_PARAM, TABLE.STATUS, TABLE.IGNORE_ERROR)
                .from(TABLE).where(conditions).fetch();

        List<TaskScriptStepDTO> taskScriptStepList = new ArrayList<>();

        if (result != null && result.size() >= 1) {
            result.map(record -> taskScriptStepList
                .add(DbRecordMapper.convertRecordToTaskScriptStep(record, TaskTypeEnum.PLAN)));
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
                .select(TABLE.ID, TABLE.PLAN_ID, TABLE.STEP_ID, TABLE.SCRIPT_TYPE, TABLE.SCRIPT_ID,
                    TABLE.SCRIPT_VERSION_ID, TABLE.CONTENT, TABLE.LANGUAGE, TABLE.SCRIPT_PARAM,
                    TABLE.SCRIPT_TIMEOUT, TABLE.EXECUTE_ACCOUNT, TABLE.DESTINATION_HOST_LIST,
                    TABLE.IS_SECURE_PARAM, TABLE.STATUS, TABLE.IGNORE_ERROR)
                .from(TABLE).where(conditions).fetch();

        Map<Long, TaskScriptStepDTO> taskScriptStepMap = new HashMap<>(stepIdList.size());

        if (result != null && result.size() >= 1) {
            result.map(record -> taskScriptStepMap.put(record.get(TABLE.STEP_ID).longValue(),
                DbRecordMapper.convertRecordToTaskScriptStep(record, TaskTypeEnum.PLAN)));
        }
        return taskScriptStepMap;
    }

    @Override
    public TaskScriptStepDTO getScriptStepById(long stepId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.STEP_ID.eq(ULong.valueOf(stepId)));
        Record15<ULong, ULong, ULong, UByte, String, ULong, String, UByte, String, ULong, ULong, String, UByte, UByte,
            UByte> record = context.select(TABLE.ID, TABLE.PLAN_ID, TABLE.STEP_ID, TABLE.SCRIPT_TYPE, TABLE.SCRIPT_ID,
            TABLE.SCRIPT_VERSION_ID, TABLE.CONTENT, TABLE.LANGUAGE, TABLE.SCRIPT_PARAM, TABLE.SCRIPT_TIMEOUT,
            TABLE.EXECUTE_ACCOUNT, TABLE.DESTINATION_HOST_LIST, TABLE.IS_SECURE_PARAM, TABLE.STATUS,
            TABLE.IGNORE_ERROR).from(TABLE).where(conditions).fetchOne();
        if (record != null) {
            return DbRecordMapper.convertRecordToTaskScriptStep(record, TaskTypeEnum.PLAN);
        } else {
            return null;
        }
    }

    @Override
    public long insertScriptStep(TaskScriptStepDTO scriptStep) {
        UByte isSecureParam = UByte.valueOf(0);
        UByte status = UByte.valueOf(0);
        UByte ignoreError = UByte.valueOf(0);
        if (scriptStep.getSecureParam() != null && scriptStep.getSecureParam()) {
            isSecureParam = UByte.valueOf(1);
        }
        if (scriptStep.getIgnoreError() != null && scriptStep.getIgnoreError()) {
            ignoreError = UByte.valueOf(1);
        }
        TaskPlanStepScriptRecord record = context.insertInto(TABLE)
            .columns(TABLE.PLAN_ID, TABLE.STEP_ID, TABLE.SCRIPT_TYPE, TABLE.SCRIPT_ID, TABLE.SCRIPT_VERSION_ID,
                TABLE.CONTENT, TABLE.LANGUAGE, TABLE.SCRIPT_PARAM, TABLE.SCRIPT_TIMEOUT, TABLE.EXECUTE_ACCOUNT,
                TABLE.DESTINATION_HOST_LIST, TABLE.IS_SECURE_PARAM, TABLE.STATUS, TABLE.IGNORE_ERROR)
            .values(ULong.valueOf(scriptStep.getPlanId()), ULong.valueOf(scriptStep.getStepId()),
                UByte.valueOf(scriptStep.getScriptSource().getType()), scriptStep.getScriptId(),
                scriptStep.getScriptVersionId() == null ? null : ULong.valueOf(scriptStep.getScriptVersionId()),
                scriptStep.getContent(), UByte.valueOf(scriptStep.getLanguage().getValue()),
                scriptStep.getScriptParam(), ULong.valueOf(scriptStep.getTimeout()),
                ULong.valueOf(scriptStep.getAccount()), scriptStep.getExecuteTarget().toString(), isSecureParam,
                status, ignoreError)
            .returning(TABLE.ID).fetchOne();
        return record.getId().longValue();
    }

    @Override
    public boolean updateScriptStepById(TaskScriptStepDTO scriptStep) {
        throw new InternalException(ErrorCode.UNSUPPORTED_OPERATION);
    }

    @Override
    public boolean deleteScriptStepById(long stepId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.STEP_ID.eq(ULong.valueOf(stepId)));
        return 1 == context.deleteFrom(TABLE).where(conditions).limit(1).execute();
    }

    @Override
    public List<TaskScriptStepDTO> batchListScriptStepIdByParentIds(List<Long> templateIdList) {
        throw new InternalException(ErrorCode.UNSUPPORTED_OPERATION);
    }

    @Override
    public int countScriptSteps(Long appId, TaskScriptSourceEnum scriptSource) {
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(tableTaskPlan.APP_ID.eq(ULong.valueOf(appId)));
        }
        if (scriptSource != null) {
            conditions.add(tableTTStepScript.SCRIPT_TYPE.eq(UByte.valueOf(scriptSource.getType())));
        }
        return context.selectCount().from(tableTTStepScript)
            .join(tableTTStep).on(tableTTStep.ID.eq(tableTTStepScript.STEP_ID))
            .join(tableTaskPlan).on(tableTTStep.PLAN_ID.eq(tableTaskPlan.ID))
            .where(conditions).fetchOne().value1();
    }

    @Override
    public int countScriptCitedByStepsByScriptIds(Long appId, List<String> scriptIdList) {
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(tableTaskPlan.APP_ID.eq(ULong.valueOf(appId)));
        }
        if (scriptIdList != null) {
            conditions.add(tableTTStepScript.SCRIPT_ID.in(scriptIdList));
        }
        return context.select(DSL.countDistinct(tableTTStepScript.SCRIPT_ID)).from(tableTTStepScript)
            .join(tableTTStep).on(tableTTStep.ID.eq(tableTTStepScript.STEP_ID))
            .join(tableTaskPlan).on(tableTTStep.PLAN_ID.eq(tableTaskPlan.ID))
            .where(conditions).fetchOne().value1();
    }

    @Override
    public int countScriptStepsByScriptIds(Long appId, List<String> scriptIdList) {
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(tableTaskPlan.APP_ID.eq(ULong.valueOf(appId)));
        }
        if (scriptIdList != null) {
            conditions.add(tableTTStepScript.SCRIPT_ID.in(scriptIdList));
        }
        return context.selectCount().from(tableTTStepScript)
            .join(tableTTStep).on(tableTTStep.ID.eq(tableTTStepScript.STEP_ID))
            .join(tableTaskPlan).on(tableTTStep.PLAN_ID.eq(tableTaskPlan.ID))
            .where(conditions).fetchOne().value1();
    }
}
