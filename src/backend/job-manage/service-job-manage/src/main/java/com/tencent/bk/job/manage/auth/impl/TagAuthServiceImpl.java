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
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.iam.util.IamUtil;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.manage.auth.TagAuthService;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签相关操作鉴权接口
 */
@Service
public class TagAuthServiceImpl implements TagAuthService {

    private final AuthService authService;
    private final AppAuthService appAuthService;

    @Autowired
    public TagAuthServiceImpl(AuthService authService,
                              AppAuthService appAuthService) {
        this.authService = authService;
        this.appAuthService = appAuthService;
    }

    private PathInfoDTO buildAppScopePath(AppResourceScope appResourceScope) {
        return PathBuilder.newBuilder(IamUtil.getIamResourceTypeIdForResourceScope(appResourceScope),
            appResourceScope.getId()).build();
    }

    @Override
    public AuthResult authCreateTag(String username, AppResourceScope appResourceScope) {
        return appAuthService.auth(username, ActionId.CREATE_TAG, appResourceScope);
    }

    @Override
    public AuthResult authManageTag(String username,
                                    AppResourceScope appResourceScope,
                                    Long tagId,
                                    String tagName) {
        return authService.auth(
            username,
            ActionId.MANAGE_TAG,
            ResourceTypeEnum.TAG,
            tagId.toString(),
            buildAppScopePath(appResourceScope)
        );
    }

    @Override
    public List<Long> batchAuthManageTag(String username,
                                         AppResourceScope appResourceScope,
                                         List<Long> tagIdList) {
        List<String> allowedIdList = appAuthService.batchAuth(username, ActionId.MANAGE_TAG,
            appResourceScope, ResourceTypeEnum.TAG,
            tagIdList.stream().map(Object::toString).collect(Collectors.toList()));
        return allowedIdList.stream().map(Long::valueOf).collect(Collectors.toList());
    }

    @Override
    public boolean registerTag(Long id, String name, String creator) {
        return authService.registerResource(id.toString(), name, ResourceTypeId.TAG, creator, null);
    }
}
