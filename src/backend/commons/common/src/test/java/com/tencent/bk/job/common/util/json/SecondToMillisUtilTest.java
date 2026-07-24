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

package com.tencent.bk.job.common.util.json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class SecondToMillisUtilTest {

    @Test
    @DisplayName("null 入参返回 null")
    void toMillis_null() {
        assertThat(SecondToMillisUtil.toMillis(null)).isNull();
    }

    @Test
    @DisplayName("秒级时间戳转换为毫秒级")
    void toMillis_secondTimestamp() {
        assertThat(SecondToMillisUtil.toMillis(1_672_531_200L)).isEqualTo(1_672_531_200_000L);
    }

    @Test
    @DisplayName("毫秒级时间戳原样返回")
    void toMillis_millisTimestamp() {
        assertThat(SecondToMillisUtil.toMillis(1_672_531_200_000L)).isEqualTo(1_672_531_200_000L);
    }

    @Test
    @DisplayName("秒级时间戳上界仍按秒处理")
    void toMillis_maxSecondTimestamp() {
        assertThat(SecondToMillisUtil.toMillis(9_999_999_999L)).isEqualTo(9_999_999_999_000L);
    }

    @Test
    @DisplayName("超过秒级上界的时间戳视为毫秒级")
    void toMillis_aboveMaxSecondTimestamp() {
        assertThat(SecondToMillisUtil.toMillis(10_000_000_000L)).isEqualTo(10_000_000_000L);
    }
}
