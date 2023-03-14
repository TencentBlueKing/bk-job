package com.tencent.bk.job.common.util.feature;

import com.tencent.bk.job.common.util.feature.strategy.ToggleStrategy;
import lombok.Data;

/**
 * 特性
 */
@Data
public class Feature {
    /**
     * 特性ID
     *
     * @see FeatureIdConstants
     */
    private String id;
    /**
     * 是否启用特性
     */
    private boolean enabled;
    /**
     * 特性启用灰度策略
     */
    private ToggleStrategy strategy;


}
