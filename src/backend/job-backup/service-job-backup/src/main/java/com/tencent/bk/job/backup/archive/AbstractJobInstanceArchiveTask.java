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
import com.tencent.bk.job.backup.archive.dao.impl.AbstractJobInstanceMainHotRecordDAO;
import com.tencent.bk.job.backup.archive.model.ArchiveTaskInfo;
import com.tencent.bk.job.backup.archive.model.BackupResult;
import com.tencent.bk.job.backup.archive.model.DeleteResult;
import com.tencent.bk.job.backup.archive.model.IdBasedArchiveProcess;
import com.tencent.bk.job.backup.archive.model.TablesBackupResult;
import com.tencent.bk.job.backup.archive.model.TablesDeleteResult;
import com.tencent.bk.job.backup.archive.service.ArchiveTaskService;
import com.tencent.bk.job.backup.archive.util.lock.ArchiveTaskExecuteLock;
import com.tencent.bk.job.backup.config.ArchiveProperties;
import com.tencent.bk.job.backup.constant.ArchiveModeEnum;
import com.tencent.bk.job.backup.constant.ArchiveTaskStatusEnum;
import com.tencent.bk.job.backup.metrics.ArchiveErrorTaskCounter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.TableRecord;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 作业实例数据归档任务基础实现
 *
 * @param <T> 表记录
 */
