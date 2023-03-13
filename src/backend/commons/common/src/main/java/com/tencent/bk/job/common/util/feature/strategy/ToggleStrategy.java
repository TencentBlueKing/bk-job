package com.tencent.bk.job.common.util.feature.strategy;

import com.tencent.bk.job.common.util.feature.FeatureExecutionContext;

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
     * 获取初始化参数
     */
    Map<String, String> getInitParams();

    /**
     * 判断是否开始特性
     *
     * @param featureId 特性ID
     * @param ctx       执行上下文
     * @return true: 特性开启,false: 特性关闭
     */
    boolean evaluate(String featureId, FeatureExecutionContext ctx);
}
