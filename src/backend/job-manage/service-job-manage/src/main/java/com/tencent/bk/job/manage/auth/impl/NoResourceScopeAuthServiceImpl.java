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
import com.tencent.bk.job.common.iam.service.AuthService;
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
    public AuthResult authCreateWhiteList(String username) {
        return authService.auth(username, ActionId.CREATE_WHITELIST);
    }

    @Override
    public AuthResult authManageWhiteList(String username) {
        return authService.auth(username, ActionId.MANAGE_WHITELIST);
    }

    @Override
    public AuthResult authCreatePublicScript(String username) {
        return authService.auth(username, ActionId.CREATE_PUBLIC_SCRIPT);
    }

    @Override
    public AuthResult authManagePublicScript(String username, String scriptId) {
        return authService.auth(
            username,
            ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE,
            ResourceTypeEnum.PUBLIC_SCRIPT,
            scriptId,
            null
        );
    }

    @Override
    public List<String> batchAuthManagePublicScript(String username, List<String> scriptIdList) {
        return authService.batchAuth(username, ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE,
            ResourceTypeEnum.PUBLIC_SCRIPT, scriptIdList);
    }

    @Override
    public AuthResult batchAuthResultManagePublicScript(String username, List<String> scriptIdList) {
        List<PermissionResource> resources = scriptIdList.stream().map(scriptId -> {
            PermissionResource resource = new PermissionResource();
            resource.setResourceId(scriptId);
            resource.setResourceType(ResourceTypeEnum.PUBLIC_SCRIPT);
            return resource;
        }).collect(Collectors.toList());
        return authService.batchAuthResources(username, ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE, resources);
    }

    @Override
    public AuthResult authGlobalSetting(String username) {
        return authService.auth(username, ActionId.GLOBAL_SETTINGS);
    }

    @Override
    public AuthResult authViewDashBoard(String username, String dashBoardId) {
        return authService.auth(
            username,
            ActionId.DASHBOARD_VIEW,
            ResourceTypeEnum.DASHBOARD_VIEW,
            dashBoardId,
            null
        );
    }

    @Override
    public AuthResult authViewServiceState(String username) {
        return authService.auth(username, ActionId.SERVICE_STATE_ACCESS);
    }

    @Override
    public AuthResult authHighRiskDetectRule(String username) {
        return authService.auth(username, ActionId.HIGH_RISK_DETECT_RULE);
    }

    @Override
    public AuthResult authHighRiskDetectRecord(String username) {
        return authService.auth(username, ActionId.HIGH_RISK_DETECT_RECORD);
    }

    @Override
    public boolean registerPublicScript(String id, String name, String creator) {
        return authService.registerResource(id, name, ResourceTypeId.PUBLIC_SCRIPT, creator, null);
    }
}
