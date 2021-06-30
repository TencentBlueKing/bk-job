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

import com.tencent.bk.job.execute.common.util.JooqDataTypeUtil;
import com.tencent.bk.job.execute.dao.FileSourceTaskLogDAO;
import com.tencent.bk.job.execute.model.FileSourceTaskLogDTO;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.UpdateSetFirstStep;
import org.jooq.UpdateSetMoreStep;
import org.jooq.generated.tables.FileSourceTaskLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class FileSourceTaskLogDAOImpl implements FileSourceTaskLogDAO {
    FileSourceTaskLog defaultTable = FileSourceTaskLog.FILE_SOURCE_TASK_LOG;
    private DSLContext defaultContext;

    @Autowired
    public FileSourceTaskLogDAOImpl(@Qualifier("job-execute-dsl-context") DSLContext defaultContext) {
        this.defaultContext = defaultContext;
    }

    @Override
    public FileSourceTaskLogDTO getStepLastExecuteLog(long stepInstanceId) {
        FileSourceTaskLog t = FileSourceTaskLog.FILE_SOURCE_TASK_LOG;
        Record result = defaultContext.select(t.STEP_INSTANCE_ID, t.EXECUTE_COUNT, t.START_TIME, t.END_TIME,
            t.TOTAL_TIME,
            t.STATUS, t.FILE_SOURCE_BATCH_TASK_ID)
            .from(t)
            .where(t.STEP_INSTANCE_ID.eq(stepInstanceId))
            .orderBy(t.EXECUTE_COUNT.desc())
            .limit(1)
            .fetchOne();
        return extractInfo(result);
    }

    private FileSourceTaskLogDTO extractInfo(Record result) {
        if (result == null || result.size() == 0) {
            return null;
        }
        FileSourceTaskLogDTO fileSourceTaskLogDTO = new FileSourceTaskLogDTO();
        FileSourceTaskLog t = FileSourceTaskLog.FILE_SOURCE_TASK_LOG;

        fileSourceTaskLogDTO.setStepInstanceId(result.get(t.STEP_INSTANCE_ID));
        fileSourceTaskLogDTO.setExecuteCount(result.get(t.EXECUTE_COUNT));
        fileSourceTaskLogDTO.setStartTime(result.get(t.START_TIME));
        fileSourceTaskLogDTO.setEndTime(result.get(t.END_TIME));
        fileSourceTaskLogDTO.setTotalTime(result.get(t.TOTAL_TIME));
        fileSourceTaskLogDTO.setStatus(result.get(t.STATUS).intValue());
        fileSourceTaskLogDTO.setFileSourceBatchTaskId(result.get(t.FILE_SOURCE_BATCH_TASK_ID));
        return fileSourceTaskLogDTO;
    }

    @Override
    public void saveFileSourceTaskLog(FileSourceTaskLogDTO fileSourceTaskLog) {
        FileSourceTaskLog t = FileSourceTaskLog.FILE_SOURCE_TASK_LOG;
        defaultContext.insertInto(t, t.STEP_INSTANCE_ID, t.EXECUTE_COUNT, t.START_TIME, t.END_TIME, t.TOTAL_TIME,
            t.STATUS, t.FILE_SOURCE_BATCH_TASK_ID)
            .values(fileSourceTaskLog.getStepInstanceId(),
                fileSourceTaskLog.getExecuteCount(),
                fileSourceTaskLog.getStartTime(),
                fileSourceTaskLog.getEndTime(),
                fileSourceTaskLog.getTotalTime(),
                JooqDataTypeUtil.getByteFromInteger(fileSourceTaskLog.getStatus()),
                fileSourceTaskLog.getFileSourceBatchTaskId())
            .onDuplicateKeyUpdate()
            .set(t.START_TIME, fileSourceTaskLog.getStartTime())
            .set(t.END_TIME, fileSourceTaskLog.getEndTime())
            .set(t.TOTAL_TIME, fileSourceTaskLog.getTotalTime())
            .set(t.STATUS, JooqDataTypeUtil.getByteFromInteger(fileSourceTaskLog.getStatus())).set(t.FILE_SOURCE_BATCH_TASK_ID, fileSourceTaskLog.getFileSourceBatchTaskId())
            .execute();
    }

    @Override
    public FileSourceTaskLogDTO getFileSourceTaskLog(long stepInstanceId, int executeCount) {
        FileSourceTaskLog t = FileSourceTaskLog.FILE_SOURCE_TASK_LOG;
        Record result = defaultContext.select(t.STEP_INSTANCE_ID, t.EXECUTE_COUNT, t.START_TIME, t.END_TIME,
            t.TOTAL_TIME,
            t.STATUS, t.FILE_SOURCE_BATCH_TASK_ID).from(t)
            .where(t.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(t.EXECUTE_COUNT.eq(executeCount))
            .fetchOne();
        return extractInfo(result);
    }

    @Override
    public FileSourceTaskLogDTO getFileSourceTaskLogByBatchTaskId(String fileSourceBatchTaskId) {
        Record result = defaultContext.select(defaultTable.STEP_INSTANCE_ID, defaultTable.EXECUTE_COUNT,
            defaultTable.START_TIME, defaultTable.END_TIME, defaultTable.TOTAL_TIME,
            defaultTable.STATUS, defaultTable.FILE_SOURCE_BATCH_TASK_ID).from(defaultTable)
            .where(defaultTable.FILE_SOURCE_BATCH_TASK_ID.eq(fileSourceBatchTaskId))
            .fetchOne();
        return extractInfo(result);
    }

    @Override
    public int updateTimeConsumingByBatchTaskId(String fileSourceBatchTaskId, Long startTime, Long endTime,
                                                Long totalTime) {
        UpdateSetFirstStep firstStep = defaultContext.update(defaultTable);
        UpdateSetMoreStep moreStep = null;
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
            return moreStep.where(defaultTable.FILE_SOURCE_BATCH_TASK_ID.eq(fileSourceBatchTaskId))
                .execute();
        } else {
            return 0;
        }
    }

    @Override
    public void deleteFileSourceTaskLog(long stepInstanceId, int executeCount) {
        FileSourceTaskLog t = FileSourceTaskLog.FILE_SOURCE_TASK_LOG;
        defaultContext.deleteFrom(t).where(t.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(t.EXECUTE_COUNT.eq(executeCount))
            .execute();
    }

}
