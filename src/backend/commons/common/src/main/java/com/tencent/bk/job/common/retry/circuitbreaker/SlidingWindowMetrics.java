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

package com.tencent.bk.job.common.retry.circuitbreaker;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 滑动窗口指标
 */
@Data
@AllArgsConstructor
public class SlidingWindowMetrics {
    /**
     * 总调用次数
     */
    private int totalCalls;

    /**
     * 成功调用次数
     */
    private int successCalls;

    /**
     * 失败调用次数
     */
    private int failureCalls;

    /**
     * 慢调用次数
     */
    private int slowCalls;

    /**
     * 获取失败率（百分比）
     *
     * @return 失败率（0-100）
     */
    public float getFailureRate() {
        if (totalCalls == 0) {
            return 0.0f;
        }
        return (float) failureCalls / totalCalls * 100;
    }

    /**
     * 获取慢调用率（百分比）
     *
     * @return 慢调用率（0-100）
     */
    public float getSlowCallRate() {
        if (totalCalls == 0) {
            return 0.0f;
        }
        return (float) slowCalls / totalCalls * 100;
    }
}
