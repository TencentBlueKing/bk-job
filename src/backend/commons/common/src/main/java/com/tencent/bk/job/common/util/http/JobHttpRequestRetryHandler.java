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

package com.tencent.bk.job.common.util.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.protocol.HttpContext;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Job 第三方请求调用重试 Handler
 */
@Slf4j
public class JobHttpRequestRetryHandler implements HttpRequestRetryHandler {

    private final int retryCount;

    private final Map<String, Boolean> retryMethods;

    public JobHttpRequestRetryHandler() {
        this.retryCount = 3;
        this.retryMethods = new ConcurrentHashMap<>();
        // 蓝鲸各平台仅能保证 GET、DELETE 是幂等的，跟 RESTful API 规范有差异
        this.retryMethods.put("GET", Boolean.TRUE);
        this.retryMethods.put("DELETE", Boolean.TRUE);
    }

    @Override
    public boolean retryRequest(IOException exception,
                                int executionCount,
                                HttpContext context) {
        if (isExceedMaxRetry(executionCount)) {
            if (log.isDebugEnabled()) {
                log.debug("Http request exceed max retry times : {}", this.retryCount);
            }
            return false;
        }

        if (!isExceptionRetryable(exception)) {
            return false;
        }

        return checkByRetryMode(context);

    }

    private boolean isExceptionRetryable(IOException exception) {
        if (log.isDebugEnabled()) {
            log.debug("Http exception : {}", exception.getClass().getName());
        }
        // 异常重试机制参考默认的 org.apache.http.impl.client.DefaultHttpRequestRetryHandler，略有差异
        if (exception instanceof UnknownHostException || exception instanceof SSLException
            || exception instanceof ConnectException) {
            return false;
        } else if (exception instanceof InterruptedIOException) {
            // 部分 InterruptedIOException 的子类型需要重试
            // ConnectTimeoutException 建立连接超时/从连接池获取连接超时
            // SocketTimeoutException Socket读取超时(相应超时）
            if (exception instanceof ConnectTimeoutException || exception instanceof SocketTimeoutException) {
                if (log.isDebugEnabled()) {
                    log.debug("Retryable exception : {}", exception.getClass().getName());
                }
                return true;
            } else {
                // 其他类型的InterruptedIOException异常不可以重试
                return false;
            }
        } else {
            // 其他 IOException 可以重试，比如 NoHttpResponseException
            return true;
        }
    }

    private boolean checkByRetryMode(HttpContext context) {
        Object retryModeObj = context.getAttribute(HttpConstants.RETRY_MODE);
        RetryModeEnum retryMode = RetryModeEnum.DEFAULT;
        if (retryModeObj != null) {
            retryMode = RetryModeEnum.valOf((int) retryModeObj);
        }

        if (log.isDebugEnabled()) {
            log.debug("RetryMode : {}", retryMode);
        }
        switch (retryMode) {
            case ALWAYS:
                return true;
            case DEFAULT:
                // 判断方法使用幂等
                final HttpClientContext clientContext = HttpClientContext.adapt(context);
                final HttpRequest request = clientContext.getRequest();
                final String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
                final Boolean b = this.retryMethods.get(method);
                return b != null && b;
            default:
                return false;
        }

    }

    private boolean isExceedMaxRetry(int executionCount) {
        return executionCount > this.retryCount;
    }

}
