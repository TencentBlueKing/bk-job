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

package com.tencent.bk.job.manage.auth.impl;

import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.manage.auth.NoResourceScopeAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 资源范围无关的相关操作鉴权接口实现
 */
@Service
public class NoResourceScopeAuthServiceImpl implements NoResourceScopeAuthService {

    private final AuthService authService;

    @Autowired
    public NoResourceScopeAuthServiceImpl(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public AuthResult authCreateWhiteList(User user) {
        return authService.auth(user, ActionId.CREATE_WHITELIST);
    }

    @Override
    public AuthResult authManageWhiteList(User user) {
        return authService.auth(user, ActionId.MANAGE_WHITELIST);
    }

    @Override
    public AuthResult authCreatePublicScript(User user) {
        return authService.auth(user, ActionId.CREATE_PUBLIC_SCRIPT);
    }

    @Override
    public AuthResult authManagePublicScript(User user, String scriptId) {
        return authService.auth(
            user,
            ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE,
            ResourceTypeEnum.PUBLIC_SCRIPT,
            scriptId,
            null
        );
    }

    @Override
    public List<String> batchAuthManagePublicScript(User user, List<String> scriptIdList) {
        return authService.batchAuth(user, ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE,
            ResourceTypeEnum.PUBLIC_SCRIPT, scriptIdList);
    }

    @Override
    public AuthResult batchAuthResultManagePublicScript(User user, List<String> scriptIdList) {
        List<PermissionResource> resources = scriptIdList.stream().map(scriptId -> {
            PermissionResource resource = new PermissionResource();
            resource.setResourceId(scriptId);
            resource.setResourceType(ResourceTypeEnum.PUBLIC_SCRIPT);
            return resource;
        }).collect(Collectors.toList());
        return authService.batchAuthResources(user, ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE, resources);
    }

    @Override
    public AuthResult authGlobalSetting(User user) {
        return authService.auth(user, ActionId.GLOBAL_SETTINGS);
    }

    @Override
    public AuthResult authViewDashBoard(User user, String dashBoardId) {
        return authService.auth(
            user,
            ActionId.DASHBOARD_VIEW,
            ResourceTypeEnum.DASHBOARD_VIEW,
            dashBoardId,
            null
        );
    }

    @Override
    public AuthResult authViewServiceState(User user) {
        return authService.auth(user, ActionId.SERVICE_STATE_ACCESS);
    }

    @Override
    public AuthResult authHighRiskDetectRule(User user) {
        return authService.auth(user, ActionId.HIGH_RISK_DETECT_RULE);
    }

    @Override
    public AuthResult authHighRiskDetectRecord(User user) {
        return authService.auth(user, ActionId.HIGH_RISK_DETECT_RECORD);
    }

    @Override
    public boolean registerPublicScript(User creator, String id, String name) {
        return authService.registerResource(creator, id, name, ResourceTypeId.PUBLIC_SCRIPT, null);
    }
}
