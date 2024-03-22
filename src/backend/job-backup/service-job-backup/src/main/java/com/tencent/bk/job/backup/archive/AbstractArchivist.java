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
     * 需要删除的记录的最小 ID (include)
     */
    private Long minNeedDeleteId;
    /**
     * 需要删除的记录的最大 ID (include)
     */
    private Long maxNeedDeleteId;
    /**
     * 表中已存在的记录的最小 ID
     */
    private Long minExistedRecordArchiveId;
    private CountDownLatch countDownLatch;
    /**
     * 读取DB 步长
     */
    protected int readIdStepSize;
    /**
     * 写入归档数据，单批次最小行数
     */
    protected int batchInsertRowSize;
    /**
     * 删除数据ID增加步长
     */
    protected int deleteIdStepSize = 10_000;
    protected String tableName;
    private ArchiveSummary archiveSummary;
    private boolean isAcquireLock;

    private ArchiveTaskLock archiveTaskLock;

    public AbstractArchivist(ExecuteRecordDAO<T> executeRecordDAO,
                             ExecuteArchiveDAO executeArchiveDAO,
                             ArchiveProgressService archiveProgressService,
                             ArchiveDBProperties archiveDBProperties,
                             ArchiveTaskLock archiveTaskLock,
                             Long maxNeedArchiveId,
                             CountDownLatch countDownLatch) {
        this.executeRecordDAO = executeRecordDAO;
        this.executeArchiveDAO = executeArchiveDAO;
        this.archiveProgressService = archiveProgressService;
        this.archiveDBProperties = archiveDBProperties;
        this.readIdStepSize = archiveDBProperties.getReadIdStepSize();
        this.batchInsertRowSize = archiveDBProperties.getBatchInsertRowSize();
        this.maxNeedArchiveId = maxNeedArchiveId;
        this.countDownLatch = countDownLatch;
        this.tableName = executeRecordDAO.getTable().getName().toLowerCase();
        this.archiveSummary = new ArchiveSummary(this.tableName);
        this.archiveTaskLock = archiveTaskLock;
    }

    public void archive() {
        try {
            if (!acquireLock()) {
                return;
            }
            if (!archiveDBProperties.isEnabled()) {
                log.info("[{}] Archive is disabled, skip archive", tableName);
                return;
            }

            log.info("[{}] Start archive", tableName);
            minExistedRecordArchiveId = executeRecordDAO.getMinArchiveId();
            if (minExistedRecordArchiveId == null) {
                // min 查询返回 null，说明是空表，无需归档
                log.info("[{}] Empty table, do not need archive!", tableName);
                return;
            }

            boolean archiveSuccess;
            if (isBackupEnable(archiveDBProperties)) {
                archiveSuccess = backupTable();
            } else {
                log.info("[{}] Backup is not enabled, skip backup table!", tableName);
                archiveSuccess = true;
            }

            if (archiveSuccess) {
                delete();
            }
        } catch (Throwable e) {
            String msg = MessageFormatter.format(
                "Error while archiving {}",
                tableName
            ).getMessage();
            log.error(msg, e);
        } finally {
            archiveSummary.setArchiveMode(archiveDBProperties.getMode());
            storeArchiveSummary();
            if (this.isAcquireLock) {
                archiveTaskLock.unlock(tableName);
            }
            countDownLatch.countDown();
        }
    }

    protected boolean isBackupEnable(ArchiveDBProperties archiveDBProperties) {
        return archiveDBProperties.isEnabled()
            && ArchiveModeEnum.BACKUP_THEN_DELETE == ArchiveModeEnum.valOf(archiveDBProperties.getMode());
    }

    private boolean acquireLock() {
        this.isAcquireLock = archiveTaskLock.lock(tableName);
        archiveSummary.setSkip(!isAcquireLock);
        if (!isAcquireLock) {
            log.info("[{}] Acquire lock fail", tableName);
        }
        return isAcquireLock;
    }

    /**
     * 备份表数据
     *
     * @return true: 备份操作成功
     */
    private boolean backupTable() {
        // 计算本次归档的起始 ID
        computeMinNeedArchiveId();
        if (maxNeedArchiveId < this.minNeedArchiveId) {
            log.info("[{}] MinNeedArchiveId {} is greater than maxNeedArchiveId {}, skip archive table!",
                tableName, minNeedArchiveId, maxNeedArchiveId);
            return true;
        }

        long archivedRows = 0;
        long readRows = 0;
        long startTime = System.currentTimeMillis();
        long archiveCost = 0;
        long start = this.minNeedArchiveId - 1;
        long stop = start;
        try {
            log.info("[{}] Backup table start, minNeedArchiveId: {}, maxNeedArchiveId:{}",
                tableName, minNeedArchiveId, maxNeedArchiveId);
            List<T> recordList = new ArrayList<>(readIdStepSize);

            while (maxNeedArchiveId > start) {
                stop = Math.min(maxNeedArchiveId, start + readIdStepSize);
                Pair<Long, Long> stepResult = archiveOneStepRecords(start, stop);
                readRows += stepResult.getLeft();
                archivedRows += stepResult.getRight();
                updateArchiveProgress(stop);
                start = stop;
            }

            if (CollectionUtils.isNotEmpty(recordList)) {
                // 处理没有达到批量插入阈值的最后一个批次的数据
                int insertRows = insertAndReset(stop, recordList);
                archivedRows += insertRows;
                updateArchiveProgress(stop);
            }

            // 即使没有需要归档的数据，仍然需要更新归档进度
            if (readRows == 0) {
                updateArchiveProgress(stop);
            }

            archiveCost = System.currentTimeMillis() - startTime;
            log.info("Backup {} finished, minNeedArchiveId: {}, maxNeedArchiveId: {}, readRows: {}, " +
                    "archivedRows: {}, cost: {}ms",
                tableName, minNeedArchiveId, maxNeedArchiveId, readRows, archivedRows, archiveCost);
            if (readRows != archivedRows) {
                log.warn("Backup row are unexpected, table: {}, expected: {}, actual: {}", tableName, readRows,
                    archivedRows);
            }
            return true;
        } catch (Throwable e) {
            String msg = MessageFormatter.format(
                "Error while archiving {}",
                tableName
            ).getMessage();
            log.error(msg, e);
            return false;
        } finally {
            setArchiveSummary(minNeedArchiveId, maxNeedArchiveId, readRows, archivedRows, stop, archiveCost);
        }
    }

    /**
     * 对一个批次数据进行归档
     *
     * @param start 数据起始记录ID
     * @param stop  数据终止记录ID
     * @return Pair<读取的记录数量 ， 归档成功的记录数量>
     */
    private Pair<Long, Long> archiveOneStepRecords(long start, long stop) throws IOException {
        List<T> recordList = new ArrayList<>(readIdStepSize);
        long archivedRows = 0;
        long readRows = 0;
        long offset = 0L;
        int limit = readIdStepSize;
        List<T> records;
        do {
            // 选取start<recordId<=stop的数据
            records = listRecord(start, stop, offset, (long) limit);
            readRows += records.size();
            log.info(
                "Read {}({}-{}) rows from {}, start={}, stop={}",
                records.size(), offset, offset + limit, tableName, start, stop
            );

            if (CollectionUtils.isNotEmpty(records)) {
                recordList.addAll(records);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Fetching {} found data hole at {} {}", tableName, start, readIdStepSize);
                }
            }

            if (recordList.size() >= batchInsertRowSize) {
                int insertRows = insertAndReset(stop, recordList);
                archivedRows += insertRows;
            }
            offset += limit;
        } while (records.size() == limit);
        return Pair.of(readRows, archivedRows);
    }

    private void setArchiveSummary(Long minNeedArchiveId,
                                   Long maxNeedArchiveId,
                                   long readRows,
                                   long archivedRows,
                                   long stop,
                                   long archiveCost) {
        archiveSummary.setArchiveIdStart(minNeedArchiveId);
        archiveSummary.setArchiveIdEnd(maxNeedArchiveId);
        archiveSummary.setNeedArchiveRecordSize(readRows);
        archiveSummary.setArchivedRecordSize(archivedRows);
        archiveSummary.setLastArchivedId(stop);
        archiveSummary.setArchiveCost(archiveCost);
    }

    /**
     * 计算本次归档的起始 ID
     */
    private void computeMinNeedArchiveId() {
        ArchiveProgressDTO archiveProgress = archiveProgressService.queryArchiveProgress(tableName);
        // 上次归档记录的截止ID
        long lastArchiveId = (archiveProgress == null || archiveProgress.getLastArchivedId() == null) ?
            0 : archiveProgress.getLastArchivedId();
        // 本次归档为表中的最小记录 ID 与上次归档记录的ID中的较大者，减少需要处理的数据
        this.minNeedArchiveId = Math.max(lastArchiveId + 1, minExistedRecordArchiveId);
        log.info("[{}] Compute minNeedArchiveId, lastArchiveId: {}, minExistedRecordArchiveId: {}," + "" +
                "minNeedArchiveId: {}",
            tableName, lastArchiveId, minExistedRecordArchiveId, minNeedArchiveId);
    }

    /**
     * 计算本次删除的 ID范围
     */
    private void computeNeedDeleteIdRange() {
        ArchiveProgressDTO archiveProgress = archiveProgressService.queryArchiveProgress(tableName);
        // 上次删除记录的截止ID
        Long lastDeleteId = (archiveProgress == null || archiveProgress.getLastDeletedId() == null) ?
            null : archiveProgress.getLastDeletedId();
        // 需要删除的最小 ID 为表中实际数据的最小 ID
        this.minNeedDeleteId = minExistedRecordArchiveId;
        // 需要删除的最大 ID 为本次归档的截止 ID
        this.maxNeedDeleteId = maxNeedArchiveId;
        log.info("[{}] Compute deleteIdRange, lastDeleteId: {}, minExistedRecordArchiveId: {}," + "" +
                "minNeedDeleteId: {}, maxNeedDeleteId: {}",
            tableName, lastDeleteId, minExistedRecordArchiveId, minNeedDeleteId, maxNeedDeleteId);
    }

    private void updateArchiveProgress(long lastArchivedId) {
        ArchiveProgressDTO archiveProgress = new ArchiveProgressDTO();
        archiveProgress.setTableName(tableName);
        archiveProgress.setLastArchiveTime(System.currentTimeMillis());
        archiveProgress.setLastArchivedId(lastArchivedId);
        archiveProgressService.saveArchiveProgress(archiveProgress);
    }

    private void updateDeleteProgress(long lastDeletedId) {
        ArchiveProgressDTO archiveProgress = new ArchiveProgressDTO();
        archiveProgress.setTableName(tableName);
        archiveProgress.setLastDeleteTime(System.currentTimeMillis());
        archiveProgress.setLastDeletedId(lastDeletedId);
        archiveProgressService.saveDeleteProgress(archiveProgress);
    }

    private void delete() {
        long startTime = System.currentTimeMillis();
        computeNeedDeleteIdRange();

        long deletedRows = 0;

        log.info("Delete {} start|{}|{}", tableName, minNeedDeleteId, maxNeedDeleteId);
        if (maxNeedDeleteId < minNeedDeleteId) {
            log.info("MinNeedDeleteId {} is greater than maxNeedDeleteId {}, skip delete {}!",
                minNeedDeleteId, maxNeedDeleteId, tableName);
            return;
        }

        long start = minNeedDeleteId - 1;
        long stop = minNeedDeleteId;
        try {
            while (maxNeedDeleteId > start) {
                long batchDeleteStartTime = System.currentTimeMillis();
                stop = Math.min(maxNeedDeleteId, start + deleteIdStepSize);
                int deleteCount = deleteRecord(start, stop);
                deletedRows += deleteCount;
                log.info("Delete {}, start: {}, stop: {}, delete rows: {}, cost: {}ms", tableName,
                    start, stop, deleteCount, System.currentTimeMillis() - batchDeleteStartTime);
                start += deleteIdStepSize;
                updateDeleteProgress(stop);
            }
            log.info("Delete {} finished, minNeedDeleteId: {}, maxNeedDeleteId: {}, deletedRows: " +
                    "{}, cost: {}ms",
                tableName, minNeedDeleteId, maxNeedDeleteId, deletedRows, System.currentTimeMillis() - startTime);
        } catch (Throwable e) {
            String msg = MessageFormatter.format(
                "Error while deleting {}",
                tableName
            ).getMessage();
            log.error(msg, e);
        } finally {
            archiveSummary.setDeleteCost(System.currentTimeMillis() - startTime);
            archiveSummary.setDeleteIdStart(minNeedDeleteId);
            archiveSummary.setDeleteIdEnd(maxNeedDeleteId);
            archiveSummary.setLastDeletedId(stop);
            archiveSummary.setDeleteRecordSize(deletedRows);
        }
    }

    private int insertAndReset(long lastArchivedInstanceId, List<T> recordList) throws IOException {
        if (CollectionUtils.isEmpty(recordList)) {
            return 0;
        }
        int recordSize = recordList.size();
        int insertRows = batchInsert(recordList);
        recordList.clear();
        boolean insertError = false;
        if (insertRows != recordSize) {
            insertError = true;
        }
        log.info("Batch insert {}, lastArchivedInstanceId: {}, maxBatchSize: {}, insertError: {}, expected insert " +
                "rows: {}, actual insert rows: {}",
            tableName, lastArchivedInstanceId, batchInsertRowSize, insertError, recordSize, insertRows);
        return insertRows;
    }

    private void storeArchiveSummary() {
        ArchiveSummaryHolder.getInstance().addArchiveSummary(this.archiveSummary);
    }

    private List<T> listRecord(Long start, Long stop, Long offset, Long limit) {
        return executeRecordDAO.listRecords(start, stop, offset, limit);
    }

    private int batchInsert(List<T> recordList) throws IOException {
        return executeArchiveDAO.batchInsert(recordList, 1000);
    }

    private int deleteRecord(Long start, Long stop) {
        return executeRecordDAO.deleteRecords(start, stop);
    }
}
