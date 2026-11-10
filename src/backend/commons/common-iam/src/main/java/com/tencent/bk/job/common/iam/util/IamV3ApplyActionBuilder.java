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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.iam.constant.ActionInfo;
import com.tencent.bk.job.common.iam.constant.Actions;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.PermissionActionResource;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.model.PermissionResourceGroup;
import com.tencent.bk.job.common.util.CustomCollectionUtils;
import com.tencent.bk.sdk.iam.dto.InstanceDTO;
import com.tencent.bk.sdk.iam.dto.action.ActionDTO;
import com.tencent.bk.sdk.iam.dto.resource.RelatedResourceTypeDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 把 bk-job 的 {@link PermissionActionResource} 列表转换为 IAM v3 申请所需的 {@link ActionDTO} 列表。
 * 供 v3 申请 URL 生成与 OpenAPI 无权限响应格式化复用。
 */
@Slf4j
public class IamV3ApplyActionBuilder {

    private IamV3ApplyActionBuilder() {
    }

    public static List<ActionDTO> buildApplyActions(List<PermissionActionResource> permissionActionResources) {
        Map<String, Map<String, List<PermissionResource>>> resourcesGroupByActionAndType =
            groupResourcesByActionAndResourceType(permissionActionResources);

        List<ActionDTO> actions = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<PermissionResource>>> entry :
            resourcesGroupByActionAndType.entrySet()) {
            String actionId = entry.getKey();
            Map<String, List<PermissionResource>> resourceGroups = entry.getValue();
            ActionDTO action = new ActionDTO();
            action.setId(actionId);
            actions.add(action);

            if (resourceGroups == null || resourceGroups.isEmpty()) {
                continue;
            }

            ActionInfo actionInfo = Actions.getActionInfo(actionId);
            if (actionInfo == null) {
                log.error("Invalid Action, actionId: {}", actionId);
                throw new InternalException(ErrorCode.INTERNAL_ERROR);
            }

            List<RelatedResourceTypeDTO> relatedResourceTypes = new ArrayList<>();
            List<ResourceTypeEnum> actionRelatedResourceTypes = actionInfo.getRelatedResourceTypes();
            if (CollectionUtils.isEmpty(actionRelatedResourceTypes)) {
                action.setRelatedResourceTypes(relatedResourceTypes);
                continue;
            }
            // IAM 鉴权API对于依赖资源类型的顺序有要求，需要按照注册资源时候的顺序
            for (ResourceTypeEnum resourceType : actionRelatedResourceTypes) {
                List<PermissionResource> relatedResources = resourceGroups.get(resourceType.getId());
                if (CollectionUtils.isEmpty(relatedResources)) {
                    // v4 下每个操作只关联单一资源类型，鉴权失败时携带的资源会少于 Actions（v3 模型）声明的类型，
                    // 例如 execute_script 只带 host 而不带 script。此处跳过缺失的类型，保证仍能产出可用的
                    // 无权限详情，而不是让整个响应失败
                    log.info("No resource of type {} for action {}, skip it when building apply actions",
                        resourceType.getId(), actionId);
                    continue;
                }
                RelatedResourceTypeDTO relatedResourceType = new RelatedResourceTypeDTO();
                String systemId = relatedResources.get(0).getSystemId();
                relatedResourceType.setSystemId(systemId);
                relatedResourceType.setType(resourceType.getId());
                List<List<InstanceDTO>> instanceList = new ArrayList<>();
                for (PermissionResource relatedResource : relatedResources) {
                    InstanceDTO instance = IamV3InstanceBuilder.convertPermissionResourceToInstance(relatedResource);
                    List<PermissionResource> parentResources = relatedResource.getParentHierarchicalResources();
                    if (CollectionUtils.isEmpty(parentResources) && relatedResource.getPathInfo() != null) {
                        parentResources = IamResourcePathSupport.parentResources(
                            relatedResource.getPathInfo(), relatedResource);
                    }
                    if (!CustomCollectionUtils.isEmptyCollection(parentResources)) {
                        List<InstanceDTO> hierarchicalInstance = new ArrayList<>();
                        parentResources.forEach(resource -> hierarchicalInstance.add(
                            IamV3InstanceBuilder.convertPermissionResourceToInstance(resource)));
                        hierarchicalInstance.add(instance);
                        instanceList.add(hierarchicalInstance);
                    } else {
                        instanceList.add(Collections.singletonList(instance));
                    }
                }
                relatedResourceType.setInstance(instanceList);
                relatedResourceTypes.add(relatedResourceType);
            }
            action.setRelatedResourceTypes(relatedResourceTypes);
        }
        return actions;
    }

    private static Map<String, Map<String, List<PermissionResource>>> groupResourcesByActionAndResourceType(
        List<PermissionActionResource> permissionActionResources) {
        Map<String, Map<String, List<PermissionResource>>> resourcesGroupByActionAndType = new HashMap<>();
        permissionActionResources.forEach(actionResource ->
            resourcesGroupByActionAndType.compute(actionResource.getActionId(), (actionId, resourcesGroupByType) -> {
                if (resourcesGroupByType == null) {
                    resourcesGroupByType = new HashMap<>();
                }
                for (PermissionResourceGroup resourceGroup : actionResource.getResourceGroups()) {
                    resourcesGroupByType.compute(resourceGroup.getResourceType().getId(), (resourceType, resources) -> {
                        if (resources == null) {
                            resources = new ArrayList<>();
                        }
                        resources.addAll(resourceGroup.getPermissionResources());
                        return resources;
                    });
                }
                return resourcesGroupByType;
            }));
        return resourcesGroupByActionAndType;
    }
}
