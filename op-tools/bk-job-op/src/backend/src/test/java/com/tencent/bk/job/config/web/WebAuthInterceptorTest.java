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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tencent.bk.job.service.login.BkTicketLoginService;
import com.tencent.bk.job.service.login.LoginServiceUnavailableException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Web登录拦截器的放行与拒绝行为
 */
class WebAuthInterceptorTest {

    private static final String PAGE_URL = "https://login.example.com/";

    private BkTicketLoginService loginService;

    private Logger logger;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        loginService = mock(BkTicketLoginService.class);
        logger = (Logger) LoggerFactory.getLogger(WebAuthInterceptor.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
        listAppender.stop();
    }

    private WebAuthProperties newProperties(String whitelist, String loginPageUrl) {
        WebAuthProperties properties = new WebAuthProperties();
        properties.getAuth().setWhitelist(whitelist);
        properties.getAuth().getLogin().setLoginUrl(loginPageUrl);
        properties.init();
        return properties;
    }

    private MockHttpServletRequest pageRequest() {
        return new MockHttpServletRequest("GET", "/issue-status");
    }

    private MockHttpServletRequest apiRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/web/pullRequest/list");
        request.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        return request;
    }

    private MockHttpServletRequest deleteApiRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/web/pullRequest/delete");
        request.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        return request;
    }

    private WebAuthInterceptor newInterceptor(WebAuthProperties properties) {
        return new WebAuthInterceptor(properties, loginService);
    }

    @Test
    @DisplayName("白名单内的已登录用户被放行，用户名写入request attribute")
    void shouldPassAndExposeUsernameForWhitelistedUser() throws Exception {
        when(loginService.getUsername("t1")).thenReturn("admin");
        MockHttpServletRequest request = apiRequest();
        request.setCookies(new Cookie("bk_ticket", "t1"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean passed = newInterceptor(newProperties("admin", PAGE_URL)).preHandle(request, response, new Object());

        assertThat(passed).isTrue();
        assertThat(request.getAttribute(WebAuthInterceptor.ATTR_USERNAME)).isEqualTo("admin");
    }

    @Test
    @DisplayName("白名单为空时已登录用户也被拒绝")
    void shouldRejectWhenWhitelistIsEmpty() throws Exception {
        when(loginService.getUsername("t1")).thenReturn("admin");
        MockHttpServletRequest request = apiRequest();
        request.setCookies(new Cookie("bk_ticket", "t1"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean passed = newInterceptor(newProperties("", PAGE_URL)).preHandle(request, response, new Object());

        assertThat(passed).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("无Cookie的页面请求302跳登录页并带回跳地址，且不调用登录服务")
    void shouldRedirectPageRequestWithoutTicket() throws Exception {
        MockHttpServletRequest request = pageRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean passed = newInterceptor(newProperties("admin", PAGE_URL)).preHandle(request, response, new Object());

        assertThat(passed).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.FOUND.value());
        assertThat(response.getRedirectedUrl()).startsWith(PAGE_URL).contains("c_url=");
        verify(loginService, never()).getUsername(anyString());
    }

    @Test
    @DisplayName("无Cookie的接口请求返回401 JSON而不是302，避免XHR静默跟随到登录页")
    void shouldReturnJsonForApiRequestWithoutTicket() throws Exception {
        MockHttpServletRequest request = apiRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean passed = newInterceptor(newProperties("admin", PAGE_URL)).preHandle(request, response, new Object());

        assertThat(passed).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentType()).contains(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response.getContentAsString()).contains("loginUrl");
    }

    @Test
    @DisplayName("PR删除接口无登录态返回401")
    void shouldRejectPullRequestDeletionWithoutTicket() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean passed = newInterceptor(newProperties("admin", PAGE_URL))
            .preHandle(deleteApiRequest(), response, new Object());

        assertThat(passed).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("PR删除接口对白名单内已登录用户放行")
    void shouldAllowPullRequestDeletionForWhitelistedUser() throws Exception {
        when(loginService.getUsername("t1")).thenReturn("admin");
        MockHttpServletRequest request = deleteApiRequest();
        request.setCookies(new Cookie("bk_ticket", "t1"));

        boolean passed = newInterceptor(newProperties("admin", PAGE_URL))
            .preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(passed).isTrue();
        assertThat(request.getAttribute(WebAuthInterceptor.ATTR_USERNAME)).isEqualTo("admin");
    }

    @Test
    @DisplayName("登录页未配置时返回503提示页，绝不重定向到空地址造成无限重定向")
    void shouldReturn503WhenLoginPageUrlIsNotConfigured() throws Exception {
        MockHttpServletRequest request = pageRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean passed = newInterceptor(newProperties("admin", "")).preHandle(request, response, new Object());

        assertThat(passed).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(response.getRedirectedUrl()).isNull();
        assertThat(response.getForwardedUrl()).isEqualTo("/503-login-unconfigured.html");
    }

    @Test
    @DisplayName("登录服务不可用时拒绝访问（fail-close）并返回503")
    void shouldFailCloseWhenLoginServiceIsUnavailable() throws Exception {
        when(loginService.getUsername("t1")).thenThrow(new LoginServiceUnavailableException("timeout"));
        MockHttpServletRequest request = apiRequest();
        request.setCookies(new Cookie("bk_ticket", "t1"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean passed = newInterceptor(newProperties("admin", PAGE_URL)).preHandle(request, response, new Object());

        assertThat(passed).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
    }

    @Test
    @DisplayName("登录异常链即使包含ticket也不得写入日志")
    void shouldNotLogTicketFromLoginException() throws Exception {
        String ticket = "sensitive-ticket";
        LoginServiceUnavailableException exception = new LoginServiceUnavailableException(
            "Failed to call login service",
            new IllegalStateException("GET /user/get_info/?bk_ticket=" + ticket)
        );
        when(loginService.getUsername(ticket)).thenThrow(exception);
        MockHttpServletRequest request = apiRequest();
        request.setCookies(new Cookie("bk_ticket", ticket));
        MockHttpServletResponse response = new MockHttpServletResponse();

        newInterceptor(newProperties("admin", PAGE_URL)).preHandle(request, response, new Object());

        assertThat(listAppender.list).extracting(ILoggingEvent::getFormattedMessage)
            .allSatisfy(message -> assertThat(message).doesNotContain(ticket));
    }

    @Test
    @DisplayName("ticket无效时按未登录处理")
    void shouldTreatInvalidTicketAsUnauthenticated() throws Exception {
        when(loginService.getUsername("bad")).thenReturn(null);
        MockHttpServletRequest request = apiRequest();
        request.setCookies(new Cookie("bk_ticket", "bad"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean passed = newInterceptor(newProperties("admin", PAGE_URL)).preHandle(request, response, new Object());

        assertThat(passed).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }
}
