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


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class DateUtilTest {
    @Test
    public void testDefaultFormatTimestamp() {
        LocalDateTime localDateTime = LocalDateTime.of(2019, 9, 20, 17, 10, 10);
        Timestamp time = Timestamp.valueOf(localDateTime);
        String actualTime = DateUtils.defaultFormatTimestamp(time);
        assertThat(actualTime).isEqualTo("2019-09-20 17:10:10");
    }

    @Test
    public void testFormatTimestamp() {
        //2019-08-20 17:10:10
        LocalDateTime localDateTime = LocalDateTime.of(2019, 9, 20, 17, 10, 10);
        Timestamp time = Timestamp.valueOf(localDateTime);
        String pattern = "yyyyMMddHHmmss";
        String actualTime = DateUtils.formatTimestamp(time, pattern);
        assertThat(actualTime).isEqualTo("20190920171010");
    }

    @Test
    public void testConvertDateTimeStrToTimestamp() {
        LocalDateTime localDateTime = LocalDateTime.of(2019, 9, 20, 17, 10, 10);
        Timestamp expectedTimestamp = Timestamp.valueOf(localDateTime);

        String datetimeStr = "2019-09-20 17:10:10";
        Timestamp actualTimestamp = DateUtils.convertDateTimeStrToTimestamp(datetimeStr, ZoneId.systemDefault());
        assertThat(actualTimestamp).isEqualTo(expectedTimestamp);
    }

    @Test
    public void testCalculateMillsBetweenDateTime() {
        LocalDateTime time1 = LocalDateTime.of(2019, 1, 1, 15, 0, 0, 1000000);
        LocalDateTime time2 = LocalDateTime.of(2019, 1, 1, 15, 0, 0, 9000000);
        long millSeconds = DateUtils.calculateMillsBetweenDateTime(time1, time2);
        assertThat(millSeconds).isEqualTo(8L);

        LocalDateTime time3 = LocalDateTime.of(2019, 1, 1, 15, 0, 0, 1000001);
        LocalDateTime time4 = LocalDateTime.of(2019, 1, 1, 15, 0, 0, 9000002);
        millSeconds = DateUtils.calculateMillsBetweenDateTime(time3, time4);
        assertThat(millSeconds).isEqualTo(8L);

        LocalDateTime time5 = LocalDateTime.of(2019, 1, 1, 15, 0, 0, 1000001);
        LocalDateTime time6 = LocalDateTime.of(2019, 1, 1, 15, 0, 0, 9000000);
        millSeconds = DateUtils.calculateMillsBetweenDateTime(time5, time6);
        assertThat(millSeconds).isEqualTo(7L);
    }

    @Test
    void testConvertFromMillSeconds() {
        long timestamp = 1572766266451L;
        LocalDateTime actual = DateUtils.convertFromMillSeconds(timestamp);
        assertThat(actual.getYear()).isEqualTo(2019);
        assertThat(actual.getMonthValue()).isEqualTo(11);
        assertThat(actual.getDayOfMonth()).isEqualTo(3);
        assertThat(actual.getHour()).isEqualTo(15);
        assertThat(actual.getMinute()).isEqualTo(31);
        assertThat(actual.getSecond()).isEqualTo(6);
        assertThat(actual.getNano()).isEqualTo(451_000_000);

    }

    @Test
    void testConvertFromStringDate() {
        LocalDateTime actual = DateUtils.convertFromStringDate("2020-01-01 11:11:11", "yyyy-MM-dd HH:mm:ss");
        LocalDateTime expected = LocalDateTime.of(2020, 1, 1, 11, 11, 11);
        assertThat(actual).isEqualTo(expected);

        actual = DateUtils.convertFromStringDate("2020-01-01+11:11:11", "yyyy-MM-dd+HH:mm:ss");
        expected = LocalDateTime.of(2020, 1, 1, 11, 11, 11);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("测试格式化UnixTimestamp")
    void testFormatUnixTimestamp() {
        String formatDate1 = DateUtils.formatUnixTimestamp(1581930691L, ChronoUnit.SECONDS, "yyyy-MM-dd HH:mm:ss",
            ZoneId.of("GMT+8"));
        assertThat(formatDate1).isEqualTo("2020-02-17 17:11:31");
        String formatDate2 = DateUtils.formatUnixTimestamp(1581930691000L, ChronoUnit.MILLIS, "yyyy-MM-dd HH:mm:ss",
            ZoneId.of("GMT+8"));
        assertThat(formatDate2).isEqualTo("2020-02-17 17:11:31");
        String formatDate3 = DateUtils.formatUnixTimestamp(1581930691000L, ChronoUnit.MILLIS, "yyyy-MM-dd HH:mm:ss",
            ZoneId.of("UTC"));
        assertThat(formatDate3).isEqualTo("2020-02-17 09:11:31");
    }

    @Test
    @DisplayName("测试传入错误的时间单位，抛出异常")
    void testFormatUnixTimestampWithWrongUnitThenThrowException() {
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(
            () -> DateUtils.formatUnixTimestamp(1581930691L, ChronoUnit.MINUTES,
                "yyyy-MM-dd HH:mm:ss", ZoneId.of("GMT+8")));
    }

    @Test
    @DisplayName("测试从格式化的时间转换为UnixTimestamp")
    void testConvertUnixTimestampFromDateTimeStr() {
        long timestamp1 = DateUtils.convertUnixTimestampFromDateTimeStr("2020-02-17 17:11:31", "yyyy-MM-dd HH:mm:ss"
            , ChronoUnit.SECONDS, ZoneId.of("GMT+8"));
        assertThat(timestamp1).isEqualTo(1581930691L);
        long timestamp2 = DateUtils.convertUnixTimestampFromDateTimeStr("2020-02-17 17:11:31", "yyyy-MM-dd HH:mm:ss"
            , ChronoUnit.MILLIS, ZoneId.of("GMT+8"));
        assertThat(timestamp2).isEqualTo(1581930691000L);
    }

    @Test
    @DisplayName("测试从格式化的时间转换为UnixTimestamp,传入错误的时间单位，抛出异常")
    void testConvertUnixTimestampWithWrongUnitThenThrowException() {
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(
            () -> DateUtils.convertUnixTimestampFromDateTimeStr("2020-02-17 17:11:31",
                "yyyy-MM-dd HH:mm:ss", ChronoUnit.NANOS, ZoneId.of("GMT+8")));
    }

    @Test
    @DisplayName("测试从LocalDateTime转换为UnixTimestamp")
    void testConvertUnixTimestampFromLocalDateTime() {
        LocalDateTime dateTime = LocalDateTime.of(2020, 2, 17, 17, 11, 31);
        long timestamp1 = DateUtils.convertUnixTimestampFromLocalDateTime(dateTime
            , ChronoUnit.SECONDS, ZoneId.of("GMT+8"));
        assertThat(timestamp1).isEqualTo(1581930691L);

    }


}
