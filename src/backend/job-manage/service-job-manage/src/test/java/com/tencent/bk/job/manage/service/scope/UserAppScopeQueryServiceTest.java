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

package com.tencent.bk.job.manage.service.scope;

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.iam.dto.AppResourceScopeResult;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.model.dto.ApplicationFavorDTO;
import com.tencent.bk.job.manage.model.dto.UserAppScopeDTO;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.impl.ApplicationFavorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link UserAppScopeQueryService} 单元测试。
 */
class UserAppScopeQueryServiceTest {

    private static final String TENANT_ID = "default";
    private static final String USERNAME = "tester";

    private ApplicationService applicationService;
    private ApplicationFavorService applicationFavorService;
    private AppAuthService appAuthService;
    private AppScopeMappingService appScopeMappingService;
    private UserAppScopeQueryService service;

    @BeforeEach
    void setUp() {
        applicationService = mock(ApplicationService.class);
        applicationFavorService = mock(ApplicationFavorService.class);
        appAuthService = mock(AppAuthService.class);
        appScopeMappingService = mock(AppScopeMappingService.class);
        service = new UserAppScopeQueryService(
            applicationService,
            applicationFavorService,
            appAuthService,
            appScopeMappingService
        );
        JobContextUtil.setUser(new User(TENANT_ID, USERNAME, USERNAME));
    }

    @AfterEach
    void tearDown() {
        JobContextUtil.unsetContext();
    }

    @Test
    @DisplayName("排序：有权限优先，其次收藏，再按收藏时间倒序；并过滤无权限项")
    void listAuthorizedScopesPaged_sortAndFilter() {
        ApplicationDTO authorizedFavoredOld = buildApp(1L, "biz", "1", "a");
        ApplicationDTO authorizedFavoredNew = buildApp(2L, "biz", "2", "b");
        ApplicationDTO authorizedNotFavor = buildApp(3L, "biz_set", "3", "c");
        ApplicationDTO unauthorized = buildApp(4L, "biz", "4", "d");
        when(applicationService.listAllAppsForTenant(TENANT_ID)).thenReturn(Arrays.asList(
            unauthorized, authorizedNotFavor, authorizedFavoredOld, authorizedFavoredNew
        ));

        AppResourceScopeResult authResult = new AppResourceScopeResult();
        authResult.setAny(false);
        authResult.setAppResourceScopeList(Arrays.asList(
            new AppResourceScope(1L, new ResourceScope(ResourceScopeTypeEnum.BIZ, "1")),
            new AppResourceScope(2L, new ResourceScope(ResourceScopeTypeEnum.BIZ, "2")),
            new AppResourceScope(3L, new ResourceScope(ResourceScopeTypeEnum.BIZ_SET, "3"))
        ));
        when(appAuthService.getAppResourceScopeList(any(User.class), anyList())).thenReturn(authResult);
        when(applicationFavorService.getAppFavorListByUsername(USERNAME)).thenReturn(Arrays.asList(
            new ApplicationFavorDTO(USERNAME, 1L, 100L),
            new ApplicationFavorDTO(USERNAME, 2L, 200L)
        ));

        PageData<UserAppScopeDTO> pageData = service.listAuthorizedScopesPaged(USERNAME, 0, 10);

        assertThat(pageData.getTotal()).isEqualTo(3L);
        assertThat(pageData.getData()).extracting(UserAppScopeDTO::getAppId)
            .containsExactly(2L, 1L, 3L);
        assertThat(pageData.getData()).allMatch(scope -> Boolean.TRUE.equals(scope.getHasPermission()));
    }

    @Test
    @DisplayName("分页截取生效")
    void listAuthorizedScopesPaged_pageSlice() {
        when(applicationService.listAllAppsForTenant(TENANT_ID)).thenReturn(Arrays.asList(
            buildApp(1L, "biz", "1", "a"),
            buildApp(2L, "biz", "2", "b"),
            buildApp(3L, "biz", "3", "c")
        ));
        AppResourceScopeResult authResult = new AppResourceScopeResult();
        authResult.setAny(true);
        authResult.setAppResourceScopeList(Collections.emptyList());
        when(appAuthService.getAppResourceScopeList(any(User.class), anyList())).thenReturn(authResult);
        when(applicationFavorService.getAppFavorListByUsername(eq(USERNAME))).thenReturn(Collections.emptyList());

        PageData<UserAppScopeDTO> pageData = service.listAuthorizedScopesPaged(USERNAME, 1, 1);

        assertThat(pageData.getTotal()).isEqualTo(3L);
        assertThat(pageData.getData()).hasSize(1);
        assertThat(pageData.getData().get(0).getAppId()).isEqualTo(2L);
    }

    private ApplicationDTO buildApp(Long appId, String scopeType, String scopeId, String name) {
        ApplicationDTO app = new ApplicationDTO();
        app.setId(appId);
        app.setName(name);
        app.setScope(new ResourceScope(scopeType, scopeId));
        app.setTimeZone("Asia/Shanghai");
        return app;
    }
}
