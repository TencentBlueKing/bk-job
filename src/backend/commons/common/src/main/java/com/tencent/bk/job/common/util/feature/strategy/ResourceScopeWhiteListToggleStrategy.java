package com.tencent.bk.job.common.util.feature.strategy;

import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.util.feature.FeatureExecutionContext;

import java.util.Map;

/**
 * 根据资源范围白名单灰度策略
 */
public class ResourceScopeWhiteListToggleStrategy extends AbstractResourceScopeToggleStrategy {
    /**
     * 特性开关开启策略ID
     */
    public static final String STRATEGY_ID = "ResourceScopeWhiteListToggleStrategy";

    public ResourceScopeWhiteListToggleStrategy(String featureId, Map<String, String> initParams) {
        super(featureId, initParams);
    }

    @Override
    public boolean evaluate(String featureId, FeatureExecutionContext ctx) {
        ResourceScope scope = (ResourceScope) ctx.getParam(CTX_PARAM_RESOURCE_SCOPE);
        return getResourceScopes().contains(scope);
    }
}
