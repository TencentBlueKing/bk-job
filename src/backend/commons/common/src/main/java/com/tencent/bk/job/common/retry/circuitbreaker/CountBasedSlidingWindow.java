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

import lombok.Getter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * 基于计数的滑动窗口实现（使用环形缓冲区）
 * 线程安全性：此类是线程安全的，所有公共方法都可以在多线程环境下并发调用。
 * 内部使用 synchronized 保证数据一致性。
 */
public class CountBasedSlidingWindow implements SlidingWindow {
    /**
     * 窗口大小
     */
    private final int windowSize;

    /**
     * 环形缓冲区（存储调用结果）
     */
    private final AtomicReferenceArray<CallResult> ringBuffer;

    /**
     * 当前写入位置
     */
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    /**
     * 总调用次数
     */
    private final AtomicInteger totalCalls = new AtomicInteger(0);

    /**
     * 成功调用次数
     */
    private final AtomicInteger successCalls = new AtomicInteger(0);

    /**
     * 失败调用次数
     */
    private final AtomicInteger failureCalls = new AtomicInteger(0);

    /**
     * 慢调用次数
     */
    private final AtomicInteger slowCalls = new AtomicInteger(0);

    public CountBasedSlidingWindow(int windowSize) {
        this.windowSize = windowSize;
        this.ringBuffer = new AtomicReferenceArray<>(windowSize);
    }

    @Override
    public void recordSuccess(long durationMs, boolean isSlowCall) {
        CallResult result = new CallResult(true, isSlowCall);
        addCallResult(result);
    }

    @Override
    public void recordFailure(long durationMs) {
        CallResult result = new CallResult(false, false);
        addCallResult(result);
    }

    /**
     * 添加调用结果到环形缓冲区
     */
    private synchronized void addCallResult(CallResult result) {
        int index = currentIndex.getAndIncrement() % windowSize;
        if (currentIndex.get() == windowSize) {
            currentIndex.set(0);
        }
        CallResult oldResult = ringBuffer.getAndSet(index, result);

        // 更新统计计数
        if (oldResult != null) {
            // 移除旧结果的统计
            if (oldResult.isSuccess()) {
                successCalls.decrementAndGet();
            } else {
                failureCalls.decrementAndGet();
            }
            if (oldResult.isSlowCall()) {
                slowCalls.decrementAndGet();
            }
            totalCalls.decrementAndGet();
        }

        // 添加新结果的统计
        if (result.isSuccess()) {
            successCalls.incrementAndGet();
        } else {
            failureCalls.incrementAndGet();
        }
        if (result.isSlowCall()) {
            slowCalls.incrementAndGet();
        }
        totalCalls.incrementAndGet();
    }

    @Override
    public synchronized SlidingWindowMetrics getMetrics() {
        return new SlidingWindowMetrics(
            totalCalls.get(),
            successCalls.get(),
            failureCalls.get(),
            slowCalls.get()
        );
    }

    @Override
    public synchronized void reset() {
        for (int i = 0; i < windowSize; i++) {
            ringBuffer.set(i, null);
        }
        currentIndex.set(0);
        failureCalls.set(0);
        slowCalls.set(0);
        successCalls.set(0);
        totalCalls.set(0);
    }

    /**
     * 调用结果
     */
    @Getter
    private static class CallResult {
        private final boolean success;
        private final boolean slowCall;

        CallResult(boolean success, boolean slowCall) {
            this.success = success;
            this.slowCall = slowCall;
        }
    }
}
