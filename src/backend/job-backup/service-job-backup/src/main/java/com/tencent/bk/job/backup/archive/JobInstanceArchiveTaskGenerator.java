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

import com.tencent.bk.job.backup.archive.dao.impl.JobInstanceHotRecordDAO;
import com.tencent.bk.job.backup.archive.model.DbDataNode;
import com.tencent.bk.job.backup.archive.model.JobInstanceArchiveTaskInfo;
import com.tencent.bk.job.backup.archive.service.ArchiveTaskService;
import com.tencent.bk.job.backup.archive.util.ArchiveDateTimeUtil;
import com.tencent.bk.job.backup.archive.util.lock.JobInstanceArchiveTaskGenerateLock;
import com.tencent.bk.job.backup.config.ArchiveProperties;
import com.tencent.bk.job.backup.constant.ArchiveTaskStatusEnum;
import com.tencent.bk.job.backup.constant.ArchiveTaskTypeEnum;
import com.tencent.bk.job.backup.constant.DbDataNodeTypeEnum;
import com.tencent.bk.job.common.mysql.dynamic.ds.DataSourceMode;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 生成作业实例归档任务
 */
@Slf4j
public class JobInstanceArchiveTaskGenerator {

    private final ArchiveTaskService archiveTaskService;

    private final JobInstanceHotRecordDAO taskInstanceRecordDAO;

    private final ArchiveProperties archiveProperties;

    private final JobInstanceArchiveTaskGenerateLock archiveTaskGenerateLock;

    /**
     * 归档数据时间范围计算所依据的时区
     */
    private final ZoneId archiveZoneId;


    public JobInstanceArchiveTaskGenerator(ArchiveTaskService archiveTaskService,
                                           JobInstanceHotRecordDAO taskInstanceRecordDAO,
                                           ArchiveProperties archiveProperties,
                                           JobInstanceArchiveTaskGenerateLock archiveTaskGenerateLock) {
        this.archiveTaskService = archiveTaskService;
        this.taskInstanceRecordDAO = taskInstanceRecordDAO;
        this.archiveProperties = archiveProperties;
        this.archiveTaskGenerateLock = archiveTaskGenerateLock;
        archiveZoneId = getArchiveBasedTimeZone(archiveProperties);
    }


    public void generate() {
        boolean locked = false;
        try {
            locked = archiveTaskGenerateLock.lock();
            if (!locked) {
                return;
            }

            if (taskInstanceRecordDAO.isTableEmpty()) {
                log.info("Job instance table is empty and does not require processing");
                return;
            }
            List<JobInstanceArchiveTaskInfo> archiveTaskList = new ArrayList<>();

            log.info("Compute archive task generate startDateTime and endDateTime");
            // 归档任务创建范围-起始时间
            LocalDateTime archiveStartDateTime = computeArchiveStartDateTime();
            // 归档任务创建范围-结束时间
            LocalDateTime archiveEndDateTime = computeArchiveEndTime(archiveProperties.getKeepDays());
            if (archiveEndDateTime.isBefore(archiveStartDateTime) || archiveEndDateTime.equals(archiveStartDateTime)) {
                log.info("Archive endTime is before startTime, does not require generating archive task." +
                        " startTime: {}, endTime: {}",
                    archiveStartDateTime, archiveEndDateTime);
                return;
            }

            log.info("Generate job instance archive tasks between {} and {}", archiveStartDateTime, archiveEndDateTime);
            // 创建归档任务。每个基础归档任务定义为：一个数据节点（db+表）+ 日期 + 小时
            while (archiveStartDateTime.isBefore(archiveEndDateTime)) {
                log.info("Generate archive task for datetime : {}", archiveStartDateTime);
                // 水平分库分表
                if (isHorizontalShardingEnabled()) {
                    // 作业实例数据归档任务,现版本暂不支持
                    archiveTaskList.addAll(buildArchiveTasksForShardingDataNodes(ArchiveTaskTypeEnum.JOB_INSTANCE,
                        archiveStartDateTime, archiveProperties.getTasks().getJobInstance().getShardingDataNodes()));
                } else {
                    // 单db
                    DbDataNode dbDataNode = DbDataNode.standaloneDbDataNode();
                    JobInstanceArchiveTaskInfo archiveTaskInfo =
                        buildArchiveTask(ArchiveTaskTypeEnum.JOB_INSTANCE, archiveStartDateTime, dbDataNode);
                    archiveTaskList.add(archiveTaskInfo);
                    log.info("Add JobInstanceArchiveTaskInfo: {}", JsonUtils.toJson(archiveTaskInfo));
                }

                archiveStartDateTime = archiveStartDateTime.plusHours(1L);
            }

            if (CollectionUtils.isNotEmpty(archiveTaskList)) {
                archiveTaskService.saveArchiveTasks(archiveTaskList);
                log.info("Generate archive tasks : {}", JsonUtils.toJson(archiveTaskList));
            } else {
                log.info("No new archive tasks are generated");
            }
        } catch (Throwable e) {
            log.error("Generate archive task caught exception", e);
        } finally {
            if (locked) {
                archiveTaskGenerateLock.unlock();
            }
        }

    }

