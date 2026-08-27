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
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.iam.util.IamUtil;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.manage.auth.FileSourceAuthService;
import com.tencent.bk.sdk.iam.constants.SystemId;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件源相关操作鉴权接口
 */
@Slf4j
// service-job-file-gateway 下已有同名类，轻量化部署时两者会在同一容器内注册，故显式指定 Bean 名避免冲突
@Service("jobManageFileSourceAuthService")
public class FileSourceAuthServiceImpl implements FileSourceAuthService {

    private final AppAuthService appAuthService;

    @Autowired
    public FileSourceAuthServiceImpl(AppAuthService appAuthService) {
        this.appAuthService = appAuthService;
    }

    private PathInfoDTO buildAppScopePath(AppResourceScope appResourceScope) {
        return PathBuilder.newBuilder(IamUtil.getIamResourceTypeIdForResourceScope(appResourceScope),
            appResourceScope.getId()).build();
    }

    @Override
    public AuthResult batchAuthViewFileSource(User user,
                                              AppResourceScope appResourceScope,
                                              Map<Integer, String> fileSourceIdToName) {
        if (MapUtils.isEmpty(fileSourceIdToName)) {
            return AuthResult.pass(user);
        }
        List<String> idList = fileSourceIdToName.keySet().stream()
            .map(String::valueOf)
            .collect(Collectors.toList());
        Set<String> allowedIdSet = new HashSet<>(appAuthService.batchAuth(user, ActionId.VIEW_FILE_SOURCE,
            appResourceScope, ResourceTypeEnum.FILE_SOURCE, idList));

        List<PermissionResource> deniedResources = new ArrayList<>();
        fileSourceIdToName.forEach((fileSourceId, fileSourceName) -> {
            String idStr = fileSourceId.toString();
            if (allowedIdSet.contains(idStr)) {
                return;
            }
            PermissionResource resource = new PermissionResource();
            resource.setSystemId(SystemId.JOB);
            resource.setResourceId(idStr);
            resource.setResourceType(ResourceTypeEnum.FILE_SOURCE);
            // 别名由上游查询可用性时一并带回，此处不再回查
            resource.setResourceName(StringUtils.isNotEmpty(fileSourceName) ? fileSourceName : idStr);
            resource.setPathInfo(buildAppScopePath(appResourceScope));
            deniedResources.add(resource);
        });
        if (deniedResources.isEmpty()) {
            return AuthResult.pass(user);
        }

        AuthResult authResult = AuthResult.fail(user);
        authResult.addRequiredPermissions(ActionId.VIEW_FILE_SOURCE, deniedResources);
        if (log.isDebugEnabled()) {
            log.debug("Auth view file source, authResult:{}", authResult);
        }
        return authResult;
    }
}
