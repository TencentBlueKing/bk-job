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

package com.tencent.bk.job.backup.archive.service.detector;

import com.tencent.bk.job.backup.archive.dao.ArchiveTaskDAO;
import com.tencent.bk.job.backup.archive.dao.impl.AbstractJobInstanceHotRecordDAO;
import com.tencent.bk.job.backup.archive.model.ArchiveTaskInfo;
import com.tencent.bk.job.backup.archive.util.ArchiveDateTimeUtil;
import com.tencent.bk.job.backup.config.ArchiveProperties;
import com.tencent.bk.job.backup.constant.ArchiveTaskStatusEnum;
import com.tencent.bk.job.backup.constant.ArchiveTaskTypeEnum;
import com.tencent.bk.job.backup.metrics.MetricConstants;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jooq.TableRecord;

import java.time.LocalDateTime;

/**
 * 未归档数据检测，用于检测热库中是否存在未归档的数据
 * 检测标准：
 * 1. 是否存在归档时间以前的数据
 * 2. 这个时间内对应的归档任务状态为已完成
 */
@Slf4j
public abstract class UnarchivedDataDetector {

    private final ArchiveTaskDAO archiveTaskDAO;
    private final Counter counter;
    private final ArchiveProperties archiveProperties;
    private final AbstractJobInstanceHotRecordDAO<? extends TableRecord<?>> hotRecordDAO;

    public UnarchivedDataDetector(MeterRegistry meterRegistry,
                                  ArchiveTaskDAO archiveTaskDAO,
                                  ArchiveProperties archiveProperties,
                                  AbstractJobInstanceHotRecordDAO<? extends TableRecord<?>> hotRecordDAO) {
        this.archiveTaskDAO = archiveTaskDAO;
        this.counter = meterRegistry.counter(
            MetricConstants.METRIC_NAME_UNARCHIVED_DATA_COUNT,
            MetricConstants.TAG_KEY_UNARCHIVED_TABLE_NAME, getTableName());
        this.archiveProperties = archiveProperties;
        this.hotRecordDAO = hotRecordDAO;
    }

    public void detect() {
        DetectResult detectResult = doDetect();
        if (!detectResult.isHasUnarchivedData()) {
            return;
        }

        // 重调度这种未完成的任务
        recordIfUnarchived();
        if (archiveProperties.getCheck().isRescheduleEnabled()) {
            reschedule(detectResult);
        }
    }

    private void recordIfUnarchived() {
        counter.increment();
    }

    private void reschedule(DetectResult detectResult) {
        log.info("reschedule archive task: {} with detect result[{}]", getTableName(), detectResult);
        int day = detectResult.getDay();
        int hour = detectResult.getHour();
        ArchiveTaskInfo taskInfo = archiveTaskDAO.getTaskByDayHour(ArchiveTaskTypeEnum.JOB_INSTANCE, day, hour);
        // 状态设置为PENDING，下次归档任务会自动重新执行归档任务
        archiveTaskDAO.updateArchiveTaskStatus(
            ArchiveTaskTypeEnum.JOB_INSTANCE,
            taskInfo.getDbDataNode(),
            day,
            hour,
            ArchiveTaskStatusEnum.PENDING);
    }

    /**
     * 检查主表或子表是否存在未归档的数据
     * @return 是否存在未归档数据
     */
    protected DetectResult doDetect() {
        log.info("doDetect table: {} and check if there is unarchived data", getTableName());
        LocalDateTime time = getTimeWithMinJobInstanceId();
        // 允许存在的记录的最早时间
        LocalDateTime expectedEarliestTime = LocalDateTime.now().minusDays(archiveProperties.getKeepDays() + 3);
        log.debug("expectedEarliestTime: {}", expectedEarliestTime);
        if (time == null || time.isAfter(expectedEarliestTime)) {
            log.info("table [{}] no unarchived data", getTableName());
            return DetectResult.notHasUnarchivedData();
        }
        // 存在最大保留时间以外的数据，判断归档任务是否已完成，如果已完成则视为归档失败
        int day = ArchiveDateTimeUtil.computeDay(time);
        int hour = ArchiveDateTimeUtil.computeHour(time);
        ArchiveTaskInfo taskInfo = archiveTaskDAO.getTaskByDayHour(ArchiveTaskTypeEnum.JOB_INSTANCE, day, hour);
        ArchiveTaskStatusEnum status = taskInfo == null ? null : taskInfo.getStatus();
        // 归档任务已完成，但存在未归档数据，视为归档异常
        // 若是归档任务正在执行中，不视为归档异常
        boolean hasUnarchivedData = status == null || status == ArchiveTaskStatusEnum.SUCCESS;
        if (hasUnarchivedData) {
            log.warn("[{}] has unarchived data, time is {}-{}", getTableName(), day, hour);
        }
        return DetectResult.build(hasUnarchivedData, time);
    }

    protected LocalDateTime getTimeWithMinJobInstanceId() {
        return hotRecordDAO.getTimeWithMinJobInstanceId();
    }

    protected abstract String getTableName();

    @Data
    public static class DetectResult {
        private boolean hasUnarchivedData;
        private int day;
        private int hour;

        public static DetectResult notHasUnarchivedData() {
            DetectResult detectResult = new DetectResult();
            detectResult.setHasUnarchivedData(false);
            return detectResult;
        }

        public static DetectResult build(boolean existsUnarchivedData, LocalDateTime time) {
            DetectResult detectResult = new DetectResult();
            detectResult.setHasUnarchivedData(existsUnarchivedData);
            LocalDateTime timeOfHourStart = ArchiveDateTimeUtil.toHourlyRoundDown(time);
            detectResult.setDay(ArchiveDateTimeUtil.computeDay(timeOfHourStart));
            detectResult.setHour(ArchiveDateTimeUtil.computeHour(timeOfHourStart));
            return detectResult;
        }
    }
}
