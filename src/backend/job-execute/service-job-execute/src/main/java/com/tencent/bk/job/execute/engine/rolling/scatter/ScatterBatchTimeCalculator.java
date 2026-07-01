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

import java.util.concurrent.ThreadLocalRandom;

/**
 * 并行错峰模式批次下发时间计算工具。
 * <p>
 * 调度公式（以设计文档为准）：
 * <ul>
 *     <li>批 1 偏移 = 0；</li>
 *     <li>批 k(k≥2) 偏移 = fixed*(k-1) + random(randomMin, randomMax)，随机每批独立采样。</li>
 * </ul>
 */
public class ScatterBatchTimeCalculator {

    private ScatterBatchTimeCalculator() {
    }

    /**
     * 计算某批次相对步骤开始时刻的下发偏移（毫秒）。
     *
     * @param batch          批次(从1开始)
     * @param fixedMs        批次间固定线性步长
     * @param randomMinMs    批次间随机延迟下限
     * @param randomMaxMs    批次间随机延迟上限
     * @return 偏移毫秒数，批1恒为0
     */
    public static long computeOffsetMillis(int batch, long fixedMs, long randomMinMs, long randomMaxMs) {
        if (batch <= 1) {
            return 0L;
        }
        long fixed = Math.max(0L, fixedMs);
        long min = Math.max(0L, randomMinMs);
        long max = Math.max(min, randomMaxMs);
        long randomPart = min == max ? min : ThreadLocalRandom.current().nextLong(min, max + 1);
        return fixed * (batch - 1) + randomPart;
    }

    /**
     * 计算某批次的绝对下发时刻。
     *
     * @param baseTimeMillis 步骤开始基准时刻(epoch millis)
     * @param batch          批次(从1开始)
     * @param fixedMs        批次间固定线性步长
     * @param randomMinMs    批次间随机延迟下限
     * @param randomMaxMs    批次间随机延迟上限
     * @return 绝对下发时刻(epoch millis)
     */
    public static long computeDispatchTime(long baseTimeMillis,
                                           int batch,
                                           long fixedMs,
                                           long randomMinMs,
                                           long randomMaxMs) {
        return baseTimeMillis + computeOffsetMillis(batch, fixedMs, randomMinMs, randomMaxMs);
    }
}
