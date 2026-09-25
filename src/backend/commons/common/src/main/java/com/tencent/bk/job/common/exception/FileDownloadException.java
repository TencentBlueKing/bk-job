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

package com.tencent.bk.job.common.exception;

import com.tencent.bk.job.common.model.error.FileDownloadErrorDTO;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NoHttpResponseException;
import org.apache.http.conn.ConnectionPoolTimeoutException;

import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * 文件下载失败异常，携带可直接写入文件分发日志的失败信息。
 */
@Getter
public class FileDownloadException extends InternalException {
    private final FileDownloadErrorDTO error;

    public FileDownloadException(FileDownloadErrorDTO error, int jobErrorCode) {
        super(error.getMessage(), jobErrorCode);
        this.error = error;
    }

    /**
     * 解析文件下载链路中的异常，生成可直接写入文件分发日志的失败原因。
     * 已封装过的下载异常直接返回底层响应信息；其他异常则按网络异常和原始消息兜底。
     */
    public static FileDownloadErrorDTO resolveError(Throwable throwable) {
        FileDownloadException downloadException = findCause(throwable, FileDownloadException.class);
        if (downloadException != null) {
            return downloadException.getError();
        }

        FileDownloadErrorDTO error = new FileDownloadErrorDTO();
        ServiceException serviceException = findCause(throwable, ServiceException.class);
        if (serviceException != null) {
            error.setErrorCode(String.valueOf(serviceException.getErrorCode()));
        }
        error.setMessage(resolveMessage(throwable, serviceException));
        return error;
    }

    /**
     * 优先识别网络链路异常；未命中时使用最底层异常消息。
     */
    private static String resolveMessage(Throwable throwable, ServiceException serviceException) {
        String networkMessage = resolveNetworkMessage(throwable);
        if (networkMessage != null) {
            return networkMessage;
        }
        // 异常可能被多层包装，保留最底层的非空消息。
        String message = null;
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (StringUtils.isNotBlank(cause.getMessage())) {
                message = cause.getMessage();
            }
        }
        if (StringUtils.isNotBlank(message)) {
            return message;
        }
        if (serviceException != null && StringUtils.isNotBlank(serviceException.getI18nMessage())) {
            return serviceException.getI18nMessage();
        }
        return throwable.getClass().getSimpleName();
    }

    /**
     * 将常见网络链路异常转换成更明确的失败描述。
     */
    private static String resolveNetworkMessage(Throwable throwable) {
        UnknownHostException unknownHost = findCause(throwable, UnknownHostException.class);
        if (unknownHost != null) {
            return describe("DNS resolution failed", unknownHost);
        }
        NoRouteToHostException noRoute = findCause(throwable, NoRouteToHostException.class);
        if (noRoute != null) {
            return describe("No route to host", noRoute);
        }
        ConnectException connect = findCause(throwable, ConnectException.class);
        if (connect != null) {
            return describe("Connection failed", connect);
        }
        SocketTimeoutException socketTimeout = findCause(throwable, SocketTimeoutException.class);
        if (socketTimeout != null) {
            return describe("Request timeout", socketTimeout);
        }
        ConnectionPoolTimeoutException poolTimeout = findCause(throwable, ConnectionPoolTimeoutException.class);
        if (poolTimeout != null) {
            return describe("Connection pool timeout", poolTimeout);
        }
        NoHttpResponseException noResponse = findCause(throwable, NoHttpResponseException.class);
        if (noResponse != null) {
            return describe("No response from server", noResponse);
        }
        SSLException ssl = findCause(throwable, SSLException.class);
        if (ssl != null) {
            return describe("SSL request failed", ssl);
        }
        SocketException socket = findCause(throwable, SocketException.class);
        if (socket != null) {
            return describe("Network error", socket);
        }
        return null;
    }

    private static String describe(String label, Throwable cause) {
        return StringUtils.isBlank(cause.getMessage()) ? label : label + ": " + cause.getMessage();
    }

    /**
     * 从异常调用链中查找指定类型，兼容异常被多层包装的场景。
     */
    private static <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (type.isInstance(cause)) {
                return type.cast(cause);
            }
        }
        return null;
    }
}
