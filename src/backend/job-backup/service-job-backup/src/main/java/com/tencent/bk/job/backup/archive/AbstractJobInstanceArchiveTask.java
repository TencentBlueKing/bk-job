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
import com.tencent.bk.job.backup.archive.model.DbDataNode;
import com.tencent.bk.job.backup.archive.model.JobInstanceArchiveTaskInfo;
import com.tencent.bk.job.backup.archive.model.TableReadWriteProps;
import com.tencent.bk.job.backup.archive.model.TimeAndIdBasedArchiveProcess;
import com.tencent.bk.job.backup.config.ArchiveProperties;
import com.tencent.bk.job.backup.constant.ArchiveModeEnum;
import com.tencent.bk.job.backup.constant.ArchiveTaskStatusEnum;
import com.tencent.bk.job.backup.metrics.ArchiveErrorTaskCounter;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.TableRecord;
import org.slf4j.helpers.MessageFormatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 作业实例数据归档任务基础实现
 *
 * @param <T> 表记录
 */
@Slf4j
public abstract class AbstractJobInstanceArchiveTask<T extends TableRecord<?>>  implements JobInstanceArchiveTask {
    protected JobInstanceHotRecordDAO<T> jobInstanceHotRecordDAO;
    protected JobInstanceColdDAO jobInstanceColdDAO;
    private final ArchiveProperties archiveProperties;
    private final ArchiveTaskLock archiveTaskLock;
    private final ArchiveErrorTaskCounter archiveErrorTaskCounter;
    private final ArchiveTaskService archiveTaskService;

    protected String taskId;
    protected DbDataNode dbDataNode;
    protected JobInstanceArchiveTaskInfo archiveTask;

    protected Map<String, TableReadWriteProps> tableReadWritePropsMap;

    /**
     * 归档进度
     */
    private TimeAndIdBasedArchiveProcess progress;

    private final ArchiveTaskSummary archiveTaskSummary;

    private boolean isAcquireLock;


    public AbstractJobInstanceArchiveTask(JobInstanceHotRecordDAO<T> jobInstanceHotRecordDAO,
                                          JobInstanceColdDAO jobInstanceColdDAO,
                                          ArchiveProperties archiveDbProperties,
                                          ArchiveTaskLock archiveTaskLock,
                                          ArchiveErrorTaskCounter archiveErrorTaskCounter,
                                          JobInstanceArchiveTaskInfo archiveTask,
                                          ArchiveTaskService archiveTaskService) {
        this.jobInstanceHotRecordDAO = jobInstanceHotRecordDAO;
        this.jobInstanceColdDAO = jobInstanceColdDAO;
        this.archiveProperties = archiveDbProperties;
        this.archiveTaskLock = archiveTaskLock;
        this.archiveErrorTaskCounter = archiveErrorTaskCounter;
        this.archiveTask = archiveTask;
        this.progress = archiveTask.getProcess();
        this.taskId = buildTaskId(archiveTask);
        this.archiveTaskSummary = new ArchiveTaskSummary(archiveTask, archiveDbProperties.getMode());
        this.archiveTaskService = archiveTaskService;
    }

    private String buildTaskId(JobInstanceArchiveTaskInfo archiveTask) {
        return archiveTask.getTaskType() + ":" + archiveTask.getDbDataNode().toDataNodeId()
            + ":" + archiveTask.getDay() + ":" + archiveTask.getHour();
    }

    @Override
    public void execute() {
        archive();
    }

    @Override
    public void stop() {

    }

