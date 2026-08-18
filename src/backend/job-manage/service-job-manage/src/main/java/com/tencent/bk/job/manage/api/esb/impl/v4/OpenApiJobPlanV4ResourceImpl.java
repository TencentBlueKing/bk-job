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

package com.tencent.bk.job.manage.api.esb.impl.v4;

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.audit.annotations.AuditRequestBody;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.ResolvedSummary;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.api.esb.v4.OpenApiJobPlanV4Resource;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.esb.v4.OpenApiV4JobPlanDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4CreateJobPlanRequest;
import com.tencent.bk.job.manage.service.plan.V4JobPlanCreateService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OpenApiJobPlanV4ResourceImpl implements OpenApiJobPlanV4Resource {

    private final V4JobPlanCreateService jobPlanCreateService;
    private final AppScopeMappingService appScopeMappingService;

    @Autowired
    public OpenApiJobPlanV4ResourceImpl(V4JobPlanCreateService jobPlanCreateService,
                                        AppScopeMappingService appScopeMappingService) {
        this.jobPlanCreateService = jobPlanCreateService;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    @AuditEntry(actionId = ActionId.CREATE_JOB_PLAN)
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v4_create_job_plan"})
    public EsbV4Response<OpenApiV4JobPlanDTO> createJobPlan(String username,
                                                        String appCode,
                                                        Boolean dryRun,
                                                        @AuditRequestBody V4CreateJobPlanRequest request) {
        User user = JobContextUtil.getUser();
        boolean isDryRun = Boolean.TRUE.equals(dryRun);
        TaskPlanInfoDTO plan = jobPlanCreateService.createJobPlan(user, request, isDryRun);
        if (isDryRun) {
            return EsbV4Response.dryRunSuccess(buildSummary(plan));
        }
        return EsbV4Response.success(toOpenApiV4JobPlanDTO(request.getAppId(), username, plan));
    }

    private ResolvedSummary buildSummary(TaskPlanInfoDTO plan) {
        ResolvedSummary summary = new ResolvedSummary();
        summary.setName(plan.getName());
        summary.addField("job_template_id", String.valueOf(plan.getTemplateId()));
        if (CollectionUtils.isNotEmpty(plan.getEnableStepList())) {
            summary.addField("enable_steps", StringUtils.join(plan.getEnableStepList(), ","));
        }
        if (CollectionUtils.isNotEmpty(plan.getVariableList())) {
            summary.addField("variable_count", String.valueOf(plan.getVariableList().size()));
        }
        return summary;
    }

    private OpenApiV4JobPlanDTO toOpenApiV4JobPlanDTO(Long appId, String username, TaskPlanInfoDTO savedPlan) {
        OpenApiV4JobPlanDTO data = new OpenApiV4JobPlanDTO();
        ResourceScope scope = appScopeMappingService.getScopeByAppId(appId);
        if (scope != null) {
            data.setScopeType(scope.getType().getValue());
            data.setScopeId(scope.getId());
        }
        data.setJobPlanId(savedPlan.getId());
        data.setJobPlanName(savedPlan.getName());
        data.setJobTemplateId(savedPlan.getTemplateId());
        data.setCreator(savedPlan.getCreator() != null ? savedPlan.getCreator() : username);
        Long createTimeSeconds = savedPlan.getCreateTime();
        data.setCreateTime(createTimeSeconds == null ? null : createTimeSeconds * 1000L);
        data.setNeedUpdate(Boolean.TRUE.equals(savedPlan.getNeedUpdate()));
        return data;
    }
}
