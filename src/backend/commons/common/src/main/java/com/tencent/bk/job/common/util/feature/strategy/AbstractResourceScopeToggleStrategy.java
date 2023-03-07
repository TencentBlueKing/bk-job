package com.tencent.bk.job.common.util.feature.strategy;

import com.tencent.bk.job.common.model.dto.ResourceScope;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 根据资源范围灰度策略-基础实现
 */
public abstract class AbstractResourceScopeToggleStrategy extends AbstractToggleStrategy {
    /**
     * 策略初始化参数-资源范围
     */
    public static final String INIT_PARAM_RESOURCE_SCOPE_LIST = "resourceScopeList";
    /**
     * 上下文参数-资源范围
     */
    public static final String CTX_PARAM_RESOURCE_SCOPE = "resourceScope";

    private static final Set<ResourceScope> resourceScopes = new HashSet<>();

    public AbstractResourceScopeToggleStrategy(String featureId, Map<String, String> initParams) {
        super(featureId, initParams);
        assertRequiredParameter(INIT_PARAM_RESOURCE_SCOPE_LIST);

        String resourceScopesValue = initParams.get(INIT_PARAM_RESOURCE_SCOPE_LIST);
        if (StringUtils.isNotEmpty(resourceScopesValue)) {
            String[] resourceScopes = resourceScopesValue.split(",");
            for (String resourceScope : resourceScopes) {
                String[] scopeTypeAndId = resourceScope.split(":");
                AbstractResourceScopeToggleStrategy.resourceScopes.add(new ResourceScope(scopeTypeAndId[0].trim(),
                    scopeTypeAndId[1].trim()));
            }
        }
    }

    protected Set<ResourceScope> getResourceScopes() {
        return resourceScopes;
    }
}
