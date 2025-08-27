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

import com.tencent.bk.job.common.annotation.JobInterceptor;
import com.tencent.bk.job.common.web.converter.EsbJackson2HttpMessageConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Map;


/**
 * 拦截器自动注册
 */
@Slf4j
public class WebInterceptorAutoRegister implements WebMvcConfigurer {

    private final ApplicationContext applicationContext;
    private final Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder;

    public WebInterceptorAutoRegister(ApplicationContext applicationContext,
                                      Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder) {
        this.applicationContext = applicationContext;
        this.jackson2ObjectMapperBuilder = jackson2ObjectMapperBuilder;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        Map<String, HandlerInterceptor> interceptorMap = applicationContext.getBeansOfType(HandlerInterceptor.class);
        interceptorMap.forEach((name, interceptor) -> {
            JobInterceptor jobInterceptor = interceptor.getClass().getAnnotation(JobInterceptor.class);
            if (jobInterceptor != null) {
                log.info("Add job interceptor: {}, pathPatterns: {}, order: {}",
                    interceptor.getClass().getName(),
                    jobInterceptor.pathPatterns(),
                    jobInterceptor.order());
                registry.addInterceptor(interceptor)
                    .addPathPatterns(jobInterceptor.pathPatterns())
                    .order(jobInterceptor.order());
            }
        });
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        int index = 0;
        for (int i = 0; i < converters.size(); i++) {
            if (converters.get(i) instanceof MappingJackson2HttpMessageConverter) {
                index = i;
                break;
            }
        }
        // 在默认Jackson Converter之前
        converters.add(index, new EsbJackson2HttpMessageConverter(jackson2ObjectMapperBuilder));
    }
}
