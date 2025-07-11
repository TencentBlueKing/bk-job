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

package com.tencent.bk.job.common.service.feature;

import com.tencent.bk.job.common.service.feature.config.FeatureConfig;
import com.tencent.bk.job.common.service.feature.config.FeatureToggleProperties;
import com.tencent.bk.job.common.service.feature.config.ToggleStrategyConfig;
import com.tencent.bk.job.common.service.feature.strategy.AllMatchToggleStrategy;
import com.tencent.bk.job.common.service.feature.strategy.AnyMatchToggleStrategy;
import com.tencent.bk.job.common.service.feature.strategy.FeatureConfigParseException;
import com.tencent.bk.job.common.service.feature.strategy.JobInstanceAttrToggleStrategy;
import com.tencent.bk.job.common.service.feature.strategy.ResourceScopeBlackListToggleStrategy;
import com.tencent.bk.job.common.service.feature.strategy.ResourceScopeWhiteListToggleStrategy;
import com.tencent.bk.job.common.service.feature.strategy.WeightToggleStrategy;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.feature.Feature;
import com.tencent.bk.job.common.util.feature.FeatureStore;
import com.tencent.bk.job.common.util.feature.ToggleStrategy;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 特性开关配置存储实现
 */
@Slf4j
public class InMemoryFeatureStore implements FeatureStore {

    /**
     * key: featureId; value: Feature
     */
    private volatile Map<String, Feature> features = new HashMap<>();
    /**
     * 是否初始化
     */
    private volatile boolean isInitial = false;

    @Override
    public Feature getFeature(String featureId) {
        if (!isInitial) {
            synchronized (this) {
                if (!isInitial) {
                    load(true);
                }
            }
        }
        return features.get(featureId);
    }

    @Override
    public boolean load(boolean ignoreException) {
        boolean loadResult = true;
        try {
            loadInternal();
        } catch (Throwable e) {
            log.warn("Load feature config error", e);
            loadResult = false;
            if (ignoreException) {
                log.warn("Ignore feature config load error");
            } else {
                throw e;
            }
        }
        return loadResult;
    }

    private void loadInternal() {
        synchronized (this) {
            log.info("Load feature toggle start ...");
            FeatureToggleProperties featureToggleProperties =
                ApplicationContextRegister.getBean(FeatureToggleProperties.class);

            if (featureToggleProperties.getFeatures() == null || featureToggleProperties.getFeatures().isEmpty()) {
                log.info("Feature toggle config empty!");
                return;
            }

            log.info("Parse feature toggle config: {}", JsonUtils.toJson(featureToggleProperties));

            Map<String, Feature> tmpFeatures = new HashMap<>();
            featureToggleProperties.getFeatures().forEach((featureId, featureConfig) -> {
                Feature feature = parseFeatureConfig(featureId, featureConfig);
                tmpFeatures.put(featureId, feature);
            });

            // 使用新的配置完全替换老的配置
            features = tmpFeatures;
            log.info("Load feature toggle config done! features: {}", features);
            isInitial = true;
        }
    }

    /**
     * 解析特性配置
     *
     * @param featureId     特性 ID
     * @param featureConfig 特性配置信息
     * @return 解析之后的特性
     * @throws FeatureConfigParseException 如果解析报错，抛出异常
     */
    private Feature parseFeatureConfig(String featureId,
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
                ToggleStrategy toggleStrategy = parseToggleStrategy(strategyConfig);
                if (toggleStrategy != null) {
                    feature.setStrategy(toggleStrategy);
                }
            }
        }
        return feature;
    }

    private ToggleStrategy parseToggleStrategy(ToggleStrategyConfig strategyConfig) {
        String strategyId = strategyConfig.getId();
        ToggleStrategy toggleStrategy;
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
            case JobInstanceAttrToggleStrategy.STRATEGY_ID:
                toggleStrategy = new JobInstanceAttrToggleStrategy(strategyConfig.getParams());
                break;
            case AllMatchToggleStrategy.STRATEGY_ID:
                toggleStrategy = new AllMatchToggleStrategy(
                    strategyConfig.getStrategies()
                        .stream()
                        .map(this::parseToggleStrategy)
                        .collect(Collectors.toList()),
                    strategyConfig.getParams());
                break;
            case AnyMatchToggleStrategy.STRATEGY_ID:
                toggleStrategy = new AnyMatchToggleStrategy(
                    strategyConfig.getStrategies()
                        .stream()
                        .map(this::parseToggleStrategy)
                        .collect(Collectors.toList()),
                    strategyConfig.getParams());
                break;
            default:
                log.error("Unsupported toggle strategy: {}", strategyId);
                throw new FeatureConfigParseException("Unsupported toggle strategy " + strategyId);
        }
        return toggleStrategy;
    }

    @Override
    public List<Feature> listFeatures() {
        if (!isInitial) {
            synchronized (this) {
                if (!isInitial) {
                    load(true);
                }
            }
        }
        return new ArrayList<>(features.values());
    }

}
