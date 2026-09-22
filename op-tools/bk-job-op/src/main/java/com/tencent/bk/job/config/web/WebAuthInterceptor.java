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

import com.tencent.bk.job.consts.WebPaths;
import com.tencent.bk.job.service.login.BkTicketLoginService;
import com.tencent.bk.job.service.login.LoginServiceUnavailableException;
import com.tencent.bk.job.utils.json.JsonUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * OP Web页面登录态与白名单拦截器。
 *
 * <p>拦截范围见 {@link WebPaths}，只拦精确路径，不使用任何通配兜底：存量的
 * {@code GET/POST /issueStatus} 被外部脚本裸调，必须保持免鉴权。</p>
 */
@Slf4j
public class WebAuthInterceptor implements HandlerInterceptor {

    /**
     * 校验通过后写入request的用户名属性，供 {@code GET /web/userInfo} 读取
     */
    public static final String ATTR_USERNAME = "job.op.web.username";

    private final WebAuthProperties webAuthProperties;

    private final BkTicketLoginService bkTicketLoginService;

    public WebAuthInterceptor(WebAuthProperties webAuthProperties, BkTicketLoginService bkTicketLoginService) {
        this.webAuthProperties = webAuthProperties;
        this.bkTicketLoginService = bkTicketLoginService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String ticket = readTicket(request);
        if (StringUtils.isBlank(ticket)) {
            handleUnauthenticated(request, response, "Login ticket is absent");
            return false;
        }

        String username;
        try {
            username = bkTicketLoginService.getUsername(ticket);
        } catch (LoginServiceUnavailableException e) {
            // fail-close：登录服务不可用时拒绝访问，绝不放行
            log.error("Login service is unavailable, uri={}", request.getRequestURI(), e);
            handleServiceUnavailable(request, response, "Login service is unavailable");
            return false;
        }
        if (StringUtils.isBlank(username)) {
            handleUnauthenticated(request, response, "Login ticket is invalid or expired");
            return false;
        }

        if (!webAuthProperties.isUserAllowed(username)) {
            log.warn("User {} is not in the whitelist, uri={}", username, request.getRequestURI());
            handleForbidden(request, response, username);
            return false;
        }

        request.setAttribute(ATTR_USERNAME, username);
        return true;
    }

    private String readTicket(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        String cookieName = webAuthProperties.getAuth().getLogin().getTokenName();
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 区分页面请求与接口请求：接口请求必须返回状态码+JSON，
     * 若对XHR返回302，浏览器会静默跟随到登录页HTML，前端只能看到解析失败，无法提示用户重新登录
     */
    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (WebPaths.SESSION_API_PATHS.contains(uri)) {
            return true;
        }
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            return true;
        }
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        return accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE);
    }

    private void handleUnauthenticated(HttpServletRequest request,
                                       HttpServletResponse response,
                                       String message) throws IOException, ServletException {
        String loginUrl = buildLoginUrl(request);
        if (StringUtils.isBlank(loginUrl)) {
            // 登录页未配置时绝不能重定向到空地址：那会跳回当前页形成无限重定向，用户完全看不出原因
            handleServiceUnavailable(request, response, "OP web login page is not configured");
            return;
        }
        if (isApiRequest(request)) {
            Map<String, Object> body = new HashMap<>();
            body.put("loginUrl", loginUrl);
            writeJson(response, HttpStatus.UNAUTHORIZED, message, body);
            return;
        }
        response.sendRedirect(loginUrl);
    }

    private void handleForbidden(HttpServletRequest request,
                                 HttpServletResponse response,
                                 String username) throws IOException, ServletException {
        if (isApiRequest(request)) {
            writeJson(response, HttpStatus.FORBIDDEN,
                "User " + username + " has no permission, please contact the administrator to join the whitelist",
                null);
            return;
        }
        response.setStatus(HttpStatus.FORBIDDEN.value());
        // 用静态HTML而不是前端路由，保证前端资源还没加载时也能显示
        request.getRequestDispatcher(WebPaths.STATIC_403_PAGE).forward(request, response);
    }

    private void handleServiceUnavailable(HttpServletRequest request,
                                          HttpServletResponse response,
                                          String message) throws IOException, ServletException {
        if (isApiRequest(request)) {
            writeJson(response, HttpStatus.SERVICE_UNAVAILABLE, message, null);
            return;
        }
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        request.getRequestDispatcher(WebPaths.STATIC_503_PAGE).forward(request, response);
    }

    private String buildLoginUrl(HttpServletRequest request) {
        WebAuthProperties.Login login = webAuthProperties.getAuth().getLogin();
        String pageUrl = login.getLoginUrl();
        if (StringUtils.isBlank(pageUrl)) {
            return null;
        }
        String separator = pageUrl.contains("?") ? "&" : "?";
        return pageUrl + separator + login.getRedirectParamName() + "="
            + URLEncoder.encode(buildCurrentUrl(request), StandardCharsets.UTF_8);
    }

    /**
     * 还原浏览器侧看到的URL。OP部署在网关/Ingress后面时，直接用request的scheme与host
     * 会拼出内网地址，登录成功后跳回来打不开
     */
    private String buildCurrentUrl(HttpServletRequest request) {
        String proto = StringUtils.defaultIfBlank(request.getHeader("X-Forwarded-Proto"), request.getScheme());
        String host = StringUtils.defaultIfBlank(request.getHeader("X-Forwarded-Host"), request.getHeader("Host"));
        if (StringUtils.isBlank(host)) {
            host = request.getServerName() + ":" + request.getServerPort();
        }
        String query = request.getQueryString();
        return proto + "://" + host + request.getRequestURI() + (query == null ? "" : "?" + query);
    }

    private void writeJson(HttpServletResponse response,
                           HttpStatus status,
                           String message,
                           Map<String, Object> extra) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new HashMap<>();
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("status", status.value());
        if (extra != null) {
            body.putAll(extra);
        }
        response.getWriter().write(JsonUtils.toJson(body));
        response.getWriter().flush();
    }
}
