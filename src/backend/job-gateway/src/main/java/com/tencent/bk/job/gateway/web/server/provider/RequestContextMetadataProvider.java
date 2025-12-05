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

package com.tencent.bk.job.gateway.web.server.provider;

import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.gateway.web.server.AccessLogConstants;
import reactor.netty.http.server.logging.AccessLogArgProvider;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 从上下文获取访问日志数据
 */
public class RequestContextMetadataProvider implements AccessLogMetadataProvider {

    @Override
    public Map<String, Object> extract(AccessLogArgProvider provider) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(AccessLogConstants.LogField.LOG_UPSTREAM,
            provider.requestHeader(AccessLogConstants.Header.HEAD_GATEWAY_UPSTREAM));
        map.put(AccessLogConstants.LogField.LOG_USER_NAME, provider.requestHeader(JobCommonHeaders.USERNAME) != null ?
            provider.requestHeader(JobCommonHeaders.USERNAME) : provider.requestHeader("username"));
        map.put(AccessLogConstants.LogField.LOG_TRACE_ID,
            provider.requestHeader(AccessLogConstants.Header.HEAD_TRACE_ID) != null ?
                provider.requestHeader(AccessLogConstants.Header.HEAD_TRACE_ID) :
                provider.responseHeader(JobCommonHeaders.REQUEST_ID));
        map.put(AccessLogConstants.LogField.LOG_SPAN_ID,
            provider.requestHeader(AccessLogConstants.Header.HEAD_SPAN_ID));
        return map;
    }
}
