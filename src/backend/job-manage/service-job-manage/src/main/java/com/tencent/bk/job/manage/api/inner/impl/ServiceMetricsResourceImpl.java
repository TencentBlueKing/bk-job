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

import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.manage.api.inner.ServiceMetricsResource;
import com.tencent.bk.job.manage.common.consts.JobResourceStatusEnum;
import com.tencent.bk.job.manage.common.consts.account.AccountTypeEnum;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskScriptSourceEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.service.AccountService;
import com.tencent.bk.job.manage.service.ApplicationHostService;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.ScriptService;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class ServiceMetricsResourceImpl implements ServiceMetricsResource {

    private final ApplicationService applicationService;
    private final AccountService accountService;
    private final ScriptService scriptService;
    private final TaskTemplateService taskTemplateService;
    private final TaskPlanService taskPlanService;
    private final ApplicationHostService applicationHostService;

    @Autowired
    public ServiceMetricsResourceImpl(ApplicationService applicationService, AccountService accountService,
                                      ScriptService scriptService, TaskTemplateService taskTemplateService,
                                      TaskPlanService taskPlanService, ApplicationHostService applicationHostService) {
        this.applicationService = applicationService;
        this.accountService = accountService;
        this.scriptService = scriptService;
        this.taskTemplateService = taskTemplateService;
        this.taskPlanService = taskPlanService;
        this.applicationHostService = applicationHostService;
    }

    @Override
    public ServiceResponse<Integer> countApps(String username) {
        return ServiceResponse.buildSuccessResp(applicationService.countApps(username));
    }

    @Override
    public ServiceResponse<Integer> countTemplates(Long appId) {
        return ServiceResponse.buildSuccessResp(taskTemplateService.countTemplates(appId));
    }

    @Override
    public ServiceResponse<Integer> countTaskPlans(Long appId) {
        return ServiceResponse.buildSuccessResp(taskPlanService.countTaskPlans(appId));
    }

    @Override
    public ServiceResponse<Integer> countTemplateSteps(Long appId, TaskStepTypeEnum taskStepType,
                                                       TaskScriptSourceEnum scriptSource, TaskFileTypeEnum fileType) {
        return ServiceResponse.buildSuccessResp(taskTemplateService.countTemplateSteps(appId, taskStepType,
            scriptSource, fileType));
    }

    @Override
    public ServiceResponse<Integer> countScripts(Long appId, ScriptTypeEnum scriptTypeEnum,
                                                 JobResourceStatusEnum jobResourceStatusEnum) {
        return ServiceResponse.buildSuccessResp(scriptService.countScripts(appId, scriptTypeEnum,
            jobResourceStatusEnum));
    }

    @Override
    public ServiceResponse<Integer> countCiteScripts(Long appId) {
        return ServiceResponse.buildSuccessResp(scriptService.countCiteScripts(appId));
    }

    @Override
    public ServiceResponse<Integer> countCiteScriptSteps(Long appId) {
        List<String> scriptIdList = scriptService.listScriptIds(appId);
        return ServiceResponse.buildSuccessResp(taskTemplateService.countCiteScriptSteps(appId, scriptIdList));
    }

    @Override
    public ServiceResponse<Integer> countScriptVersions(Long appId, ScriptTypeEnum scriptTypeEnum,
                                                        JobResourceStatusEnum jobResourceStatusEnum) {
        return ServiceResponse.buildSuccessResp(scriptService.countScriptVersions(appId, scriptTypeEnum,
            jobResourceStatusEnum));
    }

    @Override
    public ServiceResponse<Integer> countAccounts(AccountTypeEnum accountType) {
        return ServiceResponse.buildSuccessResp(accountService.countAccounts(accountType));
    }

    @Override
    public ServiceResponse<Long> countHostsByOsType(String osType) {
        return ServiceResponse.buildSuccessResp(applicationHostService.countHostsByOsType(osType));
    }

    @Override
    public ServiceResponse<Long> tagCitedCount(Long appId, Long tagId) {
        Integer citedScriptCount = scriptService.countByTag(appId, tagId);
        Integer citedTaskTemplateCount = taskTemplateService.countByTag(appId, tagId);
        Long tagCitedCount = (long) citedScriptCount + (long) citedTaskTemplateCount;
        return ServiceResponse.buildSuccessResp(tagCitedCount);
    }
}
