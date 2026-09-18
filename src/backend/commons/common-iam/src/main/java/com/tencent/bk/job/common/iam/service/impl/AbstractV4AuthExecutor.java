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

import com.tencent.bk.job.common.iam.model.PermissionActionResource;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.model.PermissionResourceGroup;
import com.tencent.bk.job.common.iam.util.IamV4ResourceTypeMapper;
import com.tencent.bk.sdk.iam.constants.AttributeKey;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iamv4.dto.V4AncestorDTO;
import com.tencent.bk.sdk.iamv4.dto.V4ApplyResourceDTO;
import com.tencent.bk.sdk.iamv4.dto.V4AuthResourceDTO;
import com.tencent.bk.sdk.iamv4.dto.V4PermissionDTO;
import com.tencent.bk.sdk.iamv4.service.IamV4AuthService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * IAM v4 鉴权执行器基类：持有 {@link IamV4AuthService}，并提供 v4 资源（{@link V4AuthResourceDTO}）构造。
 *
 * <p>入参中的资源类型与拓扑路径使用业务层通用的（v3）取值，构造 v4 资源时统一经
 * {@link IamV4ResourceTypeMapper} 转换为 v4 资源类型 ID。{@code _bk_iam_path_} 以路径数组承载，
 * 一个实例参与多个实例视图时可携带多条路径。</p>
 */
public abstract class AbstractV4AuthExecutor {

    protected final IamV4AuthService iamV4AuthService;

    protected AbstractV4AuthExecutor(IamV4AuthService iamV4AuthService) {
        this.iamV4AuthService = iamV4AuthService;
    }

    protected V4AuthResourceDTO buildV4Resource(String type, String id, PathInfoDTO path) {
        List<String> v4Paths = path == null ? null : Collections.singletonList(toV4Path(path));
        return buildV4ResourceWithPaths(type, id, v4Paths);
    }

    /**
     * 构造 v4 鉴权资源。
     *
     * @param type    资源类型（v3 取值，内部转换为 v4）
     * @param id      资源实例 ID
     * @param v4Paths 该实例的全部 v4 拓扑路径，可为空
     */
    protected V4AuthResourceDTO buildV4ResourceWithPaths(String type, String id, List<String> v4Paths) {
        V4AuthResourceDTO.V4AuthResourceDTOBuilder builder = V4AuthResourceDTO.builder()
            .id(id)
            .type(IamV4ResourceTypeMapper.toV4ResourceTypeId(type));
        if (CollectionUtils.isNotEmpty(v4Paths)) {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put(AttributeKey.BK_IAM_PATH, v4Paths);
            builder.attributes(attributes);
        }
        return builder.build();
    }

    /**
     * 把业务层构造的拓扑路径逐段转换为 v4 资源类型 ID，输出形如 {@code /biz,2/script,100/}。
     */
    protected String toV4Path(PathInfoDTO path) {
        StringBuilder sb = new StringBuilder();
        PathInfoDTO node = path;
        while (node != null) {
            sb.append('/')
                .append(IamV4ResourceTypeMapper.toV4ResourceTypeId(node.getType()))
                .append(',')
                .append(node.getId());
            node = node.getChild();
        }
        return sb.append('/').toString();
    }

    protected List<V4AuthResourceDTO> buildV4Resources(String type, List<String> ids, PathInfoDTO path) {
        List<V4AuthResourceDTO> resources = new ArrayList<>();
        for (String id : ids) {
            resources.add(buildV4Resource(type, id, path));
        }
        return resources;
    }

    protected List<V4AuthResourceDTO> buildV4Resources(List<PermissionResource> permissionResources) {
        List<V4AuthResourceDTO> resources = new ArrayList<>();
        for (PermissionResource resource : permissionResources) {
            resources.add(buildV4Resource(
                resolveResourceType(resource), resource.getResourceId(), resource.getPathInfo()));
        }
        return resources;
    }

    /**
     * 把 bk-job 的 {@link PermissionActionResource} 列表转换为 v4 申请权限所需的 {@link V4PermissionDTO} 列表。
     * 每个资源的父级层级资源({@code parentHierarchicalResources})映射为 v4 的 {@code ancestors}；
     * 顶层资源没有父级时不传该字段（IAM 拒绝 {@code ancestors: null}）。
     */
    protected List<V4PermissionDTO> buildV4Permissions(List<PermissionActionResource> permissionActionResources) {
        List<V4PermissionDTO> permissions = new ArrayList<>();
        for (PermissionActionResource actionResource : permissionActionResources) {
            List<V4ApplyResourceDTO> resources = new ArrayList<>();
            for (PermissionResourceGroup group : actionResource.getResourceGroups()) {
                for (PermissionResource resource : group.getPermissionResources()) {
                    resources.add(toV4ApplyResource(resource));
                }
            }
            permissions.add(V4PermissionDTO.builder()
                .actionId(actionResource.getActionId())
                .resources(resources)
                .build());
        }
        return permissions;
    }

    protected V4ApplyResourceDTO toV4ApplyResource(PermissionResource resource) {
        V4ApplyResourceDTO.V4ApplyResourceDTOBuilder builder = V4ApplyResourceDTO.builder()
            .id(resource.getResourceId())
            .type(IamV4ResourceTypeMapper.toV4ResourceTypeId(resolveResourceType(resource)));
        if (CollectionUtils.isNotEmpty(resource.getParentHierarchicalResources())) {
            List<V4AncestorDTO> ancestors = new ArrayList<>();
            for (PermissionResource parent : resource.getParentHierarchicalResources()) {
                ancestors.add(V4AncestorDTO.builder()
                    .id(parent.getResourceId())
                    .type(IamV4ResourceTypeMapper.toV4ResourceTypeId(resolveResourceType(parent)))
                    .build());
            }
            builder.ancestors(ancestors);
        }
        return builder.build();
    }

    private String resolveResourceType(PermissionResource resource) {
        return StringUtils.isEmpty(resource.getType())
            ? resource.getResourceType().getId() : resource.getType();
    }

    /**
     * 从批量鉴权结果中提取有权限的资源 ID 列表。
     */
    protected List<String> extractAllowedResourceIds(Map<String, Boolean> resultMap) {
        List<String> allowedIds = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : resultMap.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                allowedIds.add(entry.getKey());
            }
        }
        return allowedIds;
    }
}
