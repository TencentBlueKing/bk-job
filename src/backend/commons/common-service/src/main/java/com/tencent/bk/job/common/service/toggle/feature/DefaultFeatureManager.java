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

package com.tencent.bk.job.common.service.toggle.feature;

import com.tencent.bk.job.common.metrics.CommonMetricTags;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.util.toggle.ToggleEvaluateContext;
import com.tencent.bk.job.common.util.toggle.ToggleStrategy;
import com.tencent.bk.job.common.util.toggle.ToggleStrategyContextParams;
import com.tencent.bk.job.common.util.toggle.feature.Feature;
import com.tencent.bk.job.common.util.toggle.feature.FeatureManager;
import com.tencent.bk.job.common.util.toggle.feature.FeatureStore;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class DefaultFeatureManager implements FeatureManager {
    /**
     * 度量指标 - 特性灰度检查总次数
     */
    private static final String METRIC_JOB_FEATURE_TOGGLE_TOTAL = "job.feature.toggle.total";
    /**
     * 度量指标 - 特性灰度命中总次数
     */
    private static final String METRIC_JOB_FEATURE_TOGGLE_HIT_TOTAL = "job.feature.toggle.hit.total";

    private final FeatureStore featureStore;
    private final MeterRegistry meterRegistry;

    public DefaultFeatureManager(FeatureStore featureStore, MeterRegistry meterRegistry) {
        this.featureStore = featureStore;
        this.meterRegistry = meterRegistry;
    }

    public boolean isFeatureEnabled(String featureId) {
        Feature feature = featureStore.getFeature(featureId);
        if (feature == null) {
            if (log.isDebugEnabled()) {
                log.debug("Feature: {} is not exist!", featureId);
            }
            return false;
        }
        return feature.isEnabled();
    }

    /**
     * 判断特性是否开启
     *
     * @param featureId 特性ID
     * @param ctx       特性运行上下文
     * @return 是否开启
     */
    public boolean checkFeature(String featureId, ToggleEvaluateContext ctx) {
        String resourceScope = extractResourceScope(ctx);
        recordFeatureToggleTotal(featureId, resourceScope);

        Feature feature = featureStore.getFeature(featureId);
        if (log.isDebugEnabled()) {
            log.debug("Check feature {}", featureId);
        }
        if (feature == null) {
            if (log.isDebugEnabled()) {
                log.debug("Feature: {} is not exist!", featureId);
            }
            return false;
        }
        if (!feature.isEnabled()) {
            if (log.isDebugEnabled()) {
                log.debug("Feature: {} is disabled!", featureId);
            }
            return false;
        }

        ToggleStrategy strategy = feature.getStrategy();
        if (strategy == null) {
            if (log.isDebugEnabled()) {
                log.debug("Feature:{} strategy is empty!", featureId);
            }
            // 如果没有配置特性开启策略，且enabled=true，判定为特性开启
            recordFeatureToggleHitTotal(featureId, resourceScope);
            return true;
        }

        boolean result = strategy.evaluate(featureId, ctx);
        if (log.isDebugEnabled()) {
            log.debug("Apply feature toggle strategy, featureId: {}, context: {}, result: {}", featureId, ctx, result);
        }
        if (result) {
            recordFeatureToggleHitTotal(featureId, resourceScope);
        }
        return result;
    }

    private String extractResourceScope(ToggleEvaluateContext ctx) {
        Object resourceScopeObj = ctx.getParam(ToggleStrategyContextParams.CTX_PARAM_RESOURCE_SCOPE);
        if (resourceScopeObj == null) {
            return "None";
        }
        ResourceScope resourceScope = (ResourceScope) resourceScopeObj;
        return resourceScope.getType() + ":" + resourceScope.getId();
    }

    private void recordFeatureToggleTotal(String featureId, String resourceScope) {
        meterRegistry.counter(METRIC_JOB_FEATURE_TOGGLE_TOTAL,
                Tags.of(CommonMetricTags.KEY_RESOURCE_SCOPE, resourceScope).and("feature", featureId))
            .increment();
    }

    private void recordFeatureToggleHitTotal(String featureId, String resourceScope) {
        meterRegistry.counter(METRIC_JOB_FEATURE_TOGGLE_HIT_TOTAL,
                Tags.of(CommonMetricTags.KEY_RESOURCE_SCOPE, resourceScope).and("feature", featureId))
            .increment();
    }

    @Override
    public List<Feature> listFeatures() {
        return featureStore.listFeatures();
    }
}
