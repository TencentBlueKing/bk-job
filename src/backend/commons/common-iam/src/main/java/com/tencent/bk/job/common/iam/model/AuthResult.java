/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.common.iam.model;

import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.model.iam.AuthResultDTO;
import com.tencent.bk.job.common.model.iam.PathInfoDTO;
import com.tencent.bk.job.common.model.iam.PermissionActionResourceDTO;
import com.tencent.bk.job.common.model.iam.PermissionResourceDTO;
import com.tencent.bk.job.common.model.iam.PermissionResourceGroupDTO;
import com.tencent.bk.job.common.util.CustomCollectionUtils;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 鉴权结果
 */
@Data
public class AuthResult {
    /**
     * 是否鉴权通过
     */
    private boolean pass;
    /**
     * 权限申请url
     */
    private String applyUrl;
    /**
     * 依赖的权限操作与资源
     */
    private List<com.tencent.bk.job.common.iam.model.PermissionActionResource> requiredActionResources =
        new ArrayList<>();

    public static AuthResult pass() {
        AuthResult authResult = new AuthResult();
        authResult.pass = true;
        return authResult;
    }

    public static AuthResult fail() {
        AuthResult authResult = new AuthResult();
        authResult.pass = false;
        return authResult;
    }

    /**
     * 添加依赖的操作与资源
     *
     * @param actionId           操作ID
     * @param permissionResource 资源
     */
    public void addRequiredPermission(String actionId, PermissionResource permissionResource) {
        this.pass = false;

        if (permissionResource == null) {
            com.tencent.bk.job.common.iam.model.PermissionActionResource actionResource =
                new com.tencent.bk.job.common.iam.model.PermissionActionResource();
            actionResource.setActionId(actionId);
            this.requiredActionResources.add(actionResource);
            return;
        }

        com.tencent.bk.job.common.iam.model.PermissionActionResource actionResource =
            createOrGetPermissionActionResource(actionId);

        PermissionResourceGroup resourceGroup = null;
        for (PermissionResourceGroup existedResourceGroup : actionResource.getResourceGroups()) {
            if (existedResourceGroup.getResourceType() == permissionResource.getResourceType()) {
                resourceGroup = existedResourceGroup;
            }
        }
        if (resourceGroup == null) {
            resourceGroup = new PermissionResourceGroup();
            resourceGroup.setResourceType(permissionResource.getResourceType());
            resourceGroup.setSystemId(permissionResource.getSystemId());
            actionResource.addPermissionResourceGroup(resourceGroup);
        }
        resourceGroup.addPermissionResource(permissionResource);
    }

    /**
     * 添加依赖的操作与资源
     *
     * @param actionId            操作ID
     * @param permissionResources 资源列表
     */
    public void addRequiredPermissions(String actionId, List<PermissionResource> permissionResources) {
        if (CustomCollectionUtils.isEmptyCollection(permissionResources)) {
            return;
        }
        this.pass = false;

        com.tencent.bk.job.common.iam.model.PermissionActionResource actionResource =
            createOrGetPermissionActionResource(actionId);

        PermissionResourceGroup resourceGroup = null;
        ResourceTypeEnum resourceType = permissionResources.get(0).getResourceType();
        for (PermissionResourceGroup existedResourceGroup : actionResource.getResourceGroups()) {
            if (existedResourceGroup.getResourceType() == resourceType) {
                resourceGroup = existedResourceGroup;
            }
        }
        if (resourceGroup == null) {
            resourceGroup = new PermissionResourceGroup();
            resourceGroup.setResourceType(resourceType);
            resourceGroup.setSystemId(permissionResources.get(0).getSystemId());
            actionResource.addPermissionResourceGroup(resourceGroup);
        }
        resourceGroup.addPermissionResources(permissionResources);
    }

    private com.tencent.bk.job.common.iam.model.PermissionActionResource createOrGetPermissionActionResource(
        String actionId) {
        com.tencent.bk.job.common.iam.model.PermissionActionResource actionResource = null;
        for (com.tencent.bk.job.common.iam.model.PermissionActionResource existedActionResource :
            requiredActionResources) {
            if (existedActionResource.getActionId().equals(actionId)) {
                actionResource = existedActionResource;
                break;
            }
        }
        if (actionResource == null) {
            actionResource = new com.tencent.bk.job.common.iam.model.PermissionActionResource();
            actionResource.setActionId(actionId);
            requiredActionResources.add(actionResource);
        }
        return actionResource;
    }

