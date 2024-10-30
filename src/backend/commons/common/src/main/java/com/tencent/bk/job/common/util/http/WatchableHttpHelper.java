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

package com.tencent.bk.job.common.util.http;

import com.tencent.bk.job.common.exception.HttpStatusException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;

import java.util.AbstractList;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 对Http请求状态码与异常情况进行统计便于监控
 */
public class WatchableHttpHelper implements HttpHelper {

    private final HttpHelper httpHelper;
    private final Supplier<MeterRegistry> meterRegistrySupplier;

    public WatchableHttpHelper(HttpHelper httpHelper, Supplier<MeterRegistry> meterRegistrySupplier) {
        this.httpHelper = httpHelper;
        this.meterRegistrySupplier = meterRegistrySupplier;
    }

    @Override
    public Pair<HttpRequestBase, CloseableHttpResponse> getRawResp(boolean keepAlive, String url, Header[] header) {
        return httpHelper.getRawResp(keepAlive, url, header);
    }

    @Override
    public HttpResponse requestForSuccessResp(HttpRequest request) throws HttpStatusException {
        return requestInternal(request, httpHelper::requestForSuccessResp);
    }

    @Override
    public HttpResponse request(HttpRequest request) {
        return requestInternal(request, httpHelper::request);
    }

    private HttpResponse requestInternal(HttpRequest request,
                                         Function<HttpRequest, HttpResponse> requestImpl) {
        String httpMetricName = HttpMetricUtil.getHttpMetricName();
        long start = System.nanoTime();
        String httpStatusTagValue = null;
        try {
            HttpResponse response = requestImpl.apply(request);
            httpStatusTagValue = String.valueOf(response.getStatusCode());
            return response;
        } catch (HttpStatusException t) {
            httpStatusTagValue = String.valueOf(t.getHttpStatus());
            throw t;
        } finally {
            long end = System.nanoTime();
            AbstractList<Tag> httpMetricTags = HttpMetricUtil.getCurrentMetricTags();
            httpMetricTags.add(Tag.of("http_status",
                StringUtils.isNotEmpty(httpStatusTagValue) ? httpStatusTagValue : "UNKNOWN"));
            MeterRegistry meterRegistry = meterRegistrySupplier.get();
            if (meterRegistry != null && StringUtils.isNotBlank(httpMetricName)) {
                meterRegistry.timer(httpMetricName, httpMetricTags)
                    .record(end - start, TimeUnit.NANOSECONDS);
            }
        }
    }
}
