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

package com.tencent.bk.job.manage.auth.impl;

import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.iam.util.IamUtil;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.manage.auth.ScriptAuthService;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 脚本相关操作鉴权接口
 */
@Service
public class ScriptAuthServiceImpl implements ScriptAuthService {

    private final AuthService authService;
    private final AppAuthService appAuthService;

    @Autowired
    public ScriptAuthServiceImpl(AuthService authService,
                                 AppAuthService appAuthService) {
        this.authService = authService;
        this.appAuthService = appAuthService;
    }

    private PathInfoDTO buildAppScopePath(AppResourceScope appResourceScope) {
        return PathBuilder.newBuilder(IamUtil.getIamResourceTypeIdForResourceScope(appResourceScope),
            appResourceScope.getId()).build();
    }

    @Override
    public AuthResult authCreateScript(String username, AppResourceScope appResourceScope) {
        return appAuthService.auth(true, username, ActionId.CREATE_SCRIPT, appResourceScope);
    }

    @Override
    public AuthResult authViewScript(String username,
                                     AppResourceScope appResourceScope,
                                     String scriptId,
                                     String scriptName) {
        return authService.auth(true, username, ActionId.VIEW_SCRIPT, ResourceTypeEnum.SCRIPT, scriptId,
            buildAppScopePath(appResourceScope));
    }

    @Override
    public AuthResult authManageScript(String username,
                                       AppResourceScope appResourceScope,
                                       String scriptId,
                                       String scriptName) {
        return authService.auth(true, username, ActionId.MANAGE_SCRIPT, ResourceTypeEnum.SCRIPT, scriptId,
            buildAppScopePath(appResourceScope));
    }

    @Override
    public List<String> batchAuthViewScript(String username,
                                            AppResourceScope appResourceScope,
                                            List<String> scriptIdList) {
        return appAuthService.batchAuth(username, ActionId.VIEW_SCRIPT, appResourceScope,
            ResourceTypeEnum.SCRIPT, scriptIdList);
    }

    @Override
    public List<String> batchAuthManageScript(String username,
                                              AppResourceScope appResourceScope,
                                              List<String> scriptIdList) {
        return appAuthService.batchAuth(username, ActionId.MANAGE_SCRIPT, appResourceScope,
            ResourceTypeEnum.SCRIPT, scriptIdList);
    }

    @Override
    public AuthResult batchAuthResultManageScript(String username, AppResourceScope appResourceScope,
                                                  List<String> scriptIdList) {
        List<PermissionResource> resources = convertToPermissionResources(appResourceScope, scriptIdList);
        return appAuthService.batchAuthResources(username, ActionId.MANAGE_SCRIPT, appResourceScope, resources);
    }

    @Override
    public AuthResult batchAuthResultViewScript(String username, AppResourceScope appResourceScope,
                                                List<String> scriptIdList) {
        List<PermissionResource> resources = convertToPermissionResources(appResourceScope, scriptIdList);
        return appAuthService.batchAuthResources(username, ActionId.VIEW_SCRIPT, appResourceScope, resources);
    }

    private List<PermissionResource> convertToPermissionResources(AppResourceScope appResourceScope,
                                                                  List<String> scriptIdList) {
        return scriptIdList.stream().map(scriptId -> {
            PermissionResource resource = new PermissionResource();
            resource.setResourceId(scriptId);
            resource.setResourceType(ResourceTypeEnum.SCRIPT);
            resource.setPathInfo(buildAppScopePath(appResourceScope));
            return resource;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean registerScript(String id, String name, String creator) {
        return authService.registerResource(id, name, ResourceTypeId.SCRIPT, creator, null);
    }
}
