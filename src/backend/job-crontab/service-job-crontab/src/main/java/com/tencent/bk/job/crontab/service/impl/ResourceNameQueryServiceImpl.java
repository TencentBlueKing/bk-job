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

package com.tencent.bk.job.crontab.service.impl;

import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.iam.service.ResourceNameQueryService;
import com.tencent.bk.job.common.iam.util.IamUtil;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.crontab.client.ServiceApplicationResourceClient;
import com.tencent.bk.job.crontab.service.CronJobService;
import com.tencent.bk.job.common.model.ServiceApplicationDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @since 18/6/2020 15:46
 */
@Slf4j
@Service("ResourceNameQueryService")
public class ResourceNameQueryServiceImpl implements ResourceNameQueryService {

    private final CronJobService cronJobService;
    private final ServiceApplicationResourceClient applicationClient;
    private final AppScopeMappingService appScopeMappingService;

    @Autowired
    public ResourceNameQueryServiceImpl(CronJobService cronJobService,
                                        ServiceApplicationResourceClient applicationClient,
                                        AuthService authService,
                                        AppAuthService appAuthService,
                                        AppScopeMappingService appScopeMappingService) {
        this.cronJobService = cronJobService;
        this.applicationClient = applicationClient;
        this.appScopeMappingService = appScopeMappingService;
        authService.setResourceNameQueryService(this);
        appAuthService.setResourceNameQueryService(this);
    }

    private String getAppName(Long appId) {
        ServiceApplicationDTO applicationInfo = applicationClient.queryAppById(appId);
        if (applicationInfo != null) {
            if (StringUtils.isNotBlank(applicationInfo.getName())) {
                return applicationInfo.getName();
            }
        }
        return null;
    }

    @Override
    public String getResourceName(ResourceTypeEnum resourceType, String resourceId) {
        switch (resourceType) {
            case CRON:
                long cronJobId = Long.parseLong(resourceId);
                if (cronJobId > 0) {
                    return cronJobService.getCronJobNameById(cronJobId);
                }
                break;
            case BUSINESS:
            case BUSINESS_SET:
                Long appId = appScopeMappingService.getAppIdByScope(
                    IamUtil.getResourceScopeFromIamResource(resourceType, resourceId));
                if (appId != null && appId > 0) {
                    return getAppName(appId);
                }
                break;
            default:
                return null;
        }
        return null;
    }
}
