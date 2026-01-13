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

package com.tencent.bk.job.common.iam.config;

import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Objects;

/**
 * 开启IAM重试的条件，满足任意一项即可：
 * 1.external-system.retry.iam.enabled配置项明确指定开启；
 * 2.external-system.retry.iam.enabled配置项未配置，但全局配置项external-system.retry.global.enabled指定开启。
 */
@SuppressWarnings("unused")
public class IamRetryCondition extends AnyNestedCondition {
    public IamRetryCondition() {
        super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnProperty(name = "external-system.retry.iam.enabled", havingValue = "true")
    static class IamRetryEnabledCondition {

    }

    @Conditional(IamUseGlobalRetryCondition.class)
    static class GlobalRetryEnabledCondition {

    }

    static class IamUseGlobalRetryCondition extends AllNestedConditions {
        IamUseGlobalRetryCondition() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @ConditionalOnProperty(name = "external-system.retry.global.enabled", havingValue = "true")
        static class GlobalRetryEnabledCondition {

        }

        @Conditional(IamRetryNotFalseCondition.class)
        static class IamRetryNotDisabledCondition {

        }

        static class IamRetryNotFalseCondition implements Condition {
            @Override
            public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
                String enabled = context.getEnvironment().getProperty("external-system.retry.iam.enabled");
                // 配置项不存在或值不为 "false" 时返回 true
                return enabled == null || !Objects.equals(enabled, "false");
            }
        }
    }
}