    private List<JobInstanceArchiveTaskInfo> buildArchiveTasksForShardingDataNodes(
        ArchiveTaskTypeEnum archiveTaskType,
        LocalDateTime startDateTime,
        List<ArchiveProperties.ShardingDataNode> shardingDataNodes) {
        List<JobInstanceArchiveTaskInfo> tasks = new ArrayList<>();

        // 任务：dataNode + day + hour
        shardingDataNodes.forEach(dataNode -> {
            int dbNodeCount = dataNode.getDbCount();
            int tableNodeCount = dataNode.getTableCount();
            String dataSource = dataNode.getDataSourceName();
            for (int dbNodeIndex = 0; dbNodeIndex < dbNodeCount; dbNodeIndex++) {
                for (int tableNodeIndex = 0; tableNodeIndex < tableNodeCount; tableNodeIndex++) {
                    DbDataNode dbDataNode = new DbDataNode(DbDataNodeTypeEnum.SHARDING, dataSource,
                        dbNodeIndex, tableNodeIndex);
                    // 作业实例数据归档任务
                    JobInstanceArchiveTaskInfo archiveTaskInfo =
                        buildArchiveTask(archiveTaskType, startDateTime, dbDataNode);
                    tasks.add(archiveTaskInfo);
                    log.info("Add JobInstanceArchiveTaskInfo: {}", JsonUtils.toJson(archiveTaskInfo));
                }
            }
        });

        return tasks;
    }

    private JobInstanceArchiveTaskInfo buildArchiveTask(ArchiveTaskTypeEnum archiveTaskType,
                                                        LocalDateTime startDateTime,
                                                        DbDataNode dbDataNode) {
        JobInstanceArchiveTaskInfo archiveTask = new JobInstanceArchiveTaskInfo();
        int day = ArchiveDateTimeUtil.computeDay(startDateTime);
        int hour = ArchiveDateTimeUtil.computeHour(startDateTime);
        archiveTask.setDay(day);
        archiveTask.setHour(hour);
        long fromTimestamp = ArchiveDateTimeUtil.toTimestampMillsAtZone(startDateTime, archiveZoneId);
        archiveTask.setFromTimestamp(fromTimestamp);
        archiveTask.setToTimestamp(fromTimestamp + 1000 * 3600L);
        archiveTask.setTaskType(archiveTaskType);
        archiveTask.setDbDataNode(dbDataNode);
        archiveTask.setStatus(ArchiveTaskStatusEnum.PENDING);
        return archiveTask;
    }


    private LocalDateTime computeArchiveStartDateTime() {
        LocalDateTime startDateTime;
        JobInstanceArchiveTaskInfo latestArchiveTask =
            archiveTaskService.getLatestArchiveTask(ArchiveTaskTypeEnum.JOB_INSTANCE);
        if (latestArchiveTask == null) {
            // 从表数据中的 job_create_time 计算归档任务开始时间
            log.info("Latest archive task is empty, try compute from table min job create time");
            Long minJobCreateTimeMills = taskInstanceRecordDAO.getMinJobInstanceCreateTime();
            log.info("Min job create time in db is : {}", minJobCreateTimeMills);
            startDateTime = ArchiveDateTimeUtil.toHourlyRoundDown(
                ArchiveDateTimeUtil.unixTimestampMillToZoneDateTime(minJobCreateTimeMills, archiveZoneId));
        } else {
            // 根据最新的归档任务计算开始
            log.info("Compute archive from latest generated archive task: {}", JsonUtils.toJson(latestArchiveTask));
            startDateTime = ArchiveDateTimeUtil.unixTimestampMillToZoneDateTime(
                latestArchiveTask.getToTimestamp(), archiveZoneId);
        }

        return startDateTime;
    }

    /**
     * 获取归档数据时间范围计算所依据的时区
     *
     * @param archiveProperties 归档配置
     * @return 时区
     */
    private ZoneId getArchiveBasedTimeZone(ArchiveProperties archiveProperties) throws DateTimeException {
        ZoneId zoneId;
        if (StringUtils.isBlank(archiveProperties.getTimeZone())) {
            zoneId = ZoneId.systemDefault();
            log.info("Use system zone as archive base time zone, zoneId: {}", zoneId);
            return zoneId;
        }
        zoneId = ZoneId.of(archiveProperties.getTimeZone());
        log.info("Use configured zone as archive base time zone, zoneId: {}", zoneId);
        return zoneId;
    }

    private boolean isHorizontalShardingEnabled() {
        return archiveProperties.getTasks().getJobInstance().getDataSourceMode()
            .equals(DataSourceMode.Constants.HORIZONTAL_SHARDING);
    }

    private LocalDateTime computeArchiveEndTime(int archiveDays) {
        log.info("Compute archive task generate end time before {} days", archiveDays);
        LocalDateTime now = LocalDateTime.now(archiveZoneId);
        return ArchiveDateTimeUtil.computeStartOfDayBeforeDays(now, archiveDays);
    }
}
