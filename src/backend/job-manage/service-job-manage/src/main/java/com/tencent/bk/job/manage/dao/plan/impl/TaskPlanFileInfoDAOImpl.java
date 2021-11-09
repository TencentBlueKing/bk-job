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
import com.tencent.bk.job.manage.dao.TaskFileInfoDAO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep8;
import org.jooq.Record9;
import org.jooq.Result;
import org.jooq.generated.tables.TaskPlanStepFileList;
import org.jooq.generated.tables.records.TaskPlanStepFileListRecord;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @since 10/10/2019 17:34
 */
@Slf4j
@Repository("TaskPlanFileInfoDAOImpl")
public class TaskPlanFileInfoDAOImpl implements TaskFileInfoDAO {
    private static final TaskPlanStepFileList TABLE = TaskPlanStepFileList.TASK_PLAN_STEP_FILE_LIST;

    private DSLContext context;

    @Autowired
    public TaskPlanFileInfoDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext context) {
        this.context = context;
    }

    @Override
    public Map<Long, List<TaskFileInfoDTO>> listFileInfosByStepIds(List<Long> stepIdList) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.STEP_ID.in(stepIdList.stream().map(ULong::valueOf).collect(Collectors.toList())));
        Result<Record9<ULong, ULong, UByte, String, ULong, String, String, ULong, Integer>> result =
            context.select(TABLE.ID, TABLE.STEP_ID, TABLE.FILE_TYPE, TABLE.FILE_LOCATION, TABLE.FILE_SIZE,
                TABLE.FILE_HASH, TABLE.HOST,
                TABLE.HOST_ACCOUNT, TABLE.FILE_SOURCE_ID).from(TABLE).where(conditions).fetch();
        Map<Long, List<TaskFileInfoDTO>> taskFileInfoMap = new HashMap<>(stepIdList.size());
        if (result != null && result.size() >= 1) {
            result.map(record -> {
                Long stepId = record.get(TABLE.STEP_ID).longValue();
                taskFileInfoMap.computeIfAbsent(stepId, k -> new ArrayList<>());
                taskFileInfoMap.get(stepId).add(DbRecordMapper.convertRecordToTaskFileInfo(record));
                return null;
            });
        }
        return taskFileInfoMap;
    }

    @Override
    public List<TaskFileInfoDTO> listFileInfoByStepId(long stepId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.STEP_ID.eq(ULong.valueOf(stepId)));
        Result<Record9<ULong, ULong, UByte, String, ULong, String, String, ULong, Integer>> result =
            context.select(TABLE.ID, TABLE.STEP_ID, TABLE.FILE_TYPE, TABLE.FILE_LOCATION, TABLE.FILE_SIZE,
                TABLE.FILE_HASH, TABLE.HOST,
                TABLE.HOST_ACCOUNT, TABLE.FILE_SOURCE_ID).from(TABLE).where(conditions).fetch();
        List<TaskFileInfoDTO> taskFileInfoList = new ArrayList<>();
        if (result != null && result.size() >= 1) {
            result.map(record -> taskFileInfoList.add(DbRecordMapper.convertRecordToTaskFileInfo(record)));
        }
        return taskFileInfoList;
    }

    @Override
    public TaskFileInfoDTO getFileInfoById(long stepId, long fileId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.STEP_ID.eq(ULong.valueOf(stepId)));
        conditions.add(TABLE.ID.eq(ULong.valueOf(fileId)));
        Record9<ULong, ULong, UByte, String, ULong, String, String, ULong, Integer> record =
            context.select(TABLE.ID, TABLE.STEP_ID, TABLE.FILE_TYPE, TABLE.FILE_LOCATION, TABLE.FILE_SIZE,
                TABLE.FILE_HASH, TABLE.HOST,
                TABLE.HOST_ACCOUNT, TABLE.FILE_SOURCE_ID).from(TABLE).where(conditions).fetchOne();
        if (record != null) {
            return DbRecordMapper.convertRecordToTaskFileInfo(record);
        } else {
            return null;
        }
    }

    @Override
    public long insertFileInfo(TaskFileInfoDTO fileInfo) {
        TaskPlanStepFileListRecord record = context.insertInto(TABLE)
            .columns(TABLE.STEP_ID, TABLE.FILE_TYPE, TABLE.FILE_LOCATION, TABLE.FILE_SIZE, TABLE.FILE_HASH, TABLE.HOST,
                TABLE.HOST_ACCOUNT, TABLE.FILE_SOURCE_ID)
            .values(
                ULong.valueOf(fileInfo.getStepId()),
                UByte.valueOf(fileInfo.getFileType().getType()),
                JsonUtils.toJson(fileInfo.getFileLocation()),
                DbRecordMapper.getJooqLongValue(fileInfo.getFileSize()),
                fileInfo.getFileHash(),
                fileInfo.getHost() == null ? null : fileInfo.getHost().toString(),
                DbRecordMapper.getJooqLongValue(fileInfo.getHostAccount()),
                fileInfo.getFileSourceId()
            )
            .returning(TABLE.ID).fetchOne();
        return record.getId().longValue();
    }

    @Override
    public List<Long> batchInsertFileInfo(List<TaskFileInfoDTO> fileInfoList) {
        if (CollectionUtils.isEmpty(fileInfoList)) {
            return Collections.emptyList();
        }
        InsertValuesStep8<TaskPlanStepFileListRecord, ULong, UByte, String, ULong, String, String, ULong, Integer> insertStep =
            context.insertInto(TABLE).columns(TABLE.STEP_ID, TABLE.FILE_TYPE, TABLE.FILE_LOCATION, TABLE.FILE_SIZE,
                TABLE.FILE_HASH,
                TABLE.HOST, TABLE.HOST_ACCOUNT, TABLE.FILE_SOURCE_ID);

        fileInfoList.forEach(fileInfo -> insertStep.values(
            ULong.valueOf(fileInfo.getStepId()),
            UByte.valueOf(fileInfo.getFileType().getType()),
            JsonUtils.toJson(fileInfo.getFileLocation()),
            DbRecordMapper.getJooqLongValue(fileInfo.getFileSize()),
            fileInfo.getFileHash(),
            fileInfo.getHost() == null ? null : fileInfo.getHost().toString(),
            DbRecordMapper.getJooqLongValue(fileInfo.getHostAccount()),
            fileInfo.getFileSourceId()
        ));
        Result<TaskPlanStepFileListRecord> result = insertStep.returning(TABLE.ID).fetch();
        List<Long> fileInfoIdList = new ArrayList<>(fileInfoList.size());
        result.forEach(record -> fileInfoIdList.add(record.getId().longValue()));

        try {
            Iterator<TaskFileInfoDTO> fileInfoIterator = fileInfoList.iterator();
            Iterator<Long> fileInfoIdIterator = fileInfoIdList.iterator();
            while (fileInfoIterator.hasNext()) {
                TaskFileInfoDTO taskFileInfo = fileInfoIterator.next();
                taskFileInfo.setId(fileInfoIdIterator.next());
            }
        } catch (Exception e) {
            throw new InternalException(ErrorCode.BATCH_INSERT_FAILED);
        }
        return fileInfoIdList;
    }

    @Override
    public boolean updateFileInfoById(TaskFileInfoDTO fileInfo) {
        throw new InternalException(ErrorCode.UNSUPPORTED_OPERATION);
    }

    @Override
    public boolean deleteFileInfoById(long stepId, long fileId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.STEP_ID.eq(ULong.valueOf(stepId)));
        conditions.add(TABLE.ID.eq(ULong.valueOf(fileId)));
        return 1 == context.deleteFrom(TABLE).where(conditions).limit(1).execute();
    }

    @Override
    public boolean deleteFileInfosByStepId(long stepId) {
        if (stepId > 0) {
            List<Condition> conditions = new ArrayList<>();
            conditions.add(TABLE.STEP_ID.eq(ULong.valueOf(stepId)));
            return 1 <= context.deleteFrom(TABLE).where(conditions).execute();
        } else {
            return false;
        }
    }

    @Override
    public List<String> listLocalFileByStepId(List<Long> stepIdList) {
        if (CollectionUtils.isEmpty(stepIdList)) {
            return new ArrayList<>();
        }

        List<ULong> uLongStepIdList = stepIdList.parallelStream().map(ULong::valueOf).collect(Collectors.toList());

        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.STEP_ID.in(uLongStepIdList));
        conditions.add(TABLE.FILE_TYPE.equal(UByte.valueOf(2)));
        return context.select(TABLE.FILE_LOCATION).from(TABLE).where(conditions).fetch(TABLE.FILE_LOCATION);
    }
}
