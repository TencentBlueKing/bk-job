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

package com.tencent.bk.job.file_gateway.service.impl;

import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.iam.service.ResourceNameQueryService;
import com.tencent.bk.job.common.iam.util.IamUtil;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.file_gateway.client.ServiceApplicationResourceClient;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.service.FileSourceService;
import com.tencent.bk.job.manage.model.inner.ServiceApplicationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service("ResourceNameQueryService")
public class ResourceNameQueryServiceImpl implements ResourceNameQueryService {

    private final FileSourceService fileSourceService;
    private final ServiceApplicationResourceClient applicationResourceClient;
    private final AppScopeMappingService appScopeMappingService;

    @Autowired
    public ResourceNameQueryServiceImpl(AuthService authService,
                                        AppAuthService appAuthService,
                                        FileSourceService fileSourceService,
                                        ServiceApplicationResourceClient applicationResourceClient,
                                        AppScopeMappingService appScopeMappingService) {
        this.fileSourceService = fileSourceService;
        this.applicationResourceClient = applicationResourceClient;
        this.appScopeMappingService = appScopeMappingService;
        authService.setResourceNameQueryService(this);
        appAuthService.setResourceNameQueryService(this);
    }

    private String getAppName(long appId) {
        ServiceApplicationDTO serviceApplicationDTO = applicationResourceClient.queryAppById(appId);
        if (serviceApplicationDTO == null) return null;
        return serviceApplicationDTO.getName();
    }

    @Override
    public String getResourceName(ResourceTypeEnum resourceType, String resourceId) {
        switch (resourceType) {
            case BUSINESS:
            case BUSINESS_SET:
                Long appId = appScopeMappingService.getAppIdByScope(
                    IamUtil.getResourceScopeFromIamResource(resourceType, resourceId));
                if (appId != null && appId > 0) {
                    return getAppName(appId);
                }
                log.warn("Cannot find appName by appId {}", appId);
                return null;
            case FILE_SOURCE:
                FileSourceDTO fileSourceDTO = fileSourceService.getFileSourceById(Integer.parseInt(resourceId));
                if (fileSourceDTO == null) {
                    log.warn("Cannot find fileSource by fileSourceId {}", resourceId);
                    return null;
                }
                return fileSourceDTO.getAlias();
            default:
                log.warn("Cannot find ResourceName by resourceType {} resourceId {}", resourceType, resourceId);
                return null;
        }
    }
}
