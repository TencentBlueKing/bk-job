/*
 * Tencent is pleased to support the open source community by making BK-JOB 蓝鲸作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB 蓝鲸作业平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.tencent.bk.job.common.model.error;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ErrorType {
    OK(1),
    INVALID_PARAM(2),
    FAILED_PRECONDITION(3),
    UNAUTHENTICATED(4),
    PERMISSION_DENIED(5),
    NOT_FOUND(6),
    ALREADY_EXISTS(7),
    ABORTED(8),
    RESOURCE_EXHAUSTED(9),
    UNIMPLEMENTED(10),
    INTERNAL(11),
    UNAVAILABLE(12),
    TIMEOUT(13);

    @JsonValue
    private final int type;

    ErrorType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    @JsonCreator
    public static ErrorType valOf(Integer type) {
        if (type == null) {
            return null;
        }
        for (ErrorType errorType : values()) {
            if (errorType.getType() == type) {
                return errorType;
            }
        }
        return null;
    }
}
