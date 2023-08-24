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

package com.tentent.bk.job.common.api.feign.condition;

import com.tencent.bk.job.common.util.JobReflections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * FeignClient 的生效判断条件
 */
@Slf4j
public class SmartFeignClientCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        // 获取使用@FeignClient的interface的Class
        ClassMetadata classMetadata = (ClassMetadata) metadata;
        String interfaceClassName = classMetadata.getClassName();
        Class<?> interfaceClass = null;
        try {
            interfaceClass = Class.forName(interfaceClassName);
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage(), e);
        }

        // 获取interface对应的所有实现类
        Set<?> subTypes = JobReflections.getSubTypesOf(interfaceClass);

        if (subTypes == null || subTypes.isEmpty()) {
            log.info("No rpc implementation found for api: {}. FeignClient condition match", interfaceClassName);
            return true;
        }
        // 遍历bean
        for (Object subType : subTypes) {
            Class<?> subTypeClass = (Class<?>) subType;
            boolean isRpcImplementClass = isRpcImplementClass(subTypeClass);
            if (isRpcImplementClass) {
                // 如果已经有 RPC 请求的实现(RestController), 那么 FeignClient 注解的 bean 无需生效，服务调用走本地进程内调用
                log.info("Found rpc implementation for api: {}. FeignClient condition not match",
                    interfaceClassName);
                return false;
            }
        }

        return true;
    }

    private boolean isRpcImplementClass(Class<?> clazz) {
        return AnnotationUtils.findAnnotation(clazz, RestController.class) != null;
    }
}
