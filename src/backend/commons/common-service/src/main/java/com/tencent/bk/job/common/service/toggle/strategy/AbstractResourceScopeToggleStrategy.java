/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.common.service.toggle.strategy;

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.util.toggle.ToggleEvaluateContext;
import com.tencent.bk.job.common.util.toggle.ToggleStrategyContextParams;
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

    protected final Set<ResourceScope> resourceScopes = new HashSet<>();

    public AbstractResourceScopeToggleStrategy(String strategyId, Map<String, String> initParams) {
        super(strategyId, initParams);
        assertRequiredInitParam(INIT_PARAM_RESOURCE_SCOPE_LIST);

        String resourceScopesValue = initParams.get(INIT_PARAM_RESOURCE_SCOPE_LIST);
        if (StringUtils.isNotEmpty(resourceScopesValue)) {
            String[] resourceScopeArray = resourceScopesValue.split(",");
            if (resourceScopeArray.length == 0) {
                String msg = MessageFormatter.format(
                    "Parameter {} is invalid, value: {}",
                    INIT_PARAM_RESOURCE_SCOPE_LIST, resourceScopesValue).getMessage();
                log.error(msg);
                throw new ToggleStrategyParseException(msg);
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
            throw new ToggleStrategyParseException(msg);
        }
        String scopeType = scopeTypeAndId[0].trim();
        if (!ResourceScopeTypeEnum.isValid(scopeType)) {
            String msg = MessageFormatter.format(
                "Parameter {} is invalid. Invalid resource scope: {}",
                INIT_PARAM_RESOURCE_SCOPE_LIST, resourceScope).getMessage();
            log.error(msg);
            throw new ToggleStrategyParseException(msg);
        }

        String scopeId = scopeTypeAndId[1].trim();
        if (!isValidScopeId(scopeId)) {
            String msg = MessageFormatter.format(
                "Parameter {} is invalid. Invalid resource scope: {}",
                INIT_PARAM_RESOURCE_SCOPE_LIST, resourceScope).getMessage();
            log.error(msg);
            throw new ToggleStrategyParseException(msg);
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

    /**
     * 判断是否命中配置的管理空间列表
     *
     * @param scope 管理空间
     */
    protected boolean hitResourceScopeList(ResourceScope scope) {
        ResourceScope checkScope = scope;
        if (!scope.getClass().equals(ResourceScope.class)) {
            // 需要转换为 ResourceScope 类型，避免后面 contains() 判断因为 class 不同出现预期之外的结果
            checkScope = new ResourceScope(scope.getType(), scope.getId());
        }
        return this.resourceScopes.contains(checkScope);
    }

    protected boolean checkFeatureExecuteContext(ToggleEvaluateContext context) {
        return checkRequiredContextParam(context, ToggleStrategyContextParams.CTX_PARAM_RESOURCE_SCOPE);
    }
}
