package com.tencent.bk.job.common.util.feature;

import java.util.Map;

/**
 * 根据权重灰度策略
 */
public class WeightToggleStrategy extends AbstractToggleStrategy {
    /**
     * 策略参数-权重
     */
    public static final String INIT_PARAM_WEIGHT = "weight";
    /**
     * 特性开关开启策略ID
     */
    public static final String STRATEGY_ID = "WeightToggleStrategy";

    public WeightToggleStrategy(String featureId, Map<String, String> initParams) {
        super(featureId, initParams);
        assertRequiredParameter(INIT_PARAM_WEIGHT);
        String weight = initParams.get(INIT_PARAM_WEIGHT);
    }

    @Override
    public boolean evaluate(String featureId, FeatureExecutionContext ctx) {
        return true;
    }
}
