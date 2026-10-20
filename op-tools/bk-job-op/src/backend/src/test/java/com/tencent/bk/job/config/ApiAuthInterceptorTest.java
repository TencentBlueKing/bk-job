/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 */

package com.tencent.bk.job.config;

import com.tencent.bk.job.consts.ApiPaths;
import com.tencent.bk.job.consts.CommonHeader;
import com.tencent.bk.job.controller.impl.IssueStatusResourceImpl;
import com.tencent.bk.job.dao.IssueStatusMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.stream.Stream;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Issue状态正式路径必须使用API Key鉴权，已移除的路径不再提供服务。
 */
class ApiAuthInterceptorTest {

    private static final String API_KEY = "test-api-key";

    private static final String REMOVED_ISSUE_STATUS_PATH = "/" + "issueStatus";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ApiAuthProperties properties = new ApiAuthProperties();
        properties.setApiKey(API_KEY);
        IssueStatusResourceImpl resource = new IssueStatusResourceImpl(mock(IssueStatusMapper.class));
        String[] paths = ApiWebMvcConfig.resolveInterceptPaths().toArray(String[]::new);
        mockMvc = standaloneSetup(resource)
            .addMappedInterceptors(paths, new ApiAuthInterceptor(properties))
            .build();
    }

    static Stream<MockHttpServletRequestBuilder> issueStatusRequests() {
        return Stream.of(
            get(ApiPaths.ISSUE_STATUS).param("id", "issue_4523"),
            post(ApiPaths.ISSUE_STATUS)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"issue_4523\",\"status\":\"issue_todo\",\"statusDescription\":\"待处理\"}")
        );
    }

    static Stream<MockHttpServletRequestBuilder> removedIssueStatusRequests() {
        return Stream.of(
            get(REMOVED_ISSUE_STATUS_PATH).param("id", "issue_4523"),
            post(REMOVED_ISSUE_STATUS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"issue_4523\",\"status\":\"issue_todo\",\"statusDescription\":\"待处理\"}")
        );
    }

    @ParameterizedTest
    @MethodSource("issueStatusRequests")
    @DisplayName("正式路径的GET/POST缺少API Key时均返回401")
    void shouldRejectMissingApiKey(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("issueStatusRequests")
    @DisplayName("正式路径的GET/POST使用错误API Key时均返回401")
    void shouldRejectInvalidApiKey(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.header(CommonHeader.OP_API_AUTH_HEADER_KEY, "wrong"))
            .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("issueStatusRequests")
    @DisplayName("正式路径的GET/POST使用正确API Key时均放行")
    void shouldAllowCorrectApiKey(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.header(CommonHeader.OP_API_AUTH_HEADER_KEY, API_KEY))
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @MethodSource("removedIssueStatusRequests")
    @DisplayName("已移除路径的GET/POST即使携带正确API Key也返回404")
    void shouldNotExposeRemovedIssueStatusPath(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.header(CommonHeader.OP_API_AUTH_HEADER_KEY, API_KEY))
            .andExpect(status().isNotFound());
    }
}
