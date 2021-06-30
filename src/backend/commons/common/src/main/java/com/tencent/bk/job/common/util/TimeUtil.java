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

package com.tencent.bk.job.common.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

/**
 * @Description
 * @Date 2020/3/6
 * @Version 1.0
 */
public class TimeUtil {

    public static final String DEFAULT_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 从今天00:00:00开始到现在一共过了多少秒
     *
     * @return
     */
    public static int getCurrentSeconds() {
        Calendar calendar = Calendar.getInstance();
        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);
        int currentSeconds = hours * 60 * 60 + minutes * 60 + seconds;
        return currentSeconds;
    }

    public static LocalDateTime long2LocalDateTime(long timeMills) {
        Instant instant = Instant.ofEpochMilli(timeMills);
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    public static Long localDateTime2Long(LocalDateTime time) {
        return time.toInstant(ZoneOffset.of("+8")).toEpochMilli();
    }

    public static String getTodayStartTimeStr() {
        return getTodayStartTimeStr(DEFAULT_TIME_FORMAT);
    }

    public static String getTodayStartTimeStr(String format) {
        LocalDateTime todayStartTime = getTodayStartTime();
        return todayStartTime.format(DateTimeFormatter.ofPattern(format));
    }

    /**
     * 获取相对于今天的其他日期开始时间
     *
     * @param relativeDays 相对于今天的天数，1为明天，-1为昨天
     * @param format       日期格式
     * @return
     */
    public static String getRelativeDayStartTimeStr(int relativeDays, String format) {
        LocalDateTime todayStartTime = getTodayStartTime();
        LocalDateTime relativeDayStartTime = todayStartTime.plusDays(relativeDays);
        return relativeDayStartTime.format(DateTimeFormatter.ofPattern(format));
    }

    /**
     * 针对yyyy-MM-dd格式的日期字符串，获取当天开始时间
     *
     * @param dateStr
     * @return
     */
    public static LocalDateTime getDayStartTime(String dateStr) {
        String[] arr = dateStr.split("-");
        Integer year = Integer.parseInt(arr[0]);
        Integer month = Integer.parseInt(arr[1]);
        Integer day = Integer.parseInt(arr[2]);
        return LocalDateTime.of(year, month, day, 0, 0, 0);
    }

    public static LocalDateTime getDayStartTime(LocalDateTime dateTime) {
        return LocalDateTime.of(dateTime.getYear(), dateTime.getMonth(), dateTime.getDayOfMonth(), 0, 0, 0);
    }

    public static LocalDateTime getTodayStartTime() {
        String nowTimeStr = getCurrentTimeStr("yyyy-MM-dd");
        return getDayStartTime(nowTimeStr);
    }

    public static String getCurrentTimeStr() {
        return getCurrentTimeStr(DEFAULT_TIME_FORMAT);
    }

    public static String getCurrentTimeStr(String format) {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(format));
    }

    public static String getTimeStr(LocalDateTime dateTime, String format) {
        return dateTime.format(DateTimeFormatter.ofPattern(format));
    }

    private static String getTimeDescription(LocalDateTime dateTime) {
        int hour = dateTime.getHour();
        if (hour >= 6 && hour <= 8) {
            return "清晨";
        } else if (hour >= 9 && hour <= 11) {
            return "上午";
        } else if (hour >= 12 && hour <= 13) {
            return "午间";
        } else if (hour >= 14 && hour <= 18) {
            return "下午";
        } else if (hour >= 19 && hour <= 23) {
            return "晚上";
        } else {
            return "凌晨";
        }
    }

    public static String getCurrentTimeStrWithDescription(String format) {
        LocalDateTime dateTime = LocalDateTime.now();
        String description = getTimeDescription(dateTime);
        return description + dateTime.format(DateTimeFormatter.ofPattern(format));
    }

    public static String formatTime(long timeMills) {
        return formatTime(timeMills, DEFAULT_TIME_FORMAT);
    }

    public static String formatTime(long timeMills, String format) {
        Date date = new Date(timeMills);
        DateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(date);
    }
}
