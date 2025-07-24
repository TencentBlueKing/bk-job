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

package com.tencent.bk.job.common.util.date;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DateUtilsTest {

    @Test
    public void testGetPreviousDate() {
        assertThat(DateUtils.getPreviousDateStr("2020-12-20", "yyyy-MM-dd", 1)).isEqualTo("2020-12-19");
        assertThat(DateUtils.getPreviousDateStr("2020-12-20", "yyyy-MM-dd", 7)).isEqualTo("2020-12-13");
        assertThat(DateUtils.getPreviousDateStr("2020-12-20", "yyyy-MM-dd", 21)).isEqualTo("2020-11-29");
        assertThat(DateUtils.calcDaysBetween("2020-12-20", "2020-12-21")).isEqualTo(1);
        assertThat(DateUtils.calcDaysBetween("2020-12-20", "2021-12-20")).isEqualTo(365);
        assertThat(DateUtils.calcDaysBetween("2019-12-20", "2020-12-20")).isEqualTo(366);
        assertThat(DateUtils.calcDaysBetween("2020-12-31", "2021-1-1")).isEqualTo(1);
        assertThat(DateUtils.calcDaysBetween("2021-2-28", "2021-3-1")).isEqualTo(1);
        assertThat(DateUtils.calcDaysBetween("2020-2-28", "2020-3-1")).isEqualTo(2);
    }

    @Test
    void getUTCDayEndTimestamp() {
        ZonedDateTime dateTime = LocalDateTime.of(2023, 7, 14, 15, 14, 23, 12312)
            .atZone(ZoneId.of("UTC"));
        long endTime = DateUtils.getUTCDayEndTimestamp(dateTime.toLocalDate());
        // 2023-07-15 00:00:00 UTC
        assertThat(endTime).isEqualTo(1689379200000L);


        ZonedDateTime dateTime2 = LocalDateTime.of(2023, 7, 14, 7, 15, 22, 123)
            .atZone(ZoneId.of("UTC"));
        long endTime2 = DateUtils.getUTCDayEndTimestamp(dateTime2.toLocalDate());
        // 2023-07-15 00:00:00 UTC
        assertThat(endTime2).isEqualTo(1689379200000L);

        ZonedDateTime dateTime3 = LocalDateTime.of(2023, 7, 13, 0, 0, 0, 0)
            .atZone(ZoneId.of("UTC"));
        long endTime3 = DateUtils.getUTCDayEndTimestamp(dateTime3.toLocalDate());
        // 2023-07-14 00:00:00 UTC
        assertThat(endTime3).isEqualTo(1689292800000L);

        ZonedDateTime dateTime4 = LocalDateTime.of(2023, 7, 13, 23, 59, 59, 99999999)
            .atZone(ZoneId.of("UTC"));
        long endTime4 = DateUtils.getUTCDayEndTimestamp(dateTime4.toLocalDate());
        // 2023-07-14 00:00:00 UTC
        assertThat(endTime4).isEqualTo(1689292800000L);
    }

    @Test
    void convertFromStringDateByPatterns() {
        String[] patterns = new String[]{
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss.SS",
            "yyyy-MM-dd'T'HH:mm:ss.S",
            "yyyy-MM-dd'T'HH:mm:ss.",
            "yyyy-MM-dd'T'HH:mm:ss"
        };
        LocalDateTime localDateTime = DateUtils.convertFromStringDateByPatterns(
            "2022-04-21T10:55:54.655", patterns
        );
        assertThat(localDateTime).isNotNull();
        localDateTime = DateUtils.convertFromStringDateByPatterns(
            "2022-04-21T10:55:54.65", patterns
        );
        assertThat(localDateTime).isNotNull();
        localDateTime = DateUtils.convertFromStringDateByPatterns(
            "2022-04-21T10:55:54.6", patterns
        );
        assertThat(localDateTime).isNotNull();
        localDateTime = DateUtils.convertFromStringDateByPatterns(
            "2022-04-21T10:55:54.", patterns
        );
        assertThat(localDateTime).isNotNull();
        localDateTime = DateUtils.convertFromStringDateByPatterns(
            "2022-04-21T10:55:54", patterns
        );
        assertThat(localDateTime).isNotNull();
        assertThatThrownBy(() -> DateUtils.convertFromStringDateByPatterns(
            "2022-04-21T10:55:54.65"
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DateUtils.convertFromStringDateByPatterns(
            "2022-04-21 10:55:54.65"
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
