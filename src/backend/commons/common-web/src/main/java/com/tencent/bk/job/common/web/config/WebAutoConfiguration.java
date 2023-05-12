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

package com.tencent.bk.job.common.web.config;

import com.tencent.bk.job.common.util.jwt.JwtManager;
import com.tencent.bk.job.common.web.feign.FeignConfiguration;
import com.tencent.bk.job.common.web.interceptor.EsbApiLogInterceptor;
import com.tencent.bk.job.common.web.interceptor.EsbReqRewriteInterceptor;
import com.tencent.bk.job.common.web.interceptor.JobCommonInterceptor;
import com.tencent.bk.job.common.web.interceptor.ServiceSecurityInterceptor;
import com.tencent.bk.job.common.web.util.ProfileUtil;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Import({ActuatorSecurityConfig.class, SwaggerAdapterConfig.class, FeignConfiguration.class})
public class WebAutoConfiguration {

    @Bean
    public JobCommonInterceptor jobCommonInterceptor(Tracer tracer) {
        return new JobCommonInterceptor(tracer);
    }

    @Bean
    public EsbApiLogInterceptor esbApiLogInterceptor() {
        return new EsbApiLogInterceptor();
    }

    @Bean
    public EsbReqRewriteInterceptor esbReqRewriteInterceptor() {
        return new EsbReqRewriteInterceptor();
    }

    @Bean
    public ServiceSecurityInterceptor serviceSecurityInterceptor(JwtManager jwtManager, ProfileUtil profileUtil) {
        return new ServiceSecurityInterceptor(jwtManager, profileUtil);
    }

    @Bean
    public ProfileUtil profileUtil() {
        return new ProfileUtil();
    }


}
