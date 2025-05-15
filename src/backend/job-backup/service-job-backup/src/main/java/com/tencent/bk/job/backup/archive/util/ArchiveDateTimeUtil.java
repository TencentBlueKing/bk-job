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

package com.tencent.bk.job.backup.archive.util;

import com.tencent.bk.job.common.util.date.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 归档日期计算工具类
 */
@Slf4j
public class ArchiveDateTimeUtil {
    /**
     * 计算天（日期），返回 yyyyMMdd 格式的数字
     *
     * @param dateTime 日期
     * @return 天（日期）
     */
    public static int computeDay(LocalDateTime dateTime) {
        return Integer.parseInt(DateUtils.formatLocalDateTime(dateTime, "yyyyMMdd"));
    }

    /**
     * 计算小时
     *
     * @param dateTime 日期
     * @return 小时
     */
    public static int computeHour(LocalDateTime dateTime) {
        return dateTime.getHour();
    }

    public static LocalDateTime computeStartOfDayBeforeDays(LocalDateTime now, int beforeDays) {
        // 当前日期的最早时间
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        return startOfDay.minusDays(beforeDays);
    }

    public static LocalDateTime unixTimestampMillToZoneDateTime(long unixTimestampMill, ZoneId zoneId) {
        // 创建一个 Instant 对象，表示从 1970-01-01T00:00:00Z 开始的指定毫秒数
        Instant instant = Instant.ofEpochMilli(unixTimestampMill);
        // 将 Instant 对象转换为 对应时区的 LocalDateTime 对象
        return LocalDateTime.ofInstant(instant, zoneId);
    }

    public static LocalDateTime toHourlyRoundDown(LocalDateTime localDateTime) {
        return localDateTime.withMinute(0).withSecond(0).withNano(0);
    }

    public static long toTimestampMillsAtZone(LocalDateTime localDateTime, ZoneId zoneId) {
        OffsetDateTime offsetDateTime =
            localDateTime.atOffset(zoneId.getRules().getOffset(localDateTime));
        return 1000 * offsetDateTime.toEpochSecond();
    }

    /**
     * 根据配置的时区计算所依据时区
     *
     * @param timeZone 时区
     * @return 时区
     */
    public static ZoneId getArchiveBasedTimeZone(String timeZone) throws DateTimeException {
        ZoneId zoneId;
        if (StringUtils.isBlank(timeZone)) {
            zoneId = ZoneId.systemDefault();
            log.info("Use system zone as archive base time zone, zoneId: {}", zoneId);
            return zoneId;
        }
        zoneId = ZoneId.of(timeZone);
        log.info("Use configured zone as archive base time zone, zoneId: {}", zoneId);
        return zoneId;
    }

    public static LocalDateTime computeArchiveEndTime(int archiveDays, ZoneId zoneId) {
        log.info("Compute archive task generate end time before {} days", archiveDays);
        LocalDateTime now = LocalDateTime.now(zoneId);
        return ArchiveDateTimeUtil.computeStartOfDayBeforeDays(now, archiveDays);
    }
}
