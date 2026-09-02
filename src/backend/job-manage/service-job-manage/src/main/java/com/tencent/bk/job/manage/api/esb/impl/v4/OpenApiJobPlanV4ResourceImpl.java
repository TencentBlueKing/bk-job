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
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.esb.v4.OpenApiV4JobPlanDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4CreateJobPlanRequest;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobPlanVariableItem;
import com.tencent.bk.job.manage.service.plan.PlanGlobalVarSummaryBuilder;
import com.tencent.bk.job.manage.service.plan.V4JobPlanCreateService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class OpenApiJobPlanV4ResourceImpl implements OpenApiJobPlanV4Resource {

    private final V4JobPlanCreateService jobPlanCreateService;
    private final AppScopeMappingService appScopeMappingService;
    private final PlanGlobalVarSummaryBuilder globalVarSummaryBuilder;

    @Autowired
    public OpenApiJobPlanV4ResourceImpl(V4JobPlanCreateService jobPlanCreateService,
                                        AppScopeMappingService appScopeMappingService,
                                        PlanGlobalVarSummaryBuilder globalVarSummaryBuilder) {
        this.jobPlanCreateService = jobPlanCreateService;
        this.appScopeMappingService = appScopeMappingService;
        this.globalVarSummaryBuilder = globalVarSummaryBuilder;
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
            return EsbV4Response.dryRunSuccess(buildSummary(plan, request, user.getTenantId()));
        }
        return EsbV4Response.success(toOpenApiV4JobPlanDTO(request.getAppId(), username, plan));
    }

    private ResolvedSummary buildSummary(TaskPlanInfoDTO plan, V4CreateJobPlanRequest request, String tenantId) {
        ResolvedSummary summary = new ResolvedSummary();
        summary.setName(plan.getName());
        summary.addField("job_template_id", String.valueOf(plan.getTemplateId()));
        putEnableStepsField(summary, plan);
        // 预检返回的 variableList 已是合并后的全部方案变量，未被本次请求赋值的那些标为沿用模板默认值
        globalVarSummaryBuilder.fillGlobalVars(summary, plan.getVariableList(), assignedVarNames(request), tenantId);
        return summary;
    }

    /**
     * 本次请求真正给出了取值的变量名。
     * <p>
     * 声明了 follow_template、或压根没带取值的，最终生效的仍是模板默认值，不算本次指定
     */
    private Set<String> assignedVarNames(V4CreateJobPlanRequest request) {
        if (CollectionUtils.isEmpty(request.getVariables())) {
            return Collections.emptySet();
        }
        Set<String> names = new HashSet<>();
        for (V4JobPlanVariableItem item : request.getVariables()) {
            if (item == null || item.isFollowTemplate()) {
                continue;
            }
            if (item.getValue() != null || item.getExecuteTarget() != null) {
                names.add(item.getName());
            }
        }
        return names;
    }

    /**
     * 启用的步骤按<b>名称</b>逐行给出，一行一个步骤：一串步骤 ID 审批人完全看不出这个方案会跑什么。
     * <p>
     * 此处只管按行拼，渲染侧认得这个字段名并把它摘出概要表格、单独成章节逐行列出（表格单元格塞不下换行）。
     * <b>步骤不做条数截断</b>：条数上限就是模板的步骤数（人工编排出来的，不会像文件源那样上千条），
     * 而截掉几个步骤名恰好截掉的是本行唯一要说明的事。启用的是全部模板步骤时换用带「全部」注明的标签，
     * 省得审批人自己去数
     */
    private void putEnableStepsField(ResolvedSummary summary, TaskPlanInfoDTO plan) {
        List<Long> enableStepIds = plan.getEnableStepList();
        if (CollectionUtils.isEmpty(enableStepIds)) {
            return;
        }
        Map<Long, String> stepNames = new LinkedHashMap<>();
        if (CollectionUtils.isNotEmpty(plan.getStepList())) {
            for (TaskStepDTO step : plan.getStepList()) {
                stepNames.put(step.getId(), step.getName());
            }
        }
        List<String> names = new ArrayList<>(enableStepIds.size());
        for (Long stepId : enableStepIds) {
            // 名称缺失时退回 ID：整行不能因此变空，审批人至少还能拿 ID 去查
            names.add(StringUtils.defaultIfBlank(stepNames.get(stepId), String.valueOf(stepId)));
        }
        boolean allStepsEnabled = !stepNames.isEmpty() && enableStepIds.containsAll(stepNames.keySet());
        summary.addField(allStepsEnabled ? "enable_steps_all" : "enable_steps", String.join("\n", names));
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
