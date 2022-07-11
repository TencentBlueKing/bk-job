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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RecordTaskStart {
    /**
     * Name of the Timer metric.
     *
     * @return name of the Timer metric
     */
    String value() default "";

    /**
     * List of key-value pair arguments to supply the Timer as extra tags.
     *
     * @return key-value pair of tags
     * @see io.micrometer.core.instrument.Timer.Builder#tags(String...)
     */
    String[] extraTags() default {};
    /**
     * List of percentiles to calculate client-side for the {@link io.micrometer.core.instrument.Timer}.
     * For example, the 95th percentile should be passed as {@code 0.95}.
     *
     * @return percentiles to calculate
     * @see io.micrometer.core.instrument.Timer.Builder#publishPercentiles(double...)
     */
    double[] percentiles() default {};

    /**
     * Whether to enable recording of a percentile histogram for the {@link io.micrometer.core.instrument.Timer Timer}.
     *
     * @return whether percentile histogram is enabled
     * @see io.micrometer.core.instrument.Timer.Builder#publishPercentileHistogram(Boolean)
     */
    boolean histogram() default true;

    /**
     * Description of the {@link io.micrometer.core.instrument.Timer}.
     *
     * @return meter description
     * @see io.micrometer.core.instrument.Timer.Builder#description(String)
     */
    String description() default "";
}
