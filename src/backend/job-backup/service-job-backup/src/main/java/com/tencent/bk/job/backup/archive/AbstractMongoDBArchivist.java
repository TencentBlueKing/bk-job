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
import com.tencent.bk.job.backup.constant.MongoDBLogTypeEnum;
import com.tencent.bk.job.backup.metrics.ArchiveErrorTaskCounter;
import com.tencent.bk.job.backup.model.dto.ArchiveSummary;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * mongodb归档基础实现
 */
@Slf4j
public abstract class AbstractMongoDBArchivist {
    protected ArchiveDBProperties archiveDBProperties;
    protected CountDownLatch countDownLatch;
    protected ArchiveSummary archiveSummary;
    protected boolean isAcquireLock;
    protected ArchiveTaskLock archiveTaskLock;
    protected MongoDBLogTypeEnum mongoDBLogTypeEnum;
    protected AtomicInteger deleteCounter;
    protected AtomicInteger backupCounter;
    protected AtomicInteger failCounter;

    protected final ArchiveErrorTaskCounter archiveErrorTaskCounter;
    protected final MongoTemplate mongoTemplate;
    protected final String SCRIPT_LOG_PREFIX = "job_log_script";
    protected final String FILE_LOG_PREFIX = "job_log_file";
    protected final String COLLECTION_NAME_SEPARATOR = "_";
    protected final String COLLECTION_NAME_DATE_FORMATTER = "yyyy_MM_dd";

    public AbstractMongoDBArchivist(MongoTemplate mongoTemplate,
                                    ArchiveDBProperties archiveDBProperties,
                                    ArchiveTaskLock archiveTaskLock,
                                    CountDownLatch countDownLatch,
                                    ArchiveErrorTaskCounter archiveErrorTaskCounter,
                                    MongoDBLogTypeEnum mongoDBLogTypeEnum) {
        this.mongoTemplate = mongoTemplate;
        this.archiveDBProperties = archiveDBProperties;
        this.countDownLatch = countDownLatch;
        this.archiveSummary = new ArchiveSummary();
        this.archiveTaskLock = archiveTaskLock;
        this.archiveErrorTaskCounter = archiveErrorTaskCounter;
        this.mongoDBLogTypeEnum = mongoDBLogTypeEnum;
        deleteCounter = new AtomicInteger(0);
        backupCounter = new AtomicInteger(0);
        failCounter = new AtomicInteger(0);
    }

    public void archive() {
        if (!archiveDBProperties.isEnabled()) {
            archiveSummary.setSkip(true);
            log.info("[{}] Archive is disabled, skip archive", getLogTypeStr());
            return;
        }
        archiveSummary.setEnabled(true);
        log.info("[{}] Start archive, keep days:{}", getLogTypeStr(), archiveDBProperties.getKeepDays());
        List<String> targetCollectionNames = listArchiveCollectionName(archiveDBProperties.getKeepDays());
        log.debug("[{}] archive, target collections:{}", getLogTypeStr(), targetCollectionNames);

        long startTime = System.currentTimeMillis();
        boolean success = true;
        for (String collectionName : targetCollectionNames) {
            if (!acquireLock(collectionName)) {
                continue;
            }
            backupAndDelete(collectionName, isBackupEnable());
        }

        if (targetCollectionNames.size() > 0
            && targetCollectionNames.size() == failCounter.get()) {
            archiveErrorTaskCounter.increment();
            success = false;
        }

        long archiveCost = System.currentTimeMillis() - startTime;
        log.info("[{}] Archive finished, totalCollectionSize:{},deleteCollectionSize:{}, backupCollectionSize:{}," +
            "cost: {}ms",
            getLogTypeStr(),
            targetCollectionNames.size(),
            deleteCounter.get(),
            backupCounter.get(),
            archiveCost
        );
        setArchiveSummary(getLogTypeStr(),
            archiveDBProperties.getMode(),
            archiveCost,
            deleteCounter.get(),
            backupCounter.get(),
            targetCollectionNames.size(),
            success
        );
        storeArchiveSummary();

        countDownLatch.countDown();
    }

