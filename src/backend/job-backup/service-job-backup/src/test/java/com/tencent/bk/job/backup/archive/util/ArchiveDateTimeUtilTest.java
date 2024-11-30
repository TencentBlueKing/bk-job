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

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ArchiveDateTimeUtilTest {

    @Test
    void computeDay() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 11, 11, 1, 11, 11);
        assertThat(ArchiveDateTimeUtil.computeDay(dateTime)).isEqualTo(20241111);

        dateTime = LocalDateTime.of(2024, 8, 12, 0, 0, 0);
        assertThat(ArchiveDateTimeUtil.computeDay(dateTime)).isEqualTo(20240812);
    }

    @Test
    void computeHour() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 11, 11, 1, 11, 11);
        assertThat(ArchiveDateTimeUtil.computeHour(dateTime)).isEqualTo(1);

        dateTime = LocalDateTime.of(2024, 8, 12, 0, 0, 0);
        assertThat(ArchiveDateTimeUtil.computeHour(dateTime)).isEqualTo(0);

        dateTime = LocalDateTime.of(2024, 8, 12, 0, 0, 1);
        assertThat(ArchiveDateTimeUtil.computeHour(dateTime)).isEqualTo(0);
    }

    @Test
    void computeStartOfDayBeforeDays() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 11, 11, 1, 11, 11);
        LocalDateTime result = ArchiveDateTimeUtil.computeStartOfDayBeforeDays(dateTime, 1);
        assertThat(result).isEqualTo(LocalDateTime.of(2024, 11, 10, 0, 0, 0));

        result = ArchiveDateTimeUtil.computeStartOfDayBeforeDays(dateTime, 30);
        assertThat(result).isEqualTo(LocalDateTime.of(2024, 10, 12, 0, 0, 0));
    }

    @Test
    void unixTimestampMillToLocalDateTime() {
        LocalDateTime dateTime = ArchiveDateTimeUtil.unixTimestampMillToZoneDateTime(
            1732072271000L, ZoneId.of("GMT+8"));
        assertThat(dateTime).isEqualTo(LocalDateTime.of(2024, 11, 20, 11, 11, 11));
    }

    @Test
    void toHourlyRoundDown() {
        LocalDateTime dateTime =
            ArchiveDateTimeUtil.toHourlyRoundDown(LocalDateTime.of(2024, 11, 20, 11, 1, 1));
        assertThat(dateTime).isEqualTo(LocalDateTime.of(2024, 11, 20, 11, 0, 0));
    }

    @Test
    void toTimestampMillsAtZone() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 11, 11, 1, 11, 11);
        assertThat(ArchiveDateTimeUtil.toTimestampMillsAtZone(dateTime, ZoneId.of("GMT+8")))
            .isEqualTo(1731258671000L);
    }
}
