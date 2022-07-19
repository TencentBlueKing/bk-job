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

package com.tencent.bk.job.common.web.metrics;

import io.micrometer.core.instrument.Tag;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 为Job自定义timer类指标提供标签
 */
public interface CustomTimedTagsContributor {

    /**
     * 是否支持解析指定指标数据的标签
     *
     * @param metricName 指标名称
     * @return 是否支持
     */
    boolean supports(String metricName);

    /**
     * 根据指标名称、请求内容、响应、handler等原始数据提取Tags
     *
     * @param metricName    指标名称
     * @param request       请求
     * @param response      响应
     * @param handlerMethod 请求处理方法
     * @param exception     异常
     * @return 指标集合
     */
    Iterable<Tag> getTags(String metricName,
                          HttpServletRequest request,
                          HttpServletResponse response,
                          HandlerMethod handlerMethod,
                          Throwable exception);
}
