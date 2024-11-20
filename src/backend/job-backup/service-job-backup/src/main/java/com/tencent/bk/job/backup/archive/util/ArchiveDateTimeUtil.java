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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class ArchiveDateTimeUtil {
    public static int computeDay(LocalDateTime dateTime) {
        return Integer.parseInt(DateUtils.formatLocalDateTime(dateTime, "yyyyMMdd"));
    }

    public static int computeHour(LocalDateTime dateTime) {
        return dateTime.getHour();
    }

    public static LocalDateTime computeStartOfDayBeforeDays(LocalDateTime now, int beforeDays) {
        // 当前日期的最早时间
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        return startOfDay.minusDays(beforeDays);
    }

    public static LocalDateTime unixTimestampMillToLocalDateTime(long unixTimestampMill) {
        // 创建一个 Instant 对象，表示从 1970-01-01T00:00:00Z 开始的指定秒数
        Instant instant = Instant.ofEpochMilli(unixTimestampMill);
        // 将 Instant 对象转换为 当前时区的 LocalDateTime 对象
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    public static LocalDateTime toHourlyRoundDown(LocalDateTime localDateTime) {
        return localDateTime.withMinute(0).withSecond(0).withNano(0);
    }
}
