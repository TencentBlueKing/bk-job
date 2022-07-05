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
import com.tencent.bk.job.common.web.interceptor.JobApiMetricInterceptor;
import com.tencent.bk.job.common.web.interceptor.JobCommonInterceptor;
import com.tencent.bk.job.common.web.interceptor.ServiceSecurityInterceptor;
import com.tencent.bk.job.execute.common.interceptor.UriPermissionInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfiguration implements WebMvcConfigurer {

    private final JobApiMetricInterceptor jobApiMetricInterceptor;
    private final JobCommonInterceptor jobCommonInterceptor;
    private final UriPermissionInterceptor uriPermissionInterceptor;
    private final AuthAppInterceptor authAppInterceptor;
    private final EsbApiLogInterceptor esbApiLogInterceptor;
    private final ServiceSecurityInterceptor serviceSecurityInterceptor;
    private final EsbReqRewriteInterceptor esbReqRewriteInterceptor;

    @Autowired
    public InterceptorConfiguration(
        JobApiMetricInterceptor jobApiMetricInterceptor,
        JobCommonInterceptor jobCommonInterceptor,
        AuthAppInterceptor authAppInterceptor,
        UriPermissionInterceptor uriPermissionInterceptor, EsbApiLogInterceptor esbApiLogInterceptor,
        ServiceSecurityInterceptor serviceSecurityInterceptor,
        EsbReqRewriteInterceptor esbReqRewriteInterceptor
    ) {
        this.jobApiMetricInterceptor = jobApiMetricInterceptor;
        this.jobCommonInterceptor = jobCommonInterceptor;
        this.uriPermissionInterceptor = uriPermissionInterceptor;
        this.authAppInterceptor = authAppInterceptor;
        this.esbApiLogInterceptor = esbApiLogInterceptor;
        this.serviceSecurityInterceptor = serviceSecurityInterceptor;
        this.esbReqRewriteInterceptor = esbReqRewriteInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册拦截器
        // 最高优先级：初始化JobContext
        registry.addInterceptor(jobCommonInterceptor).addPathPatterns("/**").order(0);
        // 需要借助JobContext存储metrics数据
        registry.addInterceptor(jobApiMetricInterceptor).addPathPatterns("/**").order(10);
        registry.addInterceptor(serviceSecurityInterceptor).addPathPatterns("/**").order(20);
        registry.addInterceptor(uriPermissionInterceptor)
            .addPathPatterns(
                uriPermissionInterceptor.getControlUriPatterns()
            ).order(30);
        registry.addInterceptor(esbApiLogInterceptor).addPathPatterns("/esb/api/**").order(40);
        registry.addInterceptor(esbReqRewriteInterceptor).addPathPatterns("/esb/api/**").order(50);
        registry.addInterceptor(authAppInterceptor).addPathPatterns("/web/**", "/esb/api/**").order(60);
    }
}
