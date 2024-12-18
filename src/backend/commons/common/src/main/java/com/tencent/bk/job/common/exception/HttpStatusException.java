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

import lombok.Getter;
import lombok.ToString;

/**
 * 服务异常
 */
@Getter
@ToString
public class HttpStatusException extends RuntimeException {
    private final String uri;
    private final int httpStatus;
    private final String reasonPhrase;
    private final String respBodyStr;

    public HttpStatusException(String uri, int httpStatus, String reasonPhrase) {
        super("http status not ok, uri=" + uri + ", statusCode=" + httpStatus + ", reasonPhrase=" + reasonPhrase);
        this.uri = uri;
        this.httpStatus = httpStatus;
        this.reasonPhrase = reasonPhrase;
        respBodyStr = null;
    }

    public HttpStatusException(String uri, int httpStatus, String reasonPhrase, String respBodyStr) {
        super("http status not ok, uri=" + uri
            + ", statusCode=" + httpStatus + ", reasonPhrase=" + reasonPhrase + ", respBodyStr=" + respBodyStr);
        this.uri = uri;
        this.httpStatus = httpStatus;
        this.reasonPhrase = reasonPhrase;
        this.respBodyStr = respBodyStr;
    }
}
