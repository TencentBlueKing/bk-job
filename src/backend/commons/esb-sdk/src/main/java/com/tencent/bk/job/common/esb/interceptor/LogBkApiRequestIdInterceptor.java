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

package com.tencent.bk.job.common.esb.interceptor;

import com.tencent.bk.job.common.constant.JobCommonHeaders;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.protocol.HttpContext;

/**
 * 打印APIGW请求响应Header中的X-Bkapi-Request-Id，用于问题定位
 */
@Slf4j
public class LogBkApiRequestIdInterceptor implements HttpResponseInterceptor {
    @Override
    public void process(HttpResponse response, HttpContext context) {
        StatusLine statusLine = response.getStatusLine();
        // 状态码为2xx的请求会在上层逻辑打印响应头X-Bkapi-Request-Id，此处不打印减少日志量
        if (isStatusCodeSuccess(statusLine)) {
            return;
        }
        // 获取并打印响应头X-Bkapi-Request-Id
        String headerName = JobCommonHeaders.BK_GATEWAY_REQUEST_ID;
        try {
            if (response.containsHeader(headerName)) {
                log.info(headerName + "=" + response.getFirstHeader(headerName).getValue());
            } else {
                log.info(headerName + " not found in the response");
            }
        } catch (Throwable t) {
            log.warn("Failed to log header " + headerName, t);
        }
    }

    /**
     * 判断Http状态码是否为成功（2xx）系列状态码
     *
     * @param statusLine 状态行
     * @return 是则返回true，否则返回false
     */
    private boolean isStatusCodeSuccess(StatusLine statusLine) {
        return statusLine != null
            && statusLine.getStatusCode() >= HttpStatus.SC_OK
            && statusLine.getStatusCode() < HttpStatus.SC_MULTIPLE_CHOICES;
    }
}
