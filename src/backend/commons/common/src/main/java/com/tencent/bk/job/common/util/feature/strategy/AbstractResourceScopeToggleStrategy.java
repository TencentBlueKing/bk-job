package com.tencent.bk.job.common.util.feature.strategy;

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 根据资源范围灰度策略-基础实现
 */
@Slf4j
public abstract class AbstractResourceScopeToggleStrategy extends AbstractToggleStrategy {
    /**
     * 策略初始化参数-资源范围
     */
    public static final String INIT_PARAM_RESOURCE_SCOPE_LIST = "resourceScopeList";
    /**
     * 上下文参数-资源范围
     */
    public static final String CTX_PARAM_RESOURCE_SCOPE = "resourceScope";

    protected final Set<ResourceScope> resourceScopes = new HashSet<>();

    public AbstractResourceScopeToggleStrategy(String strategyId, Map<String, String> initParams) {
        super(strategyId, initParams);
        assertRequiredParameter(INIT_PARAM_RESOURCE_SCOPE_LIST);

        String resourceScopesValue = initParams.get(INIT_PARAM_RESOURCE_SCOPE_LIST);
        if (StringUtils.isNotEmpty(resourceScopesValue)) {
            String[] resourceScopeArray = resourceScopesValue.split(",");
            if (resourceScopeArray.length == 0) {
                String msg = MessageFormatter.format(
                    "Parameter {} is invalid, value: {}",
                    INIT_PARAM_RESOURCE_SCOPE_LIST, resourceScopesValue).getMessage();
                log.error(msg);
                throw new FeatureConfigParseException(msg);
            }
            for (String resourceScope : resourceScopeArray) {
                this.resourceScopes.add(parseResourceScope(resourceScope));
            }
        }
    }

    private ResourceScope parseResourceScope(String resourceScope) {
        String[] scopeTypeAndId = resourceScope.split(":");
        if (scopeTypeAndId.length != 2) {
            String msg = MessageFormatter.format(
                "Parameter {} is invalid. Invalid resource scope: {}",
                INIT_PARAM_RESOURCE_SCOPE_LIST, resourceScope).getMessage();
            log.error(msg);
            throw new FeatureConfigParseException(msg);
        }
        String scopeType = scopeTypeAndId[0].trim();
        if (!ResourceScopeTypeEnum.isValid(scopeType)) {
            String msg = MessageFormatter.format(
                "Parameter {} is invalid. Invalid resource scope: {}",
                INIT_PARAM_RESOURCE_SCOPE_LIST, resourceScope).getMessage();
            log.error(msg);
            throw new FeatureConfigParseException(msg);
        }

        String scopeId = scopeTypeAndId[1].trim();
        if (!isValidScopeId(scopeId)) {
            String msg = MessageFormatter.format(
                "Parameter {} is invalid. Invalid resource scope: {}",
                INIT_PARAM_RESOURCE_SCOPE_LIST, resourceScope).getMessage();
            log.error(msg);
            throw new FeatureConfigParseException(msg);
        }
        return new ResourceScope(scopeType, scopeId);
    }

    private boolean isValidScopeId(String scopeId) {
        if (StringUtils.isEmpty(scopeId)) {
            return false;
        }

        try {
            Long.parseLong(scopeId);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }
}
