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

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.app.ApplicationUtil;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.ResourceAppInfo;
import com.tencent.bk.job.common.iam.service.ResourceAppInfoQueryService;
import com.tencent.bk.job.execute.client.AccountResourceClient;
import com.tencent.bk.job.execute.client.TaskTemplateResourceClient;
import com.tencent.bk.job.execute.service.ApplicationService;
import com.tencent.bk.job.execute.service.ScriptService;
import com.tencent.bk.job.execute.service.TaskPlanService;
import com.tencent.bk.job.manage.model.inner.ServiceAccountDTO;
import com.tencent.bk.job.manage.model.inner.ServiceScriptDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskTemplateDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ExecuteResourceAppInfoQueryService implements ResourceAppInfoQueryService {
    private final ApplicationService applicationService;
    private final ScriptService scriptService;
    private final TaskPlanService taskPlanService;
    private final TaskTemplateResourceClient taskTemplateService;
    private final AccountResourceClient accountService;


    @Autowired
    public ExecuteResourceAppInfoQueryService(ApplicationService applicationService,
                                              ScriptService scriptService,
                                              TaskPlanService taskPlanService,
                                              TaskTemplateResourceClient taskTemplateService,
                                              AccountResourceClient accountService) {
        this.applicationService = applicationService;
        this.scriptService = scriptService;
        this.taskPlanService = taskPlanService;
        this.taskTemplateService = taskTemplateService;
        this.accountService = accountService;
    }

    private ResourceAppInfo getResourceAppInfoByAppId(Long appId) {
        if (appId == null || appId <= 0) {
            log.warn("appId({}) is invalid", appId);
            return null;
        }
        return ApplicationUtil.convertToResourceApp(applicationService.getAppById(appId));
    }

    private ResourceAppInfo getResourceAppInfoByScope(String scopeType, String scopeId) {
        if (StringUtils.isBlank(scopeType) || StringUtils.isBlank(scopeId)) {
            log.warn("scope({},{}) is invalid", scopeType, scopeId);
            return null;
        }
        return ApplicationUtil.convertToResourceApp(applicationService.getAppByScope(scopeType, scopeId));
    }

    private ResourceAppInfo getScriptApp(String resourceId) {
        if (StringUtils.isBlank(resourceId)) {
            log.warn("scriptId({}) is invalid", resourceId);
            return null;
        }
        ServiceScriptDTO script = scriptService.getBasicScriptInfo(resourceId);
        if (script == null) {
            log.warn("Cannot find script by id {}", resourceId);
            return null;
        }
        Long appId = script.getAppId();
        if (appId <= 0) {
            log.warn("appId({}) of script {} is invalid", appId, resourceId);
            return null;
        }
        return getResourceAppInfoByAppId(appId);
    }

    private ResourceAppInfo getTemplateApp(String resourceId) {
        long templateId = Long.parseLong(resourceId);
        if (templateId <= 0) {
            log.warn("templateId({}) is invalid", resourceId);
            return null;
        }
        ServiceTaskTemplateDTO templateInfoDTO = taskTemplateService.getTemplateById(templateId).getData();
        if (templateInfoDTO == null) {
            log.warn("Cannot find templateInfoDTO by id {}", templateId);
            return null;
        }
        Long appId = templateInfoDTO.getAppId();
        if (appId <= 0) {
            log.warn("appId({}) of template {} is invalid", appId, resourceId);
            return null;
        }
        return getResourceAppInfoByAppId(appId);
    }

    private ResourceAppInfo getPlanApp(String resourceId) {
        long planId = Long.parseLong(resourceId);
        if (planId <= 0) {
            log.warn("planId({}) is invalid", resourceId);
            return null;
        }
        Long appId = taskPlanService.getPlanAppId(planId);
        if (appId == null || appId <= 0) {
            log.warn("appId({}) of plan {} is invalid", appId, resourceId);
            return null;
        }
        return getResourceAppInfoByAppId(appId);
    }

    private ResourceAppInfo getAccountApp(String resourceId) {
        long accountId = Long.parseLong(resourceId);
        if (accountId <= 0) {
            log.warn("accountId({}) is invalid", resourceId);
            return null;
        }
        ServiceAccountDTO accountDTO = accountService.getAccountByAccountId(accountId).getData();
        if (accountDTO == null) {
            log.warn("Cannot find account by id {}", resourceId);
            return null;
        }
        Long appId = accountDTO.getAppId();
        if (appId <= 0) {
            log.warn("appId({}) of account {} is invalid", appId, resourceId);
            return null;
        }
        return getResourceAppInfoByAppId(appId);
    }

    @Override
    public ResourceAppInfo getResourceAppInfo(ResourceTypeEnum resourceType, String resourceId) {
        switch (resourceType) {
            case SCRIPT:
                return getScriptApp(resourceId);
            case BUSINESS:
                return getResourceAppInfoByScope(ResourceScopeTypeEnum.BIZ.getValue(), resourceId);
            case BUSINESS_SET:
                return getResourceAppInfoByScope(ResourceScopeTypeEnum.BIZ_SET.getValue(), resourceId);
            case TEMPLATE:
                return getTemplateApp(resourceId);
            case PLAN:
                return getPlanApp(resourceId);
            case ACCOUNT:
                return getAccountApp(resourceId);
            default:
                log.warn("Not support resourceType:{}, return null", resourceType);
                return null;
        }
    }
}
