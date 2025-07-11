/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.manage.api.common.constants.JobResourceStatusEnum;
import com.tencent.bk.job.manage.api.common.constants.account.AccountTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskScriptSourceEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.api.inner.ServiceMetricsResource;
import com.tencent.bk.job.manage.model.dto.ResourceTagDTO;
import com.tencent.bk.job.manage.service.AccountService;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.ScriptManager;
import com.tencent.bk.job.manage.service.TagService;
import com.tencent.bk.job.manage.service.host.HostService;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController("manageMetricsResource")
@Slf4j
public class ServiceMetricsResourceImpl implements ServiceMetricsResource {

    private final ApplicationService applicationService;
    private final AccountService accountService;
    private final ScriptManager scriptManager;
    private final TaskTemplateService taskTemplateService;
    private final TaskPlanService taskPlanService;
    private final HostService hostService;
    private final TagService tagService;

    @Autowired
    public ServiceMetricsResourceImpl(ApplicationService applicationService,
                                      AccountService accountService,
                                      ScriptManager scriptManager,
                                      TaskTemplateService taskTemplateService,
                                      TaskPlanService taskPlanService,
                                      HostService hostService,
                                      TagService tagService) {
        this.applicationService = applicationService;
        this.accountService = accountService;
        this.scriptManager = scriptManager;
        this.taskTemplateService = taskTemplateService;
        this.taskPlanService = taskPlanService;
        this.hostService = hostService;
        this.tagService = tagService;
    }

    @Override
    public InternalResponse<Integer> countApps(String username) {
        return InternalResponse.buildSuccessResp(applicationService.countApps());
    }

    @Override
    public InternalResponse<Integer> countTemplates(Long appId) {
        return InternalResponse.buildSuccessResp(taskTemplateService.countTemplates(appId));
    }

    @Override
    public InternalResponse<Integer> countTaskPlans(Long appId) {
        return InternalResponse.buildSuccessResp(taskPlanService.countTaskPlans(appId));
    }

    @Override
    public InternalResponse<Integer> countTemplateSteps(Long appId, TaskStepTypeEnum taskStepType,
                                                        TaskScriptSourceEnum scriptSource, TaskFileTypeEnum fileType) {
        return InternalResponse.buildSuccessResp(taskTemplateService.countTemplateSteps(appId, taskStepType,
            scriptSource, fileType));
    }

    @Override
    public InternalResponse<Integer> countScripts(Long appId, ScriptTypeEnum scriptTypeEnum,
                                                  JobResourceStatusEnum jobResourceStatusEnum) {
        return InternalResponse.buildSuccessResp(scriptManager.countScripts(appId, scriptTypeEnum,
            jobResourceStatusEnum));
    }

    @Override
    public InternalResponse<Integer> countCiteScripts(Long appId) {
        return InternalResponse.buildSuccessResp(scriptManager.countCiteScripts(appId));
    }

    @Override
    public InternalResponse<Integer> countCiteScriptSteps(Long appId) {
        List<String> scriptIdList = scriptManager.listScriptIds(appId);
        return InternalResponse.buildSuccessResp(taskTemplateService.countCiteScriptSteps(appId, scriptIdList));
    }

    @Override
    public InternalResponse<Integer> countScriptVersions(Long appId, ScriptTypeEnum scriptTypeEnum,
                                                         JobResourceStatusEnum jobResourceStatusEnum) {
        return InternalResponse.buildSuccessResp(scriptManager.countScriptVersions(appId, scriptTypeEnum,
            jobResourceStatusEnum));
    }

    @Override
    public InternalResponse<Integer> countAccounts(AccountTypeEnum accountType) {
        return InternalResponse.buildSuccessResp(accountService.countAccounts(accountType));
    }

    @Override
    public InternalResponse<Long> countHostsByOsType(String osType) {
        return InternalResponse.buildSuccessResp(hostService.countHostsByOsType(osType));
    }

    @Override
    public InternalResponse<Map<String, Integer>> groupHostByOsType() {
        return InternalResponse.buildSuccessResp(hostService.groupHostByOsType());
    }

    @Override
    public InternalResponse<Long> tagCitedCount(Long appId, Long tagId) {
        List<ResourceTagDTO> scriptResourceTags = tagService.listResourceTagsByTagId(appId, tagId);
        return InternalResponse.buildSuccessResp((long) scriptResourceTags.size());
    }
}
