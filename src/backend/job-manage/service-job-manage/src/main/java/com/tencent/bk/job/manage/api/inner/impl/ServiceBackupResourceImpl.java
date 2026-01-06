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

import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.manage.api.inner.ServiceBackupResource;
import com.tencent.bk.job.manage.manager.variable.StepRefVariableParser;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskPlanVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskTemplateVO;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class ServiceBackupResourceImpl implements ServiceBackupResource {
    private final TaskTemplateService taskTemplateService;
    private final TaskPlanService planService;
    private final AppScopeMappingService appScopeMappingService;

    public ServiceBackupResourceImpl(TaskTemplateService taskTemplateService,
                                     TaskPlanService planService,
                                     AppScopeMappingService appScopeMappingService) {
        this.taskTemplateService = taskTemplateService;
        this.planService = planService;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    public Response<TaskTemplateVO> getTemplateById(Long appId, Long templateId) {
        TaskTemplateInfoDTO templateInfo = taskTemplateService.getTaskTemplateById(appId, templateId);
        StepRefVariableParser.parseStepRefVars(templateInfo.getStepList(), templateInfo.getVariableList());
        TaskTemplateVO taskTemplateVO = TaskTemplateInfoDTO.toVO(templateInfo);
        return Response.buildSuccessResp(taskTemplateVO);
    }

    @Override
    public Response<TaskPlanVO> getPlanById(Long appId,
                                            Long templateId,
                                            Long planId) {
        AppResourceScope appResourceScope = appScopeMappingService.getAppResourceScope(appId, null, null);
        TaskTemplateInfoDTO taskTemplateBasicInfo =
            taskTemplateService.getTaskTemplateBasicInfoById(appResourceScope.getAppId(), templateId);
        TaskPlanInfoDTO taskPlan = planService.getTaskPlanById(appResourceScope.getAppId(), templateId, planId);
        StepRefVariableParser.parseStepRefVars(taskPlan.getStepList(), taskPlan.getVariableList());
        final String templateVersion = taskTemplateBasicInfo.getVersion();
        if (StringUtils.isNotEmpty(templateVersion)) {
            taskPlan.setTemplateVersion(templateVersion);
            taskPlan.setNeedUpdate(!templateVersion.equals(taskPlan.getVersion()));
        }
        TaskPlanVO taskPlanVO = TaskPlanInfoDTO.toVO(taskPlan);
        return Response.buildSuccessResp(taskPlanVO);
    }

    @Override
    public Response<List<TaskPlanVO>> listPlans(Long appId,
                                                Long templateId) {
        TaskTemplateInfoDTO taskTemplateBasicInfo = taskTemplateService.getTaskTemplateBasicInfoById(appId, templateId);
        List<TaskPlanInfoDTO> taskPlanInfoList = planService.listTaskPlansBasicInfo(appId, templateId);
        final String templateVersion = taskTemplateBasicInfo.getVersion();
        List<TaskPlanVO> taskPlanList = taskPlanInfoList.stream()
            .map(
                taskPlanInfo -> {
                    taskPlanInfo.setNeedUpdate(!templateVersion.equals(taskPlanInfo.getVersion()));
                    taskPlanInfo.setTemplateVersion(templateVersion);
                    return TaskPlanInfoDTO.toVO(taskPlanInfo);
                }
            ).collect(Collectors.toList());
        return Response.buildSuccessResp(taskPlanList);
    }
}
