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

package com.tencent.bk.job.file_gateway.auth.impl;


import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.iam.util.IamUtil;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.file_gateway.auth.FileSourceAuthService;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件源相关操作鉴权接口
 */
@Service
public class FileSourceAuthServiceImpl implements FileSourceAuthService {

    private final AuthService authService;
    private final AppAuthService appAuthService;

    @Autowired
    public FileSourceAuthServiceImpl(AuthService authService,
                                     AppAuthService appAuthService) {
        this.authService = authService;
        this.appAuthService = appAuthService;
    }

    private PathInfoDTO buildAppScopePath(AppResourceScope appResourceScope) {
        return PathBuilder.newBuilder(IamUtil.getIamResourceTypeIdForResourceScope(appResourceScope),
            appResourceScope.getId()).build();
    }

    @Override
    public AuthResult authCreateFileSource(String username, AppResourceScope appResourceScope) {
        return appAuthService.auth(true, username, ActionId.CREATE_FILE_SOURCE, appResourceScope);
    }

    @Override
    public AuthResult authViewFileSource(String username,
                                         AppResourceScope appResourceScope,
                                         Integer fileSourceId,
                                         String fileSourceName) {
        return authService.auth(true, username, ActionId.VIEW_FILE_SOURCE, ResourceTypeEnum.FILE_SOURCE,
            fileSourceId.toString(), buildAppScopePath(appResourceScope));
    }

    @Override
    public AuthResult authManageFileSource(String username,
                                           AppResourceScope appResourceScope,
                                           Integer fileSourceId,
                                           String fileSourceName) {
        return authService.auth(true, username, ActionId.MANAGE_FILE_SOURCE, ResourceTypeEnum.FILE_SOURCE,
            fileSourceId.toString(), buildAppScopePath(appResourceScope));
    }

    @Override
    public List<Integer> batchAuthViewFileSource(String username,
                                                 AppResourceScope appResourceScope,
                                                 List<Integer> fileSourceIdList) {
        List<String> allowedIdList = appAuthService.batchAuth(username, ActionId.VIEW_FILE_SOURCE, appResourceScope,
            ResourceTypeEnum.FILE_SOURCE,
            fileSourceIdList.parallelStream().map(Object::toString).collect(Collectors.toList()));
        return allowedIdList.parallelStream().map(Integer::valueOf).collect(Collectors.toList());
    }

    @Override
    public List<Integer> batchAuthManageFileSource(String username,
                                                   AppResourceScope appResourceScope,
                                                   List<Integer> fileSourceIdList) {
        List<String> allowedIdList = appAuthService.batchAuth(username, ActionId.MANAGE_FILE_SOURCE, appResourceScope,
            ResourceTypeEnum.FILE_SOURCE,
            fileSourceIdList.parallelStream().map(Object::toString).collect(Collectors.toList()));
        return allowedIdList.parallelStream().map(Integer::valueOf).collect(Collectors.toList());
    }

    @Override
    public boolean registerFileSource(String creator, Integer id, String name) {
        return authService.registerResource(id.toString(), name, ResourceTypeId.FILE_SOURCE, creator, null);
    }
}
