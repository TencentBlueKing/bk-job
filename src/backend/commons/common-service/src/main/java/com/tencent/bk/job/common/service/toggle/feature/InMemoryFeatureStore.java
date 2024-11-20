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

import com.tencent.bk.job.common.service.toggle.feature.config.FeatureConfig;
import com.tencent.bk.job.common.service.toggle.feature.config.FeatureToggleProperties;
import com.tencent.bk.job.common.service.toggle.strategy.ToggleStrategyParseException;
import com.tencent.bk.job.common.service.toggle.strategy.ToggleStrategyParser;
import com.tencent.bk.job.common.service.toggle.strategy.config.ToggleStrategyConfig;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.common.util.toggle.ToggleStrategy;
import com.tencent.bk.job.common.util.toggle.feature.Feature;
import com.tencent.bk.job.common.util.toggle.feature.FeatureStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 特性开关配置存储实现
 */
@Slf4j
public class InMemoryFeatureStore implements FeatureStore {

    /**
     * key: featureId; value: Feature
     */
    private volatile Map<String, Feature> features = new HashMap<>();

    public InMemoryFeatureStore(FeatureToggleProperties featureToggleProperties) {
        log.info("Init InMemoryFeatureStore, properties : {}", JsonUtils.toJson(featureToggleProperties));
        loadAllFeatures(featureToggleProperties);
        log.info("Init InMemoryFeatureStore successfully");
    }

    @Override
    public Feature getFeature(String featureId) {
        return features.get(featureId);
    }

    @Override
    public boolean handleConfigChange(Set<String> changedKeys, boolean ignoreException) {
        boolean loadResult = true;
        try {
            loadAllFeatures(getRealTimeFeatureToggleProperties());
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

    /**
     * 获取 FeatureToggleProperties 实时配置
     */
    private FeatureToggleProperties getRealTimeFeatureToggleProperties() {
        return ApplicationContextRegister.getBean(FeatureToggleProperties.class);
    }

    private void loadAllFeatures(FeatureToggleProperties featureToggleProperties) {
        synchronized (this) {
            log.info("Load feature toggle start ...");

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
        }
    }

    /**
     * 解析特性配置
     *
     * @param featureId     特性 ID
     * @param featureConfig 特性配置信息
     * @return 解析之后的特性
     * @throws ToggleStrategyParseException 如果解析策略报错，抛出异常
     * @throws FeatureConfigParseException  如果解析报错，抛出异常
     */
    private Feature parseFeatureConfig(
        String featureId,
        FeatureConfig featureConfig)
        throws ToggleStrategyParseException, FeatureConfigParseException {

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
                ToggleStrategy toggleStrategy = ToggleStrategyParser.parseToggleStrategy(strategyConfig);
                if (toggleStrategy != null) {
                    feature.setStrategy(toggleStrategy);
                }
            }
        }
        return feature;
    }

    @Override
    public List<Feature> listFeatures() {
        return new ArrayList<>(features.values());
    }

}
