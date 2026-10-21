/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 */

package com.tencent.bk.job.config;

import com.tencent.bk.job.consts.CommonHeader;
import com.tencent.bk.job.controller.api.impl.IssueStatusResourceImpl;
import com.tencent.bk.job.controller.api.impl.PullRequestResourceImpl;
import com.tencent.bk.job.controller.api.impl.ReleaseStatusResourceImpl;
import com.tencent.bk.job.dao.IssueStatusMapper;
import com.tencent.bk.job.service.PullRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.stream.Stream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ApiAuthInterceptorTest {

    private static final String API_KEY = "test-api-key";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(API_KEY);
    }

    @SuppressWarnings("unchecked")
    private MockMvc buildMockMvc(String configuredApiKey) {
        ApiAuthProperties properties = new ApiAuthProperties();
        properties.setApiKey(configuredApiKey);
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String[] paths = ApiWebMvcConfig.resolveInterceptPaths().toArray(String[]::new);
        return standaloneSetup(
            new IssueStatusResourceImpl(mock(IssueStatusMapper.class)),
            new PullRequestResourceImpl(mock(PullRequestService.class)),
            new ReleaseStatusResourceImpl(redisTemplate)
        )
            .addMappedInterceptors(paths, new ApiAuthInterceptor(properties))
            .build();
    }

    static Stream<MockHttpServletRequestBuilder> apiRequests() {
        return Stream.of(
            get("/api/issueStatus").param("id", "issue_4523"),
            post("/api/issueStatus")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"issue_4523\",\"status\":\"issue_todo\",\"statusDescription\":\"待处理\"}"),
            post("/api/pullRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"prId\":4564,\"issueId\":4563,\"codeDir\":\"F:\\\\Job\","
                    + "\"status\":\"WAIT_FOR_REVIEW\",\"agentName\":\"agent\",\"authorName\":\"author\"}"),
            get("/api/pullRequest").param("prId", "4564"),
            post("/api/pullRequest/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"prId\":4564,\"status\":\"HANDLING\"}"),
            get("/api/releaseStatus").param("env", "test"),
            post("/api/releaseStatus").param("env", "test").param("releasing", "true")
        );
    }

    @ParameterizedTest
    @MethodSource("apiRequests")
    @DisplayName("API路径缺少Key时返回401")
    void shouldRejectMissingApiKey(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("apiRequests")
    @DisplayName("API路径使用错误Key时返回401")
    void shouldRejectInvalidApiKey(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.header(CommonHeader.OP_API_AUTH_HEADER_KEY, "wrong"))
            .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("apiRequests")
    @DisplayName("API路径使用正确Key时放行")
    void shouldAllowCorrectApiKey(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.header(CommonHeader.OP_API_AUTH_HEADER_KEY, API_KEY))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("API Key未配置时即使请求带Key也返回503")
    void shouldFailCloseWhenApiKeyIsNotConfigured() throws Exception {
        MockMvc unconfiguredMockMvc = buildMockMvc(" ");

        unconfiguredMockMvc.perform(get("/api/issueStatus")
                .param("id", "issue_4523")
                .header(CommonHeader.OP_API_AUTH_HEADER_KEY, API_KEY))
            .andExpect(status().isServiceUnavailable());
    }

    @ParameterizedTest
    @MethodSource("removedRequests")
    @DisplayName("旧Issue、发布状态路径不再提供服务")
    void shouldNotExposeRemovedPaths(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.header(CommonHeader.OP_API_AUTH_HEADER_KEY, API_KEY))
            .andExpect(status().isNotFound());
    }

    static Stream<MockHttpServletRequestBuilder> removedRequests() {
        return Stream.of(
            get("/issueStatus"),
            post("/issueStatus"),
            get("/releaseStatus"),
            post("/releaseStatus")
        );
    }
}
