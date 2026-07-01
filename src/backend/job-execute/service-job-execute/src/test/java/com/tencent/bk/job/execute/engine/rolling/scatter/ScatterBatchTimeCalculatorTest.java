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

package com.tencent.bk.job.execute.engine.rolling.scatter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("并行错峰批次时间计算")
class ScatterBatchTimeCalculatorTest {

    @Test
    @DisplayName("批1偏移恒为0")
    void firstBatchOffsetIsZero() {
        assertThat(ScatterBatchTimeCalculator.computeOffsetMillis(1, 1000, 100, 500)).isZero();
    }

    @Test
    @DisplayName("random上下限相等时，偏移完全确定：fixed*(k-1)+random")
    void deterministicWhenRandomRangeEqual() {
        // fixed=1000, random=200(min==max)
        assertThat(ScatterBatchTimeCalculator.computeOffsetMillis(2, 1000, 200, 200)).isEqualTo(1200);
        assertThat(ScatterBatchTimeCalculator.computeOffsetMillis(3, 1000, 200, 200)).isEqualTo(2200);
        assertThat(ScatterBatchTimeCalculator.computeOffsetMillis(5, 1000, 200, 200)).isEqualTo(4200);
    }

    @Test
    @DisplayName("random有区间时，偏移落在[fixed*(k-1)+min, fixed*(k-1)+max]")
    void offsetWithinRandomRange() {
        for (int i = 0; i < 100; i++) {
            long offset = ScatterBatchTimeCalculator.computeOffsetMillis(4, 1000, 100, 500);
            assertThat(offset).isBetween(3000L + 100L, 3000L + 500L);
        }
    }

    @Test
    @DisplayName("绝对下发时刻 = base + offset")
    void computeDispatchTime() {
        long base = 1700000000000L;
        long dispatchTime = ScatterBatchTimeCalculator.computeDispatchTime(base, 3, 1000, 200, 200);
        assertThat(dispatchTime).isEqualTo(base + 2200);
    }

    @Test
    @DisplayName("非法参数(负值)按0处理，不抛异常")
    void negativeParamsTreatedAsZero() {
        assertThat(ScatterBatchTimeCalculator.computeOffsetMillis(2, -1, -1, -1)).isZero();
    }
}
