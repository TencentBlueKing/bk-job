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
import com.tencent.bk.job.gateway.web.server.AccessLogExchangeDataStore;
import reactor.netty.http.server.logging.AccessLogArgProvider;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 从上下文获取访问日志数据。
 * <p>
 * traceId从响应头读取（该字段本身就需要暴露给调用方），
 * 其余字段（spanId、username、upstream）从内存Store读取，避免通过HTTP响应头暴露内部数据。
 */
public class RequestContextMetadataProvider implements AccessLogMetadataProvider {

    private final AccessLogExchangeDataStore accessLogDataStore;

    public RequestContextMetadataProvider(AccessLogExchangeDataStore accessLogDataStore) {
        this.accessLogDataStore = accessLogDataStore;
    }

    @Override
    public Map<String, Object> extract(AccessLogArgProvider provider) {
        Map<String, Object> map = new LinkedHashMap<>();
        // traceId从响应头读取（本身是有意暴露给调用方的）
        CharSequence traceIdSeq = provider.responseHeader(JobCommonHeaders.REQUEST_ID);
        String traceId = traceIdSeq != null ? traceIdSeq.toString() : null;
        map.put(AccessLogConstants.LogField.TRACE_ID, traceId);

        // 其余字段从内存Store读取并清理
        Map<String, String> storeData = accessLogDataStore.getAndRemove(traceId);
        if (storeData != null) {
            map.put(AccessLogConstants.LogField.SPAN_ID, storeData.get(AccessLogConstants.LogField.SPAN_ID));
            map.put(AccessLogConstants.LogField.USER_NAME, storeData.get(AccessLogConstants.LogField.USER_NAME));
            map.put(AccessLogConstants.LogField.UPSTREAM, storeData.get(AccessLogConstants.LogField.UPSTREAM));
        }
        return map;
    }
}
