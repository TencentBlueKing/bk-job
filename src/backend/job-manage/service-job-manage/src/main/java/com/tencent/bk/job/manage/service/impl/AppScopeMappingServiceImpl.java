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

package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.manage.AbstractLocalCacheAppScopeMappingService;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AppScopeMappingServiceImpl extends AbstractLocalCacheAppScopeMappingService {

    private final ApplicationService applicationService;

    @Autowired
    public AppScopeMappingServiceImpl(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Override
    public Long queryAppByScope(ResourceScope resourceScope) throws NotFoundException {
        Long appId = applicationService.getAppIdByScope(resourceScope);
        if (appId == null) {
            log.error("App not exist, resourceScope: {}", resourceScope);
            throw new NotFoundException(ErrorCode.APP_NOT_EXIST);
        }
        return appId;
    }

    @Override
    public ResourceScope queryScopeByAppId(Long appId) throws NotFoundException {
        ResourceScope resourceScope = applicationService.getScopeByAppId(appId);
        if (resourceScope == null) {
            log.error("App not exist, appId: {}", appId);
            throw new NotFoundException(ErrorCode.APP_NOT_EXIST);
        }
        return resourceScope;
    }
}
