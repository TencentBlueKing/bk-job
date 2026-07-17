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
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.api.esb.impl.v4.OpenApiUserScopeV4ResourceImpl;
import com.tencent.bk.job.manage.model.dto.UserAppScopeDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4AuthorizedScopeDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4GetUserAuthorizedScopesResult;
import com.tencent.bk.job.manage.service.scope.UserAppScopeQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    private UserAppScopeQueryService userAppScopeQueryService;
    private OpenApiUserScopeV4ResourceImpl resource;

    @BeforeEach
    void setUp() {
        userAppScopeQueryService = mock(UserAppScopeQueryService.class);
        resource = new OpenApiUserScopeV4ResourceImpl(userAppScopeQueryService);
    }

    @Test
    @DisplayName("默认分页：offset=0, length=10")
    void getUserAuthorizedScopes_defaultPagination() {
        PageData<UserAppScopeDTO> pageData = buildPageData(0, 10, 2L, Arrays.asList(
            buildScope(1L, "biz", "2", "biz-a", true, 100L),
            buildScope(2L, "biz_set", "3", "set-a", false, null)
        ));
        when(userAppScopeQueryService.listAuthorizedScopesPaged(eq(USERNAME), eq(0), eq(10))).thenReturn(pageData);

        EsbV4Response<V4GetUserAuthorizedScopesResult> response =
            resource.getUserAuthorizedScopes(USERNAME, APP_CODE, null, null);

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
        assertThat(first.getFavorTime()).isEqualTo(100L);
        assertThat(first.getTimeZone()).isEqualTo("Asia/Shanghai");

        verify(userAppScopeQueryService).listAuthorizedScopesPaged(USERNAME, 0, 10);
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
            resource.getUserAuthorizedScopes(USERNAME, APP_CODE, null, null);

        assertThat(response.getData().getTotal()).isEqualTo(0L);
        assertThat(response.getData().getScopeList()).isEmpty();
    }

    @Test
    @DisplayName("offset 非法时抛出参数异常")
    void getUserAuthorizedScopes_invalidOffset() {
        assertThatThrownBy(() -> resource.getUserAuthorizedScopes(USERNAME, APP_CODE, -1, 10))
            .isInstanceOf(InvalidParamException.class);
    }

    @Test
    @DisplayName("length 非法时抛出参数异常")
    void getUserAuthorizedScopes_invalidLength() {
        assertThatThrownBy(() -> resource.getUserAuthorizedScopes(USERNAME, APP_CODE, 0, 0))
            .isInstanceOf(InvalidParamException.class);
        assertThatThrownBy(() -> resource.getUserAuthorizedScopes(USERNAME, APP_CODE, 0, 201))
            .isInstanceOf(InvalidParamException.class);
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
