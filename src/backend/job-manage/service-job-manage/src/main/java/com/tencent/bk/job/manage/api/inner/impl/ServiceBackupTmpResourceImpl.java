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

package com.tencent.bk.job.manage.api.inner.impl;

import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.manage.api.inner.ServiceBackupTmpResource;
import com.tencent.bk.job.manage.api.web.WebTaskPlanResource;
import com.tencent.bk.job.manage.api.web.WebTaskTemplateResource;
import com.tencent.bk.job.manage.model.web.vo.task.TaskPlanVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskTemplateVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class ServiceBackupTmpResourceImpl implements ServiceBackupTmpResource {
    private final WebTaskTemplateResource webTaskTemplateResource;
    private final WebTaskPlanResource webTaskPlanResource;
    private final AppScopeMappingService appScopeMappingService;

    public ServiceBackupTmpResourceImpl(WebTaskTemplateResource webTaskTemplateResource,
                                        WebTaskPlanResource webTaskPlanResource,
                                        AppScopeMappingService appScopeMappingService) {
        this.webTaskTemplateResource = webTaskTemplateResource;
        this.webTaskPlanResource = webTaskPlanResource;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    public Response<TaskTemplateVO> getTemplateById(String username, Long appId, Long templateId) {
        AppResourceScope appResourceScope = appScopeMappingService.getAppResourceScope(appId, null, null);
        return webTaskTemplateResource.getTemplateById(username, appResourceScope,
            appResourceScope.getType().getValue(), appResourceScope.getId(), templateId);
    }

    @Override
    public Response<TaskPlanVO> getPlanById(String username,
                                            Long appId,
                                            Long templateId,
                                            Long planId) {
        AppResourceScope appResourceScope = appScopeMappingService.getAppResourceScope(appId, null, null);
        return webTaskPlanResource.getPlanById(username, appResourceScope, appResourceScope.getType().getValue(),
            appResourceScope.getId(), templateId, planId);
    }

    @Override
    public Response<List<TaskPlanVO>> listPlans(String username,
                                                Long appId,
                                                Long templateId) {
        AppResourceScope appResourceScope = appScopeMappingService.getAppResourceScope(appId, null, null);
        return webTaskPlanResource.listPlans(username, appResourceScope, appResourceScope.getType().getValue(),
            appResourceScope.getId(), templateId);
    }
}
