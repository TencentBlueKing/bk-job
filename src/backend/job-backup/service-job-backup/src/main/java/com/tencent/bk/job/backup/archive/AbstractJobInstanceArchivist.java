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
import com.tencent.bk.job.backup.archive.dao.JobInstanceHotRecordDAO;
import com.tencent.bk.job.backup.archive.model.ArchiveTaskSummary;
import com.tencent.bk.job.backup.archive.model.HourArchiveTask;
import com.tencent.bk.job.backup.config.ArchiveDBProperties;
import com.tencent.bk.job.backup.constant.ArchiveModeEnum;
import com.tencent.bk.job.backup.constant.ArchiveTaskStatusEnum;
import com.tencent.bk.job.backup.metrics.ArchiveErrorTaskCounter;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.TableRecord;
import org.slf4j.helpers.MessageFormatter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 表归档基础实现
 *
 * @param <T> 表记录
 */
@Data
@Slf4j
public abstract class AbstractJobInstanceArchivist<T extends TableRecord<?>> {
    protected JobInstanceHotRecordDAO<T> jobInstanceHotRecordDAO;
    protected JobInstanceColdDAO jobInstanceColdDAO;
    private ArchiveDBProperties.ArchiveTaskProperties archiveTaskProperties;
    private ArchiveTaskLock archiveTaskLock;
    private final ArchiveErrorTaskCounter archiveErrorTaskCounter;
    private final ArchiveTaskService archiveTaskService;

    protected String taskId;
    protected DbDataNode dbDataNode;
    protected HourArchiveTask archiveTask;

    protected Map<String, TableReadWriteProps> tableReadWritePropsMap;

    /**
     * 归档进度
     */
    private TimeAndIdBasedArchiveProcess progress;

    private ArchiveTaskSummary archiveTaskSummary;

    private boolean isAcquireLock;


    public AbstractJobInstanceArchivist(JobInstanceHotRecordDAO<T> jobInstanceHotRecordDAO,
                                        JobInstanceColdDAO jobInstanceColdDAO,
                                        ArchiveDBProperties.ArchiveTaskProperties archiveTaskProperties,
                                        ArchiveTaskLock archiveTaskLock,
                                        ArchiveErrorTaskCounter archiveErrorTaskCounter,
                                        HourArchiveTask archiveTask,
                                        ArchiveTaskService archiveTaskService) {
        this.jobInstanceHotRecordDAO = jobInstanceHotRecordDAO;
        this.jobInstanceColdDAO = jobInstanceColdDAO;
        this.archiveTaskProperties = archiveTaskProperties;
        this.archiveTaskLock = archiveTaskLock;
        this.archiveErrorTaskCounter = archiveErrorTaskCounter;
        this.archiveTask = archiveTask;
        this.progress = archiveTask.getProcess();
        this.taskId = buildTaskId(archiveTask);
        this.archiveTaskSummary = new ArchiveTaskSummary(archiveTask, archiveTaskProperties.getMode());
        this.archiveTaskService = archiveTaskService;
    }

    private String buildTaskId(HourArchiveTask archiveTask) {
        return archiveTask.getTaskType() + ":" + archiveTask.getDbDataNode().toDataNodeId()
            + ":" + archiveTask.getDay() + ":" + archiveTask.getHour();
    }

    public void archive() {
        try {
            if (!acquireLock()) {
                archiveTaskSummary.setSkip(!isAcquireLock);
                return;
            }

            log.info("[{}] Start archive task", taskId);
            // 归档
            backupAndDelete();
        } catch (Throwable e) {
            String msg = MessageFormatter.format(
                "Error while execute archive task : {}",
                taskId
            ).getMessage();
            log.error(msg, e);
            archiveErrorTaskCounter.increment();
            archiveTaskSummary.setMessage(e.getMessage());
            // 更新归档任务状态
            updateArchiveProgress(ArchiveTaskStatusEnum.FAIL, null);
        } finally {
            if (this.isAcquireLock) {
                archiveTaskLock.unlock(taskId);
            }
            log.info(
                "[{}] Archive finished, result: {}",
                taskId,
                JsonUtils.toJson(archiveTaskSummary)
            );
        }
    }

