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
import com.tencent.bk.job.manage.auth.AccountAuthService;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 账号相关操作鉴权接口
 */
@Slf4j
@Service
public class AccountAuthServiceImpl implements AccountAuthService {

    private final AuthService authService;
    private final AppAuthService appAuthService;

    @Autowired
    public AccountAuthServiceImpl(AuthService authService,
                                  AppAuthService appAuthService) {
        this.authService = authService;
        this.appAuthService = appAuthService;
    }

    @Override
    public AuthResult authCreateAccount(String username, AppResourceScope appResourceScope) {
        return appAuthService.auth(false, username, ActionId.CREATE_ACCOUNT, appResourceScope);
    }

    private PathInfoDTO buildAppScopePath(AppResourceScope appResourceScope) {
        return PathBuilder.newBuilder(IamUtil.getIamResourceTypeIdForResourceScope(appResourceScope),
            appResourceScope.getId()).build();
    }

    @Override
    public AuthResult authManageAccount(String username,
                                        AppResourceScope appResourceScope,
                                        Long accountId,
                                        String accountName) {
        return authService.auth(
            true, username,
            ActionId.MANAGE_ACCOUNT, ResourceTypeEnum.ACCOUNT, accountId.toString(),
            buildAppScopePath(appResourceScope));
    }

    @Override
    public AuthResult authUseAccount(String username,
                                     AppResourceScope appResourceScope,
                                     Long accountId,
                                     String accountName) {
        return authService.auth(
            true, username,
            ActionId.USE_ACCOUNT, ResourceTypeEnum.ACCOUNT, accountId.toString(),
            buildAppScopePath(appResourceScope));
    }

    @Override
    public List<Long> batchAuthManageAccount(String username,
                                             AppResourceScope appResourceScope,
                                             List<Long> accountIdList) {
        List<String> allowIdList = appAuthService.batchAuth(username, ActionId.MANAGE_ACCOUNT, appResourceScope,
            ResourceTypeEnum.ACCOUNT,
            accountIdList.parallelStream().map(Object::toString).collect(Collectors.toList()));
        return allowIdList.parallelStream().map(Long::valueOf).collect(Collectors.toList());
    }

    @Override
    public List<Long> batchAuthUseAccount(String username,
                                          AppResourceScope appResourceScope,
                                          List<Long> accountIdList) {
        List<String> allowIdList = appAuthService.batchAuth(username, ActionId.USE_ACCOUNT, appResourceScope,
            ResourceTypeEnum.ACCOUNT,
            accountIdList.parallelStream().map(Object::toString).collect(Collectors.toList()));
        return allowIdList.parallelStream().map(Long::valueOf).collect(Collectors.toList());
    }

    @Override
    public boolean registerAccount(String creator, Long id, String name) {
        return authService.registerResource(id.toString(), name, ResourceTypeId.ACCOUNT, creator, null);
    }
}
