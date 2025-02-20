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
import org.apache.commons.lang3.StringUtils;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 生成作业执行日志归档任务
 */
@Slf4j
public class JobExecuteLogArchiveTaskGenerator {

    private final ArchiveTaskService archiveTaskService;

    private final JobInstanceHotRecordDAO taskInstanceRecordDAO;

    private final ArchiveProperties archiveProperties;

    private final JobExecuteLogArchiveTaskGenerateLock jobExecuteLogArchiveTaskGenerateLock;

    /**
     * 归档数据时间范围计算所依据的时区
     */
    private final ZoneId archiveZoneId;


    public JobExecuteLogArchiveTaskGenerator(ArchiveTaskService archiveTaskService,
                                             JobInstanceHotRecordDAO taskInstanceRecordDAO,
                                             ArchiveProperties archiveProperties,
                                             JobExecuteLogArchiveTaskGenerateLock jobExecuteLogArchiveTaskGenerateLock) {
        this.archiveTaskService = archiveTaskService;
        this.taskInstanceRecordDAO = taskInstanceRecordDAO;
        this.archiveProperties = archiveProperties;
        this.jobExecuteLogArchiveTaskGenerateLock = jobExecuteLogArchiveTaskGenerateLock;
        archiveZoneId = getArchiveBasedTimeZone(archiveProperties);
    }


    public void generate() {
        boolean locked = false;
        try {
            locked = jobExecuteLogArchiveTaskGenerateLock.lock();
            if (!locked) {
                return;
            }
            if (taskInstanceRecordDAO.isTableEmpty()) {
                log.info("Job instance table is empty,do not require to set up archive log task.");
                return;
            }
            List<ArchiveTaskInfo> archiveTaskList = new ArrayList<>();

            log.info("Compute archive log task generate startDateTime and endDateTime");
            LocalDateTime archiveStartDateTime = computeArchiveStartDateTime();
            LocalDateTime archiveEndDateTime = computeArchiveEndTime(archiveProperties.getKeepDays());
            if (archiveEndDateTime.isBefore(archiveStartDateTime) || archiveEndDateTime.equals(archiveStartDateTime)) {
                log.info("Archive endTime is before startTime, do not require to set up archive log task." +
                        " startTime: {}, endTime: {}",
                    archiveStartDateTime, archiveEndDateTime);
                return;
            }

            log.info("Generate job execute log archive tasks between {} and {}", archiveStartDateTime, archiveEndDateTime);
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


    private LocalDateTime computeArchiveStartDateTime() {
        LocalDateTime startDateTime;
        ArchiveTaskInfo latestArchiveTask =
            archiveTaskService.getLatestArchiveTask(ArchiveTaskTypeEnum.JOB_EXECUTE_LOG);
        if (latestArchiveTask == null) {
            // 从表数据中的 job_create_time 计算归档任务开始时间
            log.info("Latest execute log archive task is empty, try compute from table min job create time");
            Long minJobCreateTimeMills = taskInstanceRecordDAO.getMinJobInstanceCreateTime();
            log.info("Min job create time in db is : {}", minJobCreateTimeMills);
            startDateTime = ArchiveDateTimeUtil.toHourlyRoundDown(
                ArchiveDateTimeUtil.unixTimestampMillToZoneDateTime(minJobCreateTimeMills, archiveZoneId));
        } else {
            // 根据最新的归档任务计算开始
            log.info("Compute archive from latest generated archive task: {}", JsonUtils.toJson(latestArchiveTask));
            startDateTime = ArchiveDateTimeUtil.unixTimestampMillToZoneDateTime(
                latestArchiveTask.getToTimestamp(), archiveZoneId);
            startDateTime = ArchiveDateTimeUtil.toHourlyRoundDown(startDateTime.plusDays(1));
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

    private LocalDateTime computeArchiveEndTime(int archiveDays) {
        log.info("Compute archive task generate end time before {} days", archiveDays);
        LocalDateTime now = LocalDateTime.now(archiveZoneId);
        return ArchiveDateTimeUtil.computeStartOfDayBeforeDays(now, archiveDays);
    }
}
