/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 */

package com.tencent.bk.job.common.iam.model;

import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.iam.AuthResultDTO;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class AuthResultTest {

    @Test
    void dtoRoundTrip_shouldPreservePathAndParentHierarchy() {
        PermissionResource plan = new PermissionResource(ResourceTypeEnum.PLAN, "1001", "默认方案");
        plan.setPathInfo(PathBuilder.newBuilder(ResourceTypeId.BIZ, "2")
            .child(ResourceTypeId.TEMPLATE, "500").build());

        PermissionResource biz = new PermissionResource(ResourceTypeEnum.BUSINESS, "2", "蓝鲸");
        PermissionResource template = new PermissionResource(ResourceTypeEnum.TEMPLATE, "500", "发布模板");
        template.setParentHierarchicalResources(Collections.singletonList(biz));
        plan.setParentHierarchicalResources(Arrays.asList(biz, template));

        AuthResult original = AuthResult.fail(new User("default", "tester", "测试用户"));
        original.addRequiredPermission(ActionId.VIEW_JOB_PLAN, plan);

        AuthResultDTO dto = AuthResult.toAuthResultDTO(original);
        AuthResult restored = AuthResult.fromAuthResultDTO(dto);
        PermissionResource restoredPlan = restored.getRequiredActionResources().get(0)
            .getResourceGroups().get(0).getPermissionResources().get(0);

        assertPath(restoredPlan.getPathInfo(), ResourceTypeId.BIZ, "2");
        assertPath(restoredPlan.getPathInfo().getChild(), ResourceTypeId.TEMPLATE, "500");
        assertThat(restoredPlan.getParentHierarchicalResources()).hasSize(2);
        assertThat(restoredPlan.getParentHierarchicalResources())
            .extracting(PermissionResource::getResourceId)
            .containsExactly("2", "500");
        PermissionResource restoredTemplate = restoredPlan.getParentHierarchicalResources().get(1);
        assertThat(restoredTemplate.getParentHierarchicalResources()).hasSize(1);
        assertThat(restoredTemplate.getParentHierarchicalResources().get(0).getResourceId()).isEqualTo("2");
    }

    private void assertPath(PathInfoDTO path, String type, String id) {
        assertThat(path).isNotNull();
        assertThat(path.getType()).isEqualTo(type);
        assertThat(path.getId()).isEqualTo(id);
    }
}
