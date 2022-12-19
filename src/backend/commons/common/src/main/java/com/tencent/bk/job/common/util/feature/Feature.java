package com.tencent.bk.job.common.util.feature;

import lombok.Data;

/**
 * 特性
 */
@Data
public class Feature {
    private String id;
    private boolean enabled;
    private ToggleStrategy strategy;
}
