package com.tencent.bk.job.common.util.feature;

import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.util.feature.strategy.ToggleStrategyContextParams;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 特性运行上下文
 */
public class FeatureExecutionContext {
    /**
     * 运行时参数
     */
    private final Map<String, Object> params = new HashMap<>();

    public static FeatureExecutionContext builder() {
        return new FeatureExecutionContext();
    }

    private FeatureExecutionContext() {
    }

    public Object getParam(String paramName) {
        return this.params.get(paramName);
    }

    public FeatureExecutionContext addContextParam(String paramName, Object value) {
        this.params.put(paramName, value);
        return this;
    }

    public FeatureExecutionContext addResourceScopeContextParam(ResourceScope resourceScope) {
        params.put(ToggleStrategyContextParams.CTX_PARAM_RESOURCE_SCOPE, resourceScope);
        return this;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FeatureExecutionContext.class.getSimpleName() + "[", "]")
            .add("params=" + params)
            .toString();
    }
}
