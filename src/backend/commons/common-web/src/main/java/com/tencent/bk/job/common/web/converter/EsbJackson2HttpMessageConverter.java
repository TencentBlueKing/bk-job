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

package com.tencent.bk.job.common.web.converter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.bk.job.common.annotation.EsbAPI;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.JobContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * 自定义HttpMessageConverter，返回时过滤一些不必要的属性，比如null
 */
@Slf4j
public class EsbJackson2HttpMessageConverter extends MappingJackson2HttpMessageConverter {

    public EsbJackson2HttpMessageConverter() {
        super(buildObjectMapper());
    }

    private static ObjectMapper buildObjectMapper() {
        Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder =
            ApplicationContextRegister.getBean(Jackson2ObjectMapperBuilder.class);
        ObjectMapper mapper = new ObjectMapper();
        if (jackson2ObjectMapperBuilder != null) {
            mapper = jackson2ObjectMapperBuilder.build();
        }
        // 排除null字段
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    /**
     * 被@EsbAPI注解标记的controller，采用自定义序列化
     */
    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        HttpServletRequest request = JobContextUtil.getRequest();
        if (request != null) {
            Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
            if (handler instanceof HandlerMethod) {
                HandlerMethod hm = (HandlerMethod) handler;
                Class<?> controllerClass = hm.getBeanType();
                if (AnnotatedElementUtils.hasAnnotation(controllerClass, EsbAPI.class)) {
                    return super.canWrite(clazz, mediaType);
                }
            }
        }
        return false;
    }
}
