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

import com.tencent.bk.job.common.constant.Bool;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.manage.dao.TaskVariableDAO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep8;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.TableField;
import org.jooq.UpdateSetMoreStep;
import org.jooq.generated.tables.TaskPlanVariable;
import org.jooq.generated.tables.records.TaskPlanVariableRecord;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


@Slf4j
@Repository("TaskPlanVariableDAOImpl")
public class TaskPlanVariableDAOImpl implements TaskVariableDAO {

    private static final TaskPlanVariable TABLE = TaskPlanVariable.TASK_PLAN_VARIABLE;

    private static final TableField<?,?>[] ALL_FIELDS = {TABLE.TEMPLATE_VARIABLE_ID, TABLE.PLAN_ID,
        TABLE.NAME, TABLE.TYPE, TABLE.DEFAULT_VALUE, TABLE.DESCRIPTION, TABLE.IS_CHANGEABLE, TABLE.IS_REQUIRED};

    private final DSLContext context;

    @Autowired
    public TaskPlanVariableDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext context) {
        this.context = context;
    }

    @Override
    public List<TaskVariableDTO> listVariablesByParentId(long parentId) {

        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.PLAN_ID.eq(ULong.valueOf(parentId)));

        Result<Record> result = context.select(ALL_FIELDS).from(TABLE).where(conditions).fetch();
        return result.map(this::extract);
    }

    private TaskVariableDTO extract(Record record) {
        if (record == null) {
            return null;
        }
        TaskVariableDTO taskVariable = new TaskVariableDTO();
        taskVariable.setId(record.get(TABLE.TEMPLATE_VARIABLE_ID).longValue());
        taskVariable.setPlanId(record.get(TABLE.PLAN_ID).longValue());
        taskVariable.setName(record.get(TABLE.NAME));
        taskVariable.setType(TaskVariableTypeEnum.valOf(record.get(TABLE.TYPE).intValue()));
        taskVariable.setDefaultValue(record.get(TABLE.DEFAULT_VALUE));
        taskVariable.setDescription(record.get(TABLE.DESCRIPTION));
        taskVariable.setChangeable(Bool.isTrue(record.get(TABLE.IS_CHANGEABLE).intValue()));
        taskVariable.setRequired(Bool.isTrue(record.get(TABLE.IS_REQUIRED).intValue()));
        return taskVariable;
    }

    @Override
    public TaskVariableDTO getVariableById(long parentId, long id) {
        List<Condition> conditions = new ArrayList<>(2);
        conditions.add(TABLE.TEMPLATE_VARIABLE_ID.eq(ULong.valueOf(id)));
        conditions.add(TABLE.PLAN_ID.eq(ULong.valueOf(parentId)));

        Record record = context.select(ALL_FIELDS).from(TABLE).where(conditions).fetchOne();
        return extract(record);
    }

    @Override
    public TaskVariableDTO getVariableByName(long parentId, String varName) {
        List<Condition> conditions = new ArrayList<>(2);
        conditions.add(TABLE.NAME.eq(varName));
        conditions.add(TABLE.PLAN_ID.eq(ULong.valueOf(parentId)));
        Record record = context.select(ALL_FIELDS).from(TABLE).where(conditions).fetchOne();
        return extract(record);
    }

    @Override
    public long insertVariable(TaskVariableDTO variable) {
        TaskPlanVariableRecord record = context.insertInto(TABLE)
            .columns(TABLE.TEMPLATE_VARIABLE_ID, TABLE.PLAN_ID, TABLE.NAME, TABLE.TYPE, TABLE.DEFAULT_VALUE,
                TABLE.DESCRIPTION, TABLE.IS_CHANGEABLE, TABLE.IS_REQUIRED)
            .values(ULong.valueOf(variable.getId()), ULong.valueOf(variable.getPlanId()), variable.getName(),
                UByte.valueOf(variable.getType().getType()), variable.getDefaultValue(), variable.getDescription(),
                getChangeable(variable.getChangeable()), getRequired(variable.getRequired()))
            .returning(TABLE.TEMPLATE_VARIABLE_ID).fetchOne();
        return record.getTemplateVariableId().longValue();
    }

    @Override
    public List<Long> batchInsertVariables(List<TaskVariableDTO> variableList) {
        if (CollectionUtils.isEmpty(variableList)) {
            return Collections.emptyList();
        }
        InsertValuesStep8<TaskPlanVariableRecord, ULong, ULong, String, UByte, String, String, UByte,
                    UByte> insertStep = context.insertInto(TABLE).columns(TABLE.TEMPLATE_VARIABLE_ID, TABLE.PLAN_ID,
            TABLE.NAME, TABLE.TYPE, TABLE.DEFAULT_VALUE, TABLE.DESCRIPTION, TABLE.IS_CHANGEABLE, TABLE.IS_REQUIRED);

        variableList.forEach(variable -> insertStep.values(ULong.valueOf(variable.getId()),
            ULong.valueOf(variable.getPlanId()), variable.getName(), UByte.valueOf(variable.getType().getType()),
            variable.getDefaultValue(), variable.getDescription(), getChangeable(variable.getChangeable()),
            getRequired(variable.getRequired())));

        Result<TaskPlanVariableRecord> result = insertStep.returning(TABLE.TEMPLATE_VARIABLE_ID).fetch();
        List<Long> variableIdList = new ArrayList<>(variableList.size());
        result.forEach(record -> variableIdList.add(record.getTemplateVariableId().longValue()));

        try {
            Iterator<TaskVariableDTO> variableIterator = variableList.iterator();
            Iterator<Long> variableIdIterator = variableIdList.iterator();
            while (variableIterator.hasNext()) {
                TaskVariableDTO taskVariableInfo = variableIterator.next();
                taskVariableInfo.setId(variableIdIterator.next());
            }
        } catch (Exception e) {
            throw new InternalException(ErrorCode.BATCH_INSERT_FAILED);
        }

        return variableIdList;
    }

    @Override
    public boolean updateVariableById(TaskVariableDTO variable) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.TEMPLATE_VARIABLE_ID.eq(ULong.valueOf(variable.getId())));
        conditions.add(TABLE.PLAN_ID.eq(ULong.valueOf(variable.getPlanId())));
        UpdateSetMoreStep<TaskPlanVariableRecord> updateStep;
        if (variable.getType().isNeedMask() && variable.getDefaultValue().equals(variable.getType().getMask())) {
            updateStep = context.update(TABLE).set(TABLE.DEFAULT_VALUE, TABLE.DEFAULT_VALUE);
        } else {
            updateStep = context.update(TABLE).set(TABLE.DEFAULT_VALUE, variable.getDefaultValue());
        }
        if (StringUtils.isNotBlank(variable.getName())) {
            updateStep.set(TABLE.NAME, variable.getName());
        }
        if (StringUtils.isNotBlank(variable.getDescription())) {
            updateStep.set(TABLE.DESCRIPTION, variable.getDescription());
        }
        if (variable.getChangeable() != null) {
            updateStep.set(TABLE.IS_CHANGEABLE, variable.getChangeable() ? UByte.valueOf(1) : UByte.valueOf(0));
        }
        if (variable.getRequired() != null) {
            updateStep.set(TABLE.IS_REQUIRED, variable.getRequired() ? UByte.valueOf(1) : UByte.valueOf(0));
        }
        return 1 == updateStep.where(conditions).limit(1).execute();
    }

    @Override
    public boolean deleteVariableById(long parentId, long id) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.TEMPLATE_VARIABLE_ID.eq(ULong.valueOf(id)));
        conditions.add(TABLE.PLAN_ID.eq(ULong.valueOf(parentId)));
        return 1 == context.deleteFrom(TABLE).where(conditions).limit(1).execute();
    }

    @Override
    public int deleteVariableByParentId(long parentId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.PLAN_ID.eq(ULong.valueOf(parentId)));
        return context.deleteFrom(TABLE).where(conditions).execute();
    }

    @Override
    public boolean batchInsertVariableWithId(List<TaskVariableDTO> variableList) {
        return false;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class, Error.class})
    public boolean updateVariableByName(TaskVariableDTO variable) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.PLAN_ID.equal(ULong.valueOf(variable.getPlanId())));
        conditions.add(TABLE.NAME.equal(variable.getName()));
        return 1 ==
            context.update(TABLE).set(TABLE.DEFAULT_VALUE, variable.getDefaultValue()).where(conditions).limit(1).execute();
    }

    private UByte getChangeable(Boolean changeable) {
        UByte isChangeable = UByte.valueOf(0);
        if (changeable != null && changeable) {
            isChangeable = UByte.valueOf(1);
        }
        return isChangeable;
    }

    private UByte getRequired(Boolean required) {
        UByte isRequired = UByte.valueOf(0);
        if (required != null && required) {
            isRequired = UByte.valueOf(1);
        }
        return isRequired;
    }
}
