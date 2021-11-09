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
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.common.util.DbRecordMapper;
import com.tencent.bk.job.manage.dao.TaskApprovalStepDAO;
import com.tencent.bk.job.manage.model.dto.task.TaskApprovalStepDTO;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record6;
import org.jooq.Result;
import org.jooq.generated.tables.TaskPlan;
import org.jooq.generated.tables.TaskPlanStep;
import org.jooq.generated.tables.TaskPlanStepApproval;
import org.jooq.generated.tables.records.TaskPlanStepApprovalRecord;
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
 * @since 3/10/2019 21:52
 */
@Slf4j
@Repository("TaskPlanApprovalStepDAOImpl")
public class TaskPlanApprovalStepDAOImpl implements TaskApprovalStepDAO {

    private static final TaskPlanStepApproval TABLE = TaskPlanStepApproval.TASK_PLAN_STEP_APPROVAL;

    private DSLContext context;

    @Autowired
    public TaskPlanApprovalStepDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext context) {
        this.context = context;
    }

    @Override
    public Map<Long, TaskApprovalStepDTO> listApprovalsByIds(List<Long> stepIdList) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.STEP_ID.in(stepIdList.stream().map(ULong::valueOf).collect(Collectors.toList())));
        Result<Record6<ULong, ULong, UByte, String, String, String>> result =
            context.select(TABLE.ID, TABLE.STEP_ID, TABLE.APPROVAL_TYPE, TABLE.APPROVAL_USER, TABLE.APPROVAL_MESSAGE,
                TABLE.NOTIFY_CHANNEL).from(TABLE).where(conditions).fetch();
        Map<Long, TaskApprovalStepDTO> approvalStepMap = new HashMap<>(stepIdList.size());
        if (result != null && result.size() >= 1) {
            result.map(record -> approvalStepMap.put(record.get(TABLE.STEP_ID).longValue(),
                DbRecordMapper.convertRecordToTaskApprovalStep(record)));
        }
        return approvalStepMap;
    }

    @Override
    public TaskApprovalStepDTO getApprovalById(long stepId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.STEP_ID.eq(ULong.valueOf(stepId)));
        Record6<ULong, ULong, UByte, String, String, String> record =
            context.select(TABLE.ID, TABLE.STEP_ID, TABLE.APPROVAL_TYPE, TABLE.APPROVAL_USER, TABLE.APPROVAL_MESSAGE,
                TABLE.NOTIFY_CHANNEL).from(TABLE).where(conditions).fetchOne();
        if (record != null) {
            return DbRecordMapper.convertRecordToTaskApprovalStep(record);
        } else {
            return null;
        }
    }

    @Override
    public long insertApproval(TaskApprovalStepDTO approvalStep) {

        TaskPlanStepApprovalRecord record = context.insertInto(TABLE)
            .columns(TABLE.STEP_ID, TABLE.APPROVAL_TYPE, TABLE.APPROVAL_USER, TABLE.APPROVAL_MESSAGE,
                TABLE.NOTIFY_CHANNEL)
            .values(ULong.valueOf(approvalStep.getStepId()), UByte.valueOf(approvalStep.getApprovalType().getType()),
                JsonUtils.toJson(approvalStep.getApprovalUser()), approvalStep.getApprovalMessage(),
                JsonUtils.toJson(approvalStep.getNotifyChannel()))
            .returning(TABLE.ID).fetchOne();

        return record.getId().longValue();
    }

    @Override
    public boolean updateApprovalById(TaskApprovalStepDTO approvalStep) {
        throw new InternalException(ErrorCode.UNSUPPORTED_OPERATION);
    }

    @Override
    public boolean deleteApprovalById(long stepId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.STEP_ID.eq(ULong.valueOf(stepId)));
        return 1 == context.deleteFrom(TABLE).where(conditions).limit(1).execute();
    }

    @Override
    public int countApprovalSteps(Long appId) {
        TaskPlan tableTaskPlan = TaskPlan.TASK_PLAN;
        TaskPlanStep tableTTStep = TaskPlanStep.TASK_PLAN_STEP;
        TaskPlanStepApproval tableTTStepApproval = TaskPlanStepApproval.TASK_PLAN_STEP_APPROVAL;
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(tableTaskPlan.APP_ID.eq(ULong.valueOf(appId)));
        }
        return context.selectCount().from(tableTTStepApproval)
            .join(tableTTStep).on(tableTTStep.ID.eq(tableTTStepApproval.STEP_ID))
            .join(tableTaskPlan).on(tableTTStep.PLAN_ID.eq(tableTaskPlan.ID))
            .where(conditions).fetchOne().value1();
    }
}
