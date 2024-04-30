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

import com.tencent.bk.job.backup.config.ArchiveDBProperties;
import com.tencent.bk.job.backup.constant.ArchiveModeEnum;
import com.tencent.bk.job.backup.dao.ExecuteArchiveDAO;
import com.tencent.bk.job.backup.dao.ExecuteRecordDAO;
import com.tencent.bk.job.backup.metrics.ArchiveErrorTaskCounter;
import com.tencent.bk.job.backup.model.dto.ArchiveProgressDTO;
import com.tencent.bk.job.backup.model.dto.ArchiveSummary;
import com.tencent.bk.job.backup.service.ArchiveProgressService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.TableRecord;
import org.slf4j.helpers.MessageFormatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

/**
 * 表归档基础实现
 *
 * @param <T> 表记录
 */
@Data
@Slf4j
public abstract class AbstractArchivist<T extends TableRecord<?>> {
    protected ExecuteRecordDAO<T> executeRecordDAO;
    protected ExecuteArchiveDAO executeArchiveDAO;
    protected ArchiveProgressService archiveProgressService;
    private ArchiveDBProperties archiveDBProperties;
    /**
     * 需要归档的记录的最大 ID (include)
     */
    private Long maxNeedArchiveId;
    /**
     * 需要归档的记录的最小 ID (include)
     */
    private Long minNeedArchiveId;
    /**
     * 表中已存在的记录的最小 ID
     */
    private Long minExistedRecordArchiveId;
    /**
     * 上次归档任务已备份的最后一条数据的ID
     */
    private Long lastBackupId;
    /**
     * 上次归档任务已删除的最后一条数据的ID
     */
    private Long lastDeleteId;

    private CountDownLatch countDownLatch;
    /**
     * 读取DB 步长
     */
    protected int readIdStepSize;
    /**
     * 每批次从 db 表中读取的记录数量
     */
    protected int readRowLimit;
    /**
     * 写入归档数据，单批次最小行数
     */
    protected int batchInsertRowSize;
    /**
     * 删除数据ID增加步长
     */
    protected int deleteIdStepSize = 10_000;
    /**
     * 每次执行删除的最大行数
     */
    protected int deleteLimitRowCount;
    protected String tableName;
    private ArchiveSummary archiveSummary;
    private boolean isAcquireLock;

    private ArchiveTaskLock archiveTaskLock;

    private final ArchiveErrorTaskCounter archiveErrorTaskCounter;

    public AbstractArchivist(ExecuteRecordDAO<T> executeRecordDAO,
                             ExecuteArchiveDAO executeArchiveDAO,
                             ArchiveProgressService archiveProgressService,
                             ArchiveDBProperties archiveDBProperties,
                             ArchiveTaskLock archiveTaskLock,
                             Long maxNeedArchiveId,
                             CountDownLatch countDownLatch,
                             ArchiveErrorTaskCounter archiveErrorTaskCounter) {
        this.executeRecordDAO = executeRecordDAO;
        this.executeArchiveDAO = executeArchiveDAO;
        this.archiveProgressService = archiveProgressService;
        this.archiveDBProperties = archiveDBProperties;
        this.tableName = executeRecordDAO.getTable().getName().toLowerCase();
        this.readIdStepSize = computeValuePreferTableConfig(archiveDBProperties.getReadIdStepSize(),
            ArchiveDBProperties.TableConfig::getReadIdStepSize);
        this.batchInsertRowSize = computeValuePreferTableConfig(archiveDBProperties.getBatchInsertRowSize(),
            ArchiveDBProperties.TableConfig::getBatchInsertRowSize);
        this.readRowLimit = computeValuePreferTableConfig(archiveDBProperties.getReadRowLimit(),
            ArchiveDBProperties.TableConfig::getReadRowLimit);
        this.deleteLimitRowCount = computeValuePreferTableConfig(archiveDBProperties.getDeleteRowLimit(),
            ArchiveDBProperties.TableConfig::getDeleteRowLimit);
        this.maxNeedArchiveId = maxNeedArchiveId;
        this.countDownLatch = countDownLatch;
        this.archiveSummary = new ArchiveSummary(this.tableName);
        this.archiveTaskLock = archiveTaskLock;
        this.archiveErrorTaskCounter = archiveErrorTaskCounter;
    }

    /**
     * 计算归档参数的值
     *
     * @param defaultValue       默认值
     * @param tableValueProvider DB 表作用域下的参数值提供者
     */
    private <V> V computeValuePreferTableConfig(
        V defaultValue,
        Function<ArchiveDBProperties.TableConfig, V> tableValueProvider
    ) {

        V value = defaultValue;
        if (archiveDBProperties.getTableConfigs() != null
            && archiveDBProperties.getTableConfigs().containsKey(tableName)) {
            ArchiveDBProperties.TableConfig tableConfig = archiveDBProperties.getTableConfigs().get(tableName);
            V tableValue = tableValueProvider.apply(tableConfig);
            if (tableValue != null) {
                value = tableValue;
            }
        }
        return value;
    }