    /**
     * 对2个鉴权结果进行合并处理
     *
     * @param otherAuthResult 其他鉴权结果
     * @return 合并后的鉴权结果
     */
    public AuthResult mergeAuthResult(AuthResult otherAuthResult) {
        AuthResult authResult = new AuthResult();
        boolean isPass = this.pass && otherAuthResult.isPass();
        authResult.setPass(isPass);
        if (!isPass) {
            List<com.tencent.bk.job.common.iam.model.PermissionActionResource> requiredActionResourceList =
                new ArrayList<>();
            if (this.requiredActionResources != null && !this.requiredActionResources.isEmpty()) {
                requiredActionResourceList.addAll(this.requiredActionResources);
            }
            if (otherAuthResult.getRequiredActionResources() != null
                && !otherAuthResult.getRequiredActionResources().isEmpty()) {
                requiredActionResourceList.addAll(otherAuthResult.getRequiredActionResources());
            }
            authResult.setRequiredActionResources(requiredActionResourceList);
        }
        return authResult;
    }

    public static AuthResultDTO toAuthResultDTO(AuthResult authResult) {
        AuthResultDTO result = new AuthResultDTO();
        result.setPass(authResult.isPass());
        result.setApplyUrl(authResult.getApplyUrl());

        List<PermissionActionResourceDTO> actionResourcesDTOS = new ArrayList<>();
        for (com.tencent.bk.job.common.iam.model.PermissionActionResource actionResource :
            authResult.getRequiredActionResources()) {
            PermissionActionResourceDTO actionResourceDTO = new PermissionActionResourceDTO();
            actionResourceDTO.setActionId(actionResource.getActionId());

            if (CollectionUtils.isNotEmpty(actionResource.getResourceGroups())) {
                List<PermissionResourceGroupDTO> resourceGroupDTOS = new ArrayList<>();
                for (PermissionResourceGroup resourceGroup : actionResource.getResourceGroups()) {
                    PermissionResourceGroupDTO resourceGroupDTO = new PermissionResourceGroupDTO();
                    resourceGroupDTO.setResourceType(resourceGroup.getResourceType().getId());
                    resourceGroupDTO.setSystemId(resourceGroup.getSystemId());
                    if (CollectionUtils.isNotEmpty(resourceGroup.getPermissionResources())) {
                        List<PermissionResourceDTO> resourceDTOS = new ArrayList<>();
                        for (PermissionResource resource : resourceGroup.getPermissionResources()) {
                            resourceDTOS.add(toPermissionResourceDTO(resource));
                        }
                        resourceGroupDTO.setPermissionResources(resourceDTOS);
                    }
                    resourceGroupDTOS.add(resourceGroupDTO);
                }
                actionResourceDTO.setResourceGroups(resourceGroupDTOS);
            }
            actionResourcesDTOS.add(actionResourceDTO);
        }
        result.setRequiredActionResources(actionResourcesDTOS);
        return result;
    }

    private static PermissionResourceDTO toPermissionResourceDTO(PermissionResource resource) {
        PermissionResourceDTO resourceDTO = new PermissionResourceDTO();
        resourceDTO.setResourceId(resource.getResourceId());
        resourceDTO.setResourceType(resource.getType());
        resourceDTO.setResourceName(resource.getResourceName());
        resourceDTO.setSubResourceType(resource.getSubResourceType());
        resourceDTO.setSystemId(resource.getSystemId());
        resourceDTO.setPathInfo(toPathInfoDTO(resource.getPathInfo()));
        if (CollectionUtils.isNotEmpty(resource.getParentHierarchicalResources())) {
            resourceDTO.setParentHierarchicalResources(
                resource.getParentHierarchicalResources().stream().map(AuthResult::toPermissionResourceDTO)
                    .collect(Collectors.toList()));
        }
        return resourceDTO;
    }

    private static PathInfoDTO toPathInfoDTO(com.tencent.bk.sdk.iam.dto.PathInfoDTO pathInfo) {
        if (pathInfo == null) {
            return null;
        }
        PathBuilder pathBuilder = PathBuilder.newBuilder(pathInfo.getType(), pathInfo.getId());

        com.tencent.bk.sdk.iam.dto.PathInfoDTO child = pathInfo.getChild();
        while (child != null) {
            pathBuilder.child(child.getType(), child.getId());
            child = child.getChild();
        }

        return pathBuilder.build();
    }

