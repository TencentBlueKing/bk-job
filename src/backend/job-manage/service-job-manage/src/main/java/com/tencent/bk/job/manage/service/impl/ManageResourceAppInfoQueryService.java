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

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.ResourceAppInfo;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.iam.service.ResourceAppInfoQueryService;
import com.tencent.bk.job.manage.model.dto.AccountDTO;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.inner.resp.ServiceApplicationDTO;
import com.tencent.bk.job.manage.model.inner.resp.ServiceCredentialDTO;
import com.tencent.bk.job.manage.service.AccountService;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.CredentialService;
import com.tencent.bk.job.manage.service.ScriptService;
import com.tencent.bk.job.manage.service.TagService;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service("ResourceAppInfoQueryService")
public class ManageResourceAppInfoQueryService implements ResourceAppInfoQueryService {

    private final ApplicationService applicationService;
    private final ScriptService scriptService;
    private final TaskTemplateService templateService;
    private final TaskPlanService planService;
    private final AccountService accountService;
    private final TagService tagService;
    private final CredentialService credentialService;

    @Autowired
    ManageResourceAppInfoQueryService(
        ApplicationService applicationService,
        ScriptService scriptService,
        TaskTemplateService templateService,
        TaskPlanService planService,
        AccountService accountService,
        TagService tagService,
        AuthService authService,
        AppAuthService appAuthService,
        CredentialService credentialService
    ) {
        this.applicationService = applicationService;
        this.scriptService = scriptService;
        this.templateService = templateService;
        this.planService = planService;
        this.accountService = accountService;
        this.tagService = tagService;
        this.credentialService = credentialService;
        authService.setResourceAppInfoQueryService(this);
        appAuthService.setResourceAppInfoQueryService(this);
    }

    private ResourceAppInfo getResourceAppInfoByScope(String scopeType, String scopeId) {
        if (StringUtils.isBlank(scopeType) || StringUtils.isBlank(scopeId)) {
            log.warn("scope({},{}) is invalid", scopeType, scopeId);
            return null;
        }
        return ServiceApplicationDTO.toResourceApp(applicationService.getAppByScope(scopeType, scopeId));
    }

    private ResourceAppInfo getResourceAppInfoById(Long appId) {
        if (appId == null || appId <= 0) {
            log.warn("appId({}) is invalid", appId);
            return null;
        }
        return ServiceApplicationDTO.toResourceApp(applicationService.getAppByAppId(appId));
    }

    private ResourceAppInfo getScriptApp(String resourceId) {
        if (StringUtils.isBlank(resourceId)) {
            log.warn("scriptId({}) is invalid", resourceId);
            return null;
        }
        ScriptDTO script = scriptService.getScriptByScriptId(resourceId);
        if (script == null) {
            log.warn("Cannot find script by id {}", resourceId);
            return null;
        }
        Long appId = script.getAppId();
        if (appId <= 0) {
            log.warn("appId({}) of script {} is invalid", appId, resourceId);
            return null;
        }
        return getResourceAppInfoById(appId);
    }

    private ResourceAppInfo getTemplateApp(String resourceId) {
        long templateId = Long.parseLong(resourceId);
        if (templateId <= 0) {
            log.warn("templateId({}) is invalid", resourceId);
            return null;
        }
        TaskTemplateInfoDTO templateInfoDTO = templateService.getTemplateById(templateId);
        if (templateInfoDTO == null) {
            log.warn("Cannot find templateInfoDTO by id {}", templateId);
            return null;
        }
        Long appId = templateInfoDTO.getAppId();
        if (appId <= 0) {
            log.warn("appId({}) of template {} is invalid", appId, resourceId);
            return null;
        }
        return getResourceAppInfoById(appId);
    }

    private ResourceAppInfo getPlanApp(String resourceId) {
        long planId = Long.parseLong(resourceId);
        if (planId <= 0) {
            log.warn("planId({}) is invalid", resourceId);
            return null;
        }
        TaskPlanInfoDTO taskPlanInfoDTO = planService.getTaskPlanById(planId);
        if (taskPlanInfoDTO == null) {
            log.warn("Cannot find taskPlanInfoDTO by id {}", planId);
            return null;
        }
        Long appId = taskPlanInfoDTO.getAppId();
        if (appId <= 0) {
            log.warn("appId({}) of plan {} is invalid", appId, resourceId);
            return null;
        }
        return getResourceAppInfoById(appId);
    }

    private ResourceAppInfo getAccountApp(String resourceId) {
        long accountId = Long.parseLong(resourceId);
        if (accountId <= 0) {
            log.warn("accountId({}) is invalid", resourceId);
            return null;
        }
        AccountDTO accountDTO = accountService.getAccountById(accountId);
        if (accountDTO == null) {
            log.warn("Cannot find account by id {}", resourceId);
            return null;
        }
        Long appId = accountDTO.getAppId();
        if (appId <= 0) {
            log.warn("appId({}) of account {} is invalid", appId, resourceId);
            return null;
        }
        return getResourceAppInfoById(appId);
    }

    private ResourceAppInfo getTagApp(String resourceId) {
        long tagId = Long.parseLong(resourceId);
        if (tagId <= 0) {
            log.warn("tagId({}) is invalid", resourceId);
            return null;
        }
        TagDTO tagDTO = tagService.getTagInfoById(tagId);
        if (tagDTO == null) {
            log.warn("Cannot find tag by id {}", resourceId);
            return null;
        }
        Long appId = tagDTO.getAppId();
        if (appId <= 0) {
            log.warn("appId({}) of tag {} is invalid", appId, resourceId);
            return null;
        }
        return getResourceAppInfoById(appId);
    }

    private ResourceAppInfo getTicketApp(String resourceId) {
        ServiceCredentialDTO credentialDTO = credentialService.getServiceCredentialById(resourceId);
        if (credentialDTO == null) {
            log.warn("Cannot find credential by id {}", resourceId);
            return null;
        }
        Long appId = credentialDTO.getAppId();
        if (appId <= 0) {
            log.warn("appId({}) of credential {} is invalid", appId, resourceId);
            return null;
        }
        return getResourceAppInfoById(appId);
    }

    @Override
    public ResourceAppInfo getResourceAppInfo(ResourceTypeEnum resourceType, String resourceId) {
        switch (resourceType) {
            case BUSINESS:
                return getResourceAppInfoByScope(ResourceScopeTypeEnum.BIZ.getValue(), resourceId);
            case BUSINESS_SET:
                return getResourceAppInfoByScope(ResourceScopeTypeEnum.BIZ_SET.getValue(), resourceId);
            case PUBLIC_SCRIPT:
            case SCRIPT:
                return getScriptApp(resourceId);
            case TEMPLATE:
                return getTemplateApp(resourceId);
            case PLAN:
                return getPlanApp(resourceId);
            case ACCOUNT:
                return getAccountApp(resourceId);
            case TAG:
                return getTagApp(resourceId);
            case TICKET:
                return getTicketApp(resourceId);
            default:
                log.warn("Not support resourceType:{}, return null", resourceType);
                return null;
        }
    }
}
