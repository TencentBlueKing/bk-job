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

import com.tencent.bk.job.common.gse.constants.GseConstants;
import com.tencent.bk.job.execute.monitor.ExecuteMetricNames;
import com.tencent.bk.job.execute.monitor.ExecuteMetricTags;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsAutoConfig {
    @Bean
    public MeterFilter distributionMeterFilter() {
        return new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                String metricName = id.getName();
                if (metricName.startsWith("http.server.request") || metricName.startsWith("esb.api")) {
                    return DistributionStatisticConfig.builder().percentilesHistogram(true)
                        // [10ms,3s]
                        .minimumExpectedValue(10_000_000.0).maximumExpectedValue(3_000_000_000.0)
                        .build().merge(config);
                } else if (metricName.startsWith(GseConstants.GSE_API_METRICS_NAME_PREFIX)) {
                    return DistributionStatisticConfig.builder().percentilesHistogram(true)
                        // [10ms,1s]
                        .minimumExpectedValue(10_000_000.0).maximumExpectedValue(1_000_000_000.0)
                        .build().merge(config);
                } else if (metricName.startsWith(ExecuteMetricNames.RESULT_HANDLE_TASK_SCHEDULE_PREFIX)) {
                    return DistributionStatisticConfig.builder().percentilesHistogram(true)
                        // [10ms,5s]
                        .minimumExpectedValue(10_000_000.0).maximumExpectedValue(5_000_000_000.0)
                        .build().merge(config);
                } else {
                    return config;
                }
            }
        };
    }

    @Bean
    public MeterFilter denyIgnoreHttpRequestMeterFilter() {
        return new MeterFilter() {
            @Override
            public MeterFilterReply accept(Meter.Id id) {
                String metricName = id.getName();
                if (metricName.startsWith("http.server.request")) {
                    String ignore = id.getTag(ExecuteMetricTags.IGNORE_TAG);
                    if (StringUtils.isNotEmpty(ignore) && ExecuteMetricTags.BOOLEAN_TRUE_TAG_VALUE.equals(ignore)) {
                        return MeterFilterReply.DENY;
                    } else {
                        return MeterFilterReply.NEUTRAL;
                    }
                } else {
                    return MeterFilterReply.NEUTRAL;
                }
            }
        };
    }
}
