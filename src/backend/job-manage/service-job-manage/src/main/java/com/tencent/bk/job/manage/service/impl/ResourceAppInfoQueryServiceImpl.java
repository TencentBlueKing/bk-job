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
import com.tencent.bk.job.common.iam.model.ResourceAppInfo;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.iam.service.ResourceAppInfoQueryService;
import com.tencent.bk.job.common.model.dto.ApplicationInfoDTO;
import com.tencent.bk.job.manage.model.dto.AccountDTO;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service("ResourceAppInfoQueryService")
public class ResourceAppInfoQueryServiceImpl implements ResourceAppInfoQueryService {

    private final ApplicationService applicationService;
    private final ScriptService scriptService;
    private final TaskTemplateService templateService;
    private final TaskPlanService planService;
    private final AccountService accountService;
    private final TagService tagService;
    private final CredentialService credentialService;

    @Autowired
    ResourceAppInfoQueryServiceImpl(
        ApplicationService applicationService,
        ScriptService scriptService,
        TaskTemplateService templateService,
        TaskPlanService planService,
        AccountService accountService,
        TagService tagService,
        AuthService authService,
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
    }

    private ResourceAppInfo convert(ApplicationInfoDTO applicationInfoDTO) {
        if (applicationInfoDTO == null) {
            return null;
        } else {
            ResourceAppInfo resourceAppInfo = new ResourceAppInfo();
            resourceAppInfo.setAppId(applicationInfoDTO.getId());
            resourceAppInfo.setAppType(applicationInfoDTO.getAppType());
            String maintainerStr = applicationInfoDTO.getMaintainers();
            List<String> maintainerList = new ArrayList<>();
            if (StringUtils.isNotBlank(maintainerStr)) {
                String[] maintainers = maintainerStr.split("[,;]");
                maintainerList.addAll(Arrays.asList(maintainers));
            }
            resourceAppInfo.setMaintainerList(maintainerList);
            return resourceAppInfo;
        }
    }

    private ResourceAppInfo getResourceAppInfoById(Long appId) {
        if (appId == null || appId <= 0) {
            return null;
        }
        return convert(applicationService.getAppInfoById(appId));
    }

    private ResourceAppInfo getScriptApp(String resourceId) {
        ScriptDTO script = scriptService.getScriptByScriptId(resourceId);
        if (script != null) {
            return getResourceAppInfoById(script.getAppId());
        }
        return null;
    }

    private ResourceAppInfo getTemplateApp(String resourceId) {
        long templateId = Long.parseLong(resourceId);
        if (templateId > 0) {
            TaskTemplateInfoDTO templateInfoDTO = templateService.getTemplateById(templateId);
            if (templateInfoDTO != null) {
                return getResourceAppInfoById(templateInfoDTO.getAppId());
            }
        }
        return null;
    }

    private ResourceAppInfo getPlanApp(String resourceId) {
        long planId = Long.parseLong(resourceId);
        if (planId > 0) {
            TaskPlanInfoDTO taskPlanInfoDTO = planService.getTaskPlanById(planId);
            if (taskPlanInfoDTO == null) {
                log.warn("Cannot find taskPlanInfoDTO by id {}", planId);
                return null;
            }
            return getResourceAppInfoById(taskPlanInfoDTO.getAppId());
        }
        return null;
    }

    private ResourceAppInfo getAccountApp(String resourceId) {
        long accountId = Long.parseLong(resourceId);
        if (accountId > 0) {
            AccountDTO accountDTO = accountService.getAccountById(accountId);
            if (accountDTO != null) {
                return getResourceAppInfoById(accountDTO.getAppId());
            }
        }
        return null;
    }

    private ResourceAppInfo getTagApp(String resourceId) {
        long tagId = Long.parseLong(resourceId);
        if (tagId > 0) {
            TagDTO tagDTO = tagService.getTagInfoById(tagId);
            if (tagDTO != null) {
                return getResourceAppInfoById(tagDTO.getAppId());
            }
        }
        return null;
    }

    private ResourceAppInfo getTicketApp(String resourceId) {
        ServiceCredentialDTO credentialDTO = credentialService.getServiceCredentialById(resourceId);
        if (credentialDTO == null) {
            log.warn("Cannot find credential by id {}", resourceId);
            return null;
        }
        Long appId = credentialDTO.getAppId();
        if (appId > 0) {
            return getResourceAppInfoById(appId);
        }
        return null;
    }

    @Override
    public ResourceAppInfo getResourceAppInfo(ResourceTypeEnum resourceType, String resourceId) {
        switch (resourceType) {
            case BUSINESS:
                long appId = Long.parseLong(resourceId);
                return getResourceAppInfoById(appId);
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
                return null;
        }
    }
}