    public void archive() {
        try {
            if (!acquireLock()) {
                archiveSummary.setSkip(!isAcquireLock);
                return;
            }

            if (!archiveDBProperties.isEnabled()) {
                archiveSummary.setSkip(true);
                log.info("[{}] Archive is disabled, skip archive", tableName);
                return;
            }
            archiveSummary.setEnabled(true);

            log.info("[{}] Start archive", tableName);

            initArchiveIdSettings();

            log.info("[{}] Archive record config, readIdStepSize: {}, readRowLimit: {}, batchInsertRowSize: {}, " +
                    "deleteLimitRowCount: {}",
                tableName, readIdStepSize, readRowLimit, batchInsertRowSize, deleteLimitRowCount);

            if (minExistedRecordArchiveId == null) {
                // min 查询返回 null，说明是空表，无需归档
                archiveSummary.setSkip(true);
                archiveSummary.setSuccess(true);
                log.info("[{}] Empty table, do not need archive!", tableName);
                return;
            }

            if (maxNeedArchiveId < this.minNeedArchiveId) {
                log.info("[{}] MinNeedArchiveId {} is greater than maxNeedArchiveId {}, skip archive table!",
                    tableName, minNeedArchiveId, maxNeedArchiveId);
                archiveSummary.setSkip(true);
                archiveSummary.setSuccess(true);
                return;
            }

            // 归档
            backupAndDelete();

        } catch (Throwable e) {
            String msg = MessageFormatter.format(
                "Error while archiving {}",
                tableName
            ).getMessage();
            log.error(msg, e);
            archiveErrorTaskCounter.increment();
        } finally {
            archiveSummary.setArchiveMode(archiveDBProperties.getMode());
            storeArchiveSummary();
            if (this.isAcquireLock) {
                archiveTaskLock.unlock(tableName);
            }
            countDownLatch.countDown();
        }
    }

    private void backupAndDelete() throws IOException {
        boolean backupEnabled = isBackupEnable(archiveDBProperties);
        long backupRows = 0;
        long readRows = 0;
        long deleteRows = 0;
        long startTime = System.currentTimeMillis();
        long start = this.minNeedArchiveId - 1;
        long stop = start;
        boolean success = true;
        try {
            while (maxNeedArchiveId > start) {
                // start < id <= stop
                stop = Math.min(maxNeedArchiveId, start + readIdStepSize);
                Pair<Long, Long> backupResult = null;
                if (backupEnabled) {
                    backupResult = backupRecords(start, stop);
                    readRows += backupResult.getLeft();
                    backupRows += backupResult.getRight();
                }

                if (backupResult != null) {
                    if (backupResult.getLeft() > 0) {
                        // 降低 delete 执行次数：备份过程中读取的数据行数大于 0，才会执行 delete 操作
                        deleteRows += delete(start, stop);
                    }
                } else {
                    deleteRows += delete(start, stop);
                }

                start = stop;
            }
        } catch (Throwable e) {
            success = false;
            throw e;
        } finally {
            long archiveCost = System.currentTimeMillis() - startTime;
            log.info(
                "Archive {} finished, result: {}, minNeedArchiveId: {}, maxNeedArchiveId: {}, readRows: {}, " +
                    "backupRows: {}, deleteRows: {}, cost: {}ms",
                tableName,
                success ? "success" : "fail",
                minNeedArchiveId,
                maxNeedArchiveId,
                readRows,
                backupRows,
                deleteRows,
                archiveCost
            );
            setArchiveSummary(minNeedArchiveId, maxNeedArchiveId, readRows, backupRows, deleteRows,
                stop, archiveCost, success);
        }
    }

    protected boolean isBackupEnable(ArchiveDBProperties archiveDBProperties) {
        return archiveDBProperties.isEnabled()
            && ArchiveModeEnum.BACKUP_THEN_DELETE == ArchiveModeEnum.valOf(archiveDBProperties.getMode());
    }

    private boolean acquireLock() {
        this.isAcquireLock = archiveTaskLock.lock(tableName);
        if (!isAcquireLock) {
            log.info("[{}] Acquire lock fail", tableName);
        }
        return isAcquireLock;
    }

