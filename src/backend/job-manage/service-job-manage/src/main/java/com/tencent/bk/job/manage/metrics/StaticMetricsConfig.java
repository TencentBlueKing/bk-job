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

package com.tencent.bk.job.manage.metrics;

import com.tencent.bk.job.common.cc.sdk.BizCmdbClient;
import com.tencent.bk.job.common.redis.util.RedisSlideWindowFlowController;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.ToDoubleFunction;

@Slf4j
@Component
public class StaticMetricsConfig {

    @Autowired
    public StaticMetricsConfig(
        MeterRegistry meterRegistry,
        RedisSlideWindowFlowController cmdbGlobalFlowController
    ) {
        // CMDB请求线程池大小
        ThreadPoolExecutor cmdbQueryThreadPool = BizCmdbClient.threadPoolExecutor;
        meterRegistry.gauge(
            MetricsConstants.NAME_CMDB_QUERY_POOL_SIZE,
            Collections.singletonList(Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_CMDB)),
            cmdbQueryThreadPool,
            ThreadPoolExecutor::getPoolSize
        );
        // CMDB请求线程池队列大小
        meterRegistry.gauge(
            MetricsConstants.NAME_CMDB_QUERY_QUEUE_SIZE,
            Collections.singletonList(Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_CMDB)),
            cmdbQueryThreadPool,
            threadPoolExecutor -> threadPoolExecutor.getQueue().size()
        );
        try {
            Map<String, Long> configMap = cmdbGlobalFlowController.getCurrentConfig();
            for (String key : configMap.keySet()) {
                // CMDB流控：配置
                meterRegistry.gauge(
                    MetricsConstants.NAME_CMDB_RESOURCE_LIMIT,
                    Arrays.asList(
                        Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_CMDB),
                        Tag.of(MetricsConstants.TAG_KEY_ASPECT, MetricsConstants.TAG_VALUE_ASPECT_FLOW_CONTROL),
                        Tag.of(MetricsConstants.TAG_KEY_RESOURCE_ID, key)
                    ),
                    cmdbGlobalFlowController,
                    new ToDoubleFunction<RedisSlideWindowFlowController>() {
                        @Override
                        public double applyAsDouble(RedisSlideWindowFlowController cmdbGlobalFlowController) {
                            Long value = cmdbGlobalFlowController.getCurrentConfig(key);
                            if (value == null) return -1;
                            return value;
                        }
                    }
                );
                // CMDB流控：当前速率
                meterRegistry.gauge(
                    MetricsConstants.NAME_CMDB_RESOURCE_RATE,
                    Arrays.asList(
                        Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_CMDB),
                        Tag.of(MetricsConstants.TAG_KEY_ASPECT, MetricsConstants.TAG_VALUE_ASPECT_FLOW_CONTROL),
                        Tag.of(MetricsConstants.TAG_KEY_RESOURCE_ID, key)
                    ),
                    cmdbGlobalFlowController,
                    new ToDoubleFunction<RedisSlideWindowFlowController>() {
                        @Override
                        public double applyAsDouble(RedisSlideWindowFlowController cmdbGlobalFlowController) {
                            Long value = cmdbGlobalFlowController.getCurrentRate(key);
                            if (value == null) return 0;
                            return value;
                        }
                    }
                );
            }
        } catch (Exception e) {
            log.error("Fail to init cmdbGlobalFlowController gauge", e);
        }
    }
}
