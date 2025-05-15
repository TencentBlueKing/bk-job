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

import com.tencent.bk.job.backup.archive.model.ArchiveTaskInfo;
import com.tencent.bk.job.backup.archive.model.DbDataNode;
import com.tencent.bk.job.backup.archive.service.ArchiveTaskService;
import com.tencent.bk.job.backup.archive.util.ArchiveDateTimeUtil;
import com.tencent.bk.job.backup.archive.util.lock.JobExecuteLogArchiveTaskGenerateLock;
import com.tencent.bk.job.backup.config.ArchiveProperties;
import com.tencent.bk.job.backup.constant.ArchiveTaskStatusEnum;
import com.tencent.bk.job.backup.constant.ArchiveTaskTypeEnum;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 生成作业执行日志归档任务
 */
@Slf4j
public class JobExecuteLogArchiveTaskGenerator {

    private final ArchiveTaskService archiveTaskService;

    private final ArchiveProperties archiveProperties;

    private final JobExecuteLogArchiveTaskGenerateLock jobExecuteLogArchiveTaskGenerateLock;

    private final MongoTemplate mongoTemplate;

    /**
     * 归档数据时间范围计算所依据的时区
     */
    private final ZoneId archiveZoneId;

    // 匹配脚本日志、文件日志的集合名称
    private static final Pattern LOG_COLLECTION_NAME_PATTERN = Pattern.compile(
        "job_log_(?:script|file)_(\\d{4}_\\d{2}_\\d{2})"
    );

    private static final DateTimeFormatter DATE_FORMAT_YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy_MM_dd");


    public JobExecuteLogArchiveTaskGenerator(ArchiveTaskService archiveTaskService,
                                             ArchiveProperties archiveProperties,
                                             JobExecuteLogArchiveTaskGenerateLock jobExecuteLogArchiveTaskGenerateLock,
                                             MongoTemplate mongoTemplate) {
        this.archiveTaskService = archiveTaskService;
        this.archiveProperties = archiveProperties;
        this.jobExecuteLogArchiveTaskGenerateLock = jobExecuteLogArchiveTaskGenerateLock;
        this.mongoTemplate = mongoTemplate;
        archiveZoneId = ArchiveDateTimeUtil.getArchiveBasedTimeZone(archiveProperties.getTimeZone());
    }

    public void generate() {
        boolean locked = false;
        try {
            locked = jobExecuteLogArchiveTaskGenerateLock.lock();
            if (!locked) {
                return;
            }

            // 根据集合名称找出最早的一个
            Set<String> allCollections = mongoTemplate.getCollectionNames();
            Optional<String> earliestCollection = findEarliestCollection(allCollections);
            if (!earliestCollection.isPresent()) {
                log.info("No collection to be archived was found，allCollections={}", allCollections);
                return;
            }

            log.info("Compute archive log task generate startDateTime and endDateTime");
            LocalDateTime archiveStartDateTime = computeDateTimeByCollectionName(earliestCollection.get());
            LocalDateTime archiveEndDateTime =
                ArchiveDateTimeUtil.computeArchiveEndTime(archiveProperties.getExecuteLog().getKeepDays(),
                    archiveZoneId);
            if (archiveEndDateTime.isBefore(archiveStartDateTime)
                || archiveEndDateTime.equals(archiveStartDateTime)) {
                log.info("Archive endTime is before startTime, do not require to set up archive log task." +
                        " startTime: {}, endTime: {}",
                    archiveStartDateTime, archiveEndDateTime);
                return;
            }

            log.info("Generate job execute log archive tasks between {} and {}",
                archiveStartDateTime, archiveEndDateTime);
            List<ArchiveTaskInfo> archiveTaskList = new ArrayList<>();
            // 按天创建日志归档任务
            while (archiveStartDateTime.isBefore(archiveEndDateTime)) {
                log.info("Generate archive log task for datetime : {}", archiveStartDateTime);
                DbDataNode dbDataNode = DbDataNode.standaloneMongoDbDataNode();
                ArchiveTaskInfo archiveTaskInfo =
                    buildArchiveTask(ArchiveTaskTypeEnum.JOB_EXECUTE_LOG, archiveStartDateTime, dbDataNode);
                archiveTaskList.add(archiveTaskInfo);
                log.info("Add JobInstanceArchiveTaskInfo: {}", JsonUtils.toJson(archiveTaskInfo));
                archiveStartDateTime = archiveStartDateTime.plusDays(1);
            }

            if (CollectionUtils.isNotEmpty(archiveTaskList)) {
                // 获取已有的归档任务列表，如果任务已存在就不用重新创建了
                List<ArchiveTaskInfo> existedArchiveTaskList =
                    archiveTaskService.listTasksSinceDay(ArchiveTaskTypeEnum.JOB_EXECUTE_LOG,
                        archiveTaskList.get(0).getDay());
                if (CollectionUtils.isNotEmpty(existedArchiveTaskList)) {
                    Set<String> existedIds = existedArchiveTaskList.stream()
                        .map(ArchiveTaskInfo::buildTaskUniqueId)
                        .collect(Collectors.toSet());
                    archiveTaskList.removeIf(task -> existedIds.contains(task.buildTaskUniqueId()));
                }
                archiveTaskService.saveArchiveTasks(archiveTaskList);
                log.info("Generate archive log tasks : {}", JsonUtils.toJson(archiveTaskList));
            } else {
                log.info("No new archive log tasks are generated");
            }
        } catch (Throwable e) {
            log.error("Generate archive log task caught exception", e);
        } finally {
            if (locked) {
                jobExecuteLogArchiveTaskGenerateLock.unlock();
            }
        }
    }

    /**
     * 解析集合名中的日期，匹配失败返回null。
     */
    private LocalDate collectionNameToDate(String collectionName) {
        Matcher m = LOG_COLLECTION_NAME_PATTERN.matcher(collectionName);
        if (!m.matches()) {
            return null;
        }
        return LocalDate.parse(m.group(1), DATE_FORMAT_YYYY_MM_DD);
    }

    /**
     * 找到最早的集合名。
     */
    private Optional<String> findEarliestCollection(Set<String> allCollections) {
        if (allCollections.isEmpty()) return Optional.empty();
        return allCollections.stream()
            .filter(name -> collectionNameToDate(name) != null)
            .min(Comparator.comparing(this::collectionNameToDate));
    }

    private ArchiveTaskInfo buildArchiveTask(ArchiveTaskTypeEnum archiveTaskType,
                                             LocalDateTime startDateTime,
                                             DbDataNode dbDataNode) {
        ArchiveTaskInfo archiveTask = new ArchiveTaskInfo();
        int day = ArchiveDateTimeUtil.computeDay(startDateTime);
        archiveTask.setDay(day);
        // 执行日志按天清理，小时不填
        archiveTask.setHour(-1);
        long fromTimestamp = startDateTime.with(LocalTime.MIN).atZone(archiveZoneId).toInstant().toEpochMilli();
        long toTimestamp = startDateTime.with(LocalTime.MAX).atZone(archiveZoneId).toInstant().toEpochMilli();
        archiveTask.setFromTimestamp(fromTimestamp);
        archiveTask.setToTimestamp(toTimestamp);
        archiveTask.setTaskType(archiveTaskType);
        archiveTask.setDbDataNode(dbDataNode);
        archiveTask.setStatus(ArchiveTaskStatusEnum.PENDING);
        return archiveTask;
    }

    private LocalDateTime computeDateTimeByCollectionName(String name) {
        LocalDate date = collectionNameToDate(name);
        return date.atStartOfDay();
    }
}
