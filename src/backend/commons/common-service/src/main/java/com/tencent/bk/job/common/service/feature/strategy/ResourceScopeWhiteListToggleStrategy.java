/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.common.service.feature.strategy;

import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.util.feature.FeatureExecutionContext;
import com.tencent.bk.job.common.util.feature.ToggleStrategyContextParams;

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
        boolean isValidContext = checkFeatureExecuteContext(ctx);
        if (!isValidContext) {
            return false;
        }
        ResourceScope scope = (ResourceScope) ctx.getParam(ToggleStrategyContextParams.CTX_PARAM_RESOURCE_SCOPE);
        return hitResourceScopeList(scope);
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
