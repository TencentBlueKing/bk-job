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

package com.tencent.bk.job.crontab.auth.impl;

import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.iam.util.IamUtil;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.crontab.auth.CronAuthService;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 定时任务相关操作鉴权接口实现
 */
@Service
public class CronAuthServiceImpl implements CronAuthService {

    private final AuthService authService;
    private final AppAuthService appAuthService;

    @Autowired
    public CronAuthServiceImpl(AuthService authService,
                               AppAuthService appAuthService) {
        this.authService = authService;
        this.appAuthService = appAuthService;
    }

    private PathInfoDTO buildScopeResourcePath(AppResourceScope appResourceScope,
                                               String resourceId) {
        return PathBuilder.newBuilder(IamUtil.getIamResourceTypeIdForResourceScope(appResourceScope),
            appResourceScope.getId()).child(ResourceTypeEnum.CRON.getId(), resourceId).build();
    }

    @Override
    public AuthResult authCreateCron(String username, AppResourceScope appResourceScope) {
        return appAuthService.auth(username, ActionId.CREATE_CRON, appResourceScope);
    }

    @Override
    public AuthResult authManageCron(String username,
                                     AppResourceScope appResourceScope,
                                     Long cronId,
                                     String cronName) {
        return authService.auth(
            username,
            ActionId.MANAGE_CRON,
            ResourceTypeEnum.CRON,
            cronId.toString(),
            buildScopeResourcePath(appResourceScope, cronId.toString())
        );
    }

    @Override
    public List<Long> batchAuthManageCron(String username,
                                          AppResourceScope appResourceScope,
                                          List<Long> cronIdList) {

        List<String> allowedIdList = appAuthService.batchAuth(username, ActionId.MANAGE_CRON,
            appResourceScope, ResourceTypeEnum.CRON,
            cronIdList.parallelStream().map(Objects::toString).collect(Collectors.toList()));
        return allowedIdList.parallelStream().map(Long::valueOf).collect(Collectors.toList());
    }

    @Override
    public boolean registerCron(Long id, String name, String creator) {
        return authService.registerResource(id.toString(), name, ResourceTypeId.CRON, creator, null);
    }
}
