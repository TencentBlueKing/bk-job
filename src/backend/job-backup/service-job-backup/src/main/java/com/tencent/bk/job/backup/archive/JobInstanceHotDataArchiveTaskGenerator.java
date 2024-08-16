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

import com.tencent.bk.job.backup.archive.dao.ArchiveTaskDAO;
import com.tencent.bk.job.backup.archive.dao.impl.TaskInstanceRecordDAO;
import com.tencent.bk.job.backup.archive.model.DbDataNode;
import com.tencent.bk.job.backup.archive.model.JobInstanceArchiveTask;
import com.tencent.bk.job.backup.config.ArchiveDBProperties;
import com.tencent.bk.job.backup.constant.ArchiveTaskStatusEnum;
import com.tencent.bk.job.backup.constant.ArchiveTaskTypeEnum;
import com.tencent.bk.job.backup.constant.DbDataNodeTypeEnum;
import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.common.sharding.mysql.config.ShardingProperties;
import com.tencent.bk.job.common.util.date.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.DateTime;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * 生成归档任务
 */
@Slf4j
public class JobInstanceHotDataArchiveTaskGenerator {

    private final ArchiveTaskDAO archiveTaskDAO;

    private final TaskInstanceRecordDAO taskInstanceRecordDAO;

    private final ArchiveDBProperties archiveDBProperties;

    private final ShardingProperties shardingProperties;


    public JobInstanceHotDataArchiveTaskGenerator(ArchiveTaskDAO archiveTaskDAO,
                                                  TaskInstanceRecordDAO taskInstanceRecordDAO,
                                                  ArchiveDBProperties archiveDBProperties,
                                                  ShardingProperties shardingProperties) {

        this.archiveTaskDAO = archiveTaskDAO;
        this.taskInstanceRecordDAO = taskInstanceRecordDAO;
        this.archiveDBProperties = archiveDBProperties;
        this.shardingProperties = shardingProperties;
    }

    @JobTransactional(transactionManager = "jobBackupTransactionManager")
    public void generate() {
        List<JobInstanceArchiveTask> archiveTaskList = new ArrayList<>();

        LocalDateTime startDateTime = computeArchiveStartDateTime();
        LocalDateTime endDateTime = unixTimestampToUtcLocalDateTime(
            getEndTime(archiveDBProperties.getJobInstanceHotData().getKeepDays()));
        while (startDateTime.isBefore(endDateTime)) {
            int day = computeDay(startDateTime);
            int hour = computeHour(startDateTime);

            JobInstanceArchiveTask archiveTask = buildHourArchiveTask(day, hour, startDateTime);
            if (isShardingTable()) {
                int dbNodeCount = shardingProperties.getDbNodeCount();
                int tableNodeCount = shardingProperties.getTableNodeCount();
                for (int dbNodeIndex = 0; dbNodeIndex < dbNodeCount; dbNodeIndex++) {
                    for (int tableNodeIndex = 0; tableNodeIndex < tableNodeCount; tableNodeIndex++) {
                        JobInstanceArchiveTask shardingArchiveTask = archiveTask.clone();
                        DbDataNode dbDataNode = new DbDataNode(DbDataNodeTypeEnum.SHARD, dbNodeIndex, tableNodeIndex);
                        shardingArchiveTask.setTaskDesc(buildTaskDesc(dbDataNode, day, hour));
                        shardingArchiveTask.setDbDataNode(dbDataNode);
                        archiveTaskList.add(shardingArchiveTask);
                        log.info("Add archive task for sharding table, dataNodeIndex: {}, day: {}, hour: {}",
                            dbDataNode.toDataNodeId(), day, hour);
                    }
                }
            } else {
                DbDataNode dbDataNode = new DbDataNode(DbDataNodeTypeEnum.SINGLE, 0, 0);
                archiveTask.setDbDataNode(dbDataNode);
                archiveTask.setTaskDesc(buildTaskDesc(dbDataNode, day, hour));
                archiveTaskList.add(archiveTask);
                log.info("Add archive task for table, dataNodeIndex: {}, day: {}, hour: {}",
                    dbDataNode.toDataNodeId(), day, hour);
            }

            startDateTime = startDateTime.plusHours(1L);
        }

        if (CollectionUtils.isNotEmpty(archiveTaskList)) {
            archiveTaskList.forEach(archiveTaskDAO::saveArchiveTask);
        }
    }

    private JobInstanceArchiveTask buildHourArchiveTask(Integer day, Integer hour, LocalDateTime startDateTime) {
        JobInstanceArchiveTask archiveTask = new JobInstanceArchiveTask();
        archiveTask.setTaskType(ArchiveTaskTypeEnum.JOB_INSTANCE_HOT);
        archiveTask.setDay(day);
        archiveTask.setHour(hour);
        archiveTask.setStatus(ArchiveTaskStatusEnum.PENDING);
        archiveTask.setFromTimestamp(1000 * startDateTime.toEpochSecond(ZoneOffset.UTC));
        archiveTask.setToTimestamp(1000L * startDateTime.plusHours(1L).toEpochSecond(ZoneOffset.UTC));
        return archiveTask;
    }


    private LocalDateTime computeArchiveStartDateTime() {
        LocalDateTime startDateTime;
        JobInstanceArchiveTask latestArchiveTask = archiveTaskDAO.getLatestArchiveTask(ArchiveTaskTypeEnum.JOB_INSTANCE_HOT);
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

    private String buildTaskDesc(DbDataNode dbDataNode, Integer day, Integer hour) {
        return "job_instance:" + dbDataNode.toDataNodeId() + ":" + day + ":" + hour;
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
