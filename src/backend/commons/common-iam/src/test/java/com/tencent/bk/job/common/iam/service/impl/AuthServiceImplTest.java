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

package com.tencent.bk.job.common.iam.service.impl;

import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.client.IIamClient;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.model.PermissionActionResource;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.model.PermissionResourceGroup;
import com.tencent.bk.job.common.iam.service.ResourceNameQueryService;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.sdk.iam.dto.InstanceDTO;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.dto.action.ActionDTO;
import com.tencent.bk.sdk.iam.helper.AuthHelper;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthServiceImpl 单元测试：验证鉴权失败时 parentHierarchicalResources 的构建逻辑 (#2428)
 */
class AuthServiceImplTest {

    private AuthHelper authHelper;
    private IIamClient iamClient;
    private ResourceNameQueryService resourceNameQueryService;
    private AuthServiceImpl authService;

    private static final String TENANT_ID = "default";
    private static final String USERNAME = "testuser";
    private static final User TEST_USER = new User(TENANT_ID, USERNAME, "测试用户");

    private static final String ACTION_MANAGE_ACCOUNT = "manage_account";
    private static final String ACTION_VIEW_JOB_PLAN = "view_job_plan";
    private static final String ACTION_MANAGE_SCRIPT = "manage_script";

    @BeforeEach
    void setUp() {
        authHelper = mock(AuthHelper.class);
        iamClient = mock(IIamClient.class);
        MessageI18nService i18nService = mock(MessageI18nService.class);
        resourceNameQueryService = mock(ResourceNameQueryService.class);

        authService = new AuthServiceImpl(authHelper, i18nService, iamClient);
        authService.setResourceNameQueryService(resourceNameQueryService);

        // 默认所有鉴权返回 false，触发失败路径
        when(authHelper.isAllowed(anyString(), anyString(), anyString(), any(InstanceDTO.class)))
            .thenReturn(false);
    }

    @Test
    @DisplayName("Case1: 单层路径(biz) — 账号鉴权失败时，parentHierarchicalResources 应包含业务信息")
    void authFail_singleLevelPath_shouldBuildParentHierarchicalResources() {
        String bizId = "2";
        String accountId = "100";
        when(resourceNameQueryService.getResourceName(ResourceTypeEnum.ACCOUNT, accountId))
            .thenReturn("root");
        when(resourceNameQueryService.getResourceName(ResourceTypeEnum.BUSINESS, bizId))
            .thenReturn("蓝鲸");

        // biz/2 -> 单节点路径
        PathInfoDTO pathInfo = PathBuilder.newBuilder(ResourceTypeId.BIZ, bizId).build();

        AuthResult result = authService.auth(TEST_USER, ACTION_MANAGE_ACCOUNT,
            ResourceTypeEnum.ACCOUNT, accountId, pathInfo);

        assertThat(result.isPass()).isFalse();

        PermissionResource resource = extractFirstPermissionResource(result);
        assertThat(resource.getResourceId()).isEqualTo(accountId);
        assertThat(resource.getResourceName()).isEqualTo("root");

        List<PermissionResource> parents = resource.getParentHierarchicalResources();
        assertThat(parents).hasSize(1);
        assertThat(parents.get(0).getResourceType()).isEqualTo(ResourceTypeEnum.BUSINESS);
        assertThat(parents.get(0).getResourceId()).isEqualTo(bizId);
        assertThat(parents.get(0).getResourceName()).isEqualTo("蓝鲸");
    }

    @Test
    @DisplayName("Case2: 两层路径(biz->template) — 执行方案鉴权失败时，parentHierarchicalResources 应包含业务和模板信息")
    void authFail_twoLevelPath_shouldBuildMultipleParentResources() {
        String bizId = "2";
        String templateId = "500";
        String planId = "1001";
        when(resourceNameQueryService.getResourceName(ResourceTypeEnum.PLAN, planId))
            .thenReturn("默认方案");
        when(resourceNameQueryService.getResourceName(ResourceTypeEnum.BUSINESS, bizId))
            .thenReturn("蓝鲸");
        when(resourceNameQueryService.getResourceName(ResourceTypeEnum.TEMPLATE, templateId))
            .thenReturn("发布模板");

        // biz/2 -> job_template/500
        PathInfoDTO pathInfo = PathBuilder.newBuilder(ResourceTypeId.BIZ, bizId)
            .child(ResourceTypeId.TEMPLATE, templateId).build();

        AuthResult result = authService.auth(TEST_USER, ACTION_VIEW_JOB_PLAN,
            ResourceTypeEnum.PLAN, planId, pathInfo);

        assertThat(result.isPass()).isFalse();

        PermissionResource resource = extractFirstPermissionResource(result);
        assertThat(resource.getResourceId()).isEqualTo(planId);

        List<PermissionResource> parents = resource.getParentHierarchicalResources();
        assertThat(parents).hasSize(2);
        // 第一层：业务
        assertThat(parents.get(0).getResourceType()).isEqualTo(ResourceTypeEnum.BUSINESS);
        assertThat(parents.get(0).getResourceId()).isEqualTo(bizId);
        assertThat(parents.get(0).getResourceName()).isEqualTo("蓝鲸");
        // 第二层：模板
        assertThat(parents.get(1).getResourceType()).isEqualTo(ResourceTypeEnum.TEMPLATE);
        assertThat(parents.get(1).getResourceId()).isEqualTo(templateId);
        assertThat(parents.get(1).getResourceName()).isEqualTo("发布模板");
    }

    @Test
    @DisplayName("Case3: pathInfo为null — parentHierarchicalResources 不应被设置")
    void authFail_nullPathInfo_shouldNotSetParentHierarchicalResources() {
        String scriptId = "300";
        when(resourceNameQueryService.getResourceName(ResourceTypeEnum.SCRIPT, scriptId))
            .thenReturn("清理脚本");

        AuthResult result = authService.auth(TEST_USER, ACTION_MANAGE_SCRIPT,
            ResourceTypeEnum.SCRIPT, scriptId, null);

        assertThat(result.isPass()).isFalse();

        PermissionResource resource = extractFirstPermissionResource(result);
        assertThat(resource.getResourceId()).isEqualTo(scriptId);
        assertThat(resource.getParentHierarchicalResources()).isNull();
    }

    @Test
    @DisplayName("Case4: pathInfo节点type不在ResourceTypeEnum中 — 该节点应被跳过")
    void authFail_unknownResourceType_shouldSkipNode() {
        String accountId = "100";
        when(resourceNameQueryService.getResourceName(ResourceTypeEnum.ACCOUNT, accountId))
            .thenReturn("root");

        // 构造一个type不在 ResourceTypeEnum 中的 PathInfoDTO
        PathInfoDTO pathInfo = new PathInfoDTO();
        pathInfo.setType("unknown_type");
        pathInfo.setId("999");

        AuthResult result = authService.auth(TEST_USER, ACTION_MANAGE_ACCOUNT,
            ResourceTypeEnum.ACCOUNT, accountId, pathInfo);

        assertThat(result.isPass()).isFalse();

        PermissionResource resource = extractFirstPermissionResource(result);
        assertThat(resource.getResourceId()).isEqualTo(accountId);
        // 未知type被跳过，parentHierarchicalResources 为空列表 -> 不被设置
        assertThat(resource.getParentHierarchicalResources()).isNull();
    }

    @Test
    @DisplayName("Case5: pathInfo节点id为空 — 该节点应被跳过")
    void authFail_emptyNodeId_shouldSkipNode() {
        String accountId = "100";
        when(resourceNameQueryService.getResourceName(ResourceTypeEnum.ACCOUNT, accountId))
            .thenReturn("root");

        // 构造一个 id 为空的节点
        PathInfoDTO pathInfo = new PathInfoDTO();
        pathInfo.setType(ResourceTypeId.BIZ);
        pathInfo.setId("");

        AuthResult result = authService.auth(TEST_USER, ACTION_MANAGE_ACCOUNT,
            ResourceTypeEnum.ACCOUNT, accountId, pathInfo);

        assertThat(result.isPass()).isFalse();

        PermissionResource resource = extractFirstPermissionResource(result);
        assertThat(resource.getResourceId()).isEqualTo(accountId);
        // id为空被跳过
        assertThat(resource.getParentHierarchicalResources()).isNull();
    }

    @Test
    @DisplayName("Case6: pathInfo包含资源自身(biz->account) — 应去重，parentHierarchicalResources 仅包含biz")
    void authFail_pathIncludesResource_shouldDedup() {
        String bizId = "2";
        String accountId = "3";
        when(resourceNameQueryService.getResourceName(ResourceTypeEnum.ACCOUNT, accountId))
            .thenReturn("root");
        when(resourceNameQueryService.getResourceName(ResourceTypeEnum.BUSINESS, bizId))
            .thenReturn("蓝鲸");

        // biz/2 -> account/3，pathInfo 包含了资源本身
        PathInfoDTO pathInfo = PathBuilder.newBuilder(ResourceTypeId.BIZ, bizId)
            .child(ResourceTypeId.ACCOUNT, accountId).build();

        AuthResult result = authService.auth(TEST_USER, "use_account",
            ResourceTypeEnum.ACCOUNT, accountId, pathInfo);

        assertThat(result.isPass()).isFalse();

        PermissionResource resource = extractFirstPermissionResource(result);
        assertThat(resource.getResourceId()).isEqualTo(accountId);
        assertThat(resource.getResourceName()).isEqualTo("root");

        // parentHierarchicalResources 应仅包含 biz，account 节点被去重
        List<PermissionResource> parents = resource.getParentHierarchicalResources();
        assertThat(parents).hasSize(1);
        assertThat(parents.get(0).getResourceType()).isEqualTo(ResourceTypeEnum.BUSINESS);
        assertThat(parents.get(0).getResourceId()).isEqualTo(bizId);
        assertThat(parents.get(0).getResourceName()).isEqualTo("蓝鲸");
    }

    @Test
    @DisplayName("Case7: 两层路径(biz->template)鉴权Plan — template不是Plan，应全部保留")
    void authFail_pathDoesNotIncludeResource_shouldKeepAll() {
        String bizId = "2";
        String templateId = "500";
        String planId = "1001";
        when(resourceNameQueryService.getResourceName(ResourceTypeEnum.PLAN, planId))
            .thenReturn("默认方案");
        when(resourceNameQueryService.getResourceName(ResourceTypeEnum.BUSINESS, bizId))
            .thenReturn("蓝鲸");
        when(resourceNameQueryService.getResourceName(ResourceTypeEnum.TEMPLATE, templateId))
            .thenReturn("发布模板");

        // biz/2 -> job_template/500，路径不包含 Plan 资源本身
        PathInfoDTO pathInfo = PathBuilder.newBuilder(ResourceTypeId.BIZ, bizId)
            .child(ResourceTypeId.TEMPLATE, templateId).build();

        AuthResult result = authService.auth(TEST_USER, ACTION_VIEW_JOB_PLAN,
            ResourceTypeEnum.PLAN, planId, pathInfo);

        assertThat(result.isPass()).isFalse();

        PermissionResource resource = extractFirstPermissionResource(result);
        assertThat(resource.getResourceId()).isEqualTo(planId);

        // biz 和 template 都不匹配 Plan，应全部保留
        List<PermissionResource> parents = resource.getParentHierarchicalResources();
        assertThat(parents).hasSize(2);
        assertThat(parents.get(0).getResourceType()).isEqualTo(ResourceTypeEnum.BUSINESS);
        assertThat(parents.get(1).getResourceType()).isEqualTo(ResourceTypeEnum.TEMPLATE);
    }

    @Test
    @DisplayName("Case8: buildApplyActions 应将三类 scope path 转换为最终申请层级")
    void getApplyUrl_pathInfo_shouldBuildHierarchicalActionInstances() {
        PermissionResource bizPlan = planResource("1001", ResourceTypeId.BIZ, "2", "500");
        PermissionResource bizSetPlan =
            planResource("1002", ResourceTypeId.BUSINESS_SET, "20", "501");
        PermissionResource tenantSetPlan =
            planResource("1003", ResourceTypeId.TENANT_SET, "30", "502");
        AuthResult authResult = AuthResult.fail(TEST_USER);
        authResult.addRequiredPermissions(
            ACTION_VIEW_JOB_PLAN, Arrays.asList(bizPlan, bizSetPlan, tenantSetPlan));

        authService.getApplyUrl(TENANT_ID, authResult.getRequiredActionResources());

        ArgumentCaptor<List<ActionDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(iamClient).getApplyUrl(captor.capture());
        List<List<InstanceDTO>> instances = captor.getValue().get(0)
            .getRelatedResourceTypes().get(0).getInstance();
        assertThat(instances).hasSize(3);
        assertHierarchy(instances.get(0), ResourceTypeId.BIZ, "2", "500", "1001");
        assertHierarchy(instances.get(1), ResourceTypeId.BUSINESS_SET, "20", "501", "1002");
        assertHierarchy(instances.get(2), ResourceTypeId.TENANT_SET, "30", "502", "1003");
    }

    @Test
    @DisplayName("Case9: 显式 parent 与 path 同时存在时仅使用显式 parent")
    void getApplyUrl_explicitParents_shouldTakePrecedenceOverPathInfo() {
        PermissionResource plan = planResource("1001", ResourceTypeId.BIZ, "2", "500");
        PermissionResource explicitParent =
            new PermissionResource(ResourceTypeEnum.TEMPLATE, "900", "显式模板");
        plan.setParentHierarchicalResources(Collections.singletonList(explicitParent));
        AuthResult authResult = AuthResult.fail(TEST_USER);
        authResult.addRequiredPermission(ACTION_VIEW_JOB_PLAN, plan);

        authService.getApplyUrl(TENANT_ID, authResult.getRequiredActionResources());

        ArgumentCaptor<List<ActionDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(iamClient).getApplyUrl(captor.capture());
        List<InstanceDTO> hierarchy = captor.getValue().get(0)
            .getRelatedResourceTypes().get(0).getInstance().get(0);
        assertThat(hierarchy).extracting(InstanceDTO::getType)
            .containsExactly(ResourceTypeId.TEMPLATE, ResourceTypeId.PLAN);
        assertThat(hierarchy).extracting(InstanceDTO::getId)
            .containsExactly("900", "1001");
    }

    private PermissionResource planResource(String planId,
                                            String scopeType,
                                            String scopeId,
                                            String templateId) {
        PermissionResource plan = new PermissionResource(ResourceTypeEnum.PLAN, planId, "方案-" + planId);
        plan.setPathInfo(PathBuilder.newBuilder(scopeType, scopeId)
            .child(ResourceTypeId.TEMPLATE, templateId).build());
        return plan;
    }

    private void assertHierarchy(List<InstanceDTO> hierarchy,
                                 String scopeType,
                                 String scopeId,
                                 String templateId,
                                 String planId) {
        assertThat(hierarchy).extracting(InstanceDTO::getType)
            .containsExactly(scopeType, ResourceTypeId.TEMPLATE, ResourceTypeId.PLAN);
        assertThat(hierarchy).extracting(InstanceDTO::getId)
            .containsExactly(scopeId, templateId, planId);
    }

    /**
     * 从 AuthResult 中提取第一个 PermissionResource
     */
    private PermissionResource extractFirstPermissionResource(AuthResult result) {
        List<PermissionActionResource> actionResources = result.getRequiredActionResources();
        assertThat(actionResources).isNotEmpty();
        List<PermissionResourceGroup> groups = actionResources.get(0).getResourceGroups();
        assertThat(groups).isNotEmpty();
        List<PermissionResource> resources = groups.get(0).getPermissionResources();
        assertThat(resources).isNotEmpty();
        return resources.get(0);
    }
}
