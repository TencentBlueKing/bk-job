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

package com.tencent.bk.job.common.retry;

import java.util.Arrays;
import java.util.List;

/**
 * 组合重试策略。
 * <p>
 * 将多个 {@link RetryPolicy} 组合为一个策略：
 * <ul>
 *     <li>等待时间取所有策略的最大值</li>
 *     <li>是否重试取所有策略的"与"运算——任一策略判定不重试则不重试</li>
 * </ul>
 */
public class CompositeRetryPolicy implements RetryPolicy {

    private final List<RetryPolicy> policies;

    public CompositeRetryPolicy(RetryPolicy... policies) {
        if (policies == null || policies.length == 0) {
            throw new IllegalArgumentException("At least one RetryPolicy is required");
        }
        this.policies = Arrays.asList(policies);
    }

    @Override
    public long getWaitTimeMs(int attemptNumber) {
        long maxWaitTime = 0;
        for (RetryPolicy policy : policies) {
            maxWaitTime = Math.max(maxWaitTime, policy.getWaitTimeMs(attemptNumber));
        }
        return maxWaitTime;
    }

    @Override
    public boolean shouldRetry(int attemptNumber, Exception exception) {
        for (RetryPolicy policy : policies) {
            if (!policy.shouldRetry(attemptNumber, exception)) {
                return false;
            }
        }
        return true;
    }
}
