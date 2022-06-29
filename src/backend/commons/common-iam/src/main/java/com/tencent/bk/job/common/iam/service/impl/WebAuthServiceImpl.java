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

package com.tencent.bk.job.common.iam.service.impl;

import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.model.PermissionActionResource;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.model.PermissionResourceGroup;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.permission.AuthResultVO;
import com.tencent.bk.job.common.model.permission.PermissionResourceVO;
import com.tencent.bk.job.common.model.permission.RequiredPermissionVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

@Slf4j
@Service
public class WebAuthServiceImpl implements WebAuthService {
    private final MessageI18nService i18nService;
    private final AuthService authService;

    @Autowired
    public WebAuthServiceImpl(MessageI18nService i18nService, AuthService authService) {
        this.i18nService = i18nService;
        this.authService = authService;
    }

    @Override
    public AuthResultVO auth(boolean isReturnApplyUrl, String username,
                             List<PermissionActionResource> actionResources) {
        return toAuthResultVO(isReturnApplyUrl, authService.auth(isReturnApplyUrl, username, actionResources));
    }

    @Override
    public String getApplyUrl(List<PermissionActionResource> permissionActionResources) {
        return authService.getApplyUrl(permissionActionResources);
    }

    public AuthResultVO toAuthResultVO(boolean isReturnApplyUrl, AuthResult authResult) {
        AuthResultVO vo = new AuthResultVO();
        vo.setPass(authResult.isPass());
        if (!authResult.isPass()) {
            String applyUrl = authResult.getApplyUrl();
            if (isReturnApplyUrl && StringUtils.isBlank(applyUrl)) {
                applyUrl = authService.getApplyUrl(authResult.getRequiredActionResources());
            }
            vo.setApplyUrl(applyUrl);

            List<RequiredPermissionVO> requiredPermissionVOS = new ArrayList<>();
            for (PermissionActionResource actionResource : authResult.getRequiredActionResources()) {
                RequiredPermissionVO requiredPermissionVO = new RequiredPermissionVO();
                requiredPermissionVO.setActionName(
                    i18nService.getI18n("permission.action.name." + actionResource.getActionId()));
                List<PermissionResourceVO> resourceVOS = new ArrayList<>();
                actionResource.getResourceGroups().forEach(resourceGroup -> {
                    PermissionResourceVO resourceVO = new PermissionResourceVO();
                    ResourceTypeEnum resourceType = resourceGroup.getResourceType();
                    resourceVO.setResourceTypeName(
                        i18nService.getI18n("permission.resource.type.name." + resourceType.getId().toLowerCase()));
                    resourceVO.setResourceName(buildResourceName(resourceGroup));
                    resourceVOS.add(resourceVO);
                });
                requiredPermissionVO.setRelatedResources(resourceVOS);

                requiredPermissionVOS.add(requiredPermissionVO);
            }
            vo.setRequiredPermissions(requiredPermissionVOS);
        }

        return vo;
    }

    private String buildResourceName(PermissionResourceGroup resourceGroup) {
        ResourceTypeEnum resourceType = resourceGroup.getResourceType();
        if (resourceType == ResourceTypeEnum.HOST) {
            // 主机需要进行数据聚合
            int hostCount = 0;
            int topoNodeCount = 0;
            int dynamicGroupCount = 0;
            int bizSetCount = 0;
            for (PermissionResource resource : resourceGroup.getPermissionResources()) {
                String subResourceType = resource.getSubResourceType();
                switch (subResourceType) {
                    case "host":
                        hostCount++;
                        break;
                    case "topo":
                        topoNodeCount++;
                        break;
                    case "dynamic_group":
                        dynamicGroupCount++;
                        break;
                    case "biz_set":
                        bizSetCount++;
                        break;
                }
            }
            StringJoiner stringJoiner = new StringJoiner(",");
            if (hostCount > 0) {
                stringJoiner.add(hostCount + " " + i18nService.getI18n("resource.host.name"));
            }
            if (topoNodeCount > 0) {
                stringJoiner.add(topoNodeCount + " " + i18nService.getI18n("resource.topo_node.name"));
            }
            if (dynamicGroupCount > 0) {
                stringJoiner.add(dynamicGroupCount + " " + i18nService.getI18n("resource.dynamic_group.name"));
            }
            if (bizSetCount > 0) {
                stringJoiner.add(bizSetCount + " " + i18nService.getI18n("resource.biz_set.name"));
            }
            return stringJoiner.toString();
        } else {
            StringJoiner stringJoiner = new StringJoiner(",");
            resourceGroup.getPermissionResources().forEach(resource -> stringJoiner.add(resource.getResourceName()));
            return stringJoiner.toString();
        }
    }
}
