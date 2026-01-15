/*
 * Tencent is pleased to support the open source community by making BK-JOB 蓝鲸作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB 蓝鲸作业平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.tencent.bk.job.common.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 基于运营时区的定时任务注解
 * <p>
 * 该注解是对 {@link Scheduled} 的封装，默认使用运营时区 ${timezone.default.operation:Asia/Shanghai}
 * 用于统一管理后台定时任务的时区配置
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Scheduled
public @interface ScheduledOnOperationTimeZone {

    /**
     * Cron 表达式，用于定义任务执行时间
     */
    @AliasFor(annotation = Scheduled.class)
    String cron() default "";

    /**
     * 时区设置，默认使用配置中的运营时区
     */
    @AliasFor(annotation = Scheduled.class)
    String zone() default "${timezone.default.operation:Asia/Shanghai}";

    @AliasFor(annotation = Scheduled.class)
    long fixedDelay() default -1L;

    @AliasFor(annotation = Scheduled.class)
    long fixedRate() default -1L;

    @AliasFor(annotation = Scheduled.class)
    long initialDelay() default -1L;
}
