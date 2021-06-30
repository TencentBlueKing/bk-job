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

package com.tencent.bk.job.common.util.date;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

/**
 * 时间处理
 */
@Slf4j
public class DateUtils {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER_WITH_ZONE =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX");

    /**
     * 格式化Timestamp，yyyy-MM-dd HH:mm:ss
     *
     * @param timestamp
     * @return
     */
    public static String defaultFormatTimestamp(Timestamp timestamp) {
        return formatTimestamp(timestamp, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 根据给定的pattern格式化Timestamp
     *
     * @param timestamp
     * @param pattern
     * @return
     */
    public static String formatTimestamp(Timestamp timestamp, String pattern) {
        Instant now = Instant.ofEpochMilli(timestamp.getTime());
        LocalDateTime dateTime = LocalDateTime.ofInstant(now, ZoneId.systemDefault());
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 根据给定的pattern格式化Timestamp
     *
     * @param unixTimestamp UNIX 时间戳
     * @param unit          时间单位，支持ChronoUnit.SECONDS,ChronoUnit.MILLILS
     * @param pattern       格式化样式
     * @param zone          时区,如果不传默认使用系统时区
     * @return
     */
    public static String formatUnixTimestamp(Long unixTimestamp, ChronoUnit unit, String pattern, ZoneId zone) {
        return parseUnixTimestamp(unixTimestamp, unit, zone).format(DateTimeFormatter.ofPattern(pattern));
    }

    public static String formatUnixTimestamp(Long unixTimestamp, ChronoUnit unit) {
        return parseUnixTimestamp(unixTimestamp, unit, null).format(DATE_FORMATTER);
    }

    public static String formatUnixTimestampWithZone(Long unixTimestamp, ChronoUnit unit) {
        return parseUnixTimestamp(unixTimestamp, unit, null).format(DATE_FORMATTER_WITH_ZONE);
    }

    private static ZonedDateTime parseUnixTimestamp(Long unixTimestamp, ChronoUnit unit, ZoneId zone) {
        if (unit != null && (unit != ChronoUnit.SECONDS && unit != ChronoUnit.MILLIS)) {
            throw new UnsupportedOperationException("Unsupported conversion with unit:" + unit.name());
        }
        Instant instant;
        unit = (unit == null ? ChronoUnit.SECONDS : unit);
        if (unit == ChronoUnit.MILLIS) {
            instant = Instant.ofEpochMilli(unixTimestamp);
        } else {
            instant = Instant.ofEpochSecond(unixTimestamp);
        }
        ZonedDateTime dateTime;
        if (zone != null) {
            dateTime = ZonedDateTime.ofInstant(instant, zone);
        } else {
            dateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        }
        return dateTime;
    }

    /**
     * @param datetime      时间
     * @param pattern       时间格式
     * @param convertToUnit 转换之后的单位，支持ChronoUnit.SECONDS和ChronoUnit.MILLIS。如果不传入，默认使用ChronoUnit.SECONDS
     * @param zone          时区；如果未传入，默认使用系统时区
     * @return UnixTimestamp
     */
    public static long convertUnixTimestampFromDateTimeStr(String datetime, String pattern, ChronoUnit convertToUnit,
                                                           ZoneId zone) {
        LocalDateTime localDateTime = LocalDateTime.parse(datetime, DateTimeFormatter.ofPattern(pattern));
        return convertUnixTimestampFromLocalDateTime(localDateTime, convertToUnit, zone);
    }

    /**
     * @param datetime      时间
     * @param convertToUnit 转换之后的单位，支持ChronoUnit.SECONDS和ChronoUnit.MILLIS。如果不传入，默认使用ChronoUnit.SECONDS
     * @param zone          时区；如果未传入，默认使用系统时区
     * @return
     */
    public static long convertUnixTimestampFromLocalDateTime(LocalDateTime datetime, ChronoUnit convertToUnit,
                                                             ZoneId zone) {
        if (convertToUnit != null && (convertToUnit != ChronoUnit.SECONDS && convertToUnit != ChronoUnit.MILLIS)) {
            throw new UnsupportedOperationException("Unsupported conversion with unit:" + convertToUnit.name());
        }
        Instant now = Instant.now();
        ZoneId timeZone = zone == null ? ZoneId.systemDefault() : zone;
        ZoneOffset currentOffsetForMyZone = timeZone.getRules().getOffset(now);


        long unixTimestamp;
        if (convertToUnit != null) {
            if (convertToUnit == ChronoUnit.MILLIS) {
                unixTimestamp = datetime.toInstant(currentOffsetForMyZone).toEpochMilli();
            } else {
                unixTimestamp = datetime.toInstant(currentOffsetForMyZone).toEpochMilli() / 1000;
            }
        } else {
            unixTimestamp = datetime.toInstant(currentOffsetForMyZone).toEpochMilli() / 1000;
        }
        return unixTimestamp;
    }

    /**
     * 把日期字符(yyyy-MM-dd HH:mm:ss)转换成Timestamp
     *
     * @param datetime
     * @return
     */
    public static Timestamp convertDateTimeStrToTimestamp(String datetime, ZoneId currentZone) {
        Instant now = Instant.now();
        ZoneOffset currentOffsetForMyZone = currentZone.getRules().getOffset(now);

        LocalDateTime localDateTime = LocalDateTime.parse(datetime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return new Timestamp(localDateTime.toInstant(currentOffsetForMyZone).toEpochMilli());
    }

    /**
     * 把LocalDateTime转换成Timestamp
     *
     * @param localDateTime datetime
     * @param currentZone   当前时区
     * @return
     */
    public static Timestamp convertLocalDateTimeToTimestamp(LocalDateTime localDateTime, ZoneId currentZone) {
        if (localDateTime == null) {
            return null;
        }
        ZoneId finalZone = currentZone;
        if (currentZone == null) {
            finalZone = ZoneId.systemDefault();
        }
        Instant now = Instant.now();
        assert finalZone != null;
        ZoneOffset currentOffsetForMyZone = finalZone.getRules().getOffset(now);
        return new Timestamp(localDateTime.toInstant(currentOffsetForMyZone).toEpochMilli());
    }

    public static String defaultLocalDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static String formatLocalDateTime(LocalDateTime localDateTime, String pattern) {
        return localDateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    public static long calculateMillsBetweenDateTime(LocalDateTime from, LocalDateTime to) {
        Duration duration = Duration.between(from, to);
        return duration.toMillis();
    }

    public static LocalDateTime convertFromMillSeconds(long millSeconds) {
        return LocalDateTime.ofEpochSecond(millSeconds / 1000, (int) (millSeconds % 1000) * 1_000_000,
            ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
    }

    public static LocalDateTime convertFromStringDate(String dateTime, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return LocalDateTime.parse(dateTime, formatter);
    }

    public static Timestamp to(LocalDateTime u) {
        return u == null ? null : Timestamp.valueOf(u);
    }

    public static long currentTimeSeconds() {
        return System.currentTimeMillis() / 1000L;
    }

    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 把UnixTimestamp转换成java.sql.Timestamp
     *
     * @param unixTimestamp UnixTimestamp,毫秒
     * @param unit          unixTimestamp的单位，支持ChronoUnit.SECONDS和ChronoUnit.MILLIS。如果不传入，默认使用ChronoUnit.SECONDS
     * @return
     */
    public static Timestamp convertUnixTimestampToSqlTimestamp(Long unixTimestamp, ChronoUnit unit) {
        if (unixTimestamp == null) {
            return null;
        }
        if (unit != null && (unit != ChronoUnit.SECONDS && unit != ChronoUnit.MILLIS)) {
            throw new UnsupportedOperationException("Unsupported conversion with unit:" + unit.name());
        }
        long unixTimestampMillis = 0L;
        if (unit != null) {
            if (unit == ChronoUnit.MILLIS) {
                unixTimestampMillis = unixTimestamp;
            } else {
                unixTimestampMillis = 1000 * unixTimestamp;
            }
        } else {
            unixTimestampMillis = 1000 * unixTimestamp;
        }

        return new Timestamp(unixTimestampMillis);
    }

    public static String getDateStrFromUnixTimeMills(long unixTimeMills) {
        return getDateStrFromUnixTimeMills(unixTimeMills, "yyyy-MM-dd");
    }

    public static String getDateStrFromUnixTimeMills(long unixTimeMills, String pattern) {
        Date date = new Date(unixTimeMills);
        SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getDateInstance();
        sdf.applyPattern(pattern);
        return sdf.format(date);
    }

    public static String getCurrentDateStr() {
        return getCurrentDateStr("yyyy-MM-dd");
    }

    public static String getCurrentDateStr(String pattern) {
        SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getDateInstance();
        sdf.applyPattern(pattern);
        return sdf.format(new Date());
    }

    public static String getNextDateStr(String dateStr) {
        return getPreviousDateStr(dateStr, "yyyy-MM-dd", -1);
    }

    public static String getLastDateStr(String dateStr) {
        return getPreviousDateStr(dateStr, "yyyy-MM-dd", 1);
    }

    public static String getPreviousDateStr(String dateStr, int previousDays) {
        return getPreviousDateStr(dateStr, "yyyy-MM-dd", previousDays);
    }

    public static Date getPreviousDate(Date date, int previousDays) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_YEAR, -previousDays);
        return calendar.getTime();
    }

    public static String getPreviousDateStr(String dateStr, String pattern, int previousDays) {
        SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getDateInstance();
        sdf.applyPattern(pattern);
        try {
            Date date = sdf.parse(dateStr);
            Date previousDay = getPreviousDate(date, previousDays);
            return sdf.format(previousDay);
        } catch (ParseException e) {
            log.error("Fail to calc date:{} - {}days, pattern={}", dateStr, previousDays, pattern, e);
            return null;
        }
    }

    public static Long calcDaysBetween(String startDateStr, String endDateStr) {
        String pattern = "yyyy-MM-dd";
        return calcDaysBetween(startDateStr, endDateStr, pattern);
    }

    public static Long calcDaysBetween(String startDateStr, String endDateStr, String pattern) {
        if (StringUtils.isBlank(startDateStr)) {
            startDateStr = getCurrentDateStr(pattern);
            log.warn("startDateStr is blank, use currentDateStr {} as startDateStr", startDateStr);
        }
        if (StringUtils.isBlank(endDateStr)) {
            endDateStr = getCurrentDateStr(pattern);
            log.warn("endDateStr is blank, use currentDateStr {} as endDateStr", endDateStr);
        }
        SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getDateInstance();
        sdf.applyPattern(pattern);
        try {
            Date startDate = sdf.parse(startDateStr);
            Date endDate = sdf.parse(endDateStr);
            return (endDate.getTime() - startDate.getTime()) / (24 * 60 * 60 * 1000);
        } catch (ParseException e) {
            log.error("Fail to calcDaysBetween:{},{},pattern={}", startDateStr, endDateStr, pattern, e);
            return null;
        }
    }
}
