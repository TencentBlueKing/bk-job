package com.tencent.bk.job.common.util.feature.strategy;

import com.tencent.bk.job.common.util.feature.FeatureExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.helpers.MessageFormatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 特性开关开启策略基础实现抽象类
 */
@Slf4j
public abstract class AbstractToggleStrategy implements ToggleStrategy {

    protected final String id;
    protected final String description;
    protected List<ToggleStrategy> compositeStrategies = null;
    protected final Map<String, String> initParams;

    /**
     * 初始化特性开关
     *
     * @param strategyId          策略ID
     * @param description         策略说明
     * @param compositeStrategies 子策略
     * @param initParams          初始化参数
     */
    public AbstractToggleStrategy(String strategyId,
                                  String description,
                                  List<ToggleStrategy> compositeStrategies,
                                  Map<String, String> initParams) {
        this.id = strategyId;
        this.description = description;
        this.compositeStrategies = compositeStrategies;
        if (initParams != null) {
            this.initParams = initParams;
        } else {
            this.initParams = new HashMap<>();
        }
    }

    /**
     * 初始化特性开关
     *
     * @param strategyId  策略ID
     * @param description 策略说明
     * @param initParams  初始化参数
     */
    public AbstractToggleStrategy(String strategyId,
                                  String description,
                                  Map<String, String> initParams) {
        this.id = strategyId;
        this.description = description;
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

    public void assertRequiredInitParam(String paramName) {
        if (!initParams.containsKey(paramName)) {
            String msg = MessageFormatter.format(
                "Parameter {} is required for this ToggleStrategy", paramName).getMessage();
            log.error(msg);
            throw new FeatureConfigParseException(msg);
        }
    }

    public void assertRequiredAtLeastOneStrategy() {
        if (CollectionUtils.isEmpty(this.compositeStrategies)) {
            String msg = "Required at least one strategy for this ToggleStrategy";
            log.error(msg);
            throw new FeatureConfigParseException(msg);
        }
    }

    public void assertRequiredContextParam(FeatureExecutionContext context, String paramName) {
        if (context.getParam(paramName) == null) {
            String msg = MessageFormatter.format(
                "Context param {} is required for evaluate", paramName).getMessage();
            log.error(msg);
            throw new FeatureConfigParseException(msg);
        }
    }
}
