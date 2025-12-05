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

package com.tencent.bk.job.mcp.config;

import com.tencent.bk.job.consts.InterceptorOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置 - MCP认证
 */
@Slf4j
@Configuration
public class McpWebMvcConfig implements WebMvcConfigurer {
    
    private final McpAuthProperties mcpAuthProperties;
    
    @Autowired
    public McpWebMvcConfig(McpAuthProperties mcpAuthProperties) {
        this.mcpAuthProperties = mcpAuthProperties;
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("Registering MCP authentication interceptor, enabled={}", mcpAuthProperties.isEnabled());

        // Spring AI MCP Server 使用 SSE (Server-Sent Events) 协议，默认端点为 /sse
        registry.addInterceptor(new McpAuthInterceptor(mcpAuthProperties))
                .addPathPatterns("/sse", "/sse/**")
                .order(InterceptorOrder.Auth.MCP_AUTH);
        
        log.info("MCP authentication interceptor registered successfully");
    }
}
