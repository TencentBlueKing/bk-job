package com.tencent.bk.job.common.util.feature;

import com.tencent.bk.job.common.model.dto.ResourceScope;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 根据资源范围灰度策略
 */
public class ResourceScopeToggleStrategy extends AbstractToggleStrategy {
    /**
     * 策略参数-资源范围
     */
    public static final String INIT_PARAM_RESOURCE_SCOPE = "resource_scope";
    /**
     * 上下文参数-资源范围
     */
    public static final String CTX_PARAM_RESOURCE_SCOPE = "resourceScope";
    /**
     * 特性开关开启策略ID
     */
    public static final String STRATEGY_ID = "ResourceScopeToggleStrategy";

    private static final Set<ResourceScope> allowedResourceScopes = new HashSet<>();

    public ResourceScopeToggleStrategy(String featureId, Map<String, String> initParams) {
        assertRequiredParameter(INIT_PARAM_RESOURCE_SCOPE);
        super.init(featureId, initParams);
        String allowedResourceScopeValues = initParams.get(INIT_PARAM_RESOURCE_SCOPE);
        if (StringUtils.isNotEmpty(allowedResourceScopeValues)) {
            String[] resourceScopes = allowedResourceScopeValues.split(",");
            for (String resourceScope : resourceScopes) {
                String[] scopeTypeAndId = resourceScope.split(":");
                allowedResourceScopes.add(new ResourceScope(scopeTypeAndId[0].trim(), scopeTypeAndId[1].trim()));
            }
        }
    }

    @Override
    public boolean evaluate(String featureId, FeatureExecutionContext ctx) {
        ResourceScope scope = (ResourceScope) ctx.getParam(CTX_PARAM_RESOURCE_SCOPE);
        return allowedResourceScopes.contains(scope);
    }
}
