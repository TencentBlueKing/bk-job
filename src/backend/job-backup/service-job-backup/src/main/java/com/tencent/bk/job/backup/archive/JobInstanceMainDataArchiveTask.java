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

package com.tencent.bk.job.backup.archive;

import com.tencent.bk.job.backup.archive.dao.JobInstanceColdDAO;
import com.tencent.bk.job.backup.archive.dao.impl.TaskInstanceRecordDAO;
import com.tencent.bk.job.backup.archive.model.JobInstanceArchiveTaskInfo;
import com.tencent.bk.job.backup.config.ArchiveProperties;
import com.tencent.bk.job.backup.metrics.ArchiveErrorTaskCounter;
import com.tencent.bk.job.execute.model.tables.TaskInstance;
import com.tencent.bk.job.execute.model.tables.records.TaskInstanceRecord;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 作业实例数据归档
 **/
@Slf4j
public class JobInstanceMainDataArchiveTask extends AbstractJobInstanceArchiveTask<TaskInstanceRecord> {

    private final TaskInstanceRecordDAO taskInstanceRecordDAO;

    private final JobInstanceSubTableArchivers jobInstanceSubTableArchivers;

    public JobInstanceMainDataArchiveTask(TaskInstanceRecordDAO taskInstanceRecordDAO,
                                          JobInstanceSubTableArchivers jobInstanceSubTableArchivers,
                                          JobInstanceColdDAO jobInstanceColdDAO,
                                          ArchiveProperties archiveProperties,
                                          ArchiveTaskLock archiveTaskLock,
                                          ArchiveErrorTaskCounter archiveErrorTaskCounter,
                                          JobInstanceArchiveTaskInfo archiveTask,
                                          ArchiveTaskService archiveTaskService,
                                          ArchiveTablePropsStorage archiveTablePropsStorage) {
        super(
            jobInstanceColdDAO,
            archiveProperties,
            archiveTaskLock,
            archiveErrorTaskCounter,
            archiveTask,
            archiveTaskService,
            archiveTablePropsStorage
        );
        this.taskInstanceRecordDAO = taskInstanceRecordDAO;
        this.jobInstanceSubTableArchivers = jobInstanceSubTableArchivers;
    }


    @Override
    protected void backupJobInstanceToColdDb(List<TaskInstanceRecord> jobInstanceRecords) {
        List<Long> jobInstanceIds =
            jobInstanceRecords.stream().map(this::extractJobInstanceId).collect(Collectors.toList());
        // 备份主表数据
        jobInstanceColdDAO.batchInsert(jobInstanceRecords, 1000);
        // 备份子表数据
        jobInstanceSubTableArchivers.getAll().forEach(tableArchiver -> {
            tableArchiver.backupRecords(jobInstanceIds);
        });
    }

    @Override
    protected void deleteJobInstanceHotData(List<Long> jobInstanceIds) {
        // 先删除子表数据
        jobInstanceSubTableArchivers.getAll().forEach(tableArchiver -> {
            tableArchiver.deleteRecords(jobInstanceIds);
        });
        // 删除主表数据
        taskInstanceRecordDAO.deleteRecords(jobInstanceIds,
            archiveTablePropsStorage.getDeleteLimitRowCount(TaskInstance.TASK_INSTANCE.getName()));
    }

    @Override
    protected List<TaskInstanceRecord> readSortedJobInstanceFromHotDB(Long fromTimestamp,
                                                                      Long endTimestamp,
                                                                      Long fromJobInstanceId,
                                                                      int limit) {
        return taskInstanceRecordDAO.readSortedJobInstanceFromHotDB(
            fromTimestamp,
            endTimestamp,
            fromJobInstanceId,
            limit
        );
    }

    @Override
    protected Long extractJobInstanceId(TaskInstanceRecord record) {
        return record.get(TaskInstance.TASK_INSTANCE.ID);
    }

    @Override
    protected Long extractJobInstanceCreateTime(TaskInstanceRecord record) {
        return record.get(TaskInstance.TASK_INSTANCE.CREATE_TIME);
    }
}
