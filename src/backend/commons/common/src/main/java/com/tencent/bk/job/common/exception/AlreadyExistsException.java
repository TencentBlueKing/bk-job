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

package com.tencent.bk.job.common.exception;

import com.tencent.bk.job.common.model.error.ErrorType;
import lombok.Getter;
import lombok.ToString;

/**
 * 创建的资源已存在
 */
@Getter
@ToString
public class AlreadyExistsException extends ServiceException {
    public AlreadyExistsException(Integer errorCode) {
        super(ErrorType.ALREADY_EXISTS, errorCode);
    }

    public AlreadyExistsException(Integer errorCode, Object[] errorParams) {
        super(ErrorType.ALREADY_EXISTS, errorCode, errorParams);
    }

    public AlreadyExistsException(Integer errorType, Integer errorCode, Object errorParam) {
        super(ErrorType.ALREADY_EXISTS, errorCode, errorParam);
    }

    public AlreadyExistsException(Throwable cause, Integer errorCode) {
        super(cause, ErrorType.ALREADY_EXISTS, errorCode);
    }

    public AlreadyExistsException(Throwable cause, Integer errorCode, Object[] errorParams) {
        super(cause, ErrorType.ALREADY_EXISTS, errorCode, errorParams);
    }

    public AlreadyExistsException(Throwable cause, Integer errorCode, Object errorParam) {
        super(cause, ErrorType.ALREADY_EXISTS, errorCode, errorParam);
    }

    public AlreadyExistsException(String message, Throwable cause, Integer errorCode) {
        super(message, cause, ErrorType.ALREADY_EXISTS, errorCode);
    }

    public AlreadyExistsException(String message, Throwable cause, Integer errorCode,
                                  Object[] errorParams) {
        super(message, cause, ErrorType.ALREADY_EXISTS, errorCode, errorParams);
    }

    public AlreadyExistsException(String message, Throwable cause, Integer errorCode,
                                  Object errorParam) {
        super(message, cause, ErrorType.ALREADY_EXISTS, errorCode, errorParam);
    }
}
