package com.tencent.bk.job.common.util.feature;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

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

    private int weight;

    public WeightToggleStrategy(String featureId, Map<String, String> initParams) {
        super(featureId, initParams);
        assertRequiredParameter(INIT_PARAM_WEIGHT);
        String weight = initParams.get(INIT_PARAM_WEIGHT);
    }

    private int computeWeight(String weightStrValue) {
        String weightValue = weightStrValue.trim();
        if (StringUtils.isBlank(weightStrValue)) {
            return 0;
        }
        try {
            int weight = Integer.parseInt(weightValue);
            if (weight < 0 || weight > 100) {
                log.error("Weight should be set between 0 and 100, value: {}", weight);
            }
            return weight;
        } catch (NumberFormatException e) {
            log.error("Invalid weight value: {}, not a valid number", weightValue);
            return 0;
        }
    }

    @Override
    public boolean evaluate(String featureId, FeatureExecutionContext ctx) {
        return true;
    }
}
