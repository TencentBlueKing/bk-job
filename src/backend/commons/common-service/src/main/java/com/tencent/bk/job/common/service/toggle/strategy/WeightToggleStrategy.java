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

package com.tencent.bk.job.common.service.toggle.strategy;

import com.tencent.bk.job.common.util.toggle.ToggleEvaluateContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;
import java.util.StringJoiner;

/**
 * 根据权重灰度策略
 */
@Slf4j
public class WeightToggleStrategy extends AbstractToggleStrategy {
    /**
     * 策略参数-权重
     */
    public static final String INIT_PARAM_WEIGHT = "weight";
    /**
     * 特性开关开启策略ID
     */
    public static final String STRATEGY_ID = "WeightToggleStrategy";

    private final int weight;

    private final Random RANDOM = new SecureRandom();

    public WeightToggleStrategy(Map<String, String> initParams) {
        super(STRATEGY_ID, initParams);
        assertRequiredInitParam(INIT_PARAM_WEIGHT);
        String weightStrValue = initParams.get(INIT_PARAM_WEIGHT);
        this.weight = computeWeight(weightStrValue);
    }

    private int computeWeight(String weightStrValue) {
        String weightValue = weightStrValue.trim();
        if (StringUtils.isBlank(weightStrValue)) {
            log.error("Weight is empty!");
            throw new ToggleStrategyParseException("Weight is empty!");
        }
        try {
            int weight = Integer.parseInt(weightValue);
            if (weight < 0 || weight > 100) {
                log.error("Weight should be set between 0 and 100, value: {}", weight);
                throw new ToggleStrategyParseException("Weight should be set between 0 and 100");
            }
            return weight;
        } catch (NumberFormatException e) {
            log.error("Invalid weight value: {}, not a valid number", weightValue);
            throw new ToggleStrategyParseException("Weight should be a number");
        }
    }

    @Override
    public boolean evaluate(String toggleName, ToggleEvaluateContext ctx) {
        if (weight == 0) {
            return false;
        } else if (weight == 100) {
            return true;
        } else {
            int random = RANDOM.nextInt(100) + 1;
            return random <= weight;
        }
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", WeightToggleStrategy.class.getSimpleName() + "[", "]")
            .add("id='" + id + "'")
            .add("initParams=" + initParams)
            .add("weight=" + weight)
            .toString();
    }
}
