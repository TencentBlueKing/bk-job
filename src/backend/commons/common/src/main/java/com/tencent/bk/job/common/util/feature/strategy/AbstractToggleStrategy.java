package com.tencent.bk.job.common.util.feature.strategy;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 特性开关开启策略基础实现抽象类
 */
@Slf4j
public abstract class AbstractToggleStrategy implements ToggleStrategy {

    protected final String id;
    protected final Map<String, String> initParams;

    /**
     * 初始化特性开关
     *
     * @param strategyId 策略ID
     * @param initParams 初始化参数
     */
    public AbstractToggleStrategy(String strategyId, Map<String, String> initParams) {
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
}
