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

package com.tencent.bk.job.common.service.toggle.prop;

import com.tencent.bk.job.common.service.toggle.strategy.ToggleStrategyParser;
import com.tencent.bk.job.common.service.toggle.strategy.config.ToggleStrategyConfig;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.common.util.toggle.ToggleStrategy;
import com.tencent.bk.job.common.util.toggle.prop.PropChangeEventListener;
import com.tencent.bk.job.common.util.toggle.prop.PropToggle;
import com.tencent.bk.job.common.util.toggle.prop.PropToggleStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 属性开关配置存储实现
 */
@Slf4j
public class InMemoryPropToggleStore implements PropToggleStore {

    /**
     * key: prop_name; value: PropToggle
     */
    private volatile Map<String, PropToggle> propToggles = new HashMap<>();

    private final Map<String, List<PropChangeEventListener>> propEventListeners = new HashMap<>();

    public InMemoryPropToggleStore(PropToggleProperties propToggleProperties) {
        log.info("Init InMemoryPropToggleStore, properties : {}", JsonUtils.toJson(propToggleProperties));
        loadAllPropToggles(propToggleProperties);
        log.info("Init InMemoryPropToggleStore successfully");
    }

    public PropToggle getPropToggle(String propName) {
        return propToggles.get(propName);
    }

    @Override
    public boolean handleConfigChange(Set<String> changedKeys, boolean ignoreException) {
        boolean loadResult = true;
        try {
            loadAllPropToggles(getRealTimePropToggleProperties());
        } catch (Throwable e) {
            log.warn("Load prop config error", e);
            loadResult = false;
            if (ignoreException) {
                log.warn("Ignore prop config load error");
            } else {
                throw e;
            }
        }
        Set<String> uniquePropNames = getUniqueChangedPropNames(changedKeys);
        log.info("Changed prop names : {}", uniquePropNames);
        uniquePropNames.forEach(propName -> {
            List<PropChangeEventListener> listeners = propEventListeners.get(propName);
            if (CollectionUtils.isNotEmpty(listeners)) {
                listeners.forEach(listener -> {
                    log.info("Invoke listener {}", listener.getClass().getSimpleName());
                    listener.handlePropChangeEvent(propName, getPropToggle(propName));
                });
            }
        });
        return loadResult;
    }

    private Set<String> getUniqueChangedPropNames(Set<String> changedKeys) {
        Set<String> uniquePropNames = new HashSet<>();

        for (String changeKey : changedKeys) {
            if (!changeKey.startsWith(PROP_KEY_PREFIX)) {
                log.error("Invalid key : {}", changeKey);
            }
            String[] parts = changeKey.split("\\.");
            // 格式 job.toggle.props.{propName}.others
            if (parts.length >= 4) {
                uniquePropNames.add(parts[3]);
            } else {
                log.error("Invalid key : {}", changeKey);
            }
        }

        return uniquePropNames;
    }


    /**
     * 获取 PropToggleProperties 实时配置
     */
    private PropToggleProperties getRealTimePropToggleProperties() {
        return ApplicationContextRegister.getBean(PropToggleProperties.class);
    }

    private void loadAllPropToggles(PropToggleProperties propToggleProperties) {
        synchronized (this) {
            log.info("Load prop toggle start ...");

            if (propToggleProperties.getProps() == null || propToggleProperties.getProps().isEmpty()) {
                log.info("Prop toggle config empty!");
                return;
            }

            log.info("Parse prop toggle config: {}", JsonUtils.toJson(propToggleProperties));

            Map<String, PropToggle> tmpPropToggles = new HashMap<>();
            propToggleProperties.getProps().forEach((propName, propConfig) -> {
                PropToggle propToggle = parsePropToggle(propName, propConfig);
                tmpPropToggles.put(propName, propToggle);
            });

            // 使用新的配置完全替换老的配置
            propToggles = tmpPropToggles;
            log.info("Load prop toggle config done! props: {}", propToggles);
        }
    }

    /**
     * 解析配置
     *
     * @param propName   属性名
     * @param propConfig 配置信息
     */
    private PropToggle parsePropToggle(String propName,
                                       PropToggleProperties.PropToggleConfig propConfig) {

        if (StringUtils.isBlank(propName)) {
            log.error("PropName is blank");
            throw new PropConfigParseException("PropName is blank");
        }
        PropToggle propToggle = new PropToggle();
        propToggle.setDefaultValue(propConfig.getDefaultValue());

        if (CollectionUtils.isEmpty(propConfig.getConditions())) {
            return propToggle;
        }

        List<PropToggle.PropValueCondition> valueConditions = new ArrayList<>(propConfig.getConditions().size());
        for (PropToggleProperties.ConditionConfig conditionConfig : propConfig.getConditions()) {
            PropToggle.PropValueCondition valueCondition = new PropToggle.PropValueCondition();
            valueCondition.setValue(conditionConfig.getValue());
            ToggleStrategyConfig strategyConfig = conditionConfig.getStrategy();
            if (strategyConfig != null) {
                ToggleStrategy toggleStrategy = ToggleStrategyParser.parseToggleStrategy(strategyConfig);
                valueCondition.setStrategy(toggleStrategy);
            }
            valueConditions.add(valueCondition);
        }
        propToggle.setConditions(valueConditions);

        return propToggle;
    }


    @Override
    public void addPropChangeEventListener(String propName, PropChangeEventListener propChangeEventListener) {
        propEventListeners.compute(propName, (prop, listeners) -> {
            if (listeners == null) {
                listeners = new ArrayList<>();
            }
            listeners.add(propChangeEventListener);
            return listeners;
        });
    }

}
