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

package com.tencent.bk.job.common.util.feature.strategy;

import com.tencent.bk.job.common.util.feature.FeatureToggle;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ToggleStrategyRegister {
    private static final Map<String, ToggleStrategyInitial> toggleStrategyInitialMap = new HashMap<>();

    static {
        // 初始化通用的灰度策略
        toggleStrategyInitialMap.put(ResourceScopeWhiteListToggleStrategy.STRATEGY_ID,
            strategyConfig -> new ResourceScopeBlackListToggleStrategy(strategyConfig.getDescription(),
                strategyConfig.getParams()));
        toggleStrategyInitialMap.put(ResourceScopeBlackListToggleStrategy.STRATEGY_ID,
            strategyConfig -> new ResourceScopeBlackListToggleStrategy(strategyConfig.getDescription(),
                strategyConfig.getParams()));
        toggleStrategyInitialMap.put(WeightToggleStrategy.STRATEGY_ID,
            strategyConfig -> new WeightToggleStrategy(strategyConfig.getDescription(),
                strategyConfig.getParams()));
        toggleStrategyInitialMap.put(AllMatchToggleStrategy.STRATEGY_ID,
            strategyConfig -> new AllMatchToggleStrategy(
                strategyConfig.getDescription(),
                strategyConfig.getStrategies()
                    .stream()
                    .map(compositeStrategyConfig -> toggleStrategyInitialMap.get(strategyConfig.getId()).build(compositeStrategyConfig))
                    .collect(Collectors.toList()),
                strategyConfig.getParams()));
    }

    private static ToggleStrategy parseToggleStrategy(ToggleStrategyConfig strategyConfig) {
        String strategyId = strategyConfig.getId();
        ToggleStrategy toggleStrategy = null;
        ToggleStrategyInitial strategyInitial = ToggleStrategyRegister.getToggleStrategyInitial(strategyId);
        toggleStrategy = strategyInitial.build(strategyConfig);
        switch (strategyId) {
            case ResourceScopeWhiteListToggleStrategy.STRATEGY_ID:
                toggleStrategy =
                break;
            case ResourceScopeBlackListToggleStrategy.STRATEGY_ID:
                toggleStrategy = new ResourceScopeBlackListToggleStrategy(strategyConfig.getDescription(),
                    strategyConfig.getParams());
                break;
            case WeightToggleStrategy.STRATEGY_ID:
                toggleStrategy = new WeightToggleStrategy(strategyConfig.getDescription(),
                    strategyConfig.getParams());
                break;
            case AllMatchToggleStrategy.STRATEGY_ID:
                toggleStrategy = new AllMatchToggleStrategy(
                    strategyId,
                    strategyConfig.getStrategies()
                        .stream()
                        .map(FeatureToggle::parseToggleStrategy)
                        .collect(Collectors.toList()),
                    strategyConfig.getParams());
                break;
            case AnyMatchToggleStrategy.STRATEGY_ID:
                toggleStrategy = new AnyMatchToggleStrategy(
                    strategyId,
                    strategyConfig.getStrategies()
                        .stream()
                        .map(FeatureToggle::parseToggleStrategy)
                        .collect(Collectors.toList()),
                    strategyConfig.getParams());
                break;
            default:
                log.error("Unsupported toggle strategy: {} , ignore it!", strategyId);
                break;
        }
        return toggleStrategy;
    }

    public static void addToggleStrategy(String strategyId, ToggleStrategyInitial toggleStrategyInitial) {
        toggleStrategyInitialMap.put(strategyId, toggleStrategyInitial);
    }

    public static ToggleStrategyInitial getToggleStrategyInitial(String strategyId) {
        return toggleStrategyInitialMap.get(strategyId);
    }


}
