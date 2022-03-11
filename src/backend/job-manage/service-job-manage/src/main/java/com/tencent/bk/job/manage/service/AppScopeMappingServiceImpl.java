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

package com.tencent.bk.job.manage.service;

import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

@Slf4j
@Service
public class AppScopeMappingServiceImpl implements AppScopeMappingService {

    private final ApplicationService applicationService;

    @Autowired
    public AppScopeMappingServiceImpl(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Override
    public Long getAppIdByScope(ResourceScope resourceScope) {
        return applicationService.getAppIdByScope(resourceScope);
    }

    @Override
    public Long getAppIdByScope(String scopeType, String scopeId) {
        return getAppIdByScope(new ResourceScope(scopeType, scopeId));
    }

    @Override
    public ResourceScope getScopeByAppId(Long appId) {
        return applicationService.getScopeByAppId(appId);
    }

    @Override
    public AppResourceScope getAppResourceScope(Long appId, String scopeType, String scopeId) {
        if (StringUtils.isNotBlank(scopeType) && StringUtils.isNotBlank(scopeId)) {
            return new AppResourceScope(scopeType, scopeId, getAppIdByScope(scopeType, scopeId));
        } else {
            ResourceScope resourceScope = getScopeByAppId(appId);
            return new AppResourceScope(appId, resourceScope);
        }
    }

    @Override
    public Map<Long, ResourceScope> getScopeByAppIds(Collection<Long> appIds) {
        return applicationService.getScopeByAppIds(appIds);
    }
}
