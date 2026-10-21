/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 */

package com.tencent.bk.job.config.web;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WebAuthInterceptorPathTest {

    private static final HandlerInterceptor NOOP_INTERCEPTOR = new HandlerInterceptor() {
    };

    private MappedInterceptor buildMappedInterceptor(boolean webEnabled) {
        List<String> includePatterns = WebMvcConfig.resolveInterceptPaths(webEnabled);
        return new MappedInterceptor(
            StringUtils.toStringArray(includePatterns),
            null,
            NOOP_INTERCEPTOR,
            PathPatternParser.defaultInstance
        );
    }

    private boolean matches(boolean webEnabled, String uri) {
        HttpServletRequest request = new MockHttpServletRequest("GET", uri);
        ServletRequestPathUtils.parseAndCache(request);
        return buildMappedInterceptor(webEnabled).matches(request);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/web/issueStatus/list", "/web/issueStatus/delete", "/web/pullRequest/list", "/web/userInfo", "/web/future"
    })
    @DisplayName("所有Web接口均被登录态拦截")
    void shouldInterceptWebApiPaths(String uri) {
        assertThat(matches(true, uri)).isTrue();
        assertThat(matches(false, uri)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/issueStatus",
        "/api/releaseStatus",
        "/api/repoTag/listTags",
        "/public/checkServiceDependency",
        "/mcp",
        "/mcp/message",
        "/actuator/health",
        "/actuator/env"
    })
    @DisplayName("API、公开接口、MCP和Actuator不被Web拦截")
    void shouldNotInterceptOtherNamespaces(String uri) {
        assertThat(matches(true, uri)).isFalse();
        assertThat(matches(false, uri)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/issueStatus", "/checkServiceDependency", "/releaseStatus"})
    @DisplayName("已移除的旧路径不属于Web拦截范围")
    void shouldNotInterceptRemovedPaths(String uri) {
        assertThat(matches(true, uri)).isFalse();
        assertThat(matches(false, uri)).isFalse();
    }

    @Test
    @DisplayName("页面开关只控制SPA入口，不影响Web接口")
    void shouldInterceptPagePathsOnlyWhenWebEnabled() {
        assertThat(matches(true, "/issue-status")).isTrue();
        assertThat(matches(false, "/issue-status")).isFalse();
        assertThat(matches(true, "/pull-request")).isTrue();
        assertThat(matches(false, "/pull-request")).isFalse();
        assertThat(matches(true, "/no-permission")).isTrue();
        assertThat(matches(false, "/no-permission")).isFalse();
        assertThat(matches(false, "/web/issueStatus/list")).isTrue();
    }

    @Test
    @DisplayName("静态资源不属于登录态拦截范围")
    void shouldNotInterceptStaticResources() {
        assertThat(matches(true, "/403.html")).isFalse();
        assertThat(matches(true, "/503-login-unconfigured.html")).isFalse();
        assertThat(matches(true, "/js/app.js")).isFalse();
        assertThat(matches(true, "/css/app.css")).isFalse();
        assertThat(matches(true, "/favicon.ico")).isFalse();
    }

    @Test
    @DisplayName("拦截器只注册Web命名空间和明确页面入口")
    void shouldUseNamespaceAndExactPagePatterns() {
        assertThat(WebMvcConfig.resolveInterceptPaths(true))
            .containsExactly("/web/**", "/", "/index.html", "/issue-status", "/pull-request", "/no-permission");
        assertThat(WebMvcConfig.resolveInterceptPaths(false)).containsExactly("/web/**");
    }
}