    /**
     * 对一个批次数据进行备份
     *
     * @param start 数据起始记录ID
     * @param stop  数据终止记录ID
     * @return Pair<读取的记录数量, 备份成功的记录数量>
     */
    private Pair<Long, Long> backupRecords(long start, long stop) throws IOException {
        if (lastBackupId >= stop) {
            // 说明数据已经备份过，跳过
            return Pair.of(0L, 0L);
        }
        long startId = start;
        if (lastBackupId > start) {
            // 从上次备份结束的 ID 位置开始
            startId = lastBackupId;
        }

        List<T> recordList = new ArrayList<>(readRowLimit);
        long backupRows = 0;
        long readRows = 0;
        long offset = 0L;
        List<T> records;
        do {
            // 选取start<recordId<=stop的数据
            long readStartTime = System.currentTimeMillis();
            records = listRecord(startId, stop, offset, (long) readRowLimit);
            readRows += records.size();
            long readCostTime = System.currentTimeMillis() - readStartTime;
            log.info(
                "Read {}({}-{}) rows from {}, start={}, stop={}, cost={} ms",
                records.size(), offset, offset + readRowLimit, tableName, startId, stop, readCostTime
            );

            if (CollectionUtils.isNotEmpty(records)) {
                recordList.addAll(records);
            }

            if (recordList.size() >= batchInsertRowSize) {
                int insertRows = insertAndReset(recordList);
                backupRows += insertRows;
            }
            offset += readRowLimit;

        } while (records.size() == readRowLimit);

        if (CollectionUtils.isNotEmpty(recordList)) {
            // 处理没有达到批量插入阈值的最后一个批次的数据
            int insertRows = insertAndReset(recordList);
            backupRows += insertRows;
        }

        updateBackupProgress(stop);

        return Pair.of(readRows, backupRows);
    }

    private void setArchiveSummary(Long minNeedArchiveId,
                                   Long maxNeedArchiveId,
                                   long readRows,
                                   long backupRows,
                                   long deleteRows,
                                   long stop,
                                   long archiveCost,
                                   boolean success) {
        archiveSummary.setArchiveIdStart(minNeedArchiveId);
        archiveSummary.setArchiveIdEnd(maxNeedArchiveId);
        archiveSummary.setNeedArchiveRecordSize(readRows);
        archiveSummary.setBackupRecordSize(backupRows);
        archiveSummary.setDeleteRecordSize(deleteRows);
        archiveSummary.setLastBackupId(stop);
        archiveSummary.setLastDeletedId(stop);
        archiveSummary.setArchiveCost(archiveCost);
        archiveSummary.setSuccess(success);
    }

    /**
     * 初始化本次归档数据的ID设置
     */
    private void initArchiveIdSettings() {
        ArchiveProgressDTO archiveProgress = archiveProgressService.queryArchiveProgress(tableName);
        this.lastBackupId = (archiveProgress == null || archiveProgress.getLastBackupId() == null) ?
            0 : archiveProgress.getLastBackupId();
        this.lastDeleteId = (archiveProgress == null || archiveProgress.getLastDeletedId() == null) ?
            0 : archiveProgress.getLastDeletedId();
        this.minExistedRecordArchiveId = executeRecordDAO.getMinArchiveId();
        this.minNeedArchiveId = minExistedRecordArchiveId;
        log.info("[{}] Init archive id settings, lastBackupId: {}, lastDeleteId: {}, minExistedRecordArchiveId: {},"
                + " minNeedArchiveId: {}, maxNeedArchiveId: {}",
            tableName, lastBackupId, lastDeleteId, minExistedRecordArchiveId, minNeedArchiveId, maxNeedArchiveId);
    }

    private void updateBackupProgress(long archiveId) {
        ArchiveProgressDTO archiveProgress = new ArchiveProgressDTO();
        archiveProgress.setTableName(tableName);
        archiveProgress.setLastBackupTime(System.currentTimeMillis());
        archiveProgress.setLastBackupId(archiveId);
        archiveProgressService.saveArchiveProgress(archiveProgress);
    }

    private void updateDeleteProgress(long archiveId) {
        ArchiveProgressDTO archiveProgress = new ArchiveProgressDTO();
        archiveProgress.setTableName(tableName);
        archiveProgress.setLastDeleteTime(System.currentTimeMillis());
        archiveProgress.setLastDeletedId(archiveId);
        archiveProgressService.saveDeleteProgress(archiveProgress);
    }

    /**
     * 对一个批次数据进行删除
     *
     * @param start 数据起始记录ID
     * @param stop  数据终止记录ID
     */
    private int delete(long start, long stop) {
        long startTime = System.currentTimeMillis();

        int deleteCount = deleteRecord(start, stop);
        updateDeleteProgress(stop);
        log.info("Delete {}, start: {}, stop: {}, delete rows: {}, cost: {}ms", tableName,
            start, stop, deleteCount, System.currentTimeMillis() - startTime);
        return deleteCount;
    }

    private int insertAndReset(List<T> recordList) throws IOException {
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

    private void storeArchiveSummary() {
        ArchiveSummaryHolder.getInstance().addArchiveSummary(this.archiveSummary);
    }

    private List<T> listRecord(Long start, Long stop, Long offset, Long limit) {
        return executeRecordDAO.listRecords(start, stop, offset, limit);
    }

    private int batchInsert(List<T> recordList) throws IOException {
        return executeArchiveDAO.batchInsert(recordList, batchInsertRowSize);
    }

    private int deleteRecord(Long start, Long stop) {
        return executeRecordDAO.deleteRecords(start, stop, deleteLimitRowCount);
    }
}
