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

package com.tencent.bk.job.execute.metrics;

import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.metrics.CommonMetricTags;
import com.tencent.bk.job.common.web.metrics.CustomTimedTagsContributor;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 任务启动指标标签生成器
 */
@Component
public class TaskStartTagsContributor implements CustomTimedTagsContributor {

    @Override
    public boolean supports(String metricName) {
        return CommonMetricNames.JOB_TASK_START.equals(metricName);
    }

    @Override
    public Iterable<Tag> getTags(String metricName,
                                 HttpServletRequest request,
                                 HttpServletResponse response,
                                 HandlerMethod handlerMethod,
                                 Throwable exception) {
        return Tags.of(CommonMetricTags.KEY_START_STATUS, parseStartStatusFromResp(response));
    }

    private String parseStartStatusFromResp(HttpServletResponse response) {
        if (response.getStatus() < HttpStatus.SC_INTERNAL_SERVER_ERROR) {
            return CommonMetricTags.VALUE_START_STATUS_SUCCESS;
        }
        return CommonMetricTags.VALUE_START_STATUS_FAILED;
    }
}
