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

import com.tencent.bk.job.common.model.error.ErrorType;
import lombok.Getter;
import lombok.ToString;

/**
 * 认证失败
 */
@Getter
@ToString
public class UnauthenticatedException extends ServiceException {

    public UnauthenticatedException(Integer errorCode) {
        super(ErrorType.UNAUTHENTICATED, errorCode);
    }

    public UnauthenticatedException(Integer errorCode, Object[] errorParams) {
        super(ErrorType.UNAUTHENTICATED, errorCode, errorParams);
    }

    public UnauthenticatedException(String message, Integer errorCode) {
        super(message, ErrorType.UNAUTHENTICATED, errorCode);
    }

    public UnauthenticatedException(String message, Integer errorCode, Object[] errorParams) {
        super(message, ErrorType.UNAUTHENTICATED, errorCode, errorParams);
    }

    public UnauthenticatedException(Throwable cause, Integer errorCode) {
        super(cause, ErrorType.UNAUTHENTICATED, errorCode);
    }

    public UnauthenticatedException(Throwable cause, Integer errorCode, Object[] errorParams) {
        super(cause, ErrorType.UNAUTHENTICATED, errorCode, errorParams);
    }

    public UnauthenticatedException(String message, Throwable cause, Integer errorCode) {
        super(message, cause, ErrorType.UNAUTHENTICATED, errorCode);
    }

    public UnauthenticatedException(String message, Throwable cause, Integer errorCode,
                            Object[] errorParams) {
        super(message, cause, ErrorType.UNAUTHENTICATED, errorCode, errorParams);
    }
}
