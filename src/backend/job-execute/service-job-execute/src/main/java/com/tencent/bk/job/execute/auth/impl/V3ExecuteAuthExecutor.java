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

package com.tencent.bk.job.execute.auth.impl;

import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.execute.auth.ExecuteAuthDecision;
import com.tencent.bk.job.execute.auth.ExecuteAuthExecutor;
import com.tencent.bk.job.execute.auth.ExecuteAuthResource;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.sdk.iam.constants.SystemId;
import com.tencent.bk.sdk.iam.dto.InstanceDTO;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.helper.AuthHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 执行类鉴权的 v3 执行器：沿用 v3 语义，把「关联资源 + 执行目标主机」放进同一次
 * {@link AuthHelper#isAllowed} 调用，资源维度的可见性由 IAM v3 模型的依赖操作保证。
 *
 * <p>本类是原 {@code ExecuteAuthServiceImpl} 中 v3 逻辑的原样抽取，行为不变。</p>
 */
@Slf4j
public class V3ExecuteAuthExecutor implements ExecuteAuthExecutor {

    private final AuthHelper authHelper;
    private final ExecuteTargetInstanceBuilder executeTargetInstanceBuilder;

    public V3ExecuteAuthExecutor(AuthHelper authHelper,
                                 ExecuteTargetInstanceBuilder executeTargetInstanceBuilder) {
        this.authHelper = authHelper;
        this.executeTargetInstanceBuilder = executeTargetInstanceBuilder;
    }

    @Override
    public ExecuteAuthDecision authExecute(User user,
                                           String actionId,
                                           AppResourceScope appResourceScope,
                                           ExecuteAuthResource resource,
                                           ExecuteTargetDTO executeTarget) {
        List<InstanceDTO> hostInstanceList =
            executeTargetInstanceBuilder.buildHostInstances(appResourceScope, executeTarget);
        InstanceDTO resourceInstance = resource == null
            ? null : buildResourceInstance(appResourceScope, resource);

        if (log.isDebugEnabled()) {
            log.debug("Auth execute by iam v3, username:{}, actionId:{}, appResourceScope:{}, "
                    + "resourceInstance:{}, hostInstances:{}",
                user.getUsername(), actionId, appResourceScope, resourceInstance, hostInstanceList);
        }
        boolean isAllowed = authHelper.isAllowed(
            user.getTenantId(), user.getUsername(), actionId, resourceInstance, hostInstanceList);
        if (isAllowed) {
            return ExecuteAuthDecision.pass();
        }
        // v3 单次调用无法区分是资源维度还是执行目标维度未通过
        return ExecuteAuthDecision.fail(actionId, ExecuteAuthDecision.FailureScope.BOTH);
    }

    private InstanceDTO buildResourceInstance(AppResourceScope appResourceScope, ExecuteAuthResource resource) {
        PathInfoDTO pathInfo = resource.getParentResourceType() == null
            ? ExecuteIamPathBuilder.buildAppScopePath(appResourceScope)
            : ExecuteIamPathBuilder.buildAppScopeResourcePath(
                appResourceScope, resource.getParentResourceType(), resource.getParentResourceId());

        InstanceDTO instance = new InstanceDTO();
        instance.setSystem(SystemId.JOB);
        instance.setType(resource.getResourceType().getId());
        instance.setId(resource.getResourceId());
        instance.setPath(pathInfo);
        return instance;
    }
}
