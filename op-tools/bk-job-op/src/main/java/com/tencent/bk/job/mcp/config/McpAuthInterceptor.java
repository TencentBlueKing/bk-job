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

import com.tencent.bk.job.consts.CommonHeader;
import com.tencent.bk.job.utils.json.JsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * MCP认证拦截器
 */
@Slf4j
public class McpAuthInterceptor implements HandlerInterceptor {

    public static final String API_KEY_HEADER = CommonHeader.MCP_AUTH_HEADER_KEY;
    
    private final McpAuthProperties mcpAuthProperties;
    
    public McpAuthInterceptor(McpAuthProperties mcpAuthProperties) {
        this.mcpAuthProperties = mcpAuthProperties;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果未启用认证，直接放行
        if (!mcpAuthProperties.isEnabled()) {
            log.debug("MCP authentication is disabled, skip validation");
            return true;
        }
        
        // 获取请求头中的API Key
        String requestApiKey = request.getHeader(API_KEY_HEADER);
        
        // 验证API Key
        if (!StringUtils.hasText(requestApiKey)) {
            log.warn("MCP request without API Key, uri={}, remoteAddr={}", 
                    request.getRequestURI(), request.getRemoteAddr());
            sendUnauthorizedResponse(response, "Missing API Key in request header: " + API_KEY_HEADER);
            return false;
        }

        if (!requestApiKey.equals(mcpAuthProperties.getApiKey())) {
            log.warn("MCP request with invalid API Key, uri={}, remoteAddr={}", 
                    request.getRequestURI(), request.getRemoteAddr());
            sendUnauthorizedResponse(response, "Invalid API Key");
            return false;
        }
        
        log.debug("MCP authentication passed, uri={}", request.getRequestURI());
        return true;
    }
    
    /**
     * 返回未授权响应
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("message", message);
        errorResponse.put("status", HttpStatus.UNAUTHORIZED.value());
        
        response.getWriter().write(JsonUtils.toJson(errorResponse));
        response.getWriter().flush();
    }
}
