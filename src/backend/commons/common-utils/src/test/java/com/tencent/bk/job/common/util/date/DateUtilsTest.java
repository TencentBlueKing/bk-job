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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
