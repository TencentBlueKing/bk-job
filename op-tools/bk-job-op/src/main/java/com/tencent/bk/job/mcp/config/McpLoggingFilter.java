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

import com.tencent.bk.job.utils.log.LogUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * MCP日志过滤器
 * 用于记录/mcp开头的请求和响应日志
 */
@Slf4j
@Component
public class McpLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String uri = request.getRequestURI();
        
        // 只拦截/mcp开头的请求
        if (!uri.startsWith("/mcp")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 包装请求和响应，以便可以多次读取body
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);

            logRequest(requestWrapper);
            logResponse(requestWrapper, responseWrapper);
        } catch (Exception e) {
            log.error("[MCP Filter Exception] uri: {}, error: {}", uri, e.getMessage(), e);
            logRequest(requestWrapper);
            throw e;
        } finally {
            // 将缓存的响应内容写回到原始响应中
            responseWrapper.copyBodyToResponse();
        }
    }

    /**
     * 记录请求日志
     */
    private void logRequest(ContentCachingRequestWrapper request) {
        try {
            String uri = request.getRequestURI();
            String params = getRequestParams(request);
            String body = getRequestBody(request);

            log.info("[request] uri: {}, params: {}, body: {}", uri, params, body);
        } catch (Exception e) {
            log.warn("Failed to log request", e);
        }
    }

    /**
     * 记录响应日志
     */
    private void logResponse(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response) {
        try {
            String uri = request.getRequestURI();
            String contentType = response.getContentType();
            int status = response.getStatus();
            
            // 对于 SSE 流式响应，只记录状态信息
            if (contentType != null && contentType.contains("text/event-stream")) {
                log.info("[response] uri: {}, status: {}, contentType: {} (SSE stream, body not captured)", 
                        uri, status, contentType);
            } else {
                // 对于普通响应，记录完整响应体
                String responseBody = getResponseBody(response);
                log.info("[response] uri: {}, status: {}, body: {}", uri, status, responseBody);
            }
        } catch (Exception e) {
            log.warn("Failed to log response", e);
        }
    }

    /**
     * 获取请求参数
     */
    private String getRequestParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String paramValue = request.getParameter(paramName);
            params.put(paramName, paramValue);
        }
        
        return params.isEmpty() ? "{}" : params.toString();
    }

    /**
     * 获取请求体
     */
    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] buf = request.getContentAsByteArray();
        if (buf.length > 0) {
            try {
                String body = new String(buf, request.getCharacterEncoding());
                // 限制body长度，避免日志过长
                return LogUtils.truncate(body, 1000);
            } catch (UnsupportedEncodingException e) {
                return "[Unable to parse body]";
            }
        }
        return "{}";
    }

    /**
     * 获取响应体
     */
    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] buf = response.getContentAsByteArray();
        if (buf.length > 0) {
            try {
                String body = new String(buf, response.getCharacterEncoding());
                // 限制body长度，避免日志过长
                return LogUtils.truncate(body, 1000);
            } catch (UnsupportedEncodingException e) {
                return "[Unable to parse body]";
            }
        }
        return "{}";
    }
}
