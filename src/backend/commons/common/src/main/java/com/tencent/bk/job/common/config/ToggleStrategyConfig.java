package com.tencent.bk.job.common.config;

import lombok.Data;

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
     * 策略初始化参数
     */
    private Map<String, String> params;
}
