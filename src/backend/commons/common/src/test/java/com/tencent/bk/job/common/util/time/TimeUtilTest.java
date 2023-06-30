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

package com.tencent.bk.job.common.util.time;


import com.tencent.bk.job.common.util.TimeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TimeUtilTest {

    @Test
    @DisplayName("测试解析 DateTimeFormatter.ISO_DATE_TIME 格式的时间")
    void testParseIsoZonedTimeToMillis() {
        assertThat(TimeUtil.parseIsoZonedTimeToMillis(null)).isNull();
        assertThat(TimeUtil.parseIsoZonedTimeToMillis("")).isNull();
        assertThat(TimeUtil.parseIsoZonedTimeToMillis("2023-01-01T00:00:00.000AA")).isNull();
        assertThat(TimeUtil.parseIsoZonedTimeToMillis("2023-01-01T00:00:00.000Z")).isEqualTo(1672531200000L);
        assertThat(TimeUtil.parseIsoZonedTimeToMillis("2023-01-01T00:00:00.000+08:00")).isEqualTo(1672502400000L);
        assertThat(TimeUtil.parseIsoZonedTimeToMillis("2023-01-01T00:00:00.000-08:00")).isEqualTo(1672560000000L);
        assertThat(TimeUtil.parseIsoZonedTimeToMillis("2023-05-08T19:37:08.191+08:00")).isEqualTo(1683545828191L);
    }

}
