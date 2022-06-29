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

import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.service.ResourceNameQueryService;
import com.tencent.bk.job.common.iam.util.IamUtil;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.execute.client.TaskTemplateResourceClient;
import com.tencent.bk.job.execute.model.AccountDTO;
import com.tencent.bk.job.execute.service.AccountService;
import com.tencent.bk.job.execute.service.ApplicationService;
import com.tencent.bk.job.execute.service.ScriptService;
import com.tencent.bk.job.execute.service.TaskPlanService;
import com.tencent.bk.job.manage.model.inner.ServiceScriptDTO;
import com.tencent.bk.job.manage.model.inner.resp.ServiceApplicationDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ExecuteResourceNameQueryService implements ResourceNameQueryService {
    private final ApplicationService applicationService;
    private final ScriptService scriptService;
    private final TaskPlanService taskPlanService;
    private final TaskTemplateResourceClient taskTemplateService;
    private final AccountService accountService;
    private final AppScopeMappingService appScopeMappingService;


    @Autowired
    public ExecuteResourceNameQueryService(ApplicationService applicationService,
                                           ScriptService scriptService,
                                           TaskPlanService taskPlanService,
                                           TaskTemplateResourceClient taskTemplateService,
                                           AccountService accountService,
                                           AppScopeMappingService appScopeMappingService) {
        this.applicationService = applicationService;
        this.scriptService = scriptService;
        this.taskPlanService = taskPlanService;
        this.taskTemplateService = taskTemplateService;
        this.accountService = accountService;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    public String getResourceName(ResourceTypeEnum resourceType, String resourceId) {
        switch (resourceType) {
            case SCRIPT:
                ServiceScriptDTO script = scriptService.getBasicScriptInfo(resourceId);
                return script == null ? null : script.getName();
            case BUSINESS:
            case BUSINESS_SET:
                Long appId = appScopeMappingService.getAppIdByScope(
                    IamUtil.getResourceScopeFromIamResource(resourceType, resourceId));
                if (appId != null && appId > 0) {
                    return getAppName(appId);
                }
                break;
            case TEMPLATE:
                long templateId = Long.parseLong(resourceId);
                if (templateId > 0) {
                    return taskTemplateService.getTemplateNameById(templateId).getData();
                }
                break;
            case PLAN:
                long planId = Long.parseLong(resourceId);
                if (planId > 0) {
                    return taskPlanService.getPlanName(planId);
                }
                break;
            case ACCOUNT:
                long accountId = Long.parseLong(resourceId);
                if (accountId > 0) {
                    try {
                        AccountDTO account = accountService.getAccountById(accountId);
                        return account != null ? account.getAlias() : "";
                    } catch (ServiceException e) {
                        return "";
                    }
                }
                break;
            default:
                return null;
        }
        return null;
    }

    private String getAppName(Long appId) {
        ServiceApplicationDTO applicationInfo = applicationService.getAppById(appId);
        if (applicationInfo != null) {
            if (StringUtils.isNotBlank(applicationInfo.getName())) {
                return applicationInfo.getName();
            }
        }
        return null;
    }

}
