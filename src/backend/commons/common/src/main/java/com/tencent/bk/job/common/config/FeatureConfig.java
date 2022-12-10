package com.tencent.bk.job.common.config;

import lombok.Data;

/**
 * 特性配置
 */
@Data
public class FeatureConfig {
    /**
     * 特性ID
     */
    private String id;
    /**
     * 是否启用特性
     */
    private boolean enabled;
    /**
     * 特性启用策略；必须enabled=true,策略才会生效
     */
    private ToggleStrategyConfig strategy;
}
