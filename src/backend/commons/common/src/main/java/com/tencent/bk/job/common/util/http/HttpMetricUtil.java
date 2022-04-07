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
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Map;

/**
 * HTTP请求度量指标工具类
 */
@Slf4j
public class HttpMetricUtil {

    // 单个Http请求相关指标
    private static final String SCOPE_SINGLE_HTTP_REQUEST_METRIC = "SINGLE_HTTP_REQUEST_METRIC";

    private static Pair<String, AbstractList<Tag>> getHttpMetricPair() {
        Map<String, Pair<String, AbstractList<Tag>>> metricTagsMap = JobContextUtil.getOrInitMetricTagsMap();
        return metricTagsMap.get(SCOPE_SINGLE_HTTP_REQUEST_METRIC);
    }

    /**
     * 获取度量指标名称，一般一个第三方系统（CMDB、IAM、BKREPO等）对应一个指标名称
     *
     * @return 指标名称
     */
    public static String getHttpMetricName() {
        Map<String, Pair<String, AbstractList<Tag>>> metricTagsMap = JobContextUtil.getOrInitMetricTagsMap();
        String httpMetricName = null;
        Pair<String, AbstractList<Tag>> pair = metricTagsMap.get(SCOPE_SINGLE_HTTP_REQUEST_METRIC);
        if (pair != null) {
            httpMetricName = pair.getLeft();
        }
        return httpMetricName;
    }

    /**
     * 设置度量指标名称，一般一个第三方系统（CMDB、IAM、BKREPO等）对应一个指标名称
     *
     * @param httpMetricName 指标名称
     */
    public static void setHttpMetricName(String httpMetricName) {
        Map<String, Pair<String, AbstractList<Tag>>> metricTagsMap = JobContextUtil.getOrInitMetricTagsMap();
        Pair<String, AbstractList<Tag>> pair = metricTagsMap.get(SCOPE_SINGLE_HTTP_REQUEST_METRIC);
        if (pair == null || (httpMetricName != null && !httpMetricName.equals(pair.getLeft()))) {
            metricTagsMap.put(SCOPE_SINGLE_HTTP_REQUEST_METRIC, Pair.of(httpMetricName, new ArrayList<>()));
        }
    }

    /**
     * 获取当前度量指标标签
     *
     * @return 指标标签列表
     */
    public static AbstractList<Tag> getCurrentMetricTags() {
        Pair<String, AbstractList<Tag>> pair = getHttpMetricPair();
        if (pair == null) {
            return new ArrayList<>();
        }
        return pair.getRight();
    }

    /**
     * 为当前度量指标增加一个标签
     *
     * @param httpMetricTag 指标标签
     */
    public static void addTagForCurrentMetric(Tag httpMetricTag) {
        Pair<String, AbstractList<Tag>> pair = getHttpMetricPair();
        if (pair == null) {
            log.warn("Cannot add http metric tag, please set metric name first");
            return;
        }
        AbstractList<Tag> httpMetricTags = pair.getRight();
        if (httpMetricTags == null) {
            httpMetricTags = new ArrayList<>();
            pair.setValue(httpMetricTags);
        }
        httpMetricTags.add(httpMetricTag);
    }

    /**
     * 清除当前度量指标所有数据
     */
    public static void clearHttpMetric() {
        Map<String, Pair<String, AbstractList<Tag>>> metricTagsMap = JobContextUtil.getOrInitMetricTagsMap();
        metricTagsMap.remove(SCOPE_SINGLE_HTTP_REQUEST_METRIC);
    }
}
