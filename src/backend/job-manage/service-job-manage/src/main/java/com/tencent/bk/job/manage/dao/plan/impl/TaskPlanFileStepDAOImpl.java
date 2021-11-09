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
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.common.util.DbRecordMapper;
import com.tencent.bk.job.manage.dao.TaskFileStepDAO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileStepDTO;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record11;
import org.jooq.Result;
import org.jooq.generated.tables.TaskPlan;
import org.jooq.generated.tables.TaskPlanStep;
import org.jooq.generated.tables.TaskPlanStepFile;
import org.jooq.generated.tables.TaskPlanStepFileList;
import org.jooq.generated.tables.records.TaskPlanStepFileRecord;
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
@Repository("TaskPlanFileStepDAOImpl")
public class TaskPlanFileStepDAOImpl implements TaskFileStepDAO {

    private static final TaskPlanStepFile TABLE = TaskPlanStepFile.TASK_PLAN_STEP_FILE;

    private DSLContext context;

    @Autowired
    public TaskPlanFileStepDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext context) {
        this.context = context;
    }

    @Override
    public Map<Long, TaskFileStepDTO> listFileStepsByIds(List<Long> stepIdList) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.STEP_ID.in(stepIdList.stream().map(ULong::valueOf).collect(Collectors.toList())));
        Result<
            Record11<ULong, ULong, String, ULong, String, ULong, ULong, ULong, UByte, UByte,
                UByte>> result =
            context
                .select(TABLE.ID, TABLE.STEP_ID, TABLE.DESTINATION_FILE_LOCATION, TABLE.EXECUTE_ACCOUNT,
                    TABLE.DESTINATION_HOST_LIST, TABLE.TIMEOUT, TABLE.ORIGIN_SPEED_LIMIT,
                    TABLE.TARGET_SPEED_LIMIT, TABLE.IGNORE_ERROR, TABLE.DUPLICATE_HANDLER, TABLE.NOT_EXIST_PATH_HANDLER)
                .from(TABLE).where(conditions).fetch();
        Map<Long, TaskFileStepDTO> taskFileStepMap = new HashMap<>(stepIdList.size());
        if (result != null && result.size() >= 1) {
            result.map(record -> taskFileStepMap.put(record.get(TABLE.STEP_ID).longValue(),
                DbRecordMapper.convertRecordToTaskFileStep(record)));
        }
        return taskFileStepMap;
    }

    @Override
    public TaskFileStepDTO getFileStepById(long stepId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.STEP_ID.eq(ULong.valueOf(stepId)));
        Record11<ULong, ULong, String, ULong, String, ULong, ULong, ULong, UByte, UByte, UByte> record =
            context.select(TABLE.ID, TABLE.STEP_ID, TABLE.DESTINATION_FILE_LOCATION, TABLE.EXECUTE_ACCOUNT,
                TABLE.DESTINATION_HOST_LIST, TABLE.TIMEOUT, TABLE.ORIGIN_SPEED_LIMIT, TABLE.TARGET_SPEED_LIMIT,
                TABLE.IGNORE_ERROR, TABLE.DUPLICATE_HANDLER, TABLE.NOT_EXIST_PATH_HANDLER).from(TABLE).where(conditions).fetchOne();
        if (record != null) {
            return DbRecordMapper.convertRecordToTaskFileStep(record);
        } else {
            return null;
        }
    }

    @Override
    public long insertFileStep(TaskFileStepDTO fileStep) {
        UByte ignoreError = UByte.valueOf(0);
        if (fileStep.getIgnoreError() != null && fileStep.getIgnoreError()) {
            ignoreError = UByte.valueOf(1);
        }
        fixTargerLocation(fileStep);
        TaskPlanStepFileRecord record = context.insertInto(TABLE)
            .columns(TABLE.STEP_ID, TABLE.DESTINATION_FILE_LOCATION, TABLE.EXECUTE_ACCOUNT, TABLE.DESTINATION_HOST_LIST,
                TABLE.TIMEOUT, TABLE.ORIGIN_SPEED_LIMIT, TABLE.TARGET_SPEED_LIMIT, TABLE.IGNORE_ERROR,
                TABLE.DUPLICATE_HANDLER, TABLE.NOT_EXIST_PATH_HANDLER)
            .values(ULong.valueOf(fileStep.getStepId()), fileStep.getDestinationFileLocation(),
                ULong.valueOf(fileStep.getExecuteAccount()),
                fileStep.getDestinationHostList() == null ? null : fileStep.getDestinationHostList().toString(),
                fileStep.getTimeout() == null ? ULong.valueOf(0) : ULong.valueOf(fileStep.getTimeout()),
                fileStep.getOriginSpeedLimit() == null ? null : ULong.valueOf(fileStep.getOriginSpeedLimit()),
                fileStep.getTargetSpeedLimit() == null ? null : ULong.valueOf(fileStep.getTargetSpeedLimit()),
                ignoreError,
                UByte.valueOf(fileStep.getDuplicateHandler().getId()),
                UByte.valueOf(fileStep.getNotExistPathHandler().getValue())
            )
            .returning(TABLE.ID).fetchOne();
        return record.getId().longValue();
    }

    @Override
    public boolean updateFileStepById(TaskFileStepDTO fileStep) {
        throw new InternalException(ErrorCode.UNSUPPORTED_OPERATION);
    }

    @Override
    public boolean deleteFileStepById(long stepId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.STEP_ID.eq(ULong.valueOf(stepId)));
        return 1 == context.deleteFrom(TABLE).where(conditions).limit(1).execute();
    }

    @Override
    public int countFileSteps(Long appId, TaskFileTypeEnum fileType) {
        TaskPlan tableTaskPlan = TaskPlan.TASK_PLAN;
        TaskPlanStep tableTTStep = TaskPlanStep.TASK_PLAN_STEP;
        TaskPlanStepFileList tableTTStepFileList = TaskPlanStepFileList.TASK_PLAN_STEP_FILE_LIST;
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(tableTaskPlan.APP_ID.eq(ULong.valueOf(appId)));
        }
        if (fileType != null) {
            conditions.add(tableTTStepFileList.FILE_TYPE.eq(UByte.valueOf(fileType.getType())));
        }
        return context.selectCount().from(tableTTStepFileList)
            .join(tableTTStep).on(tableTTStep.ID.eq(tableTTStepFileList.STEP_ID))
            .join(tableTaskPlan).on(tableTTStep.PLAN_ID.eq(tableTaskPlan.ID))
            .where(conditions).fetchOne().value1();
    }
}
