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
import com.tencent.bk.job.common.model.BasicApp;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.service.CommonAppService;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.TopoService;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.helper.AuthHelper;
import com.tencent.bk.sdk.iamv4.service.IamV4AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExecuteAuthServiceImplTest {

    private static final User USER = new User("default", "tester", "测试用户");

    private AuthHelper authHelper;
    private AppAuthService appAuthService;
    private AppScopeMappingService appScopeMappingService;
    private JobExecuteConfig jobExecuteConfig;
    private CommonAppService commonAppService;
    private ExecuteAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        authHelper = mock(AuthHelper.class);
        ResourceNameQueryService resourceNameQueryService = mock(ResourceNameQueryService.class);
        AuthService authService = mock(AuthService.class);
        appAuthService = mock(AppAuthService.class);
        appScopeMappingService = mock(AppScopeMappingService.class);
        doCallRealMethod().when(appScopeMappingService).fillAppResourceScope(any());
        jobExecuteConfig = mock(JobExecuteConfig.class);
        commonAppService = mock(CommonAppService.class);
        TopoService topoService = mock(TopoService.class);
        ExecuteTargetInstanceBuilder executeTargetInstanceBuilder =
            new ExecuteTargetInstanceBuilder(topoService);
        service = new ExecuteAuthServiceImpl(
            new V3ExecuteAuthExecutor(authHelper, executeTargetInstanceBuilder),
            new V4ExecuteAuthExecutor(mock(IamV4AuthService.class), executeTargetInstanceBuilder),
            executeTargetInstanceBuilder,
            resourceNameQueryService,
            authService,
            appAuthService,
            appScopeMappingService,
            jobExecuteConfig,
            commonAppService
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
        verify(appScopeMappingService, never()).getScopeByAppId(any());
        verify(appScopeMappingService, never()).getAppIdByScope(anyString(), anyString());
    }

    @Test
    void incompleteScope_shouldResolveOnceAndReuseForAccountPlanAndHostAuth() {
        AppResourceScope requestScope = new AppResourceScope(2L);
        when(appScopeMappingService.getScopeByAppId(2L))
            .thenReturn(new ResourceScope(ResourceScopeTypeEnum.BIZ, "2"));
        when(jobExecuteConfig.getEnableAuthAccountMode())
            .thenReturn(FeatureToggleModeEnum.ENABLED.getMode());
        when(appAuthService.batchAuthResources(any(), anyString(), any(), anyList()))
            .thenReturn(AuthResult.pass(USER));

        service.batchAuthAccountExecutable(USER, requestScope, Collections.singleton(100L));
        service.authExecutePlan(USER, requestScope, 500L, 1001L, "方案", targetWithHost());

        verify(appScopeMappingService, times(1)).getScopeByAppId(2L);
        assertThat(requestScope.getType()).isEqualTo(ResourceScopeTypeEnum.BIZ);
        assertThat(requestScope.getId()).isEqualTo("2");
    }

    @Test
    void scopeWithTypeAndId_shouldResolveAppIdOnce() {
        AppResourceScope requestScope =
            new AppResourceScope(ResourceScopeTypeEnum.BIZ_SET, "9991001", null);
        when(appScopeMappingService.getAppIdByScope(ResourceScopeTypeEnum.BIZ_SET.getValue(), "9991001"))
            .thenReturn(2L);

        service.authExecutePlan(USER, requestScope, 500L, 1001L, "方案", targetWithHost());

        verify(appScopeMappingService, times(1))
            .getAppIdByScope(ResourceScopeTypeEnum.BIZ_SET.getValue(), "9991001");
        verify(appScopeMappingService, never()).getScopeByAppId(any());
        assertThat(requestScope.getAppId()).isEqualTo(2L);
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

        verify(appScopeMappingService, never()).getScopeByAppId(any());
        verify(appScopeMappingService, never()).getAppIdByScope(anyString(), anyString());
    }

    @Test
    void emptyOrDisabledPreconditions_shouldNotResolveScope() {
        AppResourceScope incompleteScope = new AppResourceScope(2L);
        when(jobExecuteConfig.getEnableAuthAccountMode())
            .thenReturn(FeatureToggleModeEnum.DISABLED.getMode());

        service.batchAuthAccountExecutable(USER, incompleteScope, Collections.singleton(100L));
        service.batchAuthAccountExecutable(USER, incompleteScope, Collections.emptySet());
        service.batchAuthViewFileSource(USER, incompleteScope, Collections.emptyMap());

        verify(appScopeMappingService, never()).fillAppResourceScope(any());
        verify(appScopeMappingService, never()).getScopeByAppId(any());
        verify(appAuthService, never()).batchAuthResources(any(), anyString(), any(), anyList());
    }

    @Test
    void mappingExceptions_shouldPropagateWithoutCallingIam() {
        NotFoundException notFound = new NotFoundException(ErrorCode.APP_NOT_EXIST);
        doThrow(notFound).when(appScopeMappingService).getScopeByAppId(2L);

        assertThatThrownBy(() -> service.authExecutePlan(
            USER, new AppResourceScope(2L), 500L, 1001L, "方案", targetWithHost()))
            .isSameAs(notFound);
        verify(authHelper, never()).isAllowed(anyString(), anyString(), anyString(), any(), anyList());

        InternalException internal = new InternalException(ErrorCode.INTERNAL_ERROR);
        doThrow(internal).when(appScopeMappingService).getScopeByAppId(2L);
        assertThatThrownBy(() -> service.authExecutePlan(
            USER, new AppResourceScope(2L), 500L, 1001L, "方案", targetWithHost()))
            .isSameAs(internal);
    }

    @Test
    void invalidMappedScope_shouldFailClosedWithoutCallingIam() {
        assertThatThrownBy(() -> service.authExecutePlan(
            USER, null, 500L, 1001L, "方案", targetWithHost()))
            .isInstanceOf(InternalException.class);
        assertThatThrownBy(() -> service.authExecutePlan(
            USER, new AppResourceScope((Long) null), 500L, 1001L, "方案", targetWithHost()))
            .isInstanceOf(InternalException.class);

        List<ResourceScope> invalidScopes = Arrays.asList(
            new ResourceScope((ResourceScopeTypeEnum) null, "2"),
            new ResourceScope(ResourceScopeTypeEnum.BIZ, null)
        );
        for (ResourceScope invalidScope : invalidScopes) {
            when(appScopeMappingService.getScopeByAppId(2L)).thenReturn(invalidScope);
            assertThatThrownBy(() -> service.authExecutePlan(
                USER, new AppResourceScope(2L), 500L, 1001L, "方案", targetWithHost()))
                .isInstanceOf(InternalException.class);
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

    @Test
    void rejectOtherTenant() {
        mockApplication(2L, "another-tenant");

        assertThatThrownBy(() -> service.checkViewTaskInstancePermission(USER, completeBizScope(),
            taskInstance(2L, USER.getUsername()))).isInstanceOf(NotFoundException.class);
        verify(appAuthService, never()).auth(any(), anyString(), any());
    }

    @Test
    void missingApp_shouldReject() {
        assertThatThrownBy(() -> service.checkViewTaskInstancePermission(USER, completeBizScope(),
            taskInstance(2L, USER.getUsername()))).isInstanceOf(NotFoundException.class);
        verify(appAuthService, never()).auth(any(), anyString(), any());
    }

    @Test
    void wrongApp_shouldReject() {
        mockApplication(2L, USER.getTenantId());

        assertThatThrownBy(() -> service.checkViewTaskInstancePermission(USER, completeBizScope(),
            taskInstance(3L, USER.getUsername()))).isInstanceOf(NotFoundException.class);
        verify(appAuthService, never()).auth(any(), anyString(), any());
    }

    @Test
    void operator_shouldPassWithoutIam() {
        mockApplication(2L, USER.getTenantId());
        AppResourceScope scope = new AppResourceScope(ResourceScopeTypeEnum.BIZ, "2", null);

        assertThat(service.checkViewTaskInstancePermission(USER, scope,
            taskInstance(2L, USER.getUsername())).isPass()).isTrue();
        verify(appAuthService, never()).auth(any(), anyString(), any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void nonOperator_shouldCheckHistory(boolean allowed) {
        mockApplication(2L, USER.getTenantId());
        AppResourceScope scope = completeBizScope();
        AuthResult result = allowed ? AuthResult.pass(USER) : AuthResult.fail(USER);
        when(appAuthService.auth(USER, ActionId.VIEW_HISTORY, scope)).thenReturn(result);

        assertThat(service.checkViewTaskInstancePermission(USER, scope, taskInstance(2L, "another-user")).isPass())
            .isEqualTo(allowed);
        verify(appAuthService, never()).auth(any(), eq(ActionId.ACCESS_BUSINESS), any());
        verify(appAuthService).auth(USER, ActionId.VIEW_HISTORY, scope);
    }

    private void mockApplication(Long appId, String tenantId) {
        BasicApp app = new BasicApp();
        app.setId(appId);
        app.setTenantId(tenantId);
        when(commonAppService.getApp(new ResourceScope(ResourceScopeTypeEnum.BIZ, "2"))).thenReturn(app);
    }

    private TaskInstanceDTO taskInstance(Long appId, String operator) {
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setAppId(appId);
        taskInstance.setOperator(operator);
        return taskInstance;
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
