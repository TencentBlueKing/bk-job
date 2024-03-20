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
import com.tencent.bk.job.manage.auth.TicketAuthService;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 凭证相关操作鉴权接口
 */
@Service
public class TicketAuthServiceImpl implements TicketAuthService {

    private final AuthService authService;
    private final AppAuthService appAuthService;

    @Autowired
    public TicketAuthServiceImpl(AuthService authService,
                                 AppAuthService appAuthService) {
        this.authService = authService;
        this.appAuthService = appAuthService;
    }

    private PathInfoDTO buildAppScopePath(AppResourceScope appResourceScope) {
        return PathBuilder.newBuilder(IamUtil.getIamResourceTypeIdForResourceScope(appResourceScope),
            appResourceScope.getId()).build();
    }

    @Override
    public AuthResult authCreateTicket(String username, AppResourceScope appResourceScope) {
        return appAuthService.auth(username, ActionId.CREATE_TICKET, appResourceScope);
    }

    @Override
    public AuthResult authManageTicket(String username,
                                       AppResourceScope appResourceScope,
                                       String ticketId,
                                       String ticketName) {
        return authService.auth(
            username,
            ActionId.MANAGE_TICKET,
            ResourceTypeEnum.TICKET,
            ticketId,
            buildAppScopePath(appResourceScope)
        );
    }

    @Override
    public AuthResult authUseTicket(String username,
                                    AppResourceScope appResourceScope,
                                    String ticketId,
                                    String ticketName) {
        return authService.auth(
            username,
            ActionId.USE_TICKET,
            ResourceTypeEnum.TICKET,
            ticketId,
            buildAppScopePath(appResourceScope)
        );
    }

    @Override
    public List<String> batchAuthManageTicket(String username,
                                              AppResourceScope appResourceScope,
                                              List<String> ticketIdList) {
        return appAuthService.batchAuth(
            username,
            ActionId.MANAGE_TICKET,
            appResourceScope,
            ResourceTypeEnum.TICKET,
            ticketIdList
        );
    }

    @Override
    public List<String> batchAuthUseTicket(String username,
                                           AppResourceScope appResourceScope,
                                           List<String> ticketIdList) {
        return appAuthService.batchAuth(
            username,
            ActionId.USE_TICKET,
            appResourceScope,
            ResourceTypeEnum.TICKET,
            ticketIdList
        );
    }

    @Override
    public boolean registerTicket(String creator, String id, String name) {
        return authService.registerResource(id, name, ResourceTypeId.TICKET, creator, null);
    }
}
