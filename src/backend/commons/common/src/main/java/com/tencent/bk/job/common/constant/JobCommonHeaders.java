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

package com.tencent.bk.job.common.constant;

public interface JobCommonHeaders {

    String APP_CODE = "X-AppCode";
    String USERNAME = "X-Username";
    String REQUEST_ID = "X-Bk-Job-Request-Id";

    /**
     * 蓝鲸网关-语言
     */
    String BK_GATEWAY_LANG = "Blueking-Language";
    /**
     * 蓝鲸网关-RequestId
     */
    String BK_GATEWAY_REQUEST_ID = "X-Bkapi-Request-Id";
    /**
     * 蓝鲸网关-JWT
     */
    String BK_GATEWAY_JWT = "X-Bkapi-JWT";

    /**
     * 蓝鲸网关-从网关来的请求，与ESB请求区分
     */
    String BK_GATEWAY_FROM = "X-Bkapi-From";
}
