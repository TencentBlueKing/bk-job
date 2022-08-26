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

import com.tencent.bk.job.backup.config.ArchiveConfig;
import com.tencent.bk.job.backup.dao.ExecuteArchiveDAO;
import com.tencent.bk.job.backup.dao.ExecuteRecordDAO;
import com.tencent.bk.job.backup.model.dto.ArchiveProgressDTO;
import com.tencent.bk.job.backup.model.dto.ArchiveSummary;
import com.tencent.bk.job.backup.service.ArchiveProgressService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.TableRecord;

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
    /**
     * 读取DB 步长
     */
    protected int readIdStepSize = 1_000;
    /**
     * 写入归档数据，单批次最小行数
     */
    protected int batchInsertRow = 1_000;
    /**
     * 删除数据ID增加步长
     */
    protected int deleteIdStepSize = 10_000;
    protected String tableName;
    private ArchiveSummary archiveSummary;

    public AbstractArchivist(ExecuteRecordDAO<T> executeRecordDAO,
                             ExecuteArchiveDAO executeArchiveDAO,
                             ArchiveProgressService archiveProgressService) {
        this.executeRecordDAO = executeRecordDAO;
        this.executeArchiveDAO = executeArchiveDAO;
        this.archiveProgressService = archiveProgressService;
        this.tableName = executeRecordDAO.getTable().getName().toLowerCase();
    }

    public void archive(ArchiveConfig archiveConfig, Long maxNeedArchiveId, CountDownLatch countDownLatch) {
        boolean isAcquireLock = false;
        try {
            archiveSummary = new ArchiveSummary();
            archiveSummary.setTableName(tableName);
            if (!ArchiveTaskLock.getInstance().lock(tableName)) {
                archiveSummary.setSkip(true);
                isAcquireLock = false;
                return;
            } else {
                isAcquireLock = true;
            }
            archiveSummary.setSkip(false);
            log.info("Start archive and delete, tableName: {}, archiveConfig: {}", tableName, archiveConfig);

            boolean archiveSuccess;
            if (archiveConfig.isArchiveEnabled()) {
                archiveSuccess = archive(maxNeedArchiveId);
            } else {
                log.info("Archive is not enabled, skip archive {}!", tableName);
                archiveSuccess = true;
            }

            if (archiveSuccess && archiveConfig.isDeleteEnabled()) {
                delete(maxNeedArchiveId, archiveConfig);
            }
        } catch (Throwable e) {
            log.error("Error while archiving {}", tableName, e);
        } finally {
            archiveSummary.setArchiveEnabled(archiveConfig.isArchiveEnabled());
            archiveSummary.setDeleteEnabled(archiveConfig.isDeleteEnabled());
            storeArchiveSummary();
            if (isAcquireLock) {
                ArchiveTaskLock.getInstance().unlock(tableName);
            }
            countDownLatch.countDown();
        }
    }

    private boolean archive(Long maxNeedArchiveId) {
        long lastArchivedId = 0;
        long archivedRows = 0;
        long readRows = 0;
        long startTime = System.currentTimeMillis();
        long minNeedArchiveId = 0;
        long archiveCost = 0;
        try {
            lastArchivedId = getLastArchivedId();
            minNeedArchiveId = lastArchivedId;
            log.info("Archive {} start, minNeedArchiveId: {}, maxNeedArchiveId:{}",
                tableName, minNeedArchiveId, maxNeedArchiveId);

            long start = lastArchivedId;
            long stop = start;
            List<T> recordList = new ArrayList<>(readIdStepSize);

            if (maxNeedArchiveId <= lastArchivedId) {
                log.info("LastArchivedId {} is greater than or equal to maxNeedArchiveId {}, skip archive {}!",
                    lastArchivedId, maxNeedArchiveId, tableName);
            }

            while (maxNeedArchiveId > start) {
                stop = start + readIdStepSize;
                if (stop > maxNeedArchiveId) {
                    stop = maxNeedArchiveId;
                }
                // 选取start<recordId<=stop的数据
                List<T> records = listRecord(start, stop);
                readRows += records.size();
                log.info("Read {} rows from {}", records.size(), tableName);

                if (CollectionUtils.isEmpty(records)) {
                    if (stop >= maxNeedArchiveId) {
                        log.info("Read {} finished!", tableName);
                        break;
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Fetching {} found data hole at {} {}", tableName, start, readIdStepSize);
                        }
                    }
                } else {
                    recordList.addAll(records);
                }

                start = stop;
                if (recordList.size() >= batchInsertRow) {
                    int insertRows = insertAndReset(stop, recordList);
                    lastArchivedId = stop;
                    archivedRows += insertRows;
                    updateArchiveProgress(lastArchivedId);
                }
            }
            if (CollectionUtils.isNotEmpty(recordList)) {
                int insertRows = insertAndReset(stop, recordList);
                lastArchivedId = stop;
                archivedRows += insertRows;
                updateArchiveProgress(lastArchivedId);
            }

            // 即使没有需要归档的数据，仍然需要更新归档进度
            if (readRows == 0) {
                lastArchivedId = stop;
                updateArchiveProgress(lastArchivedId);
            }

            archiveCost = System.currentTimeMillis() - startTime;
            log.info("Archive {} finished, minNeedArchiveId: {}, maxNeedArchiveId: {}, lastArchivedId: {}, readRows: " +
                    "{}, archivedRows: {}, cost: {}ms",
                tableName, minNeedArchiveId, maxNeedArchiveId, lastArchivedId, readRows,
                archivedRows, archiveCost);
            if (readRows != archivedRows) {
                log.warn("Archive row are unexpected, table: {}, expected: {}, actual: {}", tableName, readRows,
                    archivedRows);
            }
            return true;
        } catch (Throwable e) {
            log.error("Error while archiving {}", tableName, e);
            return false;
        } finally {
            archiveSummary.setArchiveIdStart(minNeedArchiveId);
            archiveSummary.setArchiveIdEnd(maxNeedArchiveId);
            archiveSummary.setNeedArchiveRecordSize(readRows);
            archiveSummary.setArchivedRecordSize(archivedRows);
            archiveSummary.setLastArchivedId(lastArchivedId);
            archiveSummary.setArchiveCost(archiveCost);
        }
    }

    /**
     * 获取上一次已归档完成的最后一个数据Id，后续用大于该Id选取范围数据
     */
    private long getLastArchivedId() {
        ArchiveProgressDTO archiveProgress = archiveProgressService.queryArchiveProgress(tableName);
        if (archiveProgress == null || archiveProgress.getLastArchivedId() == null
            || archiveProgress.getLastArchivedId() <= 0L) {
            long lastArchivedId = getFirstInstanceId() - 1;
            log.info("Archive {} for the first time! lastArchivedId: {}", tableName, lastArchivedId);
            return lastArchivedId;
        }
        return archiveProgress.getLastArchivedId();
    }

    private long getLastDeletedId() {
        ArchiveProgressDTO archiveProgress = archiveProgressService.queryArchiveProgress(tableName);
        if (archiveProgress == null || archiveProgress.getLastDeletedId() == null
            || archiveProgress.getLastDeletedId() <= 0L) {
            long lastDeletedId = getFirstInstanceId() - 1;
            log.info("Delete {} for the first time! lastDeletedId: {}", tableName, lastDeletedId);
            return lastDeletedId;
        }
        return archiveProgress.getLastDeletedId();
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

    private void delete(Long maxNeedArchiveId, ArchiveConfig archiveConfig) {
        long startTime = System.currentTimeMillis();

        Long maxNeedDeleteId;
        if (archiveConfig.isArchiveEnabled()) {
            // 如果开启归档，那么删除的最大ID必须等于已经归档的ID，保证数据在删除之前已经被正确归档
            maxNeedDeleteId = getLastArchivedId();
        } else {
            maxNeedDeleteId = maxNeedArchiveId;
        }

        long lastDeletedId = getLastDeletedId();
        long minNeedDeleteId = lastDeletedId;
        long deletedRows = 0;

        try {
            log.info("Delete {} start|{}|{}", tableName, minNeedDeleteId, maxNeedDeleteId);
            if (maxNeedDeleteId <= lastDeletedId) {
                log.info("LastDeletedId {} is greater than or equal to maxNeedDeleteId {}, skip delete {}!",
                    lastDeletedId, maxNeedDeleteId, tableName);
                return;
            }

            long start = minNeedDeleteId;
            while (maxNeedDeleteId > start) {
                long batchDeleteStartTime = System.currentTimeMillis();
                long stop = start + deleteIdStepSize;
                if (stop > maxNeedDeleteId) {
                    stop = maxNeedDeleteId;
                }
                int deleteCount = deleteRecord(start, stop);
                lastDeletedId = stop;
                deletedRows += deleteCount;
                start += deleteIdStepSize;
                log.info("Delete {}, lastDeletedId: {}, delete rows: {}, cost: {}ms", tableName,
                    lastDeletedId, deleteCount, System.currentTimeMillis() - batchDeleteStartTime);
                updateDeleteProgress(lastDeletedId);
            }
            log.info("Delete {} finished, minNeedDeleteId: {}, maxNeedDeleteId: {}, lastDeletedId: {}, deletedRows: " +
                    "{}, cost: {}ms",
                tableName, minNeedDeleteId, maxNeedDeleteId, lastDeletedId, deletedRows,
                System.currentTimeMillis() - startTime);
        } catch (Throwable e) {
            log.error("Error while deleting {}", tableName, e);
        } finally {
            archiveSummary.setDeleteCost(System.currentTimeMillis() - startTime);
            archiveSummary.setDeleteIdStart(minNeedDeleteId);
            archiveSummary.setDeleteIdEnd(maxNeedDeleteId);
            archiveSummary.setLastDeletedId(lastDeletedId);
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
            tableName, lastArchivedInstanceId, batchInsertRow, insertError, recordSize, insertRows);
        return insertRows;
    }

    private void storeArchiveSummary() {
        ArchiveSummaryHolder.getInstance().addArchiveSummary(this.archiveSummary);
    }

    private List<T> listRecord(Long start, Long stop) {
        return executeRecordDAO.listRecords(start, stop);
    }

    private int batchInsert(List<T> recordList) throws IOException {
        return executeArchiveDAO.batchInsert(recordList, 1000);
    }

    private int deleteRecord(Long start, Long stop) {
        return executeRecordDAO.deleteRecords(start, stop);
    }

    private long getFirstInstanceId() {
        return executeRecordDAO.getFirstArchiveId();
    }
}
