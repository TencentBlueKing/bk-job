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

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * MCP全局异常处理器
 * 用于捕获MCP端点的异常并返回友好的错误信息给AI Agent
 */
@Slf4j
@RestControllerAdvice
public class McpGlobalExceptionHandler {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 通用异常处理
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex, HttpServletRequest request) {
        String uri = request.getRequestURI();
        
        // 只处理MCP相关的请求
        if (!uri.startsWith("/mcp") && !uri.startsWith("/sse")) {
            // 对于非MCP请求，重新抛出异常让其他处理器处理
            throw new RuntimeException(ex);
        }

        // 记录详细的异常日志
        String errorMsg = String.format("[MCP Exception] uri: %s, error: %s", uri, ex.getMessage());
        log.error(errorMsg, ex);

        // 构建友好的错误响应
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", true);
        errorResponse.put("message", ex.getMessage() != null ? ex.getMessage() : "Internal Server Error");
        errorResponse.put("exceptionType", ex.getClass().getSimpleName());
        errorResponse.put("path", uri);
        errorResponse.put("timestamp", LocalDateTime.now().format(DATE_TIME_FORMATTER));

        // 返回500状态码和详细错误信息
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        String uri = request.getRequestURI();
        
        // 只处理MCP相关的请求
        if (!uri.startsWith("/mcp") && !uri.startsWith("/sse")) {
            throw new RuntimeException(ex);
        }

        String errorMsg = String.format("[MCP Validation Error] uri: %s, error: %s", uri, ex.getMessage());
        log.warn(errorMsg, ex);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", true);
        errorResponse.put("message", "参数校验失败: " + ex.getMessage());
        errorResponse.put("exceptionType", "ValidationError");
        errorResponse.put("path", uri);
        errorResponse.put("timestamp", LocalDateTime.now().format(DATE_TIME_FORMATTER));

        // 返回400状态码
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

}