    private static class PathBuilder {
        private final String id;
        private final String type;
        private PathBuilder child;
        private PathBuilder head;

        private PathBuilder(String type, String id) {
            this.type = type;
            this.id = id;
            this.head = this;
        }

        private PathBuilder(String type, String id, PathBuilder head) {
            this.type = type;
            this.id = id;
            this.head = head;
        }

        public static PathBuilder newBuilder(String type, String id) {
            return new PathBuilder(type, id);
        }

        public PathBuilder head(PathBuilder head) {
            this.head = head;
            return this;
        }

        public PathBuilder child(String type, String id) {
            this.child = new PathBuilder(type, id, this.head);
            return this.child;
        }

        public PathInfoDTO build() {
            if (this.head == this) {
                PathInfoDTO pathInfo = new PathInfoDTO(this.id, this.type);
                if (this.child != null) {
                    pathInfo.setChild(this.child.head(this.child).build());
                }

                return pathInfo;
            } else {
                return this.head.build();
            }
        }
    }

    public static AuthResult fromAuthResultDTO(AuthResultDTO authResultDTO) {
        AuthResult result = new AuthResult();
        result.setPass(authResultDTO.isPass());
        result.setApplyUrl(authResultDTO.getApplyUrl());

        List<PermissionActionResource> actionResources = new ArrayList<>();
        for (PermissionActionResourceDTO actionResourceDTO : authResultDTO.getRequiredActionResources()) {
            PermissionActionResource actionResource = new PermissionActionResource();
            actionResource.setActionId(actionResourceDTO.getActionId());

            if (CollectionUtils.isNotEmpty(actionResourceDTO.getResourceGroups())) {
                List<PermissionResourceGroup> resourceGroups = new ArrayList<>();
                for (PermissionResourceGroupDTO resourceGroupDTO : actionResourceDTO.getResourceGroups()) {
                    PermissionResourceGroup resourceGroup = new PermissionResourceGroup();
                    resourceGroup.setResourceType(ResourceTypeEnum.getByResourceTypeId(resourceGroupDTO.getResourceType()));
                    resourceGroup.setSystemId(resourceGroupDTO.getSystemId());
                    if (CollectionUtils.isNotEmpty(resourceGroup.getPermissionResources())) {
                        List<PermissionResource> resources = new ArrayList<>();
                        for (PermissionResourceDTO resourceDTO : resourceGroupDTO.getPermissionResources()) {
                            resources.add(toPermissionResource(resourceDTO));
                        }
                        resourceGroup.setPermissionResources(resources);
                    }
                    resourceGroups.add(resourceGroup);
                }
                actionResource.setResourceGroups(resourceGroups);
            }
            actionResources.add(actionResource);
        }
        result.setRequiredActionResources(actionResources);
        return result;
    }

    private static PermissionResource toPermissionResource(PermissionResourceDTO resourceDTO) {
        PermissionResource resource = new PermissionResource();
        resource.setResourceId(resourceDTO.getResourceId());
        resource.setResourceType(ResourceTypeEnum.getByResourceTypeId(resourceDTO.getResourceType()));
        resource.setResourceName(resourceDTO.getResourceName());
        resource.setSubResourceType(resourceDTO.getSubResourceType());
        resource.setSystemId(resourceDTO.getSystemId());
        resource.setPathInfo(toIamPathInfoDTO(resourceDTO.getPathInfo()));
        if (CollectionUtils.isNotEmpty(resourceDTO.getParentHierarchicalResources())) {
            resource.setParentHierarchicalResources(
                resourceDTO.getParentHierarchicalResources().stream().map(AuthResult::toPermissionResource)
                    .collect(Collectors.toList()));
        }
        return resource;
    }

    private static com.tencent.bk.sdk.iam.dto.PathInfoDTO toIamPathInfoDTO(PathInfoDTO pathInfo) {
        if (pathInfo == null) {
            return null;
        }
        com.tencent.bk.sdk.iam.util.PathBuilder pathBuilder =
            com.tencent.bk.sdk.iam.util.PathBuilder.newBuilder(pathInfo.getType(), pathInfo.getId());

        PathInfoDTO child = pathInfo.getChild();
        while (child != null) {
            pathBuilder.child(child.getType(), child.getId());
            child = child.getChild();
        }

        return pathBuilder.build();
    }

}
