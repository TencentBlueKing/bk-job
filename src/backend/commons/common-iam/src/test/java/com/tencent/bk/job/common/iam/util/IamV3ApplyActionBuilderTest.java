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

package com.tencent.bk.job.common.iam.util;

import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.PermissionActionResource;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.sdk.iam.constants.SystemId;
import com.tencent.bk.sdk.iam.dto.action.ActionDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IamV3ApplyActionBuilder：无权限详情的构造")
class IamV3ApplyActionBuilderTest {

    /**
     * v4 下每个操作只关联单一资源类型，execute_script 鉴权失败时只携带 host 资源，
     * 而 v3 模型的 Actions 声明该操作关联 [script, host]。此时不应整体失败。
     */
    @Test
    @DisplayName("v4 单资源类型：缺少 Actions 声明的其他资源类型时跳过而非抛异常")
    void skipMissingResourceTypeInsteadOfThrowing() {
        PermissionResource hostResource = new PermissionResource();
        hostResource.setSystemId(SystemId.CMDB);
        hostResource.setResourceId("100");
        hostResource.setResourceType(ResourceTypeEnum.HOST);
        hostResource.setResourceName("127.0.0.1");

        PermissionActionResource actionResource = new PermissionActionResource();
        actionResource.setActionId(ActionId.EXECUTE_SCRIPT);
        actionResource.addResource(hostResource);

        List<ActionDTO> actions = IamV3ApplyActionBuilder.buildApplyActions(
            Collections.singletonList(actionResource));

        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getId()).isEqualTo(ActionId.EXECUTE_SCRIPT);
        // 只产出实际携带的 host 类型，script 类型被跳过
        assertThat(actions.get(0).getRelatedResourceTypes()).hasSize(1);
        assertThat(actions.get(0).getRelatedResourceTypes().get(0).getType())
            .isEqualTo(ResourceTypeEnum.HOST.getId());
    }

    @Test
    @DisplayName("v3 完整资源：两种资源类型都按 Actions 声明的顺序产出")
    void keepAllResourceTypesWhenPresent() {
        PermissionResource scriptResource = new PermissionResource();
        scriptResource.setSystemId(SystemId.JOB);
        scriptResource.setResourceId("s1");
        scriptResource.setResourceType(ResourceTypeEnum.SCRIPT);
        scriptResource.setResourceName("script-1");

        PermissionResource hostResource = new PermissionResource();
        hostResource.setSystemId(SystemId.CMDB);
        hostResource.setResourceId("100");
        hostResource.setResourceType(ResourceTypeEnum.HOST);
        hostResource.setResourceName("127.0.0.1");

        PermissionActionResource actionResource = new PermissionActionResource();
        actionResource.setActionId(ActionId.EXECUTE_SCRIPT);
        actionResource.addResource(scriptResource);
        actionResource.addResource(hostResource);

        List<ActionDTO> actions = IamV3ApplyActionBuilder.buildApplyActions(
            Collections.singletonList(actionResource));

        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getRelatedResourceTypes()).hasSize(2);
        assertThat(actions.get(0).getRelatedResourceTypes())
            .extracting(relatedResourceType -> relatedResourceType.getType())
            .containsExactly(ResourceTypeEnum.SCRIPT.getId(), ResourceTypeEnum.HOST.getId());
    }
}