    private void backupAndDelete(String collectionName, boolean backupEnabled) {
        long startTime = System.currentTimeMillis();
        boolean success = true;
        try {
            if (backupEnabled) {
                backupRecords(collectionName);
            }
            deleteRecord(collectionName);
        } catch (Exception e) {
            success = false;
            failCounter.incrementAndGet();
            String msg = MessageFormatter.format(
                "[{}] archive error, collectionName:{}",
                getLogTypeStr(),
                collectionName
            ).getMessage();
            log.error(msg, e);
        } finally {
            long archiveCost = System.currentTimeMillis() - startTime;
            log.info(
                "Archive {} finished, result: {}, cost: {}ms",
                collectionName,
                success ? "success" : "fail",
                archiveCost
            );
            if (this.isAcquireLock) {
                archiveTaskLock.unlock(collectionName);
            }
        }
    }

    protected void backupRecords(String collectionName) {
        // 暂不支持备份
    }

    private void deleteRecord(String collectionName) {
        mongoTemplate.dropCollection(collectionName);
        deleteCounter.incrementAndGet();
    }

    /**
     * 获取满足归档条件的集合名称
     */
    private List<String> listArchiveCollectionName(int keepDays) {
        Set<String> allCollectionNames = mongoTemplate.getCollectionNames();
        List<String> targetCollectionNames = filterCollectionNames(allCollectionNames, keepDays);
        return targetCollectionNames;
    }

    /**
     * 从所有集合名称中过滤出要归档的集合名称，返回集合名称列表，并按日期排序
     */
    private List<String> filterCollectionNames(Set<String> collectionNames, int keepDays) {
        LocalDate endDate = LocalDate.now().minusDays(keepDays);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(COLLECTION_NAME_DATE_FORMATTER);
        String endDateString = endDate.format(formatter);
        log.info("[{}] archive,the deadline is {}", getLogTypeStr(), endDateString);
        return collectionNames.stream()
            .filter(collectionName -> {
                if (collectionName.startsWith(getCollectionNamePrefix())) {
                    String dateString = collectionName.substring(getCollectionNamePrefix().length());
                    LocalDate collectionDate = LocalDate.parse(dateString, formatter);
                    return collectionDate.isBefore(endDate);
                }
                return false;
            })
            .sorted((collectionName1, collectionName2) -> {
                LocalDate date1 = extractDateFromCollectionName(collectionName1);
                LocalDate date2 = extractDateFromCollectionName(collectionName2);
                return date1.compareTo(date2);
            })
            .collect(Collectors.toList());
    }

    private String getCollectionNamePrefix() {
        Integer type = mongoDBLogTypeEnum.getValue();
        String prefix = "";
        if (MongoDBLogTypeEnum.SCRIPT.getValue().equals(type)) {
            prefix = SCRIPT_LOG_PREFIX + COLLECTION_NAME_SEPARATOR;
        } else if (MongoDBLogTypeEnum.FILE.getValue().equals(type)) {
            prefix = FILE_LOG_PREFIX + COLLECTION_NAME_SEPARATOR;
        }
        return prefix;
    }

    private LocalDate extractDateFromCollectionName(String collectionName) {
        String dateString = collectionName.substring(getCollectionNamePrefix().length());
        return LocalDate.parse(dateString, DateTimeFormatter.ofPattern(COLLECTION_NAME_DATE_FORMATTER));
    }

    private String getLogTypeStr() {
        Integer type = mongoDBLogTypeEnum.getValue();
        String str = "script_log";
        if (MongoDBLogTypeEnum.FILE.getValue().equals(type)) {
            str = "file_log";
        }
        return str;
    }

    private boolean acquireLock(String collectionName) {
        this.isAcquireLock = archiveTaskLock.lock(collectionName);
        if (!isAcquireLock) {
            log.info("[{}] Acquire lock fail", collectionName);
        }
        return isAcquireLock;
    }

    private boolean isBackupEnable() {
        return archiveDBProperties.isEnabled()
            && ArchiveModeEnum.BACKUP_THEN_DELETE == ArchiveModeEnum.valOf(archiveDBProperties.getMode());
    }

    private void setArchiveSummary(String tableName,
                                   String archiveMode,
                                   long archiveCost,
                                   long deleteCollectionSize,
                                   long backupCollectionSize,
                                   long totalCollectionSize,
                                   boolean success) {
        archiveSummary.setArchiveCost(archiveCost);
        archiveSummary.setArchiveMode(archiveMode);
        archiveSummary.setSuccess(success);
        archiveSummary.setTableName(tableName);
        archiveSummary.setDeleteCollectionSize(deleteCollectionSize);
        archiveSummary.setBackupCollectionSize(backupCollectionSize);
        archiveSummary.setTotalCollectionSize(totalCollectionSize);
    }

    private void storeArchiveSummary() {
        ArchiveSummaryHolder.getInstance().addArchiveSummary(this.archiveSummary);
    }

}
