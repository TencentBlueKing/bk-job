package com.tencent.bk.job.common.util.feature.strategy;

import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.util.feature.FeatureExecutionContext;

import java.util.Map;
import java.util.StringJoiner;

/**
 * 根据资源范围白名单灰度策略
 */
public class ResourceScopeWhiteListToggleStrategy extends AbstractResourceScopeToggleStrategy {
    /**
     * 特性开关开启策略ID
     */
    public static final String STRATEGY_ID = "ResourceScopeWhiteListToggleStrategy";

    public ResourceScopeWhiteListToggleStrategy(Map<String, String> initParams) {
        super(STRATEGY_ID, initParams);
    }

    @Override
    public boolean evaluate(String featureId, FeatureExecutionContext ctx) {
        ResourceScope scope = (ResourceScope) ctx.getParam(CTX_PARAM_RESOURCE_SCOPE);
        return this.resourceScopes.contains(scope);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ResourceScopeWhiteListToggleStrategy.class.getSimpleName() + "[", "]")
            .add("id='" + id + "'")
            .add("initParams=" + initParams)
            .add("resourceScopes=" + resourceScopes)
            .toString();
    }
}
