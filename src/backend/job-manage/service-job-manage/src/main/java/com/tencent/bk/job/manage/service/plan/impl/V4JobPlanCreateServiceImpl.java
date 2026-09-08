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

package com.tencent.bk.job.manage.service.plan.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.AlreadyExistsException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.manage.auth.PlanAuthService;
import com.tencent.bk.job.manage.auth.TemplateAuthService;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4CreateJobPlanRequest;
import com.tencent.bk.job.manage.service.plan.OpenApiV4JobPlanRequestResolver;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import com.tencent.bk.job.manage.service.plan.V4JobPlanCreateService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class V4JobPlanCreateServiceImpl implements V4JobPlanCreateService {

    private final TaskPlanService planService;
    private final TaskTemplateService templateService;
    private final TemplateAuthService templateAuthService;
    private final PlanAuthService planAuthService;
    private final AppScopeMappingService appScopeMappingService;
    private final OpenApiV4JobPlanRequestResolver requestResolver;

    @Autowired
    public V4JobPlanCreateServiceImpl(TaskPlanService planService,
                                      TaskTemplateService templateService,
                                      TemplateAuthService templateAuthService,
                                      PlanAuthService planAuthService,
                                      AppScopeMappingService appScopeMappingService,
                                      OpenApiV4JobPlanRequestResolver requestResolver) {
        this.planService = planService;
        this.templateService = templateService;
        this.templateAuthService = templateAuthService;
        this.planAuthService = planAuthService;
        this.appScopeMappingService = appScopeMappingService;
        this.requestResolver = requestResolver;
    }

    @Override
    public TaskPlanInfoDTO createJobPlan(User operator, V4CreateJobPlanRequest request, boolean dryRun) {
        request.fillAppResourceScope(appScopeMappingService);
        Long appId = request.getAppId();
        AppResourceScope appResourceScope = request.getAppResourceScope();

        templateAuthService.authViewJobTemplate(operator, appResourceScope, request.getJobTemplateId())
            .denyIfNoPermission();
        planAuthService.authCreateJobPlan(operator, appResourceScope, request.getJobTemplateId(), null)
            .denyIfNoPermission();

        TaskTemplateInfoDTO template = templateService.getTaskTemplateById(appId, request.getJobTemplateId());
        if (template == null) {
            throw new NotFoundException(ErrorCode.TEMPLATE_NOT_EXIST);
        }

        List<Long> enableSteps = requestResolver.resolveEnableStepsForCreate(request.getEnableSteps(), template);
        List<TaskVariableDTO> variableList =
            requestResolver.mapVariables(request.getVariables(), template, operator.getTenantId());

        String planName = StringUtils.strip(request.getName());
        if (Boolean.FALSE.equals(
            planService.checkPlanName(appId, request.getJobTemplateId(), 0L, planName)
        )) {
            throw new AlreadyExistsException(ErrorCode.PLAN_NAME_EXIST);
        }

        TaskPlanInfoDTO planInfoDTO = requestResolver.buildTaskPlanInfoDTO(
            operator.getUsername(), appId, request.getJobTemplateId(), planName, enableSteps, variableList
        );
        planInfoDTO.setCreator(operator.getUsername());

        // ============ dryRun 预检返回点 ============
        // 此行之上不得新增写操作：预检与真实创建必须走同一段校验代码，但预检绝不能把执行方案落库。
        // 往上插入写操作会让预检穿透成真实创建，用户还没审批，执行方案已经建出来了。
        if (dryRun) {
            // 审批概要要按名称而不是 ID 展示启用的步骤，把上文已查出的模板步骤带回去，省得为一行展示再查一次模板。
            // 与真实创建时 TaskPlanInfoDTO#buildPlanInfo 的填法一致：stepList 是方案的全部步骤，
            // enableStepList 是其中启用的那些
            planInfoDTO.setStepList(template.getStepList());
            // 概要要列出方案生效的全部变量，未覆盖的也得带上模板默认值，因此这里复用真实创建的合并逻辑，
            // 把 variableList 从"本次覆盖项"换成"合并后的全部方案变量"。纯内存计算，不落库
            TaskPlanInfoDTO.fillPlanVariablesFromTemplate(planInfoDTO, template);
            return planInfoDTO;
        }

        return planService.createTaskPlan(operator, planInfoDTO);
    }
}
