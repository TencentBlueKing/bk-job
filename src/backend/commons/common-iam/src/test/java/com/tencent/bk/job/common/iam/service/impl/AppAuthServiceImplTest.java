/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 */

package com.tencent.bk.job.common.iam.service.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.client.IIamClient;
import com.tencent.bk.job.common.iam.config.JobIamProperties;
import com.tencent.bk.job.common.iam.config.JobIamV4Properties;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.service.ActionAuthExecutor;
import com.tencent.bk.job.common.iam.service.AppAuthExecutor;
import com.tencent.bk.job.common.iam.service.ResourceNameQueryService;
import com.tencent.bk.job.common.iam.util.BusinessAuthHelper;
import com.tencent.bk.job.common.model.BasicApp;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.CommonAppService;
import com.tencent.bk.sdk.iam.dto.InstanceDTO;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.dto.action.ActionDTO;
import com.tencent.bk.sdk.iam.helper.AuthHelper;
import com.tencent.bk.sdk.iam.service.PolicyService;
import com.tencent.bk.sdk.iam.util.PathBuilder;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AppAuthServiceImplTest {

    private static final User USER = new User("default", "tester", "测试用户");

    private AuthHelper authHelper;
    private IIamClient iamClient;
    private ResourceNameQueryService resourceNameQueryService;
    private AppAuthServiceImpl appAuthService;
    private CommonAppService commonAppService;

    @BeforeEach
    void setUp() {
        authHelper = mock(AuthHelper.class);
        iamClient = mock(IIamClient.class);
        resourceNameQueryService = mock(ResourceNameQueryService.class);
        commonAppService = mock(CommonAppService.class);
        AppAuthExecutor v3Executor = new V3AppAuthExecutor(
            authHelper, mock(BusinessAuthHelper.class), mock(PolicyService.class), iamClient);
        appAuthService = new AppAuthServiceImpl(
            v3Executor,
            mock(AppAuthExecutor.class),
            mock(JobIamProperties.class),
            mock(JobIamV4Properties.class),
            commonAppService
        );
        appAuthService.setResourceNameQueryService(resourceNameQueryService);
    }

    @Test
    void batchAuthResources_shouldCopyCompleteDeniedResourceWithoutMutatingSource() {
        PermissionResource source = accountResource(
            "5", ResourceTypeId.BUSINESS_SET, "9991001");
        PermissionResource parent =
            new PermissionResource(ResourceTypeEnum.BUSINESS_SET, "9991001", "业务集");
        source.setParentHierarchicalResources(Collections.singletonList(parent));
        when(authHelper.isAllowed(anyString(), anyString(), anyString(), anyList()))
            .thenReturn(Collections.emptyList());
        when(resourceNameQueryService.getResourceName(ResourceTypeEnum.ACCOUNT, "5"))
            .thenReturn("root");

        AuthResult result = appAuthService.batchAuthResources(
            USER,
            ActionId.USE_ACCOUNT,
            new AppResourceScope(1L),
            Collections.singletonList(source)
        );

        PermissionResource denied = firstResource(result);
        assertThat(denied).isNotSameAs(source);
        assertThat(denied.getResourceId()).isEqualTo("5");
        assertThat(denied.getResourceType()).isEqualTo(ResourceTypeEnum.ACCOUNT);
        assertThat(denied.getSystemId()).isEqualTo(ResourceTypeEnum.ACCOUNT.getSystemId());
        assertThat(denied.getType()).isEqualTo(ResourceTypeId.ACCOUNT);
        assertThat(denied.getResourceName()).isEqualTo("root");
        assertThat(denied.getParentHierarchicalResources()).containsExactly(parent);
        assertPath(denied.getPathInfo(), ResourceTypeId.BUSINESS_SET, "9991001");
        assertPath(denied.getPathInfo().getChild(), ResourceTypeId.ACCOUNT, "5");

        assertThat(source.getSystemId()).isNull();
        assertThat(source.getType()).isNull();
        assertThat(source.getResourceName()).isNull();
    }

    @Test
    void batchAuthResources_partialAllowed_shouldKeepOnlyDeniedResourcePath() {
        PermissionResource allowed = accountResource("5", ResourceTypeId.BIZ, "2");
        PermissionResource denied = accountResource("6", ResourceTypeId.TENANT_SET, "3");
        when(authHelper.isAllowed(anyString(), anyString(), anyString(), anyList()))
            .thenReturn(Collections.singletonList("5"));
        when(resourceNameQueryService.getResourceName(ResourceTypeEnum.ACCOUNT, "6"))
            .thenReturn("account-6");

        AuthResult result = appAuthService.batchAuthResources(
            USER,
            ActionId.USE_ACCOUNT,
            new AppResourceScope(1L),
            Arrays.asList(allowed, denied)
        );

        PermissionResource failedResource = firstResource(result);
        assertThat(failedResource.getResourceId()).isEqualTo("6");
        assertPath(failedResource.getPathInfo(), ResourceTypeId.TENANT_SET, "3");
        assertPath(failedResource.getPathInfo().getChild(), ResourceTypeId.ACCOUNT, "6");
    }

    @Test
    void batchAuthResources_shouldPreserveExplicitFieldsAndSkipNameLookup() {
        PermissionResource source = accountResource("5", ResourceTypeId.BIZ, "2");
        source.setSystemId("custom-system");
        source.setType("custom-type");
        source.setSubResourceType("custom-sub-type");
        source.setResourceName("explicit-name");
        when(authHelper.isAllowed(anyString(), anyString(), anyString(), anyList()))
            .thenReturn(Collections.emptyList());

        PermissionResource denied = firstResource(appAuthService.batchAuthResources(
            USER,
            ActionId.USE_ACCOUNT,
            new AppResourceScope(1L),
            Collections.singletonList(source)
        ));

        assertThat(denied.getSystemId()).isEqualTo("custom-system");
        assertThat(denied.getType()).isEqualTo("custom-type");
        assertThat(denied.getSubResourceType()).isEqualTo("custom-sub-type");
        assertThat(denied.getResourceName()).isEqualTo("explicit-name");
        verify(resourceNameQueryService, never()).getResourceName(
            ResourceTypeEnum.ACCOUNT, "5");
    }

    @Test
    void deniedAccounts_shouldBuildScopeToAccountApplicationWithoutDuplicateAccount() {
        PermissionResource bizAccount = accountResource("5", ResourceTypeId.BIZ, "2");
        PermissionResource bizSetAccount =
            accountResource("6", ResourceTypeId.BUSINESS_SET, "9991001");
        when(authHelper.isAllowed(anyString(), anyString(), anyString(), anyList()))
            .thenReturn(Collections.emptyList());
        when(resourceNameQueryService.getResourceName(ResourceTypeEnum.ACCOUNT, "5"))
            .thenReturn("account-5");
        when(resourceNameQueryService.getResourceName(ResourceTypeEnum.ACCOUNT, "6"))
            .thenReturn("account-6");

        AuthResult result = appAuthService.batchAuthResources(
            USER,
            ActionId.USE_ACCOUNT,
            new AppResourceScope(1L),
            Arrays.asList(bizAccount, bizSetAccount)
        );
        AuthServiceImpl authService = newAuthService();
        authService.getApplyUrl(USER.getTenantId(), result.getRequiredActionResources());

        ArgumentCaptor<List<ActionDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(iamClient).getApplyUrl(captor.capture());
        List<List<InstanceDTO>> instances = captor.getValue().get(0)
            .getRelatedResourceTypes().get(0).getInstance();
        assertHierarchy(instances.get(0), ResourceTypeId.BIZ, "2", "5");
        assertHierarchy(instances.get(1), ResourceTypeId.BUSINESS_SET, "9991001", "6");
    }

    @Test
    void accessBusinessDenied_bizSet_shouldBuildSingleBizSetNode() {
        assertAccessBusinessApplication(
            ResourceScopeTypeEnum.BIZ_SET, "9992001", ResourceTypeId.BUSINESS_SET);
    }

    @Test
    void accessBusinessDenied_tenantSet_shouldBuildSingleTenantSetNode() {
        assertAccessBusinessApplication(
            ResourceScopeTypeEnum.TENANT_SET, "9993001", ResourceTypeId.TENANT_SET);
    }

    @Test
    void accessBusinessDenied_biz_shouldBuildSingleBizNode() {
        assertAccessBusinessApplication(ResourceScopeTypeEnum.BIZ, "2", ResourceTypeId.BIZ);
    }

    @Test
    void buildFailAuthResultFromResources_invalidResource_shouldFailClosed() {
        AbstractAuthResultBuilder basicAuthService = new AbstractAuthResultBuilder() {
        };
        basicAuthService.setResourceNameQueryService(resourceNameQueryService);
        PermissionResource missingType = new PermissionResource();
        missingType.setResourceId("5");
        PermissionResource blankId = new PermissionResource();
        blankId.setResourceType(ResourceTypeEnum.ACCOUNT);
        blankId.setResourceId(" ");

        for (PermissionResource invalidResource : Arrays.asList(null, missingType, blankId)) {
            assertThatThrownBy(() -> basicAuthService.buildFailAuthResultFromResources(
                USER, ActionId.USE_ACCOUNT, Collections.singletonList(invalidResource)))
                .isInstanceOf(InternalException.class);
        }
    }

    private AuthServiceImpl newAuthService() {
        ActionAuthExecutor v3Executor = new V3ActionAuthExecutor(authHelper, iamClient);
        AuthServiceImpl authService = new AuthServiceImpl(
            v3Executor, mock(ActionAuthExecutor.class), mock(MessageI18nService.class));
        authService.setResourceNameQueryService(resourceNameQueryService);
        return authService;
    }

    private PermissionResource accountResource(String accountId, String scopeType, String scopeId) {
        PermissionResource resource = new PermissionResource();
        resource.setResourceType(ResourceTypeEnum.ACCOUNT);
        resource.setResourceId(accountId);
        resource.setPathInfo(PathBuilder.newBuilder(scopeType, scopeId)
            .child(ResourceTypeId.ACCOUNT, accountId).build());
        return resource;
    }

    private PermissionResource firstResource(AuthResult result) {
        return result.getRequiredActionResources().get(0)
            .getResourceGroups().get(0).getPermissionResources().get(0);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rejectOtherTenant(boolean byAppId) {
        BasicApp app = tenantApp("other-tenant");
        AppResourceScope scope = byAppId
            ? new AppResourceScope(ResourceScopeTypeEnum.BIZ, "999", 1L)
            : new AppResourceScope(ResourceScopeTypeEnum.BIZ_SET, "2", null);
        if (byAppId) {
            when(commonAppService.getApp(1L)).thenReturn(app);
        } else {
            when(commonAppService.getApp(app.getScope())).thenReturn(app);
        }

        assertThatThrownBy(() -> appAuthService.auth(USER, ActionId.ACCESS_BUSINESS, scope))
            .isInstanceOfSatisfying(NotFoundException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.APP_NOT_EXIST));
        verifyNoInteractions(authHelper, iamClient, resourceNameQueryService);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void sameTenant_shouldResolveScope(boolean byAppId) {
        BasicApp app = tenantApp(USER.getTenantId());
        AppResourceScope scope = byAppId ? new AppResourceScope(1L)
            : new AppResourceScope(ResourceScopeTypeEnum.BIZ_SET, "2", null);
        if (byAppId) {
            when(commonAppService.getApp(1L)).thenReturn(app);
        } else {
            when(commonAppService.getApp(app.getScope())).thenReturn(app);
        }
        when(authHelper.isAllowed(any(), anyString(), anyString(), any(InstanceDTO.class))).thenReturn(true);

        assertThat(appAuthService.auth(USER, ActionId.ACCESS_BUSINESS, scope).isPass()).isTrue();
        ArgumentCaptor<InstanceDTO> instance = ArgumentCaptor.forClass(InstanceDTO.class);
        verify(authHelper).isAllowed(any(), anyString(), anyString(), instance.capture());
        assertPath(instance.getValue().getPath(), ResourceTypeId.BUSINESS_SET, "2");
    }

    @Test
    void missingApp_shouldReject() {
        assertThatThrownBy(() -> appAuthService.auth(USER, ActionId.ACCESS_BUSINESS, new AppResourceScope(1L)))
            .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(authHelper, resourceNameQueryService);
    }

    @Test
    void viewHistory_shouldOnlyCheckIam() {
        when(authHelper.isAllowed(any(), anyString(), anyString(), any(InstanceDTO.class))).thenReturn(true);

        assertThat(appAuthService.auth(USER, ActionId.VIEW_HISTORY,
            new AppResourceScope(ResourceScopeTypeEnum.BIZ, "2", 1L)).isPass()).isTrue();
        verify(authHelper).isAllowed(eq(USER.getTenantId()), eq(USER.getUsername()),
            eq(ActionId.VIEW_HISTORY), any(InstanceDTO.class));
        verifyNoInteractions(commonAppService);
    }

    private BasicApp tenantApp(String tenantId) {
        BasicApp app = new BasicApp();
        app.setId(1L);
        app.setTenantId(tenantId);
        app.setScope(new ResourceScope(ResourceScopeTypeEnum.BIZ_SET, "2"));
        return app;
    }

    private void assertAccessBusinessApplication(ResourceScopeTypeEnum scopeType,
                                                 String scopeId,
                                                 String expectedResourceType) {
        BasicApp app = new BasicApp();
        app.setId(1L);
        app.setScope(new ResourceScope(scopeType, scopeId));
        app.setTenantId(USER.getTenantId());
        when(commonAppService.getApp(1L)).thenReturn(app);
        AuthResult result = appAuthService.auth(
            USER,
            ActionId.ACCESS_BUSINESS,
            new AppResourceScope(scopeType, scopeId, 1L)
        );
        AuthServiceImpl authService = newAuthService();
        authService.getApplyUrl(USER.getTenantId(), result.getRequiredActionResources());

        ArgumentCaptor<List<ActionDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(iamClient).getApplyUrl(captor.capture());
        List<List<InstanceDTO>> instances = captor.getValue().get(0)
            .getRelatedResourceTypes().get(0).getInstance();
        assertThat(instances).hasSize(1);
        assertThat(instances.get(0)).extracting(InstanceDTO::getType)
            .containsExactly(expectedResourceType);
        assertThat(instances.get(0)).extracting(InstanceDTO::getId)
            .containsExactly(scopeId);
    }

    private void assertPath(PathInfoDTO path, String type, String id) {
        assertThat(path).isNotNull();
        assertThat(path.getType()).isEqualTo(type);
        assertThat(path.getId()).isEqualTo(id);
    }

    private void assertHierarchy(List<InstanceDTO> hierarchy,
                                 String scopeType,
                                 String scopeId,
                                 String accountId) {
        assertThat(hierarchy).extracting(InstanceDTO::getType)
            .containsExactly(scopeType, ResourceTypeId.ACCOUNT);
        assertThat(hierarchy).extracting(InstanceDTO::getId)
            .containsExactly(scopeId, accountId);
    }
}
