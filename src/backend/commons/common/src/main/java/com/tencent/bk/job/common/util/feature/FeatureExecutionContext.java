package com.tencent.bk.job.common.util.feature;

import java.util.Map;
import java.util.StringJoiner;

/**
 * 特性运行上下文
 */
public class FeatureExecutionContext {
    /**
     * 运行时参数
     */
    private final Map<String, Object> params;

    public FeatureExecutionContext(Map<String, Object> params) {
        this.params = params;
    }

    public Object getParam(String paramName) {
        if (params == null) {
            return null;
        }
        return this.params.get(paramName);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FeatureExecutionContext.class.getSimpleName() + "[", "]")
            .add("params=" + params)
            .toString();
    }
}
