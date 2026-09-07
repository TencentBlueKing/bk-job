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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.web.server.WebFilter;

/**
 * 管理端口（management.server.port）上的 Actuator 鉴权。
 * <p>
 * 管理端口与服务端口不同时，Spring Boot 会为管理端点创建独立的子上下文。Servlet 应用由
 * ServletManagementChildContextConfiguration 自动把父上下文的 Security 过滤器桥接过去，
 * 但 Reactive 应用的 ReactiveManagementChildContextConfiguration 没有对应处理，其 HttpHandler
 * 只收集子上下文自身的 WebFilter，导致 {@link ActuatorSecurityConfig} 无法保护管理端口。
 * <p>
 * 这里显式把父上下文的 WebFilterChainProxy 注册为子上下文的 WebFilter，补齐这一环。
 * <p>
 * SearchStrategy.ANCESTORS 的条件必不可少：本类位于被组件扫描的包下，主上下文也会扫描到它。
 * 主上下文自身持有 WebFilterChainProxy，其祖先（Spring Cloud bootstrap 上下文）并不持有，
 * 因此条件在主上下文不成立、仅在管理子上下文成立，避免重复注册导致启动失败。
 * 该做法与 Boot 的 ServletManagementChildContextConfiguration 保持一致。
 */
@Slf4j
@ManagementContextConfiguration(value = ManagementContextType.CHILD, proxyBeanMethods = false)
@ConditionalOnBean(value = WebFilterChainProxy.class, search = SearchStrategy.ANCESTORS)
public class ManagementContextSecurityConfiguration {

    /**
     * 与 Spring Security 在主上下文中的过滤器顺序保持一致，确保鉴权先于端点处理执行
     */
    private static final int SECURITY_WEB_FILTER_CHAIN_ORDER = -100;

    @Bean
    @Order(SECURITY_WEB_FILTER_CHAIN_ORDER)
    public WebFilter managementSecurityWebFilter(HierarchicalBeanFactory beanFactory) {
        BeanFactory parent = beanFactory.getParentBeanFactory();
        if (parent == null) {
            // 宁可启动失败也不要让管理端点在无鉴权的情况下对外提供服务
            throw new IllegalStateException(
                "Cannot secure actuator on management port: parent bean factory is not available");
        }
        WebFilterChainProxy securityWebFilter = parent.getBean(WebFilterChainProxy.class);
        log.info("Actuator security filter is bridged into management context");
        return securityWebFilter;
    }
}