@Slf4j
public abstract class AbstractJobInstanceArchiveTask<T extends TableRecord<?>>
    extends AbstractHistoricalDataArchiveTask {

    /**
     * 作业实例主表 DAO
     */
    protected AbstractJobInstanceMainHotRecordDAO<T> jobInstanceMainRecordDAO;
    /**
     * 冷 DB DAO
     */
    protected JobInstanceColdDAO jobInstanceColdDAO;

    private final ArchiveTaskExecuteLock archiveTaskExecuteLock;
    protected final ArchiveTablePropsStorage archiveTablePropsStorage;

    public AbstractJobInstanceArchiveTask(AbstractJobInstanceMainHotRecordDAO<T> jobInstanceMainRecordDAO,
                                          JobInstanceColdDAO jobInstanceColdDAO,
                                          ArchiveProperties archiveProperties,
                                          ArchiveTaskExecuteLock archiveTaskExecuteLock,
                                          ArchiveErrorTaskCounter archiveErrorTaskCounter,
                                          ArchiveTaskInfo archiveTaskInfo,
                                          ArchiveTaskService archiveTaskService,
                                          ArchiveTablePropsStorage archiveTablePropsStorage) {
        super(archiveProperties, archiveErrorTaskCounter, archiveTaskInfo, archiveTaskService);
        this.jobInstanceMainRecordDAO = jobInstanceMainRecordDAO;
        this.jobInstanceColdDAO = jobInstanceColdDAO;
        this.archiveTaskExecuteLock = archiveTaskExecuteLock;
        this.archiveTablePropsStorage = archiveTablePropsStorage;
    }

    @Override
    public void backupAndDelete() {
        boolean backupEnabled = isBackupEnable();
        boolean deleteEnabled = isDeleteEnable();
        // 获取主表对应的 readRowLimit
        int readLimit = archiveTablePropsStorage.getReadRowLimit(jobInstanceMainRecordDAO.getTable().getName());
        long archivedJobInstanceCount = 0;

        long startTime = System.currentTimeMillis();
        log.info("[{}] Archive task mode: {}, backupEnabled: {}, deleteEnabled: {}",
            taskId, archiveProperties.getMode(), backupEnabled, deleteEnabled);
        try {
            List<T> jobInstanceRecords;
            do {
                // 检查任务终止标识
                if (checkStopFlag()) {
                    stopTask();
                }

                jobInstanceRecords = readJobInstanceRecords(readLimit);
                if (CollectionUtils.isEmpty(jobInstanceRecords)) {
                    long archiveCost = System.currentTimeMillis() - startTime;
                    setArchiveTaskExecutionDetail(archivedJobInstanceCount, archiveCost, null);
                    updateCompletedExecuteInfo(ArchiveTaskStatusEnum.SUCCESS, null);
                    return;
                }
                archivedJobInstanceCount += jobInstanceRecords.size();

                List<Long> jobInstanceIds =
                    jobInstanceRecords.stream().map(this::extractJobInstanceId).collect(Collectors.toList());

                TablesBackupResult tablesBackupResult = null;
                TablesDeleteResult tablesDeleteResult = null;
                // 写入数据到冷 db
                if (backupEnabled) {
                    long backupStartTime = System.currentTimeMillis();
                    tablesBackupResult = backupJobInstanceToColdDb(jobInstanceRecords);
                    log.info("[{}] Backup to cold db, jobInstanceRecordSize: {}, cost: {}",
                        taskId, jobInstanceRecords.size(), System.currentTimeMillis() - backupStartTime);
                }
                // 从热 db 删除数据
                if (deleteEnabled) {
                    long deleteStartTime = System.currentTimeMillis();
                    tablesDeleteResult = deleteJobInstanceHotData(jobInstanceIds);
                    log.info("[{}] Delete hot db, jobInstanceRecordSize: {}, cost: {}",
                        taskId, jobInstanceRecords.size(), System.currentTimeMillis() - deleteStartTime);
                }

                checkBackupDeleteDataQuantity(tablesBackupResult, tablesDeleteResult);

                // 更新归档进度
                T lastRecord = jobInstanceRecords.get(jobInstanceRecords.size() - 1);
                Long lastJobInstanceId = extractJobInstanceId(lastRecord);
                IdBasedArchiveProcess progress = new IdBasedArchiveProcess(lastJobInstanceId);
                boolean isFinished = jobInstanceRecords.size() < readLimit;
                if (isFinished) {
                    // 更新任务结束信息
                    long archiveCost = System.currentTimeMillis() - startTime;
                    setArchiveTaskExecutionDetail(archivedJobInstanceCount, archiveCost, null);
                    updateCompletedExecuteInfo(ArchiveTaskStatusEnum.SUCCESS, progress);
                } else {
                    // 更新任务运行信息
                    updateRunningExecuteInfo(progress);
                }
            } while (jobInstanceRecords.size() == readLimit);
        } finally {
            long archiveCost = System.currentTimeMillis() - startTime;
            setArchiveTaskExecutionDetail(archivedJobInstanceCount, archiveCost, null);
        }
    }

    /**
     * 检查备份与删除的数据数量
     */
    private void checkBackupDeleteDataQuantity(TablesBackupResult tablesBackupResult,
                                               TablesDeleteResult tablesDeleteResult) {
        if (tablesBackupResult == null || tablesDeleteResult == null) {
            // 无需比较
            return;
        }
        if (!tablesBackupResult.getTables().keySet().equals(tablesDeleteResult.getTables().keySet())) {
            log.error("Backup tables are not equals delete tables, backupTables: {}, deleteTables: {}",
                tablesBackupResult.getTables().keySet(), tablesDeleteResult.getTables().keySet());
            throw new ArchiveException("Backup and delete table count not match");
        }
        boolean isBackupDeleteRowsMatch = true;
        for (Map.Entry<String, BackupResult> entry : tablesBackupResult.getTables().entrySet()) {
            String tableName = entry.getKey();
            BackupResult backupResult = entry.getValue();
            DeleteResult deleteResult = tablesDeleteResult.getTables().get(tableName);
            if (backupResult == BackupResult.NON_OP_BACKUP_RESULT) {
                // 无需归档的表，无需比较
                continue;
            }
            if (backupResult.getBackupRows() != deleteResult.getDeletedRows()) {
                log.error("Backup rows and delete row not match, table: {}, backupRows: {}, deleteRows: {}",
                    tableName, backupResult.getBackupRows(), deleteResult.getDeletedRows());
                isBackupDeleteRowsMatch = false;
            }
        }
        if (!isBackupDeleteRowsMatch) {
            throw new ArchiveException("Backup and delete row count not match");
        }
    }

    private List<T> readJobInstanceRecords(int readLimit) {
        List<T> jobInstanceRecords;
        long readStartTime = System.currentTimeMillis();

        Long fromTaskInstanceId = progress != null ? progress.getId() : null;
        jobInstanceRecords = jobInstanceMainRecordDAO.readSortedJobInstanceFromHotDB(
            archiveTaskInfo.getFromTimestamp(),
            archiveTaskInfo.getToTimestamp(),
            fromTaskInstanceId,
            readLimit);
        long cost = System.currentTimeMillis() - readStartTime;
        log.info("[{}] Read sorted job instance from hot db, fromJobCreateTime: {}, toJobCreatTime: {}, " +
                "fromJobInstanceId: {}, recordSize: {}, cost: {} ms",
            taskId,
            archiveTaskInfo.getFromTimestamp(),
            archiveTaskInfo.getToTimestamp(),
            fromTaskInstanceId,
            jobInstanceRecords.size(),
            cost
        );
        if (cost > 1000L) {
            log.info("[{}] SlowQuery-ReadJobInstanceRecords, cost: {}ms", taskId, cost);
        }
        return jobInstanceRecords;
    }

    /**
     * 备份作业实例数据到冷存储
     *
     * @param jobInstances 作业实例列表
     */
    protected abstract TablesBackupResult backupJobInstanceToColdDb(List<T> jobInstances);

    /**
     * 删除作业实例热数据
     *
     * @param jobInstanceIds 作业实例 ID 列表
     */
    protected abstract TablesDeleteResult deleteJobInstanceHotData(List<Long> jobInstanceIds);

    @Override
    public boolean acquireLock() {
        this.isAcquireLock = archiveTaskExecuteLock.lock(taskId);
        if (!isAcquireLock) {
            log.info("{} [{}] Acquire archive task lock fail", getClass().getSimpleName(), taskId);
        }
        return isAcquireLock;
    }

    @Override
    protected void unlock() {
        archiveTaskExecuteLock.unlock(taskId);
    }

    private void updateRunningExecuteInfo(IdBasedArchiveProcess process) {
        archiveTaskInfo.setProcess(process);

        if (!checkUpdateEnabled()) {
            return;
        }

        archiveTaskService.updateRunningExecuteInfo(
            archiveTaskInfo.getTaskType(),
            archiveTaskInfo.getDbDataNode(),
            archiveTaskInfo.getDay(),
            archiveTaskInfo.getHour(),
            process
        );
    }

    /**
     * 从作业实例记录中提取作业实例 ID
     *
     * @param record 作业实例记录
     */
    protected Long extractJobInstanceId(T record) {
        return record.get(jobInstanceMainRecordDAO.getJobInstanceIdField());
    }

    protected boolean isBackupEnable() {
        ArchiveModeEnum archiveMode = ArchiveModeEnum.valOf(archiveProperties.getMode());
        return archiveProperties.isEnabled()
            && (ArchiveModeEnum.BACKUP_THEN_DELETE == archiveMode || ArchiveModeEnum.BACKUP_ONLY == archiveMode);
    }

    protected boolean isDeleteEnable() {
        ArchiveModeEnum archiveMode = ArchiveModeEnum.valOf(archiveProperties.getMode());
        return archiveProperties.isEnabled()
            && (ArchiveModeEnum.BACKUP_THEN_DELETE == archiveMode || ArchiveModeEnum.DELETE_ONLY == archiveMode);
    }
}
