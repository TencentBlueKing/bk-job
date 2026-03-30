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

package com.tencent.bk.job.common.cc.config;

import com.tencent.bk.job.common.retry.RetryPolicy;

import java.util.Collections;
import java.util.Set;

/**
 * 基于异常类型的不可重试策略。
 * <p>
 * 当捕获到的异常属于已知的、不应重试的异常类型时，立即终止重试。
 * 通常与 {@link com.tencent.bk.job.common.retry.CompositeRetryPolicy} 组合使用，
 * 与指数退避等通用策略共同决定重试行为。
 * </p>
 */
public class NonRetryableExceptionRetryPolicy implements RetryPolicy {

    private final Set<Class<? extends Exception>> nonRetryableExceptionTypes;

    public NonRetryableExceptionRetryPolicy(Set<Class<? extends Exception>> nonRetryableExceptionTypes) {
        this.nonRetryableExceptionTypes = Collections.unmodifiableSet(nonRetryableExceptionTypes);
    }

    @Override
    public long getWaitTimeMs(int attemptNumber) {
        return 0;
    }

    @Override
    public boolean shouldRetry(int attemptNumber, Exception exception) {
        for (Class<? extends Exception> nonRetryableType : nonRetryableExceptionTypes) {
            if (nonRetryableType.isInstance(exception)) {
                return false;
            }
        }
        return true;
    }
}
