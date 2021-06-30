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

import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.iam.service.ResourceNameQueryService;
import com.tencent.bk.job.common.model.dto.ApplicationInfoDTO;
import com.tencent.bk.job.manage.model.dto.AccountDTO;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.service.AccountService;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.ScriptService;
import com.tencent.bk.job.manage.service.TagService;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service("ResourceNameQueryService")
public class ResourceNameQueryServiceImpl implements ResourceNameQueryService {

    private ApplicationService applicationService;
    private ScriptService scriptService;
    private TaskTemplateService templateService;
    private TaskPlanService planService;
    private AccountService accountService;
    private TagService tagService;

    @Autowired
    public ResourceNameQueryServiceImpl(ApplicationService applicationService, ScriptService scriptService,
                                        TaskTemplateService templateService, TaskPlanService planService,
                                        AccountService accountService, TagService tagService, AuthService authService) {
        this.applicationService = applicationService;
        this.scriptService = scriptService;
        this.templateService = templateService;
        this.planService = planService;
        this.accountService = accountService;
        this.tagService = tagService;
        authService.setResourceNameQueryService(this);
    }

    private String getAppName(Long appId) {
        ApplicationInfoDTO appInfo = applicationService.getAppInfoById(appId);
        if (appInfo != null) {
            if (StringUtils.isNotBlank(appInfo.getName())) {
                return appInfo.getName();
            }
        }
        return null;
    }

    @Override
    public String getResourceName(ResourceTypeEnum resourceType, String resourceId) {
        switch (resourceType) {
            case BUSINESS:
                long appId = Long.parseLong(resourceId);
                if (appId > 0) {
                    return getAppName(appId);
                }
                break;
            case PUBLIC_SCRIPT:
            case SCRIPT:
                ScriptDTO script = scriptService.getScriptByScriptId(resourceId);
                if (script != null) {
                    return script.getName();
                }
                break;
            case TEMPLATE:
                long templateId = Long.parseLong(resourceId);
                if (templateId > 0) {
                    return templateService.getTemplateName(templateId);
                }
                break;
            case PLAN:
                long planId = Long.parseLong(resourceId);
                if (planId > 0) {
                    return planService.getPlanName(planId);
                }
                break;
            case ACCOUNT:
                long accountId = Long.parseLong(resourceId);
                if (accountId > 0) {
                    AccountDTO accountDTO = accountService.getAccountById(accountId);
                    if (accountDTO != null) {
                        return accountDTO.getAlias();
                    }
                }
                break;
            case TAG:
                long tagId = Long.parseLong(resourceId);
                if (tagId > 0) {
                    TagDTO tagDTO = tagService.getTagInfoById(tagId);
                    if (tagDTO != null) {
                        return tagDTO.getName();
                    }
                }
                break;
            default:
                return null;
        }
        return null;
    }

}
