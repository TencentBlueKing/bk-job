/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 */

package com.tencent.bk.job.config.actuator;

import com.tencent.bk.job.consts.CommonHeader;
import com.tencent.bk.job.dao.IssueStatusMapper;
import com.tencent.bk.job.service.login.BkTicketLoginService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "job.op.actuator.auth.username=monitor",
    "job.op.actuator.auth.password=synthetic-password",
    "api.auth.apiKey=synthetic-api-key",
    "mcp.auth.apiKey=synthetic-mcp-key",
    "job.op.web.auth.whitelist=tester",
    "job.op.web.auth.login.login-url=https://example.com/login",
    "management.health.db.enabled=false",
    "management.health.redis.enabled=false",
    "management.endpoint.health.probes.enabled=true"
})
@AutoConfigureMockMvc
class ActuatorSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BkTicketLoginService bkTicketLoginService;

    @MockitoBean
    private IssueStatusMapper issueStatusMapper;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(bkTicketLoginService.getUsername(anyString())).thenReturn("tester");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"})
    @DisplayName("health及探针匿名可访问且不暴露详情")
    void shouldAllowAnonymousSimpleHealth(String path) throws Exception {
        mockMvc.perform(get(path))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").exists())
            .andExpect(jsonPath("$.details").doesNotExist())
            .andExpect(jsonPath("$.components").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/actuator", "/actuator/info", "/actuator/env", "/actuator/metrics",
        "/actuator/beans", "/actuator/configprops"
    })
    @DisplayName("除health外的Actuator端点匿名访问返回401")
    void shouldRejectAnonymousProtectedActuator(String path) throws Exception {
        mockMvc.perform(get(path))
            .andExpect(status().isUnauthorized())
            .andExpect(header().exists("WWW-Authenticate"));
    }

    @Test
    @DisplayName("正确Basic凭据可访问受保护端点")
    void shouldAllowCorrectBasicCredentials() throws Exception {
        mockMvc.perform(get("/actuator/info")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic("monitor", "synthetic-password")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("错误Basic凭据返回401")
    void shouldRejectIncorrectBasicCredentials() throws Exception {
        mockMvc.perform(get("/actuator/info")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic("monitor", "wrong")))
            .andExpect(status().isUnauthorized())
            .andExpect(header().exists("WWW-Authenticate"));
    }

    @Test
    @DisplayName("错误Basic凭据不影响公开接口")
    void shouldIgnoreIncorrectBasicCredentialsForPublicEndpoint() throws Exception {
        mockMvc.perform(get("/public/checkServiceDependency")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic("monitor", "wrong")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("错误Basic凭据不抢占非Actuator API的API Key鉴权")
    void shouldIgnoreIncorrectBasicCredentialsForApiEndpoint() throws Exception {
        mockMvc.perform(get("/api/issueStatus")
                .param("id", "issue_4523")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic("monitor", "wrong")))
            .andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist("WWW-Authenticate"))
            .andExpect(jsonPath("$.message").value("Missing API Key in request header: "
                + CommonHeader.OP_API_AUTH_HEADER_KEY));

        mockMvc.perform(get("/api/issueStatus")
                .param("id", "issue_4523")
                .header(CommonHeader.OP_API_AUTH_HEADER_KEY, "synthetic-api-key")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic("monitor", "wrong")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("发布状态新路径纳入API Key鉴权")
    void shouldProtectReleaseStatusWithApiKey() throws Exception {
        mockMvc.perform(get("/api/releaseStatus").param("env", "test"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/releaseStatus")
                .param("env", "test")
                .header(CommonHeader.OP_API_AUTH_HEADER_KEY, "synthetic-api-key"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("错误Basic凭据不影响Web登录态鉴权与SPA回落")
    void shouldPreserveWebAuthenticationAndSpaFallback() throws Exception {
        mockMvc.perform(get("/web/userInfo")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic("monitor", "wrong")))
            .andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist("WWW-Authenticate"));

        Cookie ticket = new Cookie("bk_ticket", "synthetic-ticket");
        mockMvc.perform(get("/web/userInfo")
                .cookie(ticket)
                .with(SecurityMockMvcRequestPostProcessors.httpBasic("monitor", "wrong")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("tester"));
        mockMvc.perform(get("/issue-status").cookie(ticket))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    @DisplayName("错误Basic凭据不抢占MCP拦截器鉴权")
    void shouldLeaveMcpAuthenticationToExistingInterceptor() throws Exception {
        mockMvc.perform(post("/mcp")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic("monitor", "wrong")))
            .andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    @DisplayName("静态错误页不被Spring Security拦截")
    void shouldAllowStaticResources() throws Exception {
        mockMvc.perform(get("/403.html")).andExpect(status().isOk());
    }
}
