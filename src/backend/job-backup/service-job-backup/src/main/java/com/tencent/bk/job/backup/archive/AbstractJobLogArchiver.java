/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

import com.tencent.bk.job.backup.archive.model.ArchiveTaskInfo;
import com.tencent.bk.job.backup.archive.model.BackupResult;
import com.tencent.bk.job.backup.archive.model.DeleteResult;
import com.tencent.bk.job.backup.archive.service.ArchiveTaskService;
import com.tencent.bk.job.backup.config.JobLogArchiveProperties;
import com.tencent.bk.job.backup.constant.ArchiveModeEnum;
import com.tencent.bk.job.backup.constant.ArchiveTaskStatusEnum;
import com.tencent.bk.job.backup.constant.ArchiveTaskTypeEnum;
import com.tencent.bk.job.logsvr.consts.LogTypeEnum;
import com.tencent.bk.job.logsvr.util.CollectionNameUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 执行日志归档基础实现
 */
@Slf4j
public abstract class AbstractJobLogArchiver implements JobLogArchiver {
    private final JobLogArchiveProperties archiveProperties;
    private final ArchiveTaskService archiveTaskService;
    private final LogTypeEnum logTypeEnum;
    private final MongoTemplate mongoTemplate;
    private static final String ARCHIVE_DAY_FORMATTER = "yyyyMMdd";
    private static final String COLLECTION_NAME_DATE_FORMATTER = "yyyy_MM_dd";
    private static final DateTimeFormatter INPUT_DATE_FORMATTER = DateTimeFormatter.ofPattern(ARCHIVE_DAY_FORMATTER);
    private static final DateTimeFormatter OUTPUT_DATE_FORMATTER =
        DateTimeFormatter.ofPattern(COLLECTION_NAME_DATE_FORMATTER);

    public AbstractJobLogArchiver(MongoTemplate mongoTemplate,
                                  ArchiveTaskService archiveTaskService,
                                  JobLogArchiveProperties archiveProperties,
                                  LogTypeEnum logTypeEnum) {
        this.mongoTemplate = mongoTemplate;
        this.archiveTaskService = archiveTaskService;
        this.archiveProperties = archiveProperties;
        this.logTypeEnum = logTypeEnum;
    }

    @Override
    public DeleteResult deleteRecords(Integer archiveDay) {
        // 作业执行日志按集合删除，当前仅支持删除
        if (!isDeleteEnable()) {
            log.info("Delete job execute log is disabled, skip delete, mode={}",
                archiveProperties.getMode());
            return DeleteResult.NON_OP_DELETE_RESULT;
        }
        String collectionName = getCollectionName(archiveDay);
        if (archiveProperties.isDryRun()) {
            log.info("Dry-run mode is enabled, skipping the actual operation of drop the collection [{}]",
                collectionName);
            return DeleteResult.NON_OP_DELETE_RESULT;
        }

        List<ArchiveTaskInfo> archiveTaskInfoList = archiveTaskService.listTasks(ArchiveTaskTypeEnum.JOB_INSTANCE,
            archiveDay);
        if (archiveTaskInfoList.isEmpty()) {
            log.warn("Job instance has not been deleted yet, " +
                "execution log will not be found in the details of the job execution history. " +
                "date={}, dropCollection={}", archiveDay, collectionName);
        }
        List<ArchiveTaskInfo> unfinishedTaskList = archiveTaskInfoList.stream()
            .filter(taskInfo -> taskInfo.getStatus().getStatus() != ArchiveTaskStatusEnum.SUCCESS.getStatus())
            .collect(Collectors.toList());
        if (!unfinishedTaskList.isEmpty()) {
            log.warn("Job instance archive task is not fully completed, " +
                "execution log will not be found in the details of the job execution history. " +
                "date={}, dropCollection={}", archiveDay, collectionName);
        }
        long deleteStartTime = System.currentTimeMillis();
        log.info("Drop [{}] collection", collectionName);
        mongoTemplate.dropCollection(collectionName);
        long deleteCost = System.currentTimeMillis() - deleteStartTime;
        ArchiveTaskContextHolder.get().accumulateTableDelete(
            collectionName,
            0,
            deleteCost
        );
        return new DeleteResult(-1, deleteCost);
    }

    @Override
    public BackupResult backupRecords(Integer archiveDay) {
        // 暂不支持执行日志备份
        log.info("[{}] Log archiving is not supported", archiveDay);
        return BackupResult.NON_OP_BACKUP_RESULT;
    }

    private String getCollectionName(Integer day) {
        String prefix = CollectionNameUtil.buildCollectionNamePrefix(logTypeEnum);
        String formattedDate = LocalDate.parse(
            String.valueOf(day),
            INPUT_DATE_FORMATTER
        ).format(OUTPUT_DATE_FORMATTER);
        return prefix + formattedDate;
    }

    private boolean isDeleteEnable() {
        return archiveProperties.isEnabled()
            && ArchiveModeEnum.DELETE_ONLY == ArchiveModeEnum.valOf(archiveProperties.getMode());
    }
}
