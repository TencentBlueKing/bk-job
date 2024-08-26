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

import com.tencent.bk.job.backup.archive.dao.impl.TaskInstanceRecordDAO;
import com.tencent.bk.job.backup.archive.model.DbDataNode;
import com.tencent.bk.job.backup.archive.model.JobInstanceArchiveTaskInfo;
import com.tencent.bk.job.backup.config.ArchiveProperties;
import com.tencent.bk.job.backup.constant.ArchiveTaskStatusEnum;
import com.tencent.bk.job.backup.constant.ArchiveTaskTypeEnum;
import com.tencent.bk.job.backup.constant.DbDataNodeTypeEnum;
import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.common.sharding.mysql.config.ShardingProperties;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.DateTime;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * 生成作业实例归档任务
 */
@Slf4j
public class JobInstanceArchiveTaskGenerator {

    private final ArchiveTaskService archiveTaskService;

    private final TaskInstanceRecordDAO taskInstanceRecordDAO;

    private final ArchiveProperties archiveProperties;

    private final ShardingProperties shardingProperties;


    public JobInstanceArchiveTaskGenerator(ArchiveTaskService archiveTaskService,
                                           TaskInstanceRecordDAO taskInstanceRecordDAO,
                                           ArchiveProperties archiveProperties,
                                           ShardingProperties shardingProperties) {

        this.archiveTaskService = archiveTaskService;
        this.taskInstanceRecordDAO = taskInstanceRecordDAO;
        this.archiveProperties = archiveProperties;
        this.shardingProperties = shardingProperties;
    }

    @JobTransactional(transactionManager = "jobBackupTransactionManager")
    public void generate() {
        List<JobInstanceArchiveTaskInfo> archiveTaskList = new ArrayList<>();

        LocalDateTime startDateTime = computeArchiveStartDateTime();
        LocalDateTime endDateTime = unixTimestampToUtcLocalDateTime(
            getEndTime(archiveProperties.getKeepDays()));
        while (startDateTime.isBefore(endDateTime)) {
            int day = computeDay(startDateTime);
            int hour = computeHour(startDateTime);

            JobInstanceArchiveTaskInfo archiveTask = buildBasicArchiveTask(day, hour, startDateTime);
            if (isShardingTable()) {
                int dbNodeCount = shardingProperties.getDbNodeCount();
                int tableNodeCount = shardingProperties.getTableNodeCount();
                for (int dbNodeIndex = 0; dbNodeIndex < dbNodeCount; dbNodeIndex++) {
                    for (int tableNodeIndex = 0; tableNodeIndex < tableNodeCount; tableNodeIndex++) {
                        DbDataNode dbDataNode = new DbDataNode(DbDataNodeTypeEnum.SHARD, dbNodeIndex, tableNodeIndex);
                        // 作业实例数据归档任务
                        archiveTaskList.add(buildNewArchiveTask(archiveTask, ArchiveTaskTypeEnum.JOB_INSTANCE,
                            dbDataNode));
                        // 作业实例按业务冗余数据归档
                        archiveTaskList.add(buildNewArchiveTask(archiveTask, ArchiveTaskTypeEnum.JOB_INSTANCE_APP,
                            dbDataNode));
                    }
                }
            } else {
                DbDataNode dbDataNode = new DbDataNode(DbDataNodeTypeEnum.SINGLE, 0, 0);
                archiveTask.setDbDataNode(dbDataNode);
                archiveTaskList.add(buildNewArchiveTask(archiveTask, ArchiveTaskTypeEnum.JOB_INSTANCE, dbDataNode));
                archiveTaskList.add(buildNewArchiveTask(archiveTask, ArchiveTaskTypeEnum.JOB_INSTANCE_APP, dbDataNode));
            }

            startDateTime = startDateTime.plusHours(1L);
        }

        if (CollectionUtils.isNotEmpty(archiveTaskList)) {
            archiveTaskList.forEach(archiveTaskService::saveArchiveTask);
            log.info("Add archive tasks : {}", JsonUtils.toJson(archiveTaskList));
        }
    }

    private JobInstanceArchiveTaskInfo buildNewArchiveTask(JobInstanceArchiveTaskInfo basicArchiveTask,
                                                           ArchiveTaskTypeEnum archiveTaskType,
                                                           DbDataNode dbDataNode) {
        JobInstanceArchiveTaskInfo newArchiveTask = basicArchiveTask.clone();
        newArchiveTask.setTaskType(archiveTaskType);
        newArchiveTask.setDbDataNode(dbDataNode);
        return newArchiveTask;
    }

    private JobInstanceArchiveTaskInfo buildBasicArchiveTask(Integer day, Integer hour, LocalDateTime startDateTime) {
        JobInstanceArchiveTaskInfo archiveTask = new JobInstanceArchiveTaskInfo();
        archiveTask.setDay(day);
        archiveTask.setHour(hour);
        archiveTask.setStatus(ArchiveTaskStatusEnum.PENDING);
        archiveTask.setFromTimestamp(1000 * startDateTime.toEpochSecond(ZoneOffset.UTC));
        archiveTask.setToTimestamp(1000L * startDateTime.plusHours(1L).toEpochSecond(ZoneOffset.UTC));
        return archiveTask;
    }


    private LocalDateTime computeArchiveStartDateTime() {
        LocalDateTime startDateTime;
        JobInstanceArchiveTaskInfo latestArchiveTask =
            archiveTaskService.getLatestArchiveTask(ArchiveTaskTypeEnum.JOB_INSTANCE);
        if (latestArchiveTask == null) {
            log.info("Latest archive task is empty, try compute from task_instance table record");
            Long minJobCreateTime = taskInstanceRecordDAO.getMinJobCreateTime();
            startDateTime = toHourlyRoundDown(unixTimestampToUtcLocalDateTime(minJobCreateTime));
        } else {
            startDateTime = unixTimestampToUtcLocalDateTime(latestArchiveTask.getToTimestamp());
        }

        return startDateTime;
    }

    private boolean isShardingTable() {
        return shardingProperties != null && shardingProperties.isEnabled();
    }

    private int computeDay(LocalDateTime dateTime) {
        return Integer.parseInt(DateUtils.formatLocalDateTime(dateTime, "%Y%m%d"));
    }

    private int computeHour(LocalDateTime dateTime) {
        return dateTime.getHour();
    }

    private Long getEndTime(int archiveDays) {
        DateTime now = DateTime.now();
        // 置为前一天天 24:00:00
        long todayMaxMills = now.minusMillis(now.getMillisOfDay()).getMillis();

        //减掉当前xx天后
        long archiveMills = archiveDays * 24 * 3600 * 1000L;
        return todayMaxMills - archiveMills;
    }

    private LocalDateTime unixTimestampToUtcLocalDateTime(long unixTimestamp) {
        // 创建一个 Instant 对象，表示从 1970-01-01T00:00:00Z 开始的指定秒数
        Instant instant = Instant.ofEpochSecond(unixTimestamp);
        // 将 Instant 对象转换为 UTC 时区的 LocalDateTime 对象
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private LocalDateTime toHourlyRoundDown(LocalDateTime localDateTime) {
        return localDateTime.withMinute(0).withSecond(0).withNano(0);
    }
}
