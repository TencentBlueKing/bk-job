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

package com.tencent.bk.job.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.web.server.WebFilter;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class ManagementContextSecurityConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(ManagementContextSecurityConfiguration.class);

    /**
     * 模拟管理子上下文：父上下文（主应用上下文）持有 WebFilterChainProxy，桥接 Bean 应被注册
     */
    @Test
    void bridgeFilterIsRegisteredWhenParentHasSecurityFilter() {
        try (AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext()) {
            parent.registerBean(WebFilterChainProxy.class,
                () -> new WebFilterChainProxy(Collections.emptyList()));
            parent.refresh();

            contextRunner.withParent(parent).run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasBean("managementSecurityWebFilter");
                assertThat(context.getBean("managementSecurityWebFilter", WebFilter.class))
                    .isSameAs(parent.getBean(WebFilterChainProxy.class));
            });
        }
    }

    /**
     * 模拟主应用上下文：其祖先（Spring Cloud bootstrap 上下文）不持有 WebFilterChainProxy，
     * 此时不得注册桥接 Bean，否则会导致主上下文启动失败
     */
    @Test
    void bridgeFilterIsSkippedWhenAncestorsHaveNoSecurityFilter() {
        try (AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext()) {
            parent.refresh();

            contextRunner.withParent(parent).run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean("managementSecurityWebFilter");
            });
        }
    }
}
