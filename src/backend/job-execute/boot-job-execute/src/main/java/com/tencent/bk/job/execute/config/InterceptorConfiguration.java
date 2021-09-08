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

package com.tencent.bk.job.execute.config;

import com.tencent.bk.job.common.iam.interceptor.AuthAppInterceptor;
import com.tencent.bk.job.common.web.interceptor.EsbApiLogInterceptor;
import com.tencent.bk.job.common.web.interceptor.EsbReqRewriteInterceptor;
import com.tencent.bk.job.common.web.interceptor.JobCommonInterceptor;
import com.tencent.bk.job.common.web.interceptor.ServiceSecurityInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfiguration implements WebMvcConfigurer {

    private final JobCommonInterceptor jobCommonInterceptor;
    private final AuthAppInterceptor authAppInterceptor;
    private final EsbApiLogInterceptor esbApiLogInterceptor;
    private final ServiceSecurityInterceptor serviceSecurityInterceptor;
    private final EsbReqRewriteInterceptor esbReqRewriteInterceptor;

    @Autowired
    public InterceptorConfiguration(
        JobCommonInterceptor jobCommonInterceptor,
        AuthAppInterceptor authAppInterceptor,
        EsbApiLogInterceptor esbApiLogInterceptor,
        ServiceSecurityInterceptor serviceSecurityInterceptor,
        EsbReqRewriteInterceptor esbReqRewriteInterceptor
    ) {
        this.jobCommonInterceptor = jobCommonInterceptor;
        this.authAppInterceptor = authAppInterceptor;
        this.esbApiLogInterceptor = esbApiLogInterceptor;
        this.serviceSecurityInterceptor = serviceSecurityInterceptor;
        this.esbReqRewriteInterceptor = esbReqRewriteInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册拦截器
        registry.addInterceptor(serviceSecurityInterceptor).addPathPatterns("/**").order(0);
        registry.addInterceptor(jobCommonInterceptor).addPathPatterns("/**").order(1);
        registry.addInterceptor(esbApiLogInterceptor).addPathPatterns("/esb/api/**").order(10);
        registry.addInterceptor(esbReqRewriteInterceptor).addPathPatterns("/esb/api/**").order(11);
        registry.addInterceptor(authAppInterceptor).addPathPatterns("/web/**", "/esb/api/**").order(12);
    }
}
