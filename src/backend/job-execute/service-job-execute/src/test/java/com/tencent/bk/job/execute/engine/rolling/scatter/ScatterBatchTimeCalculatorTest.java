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

@DisplayName("并行错峰批次时间计算(累积式)")
class ScatterBatchTimeCalculatorTest {

    @Test
    @DisplayName("min与max相等时，相邻批间隔完全确定：min")
    void deterministicIntervalWhenRangeEqual() {
        // min==max==1200 → 间隔恒为 1200
        assertThat(ScatterBatchTimeCalculator.computeBatchInterval(1200, 1200)).isEqualTo(1200);
    }

    @Test
    @DisplayName("有区间时，相邻批间隔落在[min, max]")
    void intervalWithinRange() {
        for (int i = 0; i < 100; i++) {
            long interval = ScatterBatchTimeCalculator.computeBatchInterval(1100, 1500);
            assertThat(interval).isBetween(1100L, 1500L);
        }
    }

    @Test
    @DisplayName("min=0 时退化为[0, max]随机抖动")
    void intervalWithZeroMin() {
        for (int i = 0; i < 100; i++) {
            long interval = ScatterBatchTimeCalculator.computeBatchInterval(0, 500);
            assertThat(interval).isBetween(0L, 500L);
        }
    }

    @Test
    @DisplayName("非法参数(负值)按0处理，不抛异常")
    void negativeParamsTreatedAsZero() {
        assertThat(ScatterBatchTimeCalculator.computeBatchInterval(-1, -1)).isZero();
    }

    @Test
    @DisplayName("累积式：下一批下发时刻 = 上一批 + random(确定)")
    void computeNextDispatchTimeDeterministic() {
        long base = 1700000000000L;
        long batch2 = ScatterBatchTimeCalculator.computeNextDispatchTime(base, 1200, 1200);
        long batch3 = ScatterBatchTimeCalculator.computeNextDispatchTime(batch2, 1200, 1200);
        long batch4 = ScatterBatchTimeCalculator.computeNextDispatchTime(batch3, 1200, 1200);
        // 批1=base(偏移0)；累积间隔恒为1200
        assertThat(batch2).isEqualTo(base + 1200);
        assertThat(batch3).isEqualTo(base + 2400);
        assertThat(batch4).isEqualTo(base + 3600);
    }

    @Test
    @DisplayName("累积式：相邻批间隔恒≥min，始终错开")
    void adjacentBatchesAlwaysStaggered() {
        // 区间[500,2500]，累积式保证相邻批间隔≥500，始终错开
        long base = 1700000000000L;
        long previous = base;
        for (int batch = 2; batch <= 20; batch++) {
            long dispatchTime = ScatterBatchTimeCalculator.computeNextDispatchTime(previous, 500, 2500);
            long interval = dispatchTime - previous;
            assertThat(interval).isBetween(500L, 2500L);
            previous = dispatchTime;
        }
    }

    @Test
    @DisplayName("computeDispatchTimes：totalBatch=1 仅批1=baseTime")
    void computeDispatchTimesSingleBatch() {
        long base = 1700000000000L;
        long[] times = ScatterBatchTimeCalculator.computeDispatchTimes(base, 1, 1200, 1800);
        assertThat(times).hasSize(1);
        assertThat(times[0]).isEqualTo(base);
    }

    @Test
    @DisplayName("computeDispatchTimes：totalBatch=2 批1=baseTime，批2=base+间隔(确定)")
    void computeDispatchTimesTwoBatches() {
        long base = 1700000000000L;
        long[] times = ScatterBatchTimeCalculator.computeDispatchTimes(base, 2, 1200, 1200);
        assertThat(times).hasSize(2);
        assertThat(times[0]).isEqualTo(base);
        assertThat(times[1]).isEqualTo(base + 1200);
    }

    @Test
    @DisplayName("computeDispatchTimes：totalBatch=N 与逐批computeNextDispatchTime等价(确定间隔)")
    void computeDispatchTimesEquivalentToStepwiseDeterministic() {
        long base = 1700000000000L;
        long[] times = ScatterBatchTimeCalculator.computeDispatchTimes(base, 5, 1200, 1200);
        assertThat(times).containsExactly(base, base + 1200, base + 2400, base + 3600, base + 4800);
    }

    @Test
    @DisplayName("computeDispatchTimes：批1恒为baseTime，其余严格递增且间隔∈[min,max]")
    void computeDispatchTimesMonotonicWithinBounds() {
        long base = 1700000000000L;
        int totalBatch = 30;
        long[] times = ScatterBatchTimeCalculator.computeDispatchTimes(base, totalBatch, 600, 1400);
        assertThat(times).hasSize(totalBatch);
        assertThat(times[0]).isEqualTo(base);
        for (int i = 1; i < totalBatch; i++) {
            long interval = times[i] - times[i - 1];
            assertThat(interval).isBetween(600L, 1400L);
        }
    }

    @Test
    @DisplayName("computeDispatchTimes：totalBatch<=0 返回空数组，不抛异常")
    void computeDispatchTimesNonPositive() {
        assertThat(ScatterBatchTimeCalculator.computeDispatchTimes(1L, 0, 1000, 1000)).isEmpty();
        assertThat(ScatterBatchTimeCalculator.computeDispatchTimes(1L, -3, 1000, 1000)).isEmpty();
    }
}
