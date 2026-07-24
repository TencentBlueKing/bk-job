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

package com.tencent.bk.job.assemble.metrics;

import com.tencent.bk.job.common.mq.metrics.MqConsumeDelayRecorder;
import com.tencent.bk.job.common.mq.metrics.MqMetricsProperties;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * 轻量化部署模式（job-assemble）下统一的MQ延迟消费记录器
 * <p>
 * 注意：该类不使用@Component自动装配，由JobAssembleConfiguration显式声明Bean，
 * 以避免与各service-job-xxx子模块下的XxxMqConsumeDelayRecorder共存导致的多Bean冲突
 */
public class JobAssembleMqConsumeDelayRecorder extends MqConsumeDelayRecorder {

    public JobAssembleMqConsumeDelayRecorder(MeterRegistry meterRegistry,
                                             MqMetricsProperties mqMetricsProperties) {
        super(meterRegistry, mqMetricsProperties);
    }

    @Override
    protected MqMetricsProperties.ConsumerMetric getConsumeMetric() {
        return mqMetricsProperties.getConsumer().getJobAssemble();
    }
}
