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

package com.tencent.bk.job.file_gateway.service.retry.impl;

import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.file_gateway.service.context.impl.FileSourceTaskRetryContext;
import com.tencent.bk.job.file_gateway.service.retry.FileSourceTaskRetryPolicy;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * 针对由于某些异常导致的任务启动失败重试策略
 * 当前支持的异常：UnknownHostException、SocketTimeoutException: connect timed out
 */
public class ExceptionRetryPolicy implements FileSourceTaskRetryPolicy {

    /**
     * 最大重试次数
     */
    private final int maxRetryCount;
    /**
     * 重试前应当休眠的毫秒数
     */
    private final int sleepMillsBeforeRetryCount;

    public ExceptionRetryPolicy(int maxRetryCount, int sleepMillsBeforeRetryCount) {
        this.maxRetryCount = maxRetryCount;
        this.sleepMillsBeforeRetryCount = sleepMillsBeforeRetryCount;
    }

    @Override
    public boolean shouldRetry(FileSourceTaskRetryContext context, int retryCount) {
        // 超出重试次数后不再重试
        if (retryCount > maxRetryCount) {
            return false;
        }
        // 各层次异常检查
        Exception exception = context.getException();
        if (!(exception instanceof InternalException)) {
            return false;
        }
        Throwable cause = exception.getCause();
        if (!(cause instanceof ResourceAccessException)) {
            return false;
        }
        Throwable innerCause = cause.getCause();
        if (isTargetThrowable(innerCause)) {
            ThreadUtils.sleep(sleepMillsBeforeRetryCount);
            return true;
        }
        return false;
    }

    private boolean isTargetThrowable(Throwable t) {
        return isUnknownHostException(t) || isConnectTimeoutException(t);
    }

    private boolean isUnknownHostException(Throwable t) {
        return t instanceof UnknownHostException;
    }

    private boolean isConnectTimeoutException(Throwable t) {
        if (!(t instanceof SocketTimeoutException)) {
            return false;
        }
        String message = t.getMessage();
        return message != null && message.equalsIgnoreCase("connect timed out");
    }
}
