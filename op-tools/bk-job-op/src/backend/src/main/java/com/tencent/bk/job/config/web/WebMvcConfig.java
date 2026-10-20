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

package com.tencent.bk.job.config.web;

import com.tencent.bk.job.consts.InterceptorOrder;
import com.tencent.bk.job.service.login.BkTicketLoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * Web MVC 配置 - OP Web页面登录态认证与SPA路由回落。
 *
 * <p>Spring会合并所有WebMvcConfigurer的回调，本类与已有的 ApiWebMvcConfig、McpWebMvcConfig 并存，
 * 三者的拦截路径互不相交。</p>
 */
@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final String WEB_API_PATTERN = "/web/**";

    private static final String PAGE_ISSUE_STATUS = "/issue-status";

    private static final String PAGE_NO_PERMISSION = "/no-permission";

    private static final List<String> PAGE_PATHS = List.of(
        "/", "/index.html", PAGE_ISSUE_STATUS, PAGE_NO_PERMISSION
    );

    private final WebAuthProperties webAuthProperties;

    private final BkTicketLoginService bkTicketLoginService;

    @Autowired
    public WebMvcConfig(WebAuthProperties webAuthProperties, BkTicketLoginService bkTicketLoginService) {
        this.webAuthProperties = webAuthProperties;
        this.bkTicketLoginService = bkTicketLoginService;
    }

    /**
     * 计算Web登录拦截器的拦截路径。
     *
     * <p>会话接口无条件拦截：关掉页面入口不等于把接口裸奔出去。页面入口只在页面开关打开时才存在，
     * 因而也只在打开时拦截。</p>
     */
    public static List<String> resolveInterceptPaths(boolean webEnabled) {
        List<String> patterns = new ArrayList<>(List.of(WEB_API_PATTERN));
        if (webEnabled) {
            patterns.addAll(PAGE_PATHS);
        }
        return patterns;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> patterns = resolveInterceptPaths(webAuthProperties.isEnabled());
        log.info("Registering OP web authentication interceptor, enabled={}, patterns={}",
            webAuthProperties.isEnabled(), patterns);

        registry.addInterceptor(new WebAuthInterceptor(webAuthProperties, bkTicketLoginService))
            .addPathPatterns(patterns)
            .order(InterceptorOrder.Auth.WEB_AUTH);

        log.info("OP web authentication interceptor registered successfully");
    }

    /**
     * 前端history路由回落。显式枚举路由而不用通配：通配会把404和拼错的接口路径也吞成index.html，
     * 返回200 HTML，排障非常痛苦
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        if (!webAuthProperties.isEnabled()) {
            return;
        }
        registry.addViewController(PAGE_ISSUE_STATUS).setViewName("forward:/index.html");
        registry.addViewController(PAGE_NO_PERMISSION).setViewName("forward:/index.html");
    }
}
