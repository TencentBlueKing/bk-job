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
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.iam.util.IamUtil;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.manage.auth.TemplateAuthService;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 作业模板相关操作鉴权接口
 */
@Service
public class TemplateAuthServiceImpl implements TemplateAuthService {

    private final AuthService authService;
    private final AppAuthService appAuthService;

    @Autowired
    public TemplateAuthServiceImpl(AuthService authService,
                                   AppAuthService appAuthService) {
        this.authService = authService;
        this.appAuthService = appAuthService;
    }

    private PathInfoDTO buildAppScopePath(AppResourceScope appResourceScope) {
        return PathBuilder.newBuilder(IamUtil.getIamResourceTypeIdForResourceScope(appResourceScope),
            appResourceScope.getId()).build();
    }

    @Override
    public AuthResult authCreateJobTemplate(User user, AppResourceScope appResourceScope) {
        return appAuthService.auth(user, ActionId.CREATE_JOB_TEMPLATE, appResourceScope);
    }

    @Override
    public AuthResult authViewJobTemplate(User user,
                                          AppResourceScope appResourceScope,
                                          Long jobTemplateId) {
        return authService.auth(
            user,
            ActionId.VIEW_JOB_TEMPLATE,
            ResourceTypeEnum.TEMPLATE,
            jobTemplateId.toString(),
            buildAppScopePath(appResourceScope)
        );
    }

    @Override
    public AuthResult authEditJobTemplate(User user,
                                          AppResourceScope appResourceScope,
                                          Long jobTemplateId) {
        return authService.auth(
            user,
            ActionId.EDIT_JOB_TEMPLATE,
            ResourceTypeEnum.TEMPLATE,
            jobTemplateId.toString(),
            buildAppScopePath(appResourceScope)
        );
    }

    @Override
    public AuthResult authDeleteJobTemplate(User user,
                                            AppResourceScope appResourceScope,
                                            Long jobTemplateId) {
        return authService.auth(
            user,
            ActionId.DELETE_JOB_TEMPLATE,
            ResourceTypeEnum.TEMPLATE,
            jobTemplateId.toString(),
            buildAppScopePath(appResourceScope)
        );
    }

    @Override
    public List<Long> batchAuthViewJobTemplate(User user,
                                               AppResourceScope appResourceScope,
                                               List<Long> jobTemplateIdList) {
        List<String> allowedIdList = appAuthService.batchAuth(user, ActionId.VIEW_JOB_TEMPLATE,
            appResourceScope, ResourceTypeEnum.TEMPLATE,
            jobTemplateIdList.stream().map(Object::toString).collect(Collectors.toList()));
        return allowedIdList.stream().map(Long::valueOf).collect(Collectors.toList());
    }

    @Override
    public List<Long> batchAuthEditJobTemplate(User user,
                                               AppResourceScope appResourceScope,
                                               List<Long> jobTemplateIdList) {
        List<String> allowedIdList = appAuthService.batchAuth(
            user,
            ActionId.EDIT_JOB_TEMPLATE,
            appResourceScope,
            ResourceTypeEnum.TEMPLATE,
            jobTemplateIdList.stream().map(Object::toString).collect(Collectors.toList())
        );
        return allowedIdList.stream().map(Long::valueOf).collect(Collectors.toList());
    }

    @Override
    public AuthResult batchAuthResultEditJobTemplate(User user,
                                                     AppResourceScope appResourceScope,
                                                     List<Long> jobTemplateIdList) {
        List<PermissionResource> resources = jobTemplateIdList.stream().map(templateId -> {
            PermissionResource resource = new PermissionResource();
            resource.setResourceId(templateId.toString());
            resource.setResourceType(ResourceTypeEnum.TEMPLATE);
            resource.setPathInfo(buildAppScopePath(appResourceScope));
            return resource;
        }).collect(Collectors.toList());
        return appAuthService.batchAuthResources(user, ActionId.EDIT_JOB_TEMPLATE, appResourceScope, resources);
    }

    @Override
    public List<Long> batchAuthDeleteJobTemplate(User user,
                                                 AppResourceScope appResourceScope,
                                                 List<Long> jobTemplateIdList) {
        List<String> allowedIdList = appAuthService.batchAuth(user, ActionId.DELETE_JOB_TEMPLATE,
            appResourceScope, ResourceTypeEnum.TEMPLATE,
            jobTemplateIdList.stream().map(Object::toString).collect(Collectors.toList()));
        return allowedIdList.stream().map(Long::valueOf).collect(Collectors.toList());
    }

    @Override
    public boolean registerTemplate(User creator, Long id, String name) {
        return authService.registerResource(creator, id.toString(), name, ResourceTypeId.TEMPLATE, null);
    }
}
