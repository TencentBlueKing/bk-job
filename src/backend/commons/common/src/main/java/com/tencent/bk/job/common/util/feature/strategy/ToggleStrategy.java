package com.tencent.bk.job.common.util.feature.strategy;

import com.tencent.bk.job.common.util.feature.FeatureExecutionContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 特性开关开启策略
 */
public interface ToggleStrategy {
    /**
     * 获取特性开关开启策略ID
     */
    String getId();

    /**
     * 获取策略说明
     */
    default String getDescription() {
        return "";
    }

    /**
     * 获取初始化参数
     */
    Map<String, String> getInitParams();

    /**
     * 是否是复合策略
     */
    default boolean isCompositeStrategy() {
        return false;
    }

    /**
     * 获取复合策略
     */
    default List<ToggleStrategy> getCompositeToggleStrategies() {
        return Collections.emptyList();
    }

    /**
     * 判断是否开启特性
     *
     * @param featureId 特性ID
     * @param ctx       执行上下文
     * @return true: 特性开启,false: 特性关闭
     */
    boolean evaluate(String featureId, FeatureExecutionContext ctx);
}
