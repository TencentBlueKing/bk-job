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
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.tencent.bk.job.common.util.http.RetryModeEnum.SAFE_GUARANTEED;

/**
 * Job 第三方请求调用重试 Handler
 */
@Slf4j
public class JobHttpRequestRetryHandler implements HttpRequestRetryHandler {

    private final int retryCount;

    private final Map<String, Boolean> retryMethods;

    private final Set<Class<? extends IOException>> retryableExceptions;

    public JobHttpRequestRetryHandler() {
        this.retryCount = 3;
        this.retryMethods = new ConcurrentHashMap<>();
        // 蓝鲸各平台仅能保证 GET、DELETE 是幂等的，跟 RESTful API 规范有差异
        this.retryMethods.put("GET", Boolean.TRUE);
        this.retryMethods.put("DELETE", Boolean.TRUE);
        retryableExceptions = new HashSet<>();
        // ConnectTimeoutException 建立连接超时/从连接池获取连接超时
        // SocketTimeoutException Socket连接/读取超时
        // NoHttpResponseException 服务器端关闭连接或者服务端负载太高无法响应，nginx reload，以及网络连接问题
        // httpclient 异常处理参考 https://hc.apache.org/httpclient-legacy/exception-handling.html 的说明
        retryableExceptions.add(ConnectTimeoutException.class);
        retryableExceptions.add(SocketTimeoutException.class);
        retryableExceptions.add(NoHttpResponseException.class);
    }

    @Override
    public boolean retryRequest(IOException exception,
                                int executionCount,
                                HttpContext context) {
        if (isExceedMaxRetry(executionCount)) {
            log.info("[{}] Retry rejected. Http request exceed max retry times : {}",
                exception.getClass().getSimpleName(), this.retryCount);
            return false;
        }

        if (!isExceptionRetryable(exception)) {
            log.info("[{}] Retry rejected. Exception is not retryable", exception.getClass().getSimpleName());
            return false;
        }

        return checkByRetryMode(context, exception);

    }

    private boolean isExceptionRetryable(IOException exception) {
        if (log.isDebugEnabled()) {
            log.debug("Http exception : {}", exception.getClass().getName());
        }
        return retryableExceptions.contains(exception.getClass());
    }

    private boolean checkByRetryMode(HttpContext context, IOException exception) {
        RetryModeEnum retryMode = getRetryMode(context);

        switch (retryMode) {
            case ALWAYS:
                log.info("[{}] Retry accepted. Retry mode is ALWAYS", exception.getClass().getSimpleName());
                return true;
            case NEVER:
                log.info("[{}] Retry rejected. Retry mode is NEVER", exception.getClass().getSimpleName());
                return false;
            case SAFE_GUARANTEED:
                // 判断重试请求是否安全
                boolean isRetryable = checkIsRetrySafe(context, exception);
                if (isRetryable) {
                    log.info("[{}] Retry accepted. Retry is safe", exception.getClass().getSimpleName());
                    return true;
                } else {
                    log.info("[{}] Retry rejected. Retry is not safe", exception.getClass().getSimpleName());
                    return false;
                }
            default:
                return false;
        }

    }

    private boolean checkIsRetrySafe(HttpContext context, IOException exception) {
        boolean isRequestIdempotent = isRequestIdempotent(context);
        if (isRequestIdempotent) {
            return true;
        }
        // ConnectTimeoutException/NoHttpResponseException，服务端并没有开始接受和正式处理请求，可以重试
        // SocketTimeoutException 可能服务端已经接受处理了请求，如果二次请求可能引发问题
        return exception instanceof ConnectTimeoutException || exception instanceof NoHttpResponseException;
    }

    private boolean isRequestIdempotent(HttpContext context) {
        // 判断方法使用幂等
        Object isIdempotentAttrVal = context.getAttribute(HttpContextAttributeNames.IS_IDEMPOTENT);
        if (log.isDebugEnabled()) {
            log.debug("HttpContext::IS_IDEMPOTENT: {}", isIdempotentAttrVal);
        }
        boolean isIdempotent = isIdempotentAttrVal != null && (boolean) isIdempotentAttrVal;
        if (isIdempotent) {
            // 如果上下文主动设置了该请求的幂等参数，那么优先使用
            return true;
        }

        // 上下文没有主动设置幂等参数，那么按照 http method 的实际幂等情况判断
        final HttpClientContext clientContext = HttpClientContext.adapt(context);
        final HttpRequest request = clientContext.getRequest();
        final String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
        final Boolean b = this.retryMethods.get(method);
        if (log.isDebugEnabled()) {
            log.debug("Method: {}, isRequestIdempotent: {}",method, isIdempotentAttrVal);
        }
        return b != null && b;
    }


    private RetryModeEnum getRetryMode(HttpContext context) {
        Object retryModeObj = context.getAttribute(HttpContextAttributeNames.RETRY_MODE);
        RetryModeEnum retryMode = SAFE_GUARANTEED;
        if (retryModeObj != null) {
            retryMode = RetryModeEnum.valOf((int) retryModeObj);
        }
        if (log.isDebugEnabled()) {
            log.debug("RetryMode : {}", retryMode);
        }
        return retryMode;
    }

    private boolean isExceedMaxRetry(int executionCount) {
        return executionCount > this.retryCount;
    }

}
