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

import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.ResourceAppInfo;
import com.tencent.bk.job.common.iam.service.ResourceAppInfoQueryService;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    private ResourceAppInfo convert(ApplicationDTO applicationDTO) {
        if (applicationDTO == null) {
            return null;
        } else {
            ResourceAppInfo resourceAppInfo = new ResourceAppInfo();
            resourceAppInfo.setAppId(applicationDTO.getId());
            resourceAppInfo.setAppType(applicationDTO.getAppType());
            String maintainerStr = applicationDTO.getMaintainers();
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
        return convert(applicationService.getAppById(appId));
    }

    @Override
    public ResourceAppInfo getResourceAppInfo(ResourceTypeEnum resourceType, String resourceId) {
        switch (resourceType) {
            case SCRIPT:
                ServiceScriptDTO script = scriptService.getBasicScriptInfo(resourceId);
                if (script != null) {
                    return getResourceAppInfoById(script.getAppId());
                }
                break;
            case BUSINESS:
                long appId = Long.parseLong(resourceId);
                if (appId > 0) {
                    return getResourceAppInfoById(appId);
                }
                break;
            case TEMPLATE:
                long templateId = Long.parseLong(resourceId);
                if (templateId > 0) {
                    ServiceTaskTemplateDTO templateDTO = taskTemplateService.getTemplateById(templateId).getData();
                    if (templateDTO != null) {
                        return getResourceAppInfoById(templateDTO.getAppId());
                    }
                }
                break;
            case PLAN:
                long planId = Long.parseLong(resourceId);
                if (planId > 0) {
                    Long planAppId = taskPlanService.getPlanAppId(planId);
                    if (planAppId != null && planAppId > 0) {
                        return getResourceAppInfoById(planAppId);
                    }
                }
                break;
            case ACCOUNT:
                long accountId = Long.parseLong(resourceId);
                if (accountId > 0) {
                    ServiceAccountDTO accountDTO = accountService.getAccountByAccountId(accountId).getData();
                    if (accountDTO != null) {
                        return getResourceAppInfoById(accountDTO.getAppId());
                    } else {
                        log.warn("Cannot find account by id:{}, account may be deleted, please check", accountId);
                    }
                }
                break;
            default:
                return null;
        }
        return null;
    }
}
