package com.tencent.bk.job.common.service.config;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 特性启用策略配置
 */
@Data
public class ToggleStrategyConfig {
    /**
     * 策略ID
     */
    private String id;
    /**
     * 策略说明
     */
    private String description;
    /**
     * 组合策略
     */
    private List<ToggleStrategyConfig> strategies;

    /**
     * 策略初始化参数
     */
    private Map<String, String> params;
}
