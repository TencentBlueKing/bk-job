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

import com.tencent.bk.job.consts.ApiPaths;
import com.tencent.bk.job.consts.WebPaths;
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

/**
 * Web登录拦截器的拦截范围护栏。
 *
 * <p>本测试用 {@link MappedInterceptor} + {@link PathPatternParser} 构造匹配器，与 Spring Boot 3
 * 运行时 {@code InterceptorRegistry} 实际使用的匹配方式一致（Boot 3 下MVC默认走 PathPatternParser
 * 而非 AntPathMatcher），否则护栏可能全绿而线上行为不同。</p>
 *
 * <p>Web会话域与API Key鉴权域必须严格隔离。</p>
 */
class WebAuthInterceptorPathTest {

    private static final HandlerInterceptor NOOP_INTERCEPTOR = new HandlerInterceptor() {
    };

    private MappedInterceptor buildMappedInterceptor(boolean webEnabled) {
        List<String> includePatterns = WebMvcConfig.resolveInterceptPaths(webEnabled);
        return new MappedInterceptor(
            StringUtils.toStringArray(includePatterns),
            StringUtils.toStringArray(WebPaths.EXCLUDE_PATHS),
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
    @ValueSource(strings = {"/web/issueStatus/list", "/web/issueStatus/delete", "/web/userInfo"})
    @DisplayName("Web页面会话接口被拦截")
    void shouldInterceptSessionApiPaths(String uri) {
        assertThat(matches(true, uri)).isTrue();
        assertThat(matches(false, uri)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/issueStatus",
        "/api/issueStatus",
        "/api/test",
        "/api/v1/test",
        "/mcp",
        "/mcp/message",
        "/actuator",
        "/actuator/health",
        "/actuator/health/liveness",
        "/checkServiceDependency",
        "/releaseStatus"
    })
    @DisplayName("API鉴权域、兼容旧路径与探针路径不被Web拦截")
    void shouldNotInterceptExistingPaths(String uri) {
        assertThat(matches(true, uri)).isFalse();
        assertThat(matches(false, uri)).isFalse();
    }

    @Test
    @DisplayName("Issue状态API不被Web拦截，Web列表和删除接口被拦截")
    void shouldSeparateApiAndWebIssueStatusPaths() {
        assertThat(matches(true, ApiPaths.ISSUE_STATUS)).isFalse();
        assertThat(matches(true, ApiPaths.LEGACY_ISSUE_STATUS)).isFalse();
        assertThat(matches(true, WebPaths.ISSUE_STATUS_LIST)).isTrue();
        assertThat(matches(true, WebPaths.ISSUE_STATUS_DELETE)).isTrue();
    }

    @Test
    @DisplayName("页面开关关闭时不拦截页面入口，但会话接口仍受保护")
    void shouldInterceptPagePathsOnlyWhenWebEnabled() {
        assertThat(matches(true, "/issue-status")).isTrue();
        assertThat(matches(false, "/issue-status")).isFalse();

        assertThat(matches(true, "/no-permission")).isTrue();
        assertThat(matches(false, "/no-permission")).isFalse();

        assertThat(matches(true, WebPaths.ISSUE_STATUS_LIST)).isTrue();
        assertThat(matches(false, WebPaths.ISSUE_STATUS_LIST)).isTrue();
    }

    @Test
    @DisplayName("提示页与前端静态资源不被拦截，避免未登录时连提示页都加载不出来")
    void shouldNotInterceptStaticResources() {
        assertThat(matches(true, WebPaths.STATIC_403_PAGE)).isFalse();
        assertThat(matches(true, WebPaths.STATIC_503_PAGE)).isFalse();
        assertThat(matches(true, "/js/app.js")).isFalse();
        assertThat(matches(true, "/css/app.css")).isFalse();
        assertThat(matches(true, "/favicon.ico")).isFalse();
    }
}
