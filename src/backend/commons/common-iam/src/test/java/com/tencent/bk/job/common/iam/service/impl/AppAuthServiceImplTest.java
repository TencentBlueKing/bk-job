/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 */

package com.tencent.bk.job.common.iam.service.impl;

import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.client.IIamClient;
import com.tencent.bk.job.common.iam.config.JobIamProperties;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.service.ResourceNameQueryService;
import com.tencent.bk.job.common.iam.util.BusinessAuthHelper;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.sdk.iam.dto.InstanceDTO;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.dto.action.ActionDTO;
import com.tencent.bk.sdk.iam.helper.AuthHelper;
import com.tencent.bk.sdk.iam.service.PolicyService;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppAuthServiceImplTest {

    private static final User USER = new User("default", "tester", "测试用户");

    private AuthHelper authHelper;
    private IIamClient iamClient;
    private ResourceNameQueryService resourceNameQueryService;
    private AppAuthServiceImpl appAuthService;

    @BeforeEach
    void setUp() {
        authHelper = mock(AuthHelper.class);
        iamClient = mock(IIamClient.class);
        resourceNameQueryService = mock(ResourceNameQueryService.class);
        appAuthService = new AppAuthServiceImpl(
            authHelper,
            mock(BusinessAuthHelper.class),
            mock(PolicyService.class),
            mock(JobIamProperties.class),
            iamClient
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
        AuthServiceImpl authService = new AuthServiceImpl(
            authHelper, mock(MessageI18nService.class), iamClient);
        authService.setResourceNameQueryService(resourceNameQueryService);
        authService.getApplyUrl(USER.getTenantId(), result.getRequiredActionResources());

        ArgumentCaptor<List<ActionDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(iamClient).getApplyUrl(captor.capture());
        List<List<InstanceDTO>> instances = captor.getValue().get(0)
            .getRelatedResourceTypes().get(0).getInstance();
        assertHierarchy(instances.get(0), ResourceTypeId.BIZ, "2", "5");
        assertHierarchy(instances.get(1), ResourceTypeId.BUSINESS_SET, "9991001", "6");
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
