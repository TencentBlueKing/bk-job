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
 * 调度公式（累积式，以设计文档为准）：
 * <ul>
 *     <li>批 1 下发时刻 = 步骤开始基准时刻（偏移 0，立即下发）；</li>
 *     <li>批 k(k≥2) 下发时刻 = 批(k-1) 下发时刻 + fixed + random(randomMin, randomMax)，随机每批独立采样。</li>
 * </ul>
 * 因此相邻批次间隔恒 ≥ {@code fixed + randomMin}，保证始终错开（不会因随机区间宽度 ≥ fixed 而近乎同时启动）。
 */
public class ScatterBatchTimeCalculator {

    private ScatterBatchTimeCalculator() {
    }

    /**
     * 计算相邻两批之间的错峰间隔（毫秒）= fixed + random(randomMin, randomMax)，随机独立采样一次。
     *
     * @param fixedMs        批次间固定间隔
     * @param randomMinMs    批次间随机延迟下限
     * @param randomMaxMs    批次间随机延迟上限
     * @return 相邻两批间隔毫秒数，落在 [fixed+min, fixed+max]
     */
    public static long computeBatchInterval(long fixedMs, long randomMinMs, long randomMaxMs) {
        long fixed = Math.max(0L, fixedMs);
        long min = Math.max(0L, randomMinMs);
        long max = Math.max(min, randomMaxMs);
        long randomPart = min == max ? min : ThreadLocalRandom.current().nextLong(min, max + 1);
        return fixed + randomPart;
    }

    /**
     * 累积式计算下一批的绝对下发时刻：上一批下发时刻 + fixed + random(randomMin, randomMax)。
     * <p>
     * 调用方需按批次顺序依次调用，并以上一次返回值作为下一次的 {@code previousDispatchTime}，
     * 从而保证 dispatch_time 登记与调度入队使用同一累积序列。
     *
     * @param previousDispatchTime 上一批的绝对下发时刻(epoch millis)
     * @param fixedMs              批次间固定间隔
     * @param randomMinMs          批次间随机延迟下限
     * @param randomMaxMs          批次间随机延迟上限
     * @return 下一批的绝对下发时刻(epoch millis)
     */
    public static long computeNextDispatchTime(long previousDispatchTime,
                                               long fixedMs,
                                               long randomMinMs,
                                               long randomMaxMs) {
        return previousDispatchTime + computeBatchInterval(fixedMs, randomMinMs, randomMaxMs);
    }
}
