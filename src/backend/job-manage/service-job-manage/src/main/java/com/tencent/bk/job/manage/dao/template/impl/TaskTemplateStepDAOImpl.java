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

import com.tencent.bk.job.manage.common.consts.task.TaskTypeEnum;
import com.tencent.bk.job.manage.common.util.DbRecordMapper;
import com.tencent.bk.job.manage.dao.AbstractTaskStepDAO;
import com.tencent.bk.job.manage.dao.TaskStepDAO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record11;
import org.jooq.Result;
import org.jooq.UpdateSetMoreStep;
import org.jooq.generated.tables.TaskTemplateStep;
import org.jooq.generated.tables.records.TaskTemplateStepRecord;
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
 * @since 3/10/2019 21:30
 */
@Slf4j
@Repository("TaskTemplateStepDAOImpl")
public class TaskTemplateStepDAOImpl extends AbstractTaskStepDAO implements TaskStepDAO {

    private static final TaskTemplateStep TABLE = TaskTemplateStep.TASK_TEMPLATE_STEP;

    private DSLContext context;

    @Autowired
    public TaskTemplateStepDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext context) {
        this.context = context;
    }

    @Override
    public List<TaskStepDTO> listStepsByParentId(long parentId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.TEMPLATE_ID.eq(ULong.valueOf(parentId)));
        conditions.add(TABLE.IS_DELETED.eq(UByte.valueOf(0)));
        Result<Record11<ULong, ULong, String, UByte, ULong, ULong, ULong, ULong, ULong, ULong, UByte>> records = context
            .select(TABLE.ID, TABLE.TEMPLATE_ID, TABLE.NAME, TABLE.TYPE, TABLE.PREVIOUS_STEP_ID, TABLE.NEXT_STEP_ID,
                TABLE.ID, TABLE.SCRIPT_STEP_ID, TABLE.FILE_STEP_ID, TABLE.APPROVAL_STEP_ID, TABLE.IS_DELETED)
            .from(TABLE).where(conditions).fetch();

        List<TaskStepDTO> taskStepList = new ArrayList<>();
        if (records != null && records.size() >= 1) {
            Map<Long, TaskStepDTO> taskStepMap = new HashMap<>(records.size());
            records.forEach(record -> {
                TaskStepDTO taskStep = DbRecordMapper.convertRecordToTaskStep(record, TaskTypeEnum.TEMPLATE);
                if (taskStep.getPreviousStepId() == 0) {
                    taskStepList.add(taskStep);
                } else {
                    taskStepMap.put(taskStep.getId(), taskStep);
                }
            });
            sortStep(taskStepList, taskStepMap);
        }
        return taskStepList;
    }

    @Override
    public TaskStepDTO getStepById(long parentId, long id) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.eq(ULong.valueOf(id)));
        conditions.add(TABLE.TEMPLATE_ID.eq(ULong.valueOf(parentId)));
        conditions.add(TABLE.IS_DELETED.eq(UByte.valueOf(0)));
        Record11<ULong, ULong, String, UByte, ULong, ULong, ULong, ULong, ULong, ULong,
            UByte> record = context
            .select(TABLE.ID, TABLE.TEMPLATE_ID, TABLE.NAME, TABLE.TYPE, TABLE.PREVIOUS_STEP_ID, TABLE.NEXT_STEP_ID,
                TABLE.ID, TABLE.SCRIPT_STEP_ID, TABLE.FILE_STEP_ID, TABLE.APPROVAL_STEP_ID, TABLE.IS_DELETED)
            .from(TABLE).where(conditions).fetchOne();
        if (record != null) {
            return DbRecordMapper.convertRecordToTaskStep(record, TaskTypeEnum.TEMPLATE);
        } else {
            return null;
        }
    }

    @Override
    public long insertStep(TaskStepDTO taskStep) {
        TaskTemplateStepRecord record = context.insertInto(TABLE)
            .columns(TABLE.TEMPLATE_ID, TABLE.NAME, TABLE.TYPE, TABLE.PREVIOUS_STEP_ID, TABLE.NEXT_STEP_ID,
                TABLE.IS_DELETED)
            .values(ULong.valueOf(taskStep.getTemplateId()), taskStep.getName(),
                UByte.valueOf(taskStep.getType().getValue()), ULong.valueOf(taskStep.getPreviousStepId()),
                ULong.valueOf(taskStep.getNextStepId()), UByte.valueOf(0))
            .returning(TABLE.ID).fetchOne();
        return record.getId().longValue();
    }

    @Override
    public boolean updateStepById(TaskStepDTO taskStep) {
        UpdateSetMoreStep<TaskTemplateStepRecord> updateStep =
            context.update(TABLE).set(TABLE.NAME, taskStep.getName());
        switch (taskStep.getType()) {
            case SCRIPT:
                if (taskStep.getScriptStepId() != null) {
                    updateStep = updateStep.set(TABLE.SCRIPT_STEP_ID, ULong.valueOf(taskStep.getScriptStepId()));
                }
                break;
            case FILE:
                if (taskStep.getFileStepId() != null) {
                    updateStep = updateStep.set(TABLE.FILE_STEP_ID, ULong.valueOf(taskStep.getFileStepId()));
                }
                break;
            case APPROVAL:
                if (taskStep.getApprovalStepId() != null) {
                    updateStep = updateStep.set(TABLE.APPROVAL_STEP_ID, ULong.valueOf(taskStep.getApprovalStepId()));
                }
                break;
            default:
                return false;
        }

        if (taskStep.getPreviousStepId() != null) {
            updateStep = updateStep.set(TABLE.PREVIOUS_STEP_ID, ULong.valueOf(taskStep.getPreviousStepId()));
        }
        if (taskStep.getNextStepId() != null) {
            updateStep = updateStep.set(TABLE.NEXT_STEP_ID, ULong.valueOf(taskStep.getNextStepId()));
        }

        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.eq(ULong.valueOf(taskStep.getId())));
        conditions.add(TABLE.TEMPLATE_ID.eq(ULong.valueOf(taskStep.getTemplateId())));
        conditions.add(TABLE.TYPE.eq(UByte.valueOf(taskStep.getType().getValue())));
        return 1 == updateStep.where(conditions).limit(1).execute();
    }

    @Override
    public boolean deleteStepById(long parentId, long id) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.eq(ULong.valueOf(id)));
        conditions.add(TABLE.TEMPLATE_ID.eq(ULong.valueOf(parentId)));
        return 1 == context.deleteFrom(TABLE).where(conditions).limit(1).execute();
    }

    @Override
    public List<Long> listStepIdByParentId(List<Long> parentIdList) {
        if (CollectionUtils.isEmpty(parentIdList)) {
            return new ArrayList<>();
        }

        List<ULong> uLongTemplateIdList =
            parentIdList.parallelStream().map(ULong::valueOf).collect(Collectors.toList());

        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.TEMPLATE_ID.in(uLongTemplateIdList));
        List<Long> stepIdList = new ArrayList<>();
        context.select(TABLE.ID).from(TABLE).where(conditions).fetch().map(record -> stepIdList.add(record.get(TABLE.ID).longValue()));
        return stepIdList;
    }
}