    private void backupAndDelete() {
        boolean backupEnabled = isBackupEnable();
        boolean deleteEnabled = isDeleteEnable();

        int readLimit = 1000;
        long archivedRecordCount = 0;

        long startTime = System.currentTimeMillis();
        log.info("[{}] Archive task mode: {}, backupEnabled: {}, deleteEnabled: {}",
            taskId, archiveTaskProperties.getMode(), backupEnabled, deleteEnabled);
        try {
            List<T> jobInstanceRecords;
            do {
                if (progress != null) {
                    jobInstanceRecords = listSortedJobInstance(progress.getTimestamp(),
                        archiveTask.getToTimestamp(), progress.getId(), readLimit);
                } else {
                    jobInstanceRecords = listSortedJobInstance(archiveTask.getFromTimestamp(),
                        archiveTask.getToTimestamp(), null, readLimit);
                }

                if (CollectionUtils.isEmpty(jobInstanceRecords)) {
                    updateArchiveProgress(ArchiveTaskStatusEnum.SUCCESS, null);
                    return;
                }
                archivedRecordCount++;

                List<Long> jobInstanceIds =
                    jobInstanceRecords.stream().map(this::extractJobInstanceId).collect(Collectors.toList());

                // 写入数据到冷 db
                if (backupEnabled) {
                    backupJobInstanceToColdDb(jobInstanceIds);
                }
                // 从热 db 删除数据
                if (deleteEnabled) {
                    deleteJobInstanceHotData(jobInstanceIds);
                }

                // 更新归档进度
                T lastRecord = jobInstanceRecords.get(jobInstanceRecords.size() - 1);
                Long lastTimestamp = extractJobInstanceCreateTime(lastRecord);
                Long lastJobInstanceId = extractJobInstanceId(lastRecord);
                progress = new TimeAndIdBasedArchiveProcess(lastTimestamp, lastJobInstanceId);
                boolean isFinished = jobInstanceRecords.size() < readLimit;
                updateArchiveProgress(isFinished ? ArchiveTaskStatusEnum.SUCCESS : ArchiveTaskStatusEnum.RUNNING,
                    progress);
            } while (jobInstanceRecords != null && jobInstanceRecords.size() == readLimit);
        } finally {
            long archiveCost = System.currentTimeMillis() - startTime;
            archiveTaskSummary.setArchivedRecordSize(archivedRecordCount);
            archiveTaskSummary.setArchiveCost(archiveCost);
        }
    }

    /**
     * 备份作业实例数据到冷存储
     *
     * @param jobInstanceIds 作业实例 ID 列表
     */
    protected abstract void backupJobInstanceToColdDb(List<Long> jobInstanceIds);

    /**
     * 删除作业实例热数据
     *
     * @param jobInstanceIds 作业实例 ID 列表
     */
    protected abstract void deleteJobInstanceHotData(List<Long> jobInstanceIds);

    protected boolean isBackupEnable() {
        ArchiveModeEnum archiveMode = ArchiveModeEnum.valOf(archiveTaskProperties.getMode());
        return archiveTaskProperties.isEnabled()
            && (ArchiveModeEnum.BACKUP_THEN_DELETE == archiveMode || ArchiveModeEnum.BACKUP_ONLY == archiveMode);
    }

    protected boolean isDeleteEnable() {
        ArchiveModeEnum archiveMode = ArchiveModeEnum.valOf(archiveTaskProperties.getMode());
        return archiveTaskProperties.isEnabled()
            && (ArchiveModeEnum.BACKUP_THEN_DELETE == archiveMode || ArchiveModeEnum.DELETE_ONLY == archiveMode);
    }

    private boolean acquireLock() {
        this.isAcquireLock = archiveTaskLock.lock(taskId);
        if (!isAcquireLock) {
            log.info("[{}] Acquire lock fail", taskId);
        }
        return isAcquireLock;
    }

    private void updateArchiveProgress(ArchiveTaskStatusEnum taskStatus, TimeAndIdBasedArchiveProcess progress) {
        archiveTask.setStatus(taskStatus);
        archiveTask.setProcess(progress);
        archiveTaskService.updateTask(archiveTask);
    }


    protected int insertAndReset(List<T> recordList) throws IOException {
        if (CollectionUtils.isEmpty(recordList)) {
            return 0;
        }
        long startTime = System.currentTimeMillis();
        int recordSize = recordList.size();
        int insertRows = batchInsert(recordList);
        recordList.clear();
        if (insertRows != recordSize) {
            throw new ArchiveException(String.format("Insert rows not expected, expect: %s, actual: %s",
                recordSize, insertRows));
        }
        long costTime = System.currentTimeMillis() - startTime;
        log.info("Batch insert {}, maxBatchSize: {}, insert rows: {}, cost: {}",
            tableName, batchInsertRowSize, insertRows, costTime);
        return insertRows;
    }

    protected abstract List<T> listSortedJobInstance(Long fromTimestamp,
                                                     Long endTimestamp,
                                                     Long fromJobInstanceId,
                                                     int limit);

    protected abstract Long extractJobInstanceId(T record);

    protected abstract Long extractJobInstanceCreateTime(T record);

    private int batchInsert(List<T> recordList) throws IOException {
        return jobInstanceColdDAO.batchInsert(recordList, batchInsertRowSize);
    }

    private int deleteRecord(Long start, Long stop) {
        return jobInstanceHotRecordDAO.deleteRecords(start, stop, deleteLimitRowCount);
    }

    /**
     * 计算归档参数的值
     *
     * @param tableName          被归档的表名
     * @param defaultValue       默认值
     * @param tableValueProvider DB 表作用域下的参数值提供者
     */
    protected <V> V computeValuePreferTableConfig(
        String tableName,
        V defaultValue,
        Function<ArchiveDBProperties.TableConfig, V> tableValueProvider
    ) {

        V value = defaultValue;
        if (archiveTaskProperties.getTableConfigs() != null
            && archiveTaskProperties.getTableConfigs().containsKey(tableName)) {
            ArchiveDBProperties.TableConfig tableConfig = archiveTaskProperties.getTableConfigs().get(tableName);
            V tableValue = tableValueProvider.apply(tableConfig);
            if (tableValue != null) {
                value = tableValue;
            }
        }
        return value;
    }

}
