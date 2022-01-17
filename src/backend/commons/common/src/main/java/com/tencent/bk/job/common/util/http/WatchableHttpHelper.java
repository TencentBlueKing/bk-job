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

import com.tencent.bk.job.common.util.JobContextUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.util.AbstractList;
import java.util.concurrent.TimeUnit;

/**
 * 对Http请求状态码与异常情况进行统计便于监控
 */
public class WatchableHttpHelper implements HttpHelper {

    private final HttpHelper httpHelper;
    private final MeterRegistry meterRegistry;

    public WatchableHttpHelper(HttpHelper httpHelper, MeterRegistry meterRegistry) {
        this.httpHelper = httpHelper;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public CloseableHttpResponse getRawResp(boolean keepAlive, String url, Header[] header) {
        return httpHelper.getRawResp(keepAlive, url, header);
    }

    @Override
    public Pair<Integer, String> get(boolean keepAlive, String url, Header[] header) {
        String httpMetricName = JobContextUtil.getHttpMetricName();
        long start = System.nanoTime();
        String httpStatus = null;
        try {
            Pair<Integer, String> pair = httpHelper.get(keepAlive, url, header);
            if (pair != null) {
                httpStatus = "" + pair.getLeft();
            } else {
                httpStatus = "null";
            }
            return pair;
        } catch (Throwable t) {
            httpStatus = "error";
            throw t;
        } finally {
            long end = System.nanoTime();
            AbstractList<Tag> httpMetricTags = JobContextUtil.getHttpMetricTags();
            httpMetricTags.add(Tag.of("http_status", httpStatus));
            if (meterRegistry != null && StringUtils.isNotBlank(httpMetricName)) {
                meterRegistry.timer(httpMetricName, httpMetricTags)
                    .record(end - start, TimeUnit.NANOSECONDS);
            }
        }
    }

    @Override
    public Pair<Integer, byte[]> post(String url, HttpEntity requestEntity, Header... headers) {
        String httpMetricName = JobContextUtil.getHttpMetricName();
        long start = System.nanoTime();
        String httpStatus = null;
        try {
            Pair<Integer, byte[]> pair = httpHelper.post(url, requestEntity, headers);
            if (pair != null) {
                httpStatus = "" + pair.getLeft();
            } else {
                httpStatus = "null";
            }
            return pair;
        } catch (Throwable t) {
            httpStatus = "error";
            throw t;
        } finally {
            long end = System.nanoTime();
            AbstractList<Tag> httpMetricTags = JobContextUtil.getHttpMetricTags();
            httpMetricTags.add(Tag.of("http_status", httpStatus));
            if (meterRegistry != null && StringUtils.isNotBlank(httpMetricName)) {
                meterRegistry.timer(httpMetricName, httpMetricTags)
                    .record(end - start, TimeUnit.NANOSECONDS);
            }
        }
    }

    @Override
    public Pair<Integer, String> put(String url, HttpEntity requestEntity, Header... headers) {
        String httpMetricName = JobContextUtil.getHttpMetricName();
        long start = System.nanoTime();
        String httpStatus = null;
        try {
            Pair<Integer, String> pair = httpHelper.put(url, requestEntity, headers);
            if (pair != null) {
                httpStatus = "" + pair.getLeft();
            } else {
                httpStatus = "null";
            }
            return pair;
        } catch (Throwable t) {
            httpStatus = "error";
            throw t;
        } finally {
            long end = System.nanoTime();
            AbstractList<Tag> httpMetricTags = JobContextUtil.getHttpMetricTags();
            httpMetricTags.add(Tag.of("http_status", httpStatus));
            if (meterRegistry != null && StringUtils.isNotBlank(httpMetricName)) {
                meterRegistry.timer(httpMetricName, httpMetricTags)
                    .record(end - start, TimeUnit.NANOSECONDS);
            }
        }
    }

    @Override
    public Pair<Integer, String> delete(String url, String content, Header... headers) {
        String httpMetricName = JobContextUtil.getHttpMetricName();
        long start = System.nanoTime();
        String httpStatus = null;
        try {
            Pair<Integer, String> pair = httpHelper.delete(url, content, headers);
            if (pair != null) {
                httpStatus = "" + pair.getLeft();
            } else {
                httpStatus = "null";
            }
            return pair;
        } catch (Throwable t) {
            httpStatus = "error";
            throw t;
        } finally {
            long end = System.nanoTime();
            AbstractList<Tag> httpMetricTags = JobContextUtil.getHttpMetricTags();
            httpMetricTags.add(Tag.of("http_status", httpStatus));
            if (meterRegistry != null && StringUtils.isNotBlank(httpMetricName)) {
                meterRegistry.timer(httpMetricName, httpMetricTags)
                    .record(end - start, TimeUnit.NANOSECONDS);
            }
        }
    }
}
