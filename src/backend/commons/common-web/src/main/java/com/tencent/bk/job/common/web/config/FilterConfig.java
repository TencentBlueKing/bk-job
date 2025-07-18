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

package com.tencent.bk.job.common.web.config;

import com.tencent.bk.job.common.web.filter.RepeatableReadWriteServletRequestResponseFilter;
import com.tencent.bk.job.common.web.filter.WebRepeatableReadServletRequestFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    /**
     * 给/esb/api/*， /service/* 用的过滤器，包装request和response
     *
     */
    @Bean
    public FilterRegistrationBean repeatableRSRRFilterRegister() {
        FilterRegistrationBean<RepeatableReadWriteServletRequestResponseFilter> registration =
            new FilterRegistrationBean<>();
        registration.setFilter(repeatableRRRFilter());
        registration.addUrlPatterns("/esb/api/*", "/service/*");
        registration.setName("repeatableReadRequestResponseFilter");
        registration.setOrder(0);
        return registration;
    }

    /**
     * 给/web/* 用的过滤器，仅包装request
     *
     */
    @Bean
    public FilterRegistrationBean webRepeatableRRFilterRegister() {
        FilterRegistrationBean<WebRepeatableReadServletRequestFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(webRepeatableReadRequestFilter());
        registration.addUrlPatterns("/web/*");
        registration.setName("webRepeatableReadRequestFilter");
        registration.setOrder(1);
        return registration;
    }

    @Bean(name = "repeatableReadRequestResponseFilter")
    public RepeatableReadWriteServletRequestResponseFilter repeatableRRRFilter() {
        return new RepeatableReadWriteServletRequestResponseFilter();
    }

    @Bean(name = "webRepeatableReadRequestFilter")
    public WebRepeatableReadServletRequestFilter webRepeatableReadRequestFilter() {
        return new WebRepeatableReadServletRequestFilter();
    }
}
