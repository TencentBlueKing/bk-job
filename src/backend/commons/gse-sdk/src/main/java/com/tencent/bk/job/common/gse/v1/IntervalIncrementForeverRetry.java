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

package com.tencent.bk.job.common.gse.v1;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.RetrySleeper;

import java.util.concurrent.TimeUnit;

@Slf4j
public class IntervalIncrementForeverRetry implements RetryPolicy {

    private final int maxSleepMs;

    /**
     * @param maxSleepMs max time in ms to sleep on each retry
     */
    public IntervalIncrementForeverRetry(int maxSleepMs) {
        this.maxSleepMs = maxSleepMs;
    }

    @Override
    public boolean allowRetry(int retryCount, long elapsedTimeMs, RetrySleeper sleeper) {
        try {
            long sleepTimeMs = getSleepTimeMs(retryCount);
            log.info("Retry {}, sleepTimeMs: {}, elapsedTimeMs: {}", retryCount, sleepTimeMs, elapsedTimeMs);
            sleeper.sleepFor(sleepTimeMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return true;
    }

    private long getSleepTimeMs(int retryCount) {
        if (retryCount < 10) {
            return Math.min(1000, maxSleepMs);
        } else if (retryCount < 50) {
            return Math.min(5000, maxSleepMs);
        } else if (retryCount < 100) {
            return Math.min(10000, maxSleepMs);
        } else {
            return maxSleepMs;
        }
    }
}
