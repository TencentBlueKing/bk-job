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

package com.tencent.bk.job.backup.archive.impl;

import com.tencent.bk.job.backup.archive.ArchiveTablePropsStorage;
import com.tencent.bk.job.backup.archive.ArchiveTaskContextHolder;
import com.tencent.bk.job.backup.archive.JobInstanceSubTableArchiver;
import com.tencent.bk.job.backup.archive.dao.JobInstanceColdDAO;
import com.tencent.bk.job.backup.archive.dao.impl.AbstractJobInstanceHotRecordDAO;
import com.tencent.bk.job.backup.archive.dao.resultset.RecordResultSet;
import com.tencent.bk.job.backup.archive.model.BackupResult;
import com.tencent.bk.job.backup.archive.model.DeleteResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.TableRecord;

import java.util.List;

@Slf4j
public class AbstractJobInstanceSubTableArchiver implements JobInstanceSubTableArchiver {

    private final JobInstanceColdDAO jobInstanceColdDAO;

    private final AbstractJobInstanceHotRecordDAO<? extends TableRecord<?>> jobInstanceHotRecordDAO;

    private final ArchiveTablePropsStorage archiveTablePropsStorage;

    protected String tableName;

    public AbstractJobInstanceSubTableArchiver(
        JobInstanceColdDAO jobInstanceColdDAO,
        AbstractJobInstanceHotRecordDAO<? extends TableRecord<?>> jobInstanceHotRecordDAO,
        ArchiveTablePropsStorage archiveTablePropsStorage
    ) {
        this.jobInstanceColdDAO = jobInstanceColdDAO;
        this.jobInstanceHotRecordDAO = jobInstanceHotRecordDAO;
        this.archiveTablePropsStorage = archiveTablePropsStorage;
        this.tableName = jobInstanceHotRecordDAO.getTable().getName();
    }

    @Override
    public BackupResult backupRecords(List<Long> jobInstanceIds) {
        long startTime = System.currentTimeMillis();
        long backupRows = 0;

        RecordResultSet<? extends TableRecord<?>> recordResultSet =
            jobInstanceHotRecordDAO.executeQuery(jobInstanceIds,
                archiveTablePropsStorage.getReadRowLimit(tableName));
        // 数据偏移量
        int offset = 0;
        long readStartTime = System.currentTimeMillis();
        while (recordResultSet.next()) {
            List<? extends TableRecord<?>> records = recordResultSet.getRecords();
            long readEndTime = System.currentTimeMillis();
            int rowSize = CollectionUtils.isEmpty(records) ? 0 : records.size();
            long readCost = readEndTime - readStartTime;
            log.info("[{}] Read {}, offset[{}-{}], readRows: {}, cost: {}ms",
                ArchiveTaskContextHolder.getArchiveTaskId(),
                tableName,
                offset + 1,
                offset + rowSize,
                rowSize,
                readCost
            );
            if (readCost > 1000L) {
                log.info("[{}] SlowQuery-ReadBackupRecords, table: {}, cost: {}ms",
                    ArchiveTaskContextHolder.getArchiveTaskId(), tableName, readCost);
            }
            if (CollectionUtils.isNotEmpty(records)) {
                jobInstanceColdDAO.batchInsert(records,
                    archiveTablePropsStorage.getBatchInsertRowSize(tableName));
                backupRows += records.size();
            }

            readStartTime = System.currentTimeMillis();
            offset += rowSize;
        }

        long costTime = System.currentTimeMillis() - startTime;
        ArchiveTaskContextHolder.get().accumulateTableBackup(tableName, backupRows, costTime);
        return new BackupResult(backupRows, costTime);
    }

    @Override
    public DeleteResult deleteRecords(List<Long> jobInstanceIds) {
        long startTime = System.currentTimeMillis();
        int deleteRows = jobInstanceHotRecordDAO.deleteRecords(jobInstanceIds,
            archiveTablePropsStorage.getDeleteLimitRowCount(tableName));
        log.info("[{}] Delete {}, deletedRows: {}, cost: {}ms",
            ArchiveTaskContextHolder.getArchiveTaskId(), tableName, deleteRows,
            System.currentTimeMillis() - startTime);

        long costTime = System.currentTimeMillis() - startTime;
        ArchiveTaskContextHolder.get().accumulateTableDelete(tableName, deleteRows, costTime);
        return new DeleteResult(deleteRows, costTime);
    }

    @Override
    public String getTableName() {
        return tableName;
    }
}