    private void archive() {
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
            archiveTaskSummary.setMessage(e.getMessage());

            archiveErrorTaskCounter.increment();

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
        long archivedJobInstanceCount = 0;

        long startTime = System.currentTimeMillis();
        log.info("[{}] Archive task mode: {}, backupEnabled: {}, deleteEnabled: {}",
            taskId, archiveProperties.getMode(), backupEnabled, deleteEnabled);
        try {
            List<T> jobInstanceRecords;
            do {
                if (progress != null) {
                    jobInstanceRecords = readSortedJobInstanceFromHotDB(progress.getTimestamp(),
                        archiveTask.getToTimestamp(), progress.getId(), readLimit);
                } else {
                    jobInstanceRecords = readSortedJobInstanceFromHotDB(archiveTask.getFromTimestamp(),
                        archiveTask.getToTimestamp(), null, readLimit);
                }

                if (CollectionUtils.isEmpty(jobInstanceRecords)) {
                    updateArchiveProgress(ArchiveTaskStatusEnum.SUCCESS, null);
                    return;
                }
                archivedJobInstanceCount++;

                List<Long> jobInstanceIds =
                    jobInstanceRecords.stream().map(this::extractJobInstanceId).collect(Collectors.toList());

                // 写入数据到冷 db
                if (backupEnabled) {
                    backupJobInstanceToColdDb(jobInstanceRecords, jobInstanceIds);
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
            archiveTaskSummary.setArchivedRecordSize(archivedJobInstanceCount);
            archiveTaskSummary.setArchiveCost(archiveCost);
        }
    }

    /**
     * 备份作业实例数据到冷存储
     *
     * @param jobInstances   作业实例列表
     * @param jobInstanceIds 作业实例ID列表
     */
    protected abstract void backupJobInstanceToColdDb(List<T> jobInstances, List<Long> jobInstanceIds);

    /**
     * 删除作业实例热数据
     *
     * @param jobInstanceIds 作业实例 ID 列表
     */
    protected abstract void deleteJobInstanceHotData(List<Long> jobInstanceIds);

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

    /**
     * 从热 db 读取作业实例熟悉，按照时间+ID 的顺序排序
     *
     * @param fromTimestamp     时间范围-起始-作业实例创建时间（include)
     * @param endTimestamp      时间范围-起始-作业实例创建时间（exclude)
     * @param fromJobInstanceId 作业实例 ID-起始 (exclude)
     * @param limit             读取记录最大数量
     * @return 作业实例记录
     */
    protected abstract List<T> readSortedJobInstanceFromHotDB(Long fromTimestamp,
                                                              Long endTimestamp,
                                                              Long fromJobInstanceId,
                                                              int limit);

    /**
     * 从作业实例记录中提取作业实例 ID
     *
     * @param record 作业实例记录
     */
    protected abstract Long extractJobInstanceId(T record);

    /**
     * 从作业实例记录中提取作业实例创建时间
     *
     * @param record 作业实例记录
     */
    protected abstract Long extractJobInstanceCreateTime(T record);


    protected <V extends TableRecord<?>> void backupTableRecords(JobInstanceHotRecordDAO<V> jobInstanceHotRecordDAO,
                                                                 Collection<Long> jobInstanceIds,
                                                                 int readRowLimit,
                                                                 int batchInsertRowSize) throws IOException {

        String tableName = jobInstanceHotRecordDAO.getTable().getName();
        List<V> recordList = new ArrayList<>(readRowLimit);
        long offset = 0L;
        List<V> records;
        do {
            // 选取start<recordId<=stop的数据
            long readStartTime = System.currentTimeMillis();
            records = jobInstanceHotRecordDAO.listRecords(jobInstanceIds, offset, (long) readRowLimit);
            long readCostTime = System.currentTimeMillis() - readStartTime;
            log.info(
                "Read {}({}-{}) rows from {}, cost={} ms",
                records.size(), offset, offset + readRowLimit, tableName, readCostTime
            );

            if (CollectionUtils.isNotEmpty(records)) {
                recordList.addAll(records);
            }

            if (recordList.size() >= batchInsertRowSize) {
                insertAndReset(tableName, recordList, batchInsertRowSize);
            }
            offset += readRowLimit;

        } while (records.size() == readRowLimit);

        if (CollectionUtils.isNotEmpty(recordList)) {
            // 处理没有达到批量插入阈值的最后一个批次的数据
            insertAndReset(tableName, recordList, batchInsertRowSize);
        }
    }

    protected <V extends TableRecord<?>> void backupTableRecords(JobInstanceHotRecordDAO<V> jobInstanceHotRecordDAO,
                                                                 List<V> records,
                                                                 int batchInsertRowSize) throws IOException {

        String tableName = jobInstanceHotRecordDAO.getTable().getName();
        insertTableRecords(tableName, records, batchInsertRowSize);
    }

    private <V extends TableRecord<?>> void insertAndReset(String tableName,
                                                           List<V> recordList,
                                                           int batchInsertRowSize) throws IOException {
        insertTableRecords(tableName, recordList, batchInsertRowSize);
        recordList.clear();
    }

    private <V extends TableRecord<?>> void insertTableRecords(String tableName,
                                                               List<V> recordList,
                                                               int batchInsertRowSize) throws IOException {
        if (CollectionUtils.isEmpty(recordList)) {
            return;
        }
        long startTime = System.currentTimeMillis();
        int recordSize = recordList.size();
        int insertRows = batchInsertColdDb(recordList, batchInsertRowSize);
        if (insertRows != recordSize) {
            throw new ArchiveException(String.format("Insert rows not expected, expect: %s, actual: %s",
                recordSize, insertRows));
        }
        long costTime = System.currentTimeMillis() - startTime;
        log.info("Batch insert {}, maxBatchSize: {}, insert rows: {}, cost: {}",
            tableName, batchInsertRowSize, insertRows, costTime);
    }

    private <V extends TableRecord<?>> int batchInsertColdDb(
        List<V> recordList, int batchInsertRowSize) throws IOException {

        return jobInstanceColdDAO.batchInsert(recordList, batchInsertRowSize);
    }

    protected <V extends TableRecord<?>> int deleteTableRecord(JobInstanceHotRecordDAO<V> jobInstanceHotRecordDAO,
                                                               List<Long> jobInstanceIds,
                                                               int deleteLimitRowCount) {
        return jobInstanceHotRecordDAO.deleteRecords(jobInstanceIds, deleteLimitRowCount);
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
        Function<ArchiveProperties.TableConfig, V> tableValueProvider
    ) {

        V value = defaultValue;
        if (archiveProperties.getTableConfigs() != null
            && archiveProperties.getTableConfigs().containsKey(tableName)) {
            ArchiveProperties.TableConfig tableConfig = archiveProperties.getTableConfigs().get(tableName);
            V tableValue = tableValueProvider.apply(tableConfig);
            if (tableValue != null) {
                value = tableValue;
            }
        }
        return value;
    }

}
