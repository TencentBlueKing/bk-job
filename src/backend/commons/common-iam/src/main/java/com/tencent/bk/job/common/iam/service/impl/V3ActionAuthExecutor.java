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

import com.tencent.bk.job.common.iam.client.IIamClient;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.PermissionActionResource;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.service.ActionAuthExecutor;
import com.tencent.bk.job.common.iam.util.IamV3ApplyActionBuilder;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.dto.action.ActionDTO;
import com.tencent.bk.sdk.iam.dto.resource.RelatedResourceTypeDTO;
import com.tencent.bk.sdk.iam.dto.resource.ResourceDTO;
import com.tencent.bk.sdk.iam.helper.AuthHelper;

import java.util.Collections;
import java.util.List;

/**
 * 一般资源级鉴权 v3 执行器：走 {@link AuthHelper}，构造 v3 的 InstanceDTO。
 */
public class V3ActionAuthExecutor extends AbstractV3AuthExecutor implements ActionAuthExecutor {

    private final IIamClient iamClient;

    public V3ActionAuthExecutor(AuthHelper authHelper, IIamClient iamClient) {
        super(authHelper);
        this.iamClient = iamClient;
    }

    @Override
    public boolean auth(User user, String actionId) {
        return authHelper.isAllowed(user.getTenantId(), user.getUsername(), actionId);
    }

    @Override
    public boolean auth(User user,
                        String actionId,
                        ResourceTypeEnum resourceType,
                        String resourceId,
                        PathInfoDTO pathInfo) {
        return authHelper.isAllowed(user.getTenantId(), user.getUsername(),
            actionId, buildInstance(resourceType, resourceId, pathInfo));
    }

    @Override
    public List<String> batchAuth(User user,
                                  String actionId,
                                  ResourceTypeEnum resourceType,
                                  List<String> resourceIdList) {
        return authHelper.isAllowed(user.getTenantId(), user.getUsername(),
            actionId, buildInstanceList(resourceType, resourceIdList));
    }

    @Override
    public List<String> listAllowedResourceIds(User user, String actionId, List<PermissionResource> resources) {
        return authHelper.isAllowed(user.getTenantId(), user.getUsername(),
            actionId, buildInstanceList(resources));
    }

    @Override
    public String getApplyUrl(String actionId, ResourceTypeEnum resourceType) {
        RelatedResourceTypeDTO relatedResourceType = new RelatedResourceTypeDTO();
        relatedResourceType.setSystemId(resourceType.getSystemId());
        relatedResourceType.setType(resourceType.getId());
        relatedResourceType.setInstance(Collections.emptyList());

        ActionDTO action = new ActionDTO();
        action.setId(actionId);
        action.setRelatedResourceTypes(Collections.singletonList(relatedResourceType));
        return iamClient.getApplyUrl(Collections.singletonList(action));
    }

    @Override
    public String getApplyUrl(String tenantId, List<PermissionActionResource> permissionActionResources) {
        return iamClient.getApplyUrl(IamV3ApplyActionBuilder.buildApplyActions(permissionActionResources));
    }

    @Override
    public boolean registerResource(User creator, String id, String name, String type, List<ResourceDTO> ancestors) {
        return iamClient.registerResource(id, name, type, creator.getUsername(), ancestors);
    }
}
