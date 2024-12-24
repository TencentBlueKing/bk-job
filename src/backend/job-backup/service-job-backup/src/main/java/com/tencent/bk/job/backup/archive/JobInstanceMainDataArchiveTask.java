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
import com.tencent.bk.job.backup.archive.dao.impl.JobInstanceHotRecordDAO;
import com.tencent.bk.job.backup.archive.model.BackupResult;
import com.tencent.bk.job.backup.archive.model.DeleteResult;
import com.tencent.bk.job.backup.archive.model.JobInstanceArchiveTaskInfo;
import com.tencent.bk.job.backup.archive.model.TablesBackupResult;
import com.tencent.bk.job.backup.archive.model.TablesDeleteResult;
import com.tencent.bk.job.backup.archive.service.ArchiveTaskService;
import com.tencent.bk.job.backup.archive.util.lock.ArchiveTaskExecuteLock;
import com.tencent.bk.job.backup.config.ArchiveProperties;
import com.tencent.bk.job.backup.metrics.ArchiveErrorTaskCounter;
import com.tencent.bk.job.execute.model.tables.records.TaskInstanceRecord;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 作业实例数据归档
 **/
@Slf4j
public class JobInstanceMainDataArchiveTask extends AbstractJobInstanceArchiveTask<TaskInstanceRecord> {

    private final JobInstanceSubTableArchivers jobInstanceSubTableArchivers;

    public JobInstanceMainDataArchiveTask(JobInstanceHotRecordDAO jobInstanceHotRecordDAO,
                                          JobInstanceSubTableArchivers jobInstanceSubTableArchivers,
                                          JobInstanceColdDAO jobInstanceColdDAO,
                                          ArchiveProperties archiveProperties,
                                          ArchiveTaskExecuteLock archiveTaskExecuteLock,
                                          ArchiveErrorTaskCounter archiveErrorTaskCounter,
                                          JobInstanceArchiveTaskInfo archiveTask,
                                          ArchiveTaskService archiveTaskService,
                                          ArchiveTablePropsStorage archiveTablePropsStorage) {
        super(
            jobInstanceHotRecordDAO,
            jobInstanceColdDAO,
            archiveProperties,
            archiveTaskExecuteLock,
            archiveErrorTaskCounter,
            archiveTask,
            archiveTaskService,
            archiveTablePropsStorage
        );
        this.jobInstanceSubTableArchivers = jobInstanceSubTableArchivers;
    }


    @Override
    protected TablesBackupResult backupJobInstanceToColdDb(List<TaskInstanceRecord> jobInstanceRecords) {
        TablesBackupResult tablesBackupResult = new TablesBackupResult();

        List<Long> jobInstanceIds =
            jobInstanceRecords.stream().map(this::extractJobInstanceId).collect(Collectors.toList());
        // 备份主表数据
        BackupResult primaryTableBackupResult =
            backupPrimaryTableRecord(jobInstanceRecords);
        tablesBackupResult.add(jobInstanceMainRecordDAO.getTable().getName(), primaryTableBackupResult);

        // 备份子表数据
        jobInstanceSubTableArchivers.getAll().forEach(tableArchiver -> {
            BackupResult backupResult = tableArchiver.backupRecords(jobInstanceIds);
            tablesBackupResult.add(tableArchiver.getTableName(), backupResult);
        });
        return tablesBackupResult;
    }

    private BackupResult backupPrimaryTableRecord(List<TaskInstanceRecord> jobInstanceRecords) {
        // 备份主表数据
        long startTime = System.currentTimeMillis();
        long backupRows = jobInstanceColdDAO.batchInsert(jobInstanceRecords,
            archiveTablePropsStorage.getBatchInsertRowSize(jobInstanceMainRecordDAO.getTable().getName()));
        long cost = System.currentTimeMillis() - startTime;
        ArchiveTaskContextHolder.get().accumulateTableBackup(
            jobInstanceMainRecordDAO.getTable().getName(),
            backupRows,
            cost
        );
        return new BackupResult(backupRows, cost);
    }

    @Override
    protected TablesDeleteResult deleteJobInstanceHotData(List<Long> jobInstanceIds) {
        TablesDeleteResult tablesDeleteResult = new TablesDeleteResult();
        long startTime = System.currentTimeMillis();
        // 先删除子表数据
        jobInstanceSubTableArchivers.getAll().forEach(tableArchiver -> {
            DeleteResult deleteResult = tableArchiver.deleteRecords(jobInstanceIds);
            tablesDeleteResult.add(tableArchiver.getTableName(), deleteResult);
        });
        // 删除主表数据
        DeleteResult primaryTableDeleteResult = deletePrimaryTableRecord(jobInstanceIds);
        tablesDeleteResult.add(jobInstanceMainRecordDAO.getTable().getName(), primaryTableDeleteResult);

        log.info("Delete {}, taskInstanceIdSize: {}, cost: {}ms", "task_instance",
            jobInstanceIds.size(), System.currentTimeMillis() - startTime);

        return tablesDeleteResult;
    }

    private DeleteResult deletePrimaryTableRecord(List<Long> jobInstanceIds) {
        long startTime = System.currentTimeMillis();
        String tableName = jobInstanceMainRecordDAO.getTable().getName();
        int deleteRows = jobInstanceMainRecordDAO.deleteRecords(jobInstanceIds,
            archiveTablePropsStorage.getDeleteLimitRowCount(tableName));
        long cost = System.currentTimeMillis() - startTime;
        ArchiveTaskContextHolder.get().accumulateTableDelete(
            tableName,
            deleteRows,
            cost
        );
        return new DeleteResult(deleteRows, cost);
    }
}
