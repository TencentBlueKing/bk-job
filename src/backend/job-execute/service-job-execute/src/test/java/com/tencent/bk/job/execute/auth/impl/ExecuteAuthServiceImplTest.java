/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 */

package com.tencent.bk.job.execute.auth.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.FeatureToggleModeEnum;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.model.PermissionResourceGroup;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.iam.service.ResourceNameQueryService;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.service.TopoService;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.helper.AuthHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExecuteAuthServiceImplTest {

    private static final User USER = new User("default", "tester", "测试用户");

    private AuthHelper authHelper;
    private AppAuthService appAuthService;
    private AppScopeMappingService appScopeMappingService;
    private JobExecuteConfig jobExecuteConfig;
    private ExecuteAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        authHelper = mock(AuthHelper.class);
        ResourceNameQueryService resourceNameQueryService = mock(ResourceNameQueryService.class);
        AuthService authService = mock(AuthService.class);
        appAuthService = mock(AppAuthService.class);
        appScopeMappingService = mock(AppScopeMappingService.class);
        jobExecuteConfig = mock(JobExecuteConfig.class);
        TopoService topoService = mock(TopoService.class);
        service = new ExecuteAuthServiceImpl(
            authHelper,
            resourceNameQueryService,
            authService,
            appAuthService,
            appScopeMappingService,
            jobExecuteConfig,
            topoService
        );
    }

    @Test
    void deniedResources_shouldKeepScopePathsExceptPublicScript() {
        AppResourceScope scope = completeBizScope();
        ExecuteTargetDTO emptyTarget = new ExecuteTargetDTO();

        PermissionResource script = firstResourceOfType(
            service.authExecuteAppScript(USER, scope, "300", "脚本", emptyTarget),
            ResourceTypeEnum.SCRIPT
        );
        PermissionResource plan = firstResourceOfType(
            service.authExecutePlan(USER, scope, 500L, 1001L, "方案", emptyTarget),
            ResourceTypeEnum.PLAN
        );
        PermissionResource template = firstResourceOfType(
            service.authDebugTemplate(USER, scope, 500L, emptyTarget),
            ResourceTypeEnum.TEMPLATE
        );
        PermissionResource publicScript = firstResourceOfType(
            service.authExecutePublicScript(USER, scope, "301", "公共脚本", emptyTarget),
            ResourceTypeEnum.PUBLIC_SCRIPT
        );

        assertPath(script.getPathInfo(), ResourceTypeId.BIZ, "2");
        assertPath(plan.getPathInfo(), ResourceTypeId.BIZ, "2");
        assertPath(plan.getPathInfo().getChild(), ResourceTypeId.TEMPLATE, "500");
        assertPath(template.getPathInfo(), ResourceTypeId.BIZ, "2");
        assertThat(publicScript.getPathInfo()).isNull();
        verify(appScopeMappingService, never()).getAppResourceScope(any());
    }

    @Test
    void incompleteScope_shouldResolveOnceAndReuseForAccountPlanAndHostAuth() {
        AppResourceScope requestScope = new AppResourceScope(2L);
        when(appScopeMappingService.getAppResourceScope(2L)).thenReturn(completeBizScope());
        when(jobExecuteConfig.getEnableAuthAccountMode())
            .thenReturn(FeatureToggleModeEnum.ENABLED.getMode());
        when(appAuthService.batchAuthResources(any(), anyString(), any(), anyList()))
            .thenReturn(AuthResult.pass(USER));

        service.batchAuthAccountExecutable(USER, requestScope, Collections.singleton(100L));
        service.authExecutePlan(USER, requestScope, 500L, 1001L, "方案", targetWithHost());

        verify(appScopeMappingService, times(1)).getAppResourceScope(2L);
        assertThat(requestScope.getType()).isEqualTo(ResourceScopeTypeEnum.BIZ);
        assertThat(requestScope.getId()).isEqualTo("2");
    }

    @Test
    void batchAccountAuth_shouldPassAndReturnCompleteAccountPath() {
        when(jobExecuteConfig.getEnableAuthAccountMode())
            .thenReturn(FeatureToggleModeEnum.ENABLED.getMode());
        when(appAuthService.batchAuthResources(any(), anyString(), any(), anyList()))
            .thenAnswer(invocation -> {
                List<PermissionResource> resources = invocation.getArgument(3);
                AuthResult result = AuthResult.fail(USER);
                result.addRequiredPermissions(ActionId.USE_ACCOUNT, resources);
                return result;
            });

        AuthResult result = service.batchAuthAccountExecutable(
            USER, completeBizScope(), Collections.singleton(100L));

        ArgumentCaptor<List<PermissionResource>> captor = ArgumentCaptor.forClass(List.class);
        verify(appAuthService).batchAuthResources(
            any(), anyString(), any(), captor.capture());
        PermissionResource inputAccount = captor.getValue().get(0);
        assertPath(inputAccount.getPathInfo(), ResourceTypeId.BIZ, "2");
        assertPath(inputAccount.getPathInfo().getChild(), ResourceTypeId.ACCOUNT, "100");
        PermissionResource deniedAccount =
            firstResourceOfType(result, ResourceTypeEnum.ACCOUNT);
        assertThat(deniedAccount.getPathInfo()).isNotNull();
        assertPath(deniedAccount.getPathInfo().getChild(), ResourceTypeId.ACCOUNT, "100");
    }

    @Test
    void completeScope_shouldNotCallMappingService() {
        service.authExecutePlan(USER, completeBizScope(), 500L, 1001L, "方案", targetWithHost());

        verify(appScopeMappingService, never()).getAppResourceScope(any());
    }

    @Test
    void emptyOrDisabledPreconditions_shouldNotResolveScope() {
        AppResourceScope incompleteScope = new AppResourceScope(2L);
        when(jobExecuteConfig.getEnableAuthAccountMode())
            .thenReturn(FeatureToggleModeEnum.DISABLED.getMode());

        service.batchAuthAccountExecutable(USER, incompleteScope, Collections.singleton(100L));
        service.batchAuthAccountExecutable(USER, incompleteScope, Collections.emptySet());
        service.batchAuthViewFileSource(USER, incompleteScope, Collections.emptyMap());

        verify(appScopeMappingService, never()).getAppResourceScope(any());
        verify(appAuthService, never()).batchAuthResources(any(), anyString(), any(), anyList());
    }

    @Test
    void mappingExceptions_shouldPropagateWithoutCallingIam() {
        NotFoundException notFound = new NotFoundException(ErrorCode.APP_NOT_EXIST);
        when(appScopeMappingService.getAppResourceScope(2L)).thenThrow(notFound);

        assertThatThrownBy(() -> service.authExecutePlan(
            USER, new AppResourceScope(2L), 500L, 1001L, "方案", targetWithHost()))
            .isSameAs(notFound);
        verify(authHelper, never()).isAllowed(anyString(), anyString(), anyString(), any(), anyList());

        reset(appScopeMappingService);
        InternalException internal = new InternalException(ErrorCode.INTERNAL_ERROR);
        when(appScopeMappingService.getAppResourceScope(2L)).thenThrow(internal);
        assertThatThrownBy(() -> service.authExecutePlan(
            USER, new AppResourceScope(2L), 500L, 1001L, "方案", targetWithHost()))
            .isSameAs(internal);
    }

    @Test
    void invalidMappedScope_shouldFailClosedWithoutCallingIam() {
        List<AppResourceScope> invalidScopes = Arrays.asList(
            null,
            new AppResourceScope(ResourceScopeTypeEnum.BIZ, "2", null),
            new AppResourceScope(2L),
            new AppResourceScope(ResourceScopeTypeEnum.BIZ, null, 2L)
        );
        for (AppResourceScope invalidScope : invalidScopes) {
            when(appScopeMappingService.getAppResourceScope(2L)).thenReturn(invalidScope);
            assertThatThrownBy(() -> service.authExecutePlan(
                USER, new AppResourceScope(2L), 500L, 1001L, "方案", targetWithHost()))
                .isInstanceOf(InternalException.class);
            reset(appScopeMappingService);
        }
        verify(authHelper, never()).isAllowed(anyString(), anyString(), anyString(), any(), anyList());
    }

    @Test
    void quickExecute_shouldOnlyReturnHostResources() {
        AuthResult result = service.authFastExecuteScript(USER, completeBizScope(), targetWithHost());
        AuthResult fileResult = service.authFastPushFile(USER, completeBizScope(), targetWithHost());

        assertThat(result.getRequiredActionResources()).hasSize(1);
        assertThat(result.getRequiredActionResources().get(0).getResourceGroups())
            .extracting(PermissionResourceGroup::getResourceType)
            .containsExactly(ResourceTypeEnum.HOST);
        assertThat(fileResult.getRequiredActionResources()).hasSize(1);
        assertThat(fileResult.getRequiredActionResources().get(0).getResourceGroups())
            .extracting(PermissionResourceGroup::getResourceType)
            .containsExactly(ResourceTypeEnum.HOST);
    }

    private AppResourceScope completeBizScope() {
        return new AppResourceScope(ResourceScopeTypeEnum.BIZ, "2", 2L);
    }

    private ExecuteTargetDTO targetWithHost() {
        ExecuteTargetDTO target = new ExecuteTargetDTO();
        target.setStaticIpList(Collections.singletonList(new HostDTO(101L)));
        return target;
    }

    private PermissionResource firstResourceOfType(AuthResult result, ResourceTypeEnum resourceType) {
        return result.getRequiredActionResources().stream()
            .flatMap(actionResource -> actionResource.getResourceGroups().stream())
            .filter(group -> group.getResourceType() == resourceType)
            .flatMap(group -> group.getPermissionResources().stream())
            .findFirst()
            .orElseThrow(AssertionError::new);
    }

    private void assertPath(PathInfoDTO path, String type, String id) {
        assertThat(path).isNotNull();
        assertThat(path.getType()).isEqualTo(type);
        assertThat(path.getId()).isEqualTo(id);
    }
}
