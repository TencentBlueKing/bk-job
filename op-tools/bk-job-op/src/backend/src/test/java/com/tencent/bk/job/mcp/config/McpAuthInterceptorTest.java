/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 */

package com.tencent.bk.job.mcp.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class McpAuthInterceptorTest {

    @Test
    @DisplayName("启用认证但Key为空时返回401并拒绝请求")
    void shouldFailCloseWhenApiKeyIsNotConfigured() throws Exception {
        McpAuthProperties properties = new McpAuthProperties();
        properties.setEnabled(true);
        properties.setApiKey(" ");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        request.addHeader(McpAuthInterceptor.API_KEY_HEADER, "synthetic-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = new McpAuthInterceptor(properties).preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("显式关闭认证时保持既有放行语义")
    void shouldAllowWhenAuthenticationIsExplicitlyDisabled() throws Exception {
        McpAuthProperties properties = new McpAuthProperties();
        properties.setEnabled(false);

        boolean allowed = new McpAuthInterceptor(properties).preHandle(
            new MockHttpServletRequest("POST", "/mcp"),
            new MockHttpServletResponse(),
            new Object()
        );

        assertThat(allowed).isTrue();
    }
}
