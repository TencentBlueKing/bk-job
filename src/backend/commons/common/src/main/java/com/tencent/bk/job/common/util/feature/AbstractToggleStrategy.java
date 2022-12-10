package com.tencent.bk.job.common.util.feature;

import org.slf4j.helpers.MessageFormatter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 特性开关开启策略基础实现抽象类
 */
public abstract class AbstractToggleStrategy implements ToggleStrategy {

    private Map<String, String> initParams = new HashMap<>();
    private String featureId;



    @Override
    public void init(String featureId, Map<String, String> initParams) {
        this.featureId = featureId;
        this.initParams = initParams;
    }

    @Override
    public Map<String, String> getInitParams() {
        return this.initParams;
    }

    @Override
    public String getFeatureId() {
        return featureId;
    }

    public void assertRequiredParameter(String paramName) {
        if (!initParams.containsKey(paramName)) {
            String msg = MessageFormatter.format(
                "Parameter {} is required for this ToggleStrategy", paramName).getMessage();
            throw new IllegalArgumentException(msg);
        }
    }

    public void assertRequiredAtLeastOneParameter(String... paramNames) {
        boolean anyMatch = Arrays.stream(paramNames).anyMatch(paramName -> initParams.containsKey(paramName));
        if (!anyMatch) {
            String msg = MessageFormatter.format(
                "Required at least one parameter({}) for this ToggleStrategy", paramNames).getMessage();
            throw new IllegalArgumentException(msg);
        }
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AbstractToggleStrategy.class.getSimpleName() + "[", "]")
            .add("initParams=" + initParams)
            .toString();
    }
}
