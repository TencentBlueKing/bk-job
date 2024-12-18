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

package com.tencent.bk.job.execute.dao.impl;

import com.tencent.bk.job.common.mysql.dynamic.ds.DbOperationEnum;
import com.tencent.bk.job.common.mysql.dynamic.ds.MySQLOperation;
import com.tencent.bk.job.common.mysql.jooq.JooqDataTypeUtil;
import com.tencent.bk.job.execute.dao.FileSourceTaskLogDAO;
import com.tencent.bk.job.execute.dao.common.DSLContextProviderFactory;
import com.tencent.bk.job.execute.model.FileSourceTaskLogDTO;
import com.tencent.bk.job.execute.model.tables.FileSourceTaskLog;
import com.tencent.bk.job.execute.model.tables.records.FileSourceTaskLogRecord;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.UpdateSetFirstStep;
import org.jooq.UpdateSetMoreStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class FileSourceTaskLogDAOImpl extends BaseDAO implements FileSourceTaskLogDAO {

    private static final FileSourceTaskLog defaultTable = FileSourceTaskLog.FILE_SOURCE_TASK_LOG;

    private static final TableField<?, ?>[] ALL_FIELDS = {
        defaultTable.ID,
        defaultTable.TASK_INSTANCE_ID,
        defaultTable.STEP_INSTANCE_ID,
        defaultTable.EXECUTE_COUNT,
        defaultTable.START_TIME,
        defaultTable.END_TIME,
        defaultTable.TOTAL_TIME,
        defaultTable.STATUS,
        defaultTable.FILE_SOURCE_BATCH_TASK_ID
    };


    @Autowired
    public FileSourceTaskLogDAOImpl(DSLContextProviderFactory dslContextProviderFactory) {
        super(dslContextProviderFactory, defaultTable.getName());
    }

    private FileSourceTaskLogDTO extractInfo(Record record) {
        if (record == null) {
            return null;
        }
        FileSourceTaskLogDTO fileSourceTaskLogDTO = new FileSourceTaskLogDTO();
        FileSourceTaskLog t = FileSourceTaskLog.FILE_SOURCE_TASK_LOG;

        fileSourceTaskLogDTO.setId(record.get(t.ID));
        fileSourceTaskLogDTO.setTaskInstanceId(record.get(t.TASK_INSTANCE_ID));
        fileSourceTaskLogDTO.setStepInstanceId(record.get(t.STEP_INSTANCE_ID));
        fileSourceTaskLogDTO.setExecuteCount(record.get(t.EXECUTE_COUNT));
        fileSourceTaskLogDTO.setStartTime(record.get(t.START_TIME));
        fileSourceTaskLogDTO.setEndTime(record.get(t.END_TIME));
        fileSourceTaskLogDTO.setTotalTime(record.get(t.TOTAL_TIME));
        fileSourceTaskLogDTO.setStatus(record.get(t.STATUS).intValue());
        fileSourceTaskLogDTO.setFileSourceBatchTaskId(record.get(t.FILE_SOURCE_BATCH_TASK_ID));
        return fileSourceTaskLogDTO;
    }

    @Override
    @MySQLOperation(table = "file_source_task_log", op = DbOperationEnum.WRITE)
    public int insertFileSourceTaskLog(FileSourceTaskLogDTO fileSourceTaskLog) {
        FileSourceTaskLog t = FileSourceTaskLog.FILE_SOURCE_TASK_LOG;
        return dsl().insertInto(
            t,
            t.ID,
            t.TASK_INSTANCE_ID,
            t.STEP_INSTANCE_ID,
            t.EXECUTE_COUNT,
            t.START_TIME,
            t.END_TIME,
            t.TOTAL_TIME,
            t.STATUS,
            t.FILE_SOURCE_BATCH_TASK_ID
        ).values(
            fileSourceTaskLog.getId(),
            fileSourceTaskLog.getTaskInstanceId(),
            fileSourceTaskLog.getStepInstanceId(),
            fileSourceTaskLog.getExecuteCount(),
            fileSourceTaskLog.getStartTime(),
            fileSourceTaskLog.getEndTime(),
            fileSourceTaskLog.getTotalTime(),
            JooqDataTypeUtil.toByte(fileSourceTaskLog.getStatus()),
            fileSourceTaskLog.getFileSourceBatchTaskId()
        ).execute();
    }

    @Override
    @MySQLOperation(table = "file_source_task_log", op = DbOperationEnum.WRITE)
    public int updateFileSourceTaskLogByStepInstance(FileSourceTaskLogDTO fileSourceTaskLog) {
        List<Condition> conditionList = new ArrayList<>();
        conditionList.add(defaultTable.TASK_INSTANCE_ID.eq(fileSourceTaskLog.getTaskInstanceId()));
        conditionList.add(defaultTable.STEP_INSTANCE_ID.eq(fileSourceTaskLog.getStepInstanceId()));
        conditionList.add(defaultTable.EXECUTE_COUNT.eq(fileSourceTaskLog.getExecuteCount()));
        return dsl().update(defaultTable)
            .set(defaultTable.START_TIME, fileSourceTaskLog.getStartTime())
            .set(defaultTable.END_TIME, fileSourceTaskLog.getEndTime())
            .set(defaultTable.TOTAL_TIME, fileSourceTaskLog.getTotalTime())
            .set(defaultTable.STATUS, JooqDataTypeUtil.toByte(fileSourceTaskLog.getStatus()))
            .set(defaultTable.FILE_SOURCE_BATCH_TASK_ID, fileSourceTaskLog.getFileSourceBatchTaskId())
            .where(conditionList)
            .limit(1)
            .execute();
    }

    @Override
    @MySQLOperation(table = "file_source_task_log", op = DbOperationEnum.READ)
    public FileSourceTaskLogDTO getFileSourceTaskLog(Long taskInstanceId, long stepInstanceId, int executeCount) {
        FileSourceTaskLog t = FileSourceTaskLog.FILE_SOURCE_TASK_LOG;
        Record record = dsl().select(
                ALL_FIELDS
            ).from(t)
            .where(t.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(buildTaskInstanceIdQueryCondition(taskInstanceId))
            .and(t.EXECUTE_COUNT.eq(executeCount))
            .fetchOne();
        return extractInfo(record);
    }

    private Condition buildTaskInstanceIdQueryCondition(Long taskInstanceId) {
        return TaskInstanceIdDynamicCondition.build(
            taskInstanceId,
            FileSourceTaskLog.FILE_SOURCE_TASK_LOG.TASK_INSTANCE_ID::eq
        );
    }

    @Override
    @MySQLOperation(table = "file_source_task_log", op = DbOperationEnum.READ)
    public FileSourceTaskLogDTO getFileSourceTaskLogByBatchTaskId(Long taskInstanceId, String fileSourceBatchTaskId) {
        Record record = dsl().select(ALL_FIELDS)
            .from(defaultTable)
            .where(defaultTable.FILE_SOURCE_BATCH_TASK_ID.eq(fileSourceBatchTaskId))
            .and(buildTaskInstanceIdQueryCondition(taskInstanceId))
            .fetchOne();
        return extractInfo(record);
    }

    @Override
    @MySQLOperation(table = "file_source_task_log", op = DbOperationEnum.WRITE)
    public int updateTimeConsumingByBatchTaskId(Long taskInstanceId,
                                                String fileSourceBatchTaskId,
                                                Long startTime,
                                                Long endTime,
                                                Long totalTime) {
        UpdateSetFirstStep<FileSourceTaskLogRecord> firstStep = dsl().update(defaultTable);
        UpdateSetMoreStep<?> moreStep = null;
        if (startTime != null) {
            moreStep = firstStep.set(defaultTable.START_TIME, startTime);
        }
        if (endTime != null) {
            if (moreStep != null) {
                moreStep = moreStep.set(defaultTable.END_TIME, endTime);
            } else {
                moreStep = firstStep.set(defaultTable.END_TIME, endTime);
            }
        }
        if (totalTime != null) {
            if (moreStep != null) {
                moreStep = moreStep.set(defaultTable.TOTAL_TIME, totalTime);
            } else {
                moreStep = firstStep.set(defaultTable.TOTAL_TIME, totalTime);
            }
        }
        if (moreStep != null) {
            return moreStep.where(
                    defaultTable.FILE_SOURCE_BATCH_TASK_ID.eq(fileSourceBatchTaskId))
                .and(buildTaskInstanceIdQueryCondition(taskInstanceId))
                .execute();
        } else {
            return 0;
        }
    }

}
