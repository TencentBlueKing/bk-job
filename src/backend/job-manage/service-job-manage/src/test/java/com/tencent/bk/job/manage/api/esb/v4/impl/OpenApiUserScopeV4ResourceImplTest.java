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

package com.tencent.bk.job.manage.api.esb.v4.impl;

import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.api.esb.impl.v4.OpenApiUserScopeV4ResourceImpl;
import com.tencent.bk.job.manage.model.dto.UserAppScopeDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4AuthorizedScopeDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4GetUserAuthorizedScopesResult;
import com.tencent.bk.job.manage.service.scope.UserAppScopeQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link OpenApiUserScopeV4ResourceImpl} 单元测试。
 */
class OpenApiUserScopeV4ResourceImplTest {

    private static final String USERNAME = "tester";
    private static final String APP_CODE = "bk_job";
    private static final long FAVOR_TIME_MILLIS = 1710000000000L;
    private static final String FAVOR_TIME_FORMATTED = "2024-03-10 00:00:00.000";

    private UserAppScopeQueryService userAppScopeQueryService;
    private OpenApiUserScopeV4ResourceImpl resource;

    @BeforeEach
    void setUp() {
        userAppScopeQueryService = mock(UserAppScopeQueryService.class);
        resource = new OpenApiUserScopeV4ResourceImpl(userAppScopeQueryService);
    }

    @AfterEach
    void tearDown() {
        // 清理线程上下文，避免设置的用户时区污染其他用例
        JobContextUtil.unsetContext();
    }

    @Test
    @DisplayName("默认分页：offset=0, length=10")
    void getUserAuthorizedScopes_defaultPagination() {
        PageData<UserAppScopeDTO> pageData = buildPageData(0, 10, 2L, Arrays.asList(
            buildScope(1L, "biz", "2", "biz-a", true, FAVOR_TIME_MILLIS),
            buildScope(2L, "biz_set", "3", "set-a", false, null)
        ));
        when(userAppScopeQueryService.listAuthorizedScopesPaged(eq(USERNAME), eq(0), eq(10))).thenReturn(pageData);

        EsbV4Response<V4GetUserAuthorizedScopesResult> response =
            resource.getUserAuthorizedScopes(USERNAME, APP_CODE, 0, 10);

        V4GetUserAuthorizedScopesResult result = response.getData();
        assertThat(result.getTotal()).isEqualTo(2L);
        assertThat(result.getOffset()).isEqualTo(0);
        assertThat(result.getLength()).isEqualTo(10);
        assertThat(result.getScopeList()).hasSize(2);

        V4AuthorizedScopeDTO first = result.getScopeList().get(0);
        assertThat(first.getScopeType()).isEqualTo("biz");
        assertThat(first.getScopeId()).isEqualTo("2");
        assertThat(first.getName()).isEqualTo("biz-a");
        assertThat(first.getFavor()).isTrue();
        assertThat(first.getFavorTime()).isEqualTo(FAVOR_TIME_FORMATTED);
        assertThat(first.getTimeZone()).isEqualTo("Asia/Shanghai");

        V4AuthorizedScopeDTO second = result.getScopeList().get(1);
        assertThat(second.getFavorTime()).isNull();

        verify(userAppScopeQueryService).listAuthorizedScopesPaged(USERNAME, 0, 10);
    }

    @Test
    @DisplayName("favor_time 按用户时区格式化，不受资源范围（业务）时区影响")
    void getUserAuthorizedScopes_favorTimeUsesUserTimeZone() {
        // 用户时区设为美东（非东八区），资源范围时区仍为 Asia/Shanghai
        JobContextUtil.setTimeZone(ZoneId.of("America/New_York"));
        PageData<UserAppScopeDTO> pageData = buildPageData(0, 10, 1L, Collections.singletonList(
            buildScope(1L, "biz", "2", "biz-a", true, FAVOR_TIME_MILLIS)
        ));
        when(userAppScopeQueryService.listAuthorizedScopesPaged(eq(USERNAME), eq(0), eq(10))).thenReturn(pageData);

        EsbV4Response<V4GetUserAuthorizedScopesResult> response =
            resource.getUserAuthorizedScopes(USERNAME, APP_CODE, 0, 10);

        V4AuthorizedScopeDTO first = response.getData().getScopeList().get(0);
        // 1710000000000ms = 2024-03-09T16:00:00Z，美东（EST, UTC-5）为 2024-03-09 11:00:00.000
        assertThat(first.getFavorTime()).isEqualTo("2024-03-09 11:00:00.000");
        // time_zone 仍返回资源范围（业务）时区，不随用户时区变化
        assertThat(first.getTimeZone()).isEqualTo("Asia/Shanghai");
    }

    @Test
    @DisplayName("自定义分页参数生效")
    void getUserAuthorizedScopes_customPagination() {
        PageData<UserAppScopeDTO> pageData = buildPageData(10, 5, 12L, Collections.emptyList());
        when(userAppScopeQueryService.listAuthorizedScopesPaged(eq(USERNAME), eq(10), eq(5))).thenReturn(pageData);

        EsbV4Response<V4GetUserAuthorizedScopesResult> response =
            resource.getUserAuthorizedScopes(USERNAME, APP_CODE, 10, 5);

        assertThat(response.getData().getOffset()).isEqualTo(10);
        assertThat(response.getData().getLength()).isEqualTo(5);
        assertThat(response.getData().getTotal()).isEqualTo(12L);
        assertThat(response.getData().getScopeList()).isEmpty();
        verify(userAppScopeQueryService).listAuthorizedScopesPaged(USERNAME, 10, 5);
    }

    @Test
    @DisplayName("无权限时返回空列表")
    void getUserAuthorizedScopes_emptyResult() {
        PageData<UserAppScopeDTO> pageData = buildPageData(0, 10, 0L, Collections.emptyList());
        when(userAppScopeQueryService.listAuthorizedScopesPaged(eq(USERNAME), eq(0), eq(10))).thenReturn(pageData);

        EsbV4Response<V4GetUserAuthorizedScopesResult> response =
            resource.getUserAuthorizedScopes(USERNAME, APP_CODE, 0, 10);

        assertThat(response.getData().getTotal()).isEqualTo(0L);
        assertThat(response.getData().getScopeList()).isEmpty();
    }

    private PageData<UserAppScopeDTO> buildPageData(int start,
                                                    int pageSize,
                                                    long total,
                                                    List<UserAppScopeDTO> data) {
        PageData<UserAppScopeDTO> pageData = new PageData<>();
        pageData.setStart(start);
        pageData.setPageSize(pageSize);
        pageData.setTotal(total);
        pageData.setData(data);
        return pageData;
    }

    private UserAppScopeDTO buildScope(Long appId,
                                       String scopeType,
                                       String scopeId,
                                       String name,
                                       Boolean favor,
                                       Long favorTime) {
        return new UserAppScopeDTO(
            appId,
            scopeType,
            scopeId,
            false,
            false,
            name,
            true,
            "Asia/Shanghai",
            favor,
            favorTime
        );
    }
}
