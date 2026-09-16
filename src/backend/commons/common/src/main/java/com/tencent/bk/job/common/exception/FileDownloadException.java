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

    private static String resolveMessage(Throwable throwable, ServiceException serviceException) {
        String linkErrorMessage = resolveLinkErrorMessage(throwable);
        if (StringUtils.isNotBlank(linkErrorMessage)) {
            return linkErrorMessage;
        }

        String message = getRootMessage(throwable);
        if (StringUtils.isNotBlank(message)) {
            return message;
        }
        if (serviceException != null && StringUtils.isNotBlank(serviceException.getMessage())) {
            return serviceException.getMessage();
        }
        if (serviceException != null && StringUtils.isNotBlank(serviceException.getI18nMessage())) {
            return serviceException.getI18nMessage();
        }
        return throwable.getClass().getSimpleName();
    }

    private static <T extends Throwable> T findCause(Throwable throwable, Class<T> causeClass) {
        Throwable current = throwable;
        while (current != null) {
            if (causeClass.isInstance(current)) {
                return causeClass.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }

    private static String getRootMessage(Throwable throwable) {
        String message = null;
        Throwable current = throwable;
        while (current != null) {
            if (StringUtils.isNotBlank(current.getMessage())) {
                message = current.getMessage();
            }
            current = current.getCause();
        }
        return message;
    }

    private static String resolveLinkErrorMessage(Throwable throwable) {
        UnknownHostException unknownHostException = findCause(throwable, UnknownHostException.class);
        if (unknownHostException != null) {
            return appendExceptionMessage("DNS resolution failed", unknownHostException);
        }
        NoRouteToHostException noRouteToHostException = findCause(throwable, NoRouteToHostException.class);
        if (noRouteToHostException != null) {
            return appendExceptionMessage("No route to host", noRouteToHostException);
        }
        ConnectException connectException = findCause(throwable, ConnectException.class);
        if (connectException != null) {
            return appendExceptionMessage("Connection failed", connectException);
        }
        SocketTimeoutException timeoutException = findCause(throwable, SocketTimeoutException.class);
        if (timeoutException != null) {
            return appendExceptionMessage("Request timeout", timeoutException);
        }
        ConnectionPoolTimeoutException connectionPoolTimeoutException =
            findCause(throwable, ConnectionPoolTimeoutException.class);
        if (connectionPoolTimeoutException != null) {
            return appendExceptionMessage("Connection pool timeout", connectionPoolTimeoutException);
        }
        NoHttpResponseException noHttpResponseException = findCause(throwable, NoHttpResponseException.class);
        if (noHttpResponseException != null) {
            return appendExceptionMessage("No response from server", noHttpResponseException);
        }
        SSLException sslException = findCause(throwable, SSLException.class);
        if (sslException != null) {
            return appendExceptionMessage("SSL request failed", sslException);
        }
        SocketException socketException = findCause(throwable, SocketException.class);
        if (socketException != null) {
            return appendExceptionMessage("Network error", socketException);
        }
        return null;
    }

    private static String appendExceptionMessage(String message, Throwable throwable) {
        if (StringUtils.isBlank(throwable.getMessage())) {
            return message;
        }
        return message + ": " + throwable.getMessage();
    }
}
