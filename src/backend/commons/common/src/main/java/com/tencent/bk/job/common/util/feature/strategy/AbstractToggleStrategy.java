package com.tencent.bk.job.common.util.feature.strategy;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 特性开关开启策略基础实现抽象类
 */
@Slf4j
public abstract class AbstractToggleStrategy implements ToggleStrategy {

    private final String id;
    private final Map<String, String> initParams;
    private final String featureId;

    /**
     * 初始化特性开关
     *
     * @param featureId  特性ID
     * @param strategyId 策略ID
     * @param initParams 初始化参数
     */
    public AbstractToggleStrategy(String featureId, String strategyId, Map<String, String> initParams) {
        this.featureId = featureId;
        this.id = strategyId;
        if (initParams != null) {
            this.initParams = initParams;
        } else {
            this.initParams = new HashMap<>();
        }
    }

    @Override
    public String getId() {
        return id;
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
            log.error(msg);
            throw new FeatureConfigParseException(msg);
        }
    }

    public void assertRequiredAtLeastOneParameter(String... paramNames) {
        boolean anyMatch = Arrays.stream(paramNames).anyMatch(initParams::containsKey);
        if (!anyMatch) {
            String msg = MessageFormatter.format(
                "Required at least one parameter({}) for this ToggleStrategy", paramNames).getMessage();
            log.error(msg);
            throw new FeatureConfigParseException(msg);
        }
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AbstractToggleStrategy.class.getSimpleName() + "[", "]")
            .add("id='" + id + "'")
            .add("featureId='" + featureId + "'")
            .add("initParams=" + initParams)
            .toString();
    }
}
