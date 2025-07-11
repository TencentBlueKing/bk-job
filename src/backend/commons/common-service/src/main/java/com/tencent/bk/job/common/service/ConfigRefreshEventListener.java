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

package com.tencent.bk.job.common.service;

import com.tencent.bk.job.common.service.quota.ResourceQuotaStore;
import com.tencent.bk.job.common.util.feature.FeatureStore;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;

import java.util.Set;
import java.util.StringJoiner;

/**
 * 配置刷新监听
 */
@Slf4j
public class ConfigRefreshEventListener {

    private final MeterRegistry meterRegistry;

    private final FeatureStore featureStore;

    private final ResourceQuotaStore resourceQuotaStore;

    private static final String METRIC_JOB_CONFIG_REFRESH_FAIL_TOTAL = "job_config_refresh_fail_total";
    private static final String METRIC_TAG_CONFIG_NAME = "config_name";


    public ConfigRefreshEventListener(MeterRegistry meterRegistry,
                                      FeatureStore featureStore,
                                      ResourceQuotaStore resourceQuotaStore) {
        this.meterRegistry = meterRegistry;
        this.featureStore = featureStore;
        this.resourceQuotaStore = resourceQuotaStore;
        log.info("Init ConfigRefreshEventListener");
    }

    /**
     * 监听并处理Spring cloud 配置更新事件(通过/actuator/refresh 和 /actuator/busrefresh endpoint 触发)
     *
     * @param event 配置更新事件
     */
    @EventListener
    public void onEvent(EnvironmentChangeEvent event) {
        if (log.isInfoEnabled()) {
            log.info("Handle EnvironmentChangeEvent, event: {}", printEnvironmentChangeEvent(event));
        }
        reload(event.getKeys());
    }

    private String printEnvironmentChangeEvent(EnvironmentChangeEvent event) {
        return new StringJoiner(", ", EnvironmentChangeEvent.class.getSimpleName() + "[", "]")
            .add("source=" + event.getSource())
            .add("timestamp=" + event.getTimestamp())
            .add("keys=" + event.getKeys())
            .toString();
    }

    /**
     * 重载配置
     */
    private void reload(Set<String> changedKeys) {
        if (CollectionUtils.isEmpty(changedKeys)) {
            return;
        }
        if (changedKeys.stream().anyMatch(changedKey -> changedKey.startsWith("job.features."))) {
            boolean handleResult = featureStore.handleConfigChange();
            if (!handleResult) {
                meterRegistry.counter(
                        METRIC_JOB_CONFIG_REFRESH_FAIL_TOTAL,
                        Tags.of(METRIC_TAG_CONFIG_NAME, "job.feature"))
                    .increment();
            }
        }

        if (changedKeys.stream().anyMatch(changedKey -> changedKey.startsWith("job.resourceQuotaLimit."))) {
            boolean handleResult = resourceQuotaStore.handleConfigChange();
            if (!handleResult) {
                meterRegistry.counter(
                        METRIC_JOB_CONFIG_REFRESH_FAIL_TOTAL,
                        Tags.of(METRIC_TAG_CONFIG_NAME, "job.resourceQuotaLimit"))
                    .increment();
            }
        }
    }
}
