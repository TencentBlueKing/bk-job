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

package com.tencent.bk.job.common.util.feature;

import com.tencent.bk.job.common.config.FeatureConfig;
import com.tencent.bk.job.common.config.FeatureToggleConfig;
import com.tencent.bk.job.common.config.ToggleStrategyConfig;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.feature.strategy.FeatureConfigParseException;
import com.tencent.bk.job.common.util.feature.strategy.ResourceScopeBlackListToggleStrategy;
import com.tencent.bk.job.common.util.feature.strategy.ResourceScopeWhiteListToggleStrategy;
import com.tencent.bk.job.common.util.feature.strategy.ToggleStrategy;
import com.tencent.bk.job.common.util.feature.strategy.WeightToggleStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;

import java.util.HashMap;
import java.util.Map;

/**
 * 特性开关
 */
@Slf4j
public class FeatureToggle {

    /**
     * key: featureId; value: Feature
     */
    private static volatile Map<String, Feature> features = new HashMap<>();
    private static volatile boolean isInitial = false;

    public static void reload() {
        synchronized (FeatureToggle.class) {
            load();
            isInitial = true;
        }
    }

    public static void load() {
        log.info("Load feature toggle start ...");
        FeatureToggleConfig featureToggleConfig = ApplicationContextRegister.getBean(FeatureToggleConfig.class);

        if (featureToggleConfig.getFeatures() == null || featureToggleConfig.getFeatures().isEmpty()) {
            log.info("Feature toggle config empty!");
            return;
        }

        Map<String, Feature> tmpFeatures = new HashMap<>();
        featureToggleConfig.getFeatures().forEach((featureId, featureConfig) -> {
            try {
                Feature feature = parseFeatureConfig(featureId, featureConfig);
                tmpFeatures.put(featureId, feature);
            } catch (Throwable e) {
                String msg = MessageFormatter.format(
                    "Load feature toggle config fail, skip update feature toggle config! featureId: {}, " +
                        "featureConfig: {}", featureId, featureConfig).getMessage();
                log.error(msg, e);
                if (features.get(featureId) != null) {
                    // 如果加载失败，那么使用原有的特性配置
                    tmpFeatures.put(featureId, features.get(featureId));
                }
            }
        });

        // 使用新的配置完全替换老的配置
        features = tmpFeatures;
        log.info("Load feature toggle config done! features: {}", features);
    }

    private static Feature parseFeatureConfig(String featureId,
                                              FeatureConfig featureConfig) throws FeatureConfigParseException {
        if (StringUtils.isBlank(featureId)) {
            log.error("FeatureId is blank");
            throw new FeatureConfigParseException("FeatureId is blank");
        }
        Feature feature = new Feature();
        feature.setId(featureId);
        feature.setEnabled(featureConfig.isEnabled());

        if (featureConfig.isEnabled()) {
            ToggleStrategyConfig strategyConfig = featureConfig.getStrategy();
            if (strategyConfig != null) {
                String strategyId = strategyConfig.getId();
                ToggleStrategy toggleStrategy = null;
                switch (strategyId) {
                    case ResourceScopeWhiteListToggleStrategy.STRATEGY_ID:
                        toggleStrategy = new ResourceScopeWhiteListToggleStrategy(strategyConfig.getParams());
                        break;
                    case ResourceScopeBlackListToggleStrategy.STRATEGY_ID:
                        toggleStrategy = new ResourceScopeBlackListToggleStrategy(strategyConfig.getParams());
                        break;
                    case WeightToggleStrategy.STRATEGY_ID:
                        toggleStrategy = new WeightToggleStrategy(strategyConfig.getParams());
                        break;
                    default:
                        log.error("Unsupported toggle strategy: {} for feature: {}, ignore it!", strategyId,
                            featureId);
                        break;
                }
                if (toggleStrategy != null) {
                    feature.setStrategy(toggleStrategy);
                }
            }
        }
        return feature;
    }


    /**
     * 判断特性是否开启
     *
     * @param featureId 特性ID
     * @param ctx       特性运行上下文
     * @return 是否开启
     */
    public static boolean checkFeature(String featureId, FeatureExecutionContext ctx) {
        if (!isInitial) {
            synchronized (FeatureConfig.class) {
                if (!isInitial) {
                    load();
                    isInitial = true;
                }
            }
        }

        Feature feature = features.get(featureId);
        if (log.isDebugEnabled()) {
            log.debug("Check feature, featureId: {}, config: {}", featureId, feature);
        }
        if (feature == null) {
            log.debug("Feature: {} is not exist!", featureId);
            return false;
        }
        if (!feature.isEnabled()) {
            log.debug("Feature: {} is disabled!", featureId);
            return false;
        }

        ToggleStrategy strategy = feature.getStrategy();
        if (strategy == null) {
            log.debug("Feature:{} strategy is empty!", featureId);
            // 如果没有配置特性开启策略，且enabled=true，判定为特性开启
            return true;
        }

        boolean result = strategy.evaluate(featureId, ctx);
        if (log.isDebugEnabled()) {
            log.debug("Apply feature toggle strategy, featureId: {}, strategy: {}, context: {}, result: {}",
                featureId, strategy, ctx, result);
        }
        return result;
    }
}
