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

package com.tencent.bk.job.logsvr.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 日志字段工具类测试用例
 */
public class LogFieldUtilTest {

    @Test
    public void testBuildJobCreateDate() {
        String timeStr = "2024-07-26T00:00:00+00:00";
        LocalDateTime time = LocalDateTime.parse(timeStr, ISO_OFFSET_DATE_TIME);
        long timeMills = time.toInstant(ZoneOffset.UTC).getEpochSecond() * 1000L;
        System.out.println(timeStr + ": " + timeMills);
        assertThat(LogFieldUtil.buildJobCreateDate(timeMills)).isEqualTo("2024_07_26");

        timeStr = "2024-07-26T12:00:00+00:00";
        time = LocalDateTime.parse(timeStr, ISO_OFFSET_DATE_TIME);
        timeMills = time.toInstant(ZoneOffset.UTC).getEpochSecond() * 1000L;
        System.out.println(timeStr + ": " + timeMills);
        assertThat(LogFieldUtil.buildJobCreateDate(timeMills)).isEqualTo("2024_07_26");

        timeStr = "2024-07-26T23:59:59+00:00";
        time = LocalDateTime.parse(timeStr, ISO_OFFSET_DATE_TIME);
        timeMills = time.toInstant(ZoneOffset.UTC).getEpochSecond() * 1000L;
        System.out.println(timeStr + ": " + timeMills);
        assertThat(LogFieldUtil.buildJobCreateDate(timeMills)).isEqualTo("2024_07_26");
    }
}
